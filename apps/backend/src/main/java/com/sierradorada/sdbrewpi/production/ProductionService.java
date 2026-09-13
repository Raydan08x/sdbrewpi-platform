package com.sierradorada.sdbrewpi.production;

import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import com.sierradorada.sdbrewpi.fermentation.FermentationRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {
    private static final Set<String> MANUAL_EVENT_TYPES = Set.of(
        "OPERATOR_NOTE", "INGREDIENT_ADDITION", "MEASUREMENT", "SAMPLE", "DEVIATION", "QUALITY_CHECK", "SANITATION"
    );
    private final ProductionRepository repository;
    private final FermentationRepository fermentationRepository;

    public ProductionService(ProductionRepository repository, FermentationRepository fermentationRepository) {
        this.repository = repository;
        this.fermentationRepository = fermentationRepository;
    }

    public ProductionOverview overview() {
        return new ProductionOverview(Instant.now(), repository.findRecipes(), repository.findActiveBatches(),
            repository.findProcessStages());
    }

    @Transactional
    public RecipeView createRecipe(RecipeRequest request, String actor) {
        if (request.targetFinalGravity() >= request.originalGravity()) {
            throw new IllegalArgumentException("La densidad final objetivo debe ser menor que la densidad original");
        }
        String code = normalizeCode(request.code());
        int version = repository.nextVersion(code);
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        repository.insertRecipe(id, code, request.name().trim(), version, request.originalGravity(),
            request.targetFinalGravity(), request.defaultVolumeL(), safeNotes(request.notes()), now);
        for (int index = 0; index < request.steps().size(); index++) {
            repository.insertStep(UUID.randomUUID().toString(), id, index + 1, request.steps().get(index));
        }
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id, "CREATE_RECIPE_VERSION",
            code + ":v" + version, "Versión de receta creada");
        return repository.findRecipe(id).orElseThrow();
    }

    @Transactional
    public BatchView createBatch(BatchRequest request, String actor) {
        RecipeView recipe = repository.findRecipe(request.recipeVersionId())
            .orElseThrow(() -> new IllegalArgumentException("La versión de receta no existe"));
        String code = normalizeCode(request.code());
        repository.lockTank(request.tankId());
        if (repository.hasActiveBatch(request.tankId())) {
            throw new IllegalStateException("El fermentador ya tiene un lote activo");
        }
        if (repository.batchCodeExists(code)) {
            throw new IllegalStateException("El código de lote ya existe");
        }
        long durationHours = recipe.steps().stream().mapToLong(ProfileStepView::durationHours).sum();
        Instant startedAt = Instant.now();
        Instant expectedCompleteAt = startedAt.plus(durationHours, ChronoUnit.HOURS);
        String id = UUID.randomUUID().toString();
        repository.insertBatch(id, code, recipe, request.tankId(), request.volumeL(), startedAt, expectedCompleteAt);
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id, "START_BATCH", code,
            "Lote asociado al fermentador en simulación");
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView completeBatch(String id, BatchCompletionRequest request, String actor) {
        BatchView existing = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (!"ACTIVE".equals(existing.status())) throw new IllegalStateException("El lote ya está cerrado");
        if (repository.completeBatch(id, request.expectedRevision(), Instant.now()) == 0) {
            throw new RevisionConflictException();
        }
        fermentationRepository.disableProfileControl(existing.tankId());
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id, "COMPLETE_BATCH", existing.code(),
            "Lote marcado como completado");
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView startProfile(String id, ProfileCommandRequest request, String actor) {
        BatchView batch = activeBatch(id);
        requireState(batch, "NOT_STARTED", "El perfil ya fue iniciado");
        Instant now = Instant.now();
        if (repository.startProfile(id, request.expectedRevision(), now) == 0) throw new RevisionConflictException();
        ProfileStepView step = batch.profile().getFirst();
        fermentationRepository.configureForProfile(batch.tankId(), step.targetTemperatureC());
        recordProfileCommand(batch, "PROFILE_STARTED", step.order(), safeActor(actor),
            "Perfil iniciado en " + step.name(), now);
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView pauseProfile(String id, ProfileCommandRequest request, String actor) {
        BatchView batch = activeBatch(id);
        requireState(batch, "RUNNING", "El perfil no está ejecutándose");
        Instant now = Instant.now();
        long elapsed = Math.max(0, Duration.between(batch.stepStartedAt(), now).toSeconds());
        if (repository.pauseProfile(id, request.expectedRevision(), elapsed) == 0) throw new RevisionConflictException();
        recordProfileCommand(batch, "PROFILE_PAUSED", batch.currentStep(), safeActor(actor),
            "Perfil pausado; el objetivo térmico se mantiene", now);
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView resumeProfile(String id, ProfileCommandRequest request, String actor) {
        BatchView batch = activeBatch(id);
        requireState(batch, "PAUSED", "El perfil no está pausado");
        Instant now = Instant.now();
        Instant reconstructedStart = now.minusSeconds(Math.max(0, batch.stepElapsedSeconds()));
        if (repository.resumeProfile(id, request.expectedRevision(), reconstructedStart) == 0) {
            throw new RevisionConflictException();
        }
        ProfileStepView step = currentStep(batch);
        fermentationRepository.configureForProfile(batch.tankId(), step.targetTemperatureC());
        recordProfileCommand(batch, "PROFILE_RESUMED", step.order(), safeActor(actor),
            "Perfil reanudado en " + step.name(), now);
        return repository.findBatch(id).orElseThrow();
    }

    public List<BatchEventView> events(String id) {
        repository.findBatch(id).orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        return repository.findEvents(id);
    }

    @Transactional
    public BatchEventView addEvent(String id, BatchEventRequest request, String actor) {
        BatchView batch = activeBatch(id);
        String eventType = request.eventType().trim().toUpperCase(Locale.ROOT);
        if (!MANUAL_EVENT_TYPES.contains(eventType)) throw new IllegalArgumentException("El tipo de evento no es válido");
        int stepOrder = request.stepOrder() == null ? batch.currentStep() : request.stepOrder();
        if (stepOrder > batch.profile().size()) throw new IllegalArgumentException("La fase no pertenece al perfil del lote");

        String material = blankToNull(request.materialName());
        String unit = blankToNull(request.unit());
        Double quantity = request.quantity();
        if ("INGREDIENT_ADDITION".equals(eventType)
                && (material == null || unit == null || quantity == null || quantity <= 0)) {
            throw new IllegalArgumentException("Una adición requiere material, cantidad y unidad");
        }
        if (!"INGREDIENT_ADDITION".equals(eventType)) {
            material = null;
            unit = null;
            quantity = null;
        }

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String safeActor = safeActor(actor);
        String message = request.message().trim();
        repository.event(eventId, id, eventType, stepOrder, safeActor, message, material, quantity, unit, now);
        repository.audit(UUID.randomUUID().toString(), safeActor, id, "RECORD_BATCH_EVENT", eventType,
            "Evento operativo registrado en el lote");
        return new BatchEventView(eventId, now, eventType, stepOrder, safeActor, message, material, quantity, unit);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void advanceProfiles() {
        Instant now = Instant.now();
        for (BatchView candidate : repository.findRunningProfiles()) advanceDueSteps(candidate, now);
    }

    private void advanceDueSteps(BatchView initial, Instant now) {
        BatchView batch = initial;
        while ("RUNNING".equals(batch.profileState()) && batch.stepExpectedCompleteAt() != null
                && !now.isBefore(batch.stepExpectedCompleteAt())) {
            Instant transitionAt = batch.stepExpectedCompleteAt();
            int nextOrder = batch.currentStep() + 1;
            if (nextOrder > batch.profile().size()) {
                if (repository.finishProfile(batch.id(), batch.revision(), transitionAt) == 0) return;
                repository.event(UUID.randomUUID().toString(), batch.id(), "PROFILE_COMPLETED", batch.currentStep(),
                    "profile-engine", "Perfil térmico completado; se mantiene el último objetivo", transitionAt);
            } else {
                if (repository.advanceProfileStep(batch.id(), batch.revision(), nextOrder, transitionAt) == 0) return;
                ProfileStepView next = batch.profile().get(nextOrder - 1);
                fermentationRepository.configureForProfile(batch.tankId(), next.targetTemperatureC());
                repository.event(UUID.randomUUID().toString(), batch.id(), "PROFILE_STEP_CHANGED", nextOrder,
                    "profile-engine", "Inicio automático de " + next.name(), transitionAt);
            }
            batch = repository.findBatch(batch.id()).orElseThrow();
        }
    }

    private BatchView activeBatch(String id) {
        BatchView batch = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (!"ACTIVE".equals(batch.status())) throw new IllegalStateException("El lote ya está cerrado");
        if (batch.profile().isEmpty()) throw new IllegalStateException("El lote no tiene perfil de fermentación");
        return batch;
    }

    private void requireState(BatchView batch, String expected, String message) {
        if (!expected.equals(batch.profileState())) throw new IllegalStateException(message);
    }

    private ProfileStepView currentStep(BatchView batch) {
        int index = Math.max(0, Math.min(batch.currentStep() - 1, batch.profile().size() - 1));
        return batch.profile().get(index);
    }

    private void recordProfileCommand(BatchView batch, String type, Integer step, String actor, String message,
            Instant now) {
        repository.event(UUID.randomUUID().toString(), batch.id(), type, step, actor, message, now);
        repository.audit(UUID.randomUUID().toString(), actor, batch.id(), type, batch.code(), message);
    }

    private String normalizeCode(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "-")
            .replaceAll("-+", "-");
        if (normalized.isBlank() || normalized.length() > 40) {
            throw new IllegalArgumentException("El código debe contener letras o números y tener máximo 40 caracteres");
        }
        return normalized;
    }

    private String safeNotes(String value) { return value == null ? "" : value.trim(); }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) return "local-webapp";
        String trimmed = actor.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }
}

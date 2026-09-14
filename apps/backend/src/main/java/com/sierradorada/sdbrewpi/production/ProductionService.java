package com.sierradorada.sdbrewpi.production;

import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import com.sierradorada.sdbrewpi.fermentation.FermentationRepository;
import com.sierradorada.sdbrewpi.fermentation.TankView;
import com.sierradorada.sdbrewpi.plant.PlantRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final PlantRepository plantRepository;

    public ProductionService(ProductionRepository repository, FermentationRepository fermentationRepository,
            PlantRepository plantRepository) {
        this.repository = repository;
        this.fermentationRepository = fermentationRepository;
        this.plantRepository = plantRepository;
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
        validateThermalRamps(request.steps());
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
    public BatchView releaseOrder(ProductionOrderReleaseRequest request, String actor) {
        RecipeView recipe = repository.findRecipe(request.recipeVersionId())
            .orElseThrow(() -> new IllegalArgumentException("La versión de receta no existe"));
        String safeActor = safeActor(actor);
        Instant now = Instant.now();
        ZoneId plantZone = ZoneId.of(plantRepository.primarySite().timezone());
        String period = DateTimeFormatter.ofPattern("yyMM").withZone(plantZone).format(now);
        int sequence = repository.nextLotNumber(period, request.productCode(), request.batchKind());
        String token = switch (request.batchKind()) {
            case "TEST" -> "T";
            case "PILOT" -> "P";
            case "COMMERCIAL" -> "L";
            default -> throw new IllegalArgumentException("El tipo de lote no es válido");
        };
        String productName = switch (request.productCode()) {
            case "CERV" -> "Cerveza";
            case "HSEL" -> "Hard seltzer";
            default -> throw new IllegalArgumentException("El producto no es válido");
        };
        String code = "%s-%s-%s%03d".formatted(request.productCode(), period, token, sequence);
        long durationHours = recipe.steps().stream().mapToLong(ProfileStepView::durationHours).sum();
        String id = UUID.randomUUID().toString();
        repository.insertReleasedOrder(id, code, recipe, request.plannedVolumeL(), now,
            now.plus(durationHours, ChronoUnit.HOURS), safeActor, request.batchKind(), request.productCode(), productName);
        repository.event(UUID.randomUUID().toString(), id, "ORDER_RELEASED", null, safeActor,
            "Orden liberada; batch record digital abierto con código " + code, now);
        repository.audit(UUID.randomUUID().toString(), safeActor, id, "RELEASE_PRODUCTION_ORDER", code,
            "Código de lote reservado y expediente digital abierto");
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView readyForFermentation(String id, BatchTransitionRequest request, String actor) {
        BatchView batch = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (!"RELEASED".equals(batch.status())) {
            throw new IllegalStateException("Solo una orden liberada puede marcarse lista para fermentación");
        }
        if (repository.readyForFermentation(id, request.expectedRevision()) == 0) throw new RevisionConflictException();
        String safeActor = safeActor(actor);
        repository.event(UUID.randomUUID().toString(), id, "READY_FOR_FERMENTATION", null, safeActor,
            "El lote fabricado quedó disponible para transferencia a fermentación", Instant.now());
        repository.audit(UUID.randomUUID().toString(), safeActor, id, "READY_FOR_FERMENTATION", batch.code(),
            "Liberación operativa hacia fermentación");
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView assignFermentation(String id, FermentationAssignmentRequest request, String actor) {
        BatchView batch = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (!"READY_FOR_FERMENTATION".equals(batch.status())) {
            throw new IllegalStateException("El lote todavía no está listo para fermentación");
        }
        repository.lockTank(request.tankId());
        if (repository.hasActiveBatch(request.tankId())) {
            throw new IllegalStateException("El fermentador ya tiene un lote activo");
        }
        TankView tank = fermentationRepository.findTank(request.tankId())
            .orElseThrow(() -> new IllegalArgumentException("El fermentador no existe"));
        String safeActor = safeActor(actor);
        Instant now = Instant.now();
        if (repository.assignFermentation(id, tank.id(), tank.name(), tank.pillId(), tank.pillSource(),
                request.transferredVolumeL(), now, safeActor, request.expectedRevision()) == 0) {
            throw new RevisionConflictException();
        }
        repository.event(UUID.randomUUID().toString(), id, "FERMENTATION_ASSIGNED", null, safeActor,
            "Transferencia asignada a " + tank.name() + " con " + tank.pillId(), now);
        repository.audit(UUID.randomUUID().toString(), safeActor, id, "ASSIGN_FERMENTATION",
            tank.id() + ":" + tank.pillId(), "Tanque y Pill guardados como snapshot del batch record");
        return repository.findBatch(id).orElseThrow();
    }

    @Transactional
    public BatchView completeBatch(String id, BatchCompletionRequest request, String actor) {
        BatchView existing = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (Set.of("COMPLETED", "CANCELLED").contains(existing.status())) throw new IllegalStateException("El lote ya está cerrado");
        if (repository.completeBatch(id, request.expectedRevision(), Instant.now()) == 0) {
            throw new RevisionConflictException();
        }
        if (existing.tankId() != null) fermentationRepository.disableProfileControl(existing.tankId());
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
        fermentationRepository.holdProfileControl(batch.tankId());
        recordProfileCommand(batch, "PROFILE_PAUSED", batch.currentStep(), safeActor(actor),
            "Perfil pausado; el tanque queda en control manual con la demanda detenida", now);
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
        fermentationRepository.configureForProfile(batch.tankId(), profileSetpoint(batch, step, batch.stepElapsedSeconds()));
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
                fermentationRepository.updateProfileSetpoint(batch.tankId(), currentStep(batch).targetTemperatureC());
                if (repository.finishProfile(batch.id(), batch.revision(), transitionAt) == 0) return;
                repository.event(UUID.randomUUID().toString(), batch.id(), "PROFILE_COMPLETED", batch.currentStep(),
                    "profile-engine", "Perfil térmico completado; se mantiene el último objetivo", transitionAt);
            } else {
                if (repository.advanceProfileStep(batch.id(), batch.revision(), nextOrder, transitionAt) == 0) return;
                ProfileStepView next = batch.profile().get(nextOrder - 1);
                repository.event(UUID.randomUUID().toString(), batch.id(), "PROFILE_STEP_CHANGED", nextOrder,
                    "profile-engine", stepTransitionMessage(next), transitionAt);
            }
            batch = repository.findBatch(batch.id()).orElseThrow();
        }
        if ("RUNNING".equals(batch.profileState())) {
            fermentationRepository.updateProfileSetpoint(batch.tankId(), profileSetpoint(batch, currentStep(batch), now));
        }
    }

    private BatchView activeBatch(String id) {
        BatchView batch = repository.findBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("El lote no existe"));
        if (!"FERMENTING".equals(batch.status())) throw new IllegalStateException("El lote no está asignado a fermentación");
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

    private double profileSetpoint(BatchView batch, ProfileStepView step, Instant now) {
        long elapsedSeconds = batch.stepStartedAt() == null ? 0 : Math.max(0, Duration.between(batch.stepStartedAt(), now).toSeconds());
        return profileSetpoint(batch, step, elapsedSeconds);
    }

    private double profileSetpoint(BatchView batch, ProfileStepView step, long elapsedSeconds) {
        if (step.order() <= 1 || step.rampRateCPerHour() == null) return step.targetTemperatureC();
        double start = batch.profile().get(step.order() - 2).targetTemperatureC();
        double target = step.targetTemperatureC();
        double maximumChange = step.rampRateCPerHour() * elapsedSeconds / 3600.0;
        double appliedChange = Math.min(Math.abs(target - start), maximumChange);
        double value = start + Math.copySign(appliedChange, target - start);
        return Math.round(value * 100.0) / 100.0;
    }

    private String stepTransitionMessage(ProfileStepView step) {
        if (step.rampRateCPerHour() == null) return "Inicio automático de " + step.name();
        return "Inicio automático de " + step.name() + "; rampa de " + step.rampRateCPerHour() + " °C/h";
    }

    private void validateThermalRamps(List<ProfileStepRequest> steps) {
        if (steps.getFirst().rampRateCPerHour() != null) {
            throw new IllegalArgumentException("La primera fase no puede definir una rampa térmica");
        }
        for (int index = 1; index < steps.size(); index++) {
            ProfileStepRequest previous = steps.get(index - 1);
            ProfileStepRequest current = steps.get(index);
            if (current.rampRateCPerHour() == null) continue;
            double requiredChange = Math.abs(current.targetTemperatureC() - previous.targetTemperatureC());
            double possibleChange = current.rampRateCPerHour() * current.durationHours();
            if (possibleChange + 0.0001 < requiredChange) {
                throw new IllegalArgumentException("La rampa de la fase " + current.name().trim()
                    + " no alcanza su temperatura objetivo dentro de la duración configurada");
            }
        }
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

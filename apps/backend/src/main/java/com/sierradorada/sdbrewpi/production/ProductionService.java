package com.sierradorada.sdbrewpi.production;

import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {
    private final ProductionRepository repository;

    public ProductionService(ProductionRepository repository) { this.repository = repository; }

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
        repository.audit(UUID.randomUUID().toString(), safeActor(actor), id, "COMPLETE_BATCH", existing.code(),
            "Lote marcado como completado");
        return repository.findBatch(id).orElseThrow();
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

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) return "local-webapp";
        String trimmed = actor.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }
}

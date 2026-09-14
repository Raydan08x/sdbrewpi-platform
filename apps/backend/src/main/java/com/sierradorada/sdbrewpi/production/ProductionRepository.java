package com.sierradorada.sdbrewpi.production;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductionRepository {
    private final JdbcTemplate jdbc;

    public ProductionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RecipeView> findRecipes() {
        return jdbc.query("SELECT * FROM recipe_version ORDER BY recipe_code, version_number DESC",
            (rs, row) -> mapRecipeBase(rs)).stream().map(this::withSteps).toList();
    }

    public List<ProductionStageView> findProcessStages() {
        return jdbc.query("SELECT * FROM process_stage_definition WHERE enabled = TRUE ORDER BY step_order",
            (rs, row) -> new ProductionStageView(rs.getString("code"), rs.getString("phase"),
                rs.getInt("step_order"), rs.getString("name"), rs.getString("description"),
                rs.getBoolean("optional"), rs.getString("variant")));
    }

    public Optional<RecipeView> findRecipe(String id) {
        return jdbc.query("SELECT * FROM recipe_version WHERE id = ?", (rs, row) -> mapRecipeBase(rs), id)
            .stream().findFirst().map(this::withSteps);
    }

    public int nextVersion(String code) {
        Integer value = jdbc.queryForObject(
            "SELECT COALESCE(MAX(version_number), 0) + 1 FROM recipe_version WHERE recipe_code = ?", Integer.class, code);
        return value == null ? 1 : value;
    }

    public void insertRecipe(String id, String code, String name, int version, double originalGravity,
            double targetFinalGravity, double defaultVolumeL, String notes, Instant createdAt) {
        jdbc.update("INSERT INTO recipe_version VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", id, code, name, version,
            originalGravity, targetFinalGravity, defaultVolumeL, notes, Timestamp.from(createdAt));
    }

    public void insertStep(String id, String recipeId, int order, ProfileStepRequest step) {
        jdbc.update("""
            INSERT INTO fermentation_profile_step
                (id, recipe_version_id, step_order, name, target_temp_c, duration_hours, ramp_rate_c_per_hour)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, id, recipeId, order, step.name().trim(), step.targetTemperatureC(), step.durationHours(),
            step.rampRateCPerHour());
    }

    public void lockTank(String tankId) {
        List<String> ids = jdbc.query("SELECT id FROM fermentation_tank WHERE id = ? FOR UPDATE",
            (rs, row) -> rs.getString(1), tankId);
        if (ids.isEmpty()) throw new IllegalArgumentException("El fermentador no existe");
    }

    public boolean hasActiveBatch(String tankId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM production_batch WHERE tank_id = ? AND status = 'FERMENTING'", Integer.class, tankId);
        return count != null && count > 0;
    }

    public int nextLotNumber(String period, String productCode, String batchKind) {
        List<Integer> values = jdbc.query("SELECT next_number FROM lot_number_counter WHERE lot_period = ? AND product_code = ? AND batch_kind = ? FOR UPDATE",
            (rs, row) -> rs.getInt(1), period, productCode, batchKind);
        if (values.isEmpty()) {
            jdbc.update("INSERT INTO lot_number_counter (lot_period, product_code, batch_kind, next_number) VALUES (?, ?, ?, 2)", period, productCode, batchKind);
            return 1;
        }
        int current = values.getFirst();
        jdbc.update("UPDATE lot_number_counter SET next_number = ? WHERE lot_period = ? AND product_code = ? AND batch_kind = ?",
            current + 1, period, productCode, batchKind);
        return current;
    }

    public void insertReleasedOrder(String id, String code, RecipeView recipe, double volumeL,
            Instant openedAt, Instant expectedCompleteAt, String actor, String batchKind, String productCode,
            String productName) {
        jdbc.update("""
            INSERT INTO production_batch
              (id, code, recipe_version_id, recipe_code_snapshot, recipe_name_snapshot, recipe_version_snapshot,
               tank_id, volume_l, status, current_step, started_at, expected_complete_at, completed_at, revision,
               active_slot, profile_state, batch_record_opened_at, released_by, batch_kind, product_code, product_name)
            VALUES (?, ?, ?, ?, ?, ?, NULL, ?, 'RELEASED', 1, ?, ?, NULL, 0, NULL, 'NOT_STARTED', ?, ?, ?, ?, ?)
            """, id, code, recipe.id(), recipe.code(), recipe.name(), recipe.version(), volumeL,
            Timestamp.from(openedAt), Timestamp.from(expectedCompleteAt), Timestamp.from(openedAt), actor, batchKind,
            productCode, productName);
    }

    public List<BatchView> findActiveBatches() {
        return jdbc.query("SELECT * FROM production_batch WHERE status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY started_at, code",
            (rs, row) -> mapBatchBase(rs)).stream().map(this::withProfile).toList();
    }

    public List<BatchView> findRunningProfiles() {
        return jdbc.query("SELECT * FROM production_batch WHERE status = 'FERMENTING' AND profile_state = 'RUNNING' ORDER BY started_at",
            (rs, row) -> mapBatchBase(rs)).stream().map(this::withProfile).toList();
    }

    public Optional<BatchView> findBatch(String id) {
        return jdbc.query("SELECT * FROM production_batch WHERE id = ?", (rs, row) -> mapBatchBase(rs), id)
            .stream().findFirst().map(this::withProfile);
    }

    public int completeBatch(String id, long expectedRevision, Instant completedAt) {
        return jdbc.update("UPDATE production_batch SET status = 'COMPLETED', profile_state = 'COMPLETED', "
            + "profile_completed_at = COALESCE(profile_completed_at, ?), completed_at = ?, active_slot = NULL, "
            + "revision = revision + 1 "
            + "WHERE id = ? AND status NOT IN ('COMPLETED', 'CANCELLED') AND revision = ?", Timestamp.from(completedAt),
            Timestamp.from(completedAt), id, expectedRevision);
    }

    public int readyForFermentation(String id, long expectedRevision) {
        return jdbc.update("UPDATE production_batch SET status = 'READY_FOR_FERMENTATION', revision = revision + 1 "
            + "WHERE id = ? AND status = 'RELEASED' AND revision = ?", id, expectedRevision);
    }

    public int assignFermentation(String id, String tankId, String tankName, String pillId, String pillSource,
            double volumeL, Instant assignedAt, String actor, long expectedRevision) {
        return jdbc.update("""
            UPDATE production_batch SET status = 'FERMENTING', tank_id = ?, active_slot = 1,
                tank_name_snapshot = ?, pill_id_snapshot = ?, pill_source_snapshot = ?, fermentation_volume_l = ?,
                fermentation_assigned_at = ?, fermentation_assigned_by = ?, revision = revision + 1
            WHERE id = ? AND status = 'READY_FOR_FERMENTATION' AND revision = ?
            """, tankId, tankName, pillId, pillSource, volumeL, Timestamp.from(assignedAt), actor, id, expectedRevision);
    }

    public int startProfile(String id, long expectedRevision, Instant now) {
        return jdbc.update("""
            UPDATE production_batch SET profile_state = 'RUNNING', current_step = 1, step_started_at = ?,
                step_elapsed_seconds = 0, revision = revision + 1
            WHERE id = ? AND status = 'FERMENTING' AND profile_state = 'NOT_STARTED' AND revision = ?
            """, Timestamp.from(now), id, expectedRevision);
    }

    public int pauseProfile(String id, long expectedRevision, long elapsedSeconds) {
        return jdbc.update("""
            UPDATE production_batch SET profile_state = 'PAUSED', step_elapsed_seconds = ?, revision = revision + 1
            WHERE id = ? AND status = 'FERMENTING' AND profile_state = 'RUNNING' AND revision = ?
            """, elapsedSeconds, id, expectedRevision);
    }

    public int resumeProfile(String id, long expectedRevision, Instant reconstructedStepStart) {
        return jdbc.update("""
            UPDATE production_batch SET profile_state = 'RUNNING', step_started_at = ?, revision = revision + 1
            WHERE id = ? AND status = 'FERMENTING' AND profile_state = 'PAUSED' AND revision = ?
            """, Timestamp.from(reconstructedStepStart), id, expectedRevision);
    }

    public int advanceProfileStep(String id, long expectedRevision, int nextStep, Instant nextStartedAt) {
        return jdbc.update("""
            UPDATE production_batch SET current_step = ?, step_started_at = ?, step_elapsed_seconds = 0,
                revision = revision + 1
            WHERE id = ? AND status = 'FERMENTING' AND profile_state = 'RUNNING' AND revision = ?
            """, nextStep, Timestamp.from(nextStartedAt), id, expectedRevision);
    }

    public int finishProfile(String id, long expectedRevision, Instant completedAt) {
        return jdbc.update("""
            UPDATE production_batch SET profile_state = 'COMPLETED', profile_completed_at = ?,
                step_elapsed_seconds = 0, revision = revision + 1
            WHERE id = ? AND status = 'FERMENTING' AND profile_state = 'RUNNING' AND revision = ?
            """, Timestamp.from(completedAt), id, expectedRevision);
    }

    public void event(String id, String batchId, String eventType, Integer stepOrder, String actor, String message,
            Instant occurredAt) {
        event(id, batchId, eventType, stepOrder, actor, message, null, null, null, occurredAt);
    }

    public void event(String id, String batchId, String eventType, Integer stepOrder, String actor, String message,
            String materialName, Double quantity, String unit, Instant occurredAt) {
        jdbc.update("""
            INSERT INTO batch_event
                (id, batch_id, occurred_at, event_type, step_order, actor, message, material_name, quantity, unit)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, id, batchId, Timestamp.from(occurredAt), eventType, stepOrder, actor, message, materialName,
            quantity, unit);
    }

    public List<BatchEventView> findEvents(String batchId) {
        return jdbc.query("""
            SELECT id, occurred_at, event_type, step_order, actor, message, material_name, quantity, unit
            FROM batch_event WHERE batch_id = ? ORDER BY occurred_at DESC
            """, (rs, row) -> new BatchEventView(rs.getString("id"), rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("event_type"), nullableInteger(rs, "step_order"), rs.getString("actor"),
                rs.getString("message"), rs.getString("material_name"), nullableDouble(rs, "quantity"),
                rs.getString("unit")), batchId);
    }

    public void audit(String id, String actor, String target, String type, String payload, String reason) {
        jdbc.update("INSERT INTO command_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, Timestamp.from(Instant.now()),
            actor, target, type, payload, "ACCEPTED", reason);
    }

    private RecipeView mapRecipeBase(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        return new RecipeView(id, rs.getString("recipe_code"), rs.getString("name"), rs.getInt("version_number"),
            rs.getDouble("original_gravity"), rs.getDouble("target_final_gravity"), rs.getDouble("default_volume_l"),
            rs.getString("notes"), rs.getTimestamp("created_at").toInstant(), List.of());
    }

    private RecipeView withSteps(RecipeView recipe) {
        return new RecipeView(recipe.id(), recipe.code(), recipe.name(), recipe.version(), recipe.originalGravity(),
            recipe.targetFinalGravity(), recipe.defaultVolumeL(), recipe.notes(), recipe.createdAt(), findSteps(recipe.id()));
    }

    private BatchView mapBatchBase(ResultSet rs) throws SQLException {
        String recipeId = rs.getString("recipe_version_id");
        return new BatchView(rs.getString("id"), rs.getString("code"), recipeId,
            rs.getString("recipe_code_snapshot"), rs.getString("recipe_name_snapshot"),
            rs.getInt("recipe_version_snapshot"), rs.getString("tank_id"), rs.getDouble("volume_l"),
            rs.getString("status"), rs.getInt("current_step"), rs.getString("profile_state"),
            nullableInstant(rs, "step_started_at"), null, rs.getLong("step_elapsed_seconds"),
            nullableInstant(rs, "profile_completed_at"), rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("expected_complete_at").toInstant(), nullableInstant(rs, "completed_at"),
            rs.getLong("revision"), nullableInstant(rs, "batch_record_opened_at"), rs.getString("released_by"),
            rs.getString("tank_name_snapshot"), rs.getString("pill_id_snapshot"), rs.getString("pill_source_snapshot"),
            nullableDouble(rs, "fermentation_volume_l"), nullableInstant(rs, "fermentation_assigned_at"),
            rs.getString("fermentation_assigned_by"), rs.getString("batch_kind"), rs.getString("product_code"),
            rs.getString("product_name"), List.of());
    }

    private BatchView withProfile(BatchView batch) {
        List<ProfileStepView> profile = findSteps(batch.recipeVersionId());
        Instant expectedStepEnd = null;
        if (batch.stepStartedAt() != null && batch.currentStep() >= 1 && batch.currentStep() <= profile.size()) {
            expectedStepEnd = batch.stepStartedAt().plus(Duration.ofHours(profile.get(batch.currentStep() - 1).durationHours()));
        }
        return new BatchView(batch.id(), batch.code(), batch.recipeVersionId(), batch.recipeCode(), batch.recipeName(),
            batch.recipeVersion(), batch.tankId(), batch.volumeL(), batch.status(), batch.currentStep(), batch.profileState(),
            batch.stepStartedAt(), expectedStepEnd, batch.stepElapsedSeconds(), batch.profileCompletedAt(), batch.startedAt(),
            batch.expectedCompleteAt(), batch.completedAt(), batch.revision(), batch.batchRecordOpenedAt(),
            batch.releasedBy(), batch.tankNameSnapshot(), batch.pillIdSnapshot(), batch.pillSourceSnapshot(),
            batch.fermentationVolumeL(), batch.fermentationAssignedAt(), batch.fermentationAssignedBy(), batch.batchKind(),
            batch.productCode(), batch.productName(), profile);
    }

    private List<ProfileStepView> findSteps(String recipeId) {
        return jdbc.query("SELECT * FROM fermentation_profile_step WHERE recipe_version_id = ? ORDER BY step_order",
            (rs, row) -> new ProfileStepView(rs.getInt("step_order"), rs.getString("name"),
                rs.getDouble("target_temp_c"), rs.getInt("duration_hours"),
                nullableDouble(rs, "ramp_rate_c_per_hour")), recipeId);
    }

    private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}

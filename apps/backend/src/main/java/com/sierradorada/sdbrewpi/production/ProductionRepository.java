package com.sierradorada.sdbrewpi.production;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
        jdbc.update("INSERT INTO fermentation_profile_step VALUES (?, ?, ?, ?, ?, ?)", id, recipeId, order,
            step.name().trim(), step.targetTemperatureC(), step.durationHours());
    }

    public void lockTank(String tankId) {
        List<String> ids = jdbc.query("SELECT id FROM fermentation_tank WHERE id = ? FOR UPDATE",
            (rs, row) -> rs.getString(1), tankId);
        if (ids.isEmpty()) throw new IllegalArgumentException("El fermentador no existe");
    }

    public boolean hasActiveBatch(String tankId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM production_batch WHERE tank_id = ? AND status = 'ACTIVE'", Integer.class, tankId);
        return count != null && count > 0;
    }

    public boolean batchCodeExists(String code) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM production_batch WHERE code = ?", Integer.class, code);
        return count != null && count > 0;
    }

    public void insertBatch(String id, String code, RecipeView recipe, String tankId, double volumeL,
            Instant startedAt, Instant expectedCompleteAt) {
        jdbc.update("INSERT INTO production_batch (id, code, recipe_version_id, recipe_code_snapshot, "
            + "recipe_name_snapshot, recipe_version_snapshot, tank_id, volume_l, status, current_step, started_at, "
            + "expected_complete_at, completed_at, revision, active_slot) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 1, ?, ?, NULL, 0, 1)",
            id, code, recipe.id(), recipe.code(), recipe.name(), recipe.version(), tankId, volumeL,
            Timestamp.from(startedAt), Timestamp.from(expectedCompleteAt));
    }

    public List<BatchView> findActiveBatches() {
        return jdbc.query("SELECT * FROM production_batch WHERE status = 'ACTIVE' ORDER BY started_at, code",
            (rs, row) -> mapBatchBase(rs)).stream().map(this::withProfile).toList();
    }

    public Optional<BatchView> findBatch(String id) {
        return jdbc.query("SELECT * FROM production_batch WHERE id = ?", (rs, row) -> mapBatchBase(rs), id)
            .stream().findFirst().map(this::withProfile);
    }

    public int completeBatch(String id, long expectedRevision, Instant completedAt) {
        return jdbc.update("UPDATE production_batch SET status = 'COMPLETED', completed_at = ?, active_slot = NULL, "
            + "revision = revision + 1 "
            + "WHERE id = ? AND status = 'ACTIVE' AND revision = ?", Timestamp.from(completedAt), id, expectedRevision);
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
            rs.getString("status"), rs.getInt("current_step"), rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("expected_complete_at").toInstant(), nullableInstant(rs, "completed_at"),
            rs.getLong("revision"), List.of());
    }

    private BatchView withProfile(BatchView batch) {
        return new BatchView(batch.id(), batch.code(), batch.recipeVersionId(), batch.recipeCode(), batch.recipeName(),
            batch.recipeVersion(), batch.tankId(), batch.volumeL(), batch.status(), batch.currentStep(), batch.startedAt(),
            batch.expectedCompleteAt(), batch.completedAt(), batch.revision(), findSteps(batch.recipeVersionId()));
    }

    private List<ProfileStepView> findSteps(String recipeId) {
        return jdbc.query("SELECT * FROM fermentation_profile_step WHERE recipe_version_id = ? ORDER BY step_order",
            (rs, row) -> new ProfileStepView(rs.getInt("step_order"), rs.getString("name"),
                rs.getDouble("target_temp_c"), rs.getInt("duration_hours")), recipeId);
    }

    private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}

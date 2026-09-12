package com.sierradorada.sdbrewpi.fermentation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FermentationRepository {
    private final JdbcTemplate jdbc;

    public FermentationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<TankView> findTanks() {
        return jdbc.query("SELECT * FROM fermentation_tank ORDER BY id", (rs, row) -> mapTank(rs));
    }

    public Optional<TankView> findTank(String id) {
        return jdbc.query("SELECT * FROM fermentation_tank WHERE id = ?", (rs, row) -> mapTank(rs), id).stream().findFirst();
    }

    public int updateSetpoint(String id, double value, long expectedRevision) {
        return jdbc.update("UPDATE fermentation_tank SET setpoint_c = ?, revision = revision + 1 WHERE id = ? AND revision = ?", value, id, expectedRevision);
    }

    public int updateMode(String id, ControlMode mode, long expectedRevision) {
        return jdbc.update("UPDATE fermentation_tank SET mode = ?, cooling_demand = FALSE, revision = revision + 1 WHERE id = ? AND revision = ?", mode.name(), id, expectedRevision);
    }

    public void simulate(String id, double temperature, double gravity, boolean demand, Instant capturedAt) {
        jdbc.update("UPDATE fermentation_tank SET product_temp_c = ?, gravity = ?, pill_captured_at = ?, pill_quality = 'GOOD', cooling_demand = ? WHERE id = ?", temperature, gravity, Timestamp.from(capturedAt), demand, id);
    }

    public ChillerView findChiller(boolean hardwareEnabled) {
        return jdbc.queryForObject("SELECT * FROM chiller_state WHERE id = 1", (rs, row) -> new ChillerView(
            rs.getString("mode"), nullableDouble(rs, "reservoir_temp_c"), rs.getString("reservoir_quality"),
            rs.getBoolean("pump_on"), rs.getBoolean("compressor_request"), rs.getString("environment"), hardwareEnabled,
            rs.getLong("revision")));
    }

    public void simulateChiller(boolean active) {
        jdbc.update("UPDATE chiller_state SET pump_on = ?, compressor_request = ?, revision = revision + 1 WHERE id = 1", active, active);
    }

    public void audit(String id, String actor, String target, String type, String payload, String result, String reason) {
        jdbc.update("INSERT INTO command_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, Timestamp.from(Instant.now()), actor, target, type, payload, result, reason);
    }

    private TankView mapTank(java.sql.ResultSet rs) throws java.sql.SQLException {
        Instant captured = rs.getTimestamp("pill_captured_at").toInstant();
        long age = Math.max(0, java.time.Duration.between(captured, Instant.now()).toSeconds());
        return new TankView(rs.getString("id"), rs.getString("name"), ControlMode.valueOf(rs.getString("mode")),
            rs.getDouble("setpoint_c"), rs.getDouble("product_temp_c"), rs.getDouble("gravity"),
            rs.getString("pill_id"), rs.getString("pill_quality"), captured, age,
            rs.getBoolean("cooling_demand"), rs.getLong("revision"));
    }

    private Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}

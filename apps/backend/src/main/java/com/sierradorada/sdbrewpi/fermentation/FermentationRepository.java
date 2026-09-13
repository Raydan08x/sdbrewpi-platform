package com.sierradorada.sdbrewpi.fermentation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public int configureForProfile(String id, double setpointC) {
        return jdbc.update("""
            UPDATE fermentation_tank SET mode = 'AUTO', setpoint_c = ?, cooling_demand = FALSE,
                revision = revision + 1 WHERE id = ?
            """, setpointC, id);
    }

    public int disableProfileControl(String id) {
        return jdbc.update("""
            UPDATE fermentation_tank SET mode = 'OFF', cooling_demand = FALSE,
                revision = revision + 1 WHERE id = ?
            """, id);
    }

    public void simulate(String id, double temperature, double gravity, boolean demand, Instant capturedAt) {
        jdbc.update("""
            UPDATE fermentation_tank SET product_temp_c = ?, gravity = ?, pill_captured_at = ?,
                pill_received_at = ?, pill_quality = 'GOOD', pill_source = 'SIMULATION',
                pill_battery_pct = NULL, pill_rssi_dbm = NULL, cooling_demand = ? WHERE id = ?
            """, temperature, gravity, Timestamp.from(capturedAt), Timestamp.from(capturedAt), demand, id);
    }

    public void updateFromPlcDemo(String id, double temperature, Instant capturedAt) {
        jdbc.update("""
            UPDATE fermentation_tank
            SET product_temp_c = ?, pill_captured_at = ?, pill_received_at = ?, pill_quality = 'GOOD',
                pill_source = 'PLC_DEMO_SERIAL'
            WHERE id = ?
            """, temperature, Timestamp.from(capturedAt), Timestamp.from(capturedAt), id);
    }

    public void recordMeasurement(String tankId, String source, Instant capturedAt, Instant receivedAt,
            Double temperature, Double gravity, String quality) {
        jdbc.update("""
            INSERT INTO fermentation_measurement
                (tank_id, source, captured_at, received_at, temperature_c, gravity, quality)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, tankId, source, Timestamp.from(capturedAt), Timestamp.from(receivedAt), temperature, gravity, quality);
    }

    public boolean recordMeasurementIfDue(String tankId, String source, Instant capturedAt, Double temperature,
            Double gravity, String quality, Instant notAfter) {
        return jdbc.update("""
            INSERT INTO fermentation_measurement
                (tank_id, source, captured_at, received_at, temperature_c, gravity, quality)
            SELECT ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM fermentation_measurement WHERE tank_id = ? AND captured_at > ?
            )
            """, tankId, source, Timestamp.from(capturedAt), Timestamp.from(capturedAt), temperature, gravity, quality,
            tankId, Timestamp.from(notAfter)) > 0;
    }

    public List<FermentationMeasurementView> findHistory(String tankId, Instant from, int limit) {
        return jdbc.query("""
            SELECT captured_at, received_at, temperature_c, gravity, quality, source
            FROM fermentation_measurement
            WHERE tank_id = ? AND captured_at >= ?
            ORDER BY captured_at DESC LIMIT ?
            """, (rs, row) -> new FermentationMeasurementView(rs.getTimestamp("captured_at").toInstant(),
                rs.getTimestamp("received_at").toInstant(), nullableDouble(rs, "temperature_c"),
                nullableDouble(rs, "gravity"), rs.getString("quality"), rs.getString("source")),
            tankId, Timestamp.from(from), limit);
    }

    public ChillerView findChiller(boolean hardwareEnabled) {
        return jdbc.queryForObject("SELECT * FROM chiller_state WHERE id = 1", (rs, row) -> new ChillerView(
            rs.getString("mode"), nullableDouble(rs, "reservoir_temp_c"), rs.getString("reservoir_quality"),
            rs.getBoolean("pump_on"), rs.getBoolean("compressor_request"), rs.getString("environment"), hardwareEnabled,
            rs.getLong("revision")));
    }

    public void simulateChiller(boolean active) {
        jdbc.update("""
            UPDATE chiller_state SET pump_on = ?, compressor_request = ?, mode = 'SIMULATION',
                environment = 'SIMULATION', revision = revision + 1 WHERE id = 1
            """, active, active);
    }

    public void updateChillerFromPlcDemo(boolean pumpOn, boolean chillerOn) {
        jdbc.update("""
            UPDATE chiller_state SET pump_on = ?, compressor_request = ?, mode = 'PLC_DEMO',
                environment = 'PLC_DEMO', revision = revision + 1 WHERE id = 1
            """, pumpOn, chillerOn);
    }

    public List<AlarmView> findActiveAlarms() {
        return jdbc.query("""
            SELECT * FROM fermentation_alarm WHERE status = 'OPEN' ORDER BY opened_at DESC
            """, (rs, row) -> mapAlarm(rs));
    }

    public Optional<AlarmView> findAlarm(String id) {
        return jdbc.query("SELECT * FROM fermentation_alarm WHERE id = ?", (rs, row) -> mapAlarm(rs), id)
            .stream().findFirst();
    }

    public List<AlarmView> findAlarms(Instant from, int limit) {
        return jdbc.query("""
            SELECT * FROM fermentation_alarm WHERE opened_at >= ? ORDER BY opened_at DESC LIMIT ?
            """, (rs, row) -> mapAlarm(rs), Timestamp.from(from), limit);
    }

    public int acknowledgeAlarm(String id, long expectedRevision, String actor, String note, Instant now) {
        return jdbc.update("""
            UPDATE fermentation_alarm
            SET acknowledged_at = ?, acknowledged_by = ?, acknowledgment_note = ?, revision = revision + 1
            WHERE id = ? AND status = 'OPEN' AND acknowledged_at IS NULL AND revision = ?
            """, Timestamp.from(now), actor, note, id, expectedRevision);
    }

    public void reconcileAlarm(String alarmKey, String targetId, String code, String severity, String message,
            String source, boolean active, Instant now) {
        List<String> openIds = jdbc.query("SELECT id FROM fermentation_alarm WHERE alarm_key = ? AND status = 'OPEN'",
            (rs, row) -> rs.getString(1), alarmKey);
        if (active) {
            if (openIds.isEmpty()) {
                jdbc.update("""
                    INSERT INTO fermentation_alarm
                    (id, alarm_key, target_id, code, severity, status, message, source, opened_at, last_seen_at, cleared_at)
                    VALUES (?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, NULL)
                    """, UUID.randomUUID().toString(), alarmKey, targetId, code, severity, message, source,
                    Timestamp.from(now), Timestamp.from(now));
            } else {
                jdbc.update("UPDATE fermentation_alarm SET last_seen_at = ?, message = ? WHERE id = ?",
                    Timestamp.from(now), message, openIds.getFirst());
            }
        } else if (!openIds.isEmpty()) {
            jdbc.update("UPDATE fermentation_alarm SET status = 'CLEARED', cleared_at = ? WHERE id = ?",
                Timestamp.from(now), openIds.getFirst());
        }
    }

    public void audit(String id, String actor, String target, String type, String payload, String result, String reason) {
        jdbc.update("INSERT INTO command_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, Timestamp.from(Instant.now()), actor, target, type, payload, result, reason);
    }

    private TankView mapTank(java.sql.ResultSet rs) throws java.sql.SQLException {
        Instant captured = rs.getTimestamp("pill_captured_at").toInstant();
        long age = Math.max(0, java.time.Duration.between(captured, Instant.now()).toSeconds());
        return new TankView(rs.getString("id"), rs.getString("name"), ControlMode.valueOf(rs.getString("mode")),
            rs.getDouble("setpoint_c"), rs.getDouble("product_temp_c"), rs.getDouble("gravity"),
            rs.getString("pill_id"), rs.getString("pill_quality"), captured, nullableInstant(rs, "pill_received_at"), age,
            nullableInteger(rs, "pill_battery_pct"), nullableInteger(rs, "pill_rssi_dbm"), rs.getString("pill_source"),
            rs.getBoolean("cooling_demand"), rs.getLong("revision"));
    }

    private Instant nullableInstant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private AlarmView mapAlarm(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AlarmView(rs.getString("id"), rs.getString("target_id"), rs.getString("code"),
            rs.getString("severity"), rs.getString("status"), rs.getString("message"), rs.getString("source"),
            rs.getTimestamp("opened_at").toInstant(), rs.getTimestamp("last_seen_at").toInstant(),
            nullableInstant(rs, "cleared_at"), nullableInstant(rs, "acknowledged_at"),
            rs.getString("acknowledged_by"), rs.getString("acknowledgment_note"), rs.getLong("revision"));
    }
}

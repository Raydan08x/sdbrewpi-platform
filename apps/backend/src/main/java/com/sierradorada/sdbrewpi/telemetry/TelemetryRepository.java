package com.sierradorada.sdbrewpi.telemetry;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TelemetryRepository {
    private final JdbcTemplate jdbc;

    public TelemetryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean insert(PillTelemetrySample sample) {
        try {
            jdbc.update("""
                INSERT INTO pill_telemetry
                (message_id, pill_id, source_topic, source_format, sequence_number, captured_at, received_at, temperature_c, gravity,
                 battery_pct, rssi_dbm, quality, historical, retained, raw_payload)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, sample.messageId(), sample.pillId(), sample.sourceTopic(), sample.sourceFormat(),
                sample.sequence(),
                Timestamp.from(sample.capturedAt()), Timestamp.from(sample.receivedAt()), sample.temperatureC(), sample.gravity(),
                sample.batteryPct(), sample.rssiDbm(), sample.quality(), sample.historical(), sample.retained(), sample.rawPayload());
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public int updateCurrentTank(PillTelemetrySample sample) {
        return jdbc.update("""
            UPDATE fermentation_tank
            SET product_temp_c = COALESCE(?, product_temp_c), gravity = COALESCE(?, gravity),
                pill_battery_pct = COALESCE(?, pill_battery_pct), pill_rssi_dbm = COALESCE(?, pill_rssi_dbm), pill_captured_at = ?,
                pill_received_at = ?, pill_quality = ?, pill_source = ?
            WHERE pill_id = ? AND pill_captured_at <= ?
            """, sample.temperatureC(), sample.gravity(), sample.batteryPct(), sample.rssiDbm(),
            Timestamp.from(sample.capturedAt()), Timestamp.from(sample.receivedAt()), sample.quality(), sample.sourceFormat(),
            sample.pillId(), Timestamp.from(sample.capturedAt()));
    }

    public int recordMeasurement(PillTelemetrySample sample) {
        if (sample.temperatureC() == null && sample.gravity() == null) return 0;
        return jdbc.update("""
            INSERT INTO fermentation_measurement
                (tank_id, source, captured_at, received_at, temperature_c, gravity, quality)
            SELECT id, ?, ?, ?, ?, ?, ? FROM fermentation_tank WHERE pill_id = ?
            """, sample.sourceFormat(), Timestamp.from(sample.capturedAt()), Timestamp.from(sample.receivedAt()),
            sample.temperatureC(), sample.gravity(), sample.quality(), sample.pillId());
    }

    public void reject(String topic, String reason, Instant receivedAt) {
        jdbc.update("INSERT INTO telemetry_rejection (id, received_at, source_topic, reason) VALUES (?, ?, ?, ?)",
            UUID.randomUUID().toString(), Timestamp.from(receivedAt), safe(topic, 200), safe(reason, 300));
    }

    public int markStale(Instant cutoff) {
        return jdbc.update("""
            UPDATE fermentation_tank SET pill_quality = 'STALE'
            WHERE pill_source <> 'SIMULATION' AND pill_received_at < ? AND pill_quality <> 'STALE'
            """, Timestamp.from(cutoff));
    }

    public List<TelemetryRecordView> recent(String pillId, int limit) {
        return jdbc.query("""
            SELECT message_id, pill_id, source_format, sequence_number, captured_at, received_at, temperature_c, gravity,
                   battery_pct, rssi_dbm, quality, historical, retained
            FROM pill_telemetry WHERE pill_id = ? ORDER BY captured_at DESC LIMIT ?
            """, (rs, row) -> new TelemetryRecordView(
                rs.getString("message_id"), rs.getString("pill_id"), rs.getString("source_format"), nullableLong(rs, "sequence_number"),
                rs.getTimestamp("captured_at").toInstant(), rs.getTimestamp("received_at").toInstant(),
                nullableDouble(rs, "temperature_c"), nullableDouble(rs, "gravity"),
                nullableInteger(rs, "battery_pct"), nullableInteger(rs, "rssi_dbm"),
                rs.getString("quality"), rs.getBoolean("historical"), rs.getBoolean("retained")), pillId, limit);
    }

    public int rejectionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM telemetry_rejection", Integer.class);
    }

    private Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String safe(String value, int max) {
        if (value == null) return "desconocido";
        return value.length() > max ? value.substring(0, max) : value;
    }
}

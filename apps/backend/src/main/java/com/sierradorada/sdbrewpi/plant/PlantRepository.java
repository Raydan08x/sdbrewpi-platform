package com.sierradorada.sdbrewpi.plant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlantRepository {
    private final JdbcTemplate jdbc;

    public PlantRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PlantProfileView primarySite() {
        return jdbc.query("SELECT * FROM plant_site ORDER BY code FETCH FIRST 1 ROW ONLY", (rs, row) -> mapSite(rs))
            .stream().findFirst().orElseThrow();
    }

    public Optional<PlantProfileView> findSite(String id) {
        return jdbc.query("SELECT * FROM plant_site WHERE id = ?", (rs, row) -> mapSite(rs), id).stream().findFirst();
    }

    public List<PlantAssetView> findAssets(String siteId) {
        return jdbc.query("SELECT * FROM plant_asset WHERE site_id = ? ORDER BY asset_type, code",
            (rs, row) -> mapAsset(rs), siteId);
    }

    public int updateProfile(String id, PlantProfileRequest request, Instant updatedAt) {
        return jdbc.update("UPDATE plant_site SET name = ?, company_name = ?, legal_name = ?, tax_id = ?, timezone = ?, "
            + "currency = ?, nominal_batch_capacity_l = ?, planned_fermenters = ?, piping_dead_volume_l = ?, "
            + "logo_url = ?, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ?",
            request.name().trim(), request.companyName().trim(), clean(request.legalName()), clean(request.taxId()),
            request.timezone().trim(), request.currency().trim(), request.nominalBatchCapacityL(), request.plannedFermenters(),
            request.pipingDeadVolumeL(), clean(request.logoUrl()), Timestamp.from(updatedAt), id, request.expectedRevision());
    }

    public void audit(String id, String actor, String target, String payload) {
        jdbc.update("INSERT INTO command_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, Timestamp.from(Instant.now()),
            actor, target, "UPDATE_PLANT_PROFILE", payload, "ACCEPTED", "Configuración de planta actualizada");
    }

    private PlantProfileView mapSite(ResultSet rs) throws SQLException {
        return new PlantProfileView(rs.getString("id"), rs.getString("code"), rs.getString("name"),
            rs.getString("company_name"), rs.getString("legal_name"), rs.getString("tax_id"),
            rs.getString("timezone"), rs.getString("currency"), nullableDouble(rs, "nominal_batch_capacity_l"),
            rs.getInt("planned_fermenters"), rs.getDouble("piping_dead_volume_l"), rs.getString("logo_url"),
            rs.getLong("revision"), rs.getTimestamp("updated_at").toInstant());
    }

    private PlantAssetView mapAsset(ResultSet rs) throws SQLException {
        return new PlantAssetView(rs.getString("id"), rs.getString("site_id"), rs.getString("code"),
            rs.getString("asset_type"), rs.getString("name"), rs.getString("manufacturer"), rs.getString("model"),
            nullableDouble(rs, "capacity_l"), rs.getString("electrical_spec"),
            rs.getString("communication_protocol"), rs.getString("device_identifier"),
            rs.getString("firmware_profile"), rs.getString("status"), rs.getBoolean("controllable"),
            rs.getString("notes"), rs.getLong("revision"));
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
}

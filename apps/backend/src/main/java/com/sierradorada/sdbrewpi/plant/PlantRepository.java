package com.sierradorada.sdbrewpi.plant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
        return jdbc.query("SELECT * FROM plant_asset WHERE site_id = ? AND active = TRUE ORDER BY asset_type, code",
            (rs, row) -> mapAsset(rs), siteId);
    }

    public Optional<PlantAssetView> findAsset(String id) {
        return jdbc.query("SELECT * FROM plant_asset WHERE id = ?", (rs, row) -> mapAsset(rs), id)
            .stream().findFirst();
    }

    public boolean assetCodeExists(String siteId, String code, String exceptId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM plant_asset WHERE site_id = ? AND code = ? AND id <> ?",
            Integer.class, siteId, code, exceptId == null ? "" : exceptId);
        return count != null && count > 0;
    }

    public void insertAsset(String id, String siteId, String code, PlantAssetCreateRequest request, Instant now) {
        jdbc.update("INSERT INTO plant_asset (id, site_id, code, asset_type, name, manufacturer, model, capacity_l, "
            + "electrical_spec, communication_protocol, device_identifier, firmware_profile, status, controllable, "
            + "notes, revision, active, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, TRUE, ?)",
            id, siteId, code, request.assetType().trim().toUpperCase(Locale.ROOT), request.name().trim(), clean(request.manufacturer()),
            clean(request.model()), request.capacityL(), clean(request.electricalSpec()),
            clean(request.communicationProtocol()), clean(request.deviceIdentifier()), clean(request.firmwareProfile()),
            request.status().trim().toUpperCase(Locale.ROOT), request.controllable(), clean(request.notes()), Timestamp.from(now));
    }

    public int updateAsset(String id, String code, PlantAssetUpdateRequest request, Instant now) {
        return jdbc.update("UPDATE plant_asset SET code = ?, asset_type = ?, name = ?, manufacturer = ?, model = ?, "
            + "capacity_l = ?, electrical_spec = ?, communication_protocol = ?, device_identifier = ?, "
            + "firmware_profile = ?, status = ?, controllable = ?, notes = ?, revision = revision + 1, updated_at = ? "
            + "WHERE id = ? AND active = TRUE AND revision = ?",
            code, request.assetType().trim().toUpperCase(Locale.ROOT), request.name().trim(), clean(request.manufacturer()), clean(request.model()),
            request.capacityL(), clean(request.electricalSpec()), clean(request.communicationProtocol()),
            clean(request.deviceIdentifier()), clean(request.firmwareProfile()), request.status().trim().toUpperCase(Locale.ROOT),
            request.controllable(), clean(request.notes()), Timestamp.from(now), id, request.expectedRevision());
    }

    public int retireAsset(String id, long expectedRevision, Instant now) {
        return jdbc.update("UPDATE plant_asset SET active = FALSE, status = 'RETIRED', revision = revision + 1, "
            + "updated_at = ? WHERE id = ? AND active = TRUE AND revision = ?", Timestamp.from(now), id, expectedRevision);
    }

    public List<PlantWarehouseView> findWarehouses(String siteId) {
        return jdbc.query("SELECT * FROM plant_warehouse WHERE site_id = ? AND active = TRUE ORDER BY code",
            (rs, row) -> mapWarehouseRow(rs), siteId).stream().map(this::toWarehouseView).toList();
    }

    public Optional<PlantWarehouseView> findWarehouse(String id) {
        return jdbc.query("SELECT * FROM plant_warehouse WHERE id = ?", (rs, row) -> mapWarehouseRow(rs), id)
            .stream().findFirst().map(this::toWarehouseView);
    }

    public boolean warehouseCodeExists(String siteId, String code, String exceptId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM plant_warehouse WHERE site_id = ? AND code = ? AND id <> ?",
            Integer.class, siteId, code, exceptId == null ? "" : exceptId);
        return count != null && count > 0;
    }

    public void insertWarehouse(String id, String siteId, String code, PlantWarehouseCreateRequest request, Instant now) {
        jdbc.update("INSERT INTO plant_warehouse (id, site_id, code, name, purpose, temperature_controlled, notes, "
            + "revision, active, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, TRUE, ?)",
            id, siteId, code, request.name().trim(), request.purpose().trim().toUpperCase(Locale.ROOT),
            request.temperatureControlled(), clean(request.notes()), Timestamp.from(now));
        replaceWarehouseCategories(id, request.allowedCategories());
    }

    public int updateWarehouse(String id, String code, PlantWarehouseUpdateRequest request, Instant now) {
        int updated = jdbc.update("UPDATE plant_warehouse SET code = ?, name = ?, purpose = ?, temperature_controlled = ?, "
            + "notes = ?, revision = revision + 1, updated_at = ? WHERE id = ? AND active = TRUE AND revision = ?",
            code, request.name().trim(), request.purpose().trim().toUpperCase(Locale.ROOT), request.temperatureControlled(),
            clean(request.notes()), Timestamp.from(now), id, request.expectedRevision());
        if (updated > 0) replaceWarehouseCategories(id, request.allowedCategories());
        return updated;
    }

    public int retireWarehouse(String id, long expectedRevision, Instant now) {
        return jdbc.update("UPDATE plant_warehouse SET active = FALSE, revision = revision + 1, updated_at = ? "
            + "WHERE id = ? AND active = TRUE AND revision = ?", Timestamp.from(now), id, expectedRevision);
    }

    public void retireLocationsForWarehouse(String warehouseId, Instant now) {
        jdbc.update("UPDATE plant_storage_location SET active = FALSE, revision = revision + 1, updated_at = ? "
            + "WHERE warehouse_id = ? AND active = TRUE", Timestamp.from(now), warehouseId);
    }

    public Optional<PlantStorageLocationView> findLocation(String id) {
        return jdbc.query("SELECT * FROM plant_storage_location WHERE id = ?", (rs, row) -> mapLocation(rs), id)
            .stream().findFirst();
    }

    public boolean locationCodeExists(String warehouseId, String code, String exceptId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM plant_storage_location WHERE warehouse_id = ? AND code = ? AND id <> ?",
            Integer.class, warehouseId, code, exceptId == null ? "" : exceptId);
        return count != null && count > 0;
    }

    public void insertLocation(String id, String warehouseId, String code,
            PlantStorageLocationCreateRequest request, Instant now) {
        jdbc.update("INSERT INTO plant_storage_location (id, warehouse_id, code, name, location_type, notes, revision, "
            + "active, updated_at) VALUES (?, ?, ?, ?, ?, ?, 0, TRUE, ?)",
            id, warehouseId, code, request.name().trim(), request.locationType().trim().toUpperCase(Locale.ROOT),
            clean(request.notes()), Timestamp.from(now));
    }

    public int updateLocation(String id, String code, PlantStorageLocationUpdateRequest request, Instant now) {
        return jdbc.update("UPDATE plant_storage_location SET code = ?, name = ?, location_type = ?, notes = ?, "
            + "revision = revision + 1, updated_at = ? WHERE id = ? AND active = TRUE AND revision = ?",
            code, request.name().trim(), request.locationType().trim().toUpperCase(Locale.ROOT), clean(request.notes()),
            Timestamp.from(now), id, request.expectedRevision());
    }

    public int retireLocation(String id, long expectedRevision, Instant now) {
        return jdbc.update("UPDATE plant_storage_location SET active = FALSE, revision = revision + 1, updated_at = ? "
            + "WHERE id = ? AND active = TRUE AND revision = ?", Timestamp.from(now), id, expectedRevision);
    }

    public int updateProfile(String id, PlantProfileRequest request, Instant updatedAt) {
        return jdbc.update("UPDATE plant_site SET name = ?, company_name = ?, legal_name = ?, tax_id = ?, timezone = ?, "
            + "currency = ?, nominal_batch_capacity_l = ?, planned_fermenters = ?, piping_dead_volume_l = ?, "
            + "logo_url = ?, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ?",
            request.name().trim(), request.companyName().trim(), clean(request.legalName()), clean(request.taxId()),
            request.timezone().trim(), request.currency().trim(), request.nominalBatchCapacityL(), request.plannedFermenters(),
            request.pipingDeadVolumeL(), clean(request.logoUrl()), Timestamp.from(updatedAt), id, request.expectedRevision());
    }

    public void audit(String id, String actor, String target, String type, String payload, String reason) {
        jdbc.update("INSERT INTO command_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, Timestamp.from(Instant.now()),
            actor, target, type, payload, "ACCEPTED", reason);
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
            rs.getString("notes"), rs.getLong("revision"), rs.getBoolean("active"),
            rs.getTimestamp("updated_at").toInstant());
    }

    private WarehouseRow mapWarehouseRow(ResultSet rs) throws SQLException {
        return new WarehouseRow(rs.getString("id"), rs.getString("site_id"), rs.getString("code"),
            rs.getString("name"), rs.getString("purpose"), rs.getBoolean("temperature_controlled"),
            rs.getString("notes"), rs.getLong("revision"), rs.getBoolean("active"),
            rs.getTimestamp("updated_at").toInstant());
    }

    private PlantWarehouseView toWarehouseView(WarehouseRow row) {
        List<String> categories = jdbc.queryForList(
            "SELECT category_code FROM plant_warehouse_category WHERE warehouse_id = ? ORDER BY category_code",
            String.class, row.id());
        List<PlantStorageLocationView> locations = findLocations(row.id());
        return new PlantWarehouseView(row.id(), row.siteId(), row.code(), row.name(), row.purpose(),
            row.temperatureControlled(), row.notes(), categories, locations, row.revision(), row.active(), row.updatedAt());
    }

    private List<PlantStorageLocationView> findLocations(String warehouseId) {
        return jdbc.query("SELECT * FROM plant_storage_location WHERE warehouse_id = ? AND active = TRUE ORDER BY code",
            (rs, row) -> mapLocation(rs), warehouseId);
    }

    private PlantStorageLocationView mapLocation(ResultSet rs) throws SQLException {
        return new PlantStorageLocationView(rs.getString("id"), rs.getString("warehouse_id"), rs.getString("code"),
            rs.getString("name"), rs.getString("location_type"), rs.getString("notes"), rs.getLong("revision"),
            rs.getBoolean("active"), rs.getTimestamp("updated_at").toInstant());
    }

    private void replaceWarehouseCategories(String warehouseId, List<String> categories) {
        jdbc.update("DELETE FROM plant_warehouse_category WHERE warehouse_id = ?", warehouseId);
        categories.stream().map(value -> value.trim().toUpperCase(Locale.ROOT)).distinct()
            .forEach(category -> jdbc.update(
                "INSERT INTO plant_warehouse_category (warehouse_id, category_code) VALUES (?, ?)",
                warehouseId, category));
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private record WarehouseRow(String id, String siteId, String code, String name, String purpose,
            boolean temperatureControlled, String notes, long revision, boolean active, Instant updatedAt) {}
}

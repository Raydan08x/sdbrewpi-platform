package com.sierradorada.sdbrewpi.inventory;

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
class InventoryRepository {
    private final JdbcTemplate jdbc;
    InventoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    List<InventoryItemView> items(String siteId) {
        return jdbc.query("SELECT * FROM inventory_item WHERE site_id=? AND active=TRUE ORDER BY category, code", this::mapItem, siteId);
    }
    Optional<InventoryItemView> item(String id) {
        return jdbc.query("SELECT * FROM inventory_item WHERE id=?", this::mapItem, id).stream().findFirst();
    }
    boolean codeExists(String siteId, String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM inventory_item WHERE site_id=? AND code=?", Integer.class, siteId, code) > 0;
    }
    void insertItem(String id, String siteId, String code, InventoryItemRequest r, Instant now) {
        jdbc.update("INSERT INTO inventory_item VALUES (?,?,?,?,?,?,?,?,?,0,TRUE,?)", id, siteId, code,
            r.name().trim(), upper(r.category()), upper(r.baseUnit()), r.trackLots(), r.minimumStock(), clean(r.notes()), Timestamp.from(now));
    }
    List<InventoryLotView> lots(String siteId) {
        return jdbc.query("SELECT l.* FROM inventory_lot l JOIN inventory_item i ON i.id=l.item_id WHERE i.site_id=? AND i.active=TRUE ORDER BY l.created_at DESC", this::mapLot, siteId);
    }
    Optional<InventoryLotView> lot(String id) {
        return jdbc.query("SELECT * FROM inventory_lot WHERE id=?", this::mapLot, id).stream().findFirst();
    }
    void insertLot(String id, String itemId, InventoryLotRequest r, Instant now) {
        jdbc.update("INSERT INTO inventory_lot VALUES (?,?,?,?,?,?,?,?)", id, itemId, upper(r.internalCode()),
            clean(r.supplierLot()), r.expiryDate(), upper(r.qualityStatus()), clean(r.notes()), Timestamp.from(now));
    }
    boolean warehouseAllows(String warehouseId, String category) {
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM plant_warehouse w JOIN plant_warehouse_category c ON c.warehouse_id=w.id WHERE w.id=? AND w.active=TRUE AND c.category_code=?", Integer.class, warehouseId, category);
        return n!=null&&n>0;
    }
    boolean locationBelongs(String locationId, String warehouseId) {
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM plant_storage_location WHERE id=? AND warehouse_id=? AND active=TRUE", Integer.class, locationId, warehouseId);
        return n!=null&&n>0;
    }
    double balance(String itemId, String lotId, String warehouseId, String locationId) {
        Double value=jdbc.queryForObject("SELECT COALESCE(SUM(signed_quantity),0) FROM inventory_movement WHERE item_id=? AND COALESCE(lot_id,'')=? AND warehouse_id=? AND COALESCE(location_id,'')=?", Double.class,
            itemId, clean(lotId), warehouseId, clean(locationId));
        return value==null?0:value;
    }
    void insertMovement(String id, InventoryMovementRequest r, double signed, String actor, Instant now) {
        jdbc.update("INSERT INTO inventory_movement (id,item_id,lot_id,warehouse_id,location_id,movement_type,quantity,signed_quantity,reference,notes,actor,occurred_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", id, r.itemId(), blankNull(r.lotId()), r.warehouseId(), blankNull(r.locationId()),
            upper(r.movementType()), r.quantity(), signed, clean(r.reference()), clean(r.notes()), actor, Timestamp.from(now));
    }
    List<InventoryBalanceView> balances(String siteId) {
        return jdbc.query("""
            SELECT i.id item_id,i.code item_code,i.name item_name,i.category,i.base_unit,i.minimum_stock,
              w.id warehouse_id,w.code warehouse_code,w.name warehouse_name,loc.id location_id,loc.code location_code,
              l.id lot_id,l.internal_code lot_code,l.quality_status,SUM(m.signed_quantity) quantity
            FROM inventory_movement m JOIN inventory_item i ON i.id=m.item_id JOIN plant_warehouse w ON w.id=m.warehouse_id
              LEFT JOIN plant_storage_location loc ON loc.id=m.location_id LEFT JOIN inventory_lot l ON l.id=m.lot_id
            WHERE i.site_id=? AND i.active=TRUE GROUP BY i.id,i.code,i.name,i.category,i.base_unit,i.minimum_stock,
              w.id,w.code,w.name,loc.id,loc.code,l.id,l.internal_code,l.quality_status HAVING SUM(m.signed_quantity)<>0
            ORDER BY i.code,w.code,loc.code,l.internal_code""", this::mapBalance, siteId);
    }
    List<InventoryMovementView> movements(String siteId) {
        return jdbc.query("""
            SELECT m.*,i.code item_code,w.code warehouse_code,loc.code location_code,l.internal_code lot_code
            FROM inventory_movement m JOIN inventory_item i ON i.id=m.item_id JOIN plant_warehouse w ON w.id=m.warehouse_id
              LEFT JOIN plant_storage_location loc ON loc.id=m.location_id LEFT JOIN inventory_lot l ON l.id=m.lot_id
            WHERE i.site_id=? ORDER BY m.occurred_at DESC FETCH FIRST 30 ROWS ONLY""", this::mapMovement, siteId);
    }
    String primarySiteId() { return jdbc.queryForObject("SELECT id FROM plant_site ORDER BY code FETCH FIRST 1 ROW ONLY", String.class); }

    private InventoryItemView mapItem(ResultSet r,int n)throws SQLException{return new InventoryItemView(r.getString("id"),r.getString("site_id"),r.getString("code"),r.getString("name"),r.getString("category"),r.getString("base_unit"),r.getBoolean("track_lots"),r.getDouble("minimum_stock"),r.getString("notes"),r.getLong("revision"),r.getBoolean("active"),r.getTimestamp("updated_at").toInstant());}
    private InventoryLotView mapLot(ResultSet r,int n)throws SQLException{return new InventoryLotView(r.getString("id"),r.getString("item_id"),r.getString("internal_code"),r.getString("supplier_lot"),r.getObject("expiry_date",java.time.LocalDate.class),r.getString("quality_status"),r.getString("notes"),r.getTimestamp("created_at").toInstant());}
    private InventoryBalanceView mapBalance(ResultSet r,int n)throws SQLException{return new InventoryBalanceView(r.getString("item_id"),r.getString("item_code"),r.getString("item_name"),r.getString("category"),r.getString("base_unit"),r.getString("warehouse_id"),r.getString("warehouse_code"),r.getString("warehouse_name"),r.getString("location_id"),r.getString("location_code"),r.getString("lot_id"),r.getString("lot_code"),r.getString("quality_status"),r.getDouble("quantity"),r.getDouble("minimum_stock"));}
    private InventoryMovementView mapMovement(ResultSet r,int n)throws SQLException{return new InventoryMovementView(r.getString("id"),r.getString("item_id"),r.getString("item_code"),r.getString("lot_id"),r.getString("lot_code"),r.getString("warehouse_id"),r.getString("warehouse_code"),r.getString("location_id"),r.getString("location_code"),r.getString("movement_type"),r.getDouble("quantity"),r.getString("reference"),r.getString("notes"),r.getString("actor"),r.getTimestamp("occurred_at").toInstant());}
    private String upper(String s){return s.trim().toUpperCase(Locale.ROOT);} private String clean(String s){return s==null?"":s.trim();} private String blankNull(String s){return s==null||s.isBlank()?null:s.trim();}
}

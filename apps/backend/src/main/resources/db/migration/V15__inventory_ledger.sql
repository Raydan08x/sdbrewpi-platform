CREATE TABLE inventory_item (
  id VARCHAR(36) PRIMARY KEY,
  site_id VARCHAR(36) NOT NULL REFERENCES plant_site(id),
  code VARCHAR(50) NOT NULL,
  name VARCHAR(160) NOT NULL,
  category VARCHAR(40) NOT NULL,
  base_unit VARCHAR(20) NOT NULL,
  track_lots BOOLEAN NOT NULL,
  minimum_stock NUMERIC(14,3) NOT NULL,
  notes VARCHAR(1000) NOT NULL,
  revision BIGINT NOT NULL,
  active BOOLEAN NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (site_id, code)
);

CREATE TABLE inventory_lot (
  id VARCHAR(36) PRIMARY KEY,
  item_id VARCHAR(36) NOT NULL REFERENCES inventory_item(id),
  internal_code VARCHAR(80) NOT NULL,
  supplier_lot VARCHAR(100) NOT NULL,
  expiry_date DATE,
  quality_status VARCHAR(20) NOT NULL,
  notes VARCHAR(500) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (item_id, internal_code)
);

CREATE TABLE inventory_movement (
  id VARCHAR(36) PRIMARY KEY,
  item_id VARCHAR(36) NOT NULL REFERENCES inventory_item(id),
  lot_id VARCHAR(36) REFERENCES inventory_lot(id),
  warehouse_id VARCHAR(36) NOT NULL REFERENCES plant_warehouse(id),
  location_id VARCHAR(36) REFERENCES plant_storage_location(id),
  movement_type VARCHAR(30) NOT NULL,
  quantity NUMERIC(14,3) NOT NULL,
  signed_quantity NUMERIC(14,3) NOT NULL,
  reference VARCHAR(120) NOT NULL,
  notes VARCHAR(500) NOT NULL,
  actor VARCHAR(80) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_inventory_item_site ON inventory_item(site_id, category, code);
CREATE INDEX idx_inventory_lot_item ON inventory_lot(item_id, internal_code);
CREATE INDEX idx_inventory_movement_balance ON inventory_movement(item_id, warehouse_id, location_id, lot_id);
CREATE INDEX idx_inventory_movement_time ON inventory_movement(occurred_at);

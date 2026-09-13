CREATE TABLE plant_warehouse (
  id VARCHAR(36) PRIMARY KEY,
  site_id VARCHAR(36) NOT NULL REFERENCES plant_site(id),
  code VARCHAR(50) NOT NULL,
  name VARCHAR(120) NOT NULL,
  purpose VARCHAR(40) NOT NULL,
  temperature_controlled BOOLEAN NOT NULL,
  notes VARCHAR(1000) NOT NULL,
  revision BIGINT NOT NULL,
  active BOOLEAN NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (site_id, code)
);

CREATE TABLE plant_warehouse_category (
  warehouse_id VARCHAR(36) NOT NULL REFERENCES plant_warehouse(id) ON DELETE CASCADE,
  category_code VARCHAR(40) NOT NULL,
  PRIMARY KEY (warehouse_id, category_code)
);

CREATE TABLE plant_storage_location (
  id VARCHAR(36) PRIMARY KEY,
  warehouse_id VARCHAR(36) NOT NULL REFERENCES plant_warehouse(id),
  code VARCHAR(50) NOT NULL,
  name VARCHAR(120) NOT NULL,
  location_type VARCHAR(30) NOT NULL,
  notes VARCHAR(500) NOT NULL,
  revision BIGINT NOT NULL,
  active BOOLEAN NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (warehouse_id, code)
);

CREATE INDEX idx_plant_warehouse_site ON plant_warehouse (site_id, code);
CREATE INDEX idx_storage_location_warehouse ON plant_storage_location (warehouse_id, code);

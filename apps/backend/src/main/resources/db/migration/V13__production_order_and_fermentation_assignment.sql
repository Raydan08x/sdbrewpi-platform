ALTER TABLE production_batch ALTER COLUMN tank_id DROP NOT NULL;
ALTER TABLE production_batch ALTER COLUMN status SET DATA TYPE VARCHAR(32);

ALTER TABLE production_batch ADD COLUMN batch_record_opened_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE production_batch ADD COLUMN released_by VARCHAR(80);
ALTER TABLE production_batch ADD COLUMN tank_name_snapshot VARCHAR(80);
ALTER TABLE production_batch ADD COLUMN pill_id_snapshot VARCHAR(64);
ALTER TABLE production_batch ADD COLUMN pill_source_snapshot VARCHAR(32);
ALTER TABLE production_batch ADD COLUMN fermentation_volume_l NUMERIC(8,2);
ALTER TABLE production_batch ADD COLUMN fermentation_assigned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE production_batch ADD COLUMN fermentation_assigned_by VARCHAR(80);
ALTER TABLE production_batch ADD COLUMN batch_kind VARCHAR(20) NOT NULL DEFAULT 'COMMERCIAL';
ALTER TABLE production_batch ADD COLUMN product_code VARCHAR(4) NOT NULL DEFAULT 'CERV';
ALTER TABLE production_batch ADD COLUMN product_name VARCHAR(80) NOT NULL DEFAULT 'Cerveza';

UPDATE production_batch SET
  status = 'FERMENTING',
  batch_record_opened_at = started_at,
  released_by = 'migration',
  tank_name_snapshot = (SELECT name FROM fermentation_tank WHERE fermentation_tank.id = production_batch.tank_id),
  pill_id_snapshot = (SELECT pill_id FROM fermentation_tank WHERE fermentation_tank.id = production_batch.tank_id),
  pill_source_snapshot = (SELECT pill_source FROM fermentation_tank WHERE fermentation_tank.id = production_batch.tank_id),
  fermentation_volume_l = volume_l,
  fermentation_assigned_at = started_at,
  fermentation_assigned_by = 'migration'
WHERE status = 'ACTIVE';

UPDATE production_batch SET batch_kind = 'TEST' WHERE code LIKE 'SIM-%';

CREATE TABLE lot_number_counter (
  lot_period VARCHAR(4) NOT NULL,
  product_code VARCHAR(4) NOT NULL,
  batch_kind VARCHAR(20) NOT NULL,
  next_number INTEGER NOT NULL,
  PRIMARY KEY (lot_period, product_code, batch_kind)
);

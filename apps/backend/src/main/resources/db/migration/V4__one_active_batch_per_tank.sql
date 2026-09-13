ALTER TABLE production_batch ADD COLUMN active_slot INTEGER;

UPDATE production_batch SET active_slot = 1 WHERE status = 'ACTIVE';

ALTER TABLE production_batch
  ADD CONSTRAINT uq_production_batch_active_tank UNIQUE (tank_id, active_slot);

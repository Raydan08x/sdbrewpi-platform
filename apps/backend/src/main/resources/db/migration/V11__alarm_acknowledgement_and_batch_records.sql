ALTER TABLE fermentation_alarm ADD COLUMN acknowledged_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE fermentation_alarm ADD COLUMN acknowledged_by VARCHAR(80);
ALTER TABLE fermentation_alarm ADD COLUMN acknowledgment_note VARCHAR(300);
ALTER TABLE fermentation_alarm ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

ALTER TABLE batch_event ADD COLUMN material_name VARCHAR(120);
ALTER TABLE batch_event ADD COLUMN quantity NUMERIC(12,3);
ALTER TABLE batch_event ADD COLUMN unit VARCHAR(20);

CREATE INDEX idx_fermentation_alarm_opened_at
  ON fermentation_alarm (opened_at DESC);

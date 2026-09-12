ALTER TABLE fermentation_tank ADD COLUMN pill_received_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE fermentation_tank ADD COLUMN pill_battery_pct INTEGER;
ALTER TABLE fermentation_tank ADD COLUMN pill_rssi_dbm INTEGER;
ALTER TABLE fermentation_tank ADD COLUMN pill_source VARCHAR(32) NOT NULL DEFAULT 'SIMULATION';

UPDATE fermentation_tank SET pill_received_at = pill_captured_at;

CREATE TABLE pill_telemetry (
  message_id VARCHAR(64) PRIMARY KEY,
  pill_id VARCHAR(64) NOT NULL,
  source_topic VARCHAR(200) NOT NULL,
  source_format VARCHAR(32) NOT NULL,
  sequence_number BIGINT,
  captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
  received_at TIMESTAMP WITH TIME ZONE NOT NULL,
  temperature_c NUMERIC(5,2),
  gravity NUMERIC(6,4),
  battery_pct INTEGER,
  rssi_dbm INTEGER,
  quality VARCHAR(32) NOT NULL,
  historical BOOLEAN NOT NULL,
  retained BOOLEAN NOT NULL,
  raw_payload VARCHAR(2000) NOT NULL
);

CREATE INDEX idx_pill_telemetry_pill_captured ON pill_telemetry (pill_id, captured_at DESC);

CREATE TABLE telemetry_rejection (
  id VARCHAR(36) PRIMARY KEY,
  received_at TIMESTAMP WITH TIME ZONE NOT NULL,
  source_topic VARCHAR(200) NOT NULL,
  reason VARCHAR(300) NOT NULL
);

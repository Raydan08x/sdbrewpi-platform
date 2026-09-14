CREATE TABLE batch_stage_execution (
  id VARCHAR(36) PRIMARY KEY,
  batch_id VARCHAR(36) NOT NULL REFERENCES production_batch(id),
  stage_code VARCHAR(50) NOT NULL REFERENCES process_stage_definition(code),
  phase_snapshot VARCHAR(30) NOT NULL,
  step_order_snapshot INTEGER NOT NULL,
  name_snapshot VARCHAR(120) NOT NULL,
  description_snapshot VARCHAR(500) NOT NULL,
  optional_snapshot BOOLEAN NOT NULL,
  variant_snapshot VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE,
  started_by VARCHAR(80),
  completed_at TIMESTAMP WITH TIME ZONE,
  completed_by VARCHAR(80),
  notes VARCHAR(1000) NOT NULL DEFAULT '',
  measured_value NUMERIC(12,4),
  unit VARCHAR(20),
  revision BIGINT NOT NULL DEFAULT 0,
  UNIQUE (batch_id, stage_code)
);

CREATE INDEX idx_batch_stage_order ON batch_stage_execution (batch_id, step_order_snapshot);

ALTER TABLE production_batch ADD COLUMN profile_state VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';
ALTER TABLE production_batch ADD COLUMN step_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE production_batch ADD COLUMN step_elapsed_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE production_batch ADD COLUMN profile_completed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE batch_event (
  id VARCHAR(36) PRIMARY KEY,
  batch_id VARCHAR(36) NOT NULL REFERENCES production_batch(id),
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  step_order INTEGER,
  actor VARCHAR(80) NOT NULL,
  message VARCHAR(300) NOT NULL
);

CREATE INDEX idx_batch_event_batch_time ON batch_event (batch_id, occurred_at DESC);

CREATE TABLE fermentation_tank (
  id VARCHAR(32) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  setpoint_c NUMERIC(5,2) NOT NULL,
  product_temp_c NUMERIC(5,2) NOT NULL,
  gravity NUMERIC(6,4) NOT NULL,
  pill_id VARCHAR(64) NOT NULL,
  pill_quality VARCHAR(16) NOT NULL,
  pill_captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
  cooling_demand BOOLEAN NOT NULL,
  revision BIGINT NOT NULL
);

CREATE TABLE chiller_state (
  id INTEGER PRIMARY KEY,
  mode VARCHAR(16) NOT NULL,
  reservoir_temp_c NUMERIC(5,2),
  reservoir_quality VARCHAR(16) NOT NULL,
  pump_on BOOLEAN NOT NULL,
  compressor_request BOOLEAN NOT NULL,
  environment VARCHAR(16) NOT NULL,
  revision BIGINT NOT NULL
);

CREATE TABLE command_audit (
  id VARCHAR(36) PRIMARY KEY,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  actor VARCHAR(80) NOT NULL,
  target VARCHAR(64) NOT NULL,
  command_type VARCHAR(40) NOT NULL,
  payload VARCHAR(500) NOT NULL,
  result VARCHAR(24) NOT NULL,
  reason VARCHAR(300) NOT NULL
);

INSERT INTO fermentation_tank VALUES
('TANK-01', 'Fermentador 1', 'OFF', 18.00, 21.40, 1.0500, 'PILL-4D4C', 'GOOD', CURRENT_TIMESTAMP, FALSE, 0),
('TANK-02', 'Fermentador 2', 'OFF', 19.00, 20.80, 1.0440, 'PILL-3BA4', 'GOOD', CURRENT_TIMESTAMP, FALSE, 0);

INSERT INTO chiller_state VALUES
(1, 'SIMULATION', NULL, 'UNAVAILABLE', FALSE, FALSE, 'SIMULATION', 0);


CREATE TABLE recipe_version (
  id VARCHAR(36) PRIMARY KEY,
  recipe_code VARCHAR(40) NOT NULL,
  name VARCHAR(120) NOT NULL,
  version_number INTEGER NOT NULL,
  original_gravity NUMERIC(6,4) NOT NULL,
  target_final_gravity NUMERIC(6,4) NOT NULL,
  default_volume_l NUMERIC(8,2) NOT NULL,
  notes VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (recipe_code, version_number)
);

CREATE TABLE fermentation_profile_step (
  id VARCHAR(36) PRIMARY KEY,
  recipe_version_id VARCHAR(36) NOT NULL REFERENCES recipe_version(id),
  step_order INTEGER NOT NULL,
  name VARCHAR(80) NOT NULL,
  target_temp_c NUMERIC(5,2) NOT NULL,
  duration_hours INTEGER NOT NULL,
  UNIQUE (recipe_version_id, step_order)
);

CREATE TABLE production_batch (
  id VARCHAR(36) PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  recipe_version_id VARCHAR(36) NOT NULL REFERENCES recipe_version(id),
  recipe_code_snapshot VARCHAR(40) NOT NULL,
  recipe_name_snapshot VARCHAR(120) NOT NULL,
  recipe_version_snapshot INTEGER NOT NULL,
  tank_id VARCHAR(32) NOT NULL REFERENCES fermentation_tank(id),
  volume_l NUMERIC(8,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  current_step INTEGER NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expected_complete_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,
  revision BIGINT NOT NULL
);

CREATE INDEX idx_production_batch_tank_status ON production_batch (tank_id, status);

INSERT INTO recipe_version VALUES
('00000000-0000-0000-0000-000000000101', 'DEMO-PALE-ALE', 'Perfil de prueba Pale Ale', 1, 1.0500, 1.0120, 200.00, 'Datos de demostración para validar el flujo en simulación.', CURRENT_TIMESTAMP);

INSERT INTO fermentation_profile_step VALUES
('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 1, 'Fermentación primaria', 18.00, 120),
('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000101', 2, 'Descanso', 20.00, 48),
('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000101', 3, 'Maduración', 4.00, 72);

INSERT INTO production_batch VALUES
('00000000-0000-0000-0000-000000000301', 'SIM-LOTE-001', '00000000-0000-0000-0000-000000000101', 'DEMO-PALE-ALE', 'Perfil de prueba Pale Ale', 1, 'TANK-01', 200.00, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10' DAY, NULL, 0);

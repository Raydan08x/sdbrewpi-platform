CREATE TABLE plant_site (
  id VARCHAR(36) PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  company_name VARCHAR(160) NOT NULL,
  legal_name VARCHAR(180) NOT NULL,
  tax_id VARCHAR(40) NOT NULL,
  timezone VARCHAR(80) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  nominal_batch_capacity_l NUMERIC(10,2),
  planned_fermenters INTEGER NOT NULL,
  piping_dead_volume_l NUMERIC(8,2) NOT NULL,
  logo_url VARCHAR(500) NOT NULL,
  revision BIGINT NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE plant_asset (
  id VARCHAR(36) PRIMARY KEY,
  site_id VARCHAR(36) NOT NULL REFERENCES plant_site(id),
  code VARCHAR(50) NOT NULL,
  asset_type VARCHAR(40) NOT NULL,
  name VARCHAR(120) NOT NULL,
  manufacturer VARCHAR(100) NOT NULL,
  model VARCHAR(140) NOT NULL,
  capacity_l NUMERIC(10,2),
  electrical_spec VARCHAR(300) NOT NULL,
  communication_protocol VARCHAR(160) NOT NULL,
  device_identifier VARCHAR(160) NOT NULL,
  firmware_profile VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL,
  controllable BOOLEAN NOT NULL,
  notes VARCHAR(1000) NOT NULL,
  revision BIGINT NOT NULL,
  UNIQUE (site_id, code)
);

CREATE INDEX idx_plant_asset_site_type ON plant_asset (site_id, asset_type, code);

INSERT INTO plant_site VALUES
('00000000-0000-0000-0000-000000000401', 'MAIN', 'Planta principal', 'Mi cervecería', '', '', 'America/Bogota', 'COP', NULL, 2, 0.00, '', 0, CURRENT_TIMESTAMP);

INSERT INTO plant_asset VALUES
('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000401', 'CHILLER-01', 'CHILLER', 'Chiller de fermentación', 'Fabricación local', 'Modelo pendiente', 500.00, 'Motor 3 HP, 220 V trifásico', 'Mando mediante variador y contactor; interfaz pendiente', '', '', 'DOCUMENTED', TRUE, 'Depósito de 500 L confirmado por el propietario.', 0),
('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-000000000401', 'PUMP-COLD-01', 'PUMP', 'Bomba de circulación', 'Pedrollo', 'PKm 60', NULL, '0,5 HP; 110 V monofásico; 60 Hz; 5,5 A', 'Salida discreta mediante contactor o relé intermedio', '', '', 'DOCUMENTED', TRUE, 'Caudal nominal 5–40 L/min; no es accionada por el VFD.', 0),
('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-000000000401', 'VFD-CHILLER-01', 'VFD', 'Variador del chiller', 'Power Electric', '3HP/220V PD2000 3 32', NULL, 'Entrada 1PH 220–240 V 23 A; salida 3PH 0–240 V 9,6 A', 'Terminales de control visibles; protocolo y mapa pendientes', '', '', 'DOCUMENTED', TRUE, 'No habilitar hasta obtener manual y probar entradas aisladas.', 0),
('00000000-0000-0000-0000-000000000504', '00000000-0000-0000-0000-000000000401', 'TRANSFORMER-01', 'POWER', 'Transformador elevador', 'PEC', 'Elevador 3 kVA 110–220 V', NULL, '3 kVA; 110–220 V', 'Sin comunicación', '', '', 'DOCUMENTED', FALSE, 'Conexión exacta pendiente de diagrama unifilar.', 0),
('00000000-0000-0000-0000-000000000505', '00000000-0000-0000-0000-000000000401', 'TANK-01', 'FERMENTER', 'Fermentador 1', 'Pendiente', 'Pendiente', NULL, '', 'Pill y circuito de frío', 'PILL-4D4C', '', 'NEEDS_DATA', TRUE, 'Capacidad y volumen muerto pendientes.', 0),
('00000000-0000-0000-0000-000000000506', '00000000-0000-0000-0000-000000000401', 'TANK-02', 'FERMENTER', 'Fermentador 2', 'Pendiente', 'Pendiente', NULL, '', 'Pill y circuito de frío', 'PILL-3BA4', '', 'NEEDS_DATA', TRUE, 'Capacidad y volumen muerto pendientes.', 0),
('00000000-0000-0000-0000-000000000507', '00000000-0000-0000-0000-000000000401', 'PLC-HMI-01', 'CONTROLLER', 'PLC/HMI principal', 'Waveshare', 'ESP32-S3 Touch LCD 4.3B', NULL, 'Alimentación pendiente de confirmar', 'RS485, CAN e I2C disponibles', '', 'waveshare-esp32s3-43b', 'AVAILABLE', TRUE, 'Controlador de campo candidato; salidas físicas bloqueadas.', 0),
('00000000-0000-0000-0000-000000000508', '00000000-0000-0000-0000-000000000401', 'HMI-AUX-01', 'HMI', 'Pantalla auxiliar', 'Waveshare', 'ESP32-S3 Touch LCD 2', NULL, 'Alimentación pendiente de confirmar', 'Wi-Fi/BLE; buses según revisión física', '', 'waveshare-esp32s3-lcd2', 'AVAILABLE', FALSE, 'Pantalla auxiliar candidata.', 0),
('00000000-0000-0000-0000-000000000509', '00000000-0000-0000-0000-000000000401', 'RELAY-01', 'RELAY_MODULE', 'Módulo de seis relés', 'Waveshare', 'Referencia 1005006982002502', NULL, 'Tensión y capacidad de contactos pendientes', 'Interfaz pendiente de confirmar', '', '', 'NEEDS_DATA', TRUE, 'Solo gobernará circuitos de mando; no alimentará motores directamente.', 0),
('00000000-0000-0000-0000-000000000510', '00000000-0000-0000-0000-000000000401', 'REPEATER-01', 'SENSOR_GATEWAY', 'Repetidor Pill 1', 'ESP32-C3', 'Firmware heredado', NULL, '', 'BLE a MQTT', '', 'pill-repeater-c3', 'OFFLINE', FALSE, 'Pendiente de conexión.', 0),
('00000000-0000-0000-0000-000000000511', '00000000-0000-0000-0000-000000000401', 'REPEATER-02', 'SENSOR_GATEWAY', 'Repetidor Pill 2', 'ESP32-C3', 'Firmware heredado', NULL, '', 'BLE a MQTT', '', 'pill-repeater-c3', 'OFFLINE', FALSE, 'Pendiente de conexión.', 0),
('00000000-0000-0000-0000-000000000512', '00000000-0000-0000-0000-000000000401', 'PILL-01', 'SENSOR', 'Pill fermentador 1', 'RAPT', 'Pill', NULL, 'Batería descargada', 'BLE mediante repetidor', 'PILL-4D4C', '', 'WAITING_BATTERY', FALSE, 'No disponible temporalmente.', 0),
('00000000-0000-0000-0000-000000000513', '00000000-0000-0000-0000-000000000401', 'PILL-02', 'SENSOR', 'Pill fermentador 2', 'RAPT', 'Pill', NULL, 'Batería descargada', 'BLE mediante repetidor', 'PILL-3BA4', '', 'WAITING_BATTERY', FALSE, 'No disponible temporalmente.', 0);

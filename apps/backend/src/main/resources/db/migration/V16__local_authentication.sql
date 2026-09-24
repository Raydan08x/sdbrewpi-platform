CREATE TABLE app_user (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(160) NOT NULL UNIQUE,
  display_name VARCHAR(160) NOT NULL,
  role VARCHAR(30) NOT NULL,
  password_salt VARCHAR(80) NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE auth_session (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL REFERENCES app_user(id),
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_auth_session_token ON auth_session(token_hash, expires_at);

-- Contraseñas derivadas con PBKDF2-HMAC-SHA256 (120.000 iteraciones); nunca se almacena texto plano.
INSERT INTO app_user VALUES
('00000000-0000-0000-0000-000000000801','cmadero08x@gmail.com','Carlos Madero','ADMIN','wIqkiMDZ5SfFf9C27Q+dvQ==','kegumXVPVumVUQd3XCDJAlLhL6vVcxd7wwVDy6gO0kM=',TRUE,CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000802','admin','Administrador local','ADMIN','Z+rEWZQjQFu7DHyYEFSE7A==','1RBVcPNyLr97PW6DTM7A5Alw5eJp3r8AXsB6yH2XRsc=',TRUE,CURRENT_TIMESTAMP);

# QA

## Backend

```powershell
Set-Location apps/backend
.\mvnw.cmd test
```

## Frontend

```powershell
Set-Location apps/web
npm ci
npm run lint
npm run build
npm run qa:responsive
```

## Integración

```powershell
docker compose up --build -d
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/api/v1/fermentation/overview
docker compose down
```

## Línea base verificada — 2026-09-12

- Backend: 27 pruebas aprobadas, incluidas histéresis, validación, concurrencia, CORS, parsing MQTT, ingestión segura, recetas versionadas, lotes y configuración de planta con revisión optimista.
- Frontend: lint, compilación TypeScript/Vite y auditoría npm sin vulnerabilidades.
- Responsive: viewport móvil real de 390 px sin desbordamiento horizontal ni elementos recortados.
- API local: salud, lectura de dos tanques, setpoint/modo, demanda de frío, rechazo inválido y conflicto 409.
- UI: revisión visual en escritorio y móvil; orden enviada desde la WebApp y confirmada por el backend.
- Producción: perfil de demostración visible, snapshot de receta conservado y un solo lote activo permitido por fermentador.
- Mi Planta: sede y 13 activos registrados; identidad, capacidad, cantidad de fermentadores y remanente editables; URL de logo restringida a HTTP/HTTPS; instalación de firmware visible pero deshabilitada.
- Telemetría: formatos JSON, escalares, canónico e historial offline cubiertos; muestras sin tiempo verificable excluidas del control.
- Modo degradado: con MQTT habilitado y broker inalcanzable, la API permanece saludable, reporta la desconexión y no activa simulación ni hardware.
- Compose: configuración válida; construcción y ejecución completa pendientes de iniciar Docker Desktop.

Durante el QA se corrigieron el corte de controles en anchos intermedios y el rechazo CORS de comandos cuando la WebApp usa `127.0.0.1`.

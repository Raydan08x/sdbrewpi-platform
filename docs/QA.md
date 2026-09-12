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
```

## Integración

```powershell
docker compose up --build -d
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/api/v1/fermentation/overview
docker compose down
```

## Línea base verificada — 2026-09-12

- Backend: 8 pruebas aprobadas, incluidas histéresis, validación, concurrencia y CORS local.
- Frontend: lint, compilación TypeScript/Vite y auditoría npm sin vulnerabilidades.
- API local: salud, lectura de dos tanques, setpoint/modo, demanda de frío, rechazo inválido y conflicto 409.
- UI: revisión visual en escritorio y móvil; orden enviada desde la WebApp y confirmada por el backend.
- Compose: configuración válida; construcción y ejecución completa pendientes de iniciar Docker Desktop.

Durante el QA se corrigieron el corte de controles en anchos intermedios y el rechazo CORS de comandos cuando la WebApp usa `127.0.0.1`.

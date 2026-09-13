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

## Línea base verificada — 2026-09-13

- Backend: 44 pruebas aprobadas, incluidas histéresis, validación, concurrencia, CORS, parsing MQTT y PLC demo, ingestión segura, curvas, reconocimiento de alarmas, rampas térmicas, intervención manual segura, ejecución de perfiles, bitácora estructurada, recetas versionadas, lotes, catálogo de producción y gestión de equipos, bodegas y ubicaciones con revisión optimista.
- Frontend: lint, compilación TypeScript/Vite y auditoría npm sin vulnerabilidades.
- Responsive: viewport móvil real de 390 px sin desbordamiento horizontal ni elementos recortados.
- API local: salud, lectura de dos tanques, setpoint/modo, demanda de frío, rechazo inválido y conflicto 409.
- UI: revisión visual en escritorio y móvil; orden enviada desde la WebApp y confirmada por el backend.
- Producción: perfil de demostración visible, snapshot de receta conservado y un solo lote activo permitido por fermentador.
- Mi Planta: sede y 13 activos registrados; identidad, capacidad, cantidad de fermentadores y remanente editables; URL de logo restringida a HTTP/HTTPS; instalación de firmware visible pero deshabilitada.
- Equipos: alta, actualización, rechazo de tipo inválido y retiro lógico cubiertos; los activos retirados no aparecen en el inventario operativo.
- Bodegas: estado vacío, alta, categorías permitidas, ubicación interna, edición y retiro lógico verificados desde API y WebApp.
- Producción: 43 etapas visibles y agrupadas; Fermentación se abre como estación interna y conserva el control existente.
- Telemetría: formatos JSON, escalares, canónico e historial offline cubiertos; muestras sin tiempo verificable excluidas del control.
- PLC demo: COM5 leído físicamente sin escrituras; cuatro muestras aceptadas, variantes `Delta` y `Chiller/Bomba` reconocidas y pérdida posterior señalada como alarma.
- Fermentación: curvas responsive, alarmas persistentes con reconocimiento/historial, bitácora de lote con adiciones, rampas térmicas e inicio/pausa/reanudación/avance de perfiles verificados; un perfil en marcha bloquea comandos manuales, la pausa transfiere el tanque a `MANUAL`, la reanudación recupera `AUTO` y el cierre de lote deja el tanque en `OFF`.
- Modo degradado: con MQTT habilitado y broker inalcanzable, la API permanece saludable, reporta la desconexión y no activa simulación ni hardware.
- Compose: configuración válida; construcción y ejecución completa pendientes de iniciar Docker Desktop.

Durante el QA se corrigieron el corte de controles en anchos intermedios y el rechazo CORS de comandos cuando la WebApp usa `127.0.0.1`.

## Avance WebApp - gestión de lotes de fermentación

- La pantalla permite asignar código, versión de receta, tanque libre y volumen mediante la API existente.
- Crear el lote no inicia el perfil. El cierre exige escribir el código y utiliza la revisión del lote seleccionado; el backend deja el tanque en OFF.
- QA: 44 pruebas backend aprobadas, lint y build correctos; formularios y confirmación revisados a 390/1440 px. No se ejecutó un alta/cierre desde el navegador sobre los lotes persistentes del propietario; los endpoints se verificaron en las pruebas de integración H2.
- Sin modificaciones ni comandos al firmware, puertos COM o carpeta LVGL.

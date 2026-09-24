# Que cambio

Repositorio público `Raydan08x/sdbrewpi-platform`, con despliegue automático de una vista estática de solo lectura en GitHub Pages. El build local normal conserva su API.

# Archivos tocados

`.github/workflows/pages.yml`, `apps/web/src/api.ts`, `apps/web/src/App.tsx`, `apps/web/scripts/qa-responsive.mjs`, `apps/web/public/preview.json`, `README.md`, `docs/GITHUB_DEPLOYMENT.md` y este documento.

# Pruebas hechas

- Maven: 48 pruebas, cero fallos/errores; paquete Spring Boot generado.
- npm ci: cero vulnerabilidades; lint y build TypeScript/Vite correctos.
- QA responsive: ocho vistas a 390/1440 px sin desbordamientos; el intento de guardar en la vista pública fue rechazado con un mensaje explícito.
- Gitleaks: historial previo de 14 commits sin credenciales detectadas; snapshot de demostración revisado sin secretos detectados.
- Snapshot generado desde H2 en memoria aislada, con MQTT/PLC/hardware deshabilitados, un lote de prueba y datos de simulación. No se exportó la base de datos del propietario.

# Riesgos pendientes

Pages no ejecuta Java ni PostgreSQL. No es un despliegue operativo de la planta ni ofrece persistencia de cambios. No se probó hardware. La recuperación del repetidor discutida en la tarea de firmware sigue siendo un trabajo separado.

# Siguiente paso

Para operación remota completa, definir servidor para Spring Boot/PostgreSQL y añadir autenticación/autorización y HTTPS antes de exponer la API. Ver `docs/GITHUB_DEPLOYMENT.md`.

# Publicación en GitHub

Repositorio: https://github.com/Raydan08x/sdbrewpi-platform

Vista pública: https://raydan08x.github.io/sdbrewpi-platform/

GitHub Pages aloja una demostración estática de React, de solo lectura. No ejecuta Spring Boot ni PostgreSQL y no conecta con PLC, MQTT, relés o dispositivos USB. Los datos de `apps/web/public/preview.json` proceden de una instancia aislada con H2 en memoria y simulación; no son una exportación de la base local del propietario.

El workflow `.github/workflows/pages.yml` instala las dependencias bloqueadas, ejecuta lint, compila con `VITE_STATIC_PREVIEW=true` y publica en Pages tras cada push a `main`. En ese modo, las lecturas usan el snapshot y las operaciones de escritura son rechazadas en el cliente. No hay API pública de control detrás de Pages.

El uso local mantiene su comportamiento original: `npm run dev` se conecta a la API mediante el proxy Vite. Para operar remotamente la aplicación completa hace falta desplegar el backend y PostgreSQL en un servidor, añadir autenticación/autorización y configurar HTTPS. No exponer directamente la API local actual a Internet.

Para verificar el mismo build localmente:

```powershell
cd apps/web
$env:VITE_STATIC_PREVIEW='true'
npm ci
npm run lint
npm run build -- --base=/sdbrewpi-platform/
npx vite preview --host 127.0.0.1 --port 14173 --base=/sdbrewpi-platform/
```

Abrir http://127.0.0.1:14173/sdbrewpi-platform/.

Para volver a compilar una versión operativa local desde esa terminal: `Remove-Item Env:VITE_STATIC_PREVIEW` antes de compilar.

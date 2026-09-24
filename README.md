# SDBrewPi Platform

Plataforma modular para operar y gestionar una cervecería artesanal. El primer incremento cubre la fermentación de dos tanques con un chiller compartido en modo simulado.

**[Demostración pública de solo lectura](https://raydan08x.github.io/sdbrewpi-platform/)** · [Repositorio](https://github.com/Raydan08x/sdbrewpi-platform). La demostración usa datos de ejemplo congelados; no conecta con la planta ni guarda cambios. Consulta [el despliegue en GitHub](docs/GITHUB_DEPLOYMENT.md) para distinguirla de la instalación completa con Spring Boot.

## Inicio rápido

Requisito: Docker Desktop en ejecución.

```powershell
docker compose up --build
```

Abrir:

- WebApp: <http://localhost:8080>
- API: <http://localhost:8081/api/v1/fermentation/overview>
- Salud: <http://localhost:8081/actuator/health>
- Estado MQTT: <http://localhost:8081/api/v1/telemetry/status>
- Producción, recetas y lotes activos: <http://localhost:8081/api/v1/production/overview>
- Mi Planta, equipos y preparación de dispositivos: <http://localhost:8081/api/v1/plant/overview>

Detener con `docker compose down`. Los datos quedan en el volumen `postgres-data`.

Si Docker Desktop todavía no está iniciado, el backend puede ejecutarse de forma independiente con la base H2 local:

```powershell
Set-Location apps/backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

En otra terminal:

```powershell
Set-Location apps/web
npm ci
npm run dev -- --host 127.0.0.1
```

## Estado operativo

`SIMULATION` está habilitado por defecto. La aplicación no acciona GPIO, relés ni Modbus. El modo `LIVE_READ_ONLY` permite consumir las Pills sin habilitar ninguna salida física.

La demo de la pantalla/PLC puede leerse por USB serie con `PLC_DEMO_ENABLED=true`, `PLC_DEMO_PORT=COM5` y `SIMULATION_ENABLED=false`. El conector acepta las variantes observadas con estados `Chiller/Bomba` o con `Delta`, guarda curvas y alarmas, y no contiene operaciones de escritura. Se mantiene deshabilitado por defecto para no bloquear el puerto mientras se desarrolla el firmware.

Para la prueba MQTT, configura las variables en una terminal o en un archivo `.env` local que Git ignora:

```powershell
$env:MQTT_ENABLED="true"
$env:SIMULATION_ENABLED="false"
$env:MQTT_BROKER_URI="tcp://192.168.1.15:1883"
$env:MQTT_USERNAME="usuario-local"
$env:MQTT_PASSWORD="contraseña-local"
```

El estado del enlace, los mensajes aceptados y rechazados aparecen en la WebApp. Los mensajes históricos y los valores retenidos sin timestamp se guardan, pero no actualizan el estado utilizado por el control.

La instalación inicial incluye un perfil y un lote identificados como `DEMO`/`SIM` para probar la asociación receta–lote–fermentador. No representan producción real y pueden sustituirse desde la API cuando se definan las recetas de la cervecería.

En Producción → Fermentación, un lote puede iniciar, pausar y reanudar su perfil térmico. El motor avanza las fases por tiempo, aplica rampas configurables en °C/h, cambia el setpoint registrado y conserva eventos de trazabilidad. Mientras el perfil se ejecuta, bloquea órdenes manuales en conflicto; al pausarlo habilita la intervención y al reanudar recupera `AUTO` y el punto correcto de la rampa. En el estado actual esas acciones no se transmiten al hardware.

## Alcance del producto

La arquitectura es un monolito modular. El orden funcional es Mi Planta, Inventarios, Recetas, Planeación MRP, Producción, Compras, Ventas y CRM, y Finanzas. Fermentación pertenece a Producción. La contabilidad fiscal se conectará mediante adaptadores a un servicio externo.

El alcance y los límites de cada módulo están en [docs/PRODUCT-MAP.md](docs/PRODUCT-MAP.md).

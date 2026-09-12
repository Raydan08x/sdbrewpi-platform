# SDBrewPi Platform

Plataforma modular para operar y gestionar una cervecería artesanal. El primer incremento cubre la fermentación de dos tanques con un chiller compartido en modo simulado.

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

Para la prueba MQTT, configura las variables en una terminal o en un archivo `.env` local que Git ignora:

```powershell
$env:MQTT_ENABLED="true"
$env:SIMULATION_ENABLED="false"
$env:MQTT_BROKER_URI="tcp://192.168.1.15:1883"
$env:MQTT_USERNAME="usuario-local"
$env:MQTT_PASSWORD="contraseña-local"
```

El estado del enlace, los mensajes aceptados y rechazados aparecen en la WebApp. Los mensajes históricos y los valores retenidos sin timestamp se guardan, pero no actualizan el estado utilizado por el control.

## Alcance del producto

La arquitectura es un monolito modular. Fermentación es el primer módulo; recetas, producción, inventario, compras, ventas, CRM y contabilidad comparten identidad y datos maestros, pero conservan límites de dominio.

# Backlog

## M0 — Base ejecutable en PC

- [x] Estructura modular y documentación de continuidad.
- [x] PostgreSQL y migraciones.
- [x] Dos tanques y chiller en simulación.
- [x] Setpoints y modos validados.
- [x] Bitácora de comandos.
- [x] Dashboard adaptable.
- [x] Adaptador MQTT de solo lectura para los repetidores actuales.

## M1 — Telemetría real, todavía sin actuación

- [x] Contrato MQTT versionado con `capturedAt`, `receivedAt`, `sequence`, `quality` e identidad estable.
- [x] Adaptador de compatibilidad para tópicos `rapt/pill/...` y `sierra/rapt/...`.
- [x] Asociación Pill-tanque e historial de telemetría.
- [x] Estado `STALE` para lecturas atrasadas y estado de conexión MQTT.
- [ ] Actualizar los repetidores para publicar una muestra atómica con `capturedAt`, `messageId` y `sequence`.
- [ ] Asociación con lote activo.
- [ ] Alarmas persistentes de batería y pérdida de repetidor.
- [ ] Curvas de temperatura y gravedad.

## M2 — Perfiles y trazabilidad

- Recetas versionadas y snapshot por lote.
- Perfil de fermentación por fases, tiempo y rampas.
- Batch record, eventos y adiciones.
- Simulación determinista de fallos y recuperación.

## M3 — Banco de hardware

- Identificar módulo de relés, variador, bomba y sensores.
- Confirmar modelos, diagramas eléctricos, niveles de señal y protocolos del PLC, relés y variador.
- Firmware de HMI principal y auxiliar.
- Firmware/controlador con watchdog y estado seguro.
- Pruebas con salidas de baja tensión, sin cargas de potencia.
- MQTT de comandos con caducidad, ACK/NACK e idempotencia.

## M4 — Piloto físico

- Sensor de depósito y sensores de respaldo.
- Interlocks de flujo, nivel y fallo de variador.
- Puesta en marcha: lectura, sombra, manual asistido y automático.
- Pruebas de pérdida de red, broker, servidor, sensor y energía.

## Expansión empresarial

- Formulación y recetas.
- Producción y planificación MRP.
- Inventarios, compras y proveedores.
- Ventas, clientes y CRM.
- Cuentas de cobro, tesorería y contabilidad.
- Mantenimiento, calidad, costos y reportes.

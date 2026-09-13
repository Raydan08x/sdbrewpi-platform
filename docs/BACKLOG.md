# Backlog

## M0 — Base ejecutable en PC

- [x] Estructura modular y documentación de continuidad.
- [x] PostgreSQL y migraciones.
- [x] Dos tanques y chiller en simulación.
- [x] Setpoints y modos validados.
- [x] Bitácora de comandos.
- [x] Dashboard adaptable.
- [x] Adaptador MQTT de solo lectura para los repetidores actuales.
- [x] Mi Planta como módulo inicial y registro de la sede/equipos conocidos.
- [x] Configuración editable de identidad, capacidad, fermentadores previstos y remanente de tubería.

## Orden del producto

- [x] Mi Planta.
- [ ] Inventarios.
- [ ] Recetas.
- [ ] Planeación MRP.
- [x] Producción, con Fermentación como proceso inicial.
- [ ] Compras.
- [ ] Ventas y CRM.
- [ ] Finanzas e integración contable externa.

## M0.5 — Completar Mi Planta

- [ ] Editor de equipos, sensores, capacidades y conexiones.
- [ ] Modelo de circuitos, tramos y remanentes de tubería.
- [ ] Bodegas y ubicaciones físicas de la sede.
- [ ] Agente local para descubrir controladores compatibles por USB.
- [ ] Catálogo firmado de firmware por familia y revisión de hardware.
- [ ] Flujo de respaldo, instalación, autoprueba y registro sin habilitar salidas.

## M1 — Telemetría real, todavía sin actuación

- [x] Contrato MQTT versionado con `capturedAt`, `receivedAt`, `sequence`, `quality` e identidad estable.
- [x] Adaptador de compatibilidad para tópicos `rapt/pill/...` y `sierra/rapt/...`.
- [x] Asociación Pill-tanque e historial de telemetría.
- [x] Estado `STALE` para lecturas atrasadas y estado de conexión MQTT.
- [ ] Actualizar los repetidores para publicar una muestra atómica con `capturedAt`, `messageId` y `sequence`.
- [x] Asociación segura con lote activo y bloqueo de doble asignación por fermentador.
- [ ] Alarmas persistentes de batería y pérdida de repetidor.
- [ ] Curvas de temperatura y gravedad.

## M2 — Perfiles y trazabilidad

- [x] Base de recetas versionadas y snapshot de identidad por lote.
- [x] Perfil básico por fases, temperatura y duración.
- [ ] Editor Web de recetas y nuevas versiones.
- [ ] Ejecución temporal de fases, rampas y cambios de setpoint.
- Batch record, eventos y adiciones.
- Simulación determinista de fallos y recuperación.

## M3 — Banco de hardware

- [x] Registrar placas y separación eléctrica del chiller, variador, transformador y bomba.
- [ ] Identificar completamente PLC, módulo de relés y sensores.
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

- Catálogo de artículos y bodegas para materias primas, empaque, insumos, producto en proceso, producto terminado y repuestos.
- Formulación y recetas ligadas al catálogo.
- Planeación MRP basada en inventario, recetas, demanda y capacidad.
- Producción, consumos, mermas, calidad, mantenimiento y costos.
- Compras y proveedores.
- Ventas, clientes y CRM.
- Cuentas por cobrar, tesorería y adaptadores para contabilidad externa.

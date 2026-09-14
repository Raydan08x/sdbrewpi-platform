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

- [x] Alta, edición y retiro lógico de equipos, sensores, capacidades y conexiones.
- [ ] Modelo de circuitos, tramos y remanentes de tubería.
- [x] Bodegas, categorías permitidas y ubicaciones físicas de la sede.
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
- [x] Alarmas persistentes de batería, pérdida de fuente y banderas de la demo PLC.
- [x] Curvas unificadas de temperatura y gravedad para simulación, MQTT y demo PLC.

## M2 — Perfiles y trazabilidad

- [x] Base de recetas versionadas y snapshot de identidad por lote.
- [x] Perfil básico por fases, temperatura y duración.
- [ ] Editor Web de recetas y nuevas versiones.
- [x] Ejecución temporal de fases, pausa/reanudación y cambios de setpoint.
- [x] Rampas térmicas configurables entre fases y validación de que alcancen el objetivo.
- [x] Propiedad exclusiva del control durante la ejecución y pausa segura para intervención manual.
- [x] Eventos básicos de perfil en el batch record.
- [x] Bitácora de adiciones estructuradas, observaciones, mediciones manuales y eventos operativos.
- [x] Lote maestro automático por producto, período `AAMM`, clase y secuencia (`CERV-2609-L001`).
- [x] Apertura del batch record al liberar la orden y snapshot de fermentador, Pill, fuente y volumen transferido.
- [ ] Firma de operador ligada a autenticación y roles.
- [x] Reconocimiento e historial de alarmas con operador, hora y nota.
- Simulación determinista de fallos y recuperación.

## M2.5 — Ejecución integral de producción

- [x] Catálogo base de 43 etapas desde pesajes hasta producto terminado y limpieza final.
- [x] Vista general de Producción con Fermentación como estación interna.
- [ ] Configurar pasos opcionales y ruta de envasado por versión de receta.
- [x] Liberación básica de orden con código único y estados hasta la asignación de fermentación.
- [x] Ejecución secuencial desde kit de pesajes hasta enfriado, con responsables, tiempos, notas y mediciones.
- [ ] Extender la ejecución registrada a fermentación, maduración, envasado y cierre de la orden.
- [ ] Kit de pesajes ligado a reservas y lotes de inventario.
- [x] Registro genérico de inicio, cierre, notas y mediciones para maceración, cocción, adiciones, enfriado y transferencias.
- [ ] Formularios especializados y límites de proceso para cada etapa del tren de cocción.
- [ ] Consumos, mediciones, liberaciones de calidad y firmas de operador.
- [ ] Envasado por barril, botella o lata y entrada automática a producto terminado.
- [ ] Corridas hijas de envasado (`-K01`, `-B01`, `-C01`) y asociación de recipientes físicos (`BRL-001` en adelante) en el batch record.

## M3 — Banco de hardware

- [x] Registrar placas y separación eléctrica del chiller, variador, transformador y bomba.
- [ ] Identificar completamente PLC, módulo de relés y sensores.
- [x] Adaptador serie de solo lectura para la demo del HMI/PLC en COM5, deshabilitado por defecto.
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

## Incremento Web - operación de lotes de fermentación

- [x] Fermentación solo recibe lotes fabricados y listos; no crea ni cierra el lote maestro.
- [x] Asignación a tanque libre con volumen real transferido y snapshot de equipo/sensor.
- [ ] Consulta de lotes cerrados y acceso a su bitácora desde la WebApp.

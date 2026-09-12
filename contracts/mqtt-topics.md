# Contrato MQTT propuesto

Prefijo: `sdbrewpi/v1/{site}/{deviceId}`.

- `/telemetry`: lectura nueva; QoS 1, no retenida.
- `/state`: estado físico observado; QoS 1, retenido.
- `/availability`: Last Will `offline`; QoS 1, retenido.
- `/command`: intención con `commandId`, `expiresAt` y revisión esperada.
- `/ack`: resultado con el mismo `commandId`.
- `/alarm`: alarma generada localmente.

Una telemetría debe incluir `messageId`, `sequence`, `capturedAt`, `receivedAt`, `metrics` y `quality`. Los históricos atrasados se almacenan, pero nunca alimentan decisiones actuales.

## Compatibilidad temporal

El backend consume en modo de solo lectura:

- `sierra/rapt/amarilla/telemetry` y `sierra/rapt/roja/telemetry`: JSON con `temperature`, `gravity` o `sg`, batería, RSSI y timestamp.
- `rapt/pill/{id}/history`: JSON del buffer offline con `ts`, `temp`, `sg`, `bat` y `rssi`.
- `rapt/pill/{id}/{temperature|gravity|battery|rssi}`: valores escalares antiguos.

Los valores escalares retenidos carecen de hora de captura. Se almacenan con calidad `UNVERIFIED_TIME` y quedan excluidos del estado usado para control. Los mensajes `/history` se etiquetan `HISTORICAL` y también quedan excluidos. La ventana inicial para considerar una muestra actual es de 90 segundos.

## Estado del firmware encontrado

El firmware ESP32-C3 disponible publica las lecturas actuales como cuatro mensajes escalares retenidos y solo agrega `ts` al vaciar su buffer por `/history`. Por eso, una suscripción nueva no puede distinguir con seguridad una lectura actual de una copia antigua conservada por el broker.

La configuración antigua de Home Assistant contiene suscripciones a `sierra/rapt/.../telemetry`, pero en la copia revisada no aparece el productor que construía esos mensajes JSON. El adaptador acepta ese formato únicamente cuando incluye una hora de captura válida.

Para habilitar telemetría actual verificable, cada repetidor deberá publicar un único mensaje `sdbrewpi/v1/{site}/{deviceId}/telemetry` con temperatura, gravedad, batería, RSSI, `capturedAt`, `messageId` y `sequence`. Hasta entonces, la compatibilidad existente sirve para diagnóstico e historial y no habilita actuación física.

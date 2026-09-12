# Contrato MQTT propuesto

Prefijo: `sdbrewpi/v1/{site}/{deviceId}`.

- `/telemetry`: lectura nueva; QoS 1, no retenida.
- `/state`: estado físico observado; QoS 1, retenido.
- `/availability`: Last Will `offline`; QoS 1, retenido.
- `/command`: intención con `commandId`, `expiresAt` y revisión esperada.
- `/ack`: resultado con el mismo `commandId`.
- `/alarm`: alarma generada localmente.

Una telemetría debe incluir `messageId`, `sequence`, `capturedAt`, `receivedAt`, `metrics` y `quality`. Los históricos atrasados se almacenan, pero nunca alimentan decisiones actuales.


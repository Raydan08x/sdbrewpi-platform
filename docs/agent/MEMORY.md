# Memoria activa

## Decisiones

- Java 21 + Spring Boot para el backend.
- React + TypeScript para la WebApp.
- Monolito modular para el negocio; control físico desacoplado.
- Dos tanques en el primer incremento.
- Pruebas en PC y simulación antes de conectar hardware.
- MQTT se integra primero en modo de solo lectura; `hardware.enabled=false` permanece obligatorio.
- Los tópicos retenidos sin timestamp y `/history` nunca alimentan el control actual.

## Hardware identificado

- HMI/PLC: Waveshare ESP32-S3 Touch LCD 4.3B, 800x480, RS485/CAN/I2C.
- HMI auxiliar: Waveshare ESP32-S3 Touch LCD 2, 240x320.
- Relé: referencia AliExpress 1005006982002502; modelo exacto por confirmar físicamente.
- Sensor del depósito del chiller: pendiente de adquirir.
- Chiller: depósito de 500 L y motor/compresor de 3 HP, 220 V trifásico.
- Variador: Power Electric `3HP/220V PD2000 3 32`, entrada monofásica 220–240 V y salida trifásica 0–240 V; manual y protocolo pendientes.
- Bomba de circulación: Pedrollo PKm 60, monofásica 110 V, 0,5 HP, 5,5 A; es independiente del variador.
- Transformador elevador: PEC 3 kVA, 110–220 V; conexión exacta pendiente de diagrama.

## Estado

- M0 implementa una cadena simulada y no acciona hardware.
- El adaptador M1 acepta los tópicos actuales `sierra/rapt/...` y `rapt/pill/...`, además del contrato `sdbrewpi/v1/...`.
- El broker documentado en la red anterior no fue alcanzable desde la PC durante el QA del 2026-09-12.
- Los repetidores y Pills están temporalmente fuera de servicio por batería; continuar QA con simulación hasta aviso del propietario.
- Producción contiene recetas versionadas, fases y lotes activos. `DEMO-PALE-ALE` y `SIM-LOTE-001` son datos exclusivos de simulación.

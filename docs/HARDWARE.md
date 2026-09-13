# Inventario de hardware

| Equipo | Identificación | Estado |
|---|---|---|
| HMI principal | Waveshare ESP32-S3 Touch LCD 4.3B, 800x480 | Confirmado por referencia de compra |
| HMI auxiliar | Waveshare ESP32-S3 Touch LCD 2, 240x320 | Confirmado por referencia de compra |
| Módulo de relés | AliExpress 1005006982002502; USB COM3; incluye buzzer | Firmware, modelo/pines/protocolo pendientes de estabilización por el propietario |
| Variador | Pendiente | Confirmar modelo, manual y carga accionada |
| Bomba | 0.5 HP | Tensión, corriente y protección pendientes |
| Sensor de depósito | No adquirido | Requisito para automatización física |

No asignar pines, canales de relé, registros Modbus ni parámetros de variador hasta comprobar las etiquetas y manuales del hardware físico.

COM5 corresponde a la pantalla/PLC que actualmente emite telemetría de demostración. Ambos firmwares están bajo pruebas del propietario: SDBrewPi no debe flashear, modificar ni enviar comandos a COM3 o COM5 hasta nueva indicación explícita.

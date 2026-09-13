# Inventario técnico observado de la planta

Fecha de revisión: 2026-09-12. Fuente: fotografías suministradas por el propietario y datos confirmados en conversación. Una fotografía confirma presencia y placa, pero no certifica el cableado interno ni el estado operativo.

## Sistema de frío

| Elemento | Dato confirmado | Uso previsto en SDBrewPi |
|---|---|---|
| Chiller | Depósito de 500 L; motor del compresor de 3 HP, 220 V trifásico | Fuente de glicol/agua fría compartida por los fermentadores |
| Variador | Power Electric, modelo de placa `3HP/220V PD2000 3 32`; entrada AC monofásica 220–240 V, 50/60 Hz, 23 A; salida AC trifásica 0–240 V, 0–600 Hz, 9,6 A | Accionamiento del motor trifásico del chiller; protocolo y parámetros pendientes de manual y prueba en banco |
| Transformador | PEC, elevador 110–220 V, 3 kVA | Alimentación del circuito de 220 V; relación exacta con variador/chiller pendiente de verificar en diagrama eléctrico |
| Bomba de circulación | Pedrollo PKm 60, monofásica 110 V, 60 Hz, 0,37 kW / 0,5 HP, 5,5 A, P1 550 W, 3450 rpm, IPX4, clase F | Circulación del fluido frío; salida independiente del variador |
| Prestación nominal de bomba | 5–40 L/min; altura 38–5 m; altura máxima 40 m; líquido hasta 60 °C | Base para seleccionar sensor de flujo y comprobar el punto real de operación |
| Protección visible | Interruptor Schneider Electric de dos polos; referencia visible `DomA62` | Presencia confirmada; calibre, curva, poder de corte y coordinación pendientes de foto legible/inspección |

La [ficha oficial de Pedrollo](https://www.pedrollo.com/wp-content/uploads/schede-tecniche/ES/PKm-60_ES-ficha-tecnica_60Hz.pdf) confirma para la PKm 60 de 60 Hz un caudal máximo de 40 L/min, altura máxima de 40 m y consumo de 5,5 A en la versión monofásica de 110 V.

## Fermentación y proceso

- La planta fotografiada incluye dos fermentadores cónicos que serán el alcance inicial del control de frío.
- Se observan otros recipientes de proceso, tubería sanitaria, bomba de proceso, llenadora neumática y tablero manual. Se registrarán como activos futuros cuando se conozcan capacidades, conexiones y función exacta.
- Las capacidades nominales de los dos fermentadores, el diámetro de las chaquetas y el fluido real del circuito quedan pendientes.
- Los repetidores ESP32-C3 y las Pills están fuera de servicio temporalmente por batería; esto no bloquea el desarrollo en simulación.

## Separación eléctrica obligatoria

La bomba Pedrollo es una carga monofásica de 110 V. El variador entrega alimentación trifásica para el motor de 3 HP del chiller. El diseño de control los trata como dos actuadores independientes:

1. `PUMP_ENABLE`: orden discreta a contactor o relé intermedio dimensionado para la bomba.
2. `CHILLER_ENABLE`: permiso general del sistema de refrigeración.
3. `VFD_RUN`: marcha del variador mediante su entrada de control aislada.
4. `VFD_SPEED_REFERENCE`: referencia de velocidad únicamente si el manual y las pruebas confirman la interfaz.

El módulo Waveshare de seis relés no alimentará directamente motores ni compresores. Sus contactos deberán gobernar contactores o entradas de mando con protección y aislamiento adecuados.

## Señales mínimas antes de habilitar hardware

| Señal | Motivo |
|---|---|
| Temperatura del depósito del chiller | Controlar la reserva térmica y evitar congelación |
| Confirmación de flujo | Impedir compresor o demanda de frío sin circulación |
| Nivel mínimo del depósito | Proteger bomba y sistema frigorífico |
| Falla/listo del variador | Detener la secuencia ante protección del motor |
| Realimentación de bomba | Detectar orden sin circulación real |
| Parada de emergencia cableada | Llevar el sistema a estado seguro sin depender del servidor |

## Secuencia segura preliminar

1. Verificar parada de emergencia liberada, nivel válido, sensor de depósito válido y variador listo.
2. Encender la bomba de circulación.
3. Esperar confirmación de flujo durante un tiempo configurable.
4. Autorizar el chiller y posteriormente la marcha del variador.
5. Ante pérdida de flujo, nivel, sensor o comunicación, retirar la orden del compresor/variador y registrar la alarma.
6. Mantener la bomba durante un tiempo de postcirculación antes de apagarla.

Esta secuencia es una especificación de software. Los enclavamientos críticos también deberán existir en el controlador de campo y, cuando corresponda, mediante cableado dedicado.

## Pendientes de levantamiento

- Fotografía legible de la placa del motor/compresor y del interruptor de protección.
- Manual exacto del variador PD2000 y mapa de terminales/parámetros.
- Diagrama unifilar desde la acometida hasta transformador, variador, bomba y auxiliares.
- Confirmar si el transformador de 3 kVA alimenta únicamente el variador y medir tensión de entrada/salida.
- Identificar contactores, relés térmicos, presostatos y protecciones internas existentes.
- Medir el caudal real con el circuito conectado y definir concentración del fluido frío.
- Capacidades nominales y conexiones de las chaquetas de ambos fermentadores.

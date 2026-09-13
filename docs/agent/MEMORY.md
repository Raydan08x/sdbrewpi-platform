# Memoria activa

## Decisiones

- Java 21 + Spring Boot para el backend.
- React + TypeScript para la WebApp.
- Monolito modular para el negocio; control físico desacoplado.
- Dos tanques en el primer incremento.
- Pruebas en PC y simulación antes de conectar hardware.
- MQTT se integra primero en modo de solo lectura; `hardware.enabled=false` permanece obligatorio.
- Los tópicos retenidos sin timestamp y `/history` nunca alimentan el control actual.
- El orden funcional acordado es Mi Planta, Inventarios, Recetas, Planeación MRP, Producción, Compras, Ventas y CRM, y Finanzas.
- Fermentación pertenece a Producción. Inventarios antecede a Recetas para aportar artículos, unidades, lotes y costos.
- SDBrewPi conservará la operación y el costo industrial; la contabilidad fiscal se integrará por adaptadores con Alegra o Siigo, todavía sin proveedor elegido.

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
- Mi Planta es la pantalla inicial y registra una sede y 13 activos conocidos. El escáner USB y la instalación de firmware están especificados pero deshabilitados.
- Los equipos de Mi Planta admiten alta, edición y retiro lógico con revisión optimista y bitácora.
- Mi Planta admite bodegas con finalidad, control térmico, categorías permitidas y ubicaciones internas; inicia vacío para no inventar la distribución física de la sede.
- Producción abre en una ruta base de 43 etapas. Fermentación es una estación interna; los pasos opcionales y el envasado por barril, botella o lata aún no se ejecutan como workflow.
- COM3 es el módulo de seis relés y buzzer; COM5 es la pantalla/PLC que emite la demo de F1/F2. El propietario está modificando ambos firmwares: no flashearlos, modificarlos ni enviarles comandos.
- Fermentación admite COM5 en `PLC_DEMO_READ_ONLY`, deshabilitado por defecto, y conserva curvas y alarmas persistentes. La prueba física recibió cuatro muestras antes de que la demo dejara de emitir y confirmó el estado degradado posterior.
- Los perfiles de fermentación se pueden iniciar, pausar y reanudar; un planificador avanza fases por tiempo, aplica rampas configurables en °C/h, actualiza el setpoint y registra eventos. Mientras está en marcha, el perfil bloquea comandos manuales; la pausa deja el tanque en `MANUAL` y la reanudación recupera `AUTO` y la rampa. Al cerrar el lote, el control vuelve a `OFF`.
- La bitácora de fermentación acepta notas, mediciones, muestras, desviaciones, controles de calidad, sanitización y adiciones estructuradas. Las alarmas conservan historial y reconocimiento con operador, hora y nota. Falta la firma ligada a autenticación.
- La carpeta del propietario llamada `intento interfaz lvgl` es referencia de solo lectura: no editar, formatear, compilar ni cargar su contenido. Todavía no era visible dentro de `C:\Carlos` al revisar el 2026-09-13.

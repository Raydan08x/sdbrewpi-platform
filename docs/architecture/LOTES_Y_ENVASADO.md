# Identificación de lotes y corridas de envasado

## Lote maestro

El código se reserva al liberar la orden de producción y abre el batch record digital. Su forma es:

`PRODUCTO-AAMM-CLASESECUENCIA`

Ejemplos:

- `CERV-2609-L001`: cerveza comercial, septiembre de 2026, secuencia 001.
- `CERV-2609-P001`: cerveza piloto del mismo período.
- `CERV-2609-T001`: prueba técnica que no debe confundirse con producto comercial.
- `HSEL-2609-L001`: hard seltzer comercial.

La secuencia es independiente por producto, período y clase. El período se calcula en la zona horaria configurada en **Mi Planta**. El código del lote maestro permanece igual durante fabricación, fermentación, maduración y envasado.

## Corridas de envasado

Cada ejecución de envasado será un registro hijo del lote maestro:

- `CERV-2609-L001-K01`: primera corrida a barriles.
- `CERV-2609-L001-B01`: primera corrida a botellas.
- `CERV-2609-L001-C01`: primera corrida a latas.

Una corrida debe registrar fecha y hora de inicio y fin, operador, equipo, formato, volumen inicial, unidades conformes, rechazos, pérdidas, controles de calidad y liberación.

## Recipientes físicos

Los barriles son activos reutilizables y conservan un identificador permanente: `BRL-001`, `BRL-002`, … `BRL-010`. Cada uno se asocia a la corrida dentro del batch record con volumen, hora de llenado, estado de limpieza/sanitización y resultado de inspección.

El identificador del barril no se concatena al código de corrida. Se descartó `K101`: mezclaría el recipiente con la corrida, se vuelve ambiguo al crecer y dificulta seguir el historial del mismo barril entre lotes.

La llenadora disponible se registra como una sola línea con cuatro cabezales. La corrida es común a los cuatro cabezales; el cabezal puede guardarse por envase o muestra cuando se requiera investigar un defecto específico.

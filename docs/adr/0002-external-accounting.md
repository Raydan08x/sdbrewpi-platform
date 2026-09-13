# ADR 0002 — Contabilidad externa con integración por adaptadores

- Estado: aceptada para la arquitectura; proveedor pendiente.
- Fecha: 2026-09-12.

## Decisión

SDBrewPi será la fuente de verdad para planta, artículos, recetas, MRP, producción, trazabilidad, existencias operativas y costo industrial. La contabilidad fiscal, facturación electrónica, impuestos, libros oficiales y cierres vivirán en un sistema contable externo.

La integración se implementará detrás de un puerto estable del módulo Finanzas, con adaptadoresles por proveedor. Alegra y Siigo son candidatos porque ambos publican APIs para documentos, terceros, productos y operaciones contables. La selección se hará con el contador y debe considerar el país, el plan contratado, la facturación electrónica y el flujo real de conciliación.

Referencias oficiales:

- [API de Alegra](https://developer.alegra.com/)
- [API de Siigo](https://developers.siigo.com/docs/siigoapi)

## Responsabilidades

SDBrewPi calculará consumos, mermas, rendimientos, costos por lote y movimientos de inventario. Enviará al proveedor externo documentos aprobados y asientos resumidos mediante una bandeja de salida transaccional. Cada envío tendrá una clave idempotente, estado, intentos, respuesta y enlace entre identificadores.

El proveedor externo devolverá estados fiscales, números definitivos y eventos de aceptación o rechazo. Un fallo de sincronización no debe modificar ni repetir un lote de producción. Las correcciones contables se harán con documentos de reversión trazables.

## Consecuencias

- Se evita reproducir normativa tributaria y contable que cambia por país.
- La operación de planta continúa si el proveedor contable está temporalmente fuera de línea.
- Será necesario configurar cuentas, impuestos, centros de costo, bodegas, unidades y terceros antes de activar la sincronización.
- El primer adaptador se elegirá cuando el propietario confirme el software usado por su contador.

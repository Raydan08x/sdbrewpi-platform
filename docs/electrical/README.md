# Documentacion electrica

El paquete `SDBrewPi_E-101_Paquete_Electrico_Preliminar.pdf` traduce a planos A3 el documento de referencia `DOCUMENTO_DIAGRAMA_TECNICO_ELECTRICO.md`, el firmware LVGL y las fichas confirmadas del hardware.

## Estado

Revision A.1 preliminar, 11 hojas A3. Sirve para levantamiento, calculo y validacion por un profesional competente. No autoriza construir, cablear potencia ni energizar el chiller.

## Contenido

- E-100: portada, alcance y puntos de bloqueo.
- E-100A: inventario fotografico con las imagenes reales del propietario.
- E-100B: rutas funcionales de potencia y control con fotografias.
- E-101: diagrama unifilar de fuerza.
- E-102: mando, interlocks y salidas CH1-CH6.
- E-103: instrumentacion y comunicaciones.
- E-104: lista de conexiones e I/O.
- E-105: listado preliminar de materiales.
- E-106: verificaciones de ingenieria.
- E-107: protocolo de pruebas y entrega.
- E-108: fuentes, revisiones y firmas.

## Regeneracion

Desde la raiz del repositorio:

```powershell
python tools/generate_electrical_package.py
```

El PDF se escribe en `output/pdf/SDBrewPi_E-101_Paquete_Electrico_Preliminar.pdf`.

Las fotografias originales del propietario usadas en las laminas se conservan en `docs/electrical/assets/`. Los ejemplos visuales no son fuente de calibres, protecciones ni cargas. Las flechas fotograficas no sustituyen un plano de bornes.

## Datos pendientes para Revision B

- Manual exacto del variador Power PD2000 3 32.
- Placa y diagrama de bornes del compresor.
- Placa completa y tipo del transformador PEC 3 kVA.
- Placas de las electrovavulas.
- Datos de acometida, puesta a tierra, cortocircuito, longitudes y canalizaciones.
- Inventario de presostatos, termicos, flujo, nivel y proteccion de aceite existentes.

El firmware de la carpeta de referencia LVGL no fue modificado, compilado ni cargado.

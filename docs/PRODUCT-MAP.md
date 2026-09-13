# Mapa del producto

SDBrewPi se organiza alrededor de una planta configurable y reutilizable. Cada instalación tendrá una sede, equipos, capacidades, bodegas y dispositivos propios; los módulos operativos consumirán esos datos maestros.

## Orden de navegación

1. **Mi Planta**: identidad de empresa y sede, logo, capacidades, fermentadores, equipos, sensores, remanentes de tubería, red y preparación de dispositivos.
2. **Inventarios**: artículos, lotes, movimientos, existencias y bodegas.
3. **Recetas**: formulación versionada que referencia materias primas y materiales registrados.
4. **Planeación MRP**: demanda, disponibilidad, necesidades de compra y calendario de producción.
5. **Producción**: órdenes y lotes, ejecución, fermentación, consumos, mermas, trazabilidad y costos reales.
6. **Compras**: proveedores, solicitudes, órdenes, recepciones y cuentas asociadas.
7. **Ventas y CRM**: clientes, oportunidades, pedidos, despachos y servicio.
8. **Finanzas**: costos, cartera, caja y sincronización con el sistema contable externo.

Inventarios antecede a Recetas porque una receta debe referenciar códigos, unidades, costos y lotes ya definidos. Producción contiene Fermentación como proceso; Fermentación no será otro módulo principal en el menú.

## Datos de Mi Planta

- Empresa: nombre comercial, razón social, identificación tributaria, logo, moneda y zona horaria.
- Sede: código estable, nombre, capacidad nominal por lote y número previsto de fermentadores.
- Equipos: tipo, código, fabricante, modelo, capacidad, alimentación, protocolo, identificador físico, firmware y estado.
- Ingeniería de proceso: capacidad útil, remanente de tubería, pérdidas previstas, conexiones y circuitos.
- Sensores y actuadores: asociación a equipo, ubicación, calibración, unidad, límites, estado y último mantenimiento.
- Bodegas: ubicación física, finalidad y reglas para materias primas, empaque, insumos y producto terminado.

La primera sede usa el código estable `MAIN`. Los nombres pueden cambiar sin romper las relaciones internas.

## Modelo de inventario previsto

Un único catálogo de artículos tendrá una categoría y una unidad base. Las existencias se separarán por bodega, lote y estado de calidad.

| Categoría | Ejemplos | Trazabilidad principal |
|---|---|---|
| Materia prima | Malta, lúpulo, levadura, sales | Lote de proveedor, vencimiento, análisis |
| Material de empaque | Botellas, tapas, latas, etiquetas, cajas | Lote, proveedor, versión gráfica |
| Insumo operativo | Detergentes, sanitizantes, filtros, CO₂ | Lote, vencimiento, ficha de seguridad |
| Producto en proceso | Mosto y cerveza en fermentación o maduración | Lote de producción, tanque, etapa |
| Producto terminado | Barril, botella, lata o caja vendible | Lote, presentación, fecha de liberación |
| Repuesto | Sellos, mangueras, contactores, sensores | Compatibilidad, ubicación, mantenimiento |

Las bodegas no equivalen a categorías: una misma categoría puede existir en varias bodegas y una bodega puede guardar distintas categorías si sus reglas lo permiten.

Mi Planta ya permite registrar, editar y retirar lógicamente bodegas y sus ubicaciones internas. Cada bodega declara una finalidad principal, si requiere control térmico y las categorías de inventario que admite. Los artículos, existencias y movimientos consumirán esta configuración sin duplicar la estructura física.

## Instalación plug-and-play prevista

El instalador local deberá seguir una secuencia explícita y reversible:

1. Detectar puertos y anunciar únicamente hardware compatible.
2. Leer identidad, revisión de placa, versión instalada y capacidades.
3. Resolver un perfil de firmware firmado para esa revisión exacta.
4. Mostrar al operador dispositivo, versión actual, versión destino y cambios.
5. Crear respaldo cuando el dispositivo lo permita.
6. Verificar firma y checksum antes de escribir.
7. Instalar con salidas físicas bloqueadas.
8. Reiniciar, ejecutar autoprueba, registrar resultado y conservar el log.
9. Asociar el dispositivo a la sede y al equipo físico correspondiente.

El backend expondrá trabajos de instalación y su progreso. Un agente local con acceso USB hará la detección y el flasheo. El navegador nunca accederá directamente a puertos industriales. Los perfiles se versionarán por familia, revisión de hardware y esquema de configuración. Un firmware desconocido o una revisión ambigua debe detener el proceso.

En la etapa actual los equipos pueden registrarse, editarse y retirarse de forma lógica. El escáner y la instalación permanecen deshabilitados hasta disponer del hardware conectado y validar cada perfil.

## Ruta base de producción

Producción abre en una vista general del lote. Fermentación es una estación de esa ruta y conserva su pantalla de control térmico independiente.

La ruta base contiene 43 etapas agrupadas así:

1. Preparación: liberar orden, armar kit y pesajes, limpieza previa, tratamiento y llenado de agua, molienda.
2. Maceración y separación: empaste, sales y ajustes, control de pH, maceración, mash out, recirculado, filtrado del mosto, lavado y controles pre-cocción.
3. Cocción: hervor, adiciones, whirlpool/reposo y control post-cocción.
4. Lado frío: enfriado, transferencia sanitaria, oxigenación e inoculación.
5. Bodega fría: fermentación, dry hopping, descanso de diacetilo, manejo de levadura/trub, cold crash, maduración, transferencia a tanque brillante, clarificación, filtración y carbonatación.
6. Envasado: preparación y sanitización, ruta de barril, botella o lata, pasteurización opcional, control de calidad, etiquetado y empaque secundario.
7. Cierre: liberación a producto terminado y limpieza posterior.

Los pasos opcionales y las alternativas de envasado se activarán por la versión de receta y la orden de producción. El catálogo no implica todavía ejecución automática ni accionamiento físico.

# Proyecto

SDBrewPi Platform es una WebApp modular para gestionar una cervecería artesanal. La primera entrega controla conceptualmente dos fermentadores y un chiller compartido, comenzando en simulación.

## Stack

- Backend: Java 21, Spring Boot, JDBC, Flyway.
- Frontend: React, TypeScript y Vite.
- Persistencia: PostgreSQL en Docker; H2 para pruebas.
- Campo futuro: MQTT y firmware ESP32 en C/C++.

## Reglas de arquitectura

- El backend empresarial guarda recetas, lotes, perfiles, usuarios y auditoría.
- El controlador de campo conserva los interlocks y el estado seguro aunque el servidor no esté disponible.
- La WebApp expresa intenciones y muestra confirmaciones.
- El modo simulado debe ser inequívoco en API y UI.

## Orden funcional acordado

Mi Planta → Inventarios → Recetas → Planeación MRP → Producción → Compras → Ventas y CRM → Finanzas.

Mi Planta es la raíz de configuración reutilizable por sede: identidad, capacidades, equipos, sensores, tuberías, bodegas y preparación de firmware. Fermentación es un proceso del módulo Producción. La contabilidad fiscal se delegará a un proveedor externo mediante adaptadores.

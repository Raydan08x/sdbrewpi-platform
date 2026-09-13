# Arquitectura

```text
Pills -> repetidores BLE/MQTT -> broker -> backend Spring Boot -> PostgreSQL
                                            |        |
                                         WebApp   comandos/ACK
                                                      |
                                             controlador de campo
                                                      |
                                      sensores, relés, bomba y chiller
```

El sistema empresarial será un monolito modular para evitar complejidad prematura. Los paquetes previstos son `plant`, `inventory`, `recipes`, `mrp`, `production`, `fermentation`, `purchasing`, `sales`, `crm` y `finance`. `fermentation` pertenece funcionalmente a Producción. La contabilidad fiscal queda detrás de adaptadores externos descritos en `docs/adr/0002-external-accounting.md`.

La configuración de sede y el registro técnico de activos viven en `plant`. Los demás módulos referencian identificadores estables de sede, bodega, equipo y artículo; no duplican esa configuración.

La lógica de seguridad y continuidad que deba sobrevivir a la caída del servidor vivirá en el controlador de campo. Node-RED podrá coexistir como diagnóstico y adaptador temporal, sin ser la autoridad de actuación.

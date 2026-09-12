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

El sistema empresarial será un monolito modular para evitar complejidad prematura. Los paquetes previstos son `fermentation`, `recipes`, `production`, `inventory`, `purchasing`, `sales`, `crm`, `billing` y `accounting`.

La lógica de seguridad y continuidad que deba sobrevivir a la caída del servidor vivirá en el controlador de campo. Node-RED podrá coexistir como diagnóstico y adaptador temporal, sin ser la autoridad de actuación.


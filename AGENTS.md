# Instrucciones del proyecto

Lee `docs/agent/PROJECT.md`, `docs/agent/OPERATOR.md`, `docs/agent/MEMORY.md` y la tarea activa antes de editar.

- Mantén el backend como monolito modular por dominio.
- Separa planificación empresarial de control SCADA.
- Ningún endpoint web escribe GPIO o registros Modbus directamente.
- Todo comando operativo se valida, audita y devuelve estado explícito.
- `hardware.enabled=false` es el valor seguro por defecto.
- No presentes datos simulados como datos físicos.
- Ejecuta los QA descritos en `docs/QA.md` después de cambios.


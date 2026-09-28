---
name: cerrar-sprint
description: Procedimiento de cierre de un sprint de NetProtect con evidencia real. Solo cuando el usuario escribe /cerrar-sprint NN.
disable-model-invocation: true
argument-hint: "[número de sprint]"
---

Cierre del Sprint $ARGUMENTS. No te saltes pasos ni los marques como hechos sin salida real.

1. Lee `docs/sprint-$ARGUMENTS.md` y lista sus criterios de aceptación.
2. Delega a `verifier` todas las áreas que tocó el sprint (modo completo).
   Si tocó `mobile/`, exige `lintDebug`.
3. Delega a `security-reviewer`. Todo hallazgo ALTA bloquea el cierre.
4. Escribe `docs/sprint-$ARGUMENTS-evidence.md` solo con comandos que se ejecutaron y su salida
   real, incluidos los fallos del camino y cómo se corrigieron.
5. Marca cada criterio de aceptación: cumplido (con enlace a la evidencia) o pendiente (con motivo;
   si es humano, dilo).
6. Añade `## Alcance del Sprint $ARGUMENTS` al `README.md`.
7. Actualiza `docs/progress.md` (En curso, Terminado, Siguiente paso) y `docs/tasks.md`.
8. Si surgió una lección «válida para cualquier trabajo futuro sobre X», añádela a la regla de
   `.claude/rules/` de esa área — **no** a `CLAUDE.md`.
9. Propón el mensaje de commit (por qué, no solo qué). No hagas commit ni push sin confirmación.
10. Recuerda al usuario: el sprint se cierra cuando CI pasa en los 8 jobs en GitHub Actions.

# Tareas — NetProtect
Estados: `todo` · `doing` · `blocked` · `done` (los `done` se borran al cerrar sprint)
Prioridad: P1 (bloquea) · P2 (importante) · P3 (cuando haya tiempo)

## Bugs
| Id | Prioridad | Estado | Descripción | Origen |
|---|---|---|---|---|
| B-01 | P2 | todo | Dispositivos duplicados en `GET /devices/me` | `docs/sprint-29.md` |
| B-02 | P2 | todo | Reconexión del canal en tiempo real en Android | `docs/sprint-29.md` |
| B-03 | P3 | todo | Mensaje claro cuando la misma cuenta es tutor y supervisado | Sprints 28–29 |

## Pendiente de un humano
| Id | Estado | Descripción | Origen |
|---|---|---|---|
| H-01 | blocked | Login real de Google elegido por una persona | regla de evidencia |
| H-02 | blocked | Persona aceptando el diálogo de captura (vista remota) | `docs/sprint-23-evidence.md` |
| H-03 | blocked | Despliegue con dominio y cuenta cloud reales | Sprint 26 |
| H-04 | blocked | Permisos de administrador en GitHub | `docs/manuals/analisis-riesgos.md` |

## Deuda técnica
| Id | Prioridad | Descripción |
|---|---|---|
| D-01 | P2 | Filtrado web (`VpnService`) nunca construido — contradice RF-07 del informe académico |
| D-02 | P2 | `lintDebug` no corre en CI; es el único que detecta APIs > minSdk 26 |
| D-03 | P3 | Sesión web en `sessionStorage` en vez de cookie HttpOnly |
| D-04 | P3 | Sin *type checking* en Python (no hay mypy) |
| D-05 | P3 | `make android-check` usa `gradle`; CI usa `./gradlew` |
| D-06 | P3 | `TutorScreen.kt` (963 líneas) y `apiClient.ts` (889) concentran demasiado |
| D-07 | P3 | Sin medición de cobertura |

## Tareas
| Id | Prioridad | Estado | Descripción |
|---|---|---|---|

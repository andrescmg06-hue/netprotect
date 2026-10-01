# Tareas — NetProtect
Estados: `todo` · `doing` · `blocked` · `done` (los `done` se borran al cerrar sprint)
Prioridad: P1 (bloquea) · P2 (importante) · P3 (cuando haya tiempo)

## Bugs
| Id | Prioridad | Estado | Descripción | Origen |
|---|---|---|---|---|
| B-01 | P2 | done | Dispositivos duplicados en `GET /devices/me` | `docs/sprint-29.md` |
| B-02 | P2 | todo | Reconexión del canal en tiempo real en Android | `docs/sprint-29.md` |
| B-03 | P3 | todo | Mensaje claro cuando la misma cuenta es tutor y supervisado. **Hallazgo S43:** `POST /users/me/roles` concede cualquier rol sin condiciones (`roles.py`), así que no hay error que mostrar en la app; hace falta un cambio de backend (p. ej. rechazar el canje en `pairing.py` cuando el tutor del código es la misma cuenta) | Sprints 28–29, `docs/sprint-43.md` |
| B-04 | P1 | done | Latidos con 500: alertas duplicadas por reportes simultáneos (`MultipleResultsFound`) | `docs/sprint-41.md` |
| B-05 | P2 | todo | Panel web: el sondeo de alertas no renueva el token caducado (401 cada minuto) | `docs/sprint-41-evidence.md` |
| B-06 | P3 | todo | Android: con letra grande, los botones de texto se aplastan en vertical (pantallas actuales; lo cubre el rediseño) | `docs/sprint-41.md` |

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
| D-06 | P3 | `TutorScreen.kt` (963 líneas) y `apiClient.ts` (889) concentran demasiado |
| D-07 | P3 | Sin medición de cobertura |

## Tareas
| Id | Prioridad | Estado | Descripción |
|---|---|---|---|

## Rediseño Android (S40–S51)
Detalle y arquitectura objetivo: `docs/android-redesign/ARCHITECTURE_GAPS.md`. Decisiones D-01…D-14
resueltas el 2026-09-28: `docs/android-redesign/DECISIONES.md`. La prioridad P1–P3 sigue la escala de
este archivo (CRITICAL→P1, HIGH→P2, resto→P3). Los B-01/B-02 de arriba se resuelven dentro de S48/S50.

| Id | Prioridad | Estado | Descripción — sprint |
|---|---|---|---|
| G-01 | P1 | done | Token de la UI caduca a los 15 min sin renovarse — S41 |
| G-02 | P1 | done | Posible carrera entre refresh concurrentes (verificar; puede revocar la familia de tokens) — S41 |
| G-03 | P2 | done | Sin modelo de errores: se muestran `HTTP 401` crudos o se tragan (`runCatching`) — S41 |
| G-04 | P1 | done | Sin tema, tipografía, iconos, logo ni icono de lanzador — S42 |
| G-05 | P1 | done | Sin componentes compartidos (tarjeta, badge, estados vacío/carga/error, fila…) — S42 |
| G-06 | P1 | doing | Sin navegación ni "atrás" del sistema (D-01 = rutas propias) — esqueleto S42; shell del tutor S44; shell del supervisado S48 |
| G-07 | P2 | done | Formateadores ("hace 12 min", "1 h 24 min", Hoy/Ayer) y etiquetas dispersos — S42 |
| G-08 | P2 | done | `TutorScreen.kt` monolítico (963 líneas; ver D-06 de deuda técnica) — S44 |
| G-09 | P3 | done | Cuenta atrás real del código de vinculación (`expires_in_seconds`) — S44 |
| G-10 | P3 | done | `os_version`/`app_version` no se leen en `DeviceClient` — S44 |
| G-11 | P2 | done | Desvincular sin confirmación; fallos silenciados — S44 |
| G-12 | P3 | done | Apps: búsqueda, fecha real del uso, iconos (D-07 = icono local o monograma) — S45 |
| G-13 | P2 | done | Ubicación/geocercas con textos de retraso y sin datos inventados (D-05) — S45 |
| G-14 | P3 | done | Historial agrupado por día con nombre de app — S46 |
| G-15 | P3 | done | Estadísticas con la semántica real de cumplimiento (días dentro del límite) — S46 |
| G-16 | P2 | done | Cliente Android sin "marcar leída" ni "silenciar" (D-09 = indefinido) — S46 |
| G-17 | P3 | done | Mi actividad con etiquetas, agrupación y paginación — S47 |
| G-18 | P2 | done | La consulta de ubicación no se audita (D-10 = `LOCATION_VIEWED`; backend) — S47 |
| G-19 | P1 | done | `SupervisedScreen` monolítico con los efectos de servicios dentro — S48 |
| G-20 | P3 | done | Vincular con entrada de 6 casillas y errores claros — S48 |
| G-21 | P3 | done | Vinculado: última comunicación, varios tutores, resumen de permisos — S48 |
| G-22 | P2 | done | B-01 dispositivos duplicados en `GET /devices/me` — S48 |
| G-23 | P2 | done | Pantalla de bloqueo: 7 variantes con icono y categoría reales (D-11) — S49 |
| G-24 | P2 | todo | Pantalla de consentimiento completa con textos verdaderos (D-12) — S50 |
| G-25 | P2 | todo | Registro de estado de servicios (pantalla 17) — S50 |
| G-26 | P2 | todo | B-02 reconexión del canal realtime — S50 |
| G-27 | P2 | todo | Accesibilidad, rotación, sin conexión, build release, código muerto — S51 |
| L-01 | P1 | done | `AuthRepository.kt:32`: `CredentialManager.getCredential` sin capturar `NoCredentialException` (lint CredentialManagerMisuse); revisar con `security-reviewer` — S41 |
| L-02 | P2 | todo | `AndroidManifest.xml:62`: `allowBackup` obsoleto y falta `dataExtractionRules` (lint DataExtractionRules); privacidad de datos de menores — S51 |
| L-03 | P2 | done | `AndroidManifest.xml:61`: falta `android:icon` explícito (lint MissingApplicationIcon); lo cubre G-04 — S42 |
| L-04 | P3 | todo | 11 avisos UseKtx (`SharedPreferences.edit`, `String.toUri`) y 13 de dependencias/AGP desactualizados (lint); actualizar aparte, no durante el rediseño |
| L-05 | P2 | todo | `backend/.venv` desactualizado (falta `prometheus_client`): `pytest -m "not integration"` no se puede ejecutar local (21 errores de colección) |
| L-06 | P2 | todo | Solo `RuleEvaluator` tiene tests en Android (35); el rediseño de UI no tiene red de seguridad hasta D-13 (S42) |

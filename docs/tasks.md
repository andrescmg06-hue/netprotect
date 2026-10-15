# Tareas — NetProtect
Estados: `todo` · `doing` · `blocked` · `done` (los `done` se borran al cerrar sprint)
Prioridad: P1 (bloquea) · P2 (importante) · P3 (cuando haya tiempo)

## Bugs
| Id | Prioridad | Estado | Descripción | Origen |
|---|---|---|---|---|
| B-01 | P2 | done | Dispositivos duplicados en `GET /devices/me` | `docs/sprint-29.md` |
| B-02 | P2 | done | Reconexión del canal en tiempo real en Android | `docs/sprint-29.md` |
| B-03 | P3 | todo | Mensaje claro cuando la misma cuenta es tutor y supervisado. **Hallazgo S43:** `POST /users/me/roles` concede cualquier rol sin condiciones (`roles.py`), así que no hay error que mostrar en la app; hace falta un cambio de backend (p. ej. rechazar el canje en `pairing.py` cuando el tutor del código es la misma cuenta) | Sprints 28–29, `docs/sprint-43.md` |
| B-04 | P1 | done | Latidos con 500: alertas duplicadas por reportes simultáneos (`MultipleResultsFound`) | `docs/sprint-41.md` |
| B-05 | P2 | done | Panel web: el sondeo de alertas no renueva el token caducado (401 cada minuto). **Cerrado en S70:** la causa era que `AuthContext` nunca renovaba el access token; ahora se renueva antes de caducar | `docs/sprint-41-evidence.md` |
| B-13 | P3 | todo | Backend: `/auth/refresh` lee y rota la sesión sin bloquear la fila (`auth.py:126-147`, sin `with_for_update`), así que dos canjes simultáneos del mismo refresh token pueden salir bien los dos y dejar dos sesiones válidas: el «un solo uso» no es atómico. El panel ya evita el doble canje (S70); falta cerrarlo en el servidor | `docs/sprint-70-evidence.md` (revisión de seguridad) |
| B-06 | P3 | todo | Android: con letra grande, los botones de texto se aplastan en vertical. S51 corrigió la píldora de estado, la barra inferior, la duración en Estadísticas y «Dispositivo vinculado» (`FlowRow`, `615e84d`/`5042d4a`); el resto de pantallas se revisa en el S67 sobre las pantallas del rediseño v2 | `docs/sprint-41.md`, `docs/sprint-51-evidence.md` |
| B-07 | P2 | done | Android: «Cambiar de modo» (`HomeScreen.kt:127-130`) solo borra `RolePreference`; al desmontarse `SupervisedShell` se detienen bloqueo, ubicación, realtime, sincronización y latido, sin llamada al backend, sin auditoría y sin aviso al tutor (lo ve `OFFLINE` a los 300 s, sin alerta). Preexistente. **Parcial (S61):** «Cambiar de modo» desapareció con el modo tutor (`aa63cf1`, `0217236`). Queda «Cerrar sesión», que tiene el mismo efecto: confirmación en S63 y aviso al tutor en B-11 | `docs/sprint-51-evidence.md` (T11, MEDIA a) **Cerrado en S61 (PR #19):** la app es solo del supervisado y no hay «Cambiar de modo». |
| B-08 | P2 | done | Android: la desvinculación hecha por el tutor no se detecta mientras el dispositivo sigue vinculado. El latido devuelve 200 con estado `UNLINKED` y el cliente lo ignora (`DeviceClient.kt:87`); el latido está en `SupervisedShell.kt:241-256` y solo `checkLink()` (`:207-231`), al arrancar el shell, lo ve. Mientras tanto el teléfono sigue enviando ubicación, apps y latido, y el backend los acepta (`deps.py:92-94`). Preexistente. Sprint 64 (B1 del plan v2) | `docs/sprint-51-evidence.md` (T11, MEDIA b) **Cerrado en S64 (PR #22):** el teléfono consulta `/devices/me` al volver y cada 5 min y pasa a «Desvinculado» (B1); un fallo de red nunca cuenta como desvinculación. |
| B-09 | P2 | done | Backend, invariante 6: al revincular la misma instalación con otro tutor se reutiliza la fila del dispositivo (`pairing.py:204-233`) y `get_location_history` filtra solo por `device_id` (`location.py:182-189`), no por el inicio del vínculo activo; un tutor nuevo podría ver ubicaciones guardadas antes de su vínculo (dentro de la retención). Preexistente. **Sin sprint asignado: necesita priorización del dueño** | `docs/sprint-51-evidence.md` (T11, nota de privacidad) **Cerrado en S68 (la ubicación):** un tutor lee solo lo recibido desde el inicio de su vínculo activo; lo derivado de la ubicación sigue abierto en B-12. |
| B-12 | P2 | done | Backend, invariante 6: tras B-09, un tutor nuevo que revincula la misma instalación sigue viendo datos **derivados de la ubicación** anteriores a su vínculo, filtrados solo por `device_id`: las coordenadas descifradas del centro de las geovallas que creó el tutor anterior (`geofences.py:90-102`, `_to_response` descifra), sus eventos de entrada y salida (`geofences.py:180-193`), la línea de tiempo (`history.py:41-48`) y las alertas `GEOFENCE_ENTER/EXIT` (`alerts.py:50-54`); además las geovallas del tutor anterior **no se borran al desvincular** (`pairing.py:276-326`) y siguen generando eventos. **Necesita una decisión de producto:** qué pasa con las geovallas de un tutor cuando se va y llega otro (¿se borran, las hereda el nuevo, empieza sin ellas?). Mismo corte técnico posible: `linked_at` del vínculo activo contra `received_at`/`created_at`/`first_occurred_at` | `docs/sprint-68.md` (revisión de seguridad) **Cerrado en S69:** decisión del dueño, cada tutor ve solo las geovallas que creó (y sus eventos, historial, alertas y silencios); lo anterior a la migración sin dueño inequívoco queda oculto, no borrado. Ver `docs/sprint-69.md` y `docs/sprint-69-evidence.md`. |
| B-10 | P3 | done | Consentimiento de vista remota: Atrás o los 2 min (`CONSENT_TIMEOUT_MS`) ocultan la solicitud en Android sin enviar nada (`SupervisedShell.kt:361-377`), y el panel web (`RemoteViewPanel.tsx:349-350`) espera indefinidamente. Falla en modo seguro (no se captura nada). Nuevo del S50. Sprint 65 (B5, aprobado por el dueño) | `docs/sprint-51-evidence.md` (T11, BAJA c) **Cerrado en S65 (PR #23):** «Atrás» y la caducidad envían `screen_share_stop` con `dismissed` o `expired` y la web tiene un límite de espera de 135 s (envío de mejor esfuerzo, sin cola). |
| B-11 | P3 | todo | Cerrar sesión en Android (`HomeScreen.kt:97-101`) detiene toda la protección sin avisar al tutor; no se verificó si `/auth/logout` deja rastro en la auditoría. Acción legítima. Backend; fuera del alcance del rediseño v2 | `docs/sprint-51-evidence.md` (T11, BAJA d) |

## Pendiente de un humano
| Id | Estado | Descripción | Origen |
|---|---|---|---|
| H-01 | done | Login real de Google elegido por una persona — hecho por el dueño en el Galaxy S25 FE con la build `minified` (12/10/2026) | regla de evidencia, `docs/sprint-51-evidence.md` (T10) |
| H-02 | done | Persona aceptando el diálogo de captura (vista remota) — el dueño aceptó y vio el vídeo en el panel (12/10/2026; auditoría del backend y confirmación verbal, sin captura) | `docs/sprint-23-evidence.md`, `docs/sprint-51-evidence.md` (T10) |
| H-03 | blocked | Despliegue con dominio y cuenta cloud reales | Sprint 26 |
| H-04 | blocked | Permisos de administrador en GitHub | `docs/manuals/analisis-riesgos.md` |

## Deuda técnica
| Id | Prioridad | Descripción |
|---|---|---|
| D-01 | P2 | Filtrado web (`VpnService`) nunca construido — contradice RF-07 del informe académico |
| D-02 | P2 | ~~`lintDebug` no corre en CI~~ — resuelto en S51 T1 (`263ade5`): corre en el job `android` (D-14) |
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
| G-06 | P1 | done | Sin navegación ni "atrás" del sistema (D-01 = rutas propias) — esqueleto S42; shell del tutor S44; shell del supervisado S48 |
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
| G-24 | P2 | done | Pantalla de consentimiento completa con textos verdaderos (D-12) — S50 |
| G-25 | P2 | done | Registro de estado de servicios (pantalla 17) — S50 |
| G-26 | P2 | done | B-02 reconexión del canal realtime — S50 |
| G-27 | P2 | done | Accesibilidad, rotación, sin conexión, build release, código muerto — S51 (parcial: TalkBack y 200 % en todas las pantallas pasan a S67, decisión del dueño 12/10/2026) |
| L-01 | P1 | done | `AuthRepository.kt:32`: `CredentialManager.getCredential` sin capturar `NoCredentialException` (lint CredentialManagerMisuse); revisar con `security-reviewer` — S41 |
| L-02 | P2 | done | `AndroidManifest.xml:62`: `allowBackup` obsoleto y falta `dataExtractionRules` (lint DataExtractionRules); privacidad de datos de menores — S51 T3 (`f5dbfac`) |
| L-03 | P2 | done | `AndroidManifest.xml:61`: falta `android:icon` explícito (lint MissingApplicationIcon); lo cubre G-04 — S42 |
| L-04 | P3 | todo | 11 avisos UseKtx (`SharedPreferences.edit`, `String.toUri`) y 13 de dependencias/AGP desactualizados (lint); actualizar aparte, no durante el rediseño |
| L-05 | P2 | todo | `backend/.venv` desactualizado (falta `prometheus_client`): `pytest -m "not integration"` no se puede ejecutar local (21 errores de colección) |
| L-06 | P2 | todo | Solo `RuleEvaluator` tiene tests en Android (35); el rediseño de UI no tiene red de seguridad hasta D-13 (S42) |

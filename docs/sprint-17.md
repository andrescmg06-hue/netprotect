# Sprint 17 — Alertas

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 16) pide: "Tipos y niveles INFO/WARNING/HIGH/CRITICAL;
reglas de generación; bandeja para el tutor; deduplicación y silenciado." `DeviceStatus.ALERT`
(app/models/device.py) ya existía como estado del dispositivo, pero reservado para detección de
manipulación (Sprint 20, que no existe aún) — este sprint es un concepto distinto: una bandeja de
notificaciones para el tutor, generada a partir de señales que ya existían (`AppRuleEvent`,
`GeofenceEvent`), sin pipeline de detección nuevo ni cambio al estado del dispositivo.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-064 | Como tutor, quiero recibir una alerta cuando se bloquee una app o se alcance un límite de tiempo, sin tener que revisar el historial completo. |
| HU-065 | Como tutor, quiero recibir una alerta cuando el dispositivo entre o salga de una geocerca. |
| HU-066 | Como tutor, quiero que bloqueos repetidos del mismo tipo no me inunden de alertas idénticas mientras no las haya revisado. |
| HU-067 | Como tutor, quiero poder silenciar un tipo de alerta que ya no me interesa recibir, y reactivarlo después. |

## Criterios de aceptación

1. `GET /devices/{device_id}/alerts` devuelve, para el tutor dueño del dispositivo, las alertas
   generadas, ordenadas por `last_occurred_at` descendente; tutor ajeno o supervisado reciben 404.
2. Un `AppRuleEvent` con `rule_type_applied` en `{DAILY_LIMIT, WEEKLY_LIMIT}` genera una alerta
   `WARNING`/`APP_LIMIT_REACHED`; en `{BLOCK, DEFAULT_POLICY, CATEGORY, SCHOOL_MODE, SCHEDULE}`
   genera `INFO`/`APP_BLOCKED`; `ALLOW` no genera ninguna.
3. Un `GeofenceEvent` de tipo `EXIT` genera `WARNING`/`GEOFENCE_EXIT`; de tipo `ENTER` genera
   `INFO`/`GEOFENCE_ENTER`.
4. Mientras una alerta con una `dedup_key` dada siga sin leerse, una repetición de la misma señal
   incrementa su `occurrence_count` y actualiza `last_occurred_at` en vez de crear una fila nueva.
   Al marcarla leída (`POST .../alerts/{id}/read`), la siguiente ocurrencia abre una alerta nueva.
5. `POST .../alerts/{id}/silence` silencia toda futura alerta con esa `dedup_key` (no sólo esa
   fila) por `days` días, o indefinidamente si se omite; `DELETE .../alert-silences/{id}` lo
   revierte.
6. El panel web y la pantalla del tutor en Android muestran la bandeja; marcar leída y silenciar
   son acciones sólo del panel web.

## Decisiones de diseño y su motivo

### Fuente de eventos: la que ya existía, no un pipeline nuevo

Nada de detección propia: la alerta se genera inline en los dos puntos de escritura que ya
existían — `POST /rule-events` (`app/api/v1/endpoints/rules.py`) y dentro de `POST /location`
(`app/api/v1/endpoints/location.py`), justo después de `evaluate_geofence_transitions()`. Mismo
patrón "evaluar/purgar al escribir, sin scheduler" que el resto del proyecto.

### Reglas de generación → nivel

Un límite de tiempo agotado (`DAILY_LIMIT`/`WEEKLY_LIMIT`) es más urgente para el tutor que un
bloqueo esperado (`BLOCK` y sus variantes de motivo) — de ahí `WARNING` contra `INFO`. Salir de una
geocerca (`EXIT`) es la transición que normalmente importa vigilar; entrar (`ENTER`, típicamente
volver a una zona segura) es informativo. `ALLOW` no es un bloqueo de ningún tipo, así que no genera
nada. `HIGH`/`CRITICAL` quedan en el catálogo (con su propio `CHECK` constraint) sin generador
propio todavía — reservados para las señales de manipulación del Sprint 20, mismo caso ya aceptado
para `DeviceStatus.ALERT`.

### Deduplicación sin ventana de tiempo arbitraria

En vez de inventar un umbral de minutos/horas sin base en ningún enunciado, la deduplicación se
ata a un estado real del dominio: mientras la alerta siga sin leer, es la misma incidencia en
curso, así que una repetición sólo la actualiza (`occurrence_count`, `last_occurred_at`). Leerla es
la señal de que el tutor ya la revisó — la siguiente ocurrencia es, por definición, una incidencia
nueva. Evita tanto la explosión de filas idénticas como una ventana de tiempo que habría que
justificar sin criterio claro.

### Silenciado por `dedup_key`, no por alerta suelta

Silenciar una alerta puntual no serviría de nada si la siguiente ocurrencia del mismo tipo vuelve a
notificar. `AlertSilence` se crea a partir de una alerta existente pero se guarda por
`(device_id, dedup_key)` — silencia todo bloqueo futuro de esa app, o toda entrada/salida futura de
esa geocerca, hasta que expire o el tutor la quite explícitamente.

### Retención

`alert_retention_days = 90`, mismo valor y mismo patrón "purgar al escribir" que
`app_rule_event_retention_days`/`geofence_event_retention_days` (Sprint 15) — una alerta contiene
incluso menos dato sensible que su evento origen (un nivel, un tipo, un nombre de paquete o el
nombre de geocerca que el propio tutor eligió).

### Android sólo lectura

Mismo criterio ya establecido para historial/geocercas/estadísticas: crear, marcar leída y
silenciar son acciones del panel web; Android sólo muestra la bandeja.

## Fuera de alcance

- **Pipeline de detección nuevo**: las únicas fuentes son `AppRuleEvent`/`GeofenceEvent`, que ya
  existían. Ninguna señal nueva (uso agregado, ubicación cruda) genera alertas en este sprint.
- **Niveles `HIGH`/`CRITICAL` con generador propio**: sin fuente de datos hoy — llegan con la
  detección de manipulación del Sprint 20.
- **Notificaciones push/tiempo real**: el tutor revisa la bandeja al abrir el panel/la app: no hay
  WebSockets ni FCM todavía (Sprint 18).
- **Acciones de tutor en Android** (marcar leída, silenciar): sólo panel web, mismo split que
  reglas/geocercas.

## Backend

- `app/models/alert.py` (nuevo): `Alert`, `AlertSilence`, catálogos `ALERT_LEVELS`/`ALERT_TYPES`.
- `app/services/alerts.py` (nuevo): `record_alert_for_rule_event()`, `record_alert_for_geofence_event()`,
  `_record_alert()` compartida (purga, comprobación de silencio, dedup).
- `app/schemas/alert.py` (nuevo): tipos y respuestas, `MAX_ALERTS = 200`.
- `app/api/v1/endpoints/alerts.py` (nuevo): `GET /devices/{id}/alerts`, `POST .../{id}/read`,
  `POST .../{id}/silence`, `GET /devices/{id}/alert-silences`, `DELETE .../alert-silences/{id}`;
  todo protegido con `require_tutor_of_device`; las mutaciones quedan en `audit_log`
  (`ALERT_READ`, `ALERT_SILENCED`, `ALERT_SILENCE_REMOVED`).
- `app/core/config.py`: `alert_retention_days = 90`.
- `app/api/v1/endpoints/rules.py`/`location.py`: llaman a `record_alert_for_*` tras insertar su
  propio evento.
- Migración `b7c3e0d4f1a8` (tablas `alerts`, `alert_silences`). Ciclo upgrade → downgrade → upgrade
  verificado en Docker.

## Web

- `apiClient.ts`: tipos `Alert`/`AlertSilence` y `listDeviceAlerts`/`markAlertRead`/`silenceAlert`/
  `listAlertSilences`/`deleteAlertSilence`.
- `AlertsPanel` (nuevo componente): bandeja con nivel, mensaje, contador de repeticiones, botones
  "Marcar leída"/"Silenciar", y lista de silencios activos con "Reactivar". Integrado en
  `DevicesPanel` junto a los demás paneles.

## Android

- `AlertsClient` (nuevo, `core/network`, sólo lectura).
- `TutorScreen`: botón "Ver alertas" por dispositivo, mismo patrón que historial/estadísticas.

## Verificación

Ver `docs/sprint-17-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Ejecución de la pantalla de Android en un emulador/dispositivo físico visualizando la bandeja de
  alertas — se compiló (`compileDebugKotlin`) y se verificó la lógica de consumo de la API contra
  el backend real por el mismo endpoint que usa el panel web (probado con la suite de integración),
  pero no se recorrió la UI de Android a mano en esta sesión.

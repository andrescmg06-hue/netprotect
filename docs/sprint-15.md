# Sprint 15 — Historial

## Objetivo

Antes de este sprint ya existían tres registros insert-only dispersos y sin política de retención
uniforme: `AppRuleEvent` (bloqueos, Sprint 8), `GeofenceEvent` (entradas/salidas, Sprint 14) y
`DeviceLocationReport` (ubicación, con retención de 7 días desde el Sprint 13). Este sprint no crea
una tabla "historial" genérica — como advierte el estado del proyecto en `CLAUDE.md`, eso habría
duplicado lo que ya existía. En su lugar: (1) da a `AppRuleEvent` y `GeofenceEvent` la misma
retención con purga automática que `DeviceLocationReport` ya tenía, para que ningún registro crezca
sin límite, y (2) añade una vista unificada — un endpoint y una pantalla, en web y Android — que
combina esos dos registros en una sola línea de tiempo cronológica, para que el tutor no tenga que
revisar cada tipo de evento por separado.

"Web" (navegación) y "alertas", mencionados en `docs/planning/plan-desarrollo.md` para este sprint,
quedan fuera — ver "Fuera de alcance".

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-058 | Como tutor, quiero ver en un solo lugar los bloqueos de reglas y las entradas/salidas de geocercas de un dispositivo, ordenados por fecha, sin tener que revisar cada pantalla por separado. |
| HU-059 | Como dueño del proyecto, quiero que ningún registro de eventos crezca sin límite en la base de datos, con una ventana de retención explícita y purga automática, igual que ya ocurre con la ubicación. |

## Criterios de aceptación

1. `GET /devices/{device_id}/history` devuelve, para el tutor dueño del dispositivo, los eventos de
   `AppRuleEvent` y `GeofenceEvent` combinados en una sola lista, ordenados por `occurred_at`
   descendente (más reciente primero); un tutor ajeno o el propio supervisado reciben 404 (mismo
   patrón `require_tutor_of_device` de siempre).
2. Cada entrada de la lista indica su tipo (`APP_RULE` o `GEOFENCE`) y sólo trae poblados los campos
   que le corresponden a ese tipo; los del otro tipo quedan en `null`.
3. `POST /devices/{device_id}/rule-events` purga, antes de insertar, las filas de `AppRuleEvent` de
   ese mismo dispositivo con más de `app_rule_event_retention_days` (90) días — mismo patrón
   "purgar al escribir, sin scheduler" que `DeviceLocationReport` ya usa.
4. `POST /devices/{device_id}/location` purga, antes de insertar, las filas de `GeofenceEvent` de
   ese mismo dispositivo con más de `geofence_event_retention_days` (90) días, además de su purga ya
   existente de `DeviceLocationReport`.
5. Ambas purgas están acotadas al `device_id` que escribe — nunca un barrido global — mismo criterio
   ya verificado para la purga de ubicación del Sprint 13.
6. El panel web y la pantalla del tutor en Android muestran la línea de tiempo unificada.

## Decisiones de diseño y su motivo

### No crear una tabla "historial" nueva

`AppRuleEvent` y `GeofenceEvent` ya son insert-only, ya tienen `occurred_at`/`received_at`, y ya
tienen sus propios endpoints de lectura. Duplicarlos en una tercera tabla habría significado
sincronizar dos copias de la misma evidencia — un origen de bugs, no una simplificación. El nuevo
endpoint (`app/api/v1/endpoints/history.py`) sólo lee ambas tablas, las combina en Python y las
reordena; no persiste nada nuevo.

### Ubicación cruda no entra en la línea de tiempo unificada

`DeviceLocationReport` ya tiene su propia vista dedicada (`DeviceLocationPanel`, mapa embebido o
enlace externo, Sprint 13) y reporta hasta 4 veces por hora. Mezclar hasta 96 puntos/día con
eventos discretos (un bloqueo, una entrada a una geocerca) enterraría la señal que el tutor
realmente quiere ver — cuándo pasó algo digno de revisar, no dónde estuvo el dispositivo minuto a
minuto. Se documenta como decisión deliberada, no como una omisión.

### Retención de 90 días para `AppRuleEvent`/`GeofenceEvent`, distinta de los 7 días de ubicación

Los 7 días de `DeviceLocationReport` (Sprint 13) se justifican porque esa tabla guarda coordenadas
cifradas de un menor — la categoría de dato más sensible del proyecto. Ni `AppRuleEvent` ni
`GeofenceEvent` guardan coordenadas: un bloqueo de regla es un nombre de paquete y un tipo de regla,
y una entrada/salida de geocerca es un nombre que el propio tutor eligió (`geofence_name`), no un
punto en el mapa. Sin esa presión de privacidad, se optó por una ventana más larga — un trimestre
escolar aproximado — porque un registro de auditoría demasiado corto deja de ser útil para el caso
de uso real: ¿mis reglas realmente se están aplicando?, ¿el dispositivo cruzó las zonas que esperaba
esta semana, este mes? Purgado con el mismo patrón "al escribir, sin scheduler" ya establecido.

### Purga extraída a un helper compartido (`app/services/retention.py`)

`/code-review` señaló que el patrón "purgar filas vencidas de este `device_id`, luego insertar"
había quedado duplicado casi textual en tres sitios (la purga de `DeviceLocationReport` ya
existente, y las dos nuevas de `GeofenceEvent` y `AppRuleEvent`). El riesgo real no es la
duplicación en sí, sino que una cuarta copia futura olvide el filtro por `device_id` y se
convierta silenciosamente en un barrido global — justo lo que las tres docstrings advertían
explícitamente que nunca debía pasar. Se extrajo `purge_expired_rows(db, model, time_column,
device_id, retention_days)` y los tres sitios de escritura (`location.py` ×2, `rules.py` ×1) lo
usan ahora en vez de repetir el `delete(...).where(...)`.

### Purga de `GeofenceEvent` vive en `POST /location`, no en un endpoint propio

`GeofenceEvent` no tiene su propio endpoint de escritura — se crea dentro de
`evaluate_geofence_transitions()`, llamada desde `POST /devices/{id}/location` (Sprint 14). La
purga se añadió ahí mismo, junto a la purga de `DeviceLocationReport` que ya existía, para que
ocurra en cada reporte de ubicación sin importar si esa llamada en particular termina generando un
evento de transición o no — el dispositivo reportando su propia ubicación es el único punto de
escritura natural para mantener acotada su propia tabla de eventos de geocerca.

### Índices `(device_id, occurred_at)` nuevos en ambas tablas

Ninguna de las dos tenía un índice compuesto útil para "las filas de este dispositivo más
recientes primero" — sólo un índice simple en `device_id`. Con la purga corriendo en cada escritura
y el nuevo endpoint unificado leyendo ambas tablas ordenadas por `occurred_at`, un índice compuesto
evita un sort en memoria sobre la tabla completa del dispositivo. Migración `a4d8f9c1e6b2`.
`DeviceLocationReport` ya tenía el suyo desde el Sprint 13 (`ix_device_location_reports_device_
captured`) — este sprint sólo nivela los dos que faltaban.

### Merge en Python, no un `UNION` de SQL

`AppRuleEvent` y `GeofenceEvent` no comparten columnas (una tiene `package_name`/
`rule_type_applied`, la otra `geofence_id`/`geofence_name`/`event_type`) y el endpoint ya va a
acotar la respuesta a `MAX_HISTORY_EVENTS`. Escribir un `UNION` de SQL con columnas nulas
manualmente habría sido más código para el mismo resultado que hacer dos `SELECT` ya indexados y
mezclar+ordenar las listas resultantes en Python — el volumen por dispositivo (acotado por la
propia retención de 90 días) no justifica optimizar esto en la base de datos.

## Fuera de alcance

- **Historial de navegación web**: nunca se implementó ninguna captura de navegación en este
  proyecto (ni interceptor de red, ni Accessibility Service, ni VPN local) — no hay ninguna tabla ni
  endpoint de la que "historial de web" pudiera ensamblarse. Añadirla exigiría una función nueva de
  captura, con su propia decisión de viabilidad en Android (ver
  `docs/android/capability-matrix.md`) y su propio sprint — no es una purga ni una vista sobre algo
  que ya existe, que es lo que este sprint entrega para el resto de tipos.
- **Alertas**: el catálogo `INFO/WARNING/HIGH/CRITICAL` con reglas de generación es el Sprint 17;
  no existe ninguna tabla de alertas hoy sobre la cual aplicar retención o incluir en la línea de
  tiempo.
- **Uso agregado de apps (`DeviceApplicationUsage`)**: es un agregado diario, no un evento discreto,
  y el Sprint 16 (Estadísticas) necesita conservarlo con una ventana de 30 días — aplicarle la misma
  retención de 90 días de este sprint sin coordinarlo con ese sprint habría sido prematuro. Queda
  sin tocar.
- Paginación del endpoint unificado — se acota con `MAX_HISTORY_EVENTS = 500`, mismo criterio de
  "límite de tamaño de respuesta, no de retención" que `MAX_HISTORY_REPORTS`/`MAX_GEOFENCE_EVENTS`
  ya usan.

## Backend

- `app/core/config.py`: `app_rule_event_retention_days = 90`, `geofence_event_retention_days = 90`.
- `app/models/rule.py`: índice `ix_app_rule_events_device_occurred` en `AppRuleEvent`.
- `app/models/geofence.py`: índice `ix_geofence_events_device_occurred` en `GeofenceEvent`.
- `app/services/retention.py` (nuevo): `purge_expired_rows()`, compartido por los tres sitios de
  purga (ver decisión de diseño más abajo).
- `app/api/v1/endpoints/rules.py` (`report_rule_event`): purga inline antes de insertar.
- `app/api/v1/endpoints/location.py` (`report_location`): purga de `GeofenceEvent` añadida junto a
  la purga de `DeviceLocationReport` ya existente.
- `app/schemas/history.py` / `app/api/v1/endpoints/history.py` (nuevos): `GET
  /devices/{device_id}/history`, `require_tutor_of_device`, `MAX_HISTORY_EVENTS = 500`.
- Migración `a4d8f9c1e6b2` (los dos índices nuevos). Ciclo upgrade → downgrade → upgrade verificado
  en Docker (ver evidencia).

## Web

- `apiClient.ts`: tipo `HistoryEvent` y función `listDeviceHistory`.
- `HistoryPanel` (nuevo componente): lista de eventos combinados, mismo patrón que `GeofencePanel`.
  Integrado en `DevicesPanel` junto a apps/reglas/categorías/ubicación/geocercas.

## Android

- `HistoryClient` (nuevo, `core/network`, sólo lectura): `listHistory`, sin merge local — el
  backend ya entrega la lista combinada y ordenada.
- `TutorScreen`: botón "Ver historial" por dispositivo, misma estructura que "Ver geocercas".

## Verificación

Ver `docs/sprint-15-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Ejecución de la pantalla de Android en un emulador/dispositivo físico visualizando la lista
  combinada — se compiló (`compileDebugKotlin`) y se verificó la lógica de consumo de la API contra
  el backend real por el mismo endpoint que usa el panel web (probado con la suite de integración),
  pero no se recorrió la UI de Android a mano en esta sesión.

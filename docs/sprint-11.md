# Sprint 11 — Tiempo

## Objetivo

El tutor pone un límite **semanal** de minutos a una app o a una categoría, además del diario que
ya existía (Sprint 8/10). El dispositivo supervisado reporta su zona horaria real (IANA, ej.
`America/Bogota`) para que el tutor pueda interpretar correctamente los horarios y el uso que ve —
salda la deuda anotada desde el Sprint 7 (`usage_date` sin zona horaria conocida). "Modo escolar"
es un sprint aparte (Sprint 12 en el roadmap), no entra acá.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-043 | Como tutor, quiero poner un límite semanal de minutos para una app, además del diario. |
| HU-044 | Como tutor, quiero poner un límite semanal de minutos para una categoría completa. |
| HU-045 | Como tutor, quiero ver en qué zona horaria está cada dispositivo, para interpretar bien sus horarios y su uso. |
| HU-046 | Como usuario supervisado, quiero que la semana de mi límite empiece el lunes, igual que ya funcionan los horarios por día de la semana. |

## Criterios de aceptación

1. `AppRule` y `CategoryRule` ganan el tipo `WEEKLY_LIMIT` con su propio campo
   `weekly_limit_minutes` (no se reutiliza `daily_limit_minutes`: son columnas independientes,
   igual que `SCHEDULE` ya tiene las suyas — ver decisiones de diseño).
2. El dispositivo calcula el uso de la semana calendario actual (lunes 00:00 hora local hasta
   ahora) para el paquete evaluado, con el mismo mecanismo ya usado para el total diario
   (`UsageStatsManager`, sólo cambia el rango de fechas).
3. `WEEKLY_LIMIT` cuenta como aprobación mientras no se alcance el límite, igual que
   `DAILY_LIMIT` (modo lista blanca, Sprint 9) — misma lógica, ahora con dos contadores en vez de
   uno.
4. `devices` gana `timezone` (identificador IANA, nullable — un dispositivo que no reportó
   todavía no tiene uno), reportado en cada heartbeat igual que `app_version`/`os_version`.
5. El panel web y la app del tutor muestran la zona horaria del dispositivo donde ya se muestra su
   estado.
6. Sin normalización retroactiva de `usage_date` histórico ni de las franjas de `SCHEDULE` ya
   guardadas — conocer la zona horaria del dispositivo es la base para interpretarlas bien de acá
   en adelante; recalcular el pasado no es necesario para ningún criterio de este sprint.

## Decisiones de diseño relevantes

- **`weekly_limit_minutes` es una columna nueva, no un rename de `daily_limit_minutes`.** Renombrar
  hubiera significado tocar los endpoints, schemas y las suites de prueba completas de los
  Sprints 8, 9 y 10 sin necesidad real — el proyecto ya tiene precedente de una columna dedicada
  por tipo de regla (`schedule_start_minute`/`schedule_end_minute`/`schedule_days_mask` sólo para
  `SCHEDULE`), así que una columna dedicada para `WEEKLY_LIMIT` sigue exactamente ese patrón en vez
  de inventar uno nuevo.
- **Semana calendario (lunes a domingo), no una ventana móvil de 7 días.** Coherente con el
  `schedule_days_mask` que ya existe (bit 0 = lunes) y con lo que un tutor esperaría al leer "límite
  semanal" — no una ventana que cambia de significado según el momento del día en que se consulta.
- **La zona horaria se guarda, no se usa todavía para recalcular nada retroactivamente.** El uso
  diario/semanal y las franjas de horario se siguen evaluando en la hora local *actual* del propio
  dispositivo (`LocalDateTime.now()` en Android, que ya refleja la zona horaria vigente del
  teléfono) — eso ya era correcto desde el Sprint 8. Lo que faltaba era que el *backend* supiera en
  qué zona horaria vive cada dispositivo, para que el tutor pueda interpretar lo que ve (por
  ejemplo, "bloqueado 22:00–06:00" significa algo distinto en cada zona horaria). Guardar el dato es
  la corrección real; no hacía falta reescribir la lógica de evaluación, que nunca estuvo mal.
- **Sin Fase C nueva.** `UsageStatsManager` con un rango de fechas distinto no es una capacidad
  nueva (ya verificada en el Sprint 7/8), y `TimeZone.getDefault().getID()` es una API estándar de
  Java sin restricciones de plataforma que verificar — no hay nada no obvio que investigar antes de
  implementar.

## Fuera de alcance

- "Modo escolar" — Sprint 12, con su propio incremento.
- Normalizar retroactivamente `usage_date` o las franjas de `SCHEDULE` ya guardadas contra la zona
  horaria del dispositivo — no lo pide ningún criterio de este sprint (ver arriba).
- Límite semanal a nivel de dispositivo completo (todas las apps sumadas) — sólo por app y por
  categoría, igual que el resto del motor de reglas hasta ahora.

## Backend

- `app_rules`/`category_rules` ganan `weekly_limit_minutes` (columna propia, no un rename de
  `daily_limit_minutes`), con su propio `CheckConstraint`. `rule_type` amplía su lista válida a
  `WEEKLY_LIMIT`. Migración `9fa64cf938a5`, con el reemplazo de `CheckConstraint` escrito a mano
  (mismo motivo que en el Sprint 10: autogenerate no detecta el cambio del cuerpo de un constraint
  existente).
- `devices` gana `timezone` (migración en el mismo archivo). `POST /devices/{id}/heartbeat` la
  actualiza igual que `app_version`/`os_version`; `GET /devices` y `GET /devices/{id}` la exponen.

## Android

- `RuleType`/`BlockReason` ganan `WEEKLY_LIMIT`; `AppRule`/`CategoryRule` ganan
  `weeklyLimitMinutes`.
- `AppInventoryCollector.collectWeekUsage()`: mismo mecanismo que `collectTodayUsage()`, sólo
  cambia el inicio del rango (lunes 00:00 hora local en vez de hoy 00:00).
- `RuleEvaluator.evaluate()` recibe ahora también el uso semanal y compara `WEEKLY_LIMIT` contra
  ese total, independiente del contador diario.
- `SupervisedScreen` reporta `TimeZone.getDefault().id` en cada heartbeat.
- `TutorScreen` y el panel web muestran la zona horaria del dispositivo junto a su plataforma.

## Verificación

Backend: **131 pruebas pasan** (6 nuevas: `WEEKLY_LIMIT` para `AppRule` y `CategoryRule` con su
validación, y zona horaria reportada/ausente en el heartbeat), verde dos veces seguidas en
contenedor limpio, `ruff check` limpio. Ciclo upgrade → downgrade → upgrade verificado, con `psql`
confirmando que el `CheckConstraint` de `weekly_limit_minutes` rechaza filas sin minutos y acepta
las que sí los traen.

Android: `./gradlew test assembleDebug assembleRelease` — **27 pruebas** en `RuleEvaluatorTest` (24
previas + 3 nuevas: `WEEKLY_LIMIT` bajo el límite, en el límite exacto, e independiente del uso
diario), ambos APK empaquetan.

Web: `npm run lint` (`--max-warnings=0`) y `npm run build` (TypeScript sin errores) en verde.

Pendiente, mismo límite de siempre: verificación end-to-end con login real de Google.

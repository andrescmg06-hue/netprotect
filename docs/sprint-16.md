# Sprint 16 — Estadísticas

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 15) pide, para este sprint, agregaciones por hoy / 7 días
/ 30 días — apps más usadas, categorías, bloqueos y cumplimiento — con "consultas indexadas o
tablas de agregación si el volumen lo exige". Todo lo que este sprint necesita ya existe como
datos: `DeviceApplicationUsage` (uso diario por app, Sprint 7), `AppCategoryAssignment` (categoría
por paquete, Sprint 10), `AppRuleEvent` (bloqueos aplicados, con índice `(device_id, occurred_at)`
desde el Sprint 15) y `AppRule`/`CategoryRule` (límites configurados, Sprint 8/10). Este sprint no
crea ninguna tabla nueva: sólo agrega esos datos al vuelo por periodo.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-060 | Como tutor, quiero ver las apps más usadas de un dispositivo en el periodo que elija (hoy/7 días/30 días), para saber en qué se va el tiempo. |
| HU-061 | Como tutor, quiero ver ese mismo tiempo desglosado por categoría, sin tener que sumarlo app por app. |
| HU-062 | Como tutor, quiero ver cuántos bloqueos se aplicaron en el periodo y por qué motivo, para saber si mis reglas realmente están actuando. |
| HU-063 | Como tutor, quiero ver, para las apps y categorías con un límite diario configurado, qué porcentaje de los días del periodo se mantuvieron dentro de ese límite. |

## Criterios de aceptación

1. `GET /devices/{device_id}/statistics?period=today|7d|30d` devuelve, para el tutor dueño del
   dispositivo, las apps más usadas (hasta `MAX_TOP_APPS = 10`, ordenadas por tiempo total
   descendente), el desglose por categoría, el conteo de bloqueos por `rule_type_applied` y el
   cumplimiento de las reglas `DAILY_LIMIT`; un tutor ajeno o el propio supervisado reciben 404
   (mismo patrón `require_tutor_of_device` de siempre).
2. Una app con uso pero sin categoría asignada se agrupa bajo `category: null`, no se descarta.
3. El cumplimiento sólo se calcula para reglas `DAILY_LIMIT` (de `AppRule` y `CategoryRule`);
   `WEEKLY_LIMIT` y `SCHEDULE` no aparecen en `compliance`.
4. Un día sin uso reportado (de la app, o de ninguna app de la categoría) no cuenta ni a favor ni
   en contra de `days_evaluated`/`days_compliant`.
5. El panel web y la pantalla del tutor en Android muestran los cuatro bloques, con selector de
   periodo.

## Decisiones de diseño y su motivo

### Sin tabla de agregación nueva

El volumen por dispositivo es bajo: como mucho unas pocas decenas de apps × 30 filas de
`DeviceApplicationUsage`. Sumar eso con un `GROUP BY` en Python sobre las filas ya cargadas es más
simple que mantener una tabla de agregación sincronizada, mismo razonamiento que llevó al Sprint 15
a no crear una tabla de "historial" nueva. `AppRuleEvent` ya tiene el índice compuesto
`(device_id, occurred_at)` del Sprint 15, así que el filtro por periodo de los bloqueos ya está
indexado sin tocar nada. `DeviceApplicationUsage` sólo tiene índice simple en `device_id` — eso ya
acota la consulta a las filas de un único dispositivo, el mismo orden de magnitud que
`applications.py::list_device_applications` ya acepta sin índice extra desde el Sprint 7.

### Periodos como fechas UTC del servidor, no del huso horario del dispositivo

`usage_date` es, desde el Sprint 7, una etiqueta opaca que pone el dispositivo — el servidor nunca
la recalcula contra su huso horario real. Este sprint mantiene ese criterio en vez de introducir
una normalización de zona horaria sólo para estadísticas: `today` es la fecha UTC actual del
servidor, `7d`/`30d` son esa fecha menos 6/29 días. Se documenta como aproximación conocida, del
mismo tipo que la latencia de 15 minutos ya aceptada para las geocercas (Sprint 14).

### Cumplimiento sólo para `DAILY_LIMIT`, y sólo sobre días con dato reportado

`WEEKLY_LIMIT` es un total del periodo completo, no una cantidad por día — no hay un "día
cumplido/incumplido" que calcular. `SCHEDULE` no es un límite de cantidad. Ambos quedan fuera del
cálculo de cumplimiento, mismo criterio de exclusión que el Sprint 15 ya aplicó a estos dos tipos
de regla frente a su propia retención. Dentro de `DAILY_LIMIT`, un día sin ninguna fila de
`DeviceApplicationUsage` para la app (o para ninguna app de la categoría) no se cuenta ni como
cumplido ni como incumplido — no hay evidencia de qué pasó ese día, y tratarlo como cumplimiento
premiaría la falta de reporte tanto como cumplir de verdad.

### Regla de categoría: se suma el uso de todas sus apps miembro

Un límite diario de categoría (p. ej. "máximo 60 min/día de JUEGOS") se mide sobre la suma del uso
de todas las apps asignadas a esa categoría ese día, no sobre cada app por separado — es la lectura
natural de "límite de la categoría", coherente con cómo `CategoryRule` ya se evalúa en Android
(Sprint 10): una app sin `AppRule` propia cae bajo la regla de su categoría como conjunto.

### Sin librería de gráficos

`frontend/package.json` no tiene ninguna (`next`/`react`/`react-dom` solamente) y ningún panel
existente dibuja gráficos — todos son listas de texto (`HistoryPanel`, `GeofencePanel`,
`AppsList`). Se mantiene esa misma estética en vez de introducir una dependencia nueva para un solo
panel.

## Fuera de alcance

- **Tabla de agregación materializada**: el volumen no la justifica (ver decisión de diseño
  arriba); si el volumen creciera, sería un cambio a aislar en un sprint propio, no un ajuste
  incidental de este.
- **Estadísticas de ubicación/geocercas**: ya tienen sus propias vistas dedicadas
  (`DeviceLocationPanel`, `GeofencePanel`); mezclarlas aquí duplicaría UI ya existente.
- **Edición de reglas desde la pantalla de estadísticas**: sigue siendo de sólo lectura, mismo
  criterio ya establecido para historial y geocercas en Android — crear/editar reglas es sólo del
  panel web.
- **Alertas**: catálogo `INFO/WARNING/HIGH/CRITICAL` es el Sprint 17.

## Backend

- `app/schemas/statistics.py` (nuevo): `StatisticsPeriod`, `TopAppEntry`, `CategoryTotalEntry`,
  `BlockCountEntry`, `ComplianceEntry`, `DeviceStatisticsResponse`, `MAX_TOP_APPS = 10`.
- `app/api/v1/endpoints/statistics.py` (nuevo): `GET /devices/{device_id}/statistics?period=...`,
  protegido con `require_tutor_of_device`. Sin migración: no se tocó ningún modelo.
- `app/api/v1/router.py`: registro del nuevo router.

## Web

- `apiClient.ts`: tipos y `getDeviceStatistics`.
- `StatisticsPanel` (nuevo componente): selector de periodo + cuatro listas, integrado en
  `DevicesPanel` junto a los demás paneles del dispositivo.

## Android

- `StatisticsClient` (nuevo, `core/network`, sólo lectura).
- `TutorScreen`: botón "Ver estadísticas" por dispositivo con selector de periodo, mismo patrón de
  estado por dispositivo que historial/geocercas.

## Verificación

Ver `docs/sprint-16-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Ejecución de la pantalla de Android en un emulador/dispositivo físico visualizando las
  estadísticas — se compiló (`compileDebugKotlin`) y se verificó la lógica de consumo de la API
  contra el backend real por el mismo endpoint que usa el panel web (probado con la suite de
  integración), pero no se recorrió la UI de Android a mano en esta sesión.

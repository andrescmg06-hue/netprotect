# Sprint 37 — Estadísticas, Alertas y Silenciadas

Séptimo sprint de `docs/planning/plan-frontend.md`.

## Qué se hizo

- **`ChartCard`/`DonutChart`/`BarList`/`ProgressBar`** (compartidos, nuevos, en `components/ui/`,
  T4 del plan): SVG propio, sin librería de gráficos. `DonutChart` dibuja cada segmento como un
  `<circle>` completo recortado con `stroke-dasharray`/`stroke-dashoffset` y rotado -90°; `BarList`
  y `ProgressBar` reusan el patrón `transform: scaleX` (no `width`) que `PairingPanel` ya adoptó en
  el Sprint 34 para no disparar *layout thrash*. `ChartCard` formaliza el bloque
  "Card + CardHeader + cuerpo con padding (+ estado vacío)" que las tres vistas repetían.
- **`lib/categoryFormatting.ts`** (nuevo): `CATEGORIES`/`CATEGORY_LABELS`/`CATEGORY_ICONS` salen de
  `DeviceCategoriesPanel` (que los tenía locales desde el Sprint 10) porque `StatisticsPanel` los
  necesita también para el donut de "Uso por categoría" — habrían quedado duplicados por segunda
  vez, así que se extraen ahora en vez de copiarlos.
- **`lib/alertFormatting.ts`** (nuevo): tono/icono/etiqueta por `AlertLevel`, `alertLabel()` (movida
  tal cual desde `AlertsPanel`) y `silenceLabel()`, que resuelve el `dedup_key` de una
  `AlertSilence` (que sólo trae `"{alert_type}:{package_name}"` o `"{alert_type}:{geofence_id}"`, ver
  `backend/app/services/alerts.py`) al nombre real de la app o la geocerca.
- **Estadísticas**: selector de periodo (Hoy/7 días/30 días), métricas (tiempo total, apps con
  uso, bloqueos, cumplimiento promedio), donut de uso por categoría, barras de apps más usadas y de
  bloqueos por tipo, y una fila de `ProgressBar` por regla `DAILY_LIMIT` con su tasa de
  cumplimiento real. Mismo `GET /devices/{id}/statistics` del Sprint 16, sin cambios de contrato.
- **Alertas**: métricas por nivel (Info/Advertencia/Alta/Crítica), filtro por nivel, y un
  maestro-detalle (lista a la izquierda, detalle con Marcar leída/Silenciar a la derecha) en vez
  de la lista plana anterior — mismo patrón de estado local que `DevicesPanel` desde el Sprint 34.
  Sin calendario de silencios (no existe en el backend — regla 1 del plan): "Silenciar" sigue
  siendo indefinido, como desde el Sprint 17.
- **Silenciadas**: misma pestaña de `AlertsPanel` (`view="silenced"`), ahora sobre `DataTable` con
  `silenceLabel()` resolviendo cada fila a algo legible ("Salidas de Casa" en vez de
  `GEOFENCE_EXIT:<uuid>`).

## Decisiones

- **`SegmentedControl` (compartido) gana scroll horizontal propio** (`overflow-x: auto`,
  `flex-shrink: 0` en cada opción): el filtro de 5 niveles de Alertas desbordaba la página en
  390 px (7 px de más). Se corrigió en el componente base, no sólo en este panel, porque cualquier
  otro filtro con más opciones de las que caben en móvil tendría el mismo problema.
- **Sin gráfico de serie temporal**: el backend agrega por periodo (hoy/7d/30d), no devuelve una
  serie por día — un donut y dos listas de barras cubren exactamente esa forma de dato sin inventar
  un eje que no existe.

## Verificaciones

`impeccable detect --json` sobre los 16 archivos del sprint: sin hallazgos. ESLint, `tsc` y
`next build` en verde.

Datos reales sembrados contra `compose.test.yaml`: un dispositivo emparejado con 5 apps
categorizadas y su uso real de hoy sincronizado (`POST /applications/sync`), una regla
`DAILY_LIMIT` de app (Instagram, incumplida: 90 min de uso contra 60 de límite) y una de categoría
(Educación, cumplida: 15 min contra 30), tres eventos de regla aplicada y una geocerca con
entradas/salidas reales — que generaron sus propias alertas reales (`record_alert_for_*`, Sprint
17), nunca insertadas a mano. Capturas reales a 1440 y 390 px en las tres vistas; sin
desbordamiento horizontal tras la corrección del `SegmentedControl`.

Playwright (script temporal, borrado al cerrar el sprint) ejecutó 11 acciones reales contra ese
backend: cambiar de periodo en Estadísticas y "Actualizar", seleccionar una alerta distinta en el
maestro-detalle, "Marcar leída" (verificado el cambio de estado), filtrar por nivel "Advertencia",
"Silenciar" una alerta y verla aparecer en Silenciadas con su nombre de geocerca resuelto, y
"Reactivar" para quitarla de ahí — las 11 aserciones pasaron contra respuestas reales de la API.

## Tropiezo

La primera pasada de capturas mostró "Cargando…" congelado en Silenciadas a 390 px. No era un
bug: la vista hace tres llamadas (alertas, silencios y geocercas, esta última sólo para resolver
`silenceLabel()`) y el script de capturas sólo esperaba 700+400 ms — muy poco contra el puerto
publicado de Docker en Windows, que la nota de rendimiento de `CLAUDE.md` ya documenta en ~1.4 s
por petición. Con una espera de 3 s confirmado en un script de depuración aparte, la vista carga
y resuelve el nombre de la geocerca correctamente; se subió la espera del script de capturas y
quedó confirmado con capturas limpias.

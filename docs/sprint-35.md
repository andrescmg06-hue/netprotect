# Sprint 35 — Reglas, Política y horario escolar, Categorías

Quinto sprint de `docs/planning/plan-frontend.md`.

## Qué se hizo

- **`RuleTypeFields`** (compartido, nuevo): el formulario "tipo de regla + minutos/horario+días"
  estaba duplicado byte a byte entre `AppRulesPanel` y `DeviceCategoriesPanel` desde el Sprint 24.
  Ahora es un solo componente controlado (`ruleType`, `dailyLimitMinutes`, `weeklyLimitMinutes`,
  `scheduleStart`, `scheduleEnd`, `scheduleDaysMask`) más `validateRuleTypeFields()`, que devuelve
  o un error o los campos extra listos para el `UpsertAppRuleInput`/`UpsertCategoryRuleInput` de
  cada panel. `src/lib/ruleFormatting.ts` junta las funciones puras que también estaban repetidas
  (`ruleTypeLabel`, `describeRule`, conversión de horas↔minutos).
- **Reglas por aplicación**: métricas reales (activas, bloqueadas, con límite, con horario,
  contadas sobre las reglas ya cargadas), tabla de reglas activas y del historial de bloqueos
  sobre `DataTable`, formulario de creación con `RuleTypeFields`. Misma actualización en vivo del
  Sprint 18 (`useDeviceRulesRealtime`).
- **Política y horario escolar**: el modo por defecto pasa a dos tarjetas seleccionables (con
  marca de verificación en la activa) en vez de un botón que alterna texto. El horario escolar
  usa el `Switch` ya existente; al activarlo valida los campos antes de llamar al backend — si
  fallan, el interruptor no se mueve, porque su estado sigue viniendo de `schoolMode.enabled`, no
  de un estado local optimista. Se agregó una franja de 24 horas (`DayTimeline`, nuevo, local a
  este panel) que resalta la ventana configurada — incluido el caso de que cruce la medianoche.
- **Categorías**: 11 tarjetas con el conteo real de apps asignadas y la regla vigente (o "Sin
  regla"), formulario de asignación app→categoría y de regla por categoría, ambos sobre
  `RuleTypeFields`/`DataTable`.

## Decisiones

- **Nada de "activar/desactivar" con casillas por fila** en Reglas o Categorías, aunque los
  mockups las insinúan — el backend solo tiene crear y eliminar.
- **El resumen de 24 horas es solo visual.** No es un reloj ni un indicador en vivo; se calcula
  una sola vez a partir de los mismos minutos que ya se muestran en texto.

## Verificaciones

`impeccable detect --json` sobre los 9 archivos: sin hallazgos. ESLint, `tsc`, `next build` en
verde. Capturas reales (reglas, categorías y modo escolar activados de verdad vía la API, no
inventados) a 1440 y 390 px; sin desbordamiento horizontal en ninguna de las tres pantallas.

## Tropiezo

El E2E real volvió a fallar tras el rediseño, por segunda vez en dos sprints seguidos: el nuevo
campo "Aplicación (paquete)" de Reglas trae una pista `Ej. com.instagram.android`, y el paquete de
prueba que siembra `global-setup.ts` es justo `com.instagram.android` — `getByText(packageName)`
pasó a encontrar dos coincidencias (la fila real y la pista del formulario). Corregido con
`{ exact: true }`, que sólo iguala el texto completo de la fila.

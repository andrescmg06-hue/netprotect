# Sprint 56 — Rediseño editorial del panel web, reglas y horarios: Apps, Reglas, Política, Categorías

Quinto sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Código de Cristian del 04/10/2026, commit `bdfa6e8`
(10:03). Documento escrito **después**, en el S60 (T1), a partir del mensaje y de las estadísticas del commit y de
`docs/redesign/s59/LEEME.md`.

## Objetivo

Que las cuatro vistas relacionadas con reglas dejen la rejilla «métricas arriba, tarjetas abajo» y se lean como
secciones abiertas separadas por líneas sobre la página crema, con datos reales de la API únicamente.

## Vistas recompuestas

| Vista (`SectionKey`) | Qué cambió (según el commit) |
|---|---|
| Apps (`apps`) | Una frase de resumen en lugar de tres tarjetas; lista con línea (ficha con letra, nombre, paquete, marca de sistema, último uso con su fecha como regla fina de tinta); las apps desinstaladas en su propia sección, más discreta; estado vacío con iconos de línea que explica la sincronización. |
| Reglas (`rules`) | Centro de control: las reglas son filas filtradas por sus cinco tipos reales (los límites diario y semanal quedan separados); el formulario va al lado en escritorio y debajo en móvil. El inventario del dispositivo da nombre legible a las filas y un `datalist` al campo del paquete; el paquete se sigue pintando **una sola vez** por fila (lo exige el e2e) y `loadRules` conserva su identidad. |
| Política y horario (`policy`, mockup 05) | Dos secciones: el modo por defecto es una elección tranquila de dos opciones; el modo escolar se edita sobre la `ScheduleBar` única de 24 h, con los campos de hora como alternativa exacta. Si se cambia la ventana con el modo activo, se guarda por el mismo manejador y la misma carga útil de «activar»; el estado se siembra todavía desde props. |
| Categorías (`categories`) | Primero las categorías con apps o regla, con su regla y las apps quitables; las sin uso, en una línea compacta. Los formularios no cambian. |

`RuleTypeFields`: filas de radio nativas con el marcador de color del tipo; el tipo «horario» también usa `ScheduleBar`.
El mapa tipo de regla → tono de insignia vive ahora una sola vez en `ruleFormatting` (las categorías ganan el violeta
de horario).

## Decisiones

- **`ScheduleBar` de una sola ventana**: la API tiene una ventana y una máscara de días, así que **no** hay filas por
  día, plantillas ni «copiar a todos» (el plan, §4 S56, ya preveía omitirlos si la API no los permitía). La pieza nació
  en el S54 (`35af89d`) y se conecta aquí.
- **Accesibilidad de teclado**: los selectores de hora se conservan como alternativa a la barra (riesgo anotado en el plan).
- No se cambia ninguna llamada a la API ni la lógica de negocio.

## Tareas

Ruta: no consta en las fuentes; no se inventa.

- [x] T1 — Apps: `DeviceApplicationsList` recompuesta (`bdfa6e8`).
- [x] T2 — Reglas: `AppRulesPanel` y `RuleTypeFields` recompuestos (`bdfa6e8`).
- [x] T3 — Política y horario: `DevicePolicyPanel` con `ScheduleBar` (`bdfa6e8`).
- [x] T4 — Categorías: `DeviceCategoriesPanel` recompuesto (`bdfa6e8`).
- [x] T5 — Mapa de tonos de regla único en `lib/ruleFormatting.ts` (`bdfa6e8`).
- [x] T6 — E2E del conjunto S54–S59 en verde con el build de producción servido a mano: «1 passed, 15,7 s»
  (`docs/redesign/s59/LEEME.md`). Comprueba, entre otras cosas, que `com.instagram.android` sale una sola vez (regla
  de paquete único de esta vista).
- [ ] T7 — `npm run lint` y `tsc`: **no hay registro** para este commit (el mensaje no los menciona, a diferencia de S54 y
  S55). El build de producción se generó para el e2e, pero no se guardó su salida.
- [ ] T8 — Capturas a 1440 y 390 px de cada vista en el repo. Hay `rules-1440`, `policy-1440`, `policy-390` y
  `categories-1440`; **no hay ninguna de `apps`** y faltan `rules-390` y `categories-390`. Ver `docs/sprint-56-evidence.md`.
- [x] T9 — Documentación de seguimiento y evidencia (este archivo y `docs/sprint-56-evidence.md`), escrita en el S60.

## Pendiente conocido

- Sin captura de Apps en el repo ni con datos reales registrados en el LEEME: se sembraron 11 apps con uso, pero la
  vista no está entre las 18 capturas guardadas.
- No se probó con arrastre real de la `ScheduleBar` (el LEEME no lo registra); solo hay captura estática de Política.
- El LEEME declara que el contraste no se midió con herramienta, solo a ojo.
- Si el modo escolar se guardó de verdad desde la barra contra la API, no consta: el LEEME dice que la política y el
  modo escolar se sembraron por la API, no desde la interfaz.

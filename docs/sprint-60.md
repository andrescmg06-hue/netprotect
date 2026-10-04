# Sprint 60 — Rediseño editorial del panel web, cierre

Último sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Rama `sprint-60-rediseno-web-cierre`, creada desde
`fix/s54-auth-log-sin-pii` (`e9e4f69`), que contiene el código S53–S59 de Cristian más el arreglo del log de auth.
Continúa el trabajo donde él lo dejó el 04/10/2026: el dueño lo termina y deja todo correcto y al día; después hace push
y Cristian sigue sobre esa base.

## Objetivo

Dejar el rediseño terminado y la documentación al día: accesibilidad (contraste AA, foco visible, movimiento reducido),
favicon, peso de imágenes, estados vacíos coherentes, e2e en verde y registro fiel de S54–S59.

## Punto de partida (mapeo de solo lectura, 04/10/2026)

Lectura estática del código; lo marcado «inferido» hay que probarlo en navegador antes de darlo por cierto.

- Ya está: anillo de foco global (`globals.css:157`), regla global de `prefers-reduced-motion` (`globals.css:222-230`),
  ninguna lista sin estado vacío, e2e compatible con el rediseño (S59 registra «1 passed»).
- Confirmado por cálculo: contador de alertas del sidebar blanco sobre `#f04438` = 3,76:1 (necesita 4,5).
- Confirmado: `src/app/icon.png` es el logo de 219×256 y 54 KB, sin `.ico` ni tamaños estándar; `public/brand/card-twilight.jpg`
  (147 KB) no se usa en ningún sitio.
- Confirmado: faltan `docs/sprint-55..59.md` y los `-evidence.md` de S54–S59; `docs/progress.md` dice «Sprint 53»;
  el README llega a «Alcance del Sprint 53» y aún dice Playfair.
- Inferido: el `box-shadow` propio de `Button.primary` y de la fila seleccionada de Alertas puede tapar el anillo de
  foco; el anillo desaparece en modo de alto contraste (`forced-colors`); bordes de controles con 1,3–1,45:1 donde el
  borde es el único límite.

## Tareas

Ruta de cada tarea: inline, o delegada si se dispara un disparador (4+ ficheros a entender, 2+ ficheros no triviales a
escribir). Un commit por unidad de trabajo, con el porqué. Se marca solo con evidencia observada.

- [x] T1 — Documentación de S54–S59 al día: cerrar `sprint-54.md` (T5/T6), crear `sprint-55..59.md` y sus `-evidence.md`
  a partir de `docs/redesign/s59/LEEME.md` y del historial de Cristian, actualizar `docs/progress.md` y
  `docs/redesign/CONTINUAR.md` §1, `## Alcance del Sprint 54..59` en el README y corregir el del 53 (Newsreader).
  Ruta: delegada (preparación de escritura sobre 10+ ficheros). Commit `c307652`. Revisado: todas las capturas
  citadas existen, T5 de S54 sigue sin marcar y no hay atribución de IA en las líneas añadidas.
  Fuera de alcance, queda abierto: `CONTINUAR.md` §5 «Cómo empezar» aún habla de arrancar el S52.
- [x] T2 — Contraste. Cambiado: contador del sidebar (`Sidebar.module.css`, blanco sobre `--color-danger-text`: 3,76 → 5,75:1)
  y pista de `ScheduleBar` (borde a `--color-border-strong`: 3,64:1 sobre blanco, 3,23:1 sobre su relleno). Ruta: inline.
  Revisados y **sin cambio**, con motivo: `.signOut` (el texto, 7,87:1, identifica el botón), `DayPicker` (lleva texto y el
  estado activo cambia de relleno), `DeviceSelector` (tarjeta con icono, texto y sombra), `DevicePolicyPanel` (la selección
  la marca un check además del borde). Los puntos de estado de 8 px (`#16b364` 2,74:1, `#f79009` 2,35:1) no llevan texto
  encima y no son el único portador del estado: se revisan en la pasada de teclado y contraste de T7 (no hay medición automática todavía).
  Evidencia: razones calculadas con la fórmula de luminancia WCAG. **Pendiente:** build y revisión visual de los dos cambios
  (se reconstruye el web una sola vez al cerrar T6, para no interrumpir tu revisión en el navegador).
- [x] T3 — Foco (cambio hecho; **sin comprobación visual**). Ruta: inline. `Button.primary` y `.rowSelected` de Alertas
  fijaban su propio `box-shadow` con la misma especificidad (0,1,0) que el `:focus-visible` global, y las hojas de módulo
  cargan después: el anillo se perdía por orden de cascada. Se añadió `.primary:focus-visible` (0,2,0) y
  `.rowSelected:focus-visible`, que conserva la raya navy de selección y suma el anillo; la regla gana por especificidad,
  no por orden. Además, un bloque `@media (forced-colors: active)` en `globals.css` da un `outline` real, porque el modo de
  alto contraste de Windows elimina los `box-shadow`. Revisados el resto de `box-shadow` propios: son sombras de
  ventanas emergentes o la bolita del Switch, no el elemento enfocable.
  **No se pudo probar con Tab en un navegador:** la extensión de Chrome no estaba conectada y `/design-system` da 404 en el
  build de producción. Pendiente: Tab sobre un botón primario y una fila seleccionada (la hace el dueño), y probar el
  modo de alto contraste de Windows.
- [ ] T4 — Favicon: `favicon.ico`, icono cuadrado y `apple-icon` desde `logo-shield`. Ruta: inline.
- [ ] T5 — Imágenes: retirar o usar `card-twilight.jpg`, aligerar logos, comprobar `sharp` en el contenedor `web`,
  valorar la carga `eager` de la banda. Ruta: inline.
- [ ] T6 — Estados vacíos: unificar `EmptyState` frente a `<p>` sueltos (AppRulesPanel, GeofencePanel,
  DeviceLocationPanel, DeviceCategoriesPanel). Ruta: inline.
- [ ] T7 — e2e: login visible, prueba de teclado/foco y vistas S56–S59. Ruta: inline.
- [ ] T8 — Verificación y cierre: `npm run lint && npm run build` y e2e vía `verifier`, `docs/sprint-60-evidence.md`,
  `/cerrar-sprint 60`.

## Pendiente conocido

- Revisión visual del panel con sesión real de Google (T5 de S54): la hace el dueño; no se puede simular.
- Decisión de diseño abierta: Spinner con movimiento reducido (hoy queda estático; el `label` es el único indicador).
- Serif Newsreader «sigue abierta para el diseñador» (S54).
- El pase `web-redesign` → `main` lo decide el dueño; no se hace push, merge ni PR sin pedirlo.

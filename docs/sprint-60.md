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
  Evidencia: razones calculadas con la fórmula de luminancia WCAG. Build: la imagen `web` se reconstruyó con éxito y el CSS
  servido por el contenedor contiene `.Sidebar…count{background:var(--color-danger-text)}`. **Pendiente:** revisión visual
  de los dos cambios (la hace el dueño).
- [x] T3 — Foco (cambio hecho; **sin comprobación visual**). Ruta: inline. `Button.primary` y `.rowSelected` de Alertas
  fijaban su propio `box-shadow` con la misma especificidad (0,1,0) que el `:focus-visible` global, y las hojas de módulo
  cargan después: el anillo se perdía por orden de cascada. Se añadió `.primary:focus-visible` (0,2,0) y
  `.rowSelected:focus-visible`, que conserva la raya navy de selección y suma el anillo; la regla gana por especificidad,
  no por orden. Además, un bloque `@media (forced-colors: active)` en `globals.css` da un `outline` real, porque el modo de
  alto contraste de Windows elimina los `box-shadow`. Revisados el resto de `box-shadow` propios: son sombras de
  ventanas emergentes o la bolita del Switch, no el elemento enfocable.
  Comprobado en el CSS que sirve el contenedor reconstruido: contiene `.Button…primary:focus-visible{box-shadow:var(--focus-ring)}`,
  `.AlertsPanel…rowSelected:focus-visible{box-shadow:inset 2px 0 0 …, var(--focus-ring)}` y el bloque `forced-colors`.
  **No se pudo probar con Tab en un navegador:** la extensión de Chrome no estaba conectada y `/design-system` da 404 en el
  build de producción. Pendiente: Tab sobre un botón primario y una fila seleccionada (la hace el dueño), y probar el
  modo de alto contraste de Windows.
- [x] T4 — Favicon. Ruta: inline. Generados desde `public/brand/logo-shield.png` (219×256, no cuadrado) con Pillow,
  escudo centrado sobre lienzo cuadrado (LANCZOS): `src/app/icon.png` 192×192, 256 colores, 54 KB → 5,3 KB (con ligero
  bandeado en el degradado a 192 px, imperceptible a tamaño de pestaña); `src/app/favicon.ico` con 16, 32 y 48 px (7 KB);
  `src/app/apple-icon.png` 180×180 sobre `--color-bg` crema (iOS rellena en negro la transparencia; 16 KB). Next los sirve
  por convención de nombres, sin tocar `layout.tsx`. Revisados a la vista. **Pendiente:** verlos en la pestaña del
  navegador tras reconstruir el web (la caché de favicon suele exigir Ctrl+F5 o reabrir la pestaña).
- [x] T5 — Imágenes (cambio hecho; **pesos nuevos sin medir**). Ruta: inline.
  Comprobado en el contenedor `web`: el optimizador de Next funciona (`sharp` está), y banda y login ya pasan por él
  (`study.jpg` 186 KB → 36 KB a 640 px; `band-alpine.jpg` 225 KB → 52 KB a 1080 px; medido con `curl` a `/_next/image`).
  Cambiado: los tres logos (`ui/Logo.tsx`, ~100 KB cada uno, mostrados a ~44 px de alto) dejan `unoptimized`: su
  justificación («no hace falta tocar la CSP») no se sostiene, porque `/_next/image` es el mismo origen y la banda ya
  funciona con la CSP actual. Se pide `quality={90}` para que el texto de la marca no se degrade; Next 16 solo permite
  75 por defecto y redondea cualquier otro valor en silencio, así que `next.config.ts` declara `images.qualities: [75, 90]`
  (comprobado en `node_modules/next/dist/docs`). `tsc` y `eslint` limpios sobre los dos ficheros.
  **Decididos sin cambio:** `card-twilight.jpg` (147 KB, sin uso) se conserva: es un asset del paquete de diseño y S54
  decidió no usar la tarjeta de foto del pie del sidebar; borrarlo es decisión del dueño. La banda sigue `eager`: va sobre
  el pliegue en cada vista y el optimizador ya la reduce.
  **Medido tras reconstruir el `web` (desde `np-s54`):** `logo-full.png` de 103 981 B pasa a 8 263 B (`w=256`) y 29 480 B
  (`w=640`), ambos `200 image/png`, con `q=90` aceptado. Los iconos de T4 se sirven: `/icon.png` 5 339 B, `/favicon.ico`
  7 134 B, `/apple-icon.png` 15 945 B (todos `200`) y el HTML trae los tres `<link rel>` con sus `sizes`.
  **Pendiente (a la vista, del dueño):** mirar los logos a 1× y 2×; si el texto se ve blando, subir a 100 o volver a
  `unoptimized`.
- [x] T6 — Estados vacíos: **revisado, sin cambios de código** (decisión deliberada). Ruta: inline. El mapeo marcaba cinco
  `<p>` sueltos frente a `EmptyState`; leídos en el código, cuatro son notas compactas dentro de secciones secundarias de
  una tarjeta (`AppRulesPanel.tsx:272` filtro sin resultados, `GeofencePanel.tsx:554` y `DeviceLocationPanel.tsx:328`
  historial y movimientos de geocercas, `DeviceCategoriesPanel.tsx:278` que ni es un estado vacío sino la etiqueta «Sin
  regla propia» de una fila). Pasarlos al `EmptyState` (disco con icono, título y descripción) los volvería mucho más
  pesados dentro de esas tarjetas, es una decisión de diseño y no hay forma de comprobar el resultado sin navegador.
  Ninguna lista queda sin mensaje. Único candidato real a unificar, si el diseñador lo pide: la lista principal de
  `DeviceCategoriesPanel.tsx:303` (hoy un `<p class="quiet">` claro) frente al `EmptyState` que usan las demás vistas.
  Redacción: `GeofencePanel` y `DeviceLocationPanel` dicen casi lo mismo con palabras distintas («…entrada o salida»
  frente a «…entrada o salida de una geocerca»); se deja, por ser de contextos distintos.
- [ ] T7 — e2e: login visible, prueba de teclado/foco y vistas S56–S59. Ruta: inline.
- [x] T9 — Login: foto a pantalla completa con el panel como cristal desenfocado (pedido del dueño al revisar el web; se
  hace antes del cierre T8). Ruta: inline. Antes: rejilla de dos columnas, panel crema sólido y foto solo a la derecha.
  Primera pasada: la foto cubre toda la pantalla (`position: absolute; inset: 0`) y el panel pasa a ser una franja de
  cristal de alto completo, separada del borde izquierdo en escritorio; en pantallas estrechas, una tarjeta abajo con una
  banda de foto encima. `@supports not (backdrop-filter)` cae a un panel casi sólido; `sizes` de la imagen pasa a `100vw`.
  **Segunda pasada, tras ver el resultado el dueño con la referencia `mock-01-login.jpg`:** faltaba transparencia y ajustar
  letra, espacios y tamaños. Se descubrió que la foto del paquete (`photo-login-study.jpg`) es **idéntica** a
  `public/login/study.jpg` (mismo hash): la pared lisa que se ve a la izquierda de la referencia es un montaje y no existe
  en la foto, que tiene la ventana con árboles justo bajo el panel. Un cristal solo más transparente salía gris, así que el
  cristal pasa a `background` al 60 % (75 % en móvil) con `backdrop-filter: blur(28px) brightness(1.5) saturate(0.9)`: la
  luminosidad media bajo el panel queda en 0,74 frente a 0,75 de la referencia.
  Proporciones leídas de la referencia (1500 px): franja del 28,5 % que empieza al 6,5 %, contenido a 4,5 % del borde,
  bloque que arranca al 12 % de la altura (`align-items: flex-start`); logo a 64 px; título `clamp(3rem, 5.3vw, 6rem)`,
  `line-height: .92`, peso 500 (se añade a `layout.tsx`: el navegador solo descarga los pesos que se usan); botón de Google
  de 14 px, esquinas pequeñas y placa clara semitransparente con borde suave (el control lo identifican el relleno, la marca
  de Google, la etiqueta y la flecha, no solo el borde); huecos del bloque ajustados hasta coincidir con la referencia.
  **Contraste**, medido sobre la foto real desenfocada y aclarada bajo el panel (peor píxel): escritorio 1280×720 a 1920×1080,
  título 5,8–6,1:1 y textos pequeños (navy al 92 %, `#23344b`) 4,6–4,9:1; móvil con el 60 % daba 4,35:1 (no cumplía) y por eso
  usa el 75 % (6,5:1). Antes, subtítulo y nota al pie usaban `--color-text-muted` (3,35:1, tampoco cumplía sobre el cristal).
  Evidencia: capturas de Chrome sin interfaz sobre el `web` reconstruido, en `docs/redesign/s60/`: `login-1500.jpg`,
  `login-520.jpg` y `login-vs-mockup.jpg` (referencia a la izquierda, resultado a la derecha, recorte del panel). Chrome no
  admite ventanas de menos de ~500 px, así que no hay captura a 390 px. `eslint` y `tsc` limpios.
  **Diferencias que se mantienen:** el logo es el oficial (la referencia dibuja un wordmark serif propio y escribe
  «NetProject»); la franja izquierda muestra árboles de la foto en lugar de pared lisa; la tarjeta móvil tapa la cara del
  niño (no se ajustó `object-position`). **Pendiente:** verlo en un teléfono o a 390 px reales.
- [ ] T8 — Verificación y cierre: `npm run lint && npm run build` y e2e vía `verifier`, `docs/sprint-60-evidence.md`,
  `/cerrar-sprint 60`.

## Pendiente conocido

- Revisión visual del panel con sesión real de Google (T5 de S54): la hace el dueño; no se puede simular.
- Decisión de diseño abierta: Spinner con movimiento reducido (hoy queda estático; el `label` es el único indicador).
- Serif Newsreader «sigue abierta para el diseñador» (S54).
- El pase `web-redesign` → `main` lo decide el dueño; no se hace push, merge ni PR sin pedirlo.

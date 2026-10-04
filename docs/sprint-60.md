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
  A mano no se pudo probar con Tab (la extensión de Chrome no estaba conectada y `/design-system` da 404 en el build de
  producción), pero **después lo confirmó el e2e real en un navegador** (T7) y su control negativo falla con el CSS anterior
  (`docs/sprint-60-evidence.md` §1). Queda sin cubrir la fila seleccionada de Alertas por teclado (el e2e solo prueba el
  botón primario) y el modo de alto contraste de Windows (del dueño).
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
- [x] T7 — e2e: escrito, **ejecutado y en verde** (ver el resultado al final de esta tarea). Ruta: delegada (un writer;
  revisado línea a línea).
  Nuevo `frontend/e2e/login.spec.ts`: sin sembrar token (no gasta el refresh token de un solo uso), comprueba el `h1`
  «Inicia sesión» y que no hay panel. No afirma el botón de Google porque el build de CI no tiene
  `NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID` y el login muestra el aviso «Falta configurar…» en su lugar. `dashboard.spec.ts` sigue
  siendo un solo test con sus 3 steps originales intactos y añade tres: (a) Tab hasta el botón primario «Generar código» de
  Vinculación y comprobar que su `box-shadow` calculado contiene `rgb(23, 105, 255)` (el anillo opaco; la sombra de reposo
  solo lleva ese color con alfa), de modo que falla con el bug de T3; (b) abrir las 12 secciones restantes del sidebar
  (con las 4 ya visitadas cubren las 16) comprobando su `h1` y que no aparece un `alert` de error; (c) en Vista remota,
  redimensionar a 390×844 y comprobar que el aviso de consentimiento y «Solicitar ver pantalla» siguen visibles y dentro
  del viewport (cierra lo que el `security-reviewer` de S59 no pudo descartar). `tsc` y `eslint` limpios; `playwright test
  --list` lista el test de login.
  **Resultado de la ejecución real** (build de producción servido por el `webServer` de Playwright y backend de pruebas
  de `compose.test.yaml` con sesión sembrada, como el job `e2e` de CI; hecho por el `verifier`): la primera pasada falló
  en el step del anillo de foco, y era un fallo **del test, no del CSS**: `Button.module.css` anima `box-shadow` 0,15 s y el
  test leía el valor al inicio de la transición. Medido tras ella, el anillo es el esperado, `rgb(245, 243, 238) 0 0 0 2px,
  rgb(23, 105, 255) 0 0 0 4px` (la sombra de reposo sale serializada como `oklab(…)`, no `rgba(…)`), lo que confirma T3 en
  un navegador real. Corregido con `expect.poll`. Medido además: 18 Tab bastan de los 80 permitidos; `networkidle` se resuelve
  en 2–8 ms en las 12 secciones (por eso se cambió por una espera de 1 s); ninguna sección muestra un `alert` con datos
  vacíos; el aviso de consentimiento y «Solicitar ver pantalla» quedan dentro del viewport a 390×844 (el botón termina en
  y=791 de 844). Tras el arreglo: 2 ejecuciones seguidas, 2 tests pasan en cada una (dashboard ~15 s, login ~1 s), sin
  intermitencias, más `eslint` y `tsc` limpios. El spec tiene 6 steps (3 originales + 3 nuevos). Commits `8caf627` y
  `6323737`. **Sin verificar:** control negativo (que el step del anillo falle con el CSS anterior a T3) y estabilidad con
  una muestra mayor que 2 ejecuciones.
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
  **Tercera pasada, malentendido corregido:** el dueño pedía que el **panel del login** fuera más transparente, no que se
  aclarase la franja de foto de su izquierda. Se retira el velo de esa franja (`.screen::before`, commit `11179d5`): vuelve a
  ser la foto tal cual (diferencia media de 1,9/255 entre el render y la foto sin tocar, es ruido de compresión JPEG). El
  panel pasa a un velo de crema del **42 %** en escritorio (antes 60 %) y **55 %** en móvil (antes 75 %), con
  `backdrop-filter: blur(28px) brightness(2) saturate(0.9)`; el aclarado ×2 sobre el fondo es lo que permite un velo fino.
  Para eso, subtítulo y nota al pie pasan de un navy suavizado a `--color-navy` pleno.
  **Contraste final**, medido sobre la foto real desenfocada y aclarada, en el peor píxel de todo el bloque de contenido y
  con texto navy pleno: escritorio ≥ 4,6:1 en 1280×720, 1366×768, 1500×844 y 1920×1080; tarjeta móvil 4,8–4,9:1 en 390×844,
  414×896 y 520×900. (Primera medida con el 60 %: textos suaves a 4,35:1 en móvil, no cumplían; con `--color-text-muted`,
  3,35:1.) Luminosidad media del panel renderizado: 0,758 frente a 0,750 de la referencia.
  Evidencia: capturas de Chrome sin interfaz sobre el `web` reconstruido, en `docs/redesign/s60/`: `login-1500.jpg`,
  `login-520.jpg` y `login-vs-mockup.jpg` (referencia a la izquierda, resultado a la derecha, recorte del panel). Chrome no
  admite ventanas de menos de ~500 px, así que no hay captura a 390 px. `eslint` limpio.
  **Diferencias que se mantienen:** el logo es el oficial (la referencia dibuja un wordmark serif propio y escribe
  «NetProject»); la franja izquierda conserva los árboles de la foto en lugar de la pared lisa del mockup (decisión del
  dueño: dejarla original); la tarjeta móvil tapa la cara del niño (no se ajustó `object-position`). **Pendiente:** verlo
  en un teléfono o a 390 px reales. El contraste de 4,5:1 del bloque completo se midió sobre la foto, no sobre píxeles
  de pantalla en un navegador a otros zooms o con otro tamaño de fuente del sistema.
- [x] T8 — Verificación y cierre. `/cerrar-sprint 60` lanzado por el dueño. Verificación completa del `verifier` en
  modo completo (backend y frontend, e2e real y control negativo): `ruff` verde, `make test` 295 passed, `lint`, `tsc`,
  `next build` y e2e 2 passed; revisión del `security-reviewer`: 0 ALTA, 0 MEDIA, 1 BAJA documental ya corregida. Todo en
  `docs/sprint-60-evidence.md`. **No cierra el sprint hasta que CI pase los 8 jobs en GitHub Actions** (aún no hay push).

## Criterios de aceptación

Cumplido = hecho y con evidencia observada; pendiente = con motivo (humano si lo es). Evidencia: `docs/sprint-60-evidence.md`.

| Criterio | Estado | Evidencia o motivo |
|---|---|---|
| Registro fiel de S54–S59 y documentación al día (T1) | Cumplido | `c307652`; capturas citadas verificadas; contradicciones anotadas en cada sprint |
| Contraste AA (T2) | Cumplido en lo cambiado | §3: 3,76 → 5,75:1 y 3,64:1; revisión visual pendiente (humano) |
| Foco visible y teclado (T3) | Cumplido para el botón primario | §1 (e2e + control negativo) y §2.1; fila de Alertas por teclado y alto contraste de Windows sin probar (humano) |
| Movimiento reducido | Cumplido, ya existía | regla global `globals.css:222-230`; no se tocó |
| Favicon completo (T4) | Cumplido | §3: `.ico`, 192 px, `apple-icon`, tres `<link rel>`; verlo en la pestaña pendiente (humano) |
| Peso de imágenes (T5) | Cumplido | §3: logo 104 KB → 8–29 KB, medido; `card-twilight.jpg` se conserva (decisión del dueño) |
| Estados vacíos coherentes (T6) | Cumplido por revisión, sin cambios | ninguna lista queda sin mensaje; unificar `DeviceCategoriesPanel.tsx:303` queda al diseñador |
| e2e en verde (T7) | Cumplido | §1: 2 passed; spec con 6 steps y 16 secciones; Vista remota a 390×844 dentro del viewport |
| Login a pantalla completa con cristal (T9) | Cumplido | §3 y capturas en `docs/redesign/s60/`; contraste ≥ 4,6:1 medido sobre la foto; verlo a 390 px reales pendiente (humano) |
| Verificación completa y seguridad (T8) | Cumplido | §1 y §4 |
| CI en GitHub Actions con los 8 jobs | **Pendiente** | aún no hay push; es lo que cierra el sprint |
| Revisión visual con sesión real de Google (T5 de S54) y login real (H-01) | **Pendiente (humano)** | solo una persona puede hacerlo |

## Pendiente conocido

- Revisión visual del panel con sesión real de Google (T5 de S54): la hace el dueño; no se puede simular.
- Decisión de diseño abierta: Spinner con movimiento reducido (hoy queda estático; el `label` es el único indicador).
- Serif Newsreader «sigue abierta para el diseñador» (S54).
- El pase `web-redesign` → `main` lo decide el dueño; no se hace push, merge ni PR sin pedirlo.

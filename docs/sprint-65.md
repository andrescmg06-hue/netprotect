# Sprint 65 — Consentimiento y Estado, B2 y B5

Quinto sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`, fila S65). Láminas 05 (Estado de
NetProtect) y 06 (Consentimiento y notificaciones permanentes), con los textos de `02_PANTALLAS.md` §05 y §06. Es el sprint de
más riesgo: toca el consentimiento de la vista remota, el tiempo real y los servicios, así que G-19 y `security-reviewer` son
obligatorios.

Rama `sprint-65-consentimiento-estado`, creada desde `android-redesign` (`e941996`), en el worktree `C:/Users/andre/np-s65`.

## Decisiones

- **Consentimiento en 3 pasos con cuenta atrás** (lámina 06): «1 · Leer, 2 · Autorizar, 3 · Android» (el 3 es el diálogo del
  sistema, sin pantalla propia). El plazo real es de **2 min desde que llega la solicitud, solo en el teléfono**. La hora de la
  solicitud se guarda como **estado de presentación**, nunca como clave de un efecto de servicio (G-19).
- **B2:** `ServiceStatusRegistry` guarda `lastReportedAt` (en memoria), escrito **solo si el envío de ubicación tiene éxito**;
  Estado muestra «Sin confirmar» con un dato real y no según el servicio esté o no corriendo.
- **B5 (decisión 6 del plan, aprobada): sin cambiar el esquema del backend.** Si el menor sale con «Atrás» o la solicitud caduca, el
  teléfono envía `screen_share_stop` con motivo `dismissed` o `expired` (el esquema ya admite `reason` de hasta 64 caracteres y es lo
  que ya hace `projection_cancelled`). Un `granted=false` no libera el hueco del tutor en el backend; `stop` sí. La web añade los
  dos motivos a `describeStopReason` y un límite de espera en el estado `requesting`. El envío es de mejor esfuerzo y sin cola
  (`ReconnectingRealtimeChannel.send`): B5 **no puede garantizar la entrega** con el socket caído, y queda declarado.
- **Textos que se corrigen por ser falsos o engañosos** (`PLAN` §2.1): «NetProtect no graba la pantalla ni controla el dispositivo»
  → «NetProtect no graba ni guarda lo que se ve en tu pantalla.» (sí se bloquean apps); «Android no permite quitarlos» → «Cada
  aviso aparece mientras su función está activa.» (los avisos permanentes se pueden descartar desde Android 14; sin probar en
  Android 16). «Cada unos 15 minutos» en ubicación solo si el código lo hace de verdad.
- **Notificación de la vista remota:** la lámina dice «Toca para abrir NetProtect y detener la vista» y hoy tocarla no abre nada; se
  añade un `PendingIntent` inmutable que abre la app.
- No cambia el esquema del backend ni sus rutas. Sí cambian la web (`RemoteViewPanel.tsx`) y un test de backend de `stop` con motivo.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [x] T2 — `4a4d000`, `90862d3`. B2: `ServiceStatus.locationLastReportedAt` (solo el instante, nunca la ubicación), escrito por `reportAndRecord` solo
  si el envío tuvo éxito y borrado al parar el servicio; `servicesView` da la ubicación «Sin confirmar» salvo con un envío dentro de
  `LOCATION_STALE_AFTER_SECONDS` = 2 intervalos de 15 min + 5 min de margen = 35 min (el latido sigue en 300 s). Ventana de
  consentimiento pura (`CONSENT_WINDOW_SECONDS = 120`, `consentRemainingSeconds`, `isConsentExpired`, `consentProgress`,
  `consentCountdownText`: «Esta solicitud caduca en 2 min / 1 min 48 s / 45 s»), con el instante inyectado.
  `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 2m 6s,
  162 tests, 0 fallos, lint con 0 errores. Delegado (Opus). Fuera de T2, a T3 y T5: derivar `CONSENT_TIMEOUT_MS` de la ventana;
  los 3 pasos de la lámina; mostrar la hora del último reporte de ubicación (`ServicesView` no tiene el campo).
- [x] T3 — `2b2de18`, `05e792d`. `consentRequestedAt` es estado de presentación (no clave de ningún efecto de servicio); las transiciones de la solicitud
  (`Requested`, `Answered`, `Back`, `Tick`, `Stopped`) son un reducer puro, `consentStep`, con 8 tests JVM. «Atrás» envía
  `screen_share_stop` con `reason = dismissed` y la caducidad (120 s) `expired`; «Ahora no» sigue con `granted=false`; «Continuar» limpia la
  solicitud sin enviar nada para que la caducidad no mande `expired` tras un sí. El envío es de mejor esfuerzo y sin cola: con el socket
  caído el tutor no se entera y la solicitud se cierra igual en el teléfono. `CONSENT_TIMEOUT_MS` desaparece (el plazo sale de
  `CONSENT_WINDOW_SECONDS`). Los 11 efectos de servicio del shell conservan su clave. `testDebugUnitTest assembleDebug
  assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 2m 25s, 170 tests, 0 fallos, lint con 0 errores.
  Delegado (Opus). Para T5: tras «Continuar» la pantalla de consentimiento queda detrás del diálogo de Android con
  `consentRequestedAt` en nulo, y no debe mostrar cuenta atrás.
- [x] T4 — `6a1a4f9`, `31bc2af`. Web: `RemoteViewPanel` entiende `expired` y `dismissed` y tiene un límite de espera de 135 s en `requesting`
  (`REQUEST_WAIT_LIMIT_MS`, 120 s + 15 s de margen) que cierra con `teardown(true)`, la misma ruta que «Detener», que libera el hueco del
  tutor; `eslint` y `tsc` pasan, `next build` no se pudo correr localmente (enlace de `node_modules`) y lo cubre el CI. Backend: test
  `test_a_device_stop_before_answering_is_relayed_and_frees_the_slot` (`dismissed` y `expired`; una segunda conexión de tutor no recibe
  `screen_share_busy`); `make test` en contenedor → **287 passed**, 0 fallos (285 antes + 2). Sin cambios de código de producción del
  backend. Sin test de componente para el temporizador de 135 s. Delegado (Sonnet).
- [x] T5a — `e3f4f1c`, `a1c8012`, `a8cc0e8`. Consentimiento en 3 pasos (`NpStepIndicator`) con cuenta atrás y barra (`NpCountdownBar`), sin reloj cuando `requestedAt` es nulo,
  pie fijo y flecha «Atrás» por el mismo camino que el botón del sistema; Estado v2 con el panel «En curso» y «Detener ahora» y la lista plana;
  banner «Tu tutor está viendo tu pantalla»; se quita «no graba ni controla» por «NetProtect no graba ni guarda lo que se ve en tu pantalla.».
  Dos textos de ubicación corregidos por el orquestador: «Todavía no recibimos una ubicación» era inexacto con un envío de hace más de 35 min
  → «No hay una ubicación reciente confirmada», y «cada unos 15 minutos» → «aproximadamente cada 15 minutos». 7 frames de galería. El shell solo
  cambia en la llamada (`requestedAt`, `now`, `onBack`). `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks`
  (sin caché) → BUILD SUCCESSFUL en 2m 34s, 175 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` en `Pixel_8` → **110 tests, 0 fallos**.
  Delegado (Opus). Diferencias con las láminas, aceptadas: «Continuar» deshabilitado en azul al 55 %, no gris; pasos pendientes en `InkMuted` por
  contraste AA; sin fila «Visualización de pantalla» en la lista (la lámina 05 no la tiene).
- [x] T5b — `9c51898`. Textos de las notificaciones: control de apps «Control de apps funcionando en este dispositivo.», ubicación «Se envía una ubicación aproximada cada
  15 minutos.» (la cifra sale de `REPORT_INTERVAL_MS`, vigilada por test) y vista remota «Toca para abrir NetProtect y detener la vista.», que ahora es cierta: la notificación lleva
  un `PendingIntent` inmutable a `MainActivity` sin extras. Los servicios no cambian en lo demás. `ForegroundNotificationTextTest` (3 tests) prohíbe «controla», «graba» y «no se puede quitar».
  Build sin caché → 178 tests, 0 fallos, lint 0 errores. Delegado (Opus).
- [x] T6 — Emulador `Pixel_8`: revisión visual a letra 1.0 y 2.0 contra las láminas 05 y 06; **prueba G-19** en la app real (3 min 11 s, mismos `ServiceRecord` y proceso, un latido por
  minuto, 6 consultas B1, sin 5xx); los avisos de control de apps y ubicación muestran el texto nuevo. Sin probar con un tutor real (vista remota de punta a punta, «Atrás» y caducidad contra la
  web, toque sobre la notificación): declarado en `docs/sprint-65-evidence.md`. Inline.
- [x] T7 — `security-reviewer` (**Opus**): **sin hallazgos ALTA**; 5 arreglados (1 MEDIA, 4 BAJA). MEDIA: `consentRequestedAt` era `remember` y `route` `rememberSaveable`, así que al recrearse la actividad
  (giro, tamaño de letra, tema, muerte de proceso; `MainActivity` no declara `configChanges`) volvía la pantalla de consentimiento sin cuenta atrás, sin caducar, sin avisar a la web con «Atrás» y con «Continuar»
  aún operativo; ahora se guarda como milisegundos con un `Saver` (centinela `Long.MIN_VALUE`). BAJA: «Continuar» y «Ahora no» se ignoran sin solicitud pendiente; una solicitud nueva empieza sin marcar
  (`rememberSaveable(requestedAt)`); no se envía `expired`/`dismissed` mientras hay una captura activa (`shouldSendConsentStop`); la web no abre peer ni socket tras cerrar la sesión (`cancelledRef`, también en
  el `.catch` tardío); el aviso de ubicación nombra al destinatario («Tu tutor recibe una ubicación aproximada cada 15 minutos.»). `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks`
  (sin caché) → BUILD SUCCESSFUL en 1m 17s, 180 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` → **111 tests, 0 fallos**. Falta el CI de los 8 jobs, que corre con el PR.

## Criterios de aceptación

- La cuenta atrás se ve en el consentimiento y llega a cero cerrando la solicitud; «Atrás» y la caducidad envían `screen_share_stop`
  con su motivo y la web los muestra.
- Estado muestra «Sin confirmar» en ubicación solo con un dato real (B2).
- Ningún texto dice que NetProtect no controla el dispositivo.
- La prueba G-19 con salida real en `docs/sprint-65-evidence.md`; `security-reviewer` sin ALTA.
- `./gradlew test assembleDebug lintDebug`, `npm run lint && npm run build` y los 8 jobs de CI en verde.

## Pendiente que necesita una persona

- Vista remota de extremo a extremo con un tutor real en el panel web, caducidad, «Atrás» y «Detener» desde la notificación y el
  banner, y si los avisos permanentes se pueden descartar en Android 16 (Galaxy S25 FE).

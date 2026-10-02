# Sprint 51 — Endurecimiento, accesibilidad, regresión e integración a `main`

Duodécimo y último sprint del rediseño Android (`docs/android-redesign/sprints/S51-endurecimiento-y-cierre.md`).
Decisiones: D-02 (revisión con evidencia), D-14 (a): `lintDebug` en el job `android` de CI. Sin funcionalidades
nuevas: aquí se demuestra que nada se perdió.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = un escritor (subagente) o DeepSeek, con el motivo.

- [x] T1 — D-14: `lintDebug` en el job `android` de CI. Inline (una línea en `ci.yml`). `263ade5`.
- [x] T2 — Último color literal fuera de `ui/theme` (`feature/home/HomeScreen.kt:171`) → `colorScheme.background`.
  Delegado (escritor; T2–T4 tocaban 4 archivos). `6e69b46`.
- [x] T3 — L-02: `dataExtractionRules` en el manifest; nada de la app se respalda ni se transfiere. `f5dbfac`.
- [x] T4 — Texto a 200 %: la duración en Estadísticas crece en vez de cortarse; el nombre de la app en la pantalla
  de bloqueo admite 2 líneas. Claude devolvió el nombre de Estadísticas a 96 dp fijos: el escritor lo hizo
  variable y las barras dejaban de alinearse a fuente normal. `80cbee5`.
- [ ] T5 — Release con R8: `assembleRelease` compila. Reglas: Credential Manager (Play Services), WebRTC, Room y
  WorkManager traen las suyas en el AAR; el JSON se lee con `JSONObject` por nombre de clave (inmune a la
  ofuscación). Para probarla en ejecución, tipo de build `minified` (`5c33ff1`): la release con R8, firmada con la
  clave de debug y apuntando al backend local. La galería de depuración no está ni en `release` ni en `minified`
  (comprobado en el manifest de cada APK). En el emulador, `minified` instalada sobre la debug (misma clave,
  conserva la sesión) arranca sin errores en logcat. En modo supervisado lee el JSON del backend: nombre del
  tutor y «Conectado». Los tres servicios (reporte, control de apps, ubicación) aparecen «Activo». **Falta:** el
  modo tutor y la vista remota (WebRTC) con `minified` (T10, H-02).
- [x] T6 — Accesibilidad estática. Inline. Las 39 apariciones de `contentDescription = null` son decorativas: cada
  icono tiene al lado un texto que dice lo mismo, o va dentro de una tarjeta pulsable con texto (incluido
  `AppIcon`, con el nombre de la app al lado). Único arreglo: la marca, que TalkBack leía «Net», «Protect», ahora
  es un solo encabezado «NetProtect» (`BrandHeader`). `NpButton(small = true)` mide 40 dp de alto, pero Compose
  amplía el área táctil de un `clickable` a 48 dp; se comprueba con Accessibility Scanner en T10. **No hay
  encargo para DeepSeek:** la lista mecánica quedó en una sola línea y delegarla costaba más que hacerla.
- [x] T7 — Sin conexión: modo avión en Inicio del tutor y pantallas del supervisado (las otras 9 ya usan `ErrorState`).
  **Supervisado, hecho (emulador, `minified`):** Dispositivo vinculado pasa a «Sin conexión» en rojo. En Estado
  de NetProtect, el reporte sigue «Activo» hasta 3 min sin envíos y luego pasa a «Sin confirmar»
  (`REPORT_STALE_AFTER_SECONDS`); control de apps y ubicación siguen «Activo», que es verdad sin red. Al volver la
  red, el reporte se recupera solo. **Falta el modo tutor**: el emulador está vinculado como supervisado.
- [x] T8 — Rotación y muerte del proceso; D-02 se revisa con esa evidencia. **Supervisado, hecho:** al girar se
  conserva la pantalla y el horizontal se ve bien. Con «No conservar actividades» la actividad se recrea en la
  misma pantalla. `am kill` no mata el proceso porque los servicios en primer plano lo mantienen vivo, así que en
  el supervisado la muerte del proceso real no ocurre con la app en uso. **Falta el modo tutor**, que es donde
  los datos se recargan al girar (sin ViewModel).
- **Modo tutor (emulador, `minified`)**, con un supervisado de prueba vinculado por API (`backend/scripts/
  seed_test_session.py` pasado por stdin al contenedor; código real generado en la app) y datos de apps, uso y
  ubicación cargados por API:
  - **Bug encontrado y corregido** (`07d8525`): en Android `optString` devuelve el texto «null» para un JSON
    null, así que **todas** las apps salían «Desinstalada», y el nombre, la zona horaria o la última conexión
    podían mostrarse como «null». Un ayudante `optStringOrNull` cubre todos los campos anulables, con test
    instrumentado (el `org.json` de escritorio devuelve «» y ocultaría el bug). Verificado en pantalla tras el
    arreglo.
  - Sin red: el detalle del dispositivo y Ubicación recargadas muestran «Sin conexión con el servidor…» con
    «Reintentar». Si la app arranca en frío sin red, el tutor con sesión ve el login con el error y «Reintentar»,
    y la sesión se retoma al volver la red (decisión del Sprint 41; no se cambia aquí).
  - Rotación: se conserva la pantalla y la búsqueda de Apps, pero el periodo de Estadísticas («7 días») volvía
    a «Hoy». **Corregido** (`fa63aeb`, D-02 revisada, aprobado por el dueño): el periodo, el filtro de Alertas y
    el código de vinculación a medio escribir son `rememberSaveable`; los datos se recargan para la selección
    restaurada. Verificado girando en el emulador. Queda sin guardar el texto del diálogo de renombrar (texto en
    curso, no una selección).
  - Fuente 200 %: la duración de Estadísticas ya no se corta (T4 verificado). `StatusPill` se cortaba
    («Desconectado» → «Des») y la barra inferior partía palabras («Dispositi/vos»). **Corregido** (`9c7eb73`):
    la píldora baja entera a su propia línea y las etiquetas usan «…». Verificado al 200 %.
  - Menor: tras vincular, el código ya usado sigue visible con su cuenta atrás hasta salir de Inicio.
- [x] T9 — Regresión contra `docs/android-redesign/INVENTARIO.md`: 0 puntos perdidos; detalle en `docs/sprint-51-evidence.md`. Capturas de 11 de las 17 pantallas; el resto en T10.
- [ ] T10 — Pruebas humanas: release en el Galaxy S25 FE, TalkBack, fuente 130/200 %, H-02.
- [ ] T11 — `security-reviewer` sobre `main...android-redesign`, docs, cierre G-01…G-27 y PR `android-redesign` → `main`.

## Hallazgos de la auditoría (fase 1)

- El código legado (`LegacySections.kt`, `TutorScreen.kt`, `SupervisedScreen.kt`) ya no existe.
- `proguard-rules.pro` no tiene reglas y R8 está activo en release: riesgo con Room, WebRTC y receivers.
- Solo hay dos `Log.w` (errores SDP en `ScreenShareService`); no registran tokens ni datos del menor.
- No hay ViewModel (D-02 a): los datos se recargan al girar; la pila de navegación sí se conserva.

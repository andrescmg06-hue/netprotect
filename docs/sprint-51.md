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
  (comprobado en el manifest de cada APK). **Falta:** instalar `minified` y recorrer la app (T10).
- [x] T6 — Accesibilidad estática. Inline. Las 39 apariciones de `contentDescription = null` son decorativas: cada
  icono tiene al lado un texto que dice lo mismo, o va dentro de una tarjeta pulsable con texto (incluido
  `AppIcon`, con el nombre de la app al lado). Único arreglo: la marca, que TalkBack leía «Net», «Protect», ahora
  es un solo encabezado «NetProtect» (`BrandHeader`). `NpButton(small = true)` mide 40 dp de alto, pero Compose
  amplía el área táctil de un `clickable` a 48 dp; se comprueba con Accessibility Scanner en T10. **No hay
  encargo para DeepSeek:** la lista mecánica quedó en una sola línea y delegarla costaba más que hacerla.
- [ ] T7 — Sin conexión: modo avión en Inicio del tutor y pantallas del supervisado (las otras 9 ya usan `ErrorState`).
- [ ] T8 — Rotación y muerte del proceso; D-02 se revisa con esa evidencia.
- [ ] T9 — Regresión contra `docs/android-redesign/INVENTARIO.md` y capturas de las 17 pantallas.
- [ ] T10 — Pruebas humanas: release en el Galaxy S25 FE, TalkBack, fuente 130/200 %, H-02.
- [ ] T11 — `security-reviewer` sobre `main...android-redesign`, docs, cierre G-01…G-27 y PR `android-redesign` → `main`.

## Hallazgos de la auditoría (fase 1)

- El código legado (`LegacySections.kt`, `TutorScreen.kt`, `SupervisedScreen.kt`) ya no existe.
- `proguard-rules.pro` no tiene reglas y R8 está activo en release: riesgo con Room, WebRTC y receivers.
- Solo hay dos `Log.w` (errores SDP en `ScreenShareService`); no registran tokens ni datos del menor.
- No hay ViewModel (D-02 a): los datos se recargan al girar; la pila de navegación sí se conserva.

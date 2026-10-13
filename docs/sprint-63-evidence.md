# Sprint 63 — evidencia

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-s63`, rama `sprint-63-acceso-vincular`.

## T2 — errores tipados

`./gradlew test assembleDebug assembleDebugAndroidTest lintDebug` → BUILD SUCCESSFUL en 3m 1s, 105 tests unitarios, lint con
0 errores (detalle en `docs/sprint-63.md`).

## T3 — pantallas (escritor Opus)

- El escritor corrió `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` → BUILD SUCCESSFUL en
  3m 33s, 116 tests, 0 errores de lint, 25 avisos.
- Primera repetición con `verifier`: PASS, pero `testDebugUnitTest` y `assembleDebug` salieron UP-TO-DATE, así que solo
  confirmaba la corrida anterior. **No se tomó como evidencia nueva.**
- Segunda repetición con `--rerun-tasks` (`testDebugUnitTest lintDebug`): BUILD SUCCESSFUL en 3m, 116 tests, 0 fallos,
  0 errores, 0 omitidos (suma de 16 XML en `app/build/test-results/testDebugUnitTest/`), `:app:testDebugUnitTest` y
  `:app:lintDebug` ejecutadas, 24 avisos.

## T4 — emulador `Pixel_8` (Android 16), build de debug

- `./gradlew connectedDebugAndroidTest` → BUILD SUCCESSFUL en 5m 21s, **88 tests, 0 fallos, 0 errores, 0 omitidos**
  (los 77 de S62 más los nuevos de S63).
- Galería `ScreensGalleryActivity`, letra 1.0 y 2.0 (`settings put system font_scale`); el emulador quedó con 1.0.
- Defecto encontrado a 1.0: en Vincular, «Cerrar sesión» bajaba a una segunda línea y en azul, y el pie apilaba «Esperando el
  código» sobre «Comprobar vínculo»; en la lámina 02 van en una fila y en gris/azul. Causa: `NpButton` de variante texto mide
  16 sp con relleno propio y no cabe junto a la etiqueta con interletraje en 360 dp. Arreglo: `CompactAction` (14 sp, 48 dp de
  alto) y `FlowRow` en el pie. Tras `installDebug` (`testDebugUnitTest installDebug lintDebug` → BUILD SUCCESSFUL en 3m 7s,
  116 tests, 0 errores de lint) se repitieron las capturas: una fila en cada caso.
- Letra 2.0: login (servicio no disponible, «Tu sesión caducó», comprobando), Vincular (esperando, inválido o vencido),
  «Más ajustes» y el diálogo bajan de línea sin cortarse ni partir palabras por letras.

Capturas: `docs/android-redesign/supervisado-v2/s63/`.

## Pendiente (declarado)

- **No probado:** vinculación real con un código de tutor, login real de Google, el 429 contra el backend real y el cierre de
  sesión confirmado de punta a punta. Solo se vieron en galería y en tests de UI.
- **No probado:** «Quitar animaciones» y TalkBack (pasan a S67).
- Diferencias con las láminas, aceptadas: el botón «Vincular dispositivo» deshabilitado es el azul atenuado de `NpButton`, no
  el gris de la lámina; la foto del login pasa bajo la barra de estado y no se comprobó el contraste de sus iconos; el
  subtítulo de la fila «Más ajustes» en Inicio reutiliza «Opciones de este teléfono.» hasta que S64 rehaga Inicio.
- Vistos de pasada, **fuera del alcance de S63**: en `ServicesStatusScreen` a 2.0 «Ubicación aproximada» se parte por letras
  y «Control de apps» queda en una columna estrecha (mismo tipo que B-06; lo rehacen S65 y S67).
- `LoadingScreen` dice siempre «Restaurando tu sesión…», también en la espera corta tras iniciar sesión.
- T5 (revisión de seguridad y CI) sigue abierta.

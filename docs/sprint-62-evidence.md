# Sprint 62 — Evidencia

Todo lo de aquí se ejecutó el 12/10/2026. La revisión visual la hizo Claude mirando las capturas, porque los subagentes no ven
imágenes.

## Verificación en local (escritores T2 y T3)

- `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug` (con `CI` sin definir y
  `ANDROID_HOME=C:/Users/andre/AppData/Local/Android/Sdk`, porque el worktree no tiene `local.properties`):
  - tras T2 (`e0631a7`): BUILD SUCCESSFUL, 85 tests unitarios, lint con 0 errores;
  - tras T3 (`a560fdd`): BUILD SUCCESSFUL, 93 tests unitarios, lint con 0 errores y 24 avisos, ninguno en archivos nuevos.
- `grep` de `SkyGround|PaperWhite|ShieldNavy|SlateMuted|SignalBlue|NpShadow|NpElevation` sobre `mobile/app/src`: sin resultados.
- `grep -rn "Color(0x" mobile/app/src/main/java --include=*.kt | grep -v /ui/theme/`: sin resultados.
- Contrastes que calculé yo con un script (WCAG): `Ink` 14,09:1 y `InkMuted` 5,17:1 sobre crema; `BlueText` 5,63:1;
  `SuccessText` 4,88:1; `WarningText` 5,38:1; `DangerText` 5,18:1; texto blanco sobre el azul de acción 4,67:1. `InkSubtle`
  da 2,80:1 y por eso es solo decorativo. Sobre el panel navy (test JVM del escritor): resumen 11,80:1, etiquetas 8,61:1 y
  titular 17,39:1.

## CI del PR #20 (run sobre `6a7a499`)

8 de 8 jobs en verde. `android-instrumented`: `Starting 77 tests on emulator-5554` y `Finished 77 tests`, que son los 71
anteriores más los 6 de `V2ComponentsTest` (primera ejecución real de estos).

## Revisión visual (T4), emulador `Pixel_8` (API 36), build de debug, `DesignGalleryActivity`

El emulador estaba bloqueado por su PIN y tuvo que desbloquearlo el dueño.

- Letra 1.0 (`componentes-100-hero-y-sello.png`, `componentes-100-rail.png`): panel navy con titular serif, sello de 4 arcos
  (verde = listo, ámbar = pendiente, gris punteado = sin reporte), etiqueta «N DE 4 PERMISOS», resumen y botón «Revisar
  permisos» con su ficha; rail con círculo ámbar para el siguiente paso, punteado para el pendiente y verde con check para los
  configurados. Coincide con las láminas 03 y 04 en estructura y jerarquía.
- Letra 2.0 (`componentes-200-hero.png`, `componentes-200-rail.png`): el titular pasa encima del sello, la etiqueta baja de
  línea por palabras, el chip y el botón crecen y el texto del rail baja de línea sin partir palabras por letras. Nada
  cortado.
- Diferencias con las láminas, aceptadas: con 36 sp, «Todo funciona» no cabe junto al sello y baja a dos líneas; el margen
  lateral del panel es 28 dp (en las láminas, unos 24 dp); el subrayado del botón de texto aparece al pulsar en lugar de
  barrerse.

Capturas en `docs/android-redesign/supervisado-v2/s62/`.

## Pendiente (declarado)

- «Quitar animaciones» **no se probó en pantalla**: solo hay un helper con test JVM (`isReducedMotion`) y el sello arranca
  completo cuando es verdadero. Hay que comprobarlo en el teléfono con `ANIMATOR_DURATION_SCALE` en 0.
- TalkBack: los estados del rail se exponen por semántica (test instrumentado), pero nadie lo oyó con TalkBack. Pasa a S67.
- Chips, botones secundario y de texto: se ven en la galería, pero no se compararon con ninguna lámina uno por uno.
- Las pantallas actuales ya usan los tokens nuevos (cuerpo de 16 sp, botones de 60 dp) sin haberse rediseñado; se rehacen en
  S63–S66.

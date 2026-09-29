# Sprint 42 — Evidencia

## 1. Android — `cd mobile && ./gradlew test lintDebug connectedDebugAndroidTest`

```text
BUILD SUCCESSFUL
JVM: 95 tests, 0 fallos
  TokenSessionTest 13 · RuleEvaluatorTest 34 · NavStackTest 12 · ExampleUnitTest 1 (heredados)
  ui.format: RelativeTimeTest 8 · LabelsTest 10 · DurationsTest 5 · ClockTest 1 · DayGroupingTest 1
             AlertMessageFallbackTest 4 · ui.theme.ColorTokensTest 6
Instrumentados (emulador Pixel_8, Android 16): 16 tests, 0 fallos, 0 errores
  8 heredados de Room (PendingRuleEventStoreTest 4, RulesCacheStoreTest 4)
  + 8 nuevos de DeepSeek (ComponentsTest 7, DesignGalleryTest 1)
lint: 0 errores, 25 avisos (S41: 26; se resolvió MissingApplicationIcon (L-03) y el icono nuevo no deja avisos)
```

## 2. Comprobaciones de Claude en la revisión

- Los 27 colores de `DESIGN.md` (front matter) contra `Color.kt`, por script: 27 coinciden, 0 discrepancias.
  Los 3 extra (`SkyGroundEnd`, `SecondaryBorder`, `DangerHoverWash`) los pidió el encargo.
- 52 iconos Lucide + `ic_google_g`: todos de trazo (`fillColor="#00000000"`, `strokeWidth="2"`, extremos y
  uniones redondeados), 0 faltantes, 0 sobrantes.
- `grep "Color(0x"` fuera de `ui/theme/Color.kt`: 0 coincidencias en `ui/`.
- Archivos tocados por DeepSeek: todos dentro de la lista permitida de cada encargo.
- Manifiesto fusionado de release sin `DesignGalleryActivity` (0); el de debug la tiene (1).

## 3. Revisión visual en el emulador (galería, capturas en la carpeta temporal del sistema)

Revisadas todas las secciones: botones (4 variantes × estados), píldoras de estado y tonos, insignias de
severidad (icono + texto + color), avisos, estados vacío/carga/error, cabecera de marca, barra superior e
inferior, control segmentado, chips, barra de uso, línea de tiempo, código con cuenta atrás y vencido,
casillas del código (vacía, a medias, con error), tarjetas de permiso, tarjeta, fila, y los dos diálogos.
Diferencias con los mockups corregidas: barra inferior como tarjeta redondeada, sombra azulada suave,
margen bajo la barra de estado en la galería.

## 4. Fallos del camino

- Tras renombrar `mipmap-anydpi-v26` a `mipmap-anydpi`, `processDebugResources` falló con
  `resource mipmap/ic_launcher not found`; era estado viejo de Gradle: `./gradlew clean` lo resolvió.
- La comprobación automática de seguridad de la sesión falló varias veces (sin veredicto); los comandos
  afectados se reintentaron sin cambios.

## 5. No verificado

- Icono de lanzador y aspecto en el Samsung Galaxy S25 FE (teléfono desconectado en este sprint).

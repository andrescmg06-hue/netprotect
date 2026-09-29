# Sprint 42b — Componentes compartidos y galería de componentes (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.
> **Requisito:** el encargo 42a (`sprint-42a-fundamentos.md`) ya está hecho y revisado. Si no existen
> `ui/theme/NpColors`, `ui/icons/NpIcons` y `ui/format/`, **detente** y avísalo en el informe.

## 1. Objetivo

Al terminar existen, en `ui/components/`, los componentes compartidos que usarán las 17 pantallas del
rediseño, **cada uno con su `@Preview`**, y una **galería de componentes** que solo existe en builds *debug*
para revisarlos en un teléfono real. No se cambia ninguna pantalla existente. Los mockups de referencia
(solo para el estilo): `docs/android-redesign/mockups/03-tutor-inicio.jpg`, `10-tutor-alertas.jpg`,
`14-supervisado-permisos.jpg` y `mockups/README.md` (sección "Sistema visual").

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Decisiones ya tomadas (no las reabras)

| # | Decisión |
|---|---|
| D-13 | Hay tests de UI de Compose (`androidTest`): las dependencias ya están en `build.gradle.kts`. |
| — | **Cero** `Color(0x…)`, `sp`/`dp` "sueltos" para tipografía ni radios literales: colores desde `NpColors`/`NpTone`, texto desde `NpText`, formas desde `NpShapes`, sombras desde `NpElevation`, iconos desde `NpIcons`. (Los `dp` de espaciado sí van en el componente, en múltiplos de 4.) |
| — | Objetivos táctiles **≥ 48 dp** de alto en todo lo pulsable. Cada icono con `contentDescription` (o `null` solo si es decorativo y el texto ya lo dice). El nivel de una alerta **nunca solo con color**: icono + texto + color. |
| — | Los componentes **no** hacen llamadas de red ni conocen pantallas, ni leen el reloj (el tiempo entra por parámetro). Estado hacia arriba (*state hoisting*): los componentes reciben valores y lambdas. |
| — | Textos en español (es-CO), tal como aparecen en este encargo. |
| — | La galería es **exportada solo en debug** (excepción documentada por Claude): vive en `src/debug`, sin permisos, y no puede aparecer en release. |

## 3. Lo que NO debes tocar

`backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`,
`mobile/app/build.gradle.kts`, `mobile/gradle/libs.versions.toml`, `{pkg}/core/**`, `{pkg}/feature/**`,
`{pkg}/MainActivity.kt`, `{pkg}/ui/theme/**`, `{pkg}/ui/format/**`, `{pkg}/ui/icons/**`, `{pkg}/ui/navigation/**`,
`mobile/app/src/main/AndroidManifest.xml`, `res/`. Si necesitas un icono, color o etiqueta que no existe, **no
lo crees**: escribe `[PREGUNTA PARA CLAUDE]` y usa el más parecido. Sin `git commit`/`push`.

## 4. Punto de partida

Lee: `{pkg}/ui/theme/*` (`NpColors`, `NpText`, `NpShapes`, `NpElevation`, `NpTone`, `NetProtectTheme`),
`{pkg}/ui/icons/NpIcons.kt`, `{pkg}/ui/format/*` (etiquetas y formateadores), `DESIGN.md` (secciones
Components y Elevation), `frontend/src/components/ui/{Button,Card,StatusBadge,EmptyState,SegmentedControl,ProgressBar,ConfirmDialog}.tsx`
y sus `.module.css` (así es cada uno en la web), y los 3 mockups. Si hay una carpeta
`mobile/app/src/debug/`, ya tiene `res/xml/network_security_config.xml`: no la toques.

## 5. Archivos permitidos (lista cerrada)

Crear: `{pkg}/ui/components/*.kt` (uno por componente o grupo pequeño, §6),
`mobile/app/src/debug/java/com/netprotect/app/debug/{DesignGallery,DesignGalleryActivity}.kt`,
`mobile/app/src/debug/AndroidManifest.xml`,
`mobile/app/src/androidTest/java/com/netprotect/app/ui/{DesignGalleryTest,ComponentsTest}.kt`.
Todo lo demás está prohibido.

## 6. Componentes (`{pkg}/ui/components/`)

Cada `@Composable` público con `modifier: Modifier = Modifier` como primer parámetro opcional, y un
`@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)` (ese literal de color solo en la anotación)
envuelto en `NetProtectTheme`. Estilo base del panel web: superficie blanca, borde `Hairline` de 1 dp, sombra
suave, radio 16 dp (`NpShapes.Xl`) en tarjetas.

| Componente | Firma (obligatoria) y comportamiento |
|---|---|
| `NpCard` | `(modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`. Blanca, borde `Hairline`, `NpElevation.Card`, relleno 16 dp. Si `onClick != null`, es pulsable (≥ 48 dp) con semántica de botón. |
| `NpButton` | `(text, onClick, modifier, variant: NpButtonVariant = Primary, @DrawableRes icon: Int? = null, loading: Boolean = false, enabled: Boolean = true, small: Boolean = false)`. `enum NpButtonVariant { Primary, Secondary, Text, Danger }`. Alto ≥ 48 dp (`small` = 40 dp mínimo, nunca menos). Primary: `SignalBlue` + texto `PaperWhite`; Secondary: blanco, borde `SecondaryBorder`, texto `SignalBlueText`; Text: sin fondo, `SlateMuted` (pulsado `SignalBlueText`); Danger: `DangerWash`, texto `DangerText`, borde `Danger` al 30 %. `loading`: spinner en lugar del icono y desactivado (no dobles toques). `enabled = false`: 55 % de opacidad. Texto con `NpText.BodyStrong`, radio `NpShapes.Md`. **Nada de `TextButton` de Material** con su texto en una sola línea forzada: el texto debe poder envolver a dos líneas sin aplastarse en vertical. |
| `IconTile` | `(@DrawableRes icon, tone: NpTone = NpTone.Info, size: Dp = 48.dp, contentDescription: String? = null)`. Baldosa redondeada (radio 10–12 dp) con `tone.wash` y el icono en `tone.text`. |
| `ListRow` | `(title, modifier, subtitle: String? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null)`. Alto ≥ 56 dp; título `BodyStrong` en `ShieldNavy`, subtítulo `Body` en `SlateMuted`. `trailing` por defecto vacío (el chevron lo pasa quien lo use). |
| `StatusPill` | `(text, tone: NpTone, modifier, showDot: Boolean = true)`. Píldora de 24 dp, `tone.wash` + `tone.text`, `NpText.Caption`, punto de 7 dp opcional. |
| `SeverityBadge` | `(level: String, modifier)`. Usa `AlertLabels.levelLabel/levelTone/levelIcon`: icono + texto + color, en píldora. Nunca solo color. |
| `InfoBanner` | `(title, text, modifier, tone: NpTone = NpTone.Info, @DrawableRes icon: Int = NpIcons.Info)`. Fondo `tone.wash`, borde suave, icono a la izquierda, título `BodyStrong`, texto `Body`. |
| `SectionHeader` | `(title, modifier, actionLabel: String? = null, @DrawableRes actionIcon: Int? = null, onAction: (() -> Unit)? = null)`. Título `Title` en `ShieldNavy`; acción a la derecha en `SignalBlueText` (≥ 48 dp de alto). |
| `EmptyState` | `(@DrawableRes icon, title, message, modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)`. Disco `BlueWash` con el icono, título, mensaje y acción opcional, centrados. |
| `LoadingState` | `(modifier, label: String = "Cargando…")`. Indicador circular en `SignalBlue` + texto; `semantics` con `liveRegion = Polite`. |
| `ErrorState` | `(message, onRetry: () -> Unit, modifier)`. Icono `NpIcons.CircleAlert` en `Danger`, mensaje, botón Secondary **"Reintentar"**. |
| `BrandHeader` | `(modifier, subtitle: String? = null)`. Escudo (`NpIcons.LogoShield` como `Image`) + "Net" en `ShieldNavy` y "Protect" en `SignalBlue` (`NpText.Title` en 700 y ×1.4) + `subtitle` en mayúsculas, `Caption` con `letterSpacing = 0.2.em`, `SlateMuted`. |
| `NpTopBar` | `(modifier, onBack: (() -> Unit)? = null, title: String? = null, trailing: (@Composable () -> Unit)? = null)`. Si `onBack != null`, botón atrás (`NpIcons.ArrowLeft`, ≥ 48×48 dp, `contentDescription = "Atrás"`); a su lado el `title` (`Title`, `ShieldNavy`) o, si es null, `BrandHeader`. |
| `NpBottomBar` | `(items: List<NpBottomItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`. `data class NpBottomItem(val label: String, @DrawableRes val icon: Int, val badgeCount: Int = 0)`. Fondo blanco con borde superior `Hairline`; el elegido en `SignalBlueText` con una barra corta de 3 dp en `SignalBlue` debajo; los demás `SlateMuted`. Alto ≥ 64 dp, cada ítem ≥ 48 dp. Con `badgeCount > 0`, punto/número rojo (`Danger`, texto `PaperWhite`) — con `contentDescription`. |
| `SegmentedControl` | `(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`. Opción elegida: `SignalBlue` + texto blanco, radio `NpShapes.Sm`; resto `BlueMist`. Alto ≥ 48 dp. |
| `FilterChips` | `(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`. Fila desplazable horizontal; ídem colores; alto ≥ 48 dp. |
| `UsageBar` | `(fraction: Float, modifier, tone: NpTone = NpTone.Info, contentDescription: String)`. Barra de 8 dp, pista `NeutralWash`, relleno `tone.solid`; `fraction` se limita a 0..1; **se anima con `scaleX`/`graphicsLayer`, no con `width`** (evita relayout). |
| `TimelineItem` | `(@DrawableRes icon, tone: NpTone, title, time: String, modifier, subtitle: String? = null, isLast: Boolean = false)`. Icono en círculo `tone.wash`, línea vertical `Hairline` hasta el siguiente (oculta si `isLast`), título `BodyStrong`, `time` en `Caption` `SlateMuted`. |
| `DayHeader` | `(text, modifier)`. Cabecera de grupo ("Hoy", "Ayer", "26 de septiembre de 2026"): `Label` en `SlateMuted`. |
| `CodeDisplay` | `(code: String, remainingSeconds: Int, totalSeconds: Int, modifier)`. Los 6 dígitos en `NpText.Numeric` (cifras tabulares), separados por un espacio fino, `ShieldNavy`; debajo, `UsageBar` de vigencia y el texto **"Vence en 2:59"** (m:ss). Si `remainingSeconds <= 0`: **"El código venció"** en `DangerText`. Los dígitos con `semantics` que los lean uno a uno. |
| `OtpInput` | `(value: String, onValueChange: (String) -> Unit, modifier, length: Int = 6, enabled: Boolean = true, isError: Boolean = false)`. 6 casillas visibles que reflejan un único `BasicTextField` oculto con teclado numérico; **solo dígitos**, máximo `length`, **acepta pegar** (filtra a dígitos y recorta). Casilla activa con borde `SignalBlue`; error con borde `Danger`. Cada casilla ≥ 48 dp. |
| `ConfirmDialog` | `(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss, danger: Boolean = false)`. Usa `androidx.compose.ui.window.Dialog` con tarjeta blanca de radio 16 dp; icono `NpIcons.TriangleAlert` cuando `danger`; el botón de confirmar es `Danger` si `danger`, si no `Primary`. Cerrar tocando fuera = `onDismiss`. |
| `PermissionCard` | `(@DrawableRes icon, title, description, granted: Boolean, actionLabel: String, onAction: () -> Unit, modifier, secondaryLabel: String? = null, onSecondary: (() -> Unit)? = null)`. `IconTile` + título + descripción + `StatusPill` ("Concedido" en `Success` / "Pendiente" en `Warning`) y, si no está concedido, botón Primary; `secondaryLabel` (p. ej. "Ya lo activé, verificar de nuevo") como botón Text. |

## 7. Galería (solo debug)

- `mobile/app/src/debug/AndroidManifest.xml`: `<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><activity android:name="com.netprotect.app.debug.DesignGalleryActivity" android:exported="true" android:label="Galería de componentes" android:theme="@style/Theme.NetProtect"/></application></manifest>` (sin `intent-filter`, sin permisos). Con esto se abre con
  `adb shell am start -n com.netprotect.app/com.netprotect.app.debug.DesignGalleryActivity`.
- `DesignGalleryActivity`: `setContent { NetProtectTheme { DesignGallery() } }` con `enableEdgeToEdge()`.
- `DesignGallery()` (`@Composable`, pública): una columna desplazable con **una sección por componente**, cada
  una con su título en texto y **todos sus estados**: botones (4 variantes × normal/cargando/deshabilitado/con icono/`small`),
  píldoras de los 6 estados de dispositivo y los 6 tonos, `SeverityBadge` de los 4 niveles, `InfoBanner`, `EmptyState`,
  `LoadingState`, `ErrorState`, `BrandHeader` con y sin subtítulo, `NpTopBar` con y sin atrás, `NpBottomBar`
  (Inicio/Dispositivos/Actividad/Más con `badgeCount = 3` en uno), `SegmentedControl` (Hoy/7 días/30 días),
  `FilterChips`, `UsageBar` (0 %, 40 %, 100 %), `TimelineItem` (3 seguidos, el último con `isLast`), `CodeDisplay`
  ("482 917", 143 s de 180 s; y vencido), `OtpInput` (vacío, a medias, error), `PermissionCard` (pendiente y concedido),
  `NpCard` normal y pulsable, `ListRow` con icono + chevron, y un botón que abre `ConfirmDialog` (normal y `danger`).
  Datos de ejemplo obviamente ilustrativos ("Tablet de Sofía"); **nada** de esto se usa fuera de `src/debug`.

## 8. Tests (androidTest)

Con `createComposeRule()` (dependencia ya añadida), en `mobile/app/src/androidTest/java/com/netprotect/app/ui/`:
- `DesignGalleryTest`: renderiza `DesignGallery()` y comprueba que existen nodos con los títulos de sección
  clave (`onNodeWithText("NpButton").assertExists()`, etc., desplazando si hace falta) y que **no** hay excepciones.
- `ComponentsTest`: `NpButton` con `loading = true` no llama a `onClick`; `NpButton` con `enabled = false` tampoco;
  `OtpInput` ignora letras y recorta a 6 dígitos al pegar `"12a34 56789"` → `"123456"`; `CodeDisplay` con 0 s muestra
  "El código venció"; `SeverityBadge("CRITICAL")` muestra "Crítica"; `ErrorState` invoca `onRetry` al tocar "Reintentar";
  todos los elementos pulsables de `NpBottomBar` miden **≥ 48 dp** (`assertHeightIsAtLeast(48.dp)`).
Se ejecutan con `./gradlew connectedDebugAndroidTest`, que necesita un dispositivo. Si hay uno conectado
(`adb devices`, con la ruta de `AGENTS.md`), ejecútalos y pega el resultado; si no, declara **PENDIENTE** y confirma al
menos que compilan con `./gradlew assembleDebugAndroidTest`.

## 9. Pasos (con comprobación)

1. Lee `AGENTS.md`, este encargo, el código de `ui/theme|icons|format`; mueve el encargo a `active/`.
2. Crea los componentes **uno a uno**; tras cada archivo: `cd mobile && ./gradlew compileDebugKotlin`.
3. Galería (§7) y manifiesto de debug; `./gradlew assembleDebug`.
4. Tests (§8); `./gradlew assembleDebugAndroidTest`.
5. Verificación (§10) e informe.

## 10. Verificación

Pega la salida **real**:
- `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → `BUILD SUCCESSFUL`;
  lint: 0 errores y avisos nuevos con ruta:línea (base: 26).
- `grep -rnE "Color\(0x" mobile/app/src/main/java/com/netprotect/app/ui/components mobile/app/src/debug` → solo las
  anotaciones `@Preview` (`backgroundColor = 0x…`), ningún `Color(0x…)`.
- `git status --short` y `git diff --stat`: solo archivos de §5.
- Si hay teléfono: instala el debug, abre la galería con el comando de §7 y toma capturas con `adb exec-out screencap -p`
  **fuera del repo** (carpeta temporal del sistema), una por bloque de la galería; menciónalas en el informe.
- Confirma que `mobile/app/src/main/AndroidManifest.xml` **no** contiene `DesignGalleryActivity`.

## 11. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos creados. 2. Salidas reales de §10 y de los tests. 3. Componentes con alguna decisión tuya
(y por qué). 4. Pendientes, sin adornos. 5. Preguntas `[PREGUNTA PARA CLAUDE]`. No simules ninguna salida.

---

## Informe de DeepSeek

### 1. Archivos creados

**`{pkg}/ui/components/`** (23 archivos, uno por componente):
`NpCard.kt`, `NpButton.kt` (+ `enum NpButtonVariant`), `IconTile.kt`, `ListRow.kt`, `StatusPill.kt`,
`SeverityBadge.kt`, `InfoBanner.kt`, `SectionHeader.kt`, `EmptyState.kt`, `LoadingState.kt`,
`ErrorState.kt`, `BrandHeader.kt`, `NpTopBar.kt`, `NpBottomBar.kt` (+ `data class NpBottomItem`),
`SegmentedControl.kt`, `FilterChips.kt`, `UsageBar.kt`, `TimelineItem.kt`, `DayHeader.kt`,
`CodeDisplay.kt`, `OtpInput.kt`, `ConfirmDialog.kt`, `PermissionCard.kt`.

**`mobile/app/src/debug/`**: `java/com/netprotect/app/debug/DesignGallery.kt`,
`java/com/netprotect/app/debug/DesignGalleryActivity.kt`, `AndroidManifest.xml`.

**`mobile/app/src/androidTest/java/com/netprotect/app/ui/`**: `DesignGalleryTest.kt`, `ComponentsTest.kt`.

### 2. Salidas reales (§10 y tests)

`cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL** (37 s, 116 tasks).

`connectedDebugAndroidTest` (había emulador `Pixel_8` en `adb devices`): **16 tests, 0 fallos, 0 saltados** —
`DesignGalleryTest.galleryRendersEverySectionTitle` (1), `ComponentsTest` (7: loading/disabled no llaman a
`onClick`, OtpInput "12a34 56789"→"123456", CodeDisplay 0 s→"El código venció", SeverityBadge CRITICAL→"Crítica",
ErrorState invoca `onRetry`, NpBottomBar ítems ≥48 dp) y los 8 Room preexistentes.

`lintDebug`: **0 errores, 25 avisos**, ninguno en `ui/components` ni `src/debug`. (Durante el desarrollo aparecieron
3 `AutoboxingStateCreation` en `DesignGallery.kt` por `mutableStateOf(0)`; corregidos a `mutableIntStateOf` y
desaparecieron.)

`grep -rnE "Color\(0x" .../ui/components .../src/debug` → **sin coincidencias** (solo `@Preview(backgroundColor = 0xFFF5F9FF)`).

`mobile/app/src/main/AndroidManifest.xml` **no** contiene `DesignGalleryActivity` (verificado).

Galería abierta en el emulador (`adb shell am start -n com.netprotect.app/com.netprotect.app.debug.DesignGalleryActivity`)
y capturadas 6 pantallas con `adb shell screencap -p` + `pull` (3 posiciones distintas: arriba/medio/final) en
`%TEMP%\sprint42b-gallery\`, fuera del repo.

`git status --short` / `git diff --stat`: solo archivos de §5.

### 3. Componentes con decisiones mías

- **`UsageBar`**: la barra es de 8 dp y no hay radio de 4 dp en `NpShapes` (los radios literales están prohibidos);
  usé `NpShapes.Sm` (8 dp), que sobre una barra de 8 dp da extremos totalmente redondeados.
- **`NpButton`**: usé `heightIn(min = 48/40 dp)` (mínimo, no fijo) para que el texto pueda envolver a dos líneas sin
  aplastarse en vertical, tal como exige el encargo ("no `TextButton` de Material").
- **`CodeDisplay`**: agrupo los 6 dígitos 3+3 con espacio fino U+2009 ("espacio fino" del encargo); "Vence en m:ss" se
  calcula de `remainingSeconds` (143 s → "2:23"; el "2:59" del encargo es ilustrativo del formato).
- **`SegmentedControl`/`FilterChips`**: opciones a 48 dp (el encargo dice "Alto ≥ 48 dp" y `AGENTS.md` exige objetivo
  táctil ≥48 dp), más altas que los 32 px visuales de `DESIGN.md`.
- **Variante `Text` de `NpButton`**: "pulsado → SignalBlueText" lo resuelvo con `collectIsPressedAsState`.
- **`StatusPill`/`SeverityBadge`**: el icono lleva `contentDescription = null` (decorativo; el texto ya informa).

### 4. Pendientes

Ninguno en lo que depende de mí. El E2E visual de la galería lo revisa Claude (yo no puedo ver las capturas:
este modelo no acepta imágenes); las capturas quedan listas en `%TEMP%\sprint42b-gallery\`.

### 5. Preguntas para Claude

Ninguna.

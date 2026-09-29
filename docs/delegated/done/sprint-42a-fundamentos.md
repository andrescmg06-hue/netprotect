# Sprint 42a — Fundamentos del sistema de diseño Android (tema, fuente, iconos, formateadores)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Al terminar existe, **sin haber cambiado ninguna pantalla**: `NetProtectTheme` con los colores, la
tipografía y las formas de `DESIGN.md`; la fuente Inter; los iconos Lucide como `VectorDrawable`;
el logo y el icono de lanzador; formateadores de fechas/duraciones y etiquetas en español, con tests JVM.
La app se ve igual que antes salvo el icono de lanzador. Nada de esto se usa todavía en pantallas
(Claude envuelve `MainActivity` en el tema en la revisión siguiente; el encargo 42b construye los
componentes encima de lo que hagas aquí).

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`. `{res}` = `mobile/app/src/main/res`.
`{test}` = `mobile/app/src/test/java/com/netprotect/app`.

## 2. Decisiones ya tomadas (no las reabras)

| # | Decisión |
|---|---|
| D-03 | Iconos: SVG de Lucide convertidos a `VectorDrawable` en `{res}/drawable/ic_*.xml`; solo la lista cerrada de §5.3. Sin dependencia. Licencia ISC. |
| D-04 | Inter empaquetada en `{res}/font/` (pesos 400/500/600/700, licencia OFL). Sin Google Fonts descargable. |
| — | Solo tema **claro**. No hay tema oscuro. |
| — | Sin dependencias de Gradle nuevas. No toques `build.gradle.kts` ni `libs.versions.toml`. |
| — | Nada de `java.util.Locale`/`DateTimeFormatter` con `Locale` para textos: los meses, "a. m."/"p. m." y demás se construyen **a mano** (los datos de idioma de la JVM cambian entre versiones, p. ej. el espacio antes de "p. m."; aquí debe ser un espacio normal U+0020 y el resultado idéntico en cualquier máquina). |
| — | Las etiquetas de nivel/tipo de alerta, motivo de bloqueo y categoría se copian **literalmente** del panel web (§5.6). Las de acciones de auditoría y estados nuevos las da este encargo. |

## 3. Lo que NO debes tocar

`backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`,
`mobile/app/build.gradle.kts`, `mobile/gradle/libs.versions.toml`, `{pkg}/core/**`, `{pkg}/feature/**`,
`{pkg}/MainActivity.kt`, `{pkg}/ui/navigation/**` (es de Claude), `{res}/values/styles.xml`,
`AndroidManifest.xml` **salvo** las dos líneas de icono de §5.5. Sin `git commit`/`push`.

## 4. Punto de partida (léelo antes de escribir)

- `DESIGN.md` (raíz): la sección `colors:`/`typography:`/`rounded:` del principio y las secciones
  Colors, Typography, Elevation, Shapes. Es la fuente de verdad; la tabla de §5.1 es una copia para que
  no tengas que interpretarla.
- `.claude/rules/android.md`, `docs/android-redesign/mockups/README.md` (sección "Sistema visual").
- `frontend/src/lib/{alertFormatting,ruleFormatting,categoryFormatting}.ts` y
  `frontend/src/components/shell/deviceStatus.ts` (etiquetas web).
- `frontend/public/brand/logo-shield.png`, `logo-full.png`.
- `frontend/src/components/login/GoogleButton.tsx` (constante `GOOGLE_G`: los 4 trazados de la "G").
- Hoy no existe `{pkg}/ui/theme`, `ui/format` ni recursos de fuente/iconos. `{res}` solo tiene `values/` y `xml/`.

## 5. Archivos permitidos (lista cerrada) y contenido

Crear: `{pkg}/ui/theme/{Color,Type,Shape,Elevation,Tone,Theme}.kt`,
`{pkg}/ui/icons/NpIcons.kt`, `{pkg}/ui/format/{RelativeTime,Durations,DayGrouping,Clock,AlertLabels,RuleLabels,CategoryLabels,AuditLabels,DeviceStatusLabels}.kt`,
`{res}/font/inter_{regular,medium,semibold,bold}.ttf`, `{res}/drawable/ic_*.xml` (§5.3) y
`ic_google_g.xml`, `{res}/drawable-nodpi/{logo_shield,logo_full}.png`,
`{res}/mipmap-anydpi-v26/{ic_launcher,ic_launcher_round}.xml`, `{res}/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png`,
`{res}/values/ic_launcher_background.xml`, `mobile/third_party_licenses/{INTER-OFL.txt,LUCIDE-ISC.txt,README.md}`,
`{test}/ui/format/*Test.kt`, `{test}/ui/theme/ColorTokensTest.kt`.
Modificar: `mobile/app/src/main/AndroidManifest.xml` (solo §5.5).

### 5.1 `ui/theme/Color.kt`

Un `object NpColors` con un `val` por color (`Color(0xFF…)`; **este es el único archivo permitido con
literales `Color(0x…)`**). Nombres y valores (copiados de `DESIGN.md`):

| Nombre Kotlin | Hex | | Nombre Kotlin | Hex |
|---|---|---|---|---|
| `SignalBlue` | `#246BFE` | | `Success` | `#16B364` |
| `SignalBlueDeep` | `#1456D9` | | `SuccessWash` | `#E7F8EF` |
| `SignalBlueText` | `#1F5FE6` | | `SuccessText` | `#0B7A42` |
| `BlueWash` | `#EAF1FF` | | `Danger` | `#F04438` |
| `BlueMist` | `#F3F7FF` | | `DangerWash` | `#FEECEB` |
| `ShieldNavy` | `#102B63` | | `DangerText` | `#C4271C` |
| `SkyGround` | `#F5F9FF` | | `Warning` | `#F79009` |
| `PaperWhite` | `#FFFFFF` | | `WarningWash` | `#FEF3E2` |
| `PaperMuted` | `#F7F9FC` | | `WarningText` | `#9A5000` |
| `Hairline` | `#E6ECF5` | | `Violet` | `#7A5AF8` |
| `HairlineStrong` | `#D5DFEE` | | `VioletWash` | `#F1EDFE` |
| `Ink` | `#1B2B4B` | | `VioletText` | `#5B3FD6` |
| `SlateMuted` | `#5B6B82` | | `NeutralWash` | `#EEF2F7` |
| `SlateSubtle` | `#94A3B8` | | `SecondaryBorder` | `#CFDCF7` |
| `SkyGroundEnd` | `#EEF4FF` | | `DangerHoverWash` | `#FDDCD9` |

Además, `val NetProtectColorScheme = lightColorScheme(...)` con: `primary=SignalBlue`,
`onPrimary=PaperWhite`, `primaryContainer=BlueWash`, `onPrimaryContainer=SignalBlueDeep`,
`secondary=SignalBlueText`, `onSecondary=PaperWhite`, `background=SkyGround`, `onBackground=Ink`,
`surface=PaperWhite`, `onSurface=Ink`, `surfaceVariant=BlueMist`, `onSurfaceVariant=SlateMuted`,
`outline=HairlineStrong`, `outlineVariant=Hairline`, `error=Danger`, `onError=PaperWhite`,
`errorContainer=DangerWash`, `onErrorContainer=DangerText`.

Regla de uso (comentario en el archivo): **`SignalBlue` es para rellenos y bordes; el texto e iconos
azules usan `SignalBlueText`** (contraste AA). `SlateSubtle` solo para placeholders.

### 5.2 `Type.kt`, `Shape.kt`, `Elevation.kt`, `Tone.kt`, `Theme.kt`

- `Type.kt`: `val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_bold, FontWeight.Bold))`.
  `object NpText` con estos `TextStyle` (tamaños en **sp**; `lineHeight` = tamaño × factor; `letterSpacing` en `em`):

  | Estilo | sp | Peso | Interlineado | letterSpacing | Extra |
  |---|---|---|---|---|---|
  | `Display` | 32 | 700 | ×1.2 | −0.02em | |
  | `Headline` | 28 | 700 | ×1.2 | −0.01em | `fontFeatureSettings = "tnum"` |
  | `Title` | 18 | 600 | ×1.2 | 0 | |
  | `BodyLead` | 15 | 400 | ×1.5 | 0 | |
  | `Body` | 14 | 400 | ×1.5 | 0 | |
  | `BodyStrong` | 14 | 600 | ×1.5 | 0 | |
  | `Label` | 13 | 600 | ×1.5 | 0 | |
  | `Caption` | 12 | 600 | ×1.5 | 0 | |
  | `Numeric` | 28 | 700 | ×1.2 | −0.01em | `tnum` (cifras que no bailan) |
  | `NumericSmall` | 15 | 600 | ×1.5 | 0 | `tnum` |

  Y `val NetProtectTypography = Typography(...)` mapeando: `displayMedium`←Display, `headlineMedium`←Headline,
  `titleMedium`←Title, `bodyLarge`←BodyLead, `bodyMedium`←Body, `labelLarge`←Label, `labelSmall`←Caption.
  El color **no** va en los estilos (lo pone el componente).
- `Shape.kt`: `object NpShapes { val Sm = RoundedCornerShape(8.dp); val Md = 10.dp; val Lg = 14.dp; val Xl = 16.dp; val Pill = RoundedCornerShape(50) }` (`Md`, `Lg`, `Xl` también como `RoundedCornerShape`) y `val NetProtectShapes = Shapes(small = Sm, medium = Md, large = Xl)`.
- `Elevation.kt`: `object NpElevation { val Card = 2.dp; val Pop = 12.dp; val Glow = 4.dp }` y colores de sombra `NpShadow.Ambient = ShieldNavy.copy(alpha = 0.08f)`, `NpShadow.Spot = ShieldNavy.copy(alpha = 0.10f)`. Comentario: las sombras de `DESIGN.md` son en dos capas y no se reproducen exactas en Compose; Claude las ajusta al ver la galería.
- `Tone.kt`: `enum class NpTone(val solid: Color, val wash: Color, val text: Color)`:
  `Success(Success, SuccessWash, SuccessText)`, `Danger(Danger, DangerWash, DangerText)`,
  `Warning(Warning, WarningWash, WarningText)`, `Info(SignalBlue, BlueWash, SignalBlueDeep)`,
  `Purple(Violet, VioletWash, VioletText)`, `Neutral(SlateMuted, NeutralWash, SlateMuted)`.
  (En el enum, referencia los colores como `NpColors.X` para no chocar con los nombres.)
- `Theme.kt`: `@Composable fun NetProtectTheme(content: @Composable () -> Unit)` =
  `MaterialTheme(colorScheme = NetProtectColorScheme, typography = NetProtectTypography, shapes = NetProtectShapes, content = content)`. Solo claro.

### 5.3 Iconos (`{res}/drawable/ic_*.xml`)

Fuente: repositorio oficial `github.com/lucide-icons/lucide` (carpeta `icons/`, `.svg`), tomado de **una
sola versión fija** (anótala en el informe y en `LUCIDE-ISC.txt`). Sitio alterno permitido: `lucide.dev`.
Nada de otros sitios ni paquetes de terceros.

Lista cerrada (nombre Lucide → archivo): `arrow-left, arrow-right, chevron-right, chevron-down,
refresh-cw, link, qr-code, smartphone, users, user, clock, file-text, info, log-in, log-out,
arrow-left-right, pencil, trash-2, layout-grid, map-pin, map, external-link, history, bar-chart-3,
triangle-alert, octagon-alert, siren, bell, bell-off, book-open, check, circle-check, circle-alert, ban,
home, graduation-cap, building-2, search, calendar, moon, shield, shield-check, key-round, eye, eye-off,
monitor, wifi, settings, lock, x, locate-fixed, more-horizontal`. El archivo lleva guion bajo en lugar de
guion y el prefijo `ic_` (`arrow-left` → `ic_arrow_left.xml`, `trash-2` → `ic_trash_2.xml`).
Si un nombre ya no existe con ese nombre en la versión elegida (Lucide renombra iconos; hay alias en el
archivo `.json` de cada icono), usa el nombre actual **pero conserva el nombre de archivo de la lista**
(p. ej. `bar-chart-3` puede llamarse `chart-column`, y `home` → `house`) y dilo en el informe.

Conversión a `VectorDrawable` (escribe un script propio **fuera del repo**, p. ej. en la carpeta temporal
del sistema; el script no se commitea):
`<vector width="24dp" height="24dp" viewportWidth="24" viewportHeight="24">` con cada elemento como un
`<path>` **de solo trazo**: `android:fillColor="#00000000"`, `android:strokeColor="#FF000000"`,
`android:strokeWidth="2"`, `android:strokeLineCap="round"`, `android:strokeLineJoin="round"`. Los `<circle>`,
`<rect>` (con `rx`), `<line>`, `<polyline>`, `<polygon>` y `<ellipse>` de Lucide se convierten a `pathData`
(arcos `A` para círculos). **Un icono con relleno por error es un defecto** (se verá como una mancha
sólida): todos son de trazo. El color negro es solo el valor por defecto; las pantallas lo tiñen con `tint`.
`ic_google_g.xml`: la "G" de 4 colores (los 4 `path` de `GOOGLE_G` con sus `fill`, `viewportWidth=48`,
`viewportHeight=48`), esta sí con relleno y sin trazo.

`ui/icons/NpIcons.kt`: `object NpIcons { @DrawableRes val ArrowLeft = R.drawable.ic_arrow_left … }` con una
constante por icono (nombre en PascalCase) más `GoogleG`, `LogoShield`, `LogoFull`.

### 5.4 Fuente, logo y licencias

- Inter: descarga la **versión estable más reciente** desde `github.com/rsms/inter` (releases). Usa los
  `.ttf` **estáticos** Regular, Medium, SemiBold y Bold → `inter_regular.ttf`, `inter_medium.ttf`,
  `inter_semibold.ttf`, `inter_bold.ttf`. Copia el texto de la licencia OFL a
  `mobile/third_party_licenses/INTER-OFL.txt`. Si no puedes descargar, **detente en este punto**, no uses
  otra fuente, y deja `[PREGUNTA PARA CLAUDE]` en el informe.
- Lucide: `LUCIDE-ISC.txt` con la licencia ISC del repo y la versión usada. `README.md` de esa carpeta:
  una tabla `Recurso | Origen | Licencia | Versión`.
- Logo: copia `frontend/public/brand/logo-shield.png` → `{res}/drawable-nodpi/logo_shield.png` y
  `logo-full.png` → `logo_full.png` (nombres en minúscula y guion bajo).

### 5.5 Icono de lanzador

- `{res}/values/ic_launcher_background.xml`: `<color name="ic_launcher_background">#F5F9FF</color>`.
- `{res}/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`: `<adaptive-icon>` con
  `<background android:drawable="@color/ic_launcher_background"/>` y
  `<foreground android:drawable="@mipmap/ic_launcher_foreground"/>`.
- `{res}/mipmap-<densidad>/ic_launcher_foreground.png`: el escudo (`logo-shield.png`, proporción 219×256)
  centrado sobre un lienzo **transparente cuadrado** de 108 dp por densidad (mdpi 108 px, hdpi 162, xhdpi
  216, xxhdpi 324, xxxhdpi 432), con el escudo escalado a **58 dp de alto** (zona segura del icono
  adaptativo). Genéralos con Python + Pillow (`pip install pillow` en un entorno temporal si falta) o
  ImageMagick, con un script **fuera del repo**.
- `AndroidManifest.xml`: en `<application>` añade **solo** `android:icon="@mipmap/ic_launcher"` y
  `android:roundIcon="@mipmap/ic_launcher_round"`. Nada más del manifiesto.

### 5.6 `ui/format/` — formateadores y etiquetas

Todo son **funciones puras** en `object`s o funciones de nivel superior; el tiempo entra por parámetro
(nunca `Instant.now()` dentro). Meses: `enero, febrero, marzo, abril, mayo, junio, julio, agosto,
septiembre, octubre, noviembre, diciembre`.

- `RelativeTime.format(instant: Instant, now: Instant, zone: ZoneId): String`:
  diferencia < 60 s o **futura** (desfase de reloj) → `"hace un momento"`; 1–59 min → `"hace N min"`;
  1–23 h → `"hace N h"`; si el día calendario (en `zone`) fue **ayer** → `"ayer"`; en otro caso
  `"26 de septiembre de 2026"` (día sin cero a la izquierda). Orden de evaluación: primero minutos/horas,
  después ayer/fecha (24 h o más).
- `Durations.format(totalSeconds: Long): String`: `< 60` → `"menos de 1 min"`; `< 3600` → `"48 min"`;
  si no `"1 h 24 min"`, y horas exactas `"2 h"` (sin `"0 min"`). Negativos se tratan como 0.
- `DayGrouping.label(date: LocalDate, today: LocalDate): String`: `"Hoy"`, `"Ayer"`, o `"26 de septiembre de 2026"`.
- `Clock.format(time: LocalTime): String`: `"3:42 p. m."`, `"12:05 a. m."`, `"12:00 p. m."` (medianoche = 12:xx a. m.,
  mediodía = 12:xx p. m.), hora sin cero a la izquierda, minutos con dos dígitos, espacio normal antes de `a. m.`/`p. m.`.
- **Etiquetas** (una función `label(value: String): String` por archivo; si el valor es desconocido,
  devuelve el propio valor **sin dejarlo vacío**):
  - `AlertLabels.kt`: `levelLabel("INFO"→"Info","WARNING"→"Advertencia","HIGH"→"Alta","CRITICAL"→"Crítica")`;
    `levelTone("INFO"→Info,"WARNING"→Warning,"HIGH"→Danger,"CRITICAL"→Purple)`;
    `levelIcon` (`NpIcons.Info`, `TriangleAlert`, `OctagonAlert`, `Siren`);
    `alertMessage(type, packageName, geofenceName)` con los textos **literales** de `alertLabel()` de
    `alertFormatting.ts` (9 tipos: `APP_BLOCKED, APP_LIMIT_REACHED, GEOFENCE_EXIT, GEOFENCE_ENTER,
    PERMISSION_REVOKED, SERVICE_INACTIVE, HEARTBEAT_SILENCE, CLOCK_TAMPERING, UNINSTALL_ATTEMPT`);
    `tamperSignalLabel` con `TAMPER_SIGNAL_LABEL`.
  - `RuleLabels.kt`: `ruleTypeLabel` con el mapa de `ruleFormatting.ts` (8 valores: `ALLOW, BLOCK,
    DAILY_LIMIT, WEEKLY_LIMIT, SCHEDULE, CATEGORY, SCHOOL_MODE, DEFAULT_POLICY`).
  - `CategoryLabels.kt`: `CATEGORY_LABELS` de `categoryFormatting.ts` (11 valores: `SOCIAL_MEDIA, GAMES,
    STREAMING, EDUCATION, PRODUCTIVITY, COMMUNICATION, NEWS, SHOPPING, FINANCE, UTILITIES, ADULT_CONTENT`).
  - `DeviceStatusLabels.kt` (6 valores) → `(label, tone)`: `ONLINE`→`"En línea"`/Success,
    `OFFLINE`→`"Desconectado"`/Neutral, `ALERT`→`"Alerta"`/Danger (estos tres, literales de la web),
    `SYNCING`→`"Sincronizando"`/Info, `RESTRICTED`→`"Restringido"`/Warning, `UNLINKED`→`"Desvinculado"`/Neutral.
  - `AuditLabels.kt` (25 acciones, **estas son las etiquetas**; el panel web no las traduce):
    `LOGIN`→`Inicio de sesión`, `LOGOUT`→`Cierre de sesión`, `TOKEN_REFRESH`→`Renovación de sesión`,
    `ROLE_GRANTED`→`Modo asignado`, `PAIRING_CODE_GENERATED`→`Código de vinculación generado`,
    `PAIRING_CODE_REVOKED`→`Código de vinculación revocado`, `DEVICE_LINKED`→`Dispositivo vinculado`,
    `DEVICE_UNLINKED`→`Dispositivo desvinculado`, `DEVICE_RENAMED`→`Dispositivo renombrado`,
    `DEVICE_POLICY_CHANGED`→`Política del dispositivo cambiada`, `SCHOOL_MODE_CHANGED`→`Horario escolar cambiado`,
    `APP_RULE_DELETED`→`Regla de app eliminada`, `CATEGORY_RULE_DELETED`→`Regla de categoría eliminada`,
    `APP_CATEGORY_UNASSIGNED`→`App quitada de su categoría`, `GEOFENCE_CREATED`→`Geocerca creada`,
    `GEOFENCE_UPDATED`→`Geocerca actualizada`, `GEOFENCE_DELETED`→`Geocerca eliminada`,
    `ALERT_READ`→`Alerta marcada como leída`, `ALERT_SILENCED`→`Alerta silenciada`,
    `ALERT_SILENCE_REMOVED`→`Silencio de alerta quitado`, `SCREEN_SHARE_REQUESTED`→`Vista remota solicitada`,
    `SCREEN_SHARE_CONSENT_GRANTED`→`Vista remota autorizada`, `SCREEN_SHARE_CONSENT_DENIED`→`Vista remota rechazada`,
    `SCREEN_SHARE_STARTED`→`Vista remota iniciada`, `SCREEN_SHARE_STOPPED`→`Vista remota terminada`.
    Si en `backend/app` (`grep -rn "record_audit_event" backend/app`) encuentras **otras** acciones que no
    estén en esta lista, no inventes su etiqueta: dilo en el informe.

### 5.7 Tests JVM (`{test}/ui/...`)

Con JUnit 4 (ya está en el proyecto), fechas fijas, `ZoneId.of("America/Bogota")`:
- `RelativeTimeTest`: 30 s → "hace un momento"; instante futuro → "hace un momento"; 12 min → "hace 12 min";
  59 min; 60 min → "hace 1 h"; 23 h; ayer a las 23:00 visto a las 09:00 → "ayer"; 2 días → "…de …".
  Un caso que cruza medianoche en `America/Bogota` (UTC−5) para probar que usa la zona, no UTC.
- `DurationsTest`: 0, 59 s, 60 s → "1 min", 2880 s → "48 min", 3600 s → "1 h", 5040 s → "1 h 24 min", 7200 s → "2 h", −5.
- `DayGroupingTest`, `ClockTest` (00:05, 12:00, 12:30, 15:42, 23:59, 09:07).
- `LabelsTest`: para **cada** lista de valores de §5.6 (los enums reales, copiados literalmente en el test)
  comprueba que la etiqueta existe, no está vacía y **no es igual al valor crudo** salvo desconocidos;
  y un test de valor desconocido (`"NUEVO_VALOR"` → `"NUEVO_VALOR"`).
- `ColorTokensTest`: comprueba con `NpColors.X.toArgb()` (o `.value`) los 30 hex de §5.1, uno por uno.

## 6. Pasos (con comprobación)

1. Lee `AGENTS.md`, este encargo, `DESIGN.md`; mueve el encargo a `active/`.
2. Crea `ui/theme/*` (§5.1–5.2) sin `R.font` aún si la fuente no está; **añade la fuente primero** (§5.4) para
   que `R.font.inter_*` exista. `cd mobile && ./gradlew compileDebugKotlin` tras la fuente y tras cada archivo nuevo.
3. Iconos (§5.3): genera con tu script; abre 5 al azar y comprueba que son de trazo (`fillColor="#00000000"`).
   `NpIcons.kt`; `compileDebugKotlin`.
4. Logo e icono de lanzador (§5.5). `./gradlew assembleDebug` debe pasar (valida los recursos y el manifiesto).
5. `ui/format/*` (§5.6) y sus tests (§5.7). `./gradlew testDebugUnitTest`.
6. Licencias (§5.4).
7. Verificación final (§7) y tu informe.

## 7. Verificación

Ejecuta y pega la salida **real** en tu informe:
- `cd mobile && ./gradlew --console=plain test assembleDebug lintDebug` → `BUILD SUCCESSFUL`. Lint: 0 errores;
  reporta el número de avisos (base: 26) y lista cualquiera **nuevo** con ruta:línea.
- Conteo de tests por clase (`app/build/test-results/testDebugUnitTest/*.xml`).
- `git diff --stat` y `git status --short`: solo archivos de §5.
- `grep -rnE "Color\(0x" mobile/app/src/main/java | grep -v "ui/theme/Color.kt"` → **sin coincidencias** en tus archivos
  (los de pantallas existentes no son tuyos).
- Si el teléfono está conectado (`adb devices` con la ruta completa de `AGENTS.md`): instala el APK y comprueba
  que la app abre y el icono es el escudo. Si no, decláralo **PENDIENTE**.

## 8. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos creados/modificados (lista). 2. Versiones usadas de Inter y de Lucide. 3. Salidas reales de §7.
4. Iconos con nombre distinto al de la lista (tabla). 5. Acciones de auditoría no listadas (si las hay).
6. Pendientes, sin adornos. 7. Preguntas `[PREGUNTA PARA CLAUDE]`. No simules ninguna salida.

---

## Informe de DeepSeek

### 1. Archivos creados / modificados

**Creados — `{pkg}/ui/theme/`**: `Color.kt`, `Type.kt`, `Shape.kt`, `Elevation.kt`, `Tone.kt`, `Theme.kt`.
**Creados — `{pkg}/ui/icons/`**: `NpIcons.kt`.
**Creados — `{pkg}/ui/format/`**: `RelativeTime.kt`, `Durations.kt`, `DayGrouping.kt`, `Clock.kt`, `AlertLabels.kt`, `RuleLabels.kt`, `CategoryLabels.kt`, `AuditLabels.kt`, `DeviceStatusLabels.kt`.
**Creados — `{res}/font/`**: `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`.
**Creados — `{res}/drawable/`**: 52 `ic_*.xml` (Lucide) + `ic_google_g.xml`.
**Creados — `{res}/drawable-nodpi/`**: `logo_shield.png`, `logo_full.png`.
**Creados — `{res}/mipmap-anydpi-v26/`**: `ic_launcher.xml`, `ic_launcher_round.xml`.
**Creados — `{res}/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`**: `ic_launcher_foreground.png` (5).
**Creados — `{res}/values/`**: `ic_launcher_background.xml`.
**Creados — `mobile/third_party_licenses/`**: `INTER-OFL.txt`, `LUCIDE-ISC.txt`, `README.md`.
**Creados — tests** (`{test}/ui/format/`, `{test}/ui/theme/`): `RelativeTimeTest.kt`, `DurationsTest.kt`, `DayGroupingTest.kt`, `ClockTest.kt`, `LabelsTest.kt`, `ColorTokensTest.kt`.
**Modificado**: `mobile/app/src/main/AndroidManifest.xml` (solo `android:icon` + `android:roundIcon`; +2 líneas).

### 2. Versiones usadas

- **Inter**: v4.1 (release de `github.com/rsms/inter`; `.ttf` estáticos de `extras/ttf/`).
- **Lucide**: 1.48.0 (`github.com/lucide-icons/lucide`, carpeta `icons/`).

### 3. Salidas reales (§7)

`cd mobile && ./gradlew --console=plain test assembleDebug lintDebug` → **BUILD SUCCESSFUL** (26 s, 86 tasks).

Conteo de tests JVM (`app/build/test-results/testDebugUnitTest/*.xml`), todos con 0 fallos/errores:

| Clase | tests |
|---|---|
| `ui.format.ClockTest` | 1 |
| `ui.format.DayGroupingTest` | 1 |
| `ui.format.DurationsTest` | 5 |
| `ui.format.LabelsTest` | 10 |
| `ui.format.RelativeTimeTest` | 8 |
| `ui.theme.ColorTokensTest` | 6 |

(pre-existentes, también en verde: `TokenSessionTest` 13, `RuleEvaluatorTest` 34, `NavStackTest` 12, `ExampleUnitTest` 1).

`lintDebug`: **0 errores, 28 avisos**. Los avisos **nuevos** (introducidos por este encargo) son 3, todos del icono de lanzador:
- `MonochromeLauncherIcon` ×2 — `res/mipmap-anydpi-v26/ic_launcher.xml:2` y `ic_launcher_round.xml:2` (el `<adaptive-icon>` no lleva capa `<monochrome>`; §5.5 no la pide).
- `ObsoleteSdkInt` ×1 — carpeta `res/mipmap-anydpi-v26/` (`-v26` es redundante con `minSdk 26`; es la carpeta que pide §5.5).

El resto (25) son pre-existentes (`GradleDependency` 9, `UseKtx` 11, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 1, `DataExtractionRules` 1). El plan daba "base: 26"; yo mido 25 pre-existentes + 3 nuevos = 28. Ningún aviso en `ui/theme`, `ui/icons` ni `ui/format`.

`grep -rnE "Color\(0x" mobile/app/src/main/java` (excluido `ui/theme/Color.kt`) → solo en `feature/**` pre-existentes (no míos); **ninguno** en mis archivos.

`git status --short` / `git diff --stat`: solo archivos de §5 (la lista del punto 1); `AndroidManifest.xml` +2 líneas. Sin commits ni push; `GIT_OPTIONAL_LOCKS=0` en todo `git`.

### 4. Iconos con nombre distinto al de la lista

| Nombre de la lista | Nombre real (Lucide 1.48.0) | Archivo generado |
|---|---|---|
| `trash-2` | `trash` | `ic_trash_2.xml` |
| `history` | `rotate-ccw-clock` | `ic_history.xml` |
| `bar-chart-3` | `chart-column` | `ic_bar_chart_3.xml` |
| `home` | `house` | `ic_home.xml` |
| `building-2` | `building-complex` | `ic_building_2.xml` |
| `more-horizontal` | `ellipsis` | `ic_more_horizontal.xml` |

Los 46 restantes conservan el nombre de la lista. Todos son de trazo (`fillColor="#00000000"`), salvo `ic_google_g.xml` (4 colores, relleno).

### 5. Acciones de auditoría no listadas

**Ninguna.** Las 25 de §5.6 son exactamente las que emite el backend: 20 literales `action="…"` en `record_audit_event` + 5 de `_audit_action_for` (vista remota). No hay acciones extra que etiquetar.

### 6. Pendientes

- **Instalación en dispositivo/emulador**: `adb devices` no devuelve ningún dispositivo conectado, así que no pude instalar el APK ni comprobar en pantalla que el icono de lanzador es el escudo. **PENDIENTE**.

### 7. Preguntas para Claude

Ninguna.

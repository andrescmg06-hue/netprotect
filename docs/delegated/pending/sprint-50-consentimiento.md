# Sprint 50 — Supervisado: Consentimiento de vista remota, Estado de NetProtect y banda de aviso (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Tres piezas del modo supervisado se ven como los mockups:
- **Consentimiento** (`docs/android-redesign/mockups/15-supervisado-consentimiento.jpg`).
- **Estado de NetProtect** (`17-supervisado-servicios.jpg`).
- La **banda de aviso** que se ve mientras el tutor está viendo la pantalla.

Todas con los textos verdaderos de §3.

**Solo escribes presentación.** Este es el sprint más sensible del rediseño: es el consentimiento de un menor para que
vean su pantalla. Qué se envía al tutor, cuándo se abre el diálogo de Android y qué servicios corren lo decide
`SupervisedShell.kt`, que **no puedes abrir ni modificar**. Tus pantallas solo muestran valores y llaman a los
callbacks que reciben.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- **`SupervisedShell.kt`:** la reconexión del canal, el enrutado, el tiempo límite de 2 minutos, lo que se envía al
  aceptar o rechazar y el diálogo de Android. **PROHIBIDO tocarlo.**
- **`{pkg}/core/status/ServiceStatusRegistry.kt`:**
  - `ServicesView(report, lastReport, appControl, location, screenShareActive, screenShareSince)`.
  - `ServiceState` (`Active` / `Inactive` / `Unconfirmed`). La vista nunca dice «Activo» sin pruebas.
- **El acceso a «Estado de NetProtect»** desde Dispositivo vinculado ya existe (Claude lo añadió).
- **Tres archivos** existen con **firma final** y un cuerpo provisional. **Reemplaza los cuerpos; no cambies ninguna
  firma:**

```kotlin
// {pkg}/feature/supervised/consent/ScreenShareConsentScreen.kt
@Composable fun ScreenShareConsentScreen(onContinue: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier)
// {pkg}/feature/supervised/services/ServicesStatusScreen.kt
@Composable fun ServicesStatusScreen(view: ServicesView, now: Instant, onStopScreenShare: () -> Unit,
                                     onBack: () -> Unit, modifier: Modifier = Modifier)
// {pkg}/ui/components/ActiveShareBanner.kt
@Composable fun ActiveShareBanner(onStop: () -> Unit, modifier: Modifier = Modifier)
```

**Reglas que no puedes romper:**
- **Casilla del consentimiento:** el estado vive **dentro** de `ScreenShareConsentScreen` con
  `rememberSaveable { mutableStateOf(false) }`. Empieza **desmarcada** en cada petición y **«Continuar» solo está
  habilitado con la casilla marcada** (D-12 a). No hay ningún otro modo de aceptar.
- **`ActiveShareBanner`** dibuja su propio `statusBarsPadding()`, porque va encima de la pantalla. Mantenlo.

## 3. Lo que el mockup dice mal (obligatorio)

| Mockup | Qué dice | Qué poner |
|---|---|---|
| 15 | «Puedes revocar el permiso… desde los ajustes del dispositivo» | **FALSO.** Usa el tercer punto de §5.1 («Puedes detenerla cuando quieras»). |
| 15 | «Se te redirigirá a los ajustes del sistema para conceder el acceso» | **FALSO.** Usa el aviso de §5.1. |
| 15 | «…cuando lo solicite desde la app NetProtect» | El tutor lo pide desde el **panel web**. |
| 15 | «Entiendo que mi tutor podrá ver la pantalla… cuando lo solicite» | Suena a permiso permanente, y **no lo es**: vale para esta vez. Usa el texto de §5.1. |
| 15 | Flecha de volver | **Sin flecha.** Volver no es una respuesta; solo responden los botones. |
| 17 | «La visualización de pantalla se detiene cuando tu tutor la finaliza» | Incompleto: el menor también puede detenerla. Usa §5.2. |
| 17 | «Supervisión activa» como título de notificación | No existe. Los títulos reales están en §5.2. |
| 17 | «Cerrar sesión» | **No va aquí** (decisión del dueño). |
| todos | «graba», «controla», «para siempre», «siempre» | Prohibido salvo en los dos textos literales de §5: «No se graba ni se controla el dispositivo.» y «NetProtect no graba la pantalla ni controla el dispositivo…». |

## 4. Lo que NO debes tocar y archivos permitidos

No toques:
- `SupervisedShell.kt`, `SupervisedViews.kt`, `LinkedDeviceScreen.kt`;
- `{pkg}/core/**` y `{pkg}/ui/**`, salvo `ActiveShareBanner.kt`;
- `res/`, Gradle, manifiestos, `backend/`, `frontend/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`.

No hagas `git commit` ni `push`. Si necesitas algo que esté fuera de esta lista, escribe `[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar los cuerpos de los tres archivos de §2. Puedes añadir composables `private` en esos archivos.
- Crear `mobile/app/src/androidTest/java/com/netprotect/app/ui/{ScreenShareConsentScreenTest,ServicesStatusScreenTest,ActiveShareBannerTest}.kt`.
- Modificar `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantallas

**Estructura común de las dos pantallas:**
- `Box` con `fillMaxSize()` y fondo `NpColors.SkyGround`;
- dentro, una `Column` con `statusBarsPadding()`, `verticalScroll` y relleno horizontal de 20 dp, y 24 dp al final;
- arriba, `BrandHeader(subtitle = "Dispositivo supervisado")`, 24 dp, el título (`NpText.Display`, `ShieldNavy`), 8 dp,
  el subtítulo (`Body`, `SlateMuted`) y 20 dp.

**Prohibido:** `Color(0x…)`, tamaños de letra o radios fijos, `Instant.now()`, `Context`/`LocalContext`, cualquier
`Intent`, `JSONObject` o `send`.

### 5.1 `ScreenShareConsentScreen` (mockup 15)

- **Título:** «Consentimiento para ver la pantalla».
- **Subtítulo:** «Tu tutor pidió ver la pantalla de este dispositivo ahora.».

Contenido, en orden:
1. **Tarjeta «¿Qué permite?»** (`NpCard(containerColor = NpColors.BlueWash, borderColor = NpColors.BlueWash)`): fila
   con `IconTile(NpIcons.Eye, NpTone.Info, 48.dp)`, 12 dp y una columna con **«¿Qué permite?»** (`Title`) y **«Que tu
   tutor vea la pantalla de este dispositivo en tiempo real, solo durante esta sesión.»** (`Body`, `SlateMuted`).
2. 20 dp. **«¿Cuándo se puede usar?»** (`NpText.Headline`, `ShieldNavy`). 8 dp.
3. **`NpCard` con tres filas** (16 dp entre ellas). Cada fila lleva `IconTile` de 44 dp, 12 dp y una columna con el
   título (`BodyStrong`, `ShieldNavy`) y la descripción (`Body`, `SlateMuted`):

   | Icono y tono | Título | Descripción |
   |---|---|---|
   | `NpIcons.Clock`, `Purple` | **«Solo cuando tu tutor lo pide»** | **«No es continua: tu tutor la pide desde el panel web y tú decides cada vez.»** |
   | `NpIcons.Shield`, `Success` | **«Con fines de supervisión»** | **«Se usa para acompañar tu seguridad digital.»** |
   | `NpIcons.EyeOff`, `Info` | **«Puedes detenerla cuando quieras»** | **«Desde la notificación de Android o desde el aviso azul de esta app.»** |

4. 20 dp. **«Tu consentimiento»** (`Headline`, `ShieldNavy`). 8 dp.
5. **`NpCard` con la casilla.** Toda la fila es pulsable con
   `Modifier.toggleable(value = authorized, role = Role.Checkbox, onValueChange = { authorized = it })`, de modo que
   también se marca tocando el texto. Contiene:
   - `Checkbox(checked = authorized, onCheckedChange = null)`;
   - 12 dp;
   - una columna con **«Autorizo a mi tutor a ver la pantalla de este dispositivo ahora»** (`BodyStrong`, `ShieldNavy`)
     y **«Solo para esta sesión. Android te pedirá confirmarlo.»** (`Body`, `SlateMuted`).
6. 16 dp. `NpButton("Continuar", onContinue, Modifier.fillMaxWidth(), enabled = authorized)`.
7. 8 dp. `NpButton("Ahora no", onDecline, Modifier.fillMaxWidth(), variant = NpButtonVariant.Secondary)`.
8. 16 dp. `InfoBanner(title = "Información importante", text = "Android te pedirá confirmarlo en un diálogo del sistema.
   Mientras se comparta verás un aviso permanente. No se graba ni se controla el dispositivo.")`.

Comprobado por Claude: el vídeo no se graba ni se guarda (`docs/sprint-23.md`, punto 7), la notificación tiene el
botón «Detener» y Android muestra su diálogo cada vez.

### 5.2 `ServicesStatusScreen` (mockup 17)

- **Título:** «Estado de NetProtect».
- **Subtítulo:** «Aquí puedes ver qué funciones de NetProtect están activas en este dispositivo.».
- **Arriba del todo**, antes de `BrandHeader`: `NpTopBar(onBack = onBack)` en lugar de `BrandHeader` (esta pantalla sí
  tiene volver).

**Píldora de estado** (función `private` en el archivo), según el `ServiceState`:
- `Active` → `StatusPill("Activo", NpTone.Success)`;
- `Inactive` → `StatusPill("Inactivo", NpTone.Neutral)`;
- `Unconfirmed` → `StatusPill("Sin confirmar", NpTone.Warning)`.

**Relativo:** `RelativeTime.format(instant, now, ZoneId.systemDefault())`.

Contenido, en orden. Usa una `NpCard` por bloque, con 12 dp entre ellas. En cada tarjeta, la fila superior lleva
`IconTile` de 48 dp, 12 dp, una columna (peso 1) con título y descripción, y la píldora a la derecha.

1. **«Reporte del dispositivo»** (`NpIcons.Wifi`, `Success`), con la píldora de `view.report`. Descripción: **«Envía
   el estado de este dispositivo a tu tutor.»**. Debajo, 12 dp y una caja (`NpCard(containerColor = NpColors.BlueWash,
   borderColor = NpColors.BlueWash)`) con:
   - el icono `NpIcons.Clock` de 22 dp;
   - **«Último reporte: {relativo de view.lastReport}»**, o **«Último reporte: todavía no»** si es `null` (`BodyStrong`);
   - **«El dispositivo envía su estado cada minuto mientras la app esté abierta.»** (`Body`, `SlateMuted`).
2. **«Control de apps»** (`NpIcons.LayoutGrid`, `Purple`), con la píldora de `view.appControl`. Descripción: **«Aplica
   las reglas de apps de tu tutor.»**.
3. **«Ubicación aproximada»** (`NpIcons.MapPin`, `Info`), con la píldora de `view.location`. Descripción: **«Comparte la
   ubicación aproximada de este dispositivo.»**.
4. **Solo si `view.screenShareActive`:** **«Visualización de pantalla»** (`NpIcons.Monitor`, `Info`) con
   `StatusPill("En curso", NpTone.Info)`. Descripción: **«Tu tutor está viendo la pantalla con tu consentimiento.»**.
   Debajo, 12 dp y una caja como la del punto 1 con:
   - el icono `NpIcons.Eye`;
   - **«Empezó {relativo de view.screenShareSince}.»** (o nada si es `null`);
   - **«Se detiene cuando tu tutor la finaliza o cuando tú la detienes.»**;
   - 12 dp y `NpButton("Detener", onStopScreenShare, Modifier.fillMaxWidth(), variant = Secondary, icon = NpIcons.EyeOff)`.
5. **«Notificaciones permanentes»** (`NpIcons.Bell`, `Info`), sin píldora. Descripción: **«Mientras una función está
   activa, Android muestra una notificación para que sepas qué está haciendo NetProtect:»**. Debajo, una lista de
   tres líneas (`Body`, `Ink`, con 4 dp entre ellas). Son los títulos reales de las notificaciones de la app:
   - **«“NetProtect está activo” — control de apps.»**
   - **«“NetProtect comparte la ubicación” — ubicación.»**
   - **«“Tu tutor está viendo esta pantalla” — vista remota.»**
6. 16 dp. `InfoBanner(title = "Información importante", text = "NetProtect no graba la pantalla ni controla el
   dispositivo. La visualización de pantalla requiere tu consentimiento cada vez.")`.
7. 16 dp. `NpButton("Volver al inicio", onBack, Modifier.fillMaxWidth(), icon = NpIcons.Home)`.

### 5.3 `ActiveShareBanner`

Una `Row` de ancho completo con:
- `background(NpColors.SignalBlue)`, `statusBarsPadding()` y relleno de 16 × 10 dp;
- el icono `NpIcons.Eye` de 20 dp, con tinte `NpColors.PaperWhite`;
- 12 dp;
- **«Tu tutor está viendo esta pantalla»** (`BodyStrong`, `PaperWhite`, peso 1);
- `NpButton("Detener", onStop, variant = NpButtonVariant.Secondary, small = true)`.

## 6. Nada que retirar

Lo antiguo ya lo quitó Claude.

## 7. Galería (solo debug)

En `ScreensGallery.kt`, pon estos frames **al principio** de la lista, **sin ningún diálogo**. Usa
`now = Instant.parse("2026-10-01T15:00:00Z")`. Los `ServicesView` se construyen a mano.
- `ScreenShareConsentScreen`.
- `ServicesStatusScreen`:
  - todo activo, con el último reporte 1 min antes;
  - control de apps «Sin confirmar» y ubicación «Inactivo»;
  - con vista remota en curso desde hace 30 s;
  - con el reporte `Unconfirmed` y `lastReport = null`.
- `ActiveShareBanner` sola (dentro de un `Box` de 120 dp).

Guarda las capturas fuera del repo, en `%TEMP%\sprint50-gallery\`, y di en el informe qué frame sale en cada archivo.

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

- **`ScreenShareConsentScreenTest`:**
  - «Continuar» empieza **deshabilitado** (`assertIsNotEnabled()`);
  - al tocar el **texto** de la casilla, «Continuar» se habilita y llama a `onContinue`;
  - «Ahora no» llama a `onDecline` sin marcar nada;
  - no existe ningún texto que contenga «ajustes», «revocar», «redirigir» ni «desde la app NetProtect»;
  - se ve «No se graba ni se controla el dispositivo.».
- **`ServicesStatusScreenTest`:**
  - `Active`, `Inactive` y `Unconfirmed` muestran «Activo», «Inactivo» y «Sin confirmar»;
  - con `screenShareActive = false` no existe «Visualización de pantalla» ni «Detener»; con `true` sí, y «Detener»
    llama a `onStopScreenShare`;
  - con `lastReport = null` se ve «Último reporte: todavía no»;
  - se ven los tres títulos reales de notificación;
  - no existe «Cerrar sesión»;
  - «Volver al inicio» y la flecha llaman a `onBack`.
- **`ActiveShareBannerTest`:** se ve «Tu tutor está viendo esta pantalla» y «Detener» llama a `onStop`.

El emulador `Pixel_8` está encendido con la sesión supervisada real: **no cierres sesión, no cambies de modo y no
toques la app instalada**.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, los mockups 15 y 17 y `docs/android-redesign/mockups/README.md` (secciones 15 y 17).
   Lee también `ServiceStatusRegistry.kt`, los tres archivos de §2, `PermissionsScreen.kt` (modelo de pantalla
   supervisada) y `docs/delegated/done/sprint-49-app-bloqueada.md` como ejemplo del resultado esperado. Mueve el
   encargo a `active/`.
2. Cada archivo, y compila con `cd mobile && ./gradlew compileDebugKotlin` después de cada uno.
3. Galería, y compila con `./gradlew assembleDebug`.
4. Tests: `./gradlew assembleDebugAndroidTest connectedDebugAndroidTest`.
5. Verificación (§10) e informe.

Si Gradle falla con `resource … not found` sin razón aparente, ejecuta `./gradlew clean` una vez.

## 10. Verificación

Pega la salida **real** de cada comprobación:
- **Build y lint:** `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` →
  `BUILD SUCCESSFUL`, lint con 0 errores. Indica los avisos nuevos con ruta y línea (la base son 24).
- **Tests del emulador:** `connectedDebugAndroidTest`, con el número de tests y el resultado.
- **Patrones prohibidos:**
  `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalContext|Intent|JSONObject|\.send\(|ajustes|revoc|redirig|Cerrar sesión" mobile/app/src/main/java/com/netprotect/app/feature/supervised/{consent,services} mobile/app/src/main/java/com/netprotect/app/ui/components/ActiveShareBanner.kt`
  → sin coincidencias.
- **Shell intacto:** `git diff --stat -- mobile/app/src/main/java/com/netprotect/app/feature/supervised/SupervisedShell.kt mobile/app/src/main/java/com/netprotect/app/core`
  → vacío.
- **Archivos tocados:** `git status --short` → solo archivos de §4.

## 11. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos creados y modificados.
2. Salidas reales de §10 y de los tests.
3. Decisiones tuyas fuera del encargo, y por qué.
4. Pendientes.
5. `[PREGUNTA PARA CLAUDE]`, si las hay.

Si una instrucción contradice a otra o al código, **dilo** en lugar de elegir en silencio. Lo que no hayas ejecutado
se declara PENDIENTE.

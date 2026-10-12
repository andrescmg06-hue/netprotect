# Sprint 43 — Pantallas de Login, Elegir modo y Carga (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Las pantallas de carga, inicio de sesión y elección de modo se ven como los mockups
`docs/android-redesign/mockups/01-login.jpg` y `02-elegir-modo.jpg`, hechas con el tema y los componentes
del Sprint 42, y **se comportan exactamente igual que hoy**. Solo escribes **presentación**: no hay
lógica de sesión, red ni estados en lo que te toca.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- `HomeScreen.kt` **ya llama** a tus tres pantallas y ya pasa los datos; ya no tiene las pantallas viejas.
  **No lo abras para editarlo.** Sigue mandando la máquina de estados, `signIn()`, `selectRole()`, la
  comprobación de servicio y el contador de "Reintentar".
- Existen los **archivos vacíos** `{pkg}/feature/home/{LoadingScreen,LoginScreen,RoleSelectionScreen}.kt` con la
  firma final y un cuerpo `Box(modifier.fillMaxSize())`. **Tu trabajo es rellenar esos cuerpos.** **No cambies
  ninguna firma** (nombre, parámetros, orden ni tipos): HomeScreen depende de ellas.
- `{pkg}/feature/home/ServiceStatus.kt`: `enum class ServiceStatus { Checking, Ready, Unavailable }`.
- Componentes extendidos para estas pantallas: `NpButton` ahora acepta `tintIcon: Boolean = true` (con `false`
  el icono conserva sus colores, para la "G" de Google) y `trailingIcon: Int?` (flecha al final; con él el
  botón debe llevar `Modifier.fillMaxWidth()` y el texto queda en el centro). `BrandHeader` ahora acepta
  `stacked: Boolean = false` (escudo grande arriba y marca centrada debajo).

Firmas que debes respetar tal cual:

```kotlin
@Composable fun LoadingScreen(modifier: Modifier = Modifier)

@Composable fun LoginScreen(
    error: String?, service: ServiceStatus,
    onSignIn: () -> Unit, onRetryService: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable fun RoleSelectionScreen(
    displayName: String?, email: String, error: String?,
    onSelectTutor: () -> Unit, onSelectSupervised: () -> Unit, onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
)
```

## 3. Decisiones ya tomadas (no las reabras)

| # | Decisión |
|---|---|
| — | Solo presentación. Las pantallas reciben datos y lambdas; **no** llaman a red, no leen preferencias, no guardan estado de sesión. |
| — | Botón "Reintentar" solo llama a `onRetryService()` **una vez por toque**; nada de bucles ni temporizadores. |
| — | "Servicio listo" es el único detalle de infraestructura que se muestra. **Nunca** menciones base de datos, Redis, HTTP ni mensajes técnicos. |
| — | El botón de iniciar sesión sigue habilitado aunque el servicio no esté disponible (comportamiento actual). |
| — | Cancelar el selector de cuentas de Google no es un error (ya lo maneja HomeScreen; tú solo muestras `error` si no es null). |
| — | Sin subtítulo "PANEL DEL TUTOR" en la marca de estas pantallas (aún no hay modo elegido y aquí entra también el supervisado), aunque el mockup 1 lo dibuje. |
| — | Fondo: degradado vertical de `SkyGround` a `SkyGroundEnd`. **No** dibujes las ondas decorativas del mockup. |
| — | Tema claro únicamente; las barras del sistema ya están resueltas en `MainActivity`. |
| — | **Cero** `Color(0x…)`, radios, tamaños de texto ni fuentes literales: `NpColors`, `NpText`, `NpShapes`, `NpIcons`, `NpTone`. Los `dp` de espaciado sí, en múltiplos de 4. |

## 4. Lo que NO debes tocar

`backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`,
`mobile/app/build.gradle.kts`, `mobile/gradle/libs.versions.toml`, `AndroidManifest.xml` principal,
`{pkg}/feature/home/HomeScreen.kt`, `{pkg}/feature/home/ServiceStatus.kt`, `{pkg}/feature/tutor/**`,
`{pkg}/feature/supervised/**`, `{pkg}/core/**`, `{pkg}/ui/theme/**`, `{pkg}/ui/components/**`,
`{pkg}/ui/format/**`, `{pkg}/ui/icons/**`, `{pkg}/ui/navigation/**`, `res/`, `MainActivity.kt`.
Si un componente no hace lo que necesitas, **no lo modifiques**: usa el más parecido y escribe
`[PREGUNTA PARA CLAUDE]` en el informe. Sin `git commit`/`push`.

## 5. Archivos permitidos (lista cerrada)

Modificar (rellenar cuerpos): `{pkg}/feature/home/LoadingScreen.kt`, `LoginScreen.kt`, `RoleSelectionScreen.kt`.
Crear:
- `mobile/app/src/androidTest/java/com/netprotect/app/ui/{LoginScreenTest,RoleSelectionScreenTest}.kt`
- `mobile/app/src/debug/java/com/netprotect/app/debug/{ScreensGallery,ScreensGalleryActivity}.kt`
- editar `mobile/app/src/debug/AndroidManifest.xml` **solo** para añadir `ScreensGalleryActivity`
  (mismo formato que `DesignGalleryActivity`: `exported="true"`, sin `intent-filter`, sin permisos).
Todo lo demás está prohibido.

## 6. Punto de partida (léelo antes de escribir)

`docs/sprint-42.md` (§"Lo que corrigió Claude"), `docs/android-redesign/mockups/README.md` §1–2 (correcciones de
verdad de estos mockups), los dos mockups, y el código de `NpButton`, `BrandHeader`, `NpCard`, `IconTile`,
`LoadingState`, `NpText`, `NpColors`, `NpIcons` y `DesignGallery.kt` (para el patrón de la galería).
No puedes ver imágenes: **la revisión visual la hace Claude**. Tú describes por escrito lo que dibujaste.

## 7. Pantallas — especificación

Todas: `Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NpColors.SkyGround, NpColors.SkyGroundEnd))))`
y, dentro, contenido con `systemBarsPadding()` (o `statusBarsPadding()` + `navigationBarsPadding()`), relleno
horizontal de 24 dp y `verticalScroll(rememberScrollState())` para que quepa en pantallas bajas o con letra grande.

### 7.1 `LoadingScreen`
Centrada en pantalla: `BrandHeader(stacked = true)` y debajo, con 24 dp de separación, `LoadingState()`
(texto "Cargando…", ya viene en el componente).

### 7.2 `LoginScreen`
De arriba abajo, centrado horizontalmente, todo el bloque centrado verticalmente:
1. `BrandHeader()` (horizontal, **sin** subtítulo).
2. 32 dp. Título **"Control parental"**: `NpText.Display`, `NpColors.ShieldNavy`, `TextAlign.Center`.
3. 12 dp. Texto **"Inicia sesión con tu cuenta de Google para continuar como tutor o como dispositivo supervisado."**:
   `NpText.BodyLead`, `NpColors.SlateMuted`, centrado.
4. 24 dp. `NpCard` a todo el ancho con, dentro y en este orden:
   a. **Si `error != null`**: el texto del error en `NpText.Body`, `NpColors.DangerText`, con
      `semantics { liveRegion = LiveRegionMode.Assertive }`, y 12 dp de separación debajo.
   b. `NpButton(text = "Iniciar sesión con Google", onClick = onSignIn, modifier = Modifier.fillMaxWidth(),
      variant = NpButtonVariant.Secondary, icon = NpIcons.GoogleG, tintIcon = false, trailingIcon = NpIcons.ArrowRight)`.
   c. 16 dp, una línea divisoria de 1 dp `NpColors.Hairline` (`Box` con fondo, no `Divider` de Material), 16 dp.
   d. La **línea de estado del servicio**, centrada, con `semantics { liveRegion = LiveRegionMode.Polite }`:
      - `Checking`: círculo de 8 dp `NpColors.SlateSubtle` + texto **"Comprobando servicio…"** (`NpText.Body`, `SlateMuted`).
      - `Ready`: círculo de 8 dp `NpColors.Success` + **"Servicio listo"** (`SlateMuted`).
      - `Unavailable`: círculo de 8 dp `NpColors.Danger` + **"Servicio no disponible"** (`SlateMuted`), y
        debajo `NpButton(text = "Reintentar", onClick = onRetryService, variant = NpButtonVariant.Text, icon = NpIcons.RefreshCw, small = true)`.
5. Al pie de la pantalla (fuera del bloque centrado, alineado abajo al centro con 24 dp de margen inferior): icono
   `NpIcons.Info` de 16 dp (`SlateMuted`) + **"Solo puedes iniciar sesión con Google."** (`NpText.Body`, `SlateMuted`).
   Sugerencia: una `Box` raíz con un `Column` centrado y este pie con `Modifier.align(Alignment.BottomCenter)`.

### 7.3 `RoleSelectionScreen`
De arriba abajo, centrado:
1. `BrandHeader(stacked = true)` (sin subtítulo).
2. 24 dp. **"Hola, {nombre}"**: `NpText.Display`, `ShieldNavy`, centrado. `{nombre}` es la **primera palabra** de
   `displayName` (recortando espacios); si `displayName` es null o está en blanco, se usa `email` completo.
3. 8 dp. **"¿Cómo vas a usar este dispositivo?"**: `NpText.Title`, `NpColors.SlateMuted`, centrado.
4. 8 dp. **"Selecciona el modo que mejor describa cómo vas a utilizar esta app."**: `NpText.Body`, `SlateMuted`, centrado.
5. 24 dp. Dos `NpCard`, 16 dp entre ambas. Cada una: una fila con `IconTile` de 56 dp a la izquierda y, a su derecha
   (12 dp), una columna con el título y la descripción; debajo (16 dp) un `NpButton` a todo el ancho:
   - Tarjeta 1: `IconTile(NpIcons.Users, NpTone.Info, size = 56.dp)`; título **"Soy tutor"**;
     descripción **"Superviso otros dispositivos desde este teléfono o tablet."**; botón **"Elegir"**,
     `Primary`, `trailingIcon = NpIcons.ArrowRight`, `onClick = onSelectTutor`.
   - Tarjeta 2: `IconTile(NpIcons.Smartphone, NpTone.Purple, size = 56.dp)`; título **"Este es el dispositivo supervisado"**;
     descripción **"Este teléfono es el que un tutor va a supervisar."**; botón **"Elegir"**, `Secondary`,
     `trailingIcon = NpIcons.ArrowRight`, `onClick = onSelectSupervised`.
   Títulos en `NpText.Title` (`ShieldNavy`), descripciones en `NpText.Body` (`SlateMuted`).
6. **Si `error != null`**: 16 dp y el texto del error como en 7.2.a (`DangerText`, `liveRegion = Assertive`), centrado.
7. 24 dp. `NpButton(text = "Cerrar sesión", onClick = onSignOut, variant = NpButtonVariant.Text, icon = NpIcons.LogOut)`,
   centrado.

## 8. Galería de pantallas (solo debug) — para que Claude las revise

`ScreensGallery()` (`@Composable`, pública) es una columna desplazable con **cada pantalla en cada estado** dentro de
un marco de 760 dp de alto y 360 dp de ancho, con un título de texto encima de cada una:
`LoadingScreen`; `LoginScreen` × { Checking, Ready, Unavailable, Ready + error ("No se pudo iniciar sesión") };
`RoleSelectionScreen` × { con nombre ("Andrés Mosquera"), sin nombre (`displayName = null`, `email = "tutor@example.com"`),
con error ("No se pudo guardar el modo") }. Lambdas vacías. `ScreensGalleryActivity` es igual que `DesignGalleryActivity`.
Se abre con `adb shell am start -n com.netprotect.app/com.netprotect.app.debug.ScreensGalleryActivity`.

## 9. Tests (androidTest, `createComposeRule()`)

- `LoginScreenTest`: para cada `ServiceStatus` muestra su texto ("Comprobando servicio…", "Servicio listo",
  "Servicio no disponible"); solo `Unavailable` muestra "Reintentar" y al tocarlo llama **una vez** a `onRetryService`;
  el botón "Iniciar sesión con Google" llama a `onSignIn`; con `error = "No se pudo iniciar sesión"` el texto existe y
  sin error no existe; **ningún** texto contiene "Redis", "base de datos" ni "HTTP".
- `RoleSelectionScreenTest`: "Hola, Andrés" con `displayName = "Andrés Mosquera"`; con `displayName = null` muestra
  el correo; con `displayName = "  "` también el correo; los dos botones "Elegir" llaman a su lambda correcta;
  "Cerrar sesión" llama a `onSignOut`; el error se muestra solo si no es null.
Necesitan dispositivo: el emulador `Pixel_8` está encendido (`adb devices`, con la ruta completa de `AGENTS.md`).

## 10. Pasos (con comprobación)

1. Lee `AGENTS.md`, este encargo, los mockups y el código de la sección 6; mueve el encargo a `active/`.
2. `LoadingScreen` → `cd mobile && ./gradlew compileDebugKotlin`. Luego `LoginScreen` y `RoleSelectionScreen`, compilando
   tras cada uno.
3. La galería de pantallas y su actividad (§8); `./gradlew assembleDebug`.
4. Tests (§9); `./gradlew assembleDebugAndroidTest`, y con el emulador conectado `./gradlew connectedDebugAndroidTest`.
5. Verificación (§11) e informe.
Si Gradle falla con `resource … not found` sin razón aparente, prueba `./gradlew clean` una vez.

## 11. Verificación

Pega la salida **real** (no la inventes):
- `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → `BUILD SUCCESSFUL`;
  lint: 0 errores y avisos nuevos con ruta:línea (base: 25 avisos).
- `./gradlew connectedDebugAndroidTest` en el emulador: número de tests y resultado.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(" mobile/app/src/main/java/com/netprotect/app/feature/home mobile/app/src/debug` →
  solo `@Preview` (`backgroundColor = 0x…`), nada más.
- `git status --short` y `git diff --stat`: solo archivos de §5. **`HomeScreen.kt` no debe aparecer.**
- Instala el APK debug, abre `ScreensGalleryActivity` y guarda capturas con `adb exec-out screencap -p` **fuera del repo**
  (carpeta temporal del sistema); dilo en el informe. No las juzgues: no puedes verlas.

## 12. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos modificados/creados. 2. Salidas reales de §11 y de los tests. 3. Lo que decidiste tú fuera de este encargo
(y por qué). 4. Pendientes, sin adornos. 5. Preguntas `[PREGUNTA PARA CLAUDE]`.
Errores que se cometieron en el S42 y que debes evitar: escribir una regla contradictoria sin avisar (si una instrucción
de este encargo se contradice, dilo en el informe en vez de elegir en silencio), y declarar como verificado algo que no
ejecutaste (los tests conectados se ejecutan o se declaran PENDIENTES).

---

## Informe de DeepSeek

### 1. Archivos modificados / creados

**Modificados (rellenados los cuerpos, firmas intactas)**:
`{pkg}/feature/home/LoadingScreen.kt`, `LoginScreen.kt`, `RoleSelectionScreen.kt`.

**Creados**:
`mobile/app/src/androidTest/java/com/netprotect/app/ui/LoginScreenTest.kt`,
`mobile/app/src/androidTest/java/com/netprotect/app/ui/RoleSelectionScreenTest.kt`,
`mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt`,
`mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGalleryActivity.kt`.

**Modificado** (solo se añadió la actividad): `mobile/app/src/debug/AndroidManifest.xml`
(`ScreensGalleryActivity`, mismo formato que `DesignGalleryActivity`).

`HomeScreen.kt` **no** aparece en `git status` (no se tocó).

### 2. Salidas reales (§11 y tests)

- `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL** (47 s, 116 tasks).
- `lintDebug`: **0 errores, 25 avisos** (base 25); **ninguno** en `feature/home` ni `src/debug`.
- `./gradlew connectedDebugAndroidTest` en el emulador `Pixel_8` (encendido): **32 tests, 0 fallos, 0 saltados**.
  De ellos, 16 son míos y pasaron:
  - `LoginScreenTest` (8): `checkingShowsItsLabelAndNoRetry`, `readyShowsItsLabel`, `unavailableShowsItsLabelAndRetry`, `retryCallsOnRetryServiceExactlyOnce`, `signInCallsOnSignIn`, `errorIsShownWhenPresent`, `errorIsAbsentWhenNull`, `noInfrastructureJargonIsShown`.
  - `RoleSelectionScreenTest` (8): `greetsWithFirstName`, `greetsWithEmailWhenNameIsNull`, `greetsWithEmailWhenNameIsBlank`, `tutorChoiceCallsOnSelectTutor`, `supervisedChoiceCallsOnSelectSupervised`, `signOutCallsOnSignOut`, `errorShowsWhenPresent`, `errorAbsentWhenNull`.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(" .../feature/home .../src/debug` → **solo** `HomeScreen.kt:171` (`Color(0xFF090B10)`), que es **preexistente de Claude** (la superficie oscura de Tutor/Supervised aún no rediseñadas). Mis archivos (3 pantallas + 2 galería) no tienen ninguna coincidencia.
- Galería abierta en el emulador (`adb shell am start -n com.netprotect.app/com.netprotect.app.debug.ScreensGalleryActivity`) y verificada con `uiautomator dump`: se ven "LoadingScreen" (logo "Net"/"Protect" + "Cargando…") y el título "LoginScreen · Checking". Capturas guardadas fuera del repo en `%TEMP%\sprint43-gallery\`.

### 3. Lo que decidí fuera del encargo

- **Ancho del marco de la galería**: el encargo pide "360 dp de ancho"; usé `Modifier.fillMaxWidth()` (≈328 dp tras el padding de 16 dp de la galería) porque un marco literal de 360 dp se desbordaría del ancho disponible en un teléfono de 360 dp. La altura sí es `760.dp` literal.
- **Pie del `LoginScreen`**: va como hijo del `Box` raíz con `align(Alignment.BottomCenter)` + `navigationBarsPadding()` + `padding(bottom = 24.dp)`, fuera del bloque centrado (la sugerencia del encargo).
- **Nombre del saludo**: `displayName?.trim().orEmpty().substringBefore(' ').ifBlank { email }` — cumple "primera palabra" y "null/en blanco → correo" (los 3 casos están en los tests).

### 4. Pendientes

- **No pude revisar las capturas visualmente** (este modelo no acepta imágenes). Las capturas quedan en `%TEMP%\sprint43-gallery\` para Claude.
- **Nota sobre la galería**: el `verticalScroll` exterior de `ScreensGallery` no respondió a `adb shell input swipe`/`input roll` (el gesto cae sobre el `verticalScroll` interior de cada pantalla). Con `uiautomator` confirmé que la galería **sí renderiza** y que el `ScrollView` exterior es `scrollable=true`; el deslizamiento real con el dedo debería funcionar por el *nested scroll* estándar de Compose, pero no lo pude confirmar con gesto sintético. Lo dejo anotado por si Claude quiere verificarlo al tocar la galería.

### 5. Preguntas para Claude

Ninguna.

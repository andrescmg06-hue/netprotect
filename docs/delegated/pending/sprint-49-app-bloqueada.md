# Sprint 49 — Supervisado: pantalla «App bloqueada» (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

La pantalla que ve el menor cuando una app queda bloqueada se ve como el mockup
`docs/android-redesign/mockups/16-supervisado-app-bloqueada.jpg`, en sus **7 variantes**, con los textos verdaderos
de §3. **Solo escribes presentación:** qué se bloquea, cuándo y por qué lo decide el motor de reglas, que no te
corresponde. Los textos, iconos y colores de cada motivo ya están decididos en `BlockPresentation`.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- **`{pkg}/feature/supervised/block/BlockPresentation.kt`** (con tests): dado un `BlockReason` y la categoría real,
  devuelve un `BlockPresentation`:
  - `badgeIcon`: el icono del distintivo que va sobre el candado;
  - `tone`: el color (`NpTone`) del distintivo y de la tarjeta de pista;
  - `title`: «APP BLOQUEADA»;
  - `message`: la frase bajo el título;
  - `hintIcon` y `hint`: el icono y el texto de la tarjeta de color.

  Está en `blockPresentation(reason, categoryLabel)`. También `BLOCK_COVER_NOTE`, la letra pequeña.
- **El overlay y la actividad** ya envuelven la pantalla en `NetProtectTheme` y le pasan la categoría real.
- **`{pkg}/feature/supervised/block/BlockScreenContent.kt`** existe con **firma final** y un cuerpo provisional.
  **Reemplaza el cuerpo; no cambies la firma:**

```kotlin
@Composable
fun BlockScreenContent(
    packageName: String,       // para el icono real de la app
    appLabel: String,          // nombre de la app, ya resuelto
    categoryLabel: String?,    // categoría real en español, o null si la app no tiene
    reason: BlockReason,
    onGoHome: () -> Unit,
)
```

## 3. Lo que NO se dibuja aunque el mockup lo muestre (obligatorio)

| Mockup 16 | Qué muestra | Qué hacer |
|---|---|---|
| 4, 6 | Horas concretas («desde las 10:00 p. m. hasta las 6:00 a. m.», «7:00 a. m. – 2:00 p. m.») | **Ninguna hora.** No llegan a esta pantalla. El texto es siempre `presentation.hint`. |
| 2, 3 | «Podrás volver a usarla mañana / el próximo lunes» | Ya está dentro de `hint` solo en los dos límites. No lo escribas tú. |
| 1–7 | Categorías inventadas («Entretenimiento», «Navegador», «Comunicación» para Discord…) | **Solo `categoryLabel`.** Si es `null`, no se dibuja la línea de categoría. |
| 1–7 | Textos propios de cada variante («Has alcanzado el límite diario…») | **Los de `presentation.message` y `presentation.hint`, tal cual.** No escribas ningún texto de motivo. |
| 5 | «La categoría Entretenimiento está bloqueada…» | Es `presentation.hint`, que ya usa la categoría real. |
| todos | Botones de «pedir más tiempo», «desbloquear», «avisar al tutor» | **Prohibido**: esa función no existe. El único botón es «Ir al inicio». |
| todos | Logotipo grande de NetProtect | Marca pequeña: `BrandHeader()`. |

## 4. Lo que NO debes tocar y archivos permitidos

No toques:
- `{pkg}/core/**` (incluido `core/rules`), `{pkg}/ui/**`;
- `BlockPresentation.kt`, `BlockScreenActivity.kt`, `SupervisedShell.kt`;
- `res/`, Gradle, manifiestos, `backend/`, `frontend/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`.

No hagas `git commit` ni `push`. Si necesitas algo que esté fuera de esta lista, escribe `[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar el cuerpo de `{pkg}/feature/supervised/block/BlockScreenContent.kt`. Puedes añadir composables `private`
  en ese archivo.
- Crear `mobile/app/src/androidTest/java/com/netprotect/app/ui/BlockScreenContentTest.kt`.
- Modificar `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantalla

**Restricciones técnicas.** Esta pantalla se dibuja en **dos sitios**: como actividad y como ventana superpuesta del
sistema, sin actividad detrás. Por eso:
- **Prohibido:** `LocalContext.current as Activity`, `Activity`, `Window`, `WindowInsets` y `statusBarsPadding()`.
  Usa un relleno fijo arriba de 48 dp.
- Todo debe quedar visible en una pantalla pequeña: `verticalScroll(rememberScrollState())`.
- Sin animaciones infinitas ni navegación.

**Estructura:** `Box` con `fillMaxSize()` y fondo `NpColors.SkyGround`; dentro, una `Column` centrada
horizontalmente, con relleno horizontal de 24 dp, 48 dp arriba y 32 dp abajo.

Contenido, en orden. Calcula `val presentation = blockPresentation(reason, categoryLabel)` una sola vez.
1. `BrandHeader()`.
2. 24 dp.
3. **Ilustración del candado** (`Box` de 120 × 96 dp, centrado):
   - un círculo de fondo (`presentation.tone.wash`, 96 dp);
   - encima, el icono `NpIcons.Lock` de 56 dp, con tinte `NpColors.ShieldNavy`;
   - abajo a la derecha, el **distintivo**: un círculo de 36 dp con `background(NpColors.PaperWhite, CircleShape)`, un
     borde de 2 dp `presentation.tone.wash` y dentro `presentation.badgeIcon` de 20 dp con tinte
     `presentation.tone.text`.
   - La ilustración es decorativa: `contentDescription = null`.
4. 16 dp.
5. `presentation.title` («APP BLOQUEADA»): `NpText.Display`, `ShieldNavy`, centrado.
6. 8 dp.
7. `presentation.message`: `NpText.Body`, `SlateMuted`, centrado.
8. 20 dp.
9. **Tarjeta de la app** (`NpCard`, ancho completo): una fila con:
   - `AppIcon(packageName, appLabel, size = 48.dp)`;
   - 12 dp;
   - una columna con `appLabel` (`Title`, `ShieldNavy`, una línea con elipsis) y, **solo si
     `categoryLabel != null`**, `categoryLabel` (`Body`, `SlateMuted`).
10. 12 dp.
11. **Tarjeta de pista** (`NpCard(containerColor = presentation.tone.wash, borderColor = presentation.tone.wash)`):
    una fila con `Icon(presentation.hintIcon, 24 dp, tint = presentation.tone.text)`, 12 dp y `presentation.hint`
    (`Body`, `Ink`, peso 1).
12. 20 dp.
13. `NpButton("Ir al inicio", onGoHome, Modifier.fillMaxWidth(), icon = NpIcons.Home)`.
14. 16 dp.
15. `BLOCK_COVER_NOTE`: `NpText.Caption.copy(fontWeight = FontWeight.Normal)`, `SlateMuted`, centrado.

**Prohibido:**
- `Color(0x…)`, tamaños de letra o radios fijos;
- `Build.`, `Intent`, `PackageManager` (el icono ya lo resuelve `AppIcon`);
- texto de motivo escrito por ti.

## 6. Nada que retirar

El contenido antiguo (fondo oscuro con colores literales) lo reemplazas tú; no queda nada más.

## 7. Galería (solo debug)

En `ScreensGallery.kt`, pon estos frames **al principio** de la lista, **sin ningún diálogo**. Para ver iconos reales
usa `com.android.chrome` y `com.google.android.youtube`; para ver el monograma, un paquete que no exista
(`com.ejemplo.tiktok` con la etiqueta «TikTok»).

Un frame por cada uno de los 7 motivos:

| Frame | `reason` | App | `categoryLabel` |
|---|---|---|---|
| 1 | `BLOCK` | Instagram (sin icono local) | «Redes sociales» |
| 2 | `DAILY_LIMIT` | YouTube | «Streaming» |
| 3 | `WEEKLY_LIMIT` | TikTok (sin icono local) | «Redes sociales» |
| 4 | `SCHEDULE` | Discord (sin icono local) | «Comunicación» |
| 5 | `CATEGORY` | Netflix (sin icono local) | «Streaming» |
| 6 | `SCHOOL_MODE` | Chrome | `null` |
| 7 | `DEFAULT_POLICY` | Chrome | `null` |

Más dos frames: `CATEGORY` con `categoryLabel = null`, y una etiqueta de app larga («Una aplicación con un nombre
muy largo para probar el recorte») con `DAILY_LIMIT`.

Guarda las capturas fuera del repo, en `%TEMP%\sprint49-gallery\`, y di en el informe qué frame sale en cada archivo.

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

`BlockScreenContentTest`:
- para **cada** valor de `BlockReason.entries`: se ve «APP BLOQUEADA», el nombre de la app, `message` y `hint`, y
  «Ir al inicio» llama a `onGoHome`;
- con `categoryLabel = "Streaming"` se ve «Streaming»; con `null` no existe ningún nodo con el texto `"null"`;
- el límite diario muestra «mañana» y el semanal «lunes»; ningún otro motivo contiene «mañana» ni «lunes» ni una hora
  con el formato `\d{1,2}:\d{2}`;
- no existe ningún texto «Desbloquear», «Pedir», «Solicitar» ni «más tiempo»;
- se ve `BLOCK_COVER_NOTE`.

El emulador `Pixel_8` está encendido y tiene la sesión abierta en modo supervisado con una cuenta real: **no cierres
sesión, no cambies de modo y no toques la app instalada**.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, el mockup 16 y `docs/android-redesign/mockups/README.md` (sección 16). Lee también
   `BlockPresentation.kt`, el `BlockScreenContent.kt` actual, `AppIcon.kt`, `NpCard.kt`, `BrandHeader.kt` y
   `docs/delegated/done/sprint-48-supervisado.md` como ejemplo del resultado esperado. Mueve el encargo a `active/`.
2. La pantalla, y compila con `cd mobile && ./gradlew compileDebugKotlin`.
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
  `grep -nE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Build\.|Intent|PackageManager|Activity|Window|statusBarsPadding|Desbloquear|Pedir|Solicitar" mobile/app/src/main/java/com/netprotect/app/feature/supervised/block/BlockScreenContent.kt`
  → sin coincidencias.
- **Archivos tocados:** `git status --short` y `git diff --stat` → solo archivos de §4.

## 11. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos creados y modificados.
2. Salidas reales de §10 y de los tests.
3. Decisiones tuyas fuera del encargo, y por qué.
4. Pendientes.
5. `[PREGUNTA PARA CLAUDE]`, si las hay.

Si una instrucción contradice a otra o al código, **dilo** en lugar de elegir en silencio. Lo que no hayas ejecutado
se declara PENDIENTE.

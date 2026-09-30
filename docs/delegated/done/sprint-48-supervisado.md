# Sprint 48 — Supervisado: Vincular, Dispositivo vinculado y Permisos (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Las tres pantallas del modo supervisado se ven como los mockups `docs/android-redesign/mockups/12-supervisado-vincular.jpg`,
`13-supervisado-vinculado.jpg` y `14-supervisado-permisos.jpg`, con los textos **corregidos** de §3.

**Solo escribes presentación.** Este es el sprint más delicado del rediseño: los servicios que mantienen protegido el
teléfono del menor (latido, bloqueo de apps, ubicación, sincronización, vista remota) viven en
`{pkg}/feature/supervised/SupervisedShell.kt`. **No lo abras ni lo modifiques por ningún motivo.** Tus pantallas no
arrancan, detienen ni piden nada: solo muestran valores y llaman a los callbacks que reciben.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- **`SupervisedShell.kt`:** los servicios, los permisos, la navegación y el diálogo de captura de Android.
  **PROHIBIDO tocarlo.**
- **`{pkg}/feature/supervised/SupervisedViews.kt`:**
  - `PermissionsUi` (4 booleanos más `pending`);
  - `pendingPermissionsLabel(n)`: «2 permisos pendientes», «1 permiso pendiente» o «Todos los permisos están
    configurados»;
  - `isCompletePairingCode`.
- **`PermissionCard`:** ya dice «Configurado» / «Pendiente» y oculta los botones cuando el permiso está concedido.
- **Las tres pantallas** existen con **firma final** y un cuerpo provisional. **Reemplaza los cuerpos; no cambies
  ninguna firma:**

```kotlin
// {pkg}/feature/supervised/link/LinkDeviceScreen.kt
fun LinkDeviceScreen(code: String, onCodeChange: (String) -> Unit, linking: Boolean, error: String?, rechecking: Boolean,
                     onLink: () -> Unit, onCheckLink: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier)
// {pkg}/feature/supervised/linked/LinkedDeviceScreen.kt
fun LinkedDeviceScreen(deviceName: String?, androidVersion: String, tutors: List<String>, lastContact: Instant?,
                       reachable: Boolean, now: Instant, pendingPermissions: Int, screenShareRequested: Boolean,
                       onAcceptScreenShare: () -> Unit, onDeclineScreenShare: () -> Unit, onOpenPermissions: () -> Unit,
                       onSwitchMode: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier)
// {pkg}/feature/supervised/permissions/PermissionsScreen.kt
fun PermissionsScreen(permissions: PermissionsUi, onOpenUsageAccessSettings: () -> Unit, onRecheckUsageAccess: () -> Unit,
                      onRequestLocation: () -> Unit, onRecheckLocation: () -> Unit, onRequestOverlay: () -> Unit,
                      onRecheckOverlay: () -> Unit, onRequestDeviceAdmin: () -> Unit, onBack: () -> Unit,
                      modifier: Modifier = Modifier)
```

## 3. Textos que el mockup dice mal (obligatorio)

| Mockup | Qué muestra | Qué hacer |
|---|---|---|
| 13 | «Cerrar sesión — Desvincular este dispositivo de tu cuenta.» | **FALSO**: cerrar sesión no desvincula. El subtítulo es **«Cierra la sesión en este dispositivo.»** |
| 13 | «Supervisado por Andrés» (uno) | **Todos** los tutores de `tutors`, uno por línea. |
| 13 | Miniatura de tablet | `IconTile(NpIcons.Smartphone, NpTone.Info, 72.dp)`. |
| 13 | «· En línea» junto a la hora | Solo si `reachable`: «Conectado» (`Success`); si no, «Sin conexión» (`Danger`). |
| 13 | Tarjeta «Permisos pendientes» siempre naranja | Naranja solo si `pendingPermissions > 0`; con 0, en verde. |
| 14 | «Abrir Ajustes» en los permisos ya configurados | Nada: `PermissionCard` ya oculta los botones si está concedido. |
| 14 | Iconos de colores distintos por permiso | `PermissionCard` usa su propio tono: no lo cambies. |
| 12 | «Esperando código de vinculación» | Texto de §5.1. |
| todos | «Ubicación precisa», «en segundo plano», «siempre», «en tiempo real» | Prohibido. |

## 4. Lo que NO debes tocar y archivos permitidos

No toques:
- `SupervisedShell.kt`, `SupervisedViews.kt`, `HomeScreen.kt`, `TutorShell.kt`;
- `{pkg}/core/**`, `{pkg}/ui/**`, `res/`, Gradle, manifiestos;
- `backend/`, `frontend/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`.

No hagas `git commit` ni `push`. Si necesitas algo que esté fuera de esta lista, escribe `[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar los cuerpos de los tres archivos de §2. Puedes añadir composables `private` en esos mismos archivos.
- Crear `mobile/app/src/androidTest/java/com/netprotect/app/ui/{LinkDeviceScreenTest,LinkedDeviceScreenTest,PermissionsScreenTest}.kt`.
- Modificar `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantallas

**Estructura común** (igual que `MoreScreen`):
- `Box` con `fillMaxSize()` y fondo `NpColors.SkyGround`;
- dentro, una `Column` con `statusBarsPadding()`, `verticalScroll` y relleno horizontal de 20 dp;
- **sin** `navigationBarsPadding()` y 24 dp al final.

**Prohibido:** `Color(0x…)`, tamaños de letra o radios fijos, `Instant.now()`, `Build.`, `Context`/`LocalContext`,
cualquier `Intent` o cualquier `launch`. Todo llega por parámetro.

### 5.1 `LinkDeviceScreen` (mockup 12)

En orden:
1. `BrandHeader()`, sin subtítulo, como en el mockup.
2. 24 dp.
3. **«Vincular este dispositivo»** (`Display`, `ShieldNavy`).
4. 8 dp.
5. **«Introduce el código que te dio tu tutor para vincular este dispositivo y comenzar a usar NetProtect.»** (`Body`,
   `SlateMuted`).
6. 20 dp.
7. **Tarjeta del código** (`NpCard`):
   - fila: `IconTile(NpIcons.KeyRound, NpTone.Info, 56.dp)` + 12 dp + columna con **«Código de vinculación»** (`Title`) y
     **«Ingresa el código de 6 dígitos que te dio tu tutor.»** (`Body`, `SlateMuted`);
   - 16 dp;
   - `OtpInput(value = code, onValueChange = onCodeChange, isError = error != null, enabled = !linking)`;
   - si `error != null`: 8 dp + `error` (`Body`, `DangerText`);
   - 16 dp;
   - `NpButton("Vincular dispositivo", onLink, Modifier.fillMaxWidth(), enabled = isCompletePairingCode(code) && !linking,
     loading = linking)`;
   - 8 dp;
   - **«¿No tienes un código? Pídeselo a tu tutor.»** (`Body`, `SignalBlue`, centrado). Es solo texto: no hay adónde
     llevar.
8. 16 dp.
9. **Tarjeta de estado** (`NpCard(containerColor = NpColors.BlueWash)`):
   - fila: `IconTile(NpIcons.Clock, NpTone.Neutral, 48.dp)` + 12 dp + columna con **«Esperando código de vinculación»**
     (`BodyStrong`) y **«La vinculación se comprueba con el servicio de NetProtect.»** (`Body`, `SlateMuted`);
   - 12 dp;
   - `NpButton("Comprobar vínculo", onCheckLink, Modifier.fillMaxWidth(), variant = Secondary,
     icon = NpIcons.RefreshCw, loading = rechecking)`.
10. 24 dp.
11. `NpButton("Cerrar sesión", onSignOut, Modifier.fillMaxWidth(), variant = Text)`.

Los errores ya llegan en español desde el shell («El código no es válido o ya venció.», «Demasiados intentos…», «Sin
conexión…»): muéstralos tal cual.

### 5.2 `LinkedDeviceScreen` (mockup 13)

En orden:
1. `BrandHeader(subtitle = "Dispositivo supervisado")`.
2. 24 dp.
3. **«Dispositivo vinculado»** (`Display`, `ShieldNavy`).
4. 8 dp.
5. **«Este dispositivo está vinculado a un tutor y ya forma parte de NetProtect.»** (`Body`, `SlateMuted`).
6. 20 dp.
7. **Tarjeta de vista remota** (Sprint 23), **solo si** `screenShareRequested`. Va primero porque alguien espera
   respuesta al otro lado. Es una `NpCard(containerColor = NpColors.BlueWash)` con:
   - fila: `IconTile(NpIcons.Monitor, NpTone.Info, 48.dp)` + 12 dp + **«Tu tutor quiere ver esta pantalla»** (`Title`);
   - 8 dp;
   - **«Si aceptas, Android te pedirá confirmarlo otra vez y verás un aviso permanente mientras dure la transmisión.
     Puedes detenerla en cualquier momento desde ese aviso.»** (`Body`, `Ink`). Es literal: son los textos que ya
     tiene la app;
   - 12 dp;
   - `NpButton("Aceptar", onAcceptScreenShare, Modifier.fillMaxWidth())`;
   - 8 dp;
   - `NpButton("Ahora no", onDeclineScreenShare, Modifier.fillMaxWidth(), variant = Secondary)`;
   - y 16 dp después de la tarjeta.
8. **Tarjeta del dispositivo** (`NpCard`):
   - **Fila:** `IconTile(NpIcons.Smartphone, NpTone.Info, 72.dp)` + 16 dp + una columna con:
     - `deviceName ?: "Este dispositivo"` (`Title`, `ShieldNavy`);
     - **«Android {androidVersion}»** (`Body`, `SlateMuted`);
     - 8 dp;
     - `StatusPill("Vinculado", NpTone.Success)`.
   - 16 dp, una línea divisoria (`HorizontalDivider(color = NpColors.Hairline)`) y 16 dp.
   - **Fila «Supervisado por»:** icono `NpIcons.User` de 24 dp (`SlateMuted`) + 16 dp + una columna con
     **«Supervisado por»** (`Caption` normal, `SlateMuted`) y cada nombre de `tutors` en su propia línea
     (`BodyStrong`, `ShieldNavy`).
   - 16 dp, línea divisoria y 16 dp.
   - **Fila «Última comunicación»:** icono `NpIcons.Clock` de 24 dp + 16 dp + una columna con:
     - **«Última comunicación»** (`Caption` normal, `SlateMuted`);
     - una fila con el relativo y el estado:
       - el relativo es `RelativeTime.format(lastContact, now, ZoneId.systemDefault())` con la primera letra en
         mayúscula («Hace 2 min»); si `lastContact == null`, **«Todavía no»** (`Body`, `Ink`);
       - después, **« · »** + **«Conectado»** (`Body`, `SuccessText`) si `reachable`, o **«Sin conexión»**
         (`Body`, `DangerText`) si no.
9. 12 dp.
10. `InfoBanner(title = "Información importante", text = "Este dispositivo reporta su estado cada minuto mientras la
    app esté abierta.")`.
11. 12 dp.
12. **Tarjeta de permisos** (`NpCard`):
    - fila: `IconTile(NpIcons.Shield, if (pendingPermissions > 0) NpTone.Warning else NpTone.Success, 48.dp)` + 12 dp +
      columna con **«Permisos del dispositivo»** (`Title`) y **«Para que NetProtect funcione correctamente, se
      requieren algunos permisos.»** (`Body`, `SlateMuted`);
    - 12 dp;
    - **caja de estado**: `NpCard(containerColor = tono.wash, borderColor = tono.wash)` con el tono `Warning` si hay
      pendientes o `Success` si no. Dentro, una fila con:
      - el icono `CircleAlert` o `CircleCheck` de 22 dp, con tinte `tono.text`;
      - 12 dp;
      - una columna con `pendingPermissionsLabel(pendingPermissions)` (`BodyStrong`, `tono.text`) y, **solo si hay
        pendientes**, **«Revisa y concede los permisos necesarios para completar la configuración.»** (`Body`,
        `SlateMuted`);
    - 12 dp;
    - `NpButton("Revisar permisos", onOpenPermissions, Modifier.fillMaxWidth(), icon = NpIcons.Settings)`.
13. 12 dp.
14. **Opciones de cuenta:**
    - `NpCard(onClick = onSwitchMode)` con `ListRow(title = "Cambiar de modo", subtitle = "Salir del modo supervisado en
      este dispositivo.", leading = { IconTile(NpIcons.ArrowLeftRight, NpTone.Info, 44.dp) }, trailing = { chevron })`;
    - 12 dp;
    - `NpCard(onClick = onSignOut)` con `ListRow(title = "Cerrar sesión", subtitle = "Cierra la sesión en este
      dispositivo.", leading = { IconTile(NpIcons.LogOut, NpTone.Danger, 44.dp) }, trailing = { chevron })`. Si
      `ListRow` no permite dar color rojo al título, déjalo como está y dilo en el informe.

    Mira cómo lo resuelve `MoreScreen` y úsalo como modelo.

### 5.3 `PermissionsScreen` (mockup 14)

En orden:
1. `NpTopBar(onBack = onBack)`: flecha y marca, como en el S45. No acepta subtítulo, así que el «DISPOSITIVO
   SUPERVISADO» del mockup se omite aquí.
2. **«Permisos del dispositivo»** (`Display`, `ShieldNavy`).
3. 8 dp.
4. **«Para que NetProtect funcione correctamente en este dispositivo, se requieren los siguientes permisos.»** (`Body`,
   `SlateMuted`).
5. 16 dp.
6. Cuatro `PermissionCard`, con 12 dp entre ellas:

   | Icono | Título | Descripción | Botón principal | Botón secundario |
   |---|---|---|---|---|
   | `NpIcons.LayoutGrid` | **«Acceso al uso de apps»** | **«Permite conocer cuánto tiempo se usan las aplicaciones y aplicar las reglas de tu tutor. Android exige activarlo en Ajustes.»** | **«Abrir Ajustes»** → `onOpenUsageAccessSettings` | **«Ya lo activé, verificar de nuevo»** → `onRecheckUsageAccess` |
   | `NpIcons.MapPin` | **«Ubicación aproximada»** | **«Permite reportar la última ubicación conocida y detectar geocercas con información aproximada. No se usa la ubicación precisa.»** | **«Permitir ubicación aproximada»** → `onRequestLocation` | **«Ya lo activé, verificar de nuevo»** → `onRecheckLocation` |
   | `NpIcons.Monitor` | **«Mostrar sobre otras apps»** | **«Necesario para mostrar la pantalla de bloqueo cuando corresponda, también con el teléfono en uso.»** | **«Abrir Ajustes»** → `onRequestOverlay` | **«Ya lo activé, verificar de nuevo»** → `onRecheckOverlay` |
   | `NpIcons.Shield` | **«Protección contra desinstalación»** | **«Permite avisar al tutor si se intenta desinstalar NetProtect; no elimina el dispositivo ni cambia contraseñas.»** | **«Activar protección»** → `onRequestDeviceAdmin` | ninguno |

   El valor de `granted` de cada tarjeta es `permissions.usageAccess`, `.location`, `.overlay` y `.deviceAdmin`.
7. 16 dp.
8. `InfoBanner(title = "Información importante", text = "Solo solicitamos los permisos necesarios para las funciones
   de NetProtect. Puedes revocarlos en cualquier momento desde los ajustes del dispositivo.")`.

## 6. Nada que retirar

`SupervisedScreen.kt` ya lo borró Claude.

## 7. Galería (solo debug)

En `ScreensGallery.kt`, pon estos frames **al principio** de la lista, **sin ningún diálogo**. Usa
`now = Instant.parse("2026-09-30T15:00:00Z")` y textos que parezcan reales.
- **`LinkDeviceScreen`:**
  - vacío;
  - con «123» escrito;
  - con 6 dígitos;
  - vinculando (`linking = true`);
  - con error «El código no es válido o ya venció.»;
  - comprobando (`rechecking = true`).
- **`LinkedDeviceScreen`:**
  - con 2 pendientes y dos tutores («Andrés Mosquera», «María Pérez»), `lastContact = now` menos 2 min y
    `reachable = true`;
  - 0 pendientes con `reachable = false`;
  - con `screenShareRequested = true`;
  - `deviceName = null` y `lastContact = null`.
- **`PermissionsScreen`:**
  - 2 pendientes (uso y ubicación);
  - todos configurados;
  - todos pendientes.

Guarda las capturas fuera del repo, en `%TEMP%\sprint48-gallery\`, y di en el informe qué frame sale en cada archivo.

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

- **`LinkDeviceScreenTest`:**
  - con 5 dígitos, «Vincular dispositivo» no está habilitado; con 6, sí, y llama a `onLink`;
  - con `linking = true`, no está habilitado;
  - `error` se muestra;
  - «Comprobar vínculo» llama a `onCheckLink`;
  - «Cerrar sesión» llama a `onSignOut`.
- **`LinkedDeviceScreenTest`:**
  - se ven los dos tutores;
  - se ve «Cierra la sesión en este dispositivo.» y **no** existe ningún texto que contenga «Desvincular»;
  - con 2 pendientes se ve «2 permisos pendientes»; con 0, «Todos los permisos están configurados»;
  - «Revisar permisos» llama a `onOpenPermissions`;
  - con `screenShareRequested`, «Aceptar» y «Ahora no» llaman a sus callbacks; sin él, no existen;
  - con `reachable = false` se ve «Sin conexión»;
  - con `lastContact = null` se ve «Todavía no»;
  - «Cambiar de modo» llama a `onSwitchMode`.
- **`PermissionsScreenTest`:**
  - con todo pendiente, cada botón llama a su callback (los 7);
  - con todo concedido, no existe ningún «Abrir Ajustes», «Permitir…» ni «Activar protección», y hay 4 «Configurado»;
  - no existe ningún texto que contenga «precisa» salvo «No se usa la ubicación precisa.»;
  - volver llama a `onBack`.

El emulador `Pixel_8` está encendido. Tiene la sesión abierta en modo supervisado con una cuenta real: **no cierres
sesión, no cambies de modo y no toques la app instalada**. Los tests instrumentados usan su propio `createComposeRule`
y no la afectan.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, los tres mockups y `docs/android-redesign/mockups/README.md` (secciones 12–14). Lee
   también `SupervisedViews.kt`, `PermissionCard.kt`, `OtpInput.kt`, `ListRow.kt`, `NpTopBar.kt`, `BrandHeader.kt` y
   `MoreScreen.kt`, y como ejemplo del resultado esperado `docs/delegated/done/sprint-47-mi-actividad.md`. Mueve el
   encargo a `active/`.
2. Cada pantalla, y compila con `cd mobile && ./gradlew compileDebugKotlin` después de cada una.
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
  `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|Build\.|LocalContext|Intent|\.launch|Desvincular|precisa" mobile/app/src/main/java/com/netprotect/app/feature/supervised/{link,linked,permissions}`
  → la única coincidencia permitida es «No se usa la ubicación precisa.».
- **Shell intacto:** `git diff --stat -- mobile/app/src/main/java/com/netprotect/app/feature/supervised/SupervisedShell.kt`
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

---

## Informe de DeepSeek

### 1. Archivos creados y modificados

- **Reemplazados los cuerpos** (firmas intactas) de:
  - `feature/supervised/link/LinkDeviceScreen.kt`
  - `feature/supervised/linked/LinkedDeviceScreen.kt`
  - `feature/supervised/permissions/PermissionsScreen.kt`
- **Creados** los 3 tests androidTest: `LinkDeviceScreenTest`, `LinkedDeviceScreenTest`, `PermissionsScreenTest`.
- **Modificado** `src/debug/.../ScreensGallery.kt`: 13 frames del S48 puestos **al principio** (antes de los del S47).

**No toqué** `SupervisedShell.kt`, `SupervisedViews.kt`, `PermissionCard.kt`, `OtpInput.kt`, `ListRow.kt`, `NpTopBar.kt`
ni nada de §4.

### 2. Salidas reales (§10 y tests)

- `./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL**.
- `lintDebug`: **0 errores, 24 avisos** (la base son 24; **0 avisos nuevos** en mis archivos; ninguno en
  `feature/supervised/{link,linked,permissions}`).
- `connectedDebugAndroidTest` (emulador `Pixel_8`): **116 tests, 0 fallos, 0 errores, 0 saltados** (96 previos + 20 de
  este sprint). Los 20 de `LinkDeviceScreenTest` (6), `LinkedDeviceScreenTest` (10) y `PermissionsScreenTest` (4)
  pasaron.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|Build\.|LocalContext|Intent|\.launch|Desvincular|precisa" mobile/app/src/main/java/com/netprotect/app/feature/supervised/{link,linked,permissions}`
  → **una sola coincidencia**, la permitida: `PermissionsScreen.kt:75` «…No se usa la ubicación precisa.».
- **Shell intacto:** `git diff --stat -- .../SupervisedShell.kt` → **vacío**.
- `git status --short` / `git diff --stat` → solo archivos de §4 (3 pantallas + `ScreensGallery.kt` + 3 tests + el
  movimiento del encargo).
- Capturas en `%TEMP%\sprint48-gallery\`:
  - `LinkDeviceScreen-·-vacío.png`, `-·-123.png`, `-·-6-dígitos.png`, `-·-vinculando.png`, `-·-error.png`, `-·-comprobando.png`.
  - `LinkedDeviceScreen-·-2-pendientes.png`, `-·-0-pendientes-sin-conexión.png`, `-·-vista-remota.png`, `-·-sin-nombre-ni-contacto.png`.
  - `PermissionsScreen-·-2-pendientes.png`, `PermissionsScreen-todos-configurados.png`, `PermissionsScreen-todos-pendientes.png`.

### 3. Decisiones / hallazgos fuera del encargo

- **`ListRow` no permite color de título** (el título es siempre `ShieldNavy`). Como el propio encargo anticipa en
  §5.2 paso 14 («Si `ListRow` no permite dar color rojo al título, déjalo como está y dilo en el informe»), el título
  «Cerrar sesión» queda en `ShieldNavy`, no en rojo; el subtítulo sí es el corregido «Cierra la sesión en este
  dispositivo.».
- El separador « · » entre el relativo y «Conectado»/«Sin conexión» (que el encargo no colorea) lo dejé en `Ink`,
  igual que el relativo.
- `StatusPill("Vinculado", NpTone.Success)` va con el punto por defecto (`showDot = true`), como pide §5.2.

### 4. Pendientes

- **Revisión visual** de las tres pantallas: para Claude (este modelo no acepta imágenes). Capturas en
  `%TEMP%\sprint48-gallery\` (listadas arriba).

### 5. Preguntas para Claude

Ninguna.

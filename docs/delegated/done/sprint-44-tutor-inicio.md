# Sprint 44 — Tutor: Inicio, Dispositivos, Detalle y Más (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Las cuatro pantallas del modo tutor se ven como los mockups `docs/android-redesign/mockups/03-tutor-inicio.jpg`
(Inicio) y `04-tutor-detalle-dispositivo.jpg` (Detalle), hechas con el tema y los componentes de los Sprints 42–43.
**Solo escribes presentación.** La navegación, las llamadas a la red, la cuenta atrás, la validación del nombre y el
manejo de errores ya existen (los hizo Claude) y llegan a tus pantallas como parámetros.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- `{pkg}/feature/tutor/TutorShell.kt`: rutas, barra inferior (Inicio · Dispositivos · Actividad · Más), "atrás",
  rotación, clientes HTTP y estados. **Ya llama a tus cuatro pantallas con todos sus datos.** No lo abras para editarlo.
- `{pkg}/feature/tutor/home/TutorHomeState.kt`: `PairingUi` (`Idle`, `Generating`, `Active(code, remainingSeconds,
  totalSeconds, revoking)`, `Expired`, `Failed(message)`) — la cuenta atrás ya viene calculada en `remainingSeconds`.
- `{pkg}/feature/tutor/device/DeviceDetailState.kt`: `RenameUi(text, error, saving)` con `canSave`, y la constante
  `DEVICE_GONE_MESSAGE`.
- `{pkg}/ui/state/LoadState.kt`: `Loading`, `Loaded(value)`, `Failed(message, notFound)`. El `message` ya está en español.
- `{pkg}/core/network/DeviceClient.kt`: `DeviceSummary(id, name, platform, status, lastSeenAt, timezone, osVersion, appVersion)`.
  `lastSeenAt` es un instante ISO-8601 en texto o `null`; `osVersion` puede ser `null`.
- Las cuatro pantallas existen con su **firma final** y un cuerpo **provisional** (feo pero completo). Tu trabajo es
  **reemplazar esos cuerpos** por el diseño. **No cambies ninguna firma** (nombre, parámetros, orden, tipos).
- Las seis secciones del dispositivo y la pestaña "Actividad" siguen con su aspecto antiguo oscuro (`legacy/`): **no las
  toques**, se rediseñan en los Sprints 45–47.

## 3. Decisiones ya tomadas (no las reabras)

| # | Decisión |
|---|---|
| — | Solo presentación. Nada de red, preferencias, relojes (`Instant.now()`, `System.currentTimeMillis()`) ni estado de sesión en tus archivos. El "ahora" llega como `now: Instant`. |
| — | La confirmación de desvincular es tuya (estado local de la pantalla con `rememberSaveable`): el botón abre `ConfirmDialog`; solo "Desvincular" llama a `onConfirmUnlink()`, **una vez**. "Cancelar" y tocar fuera no llaman a nada. |
| — | "Guardar" del renombrado: `enabled = rename.canSave`, `loading = rename.saving`. No valides el nombre tú: ya lo hace `canSave`. |
| — | Nunca muestres datos inventados. Sin miniatura de tablet: icono genérico `NpIcons.Smartphone`. Sin "Android 13" fijo: `"Android ${osVersion}"`, o `"Android"` si es `null`. El estado sale de `DeviceStatusLabels.label(status)` (etiqueta + tono), nunca el código crudo (`ONLINE`). |
| — | "Última actividad": `RelativeTime.format(Instant.parse(lastSeenAt), now, ZoneId.systemDefault())` dentro de `runCatching`; si `lastSeenAt` es `null` o no se puede leer: **"Sin actividad registrada todavía."** |
| — | Una sola cabecera del dispositivo en el Detalle (el mockup la dibuja dos veces). |
| — | **Cero** `Color(0x…)`, radios, tamaños de texto o fuentes literales: `NpColors`, `NpText`, `NpShapes`, `NpIcons`, `NpTone`. `dp` de espaciado sí, múltiplos de 4. |
| — | Cada pantalla pinta su fondo (`NpColors.SkyGround`), aplica `statusBarsPadding()` y hace `verticalScroll`. **No** aplica `navigationBarsPadding()` (la barra inferior del shell ya lo hace). |

## 4. Lo que NO debes tocar

`backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`, Gradle, manifiestos,
`{pkg}/feature/tutor/TutorShell.kt`, `TutorRoute.kt`, `*State.kt`, `{pkg}/feature/tutor/legacy/**`, `{pkg}/feature/home/**`,
`{pkg}/feature/supervised/**`, `{pkg}/core/**`, `{pkg}/ui/**` (tema, componentes, formato, iconos, navegación, estado),
`res/`, `MainActivity.kt`. Si un componente no hace lo que necesitas, **no lo modifiques**: usa el más parecido y
escribe `[PREGUNTA PARA CLAUDE]`. Sin `git commit`/`push`.

## 5. Archivos permitidos (lista cerrada)

Reemplazar cuerpos (firma intacta): `{pkg}/feature/tutor/home/TutorHomeScreen.kt`, `devices/DevicesScreen.kt`,
`device/DeviceDetailScreen.kt`, `more/MoreScreen.kt`.
Crear: `{pkg}/feature/tutor/devices/DeviceListItem.kt` (componente compartido por Inicio y Dispositivos; al terminar,
**borra `InterimDeviceList`** de `DevicesScreen.kt`, que ya no se usará).
Crear: `mobile/app/src/androidTest/java/com/netprotect/app/ui/{TutorHomeScreenTest,DevicesScreenTest,DeviceDetailScreenTest,MoreScreenTest}.kt`.
Modificar: `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (añadir secciones, §8).
Todo lo demás está prohibido.

## 6. Punto de partida (léelo antes de escribir)

`docs/sprint-43.md`, `docs/android-redesign/mockups/README.md` §3–4, los mockups 03 y 04, el código de §2, y los
componentes que vas a usar: `BrandHeader`, `NpCard`, `IconTile`, `ListRow`, `NpButton`, `StatusPill`, `SectionHeader`,
`InfoBanner`, `CodeDisplay`, `LoadingState`, `ErrorState`, `EmptyState`, `NpTopBar`, `ConfirmDialog`, y
`ui/format/{RelativeTime,DeviceStatusLabels}.kt`. Como referencia de estilo ya aprobado, mira
`{pkg}/feature/home/RoleSelectionScreen.kt` (tarjetas con `IconTile`). No puedes ver imágenes: la revisión visual la
hace Claude.

## 7. Pantallas — especificación

### 7.1 `DeviceListItem(device: DeviceSummary, now: Instant, onClick: () -> Unit, modifier: Modifier = Modifier)`
`NpCard(onClick = onClick)` con una fila: `IconTile(NpIcons.Smartphone, NpTone.Info, size = 56.dp)`, 12 dp, columna
(peso 1): nombre (`NpText.Title`, `ShieldNavy`); fila con **"Android {osVersion}"** (o "Android") en `NpText.Body`
`SlateMuted` + 8 dp + `StatusPill(label, tone)` de `DeviceStatusLabels`; debajo **"Última actividad: {relativo}"** (o
"Sin actividad registrada todavía.") en `NpText.Body` `SlateMuted`; debajo **"Los datos se sincronizan periódicamente."**
en `NpText.Caption` `SlateMuted`. A la derecha, icono `NpIcons.ChevronRight` 20 dp `SlateMuted`, `contentDescription = null`.

### 7.2 Estados de la lista (compartidos por Inicio y Dispositivos; escribe una función privada o un composable interno en `DeviceListItem.kt`)
- `Loading` → `LoadingState()`.
- `Failed` → `ErrorState(message, onRetry = <recargar>)`.
- `Loaded` vacío → `EmptyState(NpIcons.Smartphone, title = "Todavía no hay dispositivos vinculados.", message = "Genera un código de vinculación desde Inicio para añadir uno.")`.
- `Loaded` con datos → un `DeviceListItem` por dispositivo, 12 dp entre ellos.

### 7.3 `TutorHomeScreen` (mockup 3), de arriba abajo, relleno horizontal 20 dp
1. Fila: `BrandHeader(subtitle = "Panel del tutor")` a la izquierda; a la derecha un **avatar**: círculo de 44 dp con
   fondo `NpColors.BlueWash` y la **inicial** (primera letra de `userName`, o de `userEmail` si `userName` es null/blanco,
   en mayúscula) en `NpText.Title` `ShieldNavy`, con `contentDescription = "Cuenta: {userName o userEmail}"`.
2. 24 dp. Fila: **"Modo Tutor"** (`NpText.Display`, `ShieldNavy`, peso 1) y
   `NpButton("Cambiar de modo", onSwitchMode, variant = Secondary, icon = NpIcons.ArrowLeftRight, small = true)`.
3. 4 dp. **"Supervisa y gestiona los dispositivos vinculados."** (`NpText.BodyLead`, `SlateMuted`).
4. 20 dp. **Tarjeta de vinculación** (`NpCard`), según `pairing`:
   - `Idle`: fila `IconTile(NpIcons.Link, NpTone.Info, 56.dp)` + columna con **"Vincula un dispositivo nuevo"**
     (`Title`, `ShieldNavy`) y **"Genera un código temporal para vincular un teléfono o tablet supervisado."** (`Body`,
     `SlateMuted`); 16 dp; `NpButton("Generar código de vinculación", onGenerateCode, Modifier.fillMaxWidth(), icon = NpIcons.QrCode)`.
   - `Generating`: igual que `Idle` pero el botón con `loading = true`.
   - `Active`: **"Código de vinculación"** (`Label`, `SlateMuted`); 8 dp; `CodeDisplay(code, remainingSeconds, totalSeconds)`;
     8 dp; **"Uso único. Introdúcelo en el teléfono que vas a supervisar."** (`Body`, `SlateMuted`); 16 dp;
     `NpButton("Revocar", onRevokeCode, variant = Danger, loading = pairing.revoking)`.
   - `Expired`: **"El código venció. Genera uno nuevo."** (`Body`, `DangerText`); 12 dp;
     `NpButton("Generar código de vinculación", onGenerateCode, Modifier.fillMaxWidth(), icon = NpIcons.QrCode)`.
   - `Failed`: el `message` (`Body`, `DangerText`, `liveRegion = Assertive`); 12 dp;
     `NpButton("Entendido", onDismissPairing, variant = Secondary)`.
   En **todos** los estados, al final de la tarjeta: línea divisoria de 1 dp `Hairline` (16 dp arriba y abajo) y
   `NpButton("¿Ya se vinculó? Actualizar lista", onRefreshDevices, Modifier.fillMaxWidth(), variant = Text, icon = NpIcons.RefreshCw)`.
5. 24 dp. `SectionHeader("Dispositivos vinculados", actionLabel = "Actualizar", actionIcon = NpIcons.RefreshCw, onAction = onRefreshDevices)`.
6. 8 dp. La lista (§7.2) con `onClick = { onOpenDevice(device.id) }`.
7. 16 dp. `NpCard(onClick = onOpenActivity)` con fila: `IconTile(NpIcons.FileText, NpTone.Purple, 56.dp)`, columna con
   **"Mi actividad (auditoría)"** (`Title`, `ShieldNavy`) y **"Revisa las acciones realizadas en tu cuenta."** (`Body`,
   `SlateMuted`), y `ChevronRight`.
8. 16 dp. `InfoBanner(title = "Sobre la información de los dispositivos", text = "La ubicación, el uso de apps y las geocercas se sincronizan periódicamente y pueden tener un retraso de algunos minutos.")`.
9. 16 dp. `NpButton("Cerrar sesión", onSignOut, variant = Danger, icon = NpIcons.LogOut)`, alineado a la izquierda.
10. 24 dp al final (para que el último elemento no quede pegado a la barra inferior).

### 7.4 `DevicesScreen`
Relleno horizontal 20 dp. `BrandHeader(subtitle = "Panel del tutor")`; 24 dp; **"Dispositivos"** (`Display`, `ShieldNavy`);
4 dp; **"Toca un dispositivo para ver sus datos."** (`BodyLead`, `SlateMuted`); 16 dp;
`SectionHeader("Dispositivos vinculados", actionLabel = "Actualizar", actionIcon = NpIcons.RefreshCw, onAction = onRefresh)`;
8 dp; la lista (§7.2) con `onOpenDevice(device.id)`; 24 dp al final.

### 7.5 `DeviceDetailScreen` (mockup 4)
1. `NpTopBar(onBack = onBack)` (sin título: muestra la marca).
2. Según `device`:
   - `Loading` → `LoadingState()`.
   - `Failed` con `notFound = true` → `EmptyState(NpIcons.CircleAlert, title = device.message, message = "Puede que se haya desvinculado desde otro lugar.", actionLabel = "Volver a dispositivos", onAction = onBack)`.
   - `Failed` con `notFound = false` → `ErrorState(message, onRetry = onRefresh)`.
   - `Loaded` → lo siguiente, con relleno horizontal 20 dp.
3. Título: el nombre (`Display`, `ShieldNavy`). 8 dp. Fila: "Android {osVersion}" (o "Android") (`Body`, `SlateMuted`) +
   12 dp + `StatusPill` (`DeviceStatusLabels`). 8 dp. "Última actividad: {relativo}" (`Body`, `SlateMuted`) y debajo
   "Los datos se sincronizan periódicamente." (`Caption`, `SlateMuted`).
4. 16 dp. `NpCard` de gestión:
   - Fila con `NpButton("Renombrar", onStartRename, variant = Secondary, icon = NpIcons.Pencil, small = true)` y
     `NpButton("Actualizar", onRefresh, variant = Secondary, icon = NpIcons.RefreshCw, small = true)` (8 dp entre ellos).
     Oculta "Renombrar" mientras `rename != null`.
   - Si `rename != null`: 16 dp; `OutlinedTextField(value = rename.text, onValueChange = onEditRename, label = { Text("Nombre") }, singleLine = true, isError = rename.error != null, modifier = Modifier.fillMaxWidth())`
     con `OutlinedTextFieldDefaults.colors(...)` usando solo `NpColors` (borde con foco `SignalBlue`, sin foco `HairlineStrong`,
     error `Danger`); si `rename.error != null`, el texto debajo en `Body` `DangerText`; 12 dp; fila con
     `NpButton("Guardar", onSaveRename, Modifier.weight(1f), enabled = rename.canSave, loading = rename.saving)` y
     `NpButton("Cancelar", onCancelRename, Modifier.weight(1f), variant = Secondary)`.
   - 16 dp. **Zona de peligro**: `NpCard(onClick = { abrir diálogo })` con fila: `IconTile(NpIcons.Trash2, NpTone.Danger, 48.dp)`,
     columna con **"Desvincular dispositivo"** (`BodyStrong`, `DangerText`) y **"Se eliminará la conexión de este dispositivo con tu cuenta de tutor."** (`Body`, `SlateMuted`),
     y `ChevronRight`. Si `unlinking`, en lugar del chevron un `CircularProgressIndicator` de 20 dp `Danger` y la tarjeta sin `onClick`.
     Si `unlinkError != null`, el texto debajo de la tarjeta (`Body`, `DangerText`, `liveRegion = Assertive`).
5. 16 dp. Seis tarjetas de sección, 12 dp entre ellas, cada una `NpCard(onClick = { onOpenSection(sección) })` con
   `IconTile(icono, tono, 48.dp)` + título (`Title`, `ShieldNavy`) + descripción (`Body`, `SlateMuted`) + `ChevronRight`:

   | Sección | Icono | Tono | Título | Descripción |
   |---|---|---|---|---|
   | `DeviceSection.Apps` | `LayoutGrid` | `Info` | Apps | Revisa el uso de aplicaciones en este dispositivo. |
   | `Location` | `MapPin` | `Purple` | Ubicación | Última ubicación conocida y registros. |
   | `Geofences` | `Map` | `Success` | Geocercas | Consulta las geocercas y su historial. |
   | `History` | `History` | `Warning` | Historial | Eventos de actividad, bloqueos y geocercas. |
   | `Statistics` | `BarChart3` | `Info` | Estadísticas | Uso de apps, bloqueos y cumplimiento. |
   | `Alerts` | `TriangleAlert` | `Danger` | Alertas | Eventos importantes de este dispositivo. |
6. 24 dp al final.
7. **Diálogo**: si el estado local `confirming` es true, `ConfirmDialog(title = "¿Desvincular {nombre}?", message = "Se eliminará la conexión de este dispositivo con tu cuenta de tutor. Para volver a supervisarlo habrá que vincularlo de nuevo.", confirmLabel = "Desvincular", dismissLabel = "Cancelar", onConfirm = { confirming = false; onConfirmUnlink() }, onDismiss = { confirming = false }, danger = true)`.

### 7.6 `MoreScreen`
Relleno horizontal 20 dp. `BrandHeader(subtitle = "Panel del tutor")`; 24 dp; **"Más"** (`Display`, `ShieldNavy`); 16 dp; tres `NpCard`, 12 dp entre ellas:
1. `NpCard(onClick = onSwitchMode)`: `IconTile(NpIcons.ArrowLeftRight, Info, 48.dp)` + **"Cambiar de modo"** (`Title`) +
   **"Usa este teléfono como dispositivo supervisado."** (`Body`, `SlateMuted`) + `ChevronRight`.
2. `NpCard(onClick = onSignOut)`: `IconTile(NpIcons.LogOut, Danger, 48.dp)` + **"Cerrar sesión"** (`Title`, `DangerText`) +
   **"Cierra la sesión en este teléfono."** (`Body`, `SlateMuted`).
3. `NpCard` sin `onClick`: `IconTile(NpIcons.ShieldCheck, Success, 48.dp)` + **"Acerca de NetProtect"** (`Title`) +
   **"Versión {appVersion}"** (`Body`, `SlateMuted`); 12 dp; el texto aprobado por el dueño, **literal**:
   **"NetProtect solo muestra lo que el dispositivo supervisado comparte con tu cuenta. La ubicación es aproximada, nada se graba y cada acción queda en tu registro de actividad."**
   (`Body`, `Ink`).

## 8. Galería (solo debug)

En `ScreensGallery.kt`, añade al final, con el mismo marco que ya usan las demás, secciones con datos obviamente
ilustrativos (dispositivo `"Tablet de Sofía"`, `osVersion = "13"`, `status = "ONLINE"`, `lastSeenAt` = `now` menos 12 min;
otro con `osVersion = null`, `status = "OFFLINE"`, `lastSeenAt = null`) y lambdas vacías:
`TutorHomeScreen` × { Idle con 2 dispositivos, Active (482917, 143 de 180 s), Expired con lista vacía, Failed con lista en error };
`DevicesScreen` × { Loading, Loaded con 2 };
`DeviceDetailScreen` × { Loaded, Loaded renombrando con error "Sin conexión con el servidor. Revisa tu conexión e intenta de nuevo.", Failed notFound };
`MoreScreen` × 1 (`appVersion = "0.1.0"`). Usa un `now` fijo (`Instant.parse("2026-09-29T15:00:00Z")`).

## 9. Tests (androidTest, `createComposeRule()`)

**Toca siempre con `.performScrollTo().performClick()`** (en CI la pantalla es más pequeña y los botones quedan fuera de vista:
así falló el Sprint 43).
- `TutorHomeScreenTest`: `Idle` → "Generar código de vinculación" llama a `onGenerateCode`; `Active` muestra "Uso único…" y
  "Revocar" llama a `onRevokeCode`; `Expired` muestra "El código venció. Genera uno nuevo."; `Failed` muestra el mensaje y
  "Entendido" llama a `onDismissPairing`; lista vacía muestra "Todavía no hay dispositivos vinculados."; tocar un
  dispositivo llama a `onOpenDevice` con su id; `osVersion = null` muestra "Android" (y no "Android null"); el estado se
  muestra como "En línea" y nunca "ONLINE"; `lastSeenAt = null` muestra "Sin actividad registrada todavía."
- `DevicesScreenTest`: `Failed` muestra el mensaje y "Reintentar" llama a `onRefresh`.
- `DeviceDetailScreenTest`: tocar "Desvincular dispositivo" abre el diálogo; "Cancelar" lo cierra **sin** llamar a
  `onConfirmUnlink`; "Desvincular" lo llama **exactamente una vez**; con `rename = RenameUi("   ")` el botón "Guardar" está
  deshabilitado (`assertIsNotEnabled`); `Failed(notFound = true)` muestra el mensaje y "Volver a dispositivos" llama a `onBack`;
  tocar "Alertas" llama a `onOpenSection(DeviceSection.Alerts)`.
- `MoreScreenTest`: muestra "Versión 0.1.0" y la frase de privacidad; las tarjetas llaman a sus lambdas.
El emulador `Pixel_8` está encendido (`adb devices` con la ruta completa de `AGENTS.md`).

## 10. Pasos (con comprobación)

1. Lee `AGENTS.md`, este encargo y lo de §6; mueve el encargo a `active/`.
2. `DeviceListItem.kt` → `cd mobile && ./gradlew compileDebugKotlin`. Luego cada pantalla, compilando tras cada una.
3. Borra `InterimDeviceList` y compila.
4. Galería (§8) → `./gradlew assembleDebug`. Tests (§9) → `./gradlew assembleDebugAndroidTest` y `connectedDebugAndroidTest`.
5. Verificación (§11) e informe. Si Gradle falla con `resource … not found` sin razón aparente, `./gradlew clean` una vez.

## 11. Verificación

Pega la salida **real**:
- `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → `BUILD SUCCESSFUL`;
  lint 0 errores; avisos nuevos con ruta:línea (base: 25).
- `./gradlew connectedDebugAndroidTest`: número de tests y resultado.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|currentTimeMillis" mobile/app/src/main/java/com/netprotect/app/feature/tutor/{home,devices,device,more}` →
  sin coincidencias (salvo el `Instant` que recibes por parámetro, que no es `Instant.now`).
- `grep -rn "InterimDeviceList" mobile/app/src` → sin coincidencias.
- `git status --short` y `git diff --stat`: solo archivos de §5. `TutorShell.kt` y `*State.kt` **no** deben aparecer.
- Capturas de la galería de pantallas con `adb exec-out screencap -p` **fuera del repo**; dilo en el informe.

## 12. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos modificados/creados. 2. Salidas reales de §11 y de los tests. 3. Decisiones tuyas fuera del encargo (y por qué).
4. Pendientes. 5. `[PREGUNTA PARA CLAUDE]`. Si una instrucción de este encargo contradice a otra o al código, **dilo en el
informe** en lugar de elegir en silencio. Lo que no ejecutaste, se declara PENDIENTE.

---

## Informe de DeepSeek

### 1. Archivos modificados / creados

**Reemplazados los cuerpos (firmas intactas)**: `feature/tutor/home/TutorHomeScreen.kt`,
`feature/tutor/devices/DevicesScreen.kt`, `feature/tutor/device/DeviceDetailScreen.kt`,
`feature/tutor/more/MoreScreen.kt`. `InterimDeviceList` **eliminado** (verificado: 0 coincidencias).

**Creados**: `feature/tutor/devices/DeviceListItem.kt` (con `DeviceListItem` + el composable interno
`DeviceList` de los 4 estados) y los 4 tests androidTest
(`TutorHomeScreenTest`, `DevicesScreenTest`, `DeviceDetailScreenTest`, `MoreScreenTest`).

**Modificado**: `src/debug/.../ScreensGallery.kt` (secciones nuevas de las 4 pantallas).

`TutorShell.kt` y los `*State.kt` **no** aparecen en `git status`.

### 2. Salidas reales (§11 y tests)

- `./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL** (51 s).
- `lintDebug`: **0 errores, 25 avisos** (base 25); **ninguno** en `feature/tutor/{home,devices,device,more}`.
- `./gradlew connectedDebugAndroidTest` (emulador `Pixel_8`): **50 tests, 0 fallos, 0 saltados**. De ellos, 18 son de este sprint y pasaron:
  - `TutorHomeScreenTest` (9): idle→onGenerateCode; active→"Uso único…"+"Revocar"→onRevokeCode; expired→"El código venció…"; failed→mensaje+"Entendido"→onDismissPairing; lista vacía→"Todavía no hay dispositivos…"; tocar dispositivo→onOpenDevice(id); osVersion null→"Android" (no "Android null"); estado→"En línea" (no "ONLINE"); lastSeenAt null→"Sin actividad registrada todavía.".
  - `DevicesScreenTest` (1): failed→mensaje+"Reintentar"→onRefresh.
  - `DeviceDetailScreenTest` (5): desvincular abre diálogo y "Cancelar" no llama; "Desvincular" llama una vez; `RenameUi("   ")`→"Guardar" deshabilitado; `Failed(notFound=true)`→mensaje+"Volver a dispositivos"→onBack; "Alertas"→onOpenSection(Alerts).
  - `MoreScreenTest` (3): "Versión 0.1.0" + frase de privacidad; "Cambiar de modo"→onSwitchMode; "Cerrar sesión"→onSignOut.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|currentTimeMillis" .../feature/tutor/{home,devices,device,more}` → **sin coincidencias**.
- `grep -rn "InterimDeviceList" mobile/app/src` → **sin coincidencias**.
- Galería abierta y capturadas **9 pantallas distintas** fuera del repo en `%TEMP%\sprint44-gallery\` (esta vez el deslizamiento lento de 1500 ms sí hizo scroll del `verticalScroll` exterior, a diferencia del S43 donde usé 1200 ms). La actividad quedó enfocada y sin `FATAL` en logcat.

### 3. Decisiones tuyas fuera del encargo

- **`DeviceList`** (composable `internal` en `DeviceListItem.kt`) es la "función privada o composable interno" que pedía §7.2; recibe `onRetry` además de los datos.
- **Formato "Android {osVersion}" y "Última actividad: …" duplicado** en `DeviceDetailScreen` (inline) respecto a los helpers `private` de `DeviceListItem.kt`, porque están en paquetes distintos (`device` vs `devices`) y no quise acoplar pantallas.
- **`MoreScreen`**: añadí un `Spacer(24.dp)` final (no lo menciona §7.6 explícitamente) por coherencia con las otras tres pantallas y para no pegar la última tarjeta a la barra inferior.
- **Tarjeta "Cerrar sesión" de `MoreScreen` sin chevron** (el encargo la describe sin él, a diferencia de "Cambiar de modo"): lo seguí literal.

### 4. Pendientes

- **Revisión visual** de las 4 pantallas y la galería: para Claude (este modelo no acepta imágenes). Las capturas quedan en `%TEMP%\sprint44-gallery\`.

### 5. Preguntas para Claude

Ninguna.

# Sprint 45 — Tutor: Apps, Ubicación y Geocercas (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Las pantallas Apps, Ubicación y Geocercas del tutor se ven como los mockups
`docs/android-redesign/mockups/05-tutor-apps.jpg`, `06-tutor-ubicacion.jpg` y `07-tutor-geocercas.jpg`, **sin copiar los
datos que el mockup inventa** (§3). Son datos de un menor: el riesgo de este sprint es mostrar más o distinto de lo que hay.
**Solo escribes presentación**: la carga, el orden, el filtro, el "dentro de" y el último evento ya están calculados.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- `{pkg}/feature/tutor/TutorShell.kt` ya enruta las tres secciones a tus pantallas y les pasa los datos. **No lo abras.**
- `{pkg}/feature/tutor/sections/SectionViews.kt` (funciones puras, con tests):
  `sortApps(apps)`, `filterApps(apps, query)`, `LocationView(report, insideGeofence)`,
  `GeofencesView(items: List<GeofenceItem(geofence, lastEvent)>, history)`.
- `{pkg}/ui/format/UsageText.kt`: `usageLabel(seconds, usageDate, today)` ("1 h 24 min", "12 min · ayer", "12 min · 24 sep",
  "Sin uso registrado") y `eventTimeLabel(isoInstant, today, zone)` ("Hoy, 8:12 a. m."). `RelativeTime.format(...)` ya existía.
- `{pkg}/ui/components/AppIcon.kt`: `AppIcon(packageName, label, modifier, size)` (icono real si la app está en este teléfono;
  si no, monograma). Úsalo tal cual.
- Las tres pantallas existen con **firma final** y un cuerpo provisional. **Reemplaza los cuerpos; no cambies ninguna firma:**

```kotlin
fun AppsScreen(device: DeviceSummary?, apps: LoadState<List<DeviceApplicationSummary>>, today: LocalDate, now: Instant,
               onRefresh: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier)
fun LocationScreen(device: DeviceSummary?, location: LoadState<LocationView>, now: Instant,
                   onRefresh: () -> Unit, onOpenMap: () -> Boolean, onBack: () -> Unit, modifier: Modifier = Modifier)
fun GeofencesScreen(device: DeviceSummary?, geofences: LoadState<GeofencesView>, today: LocalDate, now: Instant,
                    onRefresh: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier)
```

`device` puede ser `null` (la lista aún no cargó): entonces **no** se dibuja la cabecera del dispositivo.
`onOpenMap()` devuelve `false` si no hay app de mapas.

## 3. Lo que NO se dibuja aunque el mockup lo muestre (obligatorio)

| Mockup | Qué muestra | Qué hacer |
|---|---|---|
| 06 Ubicación | Nombre de lugar ("Acacías, Meta, Colombia") | **Nada.** No existe; obtenerlo enviaría la ubicación del menor a un tercero. |
| 06 Ubicación | Mapa con el punto | **Nada.** Sin mapa ni imagen de mapa, ni decorativa. |
| 06 Ubicación | (implícito) coordenadas | **No muestres latitud ni longitud** en ningún texto (decisión del dueño). |
| 07 Geocercas | Ciudad bajo cada geocerca ("Villavicencio, Meta") | **Nada.** Se muestra "Radio {m} m". |
| 07 Geocercas | Iconos distintos (casa, birrete, edificio) | **Un solo icono** `NpIcons.MapPin` para todas (inventar un tipo por el nombre sería adivinar). |
| 05 Apps | Flecha ">" en cada app | **Sin flecha**: no hay pantalla de detalle de app. |
| 05/06/07 | Miniatura de tablet | `IconTile(NpIcons.Smartphone)` como en el S44. |
| todos | Cualquier "en tiempo real", "ahora", "en vivo" | Prohibido. Los textos de retraso de §5 son obligatorios. |
| todos | Botones de crear/editar reglas o geocercas | Prohibido: solo lectura. |

## 4. Lo que NO debes tocar y archivos permitidos

No toques: `backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`, Gradle, manifiestos,
`TutorShell.kt`, `TutorRoute.kt`, `sections/SectionViews.kt`, `*State.kt`, `{pkg}/core/**`, `{pkg}/ui/**`, `{pkg}/feature/home/**`,
`{pkg}/feature/supervised/**`, las pantallas del S44, `res/`. Sin `git commit`/`push`. Si necesitas algo de fuera, escribe
`[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar cuerpos: `{pkg}/feature/tutor/apps/AppsScreen.kt`, `location/LocationScreen.kt`, `geofences/GeofencesScreen.kt`.
- Crear: `{pkg}/feature/tutor/sections/SectionDeviceHeader.kt` (cabecera compacta compartida, §5.1).
- Modificar: `{pkg}/feature/tutor/legacy/LegacySections.kt` **solo para quitar** lo de apps, ubicación y geocercas (§6).
- Crear: `mobile/app/src/androidTest/java/com/netprotect/app/ui/{AppsScreenTest,LocationScreenTest,GeofencesScreenTest}.kt`.
- Modificar: `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantallas

Todas: fondo `NpColors.SkyGround`, `statusBarsPadding()`, `verticalScroll`, relleno horizontal 20 dp, **sin**
`navigationBarsPadding()`. Arriba `NpTopBar(onBack = onBack)`, luego el título (`NpText.Display`, `ShieldNavy`), 12 dp, la
cabecera del dispositivo si `device != null` (§5.1), 16 dp, y el contenido. `Loading` → `LoadingState()`;
`Failed` → `ErrorState(message, onRetry = onRefresh)`. 24 dp al final. Cero `Color(0x…)`, tamaños de letra, radios o
`Instant.now()`/`LocalDate.now()`: el tiempo llega por parámetro. Zona: `ZoneId.systemDefault()`.

### 5.1 `SectionDeviceHeader(device: DeviceSummary, lines: List<String>, modifier: Modifier = Modifier)`
Fila: `IconTile(NpIcons.Smartphone, NpTone.Info, 56.dp)`, 12 dp, columna: nombre (`Title`, `ShieldNavy`); fila con
"Android {osVersion}" (o "Android") (`Body`, `SlateMuted`) + 8 dp + `StatusPill` de `DeviceStatusLabels.label(status)`;
después cada texto de `lines` en `NpText.Caption.copy(fontWeight = FontWeight.Normal)` `SlateMuted`. Sin tarjeta (fondo de
la pantalla), como en los mockups.

### 5.2 `AppsScreen` (mockup 5)
1. Título **"Apps del dispositivo"**. Cabecera con una línea: **"Uso sincronizado periódicamente · última actualización {relativo}"**
   donde `{relativo}` = `RelativeTime.format(Instant.parse(device.lastSeenAt), now, zone)`; si `lastSeenAt` es null o no se lee:
   **"Uso sincronizado periódicamente"**.
2. `NpCard` con una fila: campo de búsqueda (peso 1) y `NpButton("Actualizar", onRefresh, variant = Secondary, icon = NpIcons.RefreshCw, small = true)`.
   Campo: `OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text("Buscar aplicación…") }, leadingIcon = { Icon(NpIcons.Search …, contentDescription = null) })`,
   colores solo de `NpColors` (como en el Detalle del S44). `query` con `rememberSaveable`.
3. 12 dp. Según `apps`:
   - `Loaded` vacío → `EmptyState(NpIcons.LayoutGrid, "Sin apps sincronizadas", "Todavía no se sincronizó ninguna app desde este dispositivo.")`.
   - `Loaded` → `val visible = filterApps(sortApps(apps.value), query)`; si `visible` vacío pero hay apps → texto
     **"Ninguna app coincide con «{query}»."** (`Body`, `SlateMuted`); si no, una `NpCard` por app (8 dp entre ellas) con
     fila: `AppIcon(app.packageName, app.appLabel, size = 44.dp)`, 12 dp, columna (peso 1): `appLabel` (`BodyStrong`, `ShieldNavy`)
     y `usageLabel(app.latestUsageSeconds, app.latestUsageDate, today)` (`Body`, `SlateMuted`); a la derecha, **solo si**
     `uninstalledAt != null`, `StatusPill("Desinstalada", NpTone.Danger, showDot = false)`. **Sin chevron.**
4. 16 dp. `InfoBanner(title = "Información importante", text = "Los tiempos de uso se sincronizan periódicamente y pueden tener un retraso de algunos minutos.")`.

### 5.3 `LocationScreen` (mockup 6)
1. Título **"Ubicación"**. Cabecera con dos líneas: **"Última comunicación: {relativo de lastSeenAt}"** (o
   **"Sin comunicación registrada todavía."**) y **"Los datos se sincronizan periódicamente."**
2. Según `location`:
   - `Loaded` con `report == null` → `EmptyState(NpIcons.MapPin, "Sin ubicación todavía", "Este dispositivo todavía no ha reportado su ubicación.")`.
   - `Loaded` con `report` → `NpCard`:
     a. Fila: `IconTile(NpIcons.MapPin, NpTone.Purple, 56.dp)` + columna con **"Última ubicación conocida"** (`Title`) y el
        relativo de `report.capturedAt` con la primera letra en mayúscula (p. ej. **"Hace 12 min"**) (`Body`, `SlateMuted`).
     b. 16 dp. **Si** `insideGeofence != null`: fila con icono `NpIcons.ShieldCheck` 20 dp `Success` + columna
        **"Dentro de «{insideGeofence}»"** (`BodyStrong`, `ShieldNavy`) y **"Según la última lectura y su precisión."**
        (`Caption` normal, `SlateMuted`). 12 dp.
     c. Fila icono `NpIcons.Clock` 20 dp `SlateMuted` + **"Última lectura: {eventTimeLabel(report.capturedAt, today, zone)}"**
        (`Body`, `Ink`) — `today` = `now.atZone(zone).toLocalDate()`.
     d. 8 dp. Fila icono `NpIcons.LocateFixed` 20 dp + **"Precisión aproximada: ~{accuracyMeters redondeado} m"**.
     e. 16 dp. `NpButton("Abrir en mapa", onClick = { mapMissing = !onOpenMap() }, Modifier.fillMaxWidth(), icon = NpIcons.Map, trailingIcon = NpIcons.ExternalLink)`.
     f. 8 dp. **"Se abrirá tu aplicación de mapas con la última ubicación conocida del dispositivo."** (`Caption` normal,
        `SlateMuted`, centrado). Si `mapMissing` (estado local): debajo **"No hay una aplicación de mapas en este teléfono."**
        (`Body`, `DangerText`).
3. 16 dp. `InfoBanner(title = "Información importante", text = "La ubicación es aproximada y se sincroniza periódicamente. Puede tener un retraso de algunos minutos.")`.
**Nunca** lat/lng, nombre de lugar ni mapa.

### 5.4 `GeofencesScreen` (mockup 7)
1. Título **"Geocercas"**. Cabecera con dos líneas: **"Detección aproximada · ~15 min"** y
   **"Las entradas y salidas se detectan en el siguiente reporte."**
2. `SectionHeader("Geocercas registradas", actionLabel = "Actualizar", actionIcon = NpIcons.RefreshCw, onAction = onRefresh)`.
3. Según `geofences` (`Loaded`):
   - `items` vacío → `EmptyState(NpIcons.Map, "No hay geocercas configuradas", "Se crean desde el panel web.")`.
   - Si no, una `NpCard` por `item` (8 dp entre ellas): fila `IconTile(NpIcons.MapPin, NpTone.Info, 48.dp)` + columna:
     `geofence.name` (`Title`, `ShieldNavy`); **"Radio {radiusMeters redondeado} m"** (`Body`, `SlateMuted`); 8 dp; fila con la
     píldora del último evento y su hora:
       - `lastEvent.eventType == "ENTER"` → `StatusPill("Último evento: Entró", NpTone.Success)`;
       - `"EXIT"` → `StatusPill("Último evento: Salió", NpTone.Warning)`;
       - `null` → `StatusPill("Sin actividad", NpTone.Neutral)`;
       y, si hay evento, 8 dp + `eventTimeLabel(lastEvent.occurredAt, today, zone)` (`Caption` normal, `SlateMuted`).
4. 16 dp. `SectionHeader("Historial de entradas/salidas")`. `NpCard` con un `TimelineItem` por evento de `history`:
   `icon = if ENTER NpIcons.LogIn else NpIcons.LogOut`, `tone = if ENTER Success else Warning`,
   `title = "Entró · {geofenceName}"` / `"Salió · {geofenceName}"`, `time = eventTimeLabel(...)`, `isLast` en el último.
   Sin eventos → **"Sin entradas ni salidas registradas."** (`Body`, `SlateMuted`) dentro de la tarjeta.
5. 16 dp. `InfoBanner(title = "Información importante", text = "Las entradas y salidas de las geocercas se detectan en el siguiente reporte del dispositivo y pueden tener un retraso de algunos minutos (~15 min).")`.
**Sin** ningún botón de crear, editar ni borrar.

## 6. Retirar lo antiguo de `LegacySections.kt`

Quita (ya no se usan): `AppsState`, `LocationState`, `GeofenceState`, `AppsList`, `AppUsageRow`, `formatUsageDuration`,
`LocationSection`, `GeofenceSection`, `GeofenceEventRow`, y en `LegacyDeviceSectionScreen` las ramas `Apps`, `Location` y
`Geofences` (sustitúyelas por una sola rama `DeviceSection.Apps, DeviceSection.Location, DeviceSection.Geofences -> Unit`
con el comentario `// Sprint 45: rediseñadas; TutorShell las enruta a sus pantallas.`). Quita los imports que queden sin uso.
**No toques** historial, estadísticas, alertas ni auditoría. Compila después.

## 7. Galería (solo debug)

En `ScreensGallery.kt`, añade con el mismo marco (datos obviamente ilustrativos, `now = Instant.parse("2026-09-29T15:00:00Z")`,
`today = LocalDate.of(2026, 9, 29)`, un `DeviceSummary` "Tablet de Sofía"):
`AppsScreen` × { 6 apps — usa `com.android.chrome` y `com.google.android.youtube` para ver iconos reales en el emulador, una con
uso de ayer, una desinstalada, una sin uso —; vacío; error };
`LocationScreen` × { con `insideGeofence = "Casa"`; sin zona; `report = null`; error };
`GeofencesScreen` × { 3 zonas (entró / salió / sin actividad) con 5 eventos; vacío; error }.
**Sin coordenadas en ningún texto visible.**

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

- `AppsScreenTest`: con 3 apps, escribir en el buscador filtra (queda solo la que coincide); sin coincidencias muestra
  "Ninguna app coincide con «…»."; una desinstalada muestra "Desinstalada"; ninguna fila tiene acción de clic (no hay nodo con
  `hasClickAction()` que contenga el nombre de la app); "Actualizar" llama a `onRefresh`; vacío muestra "Sin apps sincronizadas".
- `LocationScreenTest`: con `insideGeofence = "Casa"` aparece "Dentro de «Casa»"; con `null` **no** existe ningún texto que
  empiece por "Dentro de"; con un `report` de lat `4.15123` / lng `-73.63456` **no** existe ningún nodo cuyo texto contenga
  `"4.15"` ni `"-73.6"`; "Abrir en mapa" llama a `onOpenMap`; si devuelve `false` aparece "No hay una aplicación de mapas en este
  teléfono."; `report = null` muestra "Este dispositivo todavía no ha reportado su ubicación."
- `GeofencesScreenTest`: una zona con ENTER muestra "Último evento: Entró", con EXIT "Último evento: Salió", sin eventos
  "Sin actividad"; se muestra "Radio 150 m"; no existe ningún texto "Crear", "Editar" ni "Eliminar"; vacío muestra "No hay
  geocercas configuradas".
El emulador `Pixel_8` está encendido.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, los tres mockups, `mockups/README.md` §5–7 y el código de §2; mueve el encargo a `active/`.
2. `SectionDeviceHeader.kt` → `cd mobile && ./gradlew compileDebugKotlin`. Luego cada pantalla, compilando tras cada una.
3. Retira lo antiguo (§6) y compila.
4. Galería → `./gradlew assembleDebug`. Tests → `./gradlew assembleDebugAndroidTest connectedDebugAndroidTest`.
5. Verificación (§10) e informe. Si Gradle falla con `resource … not found` sin razón aparente, `./gradlew clean` una vez.

## 10. Verificación

Pega la salida **real**:
- `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → `BUILD SUCCESSFUL`; lint
  0 errores; avisos nuevos con ruta:línea (base: 25).
- `connectedDebugAndroidTest`: número de tests y resultado.
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalDate\.now|currentTimeMillis|latitude|longitude" mobile/app/src/main/java/com/netprotect/app/feature/tutor/{apps,location,geofences,sections/SectionDeviceHeader.kt}` →
  sin coincidencias (las pantallas no deben leer `latitude`/`longitude` para nada).
- `grep -rnE "AppsList|LocationSection|GeofenceSection|formatUsageDuration" mobile/app/src` → sin coincidencias.
- `git status --short` y `git diff --stat`: solo archivos de §4.
- Capturas de la galería **fuera del repo**; dilo en el informe.

## 11. Informe final (`## Informe de DeepSeek` al final de este archivo)

1. Archivos. 2. Salidas reales de §10 y de los tests. 3. Decisiones tuyas fuera del encargo (y por qué). 4. Pendientes.
5. `[PREGUNTA PARA CLAUDE]`. Si una instrucción contradice a otra o al código, **dilo** en lugar de elegir en silencio. Lo que no
ejecutaste se declara PENDIENTE.

---

## Informe de DeepSeek

### 1. Archivos

**Reemplazados los cuerpos (firmas intactas)**: `feature/tutor/apps/AppsScreen.kt`,
`feature/tutor/location/LocationScreen.kt`, `feature/tutor/geofences/GeofencesScreen.kt`.
**Creado**: `feature/tutor/sections/SectionDeviceHeader.kt` (cabecera compartida, §5.1).
**Modificado**: `feature/tutor/legacy/LegacySections.kt` — quité `AppsState`, `LocationState`, `GeofenceState`,
`AppsList`, `AppUsageRow`, `formatUsageDuration`, `LocationSection`, `GeofenceSection`, `GeofenceEventRow`, los
imports que quedaban sin uso, y las ramas `Apps`/`Location`/`Geofences` de `LegacyDeviceSectionScreen` (sustituidas por
`DeviceSection.Apps, DeviceSection.Location, DeviceSection.Geofences -> Unit` con el comentario pedido). Sin tocar
historial/estadísticas/alertas/auditoría.
**Creados** los 3 tests androidTest (`AppsScreenTest`, `LocationScreenTest`, `GeofencesScreenTest`).
**Modificado**: `src/debug/.../ScreensGallery.kt` (10 secciones nuevas).

`TutorShell.kt`, `SectionViews.kt`, `*State.kt`, `UsageText.kt`, `AppIcon.kt` **no** aparecen en `git status`.

### 2. Salidas reales (§10 y tests)

- `./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL** (46 s).
- `lintDebug`: **0 errores, 24 avisos** (base 25; hay **uno menos** porque al quitar el código antiguo de
  `LegacySections.kt` desapareció un `UseKtx`). Ninguno en `feature/tutor/{apps,location,geofences,sections,legacy}`.
- `./gradlew connectedDebugAndroidTest` (emulador `Pixel_8`): **68 tests, 0 fallos, 0 saltados**. De ellos, 18 son de
  este sprint y pasaron:
  - `AppsScreenTest` (6): búsqueda filtra; "Ninguna app coincide con «zzz»."; "Desinstalada"; ninguna fila clicable;
    "Actualizar"→onRefresh; vacío→"Sin apps sincronizadas".
  - `LocationScreenTest` (6): "Dentro de «Casa»"; sin "Dentro de" con null; sin "4.15"/"-73.6"; "Abrir en mapa"→onOpenMap;
    "No hay una aplicación de mapas…"; sin reporte→"Este dispositivo todavía no ha reportado su ubicación.".
  - `GeofencesScreenTest` (6): "Último evento: Entró"/"Salió"/"Sin actividad"; "Radio 150 m"; sin "Crear"/"Editar"/"Eliminar";
    vacío→"No hay geocercas configuradas".
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalDate\.now|currentTimeMillis|latitude|longitude" .../feature/tutor/{apps,location,geofences,sections/SectionDeviceHeader.kt}` → **sin coincidencias**.
- `grep -rnE "AppsList|LocationSection|GeofenceSection|formatUsageDuration" mobile/app/src` → **sin coincidencias**.
- Galería abierta y **15 capturas distintas** fuera del repo en `%TEMP%\sprint45-gallery\`; sin `FATAL` en logcat.

### 3. Decisiones tuyas fuera del encargo

- **`LocationScreen`** capitaliza el relativo con `replaceFirstChar { it.uppercase() }` ("Hace 12 min", "Ayer") — el
  encargo pide "la primera letra en mayúscula".
- **`Abrir en mapa`**: `onClick = { mapMissing = !onOpenMap() }` tal como pide §5.3.e; `mapMissing` con `rememberSaveable`.
- **Test `searchFiltersApps` ajustado**: al escribir "YouTube" en el buscador hay **dos** nodos con ese texto (el valor del
  campo y la fila de la app), así que el `assertExists` original fallaba con "found 2 nodes". Lo cambié por
  `Chrome`/`Instagram` `assertDoesNotExist()` + `onAllNodes(hasText("YouTube")).assertCountEquals(2)`. No toqué la pantalla,
  solo la aserción.
- **Píldora "Último evento: …"** de Geocercas usa el `showDot` por defecto (`true`), porque §5.4 no lo especifica; la píldora
  de estado del dispositivo sí usa `showDot = false` (decisión del S44). Lo dejo anotado por si Claude quiere unificarlo.

### 4. Pendientes

- **Revisión visual** de las 3 pantallas y la galería: para Claude (este modelo no acepta imágenes). Capturas en
  `%TEMP%\sprint45-gallery\`.

### 5. Preguntas para Claude

Ninguna.

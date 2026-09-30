# Sprint 46 — Tutor: Historial, Estadísticas y Alertas (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

Las pantallas Historial, Estadísticas y Alertas del tutor se ven como los mockups
`docs/android-redesign/mockups/08-tutor-historial.jpg`, `09-tutor-estadisticas.jpg` y `10-tutor-alertas.jpg`,
**con los textos y datos reales, no los del mockup** (§3). **Solo escribes presentación:** la carga, la agrupación, las
etiquetas, los filtros y las acciones (marcar leída y silenciar) ya están hechos y probados.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- `{pkg}/feature/tutor/TutorShell.kt` enruta las tres secciones y hace las peticiones. **No lo abras.**
- `{pkg}/feature/tutor/sections/ActivityViews.kt` (funciones puras, con tests):
  - `HistoryDay(date, title, subtitle, rows)` y `HistoryRow(id, kind: HistoryKind, title, subtitle, ruleType, time)`.
    `kind` es `Block`, `Enter` o `Exit`. `title` ya dice «Bloqueo de app · YouTube» o «Entró · Casa». `subtitle` es
    «Motivo: Límite diario» o `null`. `time` es «3:42 p. m.».
  - `StatsPeriod` (`Today`/`Week`/`Month`, cada uno con `.label` «Hoy»/«7 días»/«30 días»).
  - `StatisticsView(topApps, blocks, compliance)`:
    - `TopAppRow(packageName, name, duration, fraction)`;
    - `BlockReasonItem(ruleType, label, icon, tone, count)`;
    - `ComplianceRow(name, limit, fraction: Float?, days)`.
  - Constantes `COMPLIANCE_SUBTITLE` y `NO_USAGE_IN_PERIOD`.
  - `AlertFilter` (`All`/`Unread`/`Critical`/`Warnings`, cada uno con `.label`), `AlertItem(id, level, title, time,
    repeated, unread, silenced)` y `filterAlerts(items, filter)`.
- `{pkg}/feature/tutor/sections/SectionDeviceHeader.kt` (del S45): `SectionDeviceHeader(device, lines)`.
- `{pkg}/ui/format/RuleLabels.kt`: `ruleTypeIcon`/`ruleTypeTone` (ya vienen dentro de `BlockReasonItem`).
- `{pkg}/ui/components/TimelineItem.kt`: Claude ajustó la línea vertical a la altura de la fila; úsalo tal cual.
- Las tres pantallas existen con **firma final** y un cuerpo provisional. **Reemplaza los cuerpos; no cambies ninguna
  firma:**

```kotlin
fun HistoryScreen(device: DeviceSummary?, history: LoadState<List<HistoryDay>>, now: Instant,
                  onRefresh: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier)
fun StatisticsScreen(device: DeviceSummary?, period: StatsPeriod, statistics: LoadState<StatisticsView>, now: Instant,
                     onSelectPeriod: (StatsPeriod) -> Unit, onRefresh: () -> Unit, onBack: () -> Unit,
                     modifier: Modifier = Modifier)
fun AlertsScreen(device: DeviceSummary?, alerts: LoadState<List<AlertItem>>, filter: AlertFilter,
                 busyAlertId: String?, actionError: String?, silenceTarget: AlertItem?, now: Instant,
                 onSelectFilter: (AlertFilter) -> Unit, onMarkRead: (alertId: String) -> Unit,
                 onAskSilence: (alertId: String) -> Unit, onConfirmSilence: () -> Unit, onDismissSilence: () -> Unit,
                 onRefresh: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier)
```

- `device` puede ser `null`: entonces no se dibuja la cabecera.
- `busyAlertId != null` significa que se está enviando una acción. **Todos** los botones de acción de **todas** las
  alertas quedan deshabilitados (`enabled = false`), y el botón de esa alerta muestra `loading = true`.
- `silenceTarget != null` significa que se muestra el diálogo de §5.3.

## 3. Lo que NO se dibuja aunque el mockup lo muestre (obligatorio)

| Mockup | Qué muestra | Qué hacer |
|---|---|---|
| 08 | Descripción bajo cada geocerca («Se detectó la entrada a la geocerca…») | **Nada**: solo el título. |
| 09 | «Porcentaje del tiempo permitido usado en este periodo.» | **Falso.** Usa `COMPLIANCE_SUBTITLE` («Días dentro del límite en este periodo.»). |
| 09 | Porcentaje a la derecha de cada barra («78%») | **No.** A la derecha va `row.days` («5 de 7 días»). |
| 09 | Barras de cumplimiento verdes o naranjas según el valor | **Un solo tono**: `NpTone.Success` (decisión del dueño). |
| 09 | Tres tarjetas fijas de bloqueos | Las que vengan en `view.blocks` (0 a 7), sin inventar. |
| 10 | «Protección contra desinstalación desactivada», «Ubicación actualizada» | No existen: el texto es `item.title`, tal cual. |
| 10 | Descripción larga bajo el título | **Nada**: no hay dato. |
| 10 | Flecha «>» en cada alerta | **Sin flecha**: no hay detalle de alerta. |
| 10 | Nivel en inglés («CRITICAL», «HIGH») | `SeverityBadge(item.level)`, que ya da icono, texto en español y color. |
| todos | Miniatura de tablet | La cabecera del S45 (`SectionDeviceHeader`). |
| todos | «En tiempo real», «ahora», «en vivo» | Prohibido. |

## 4. Lo que NO debes tocar y archivos permitidos

No toques: `backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`, Gradle, manifiestos,
`TutorShell.kt`, `TutorRoute.kt`, `sections/ActivityViews.kt`, `sections/ActivityState.kt`, `sections/SectionViews.kt`,
`*State.kt`, `{pkg}/core/**`, `{pkg}/ui/**`, `{pkg}/feature/home/**` y `{pkg}/feature/supervised/**`. Tampoco las
pantallas del S44 y S45, ni `res/`.

No hagas `git commit` ni `push`. Si necesitas algo que esté fuera de esta lista, escribe `[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar cuerpos: `{pkg}/feature/tutor/history/HistoryScreen.kt`, `statistics/StatisticsScreen.kt` y
  `alerts/AlertsScreen.kt`.
- Modificar `{pkg}/feature/tutor/legacy/LegacySections.kt` **solo para quitar** código (§6).
- Crear `mobile/app/src/androidTest/java/com/netprotect/app/ui/{HistoryScreenTest,StatisticsScreenTest,AlertsScreenTest}.kt`.
- Modificar `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantallas

**Reglas comunes a las tres pantallas:**
- **Estructura** (como en el S45):
  - fondo `NpColors.SkyGround`, `statusBarsPadding()`, `verticalScroll`, relleno horizontal de 20 dp, **sin**
    `navigationBarsPadding()`;
  - `NpTopBar(onBack = onBack)`, título (`NpText.Display`, `ShieldNavy`), 12 dp, la cabecera si `device != null`,
    16 dp y el contenido.
- **Estados de carga:**
  - `Loading` → `LoadingState()`;
  - `Failed` → `ErrorState(message, onRetry = onRefresh)`.
- **Final:** 16 dp, el `InfoBanner` de la pantalla con `title = "Información importante"` y 24 dp.
- **Prohibido:**
  - `Color(0x…)`, tamaños de letra o radios fijos;
  - `Instant.now()`, `LocalDate.now()`;
  - leer `packageName` para mostrarlo: ya viene resuelto en `name`/`title`.
- **Cabecera:**
  - usa `SectionDeviceHeader(device, lines)`;
  - `{relativo}` = `RelativeTime.format(Instant.parse(device.lastSeenAt), now, ZoneId.systemDefault())`;
  - si `lastSeenAt` es `null` o no se puede leer, la línea es «Datos sincronizados periódicamente».

### 5.1 `HistoryScreen` (mockup 8)
1. **Título y cabecera:**
   - título **«Historial»**;
   - cabecera con una línea: **«Datos sincronizados periódicamente · última actualización {relativo}»**.
2. **Si hay datos** (`Loaded` con días): por cada `HistoryDay` una `NpCard` (12 dp entre tarjetas) que contiene:
   - **Encabezado del día:** una fila con `day.title` (`NpText.Title`, `ShieldNavy`) y, si `day.subtitle != null`,
     8 dp + `day.subtitle` (`Body`, `SlateMuted`).
   - Luego 12 dp.
   - **Una fila por evento:** un `TimelineItem` por cada `row` de `day.rows`, con `isLast` en el último del día y estos
     valores:
     - `icon`:
       - `Block` → `RuleLabels.ruleTypeIcon(row.ruleType ?: "BLOCK")`;
       - `Enter` → `NpIcons.LogIn`;
       - `Exit` → `NpIcons.LogOut`.
     - `tone`:
       - `Block` → `RuleLabels.ruleTypeTone(row.ruleType ?: "BLOCK")`;
       - `Enter` → `NpTone.Success`;
       - `Exit` → `NpTone.Warning`.
     - `title = row.title`, `subtitle = row.subtitle`, `time = row.time`.
3. **Si está vacío** (`Loaded` vacío): `EmptyState(NpIcons.History, "Sin eventos en el historial.",
   "Aquí aparecerán los bloqueos de apps y las entradas y salidas de geocercas.")`.
4. **Aviso final:** `InfoBanner` con texto **«El historial puede tener retrasos porque los eventos se registran cuando
   el dispositivo envía su siguiente reporte.»**

### 5.2 `StatisticsScreen` (mockup 9)
1. **Título, cabecera y selector:**
   - título **«Estadísticas»** y la cabecera de §5.1;
   - `SegmentedControl(options = StatsPeriod.entries.map { it.label }, selectedIndex = period.ordinal,
     onSelect = { onSelectPeriod(StatsPeriod.entries[it]) })`;
   - 16 dp.
   - El selector **siempre** está visible, también en `Loading` y `Failed`.
2. **Si hay datos** (`Loaded`), tres `NpCard` con 12 dp entre ellas. Cada tarjeta empieza con esta cabecera:
   `IconTile(icono, tono, 44.dp)` + 12 dp + una columna con el título (`Title`, `ShieldNavy`) y el subtítulo (`Body`,
   `SlateMuted`); después, 12 dp.
   - **a. «Apps más usadas»** (`NpIcons.LayoutGrid`, `Info`), subtítulo «Tiempo de uso en este periodo.»:
     - Por cada `TopAppRow`, una fila (10 dp entre filas) con:
       - `AppIcon(packageName, name, size = 32.dp)`;
       - 12 dp;
       - `name` (`Body`, `Ink`) con un ancho fijo de 96 dp y una línea con elipsis;
       - `UsageBar(fraction, Modifier.weight(1f), NpTone.Info, contentDescription = "$name: $duration")`;
       - 12 dp;
       - `duration` (`BodyStrong`, `Ink`).
     - Vacío → «Sin datos de uso.» (`Body`, `SlateMuted`).
   - **b. «Bloqueos»** (`NpIcons.Ban`, `Danger`), subtítulo «Total de bloqueos en este periodo, por motivo.»:
     - Rejilla de 3 columnas (filas de `Row` con `weight(1f)`, 8 dp de separación, huecos vacíos al final de la última
       fila).
     - Cada celda es un `Box` con `background(item.tone.wash, NpShapes.Md)` y relleno de 12 dp, que contiene:
       - icono `item.icon` de 20 dp, tinte `item.tone.text`;
       - `item.label` (`Caption`, `item.tone.text`);
       - `item.count` (`NpText.Title`, `ShieldNavy`).
     - Vacío → «Ninguno en este periodo.».
   - **c. «Cumplimiento de límites diarios»** (`NpIcons.CircleCheck`, `Success`), subtítulo **`COMPLIANCE_SUBTITLE`**:
     - Por cada `ComplianceRow`, una columna (12 dp entre ellas) con:
       - una fila: `name` (`BodyStrong`, `Ink`, peso 1) y `days` (`Body`, `SlateMuted`);
       - `limit` (`Caption` normal, `SlateMuted`);
       - 6 dp;
       - **solo si** `fraction != null`: `UsageBar(fraction, Modifier.fillMaxWidth(), NpTone.Success,
         contentDescription = "$name: $days")`.
     - Vacío → «Sin reglas de límite diario.».
3. **Aviso final:** `InfoBanner` con texto **«Los datos de uso se sincronizan periódicamente y pueden tener algunos
   minutos de retraso.»**

### 5.3 `AlertsScreen` (mockup 10)
1. **Título y cabecera:**
   - título **«Alertas»**;
   - cabecera con dos líneas: **«Última actualización: {relativo}»** (o «Sin comunicación registrada todavía.») y
     **«Las alertas pueden aparecer en el siguiente reporte.»**
2. **Filtros:** `FilterChips(options = AlertFilter.entries.map { it.label }, selectedIndex = filter.ordinal,
   onSelect = { onSelectFilter(AlertFilter.entries[it]) })` y 12 dp. Solo se muestran cuando el estado es `Loaded`.
3. **Error de acción:** si `actionError != null`, `InfoBanner(title = "No se pudo completar",
   text = actionError, tone = NpTone.Danger, icon = NpIcons.CircleAlert)` y 12 dp.
4. **Si hay datos** (`Loaded`): `val visible = filterAlerts(alerts.value, filter)`.
   - **Casos vacíos:**
     - `alerts.value` vacío → `EmptyState(NpIcons.Bell, "Sin alertas para este dispositivo.",
       "Aparecerán aquí cuando el dispositivo reporte algo que requiera tu atención.")`;
     - `visible` vacío pero hay alertas → «No hay alertas en este filtro.» (`Body`, `SlateMuted`).
   - **Tarjetas:** una `NpCard` por cada `item` de `visible`, con 12 dp entre tarjetas. Cada tarjeta lleva:
     - **Fila superior:** `SeverityBadge(item.level)`, un espacio con peso 1 y, **si** `item.unread`,
       `StatusPill("No leída", NpTone.Info)`.
     - 8 dp.
     - **Título:** `item.title` (`Title`, `ShieldNavy`).
     - 6 dp.
     - **Fila de hora:** icono `NpIcons.Clock` de 16 dp (`SlateMuted`) + 4 dp + `item.time` (`Caption` normal,
       `SlateMuted`). Si `item.repeated != null`: 12 dp + icono `NpIcons.RefreshCw` de 16 dp + 4 dp + `item.repeated`.
     - 12 dp.
     - **Fila de acciones** (8 dp entre elementos, cada uno con peso 1):
       - **Si** `item.unread`: `NpButton("Marcar como leída", { onMarkRead(item.id) }, variant = Secondary,
         icon = NpIcons.Check, small = true, enabled = busyAlertId == null,
         loading = busyAlertId == item.id && !item.silenced)`.
       - **Si** `item.silenced`: `StatusPill("Silenciada", NpTone.Neutral)` centrado en su celda. Si no:
         `NpButton("Silenciar", { onAskSilence(item.id) }, variant = Secondary, icon = NpIcons.BellOff, small = true,
         enabled = busyAlertId == null)`.
       - Si solo hay un elemento, ocupa todo el ancho.
     - **Sin** flecha y **sin** descripción larga.
5. **Aviso final:** `InfoBanner` con texto **«Las alertas se generan cuando el dispositivo envía su siguiente reporte y
   pueden tener un retraso de algunos minutos.»**
6. **Diálogo de silenciar:** si `silenceTarget != null`, mostrar:

   ```kotlin
   ConfirmDialog(
       title = "¿Silenciar «${silenceTarget.title}»?",
       message = "No volverás a recibir esta alerta de este dispositivo. Puedes quitar el silencio desde el panel web.",
       confirmLabel = "Silenciar",
       dismissLabel = "Cancelar",
       onConfirm = onConfirmSilence,
       onDismiss = onDismissSilence,
       danger = true,
   )
   ```

   El texto del mensaje es literal (decisión D-09).

## 6. Retirar lo antiguo de `LegacySections.kt`

1. **Quita** estas declaraciones: `HistoryState`, `StatisticsState`, `AlertsState`, `HistorySection`, `HistoryEventRow`,
   `STATISTICS_PERIODS`, `formatSeconds`, `StatisticsSection`, `alertLabel`, `AlertsSection` y la función entera
   `LegacyDeviceSectionScreen` (TutorShell ya no la usa).
2. **Conserva** solo `AuditState`, `AuditSection`, `formatCapturedAt` (si `AuditSection` la usa), `LegacyFrame` y
   `LegacyActivityScreen`, que se retiran en el S47.
3. Quita los imports que queden sin uso y actualiza el comentario de cabecera del archivo: ya solo contiene la
   actividad del tutor.
4. Compila.

## 7. Galería (solo debug)

En `ScreensGallery.kt` añade estos estados, con el mismo marco que el S45 (`now = Instant.parse("2026-09-29T20:00:00Z")`
y el mismo `DeviceSummary` «Tablet de Sofía»):
- **`HistoryScreen`:**
  - dos días («Hoy» con 4 eventos: dos bloqueos con motivos distintos, una entrada y una salida; «Ayer» con 3);
  - vacío;
  - error.
- **`StatisticsScreen`:**
  - «7 días» con 4 apps, 3 motivos de bloqueo y 2 reglas de cumplimiento (una con `fraction = null` y
    `days = NO_USAGE_IN_PERIOD`);
  - «Hoy» todo vacío;
  - error.
- **`AlertsScreen`:**
  - 5 alertas: una CRITICAL no leída repetida 3 veces, una HIGH no leída, una WARNING leída y silenciada, una INFO leída
    y otra no leída;
  - la misma lista con `busyAlertId` en la primera;
  - la misma lista con `silenceTarget` en la segunda (diálogo abierto);
  - la misma lista con `actionError = "Sin conexión con el servidor."`;
  - vacía;
  - error.

Construye los `HistoryDay`/`AlertItem`/`StatisticsView` directamente, con textos que parezcan reales (usa las mismas
etiquetas: «Bloqueo de app · YouTube», «Motivo: Límite diario», «Se bloqueó YouTube», «Hoy, 3:42 p. m.»…).

**Capturas:** la galería es larga y deslizar con `adb input swipe` salta de forma errática. Para las capturas, pon los
frames del S46 **al principio** de la lista (antes de los demás). Guarda las capturas fuera del repo, en
`%TEMP%\sprint46-gallery\`, y di en el informe qué frame sale en cada archivo.

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

- **`HistoryScreenTest`:**
  - con dos días se ven «Hoy», «29 de septiembre de 2026», «Bloqueo de app · YouTube» y «Motivo: Límite diario»;
  - vacío muestra «Sin eventos en el historial.»;
  - error y «Reintentar» llama a `onRefresh`.
- **`StatisticsScreenTest`:**
  - se ve «Días dentro del límite en este periodo.» y **no** existe ningún texto que contenga «Porcentaje» ni «%»;
  - se ve «5 de 7 días»;
  - tocar «30 días» llama a `onSelectPeriod(StatsPeriod.Month)`;
  - vacío muestra «Sin datos de uso.», «Ninguno en este periodo.» y «Sin reglas de límite diario.».
- **`AlertsScreenTest`:**
  - «Marcar como leída» llama a `onMarkRead(id)` y no aparece en una alerta leída;
  - «Silenciar» llama a `onAskSilence(id)`;
  - una alerta con `silenced = true` muestra «Silenciada» y no tiene botón «Silenciar»;
  - con `busyAlertId` no nulo, los botones están deshabilitados (`assertIsNotEnabled()`);
  - con `silenceTarget` se ve el mensaje de D-09 y «Silenciar» del diálogo llama a `onConfirmSilence`; «Cancelar»
    llama a `onDismissSilence`;
  - el filtro «Críticas» llama a `onSelectFilter(AlertFilter.Critical)`, y con `filter = Critical` una alerta WARNING
    no aparece;
  - con `actionError` se ve el mensaje;
  - vacío muestra «Sin alertas para este dispositivo.»;
  - ningún texto visible contiene «CRITICAL», «HIGH» ni «WARNING».

El emulador `Pixel_8` está encendido.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, los tres mockups y `docs/android-redesign/mockups/README.md` (secciones 8, 9 y 10).
   Lee también el código de §2 y `docs/delegated/done/sprint-45-apps-ubicacion.md` como ejemplo del resultado esperado.
   Mueve el encargo a `active/`.
2. Implementa cada pantalla y compila con `cd mobile && ./gradlew compileDebugKotlin` después de cada una.
3. Retira lo antiguo (§6) y compila.
4. Galería → `./gradlew assembleDebug`.
5. Tests → `./gradlew assembleDebugAndroidTest connectedDebugAndroidTest`.
6. Verificación (§10) e informe.

Si Gradle falla con `resource … not found` sin razón aparente, ejecuta `./gradlew clean` una vez.

## 10. Verificación

Pega la salida **real** de cada comprobación:
- **Build y lint:** `cd mobile && ./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` →
  `BUILD SUCCESSFUL`, lint con 0 errores. Indica los avisos nuevos con ruta y línea (la base son 24).
- **Tests del emulador:** `connectedDebugAndroidTest`, con el número de tests y el resultado.
- **Patrones prohibidos:**
  `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalDate\.now|currentTimeMillis|packageName|CRITICAL|WARNING|\"HIGH\"" mobile/app/src/main/java/com/netprotect/app/feature/tutor/{history,statistics,alerts}`
  → sin coincidencias. La única excepción es `AppIcon(row.packageName, …)` en Estadísticas: dila en el informe.
- **Código antiguo retirado:**
  `grep -rnE "HistorySection|StatisticsSection|AlertsSection|LegacyDeviceSectionScreen|formatSeconds" mobile/app/src`
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

---

## Informe de DeepSeek

### 1. Archivos creados y modificados

**Reemplazados los cuerpos (firmas intactas)**: `feature/tutor/history/HistoryScreen.kt`,
`feature/tutor/statistics/StatisticsScreen.kt`, `feature/tutor/alerts/AlertsScreen.kt`.
**Modificado** `feature/tutor/legacy/LegacySections.kt`: quité `HistoryState`, `StatisticsState`, `AlertsState`,
`HistorySection`, `HistoryEventRow`, `STATISTICS_PERIODS`, `formatSeconds`, `StatisticsSection`, `alertLabel`,
`AlertsSection` y la función entera `LegacyDeviceSectionScreen`; quedan solo `AuditState`, `AuditSection`,
`formatCapturedAt`, `LegacyFrame` y `LegacyActivityScreen` (se retiran en el S47). Actualicé el comentario de cabecera y
quité los imports sin uso.
**Modificado** `src/debug/.../ScreensGallery.kt`: 12 frames del S46 puestos **al principio** (§7).
**Creados** los 3 tests androidTest (`HistoryScreenTest`, `StatisticsScreenTest`, `AlertsScreenTest`).

`ActivityViews.kt`, `ActivityState.kt`, `TutorShell.kt`, `TutorRoute.kt` **no** aparecen en `git status`.

### 2. Salidas reales (§10 y tests)

- `./gradlew --console=plain test assembleDebug assembleDebugAndroidTest lintDebug` → **BUILD SUCCESSFUL**.
- `lintDebug`: **0 errores, 24 avisos** (base 24; **0 nuevos**). Ninguno en `feature/tutor/{history,statistics,alerts,legacy}`.
- `./gradlew connectedDebugAndroidTest` (emulador `Pixel_8`): **86 tests, 0 fallos, 0 saltados**. De ellos, 18 son de este
  sprint y pasaron:
  - `HistoryScreenTest` (3): "Hoy"/"29 de septiembre de 2026"/"Bloqueo de app · YouTube"/"Motivo: Límite diario"; vacío;
    error→"Reintentar"→onRefresh.
  - `StatisticsScreenTest` (4): "Días dentro del límite en este periodo." sin "Porcentaje" ni "%"; "5 de 7 días"; "30 días"→
    onSelectPeriod(Month); los tres vacíos.
  - `AlertsScreenTest` (11): "Marcar como leída"→onMarkRead y ausente en leída; "Silenciar"→onAskSilence; "Silenciada" sin
    botón; busy→deshabilitado; silenceTarget→mensaje D-09 + confirm/cancel; filtro "Críticas"; actionError; vacío; sin
    "CRITICAL"/"HIGH"/"WARNING".
- `grep -rnE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalDate\.now|currentTimeMillis|packageName|CRITICAL|WARNING|\"HIGH\"" .../{history,statistics,alerts}` (case-sensitive, como `grep -E`) → **una sola coincidencia**:
  `StatisticsScreen.kt:191 AppIcon(packageName = row.packageName, …)` — la excepción documentada del propio encargo.
- `grep -rnE "HistorySection|StatisticsSection|AlertsSection|LegacyDeviceSectionScreen|formatSeconds" mobile/app/src` → **sin coincidencias**.
- Capturas en `%TEMP%\sprint46-gallery\` (ver §4).

### 3. Decisiones / hallazgos fuera del encargo

- **Contradicción en la cabecera de Alertas** (la digo, no la elijo en silencio): la regla común de §5 dice que si
  `lastSeenAt` es null «la línea es «Datos sincronizados periódicamente»», pero §5.3.1 dice que el texto de Alertas es
  «Última actualización: {relativo}» con *fallback* «Sin comunicación registrada todavía.». Seguí **§5.3.1** (la regla
  específica de la pantalla).
- **El frame "AlertsScreen · silenceTarget" rompe la galería**: `ConfirmDialog` es un `Dialog` modal y, como la galería
  usa `Column(verticalScroll)` (composición **eager** de todos los frames), el diálogo se muestra desde el arranque y
  tapa toda la galería; además no se puede cerrar (en la galería `onDismissSilence = {}` y `silenceTarget` es fijo).
  Lo dejé tal como pide §7 («diálogo abierto») y lo anoto aquí: para revisar el resto de frames hay que hacer
  `adb shell am force-stop com.netprotect.app` o, si Claude prefiere una galería navegable, cambiar a `LazyColumn` o
  quitar este frame. El comportamiento del diálogo en sí está cubierto por `AlertsScreenTest.silenceTargetShowsDialogAndCallbacksWork`.
- **`now` del S46**: usé `Instant.parse("2026-09-29T20:00:00Z")` (como pide §7), distinto del `galleryNow` del S44/S45
  ("15:00:00Z").

### 4. Pendientes

- **Revisión visual** de las 3 pantallas: para Claude (este modelo no acepta imágenes). Por el problema del diálogo
  (§3), las capturas en `%TEMP%\sprint46-gallery\` muestran el diálogo de silenciar, no los frames: no pude capturar los
  frames porque el `Dialog` modal los tapa desde el arranque.

### 5. Preguntas para Claude

Ninguna.

# Sprint 47 — Tutor: Mi actividad (Android)

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Sigue `AGENTS.md`. Lee este archivo entero
> antes de tocar nada. Al empezar, muévelo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`
> y escribe tu `## Informe de DeepSeek` al final.

## 1. Objetivo

La pestaña **Actividad** del tutor se ve como el mockup `docs/android-redesign/mockups/11-tutor-mi-actividad.jpg`, pero
**sin los detalles que el mockup inventa** (§3). Es el registro de auditoría del propio tutor y es de solo lectura.
**Solo escribes presentación:** la carga, la paginación, las etiquetas, los iconos, los colores y la agrupación por día
ya están hechos y probados.

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## 2. Lo que Claude ya dejó hecho (no lo repitas ni lo cambies)

- El backend ya audita cada consulta de ubicación (`LOCATION_VIEWED`) y la etiqueta es «Ubicación consultada».
- `{pkg}/feature/tutor/activity/MyActivityState.kt` contiene:
  - `ActivityDay(date, title, subtitle, rows)`: `title` es «Hoy», «Ayer» o una fecha; `subtitle` es la fecha completa o
    `null`.
  - `ActivityRow(id, icon, tone, title, subtitle, time)`: `title` es la etiqueta en español; `subtitle` es el recurso
    («Tablet de Sofía», «Dispositivo», «Alerta»…) o `null`; `time` es «3:42 p. m.».
- `TutorShell.kt` ya carga los datos y pagina. **No lo abras.**
- `LegacySections.kt` ya no existe.
- `MyActivityScreen` existe con **firma final** y un cuerpo provisional. **Reemplaza el cuerpo; no cambies la firma:**

```kotlin
fun MyActivityScreen(activity: LoadState<List<ActivityDay>>, hasMore: Boolean, loadingMore: Boolean,
                     moreError: String?, onLoadMore: () -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier)
```

Es la raíz de una pestaña: **no lleva botón de volver**. La barra inferior la dibuja el shell.

## 3. Lo que NO se dibuja aunque el mockup lo muestre (obligatorio)

| Mockup | Qué muestra | Qué hacer |
|---|---|---|
| 11 | «Se estableció un límite diario de 2 h para YouTube», «de 10:00 p. m. a 6:00 a. m.», «(Villavicencio, Meta)», «Se cambió el nombre de … a …» | **Nada de eso**: la auditoría no guarda detalles. El subtítulo es `row.subtitle`, tal cual (o nada si es `null`). |
| 11 | «Aplicación bloqueada», «Configuración de alertas» | No son acciones del tutor: el texto es siempre `row.title`. |
| 11 | «…acciones que has realizado en NetProtect.» (implica solo la app) | Texto corregido de §5, que menciona app **y** panel web. |
| 11 | «Este registro muestra solo las acciones que tú realizas en la app.» | Texto corregido de §5. |
| 11 | Flecha de volver arriba | Sin flecha: es una pestaña. |
| todos | Botones de exportar, borrar, editar o filtrar | Prohibido: solo lectura. |

## 4. Lo que NO debes tocar y archivos permitidos

No toques:
- `backend/`, `frontend/`, `infra/`, `.claude/`, `AGENTS.md`, `opencode.json`, `CLAUDE.md`, Gradle ni manifiestos;
- `TutorShell.kt`, `TutorRoute.kt`, `MyActivityState.kt`, `*State.kt`, `{pkg}/core/**` ni `{pkg}/ui/**`;
- las pantallas de sprints anteriores, ni `res/`.

No hagas `git commit` ni `push`. Si necesitas algo que esté fuera de esta lista, escribe `[PREGUNTA PARA CLAUDE]`.

**Permitidos (lista cerrada):**
- Reemplazar el cuerpo de `{pkg}/feature/tutor/activity/MyActivityScreen.kt`. Puedes añadir en ese mismo archivo
  composables `private`.
- Crear `mobile/app/src/androidTest/java/com/netprotect/app/ui/MyActivityScreenTest.kt`.
- Modificar `mobile/app/src/debug/java/com/netprotect/app/debug/ScreensGallery.kt` (§7).

## 5. Pantalla

Estructura, igual que `MoreScreen`:
- `Box` con `fillMaxSize()` y fondo `NpColors.SkyGround`; dentro, una `Column` con `statusBarsPadding()`,
  `verticalScroll` y relleno horizontal de 20 dp, **sin** `navigationBarsPadding()`;
- arriba, `BrandHeader(subtitle = "Panel del tutor")`.

Contenido, en orden:
1. 24 dp.
2. **«Mi actividad»** (`NpText.Display`, `ShieldNavy`).
3. 8 dp.
4. **«Registro de las acciones que has realizado en NetProtect (app y panel web). Esta información es de solo lectura.»**
   (`Body`, `SlateMuted`).
5. 16 dp.
6. Según `activity`:
   - **`Loading`:** `LoadingState()`.
   - **`Failed`:** `ErrorState(message, onRetry = onRefresh)`.
   - **`Loaded` vacío:** `EmptyState(NpIcons.History, "Sin acciones registradas todavía.", "Aquí aparecerán las acciones que
     realices desde la app o el panel web.")`.
   - **`Loaded` con días:** una `NpCard` por `ActivityDay`, con 12 dp entre tarjetas. Cada tarjeta lleva:
     - **Encabezado:** una fila con `day.title` (`NpText.Title`, `ShieldNavy`) y, si `day.subtitle != null`, 8 dp +
       `day.subtitle` (`Body`, `SlateMuted`).
     - 12 dp.
     - **Una fila por `row`** (`verticalAlignment = CenterVertically`, 14 dp entre filas):
       - `row.time` (`Caption` normal, `SlateMuted`) con un ancho fijo de 72 dp;
       - 8 dp;
       - `IconTile(row.icon, row.tone, 40.dp)`;
       - 12 dp;
       - una columna con peso 1: `row.title` (`BodyStrong`, `ShieldNavy`) y, **solo si** `row.subtitle != null`,
         `row.subtitle` (`Body`, `SlateMuted`).
     - **Sin** chevron y **sin** acción de clic.
   - **Después de las tarjetas:**
     - 12 dp;
     - si `moreError != null`: `moreError` (`Body`, `DangerText`) y 8 dp;
     - **solo si** `hasMore`: `NpButton("Cargar más", onLoadMore, Modifier.fillMaxWidth(), variant = Secondary,
       loading = loadingMore, enabled = !loadingMore)`.
7. 16 dp.
8. `InfoBanner(title = "Información importante", text = "Este registro no se puede editar ni borrar. No incluye la
   actividad del dispositivo supervisado.")`. Aparece siempre, en cualquier estado.
9. 24 dp.

Prohibido:
- `Color(0x…)`, tamaños de letra o radios fijos;
- `Instant.now()`, `LocalDate.now()`;
- formatear horas, fechas o etiquetas: todo viene ya hecho.

## 6. Nada que retirar

`LegacySections.kt` ya lo borró Claude.

## 7. Galería (solo debug)

En `ScreensGallery.kt`:
- Pon los frames del S47 **al principio** de la lista (antes de los del S46).
- Construye los `ActivityDay`/`ActivityRow` a mano, usando `AuditLabels.family(...)` para el icono y el tono, y textos
  reales: «Ubicación consultada» · «Tablet de Sofía», «Alerta silenciada» · «Alerta», «Renovación de sesión» · sin
  recurso…

Estados (**sin ningún diálogo**):
- dos días («Hoy» con 5 filas, «Ayer» con 4) con `hasMore = true`;
- el mismo con `loadingMore = true`;
- el mismo con `moreError = "Sin conexión con el servidor."`;
- vacío;
- error.

Guarda las capturas fuera del repo, en `%TEMP%\sprint47-gallery\`, y di en el informe qué frame sale en cada archivo.

## 8. Tests (androidTest; toca siempre con `.performScrollTo().performClick()`)

`MyActivityScreenTest`:
- con dos días se ven «Hoy», «Ubicación consultada» y «Tablet de Sofía»;
- una fila con `subtitle = null` no deja ningún texto vacío ni «null» (no existe ningún nodo con el texto `"null"`);
- con `hasMore = true`, «Cargar más» llama a `onLoadMore`; con `hasMore = false` no existe;
- con `loadingMore = true`, «Cargar más» no está habilitado;
- con `moreError` se ve el mensaje;
- vacío muestra «Sin acciones registradas todavía.»;
- error y «Reintentar» llama a `onRefresh`;
- no existe ningún texto «Exportar», «Borrar», «Eliminar» ni «Editar»;
- se ve «(app y panel web)» y **no** se ve «solo las acciones que tú realizas en la app».

El emulador `Pixel_8` está encendido.

## 9. Pasos

1. Lee `AGENTS.md`, este encargo, el mockup 11 y `docs/android-redesign/mockups/README.md` (sección 11). Lee también
   `MyActivityState.kt`, `MoreScreen.kt` (como modelo de raíz de pestaña) y
   `docs/delegated/done/sprint-46-historial-alertas.md` como ejemplo del resultado esperado. Mueve el encargo a
   `active/`.
2. Pantalla, y compila con `cd mobile && ./gradlew compileDebugKotlin`.
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
  `grep -nE "Color\(0x|fontSize\s*=|RoundedCornerShape\(|Instant\.now|LocalDate\.now|currentTimeMillis|resourceId|resourceType|auditActionLabel|Clock\.format" mobile/app/src/main/java/com/netprotect/app/feature/tutor/activity/MyActivityScreen.kt`
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

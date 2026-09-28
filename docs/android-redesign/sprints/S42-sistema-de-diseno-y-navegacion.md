# Sprint 42 — Sistema de diseño Android y esqueleto de navegación

**Prioridad: CRITICAL** — las 17 pantallas dependen de él. **Dueño:** Claude (navegación, dependencias
de test) + DeepSeek (tema, recursos, componentes, formateadores). **Rama:** `sprint-42-design-system`.
**Mockups:** todos (como referencia de estilo). **Decisiones:** D-01, D-03, D-04, D-13.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Al terminar existe, sin cambiar todavía ninguna pantalla: `NetProtectTheme` con los tokens de
`DESIGN.md`, Inter, iconos Lucide, logo e icono de lanzador; los componentes compartidos con sus
`@Preview`; formateadores y etiquetas en español con tests; el esqueleto de navegación; y una galería
de componentes en builds *debug* para capturas. La app se ve igual que antes salvo el icono y las
barras del sistema.

## Problema que resuelve

Hoy cada composable escribe colores a mano (tema oscuro). Sin una base común, 17 pantallas hechas
en 8 sprints por dos agentes divergirían.

## Dependencias

S41. Decisiones D-01, D-03, D-04, D-13 resueltas.

## Alcance

**Claude (antes de delegar):**
1. Esqueleto de navegación según D-01 en `{pkg}/ui/navigation/` (pila de rutas guardable + manejo de
   "atrás"), con un test JVM de la pila. Aún sin rutas concretas.
2. Si D-13 = a: `androidx.compose.ui:ui-test-junit4` (androidTest) y `ui-test-manifest` (debug) en
   `libs.versions.toml` y `app/build.gradle.kts` — **commit propio**.
3. **En la revisión corta (C1), cuando `NetProtectTheme` ya exista:** envolver `MainActivity` en él y
   poner barras del sistema claras. Las pantallas aún oscuras quedan con barras claras
   temporalmente: aceptado en la rama de integración, no en `main`.

**Encargo 1 para DeepSeek — fundamentos:**
- `ui/theme/`: `Color.kt` (todos los colores de `DESIGN.md`, con sus nombres), `Type.kt` (escala de
  `DESIGN.md` → `Typography` de Material 3 con Inter), `Shape.kt` (12 y 16 dp), `Theme.kt`
  (`NetProtectTheme`, solo claro), `Elevation`/sombra suave.
- `res/font/`: Inter 400/500/600/700 (D-04) + licencia OFL en `mobile/third_party_licenses/`.
- `res/drawable/ic_*.xml`: iconos Lucide (D-03), licencia ISC junto a la de Inter. Lista cerrada:
  `arrow-left, arrow-right, chevron-right, chevron-down, refresh-cw, link, qr-code, smartphone,
  users, user, clock, file-text, info, log-in, log-out, arrow-left-right, pencil, trash-2,
  layout-grid, map-pin, map, external-link, history, bar-chart-3, triangle-alert, octagon-alert,
  siren, bell, bell-off, book-open, check, circle-check, circle-alert, ban, home, graduation-cap,
  building-2, search, calendar, moon, shield, shield-check, key-round, eye, eye-off, monitor, wifi,
  settings, lock, x, locate-fixed, more-horizontal`.
- Logo: `frontend/public/brand/logo-shield.png` y `logo-full.png` → `res/drawable-nodpi/`. La "G" de
  Google: portar el SVG en línea de `frontend/src/components/login/GoogleButton.tsx` a `VectorDrawable`.
- Icono de lanzador adaptativo (`mipmap-anydpi-v26` + primer plano por densidad desde
  `logo-shield.png`, fondo `#F5F9FF`) y `android:icon`/`android:roundIcon` en el manifest.
- `ui/format/`: `RelativeTime` ("hace un momento", "hace 12 min", "hace 3 h", "ayer", fecha),
  `Durations` ("1 h 24 min", "48 min", "menos de 1 min"), `DayGrouping` ("Hoy", "Ayer",
  "26 de septiembre de 2026"), hora "3:42 p. m." (es-CO). Todo recibe `now`/`ZoneId` por parámetro.
- `ui/format/*Labels.kt`: etiquetas copiadas **literalmente** del panel web —
  `frontend/src/lib/alertFormatting.ts` (niveles y tipos), `ruleFormatting.ts` (motivos de bloqueo),
  `categoryFormatting.ts` (categorías), el mapa de acciones de `frontend/src/components/AuditPanel.tsx`,
  y estados de dispositivo (`ONLINE`, `OFFLINE`, `ALERT`, `RESTRICTED`, `UNLINKED`).
- Tests JVM de formateadores y de que **todo valor de cada enum real tiene etiqueta**.

**Encargo 2 para DeepSeek — componentes y galería** (tras una revisión corta de Claude del 1):
- `ui/components/`: `NpCard`, `NpButton` (primario, secundario, texto, peligro; icono; cargando;
  deshabilitado; alto ≥ 48 dp), `IconTile`, `ListRow`, `StatusPill`, `SeverityBadge` (icono + texto +
  color, mismo mapa que la web), `InfoBanner`, `SectionHeader`, `EmptyState`, `LoadingState`,
  `ErrorState` (mensaje + "Reintentar"), `BrandHeader` (logo + "Net"/"Protect" + subtítulo opcional),
  `NpTopBar` (atrás + marca), `NpBottomBar`, `SegmentedControl`, `FilterChips`, `UsageBar`,
  `TimelineItem` + `DayHeader`, `CodeDisplay` (6 dígitos + cuenta atrás), `OtpInput` (6 casillas,
  solo dígitos, pegar), `ConfirmDialog`, `PermissionCard`. Cada uno con `@Preview`.
- `mobile/app/src/debug/.../debug/DesignGalleryActivity.kt` + `src/debug/AndroidManifest.xml`: una
  pantalla que muestra todos los componentes y sus estados (para capturas con
  `adb shell am start -n com.netprotect.app/.debug.DesignGalleryActivity`).
- Si D-13 = a: un test de UI de la galería (se renderiza sin errores).

## Fuera de alcance

Rediseñar cualquier pantalla existente; tocar lógica, clientes HTTP, servicios o `core/`.

## Trabajo por capa

Backend, web y base de datos: ninguno. Android: lo anterior.

## Seguridad

La galería **no** debe existir en release: Claude comprueba el manifiesto fusionado de release.
Para lanzarla con `adb` tiene que estar exportada, lo que choca con la invariante 11 de
`security-reviewer` ("ningún componente exportado nuevo"): se acepta **solo** como excepción
documentada en `docs/sprint-42.md` (solo `src/debug`, sin permisos, ausente en release). Si el dueño no
la acepta, alternativa sin componente exportado: un test de UI que renderiza la galería y guarda las
capturas con `captureToImage()` para bajarlas con `adb pull`.
Recursos de terceros con licencia incluida. Ninguna fuente o icono descargado de un sitio no oficial
(Inter: `github.com/rsms/inter`; Lucide: `github.com/lucide-icons/lucide` o `lucide.dev`).

## Testing

JVM: formateadores con fechas fijas, etiquetas completas, pila de navegación. UI (si D-13 = a):
galería. `./gradlew test assembleDebug lintDebug`. Capturas de la galería.

## Criterios de aceptación

- [ ] `NetProtectTheme` usa exactamente los hex de `DESIGN.md` (comparación token a token).
- [ ] Cero `Color(0x…)` fuera de `ui/theme` en archivos nuevos.
- [ ] Cada componente con `@Preview` y visible en la galería; capturas revisadas por Claude.
- [ ] Todos los enums reales (alertas, motivos, categorías, acciones de auditoría, estados) con etiqueta; test verde.
- [ ] Icono de lanzador visible en el teléfono.
- [ ] La galería no aparece en el manifiesto de release.
- [ ] Las pantallas actuales siguen funcionando igual (humo manual: login, tutor, supervisado).

## Definition of Done

La común del HANDOFF.

## Riesgos

Tamaño del encargo (por eso son dos). Inter sin `fontFeatureSettings "tnum"` → cifras que "bailan"
en la cuenta atrás (usar `tnum` en el estilo de números). Iconos convertidos con trazo relleno por
error (revisar en la galería).

## Decisiones técnicas

Colores de nivel de alerta: los del panel web (`alertFormatting.ts`), no los del mockup, para que
web y móvil digan lo mismo. Sombras suaves: `shadowElevation` de 1–2 dp o sombra dibujada; lo decide
Claude al revisar la galería.

---

## PROMPT A — Claude Code (explorar, preparar y escribir los encargos)

```
Sprint 42 del rediseño Android de NetProtect: sistema de diseño y esqueleto de navegación.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S42-sistema-de-diseno-y-navegacion.md), DESIGN.md y las decisiones
D-01, D-03, D-04, D-13 de docs/android-redesign/DECISIONES.md. Mira 3 mockups para el estilo:
docs/android-redesign/mockups/03-tutor-inicio.jpg, 10-tutor-alertas.jpg, 14-supervisado-permisos.jpg.

Fases 1–3 del HANDOFF; espera mi OK al plan. Luego:
1. Implementa tu parte: navegación según D-01 en ui/navigation con su test; dependencias de test si
   D-13 = a (commit propio). MainActivity se toca en C1, cuando exista NetProtectTheme.
2. Escribe DOS encargos con el formato de docs/delegated/done/sprint-39-login-reemplazo.md:
   docs/delegated/pending/sprint-42a-fundamentos.md y sprint-42b-componentes.md, con el contenido
   de "Encargo 1" y "Encargo 2" del sprint, rutas completas, lista cerrada de archivos permitidos,
   textos y valores literales (los hex de DESIGN.md copiados en el encargo), pasos con
   compileDebugKotlin y la verificación final.
3. verifier sobre tu parte. Propón el commit docs(sprint-42) + feat(sprint-42) de tu parte.
Dime cuándo puedo abrir OpenCode para el encargo 42a.
```

## PROMPT B — OpenCode con DeepSeek V4 Pro (dos veces)

```
Ejecuta el encargo docs/delegated/pending/sprint-42a-fundamentos.md siguiendo AGENTS.md.
```

Tras la revisión corta de Claude (PROMPT C1), en una sesión nueva de OpenCode:

```
Ejecuta el encargo docs/delegated/pending/sprint-42b-componentes.md siguiendo AGENTS.md.
```

## PROMPT C1 — Claude Code (revisión corta entre encargos)

```
DeepSeek terminó docs/delegated/done/sprint-42a-fundamentos.md. Lee su informe. Propón el commit
"feat(sprint-42): fundamentos del sistema de diseño (DeepSeek, sin revisar)" con lo que hizo tal cual
y espera mi OK. Después envuelve MainActivity en NetProtectTheme con barras claras, y revisa solo: hex de Color.kt contra DESIGN.md, Type.kt contra DESIGN.md,
licencias, iconos (trazo, no relleno), etiquetas contra los archivos web, tests de formateadores.
verifier: ./gradlew test assembleDebug lintDebug. Corrige lo necesario (commit fix aparte) y, si el
encargo 42b necesita ajustes por lo aprendido, edítalo antes de que DeepSeek lo ejecute.
```

## PROMPT C2 — Claude Code (revisión final y cierre)

```
DeepSeek terminó docs/delegated/done/sprint-42b-componentes.md. Lee su informe. Commit de lo suyo
tal cual (con mi OK). Luego, fases 6–10 del HANDOFF:
- Revisa el diff contra el sprint S42 y AGENTS.md (archivos fuera de la lista, colores literales,
  dependencias nuevas, componentes sin @Preview).
- verifier: test assembleDebug lintDebug (+ connectedDebugAndroidTest si D-13 = a).
- Instala en el emulador, abre la galería con adb, toma capturas en %TEMP%\np-sprint42\ y revísalas
  tú contra 03-tutor-inicio.jpg, 10-tutor-alertas.jpg y 14-supervisado-permisos.jpg: lista las
  diferencias de estilo y corrígelas.
- Comprueba que DesignGalleryActivity no está en el manifiesto fusionado de release.
- Humo manual de las pantallas actuales (siguen funcionando).
- docs, /cerrar-sprint 42, informe del §9 (incluye qué errores cometió DeepSeek, para afinar los
  siguientes encargos). Commits separados, sin commit sin mi OK.
```

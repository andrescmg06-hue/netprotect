# Sprint 72 — Evidencia de borrados (cero usos)

Cada símbolo o archivo se volvió a comprobar con grep antes de borrarlo. Rutas relativas a la raíz del repo.
La verificación de compilación y pruebas (Gradle, `next build`) la hace T5, no este documento.

## T1 — Android

### Constantes de `NpIcons` y sus drawables

Para cada constante `<N>` y su drawable `ic_<d>` (o `logo_full`):

```
grep -rnE "NpIcons\.<N>\b" mobile/app/src --include=*.kt        # excluyendo ui/icons/NpIcons.kt  -> 0 coincidencias
grep -rnE "(R\.drawable\.|@drawable/|drawable/)ic_<d>\b" mobile  # excluyendo NpIcons.kt y /build/ -> 0 coincidencias
grep -rnE "getIdentifier|with\(NpIcons|NpIcons\.\*|NpIcons::|ui\.icons\.\*" mobile --include=*.kt --include=*.xml  # -> 0 (no hay acceso dinámico ni comodines)
```

Resultado: cero usos en `main`, `test`, `androidTest`, `debug`, manifiesto y XML de `res`. Borradas las 26
constantes y sus recursos:

| Constante | Drawable borrado |
|---|---|
| ChevronDown | ic_chevron_down.xml |
| Link | ic_link.xml |
| QrCode | ic_qr_code.xml |
| Users | ic_users.xml |
| FileText | ic_file_text.xml |
| LogIn | ic_log_in.xml |
| ArrowLeftRight | ic_arrow_left_right.xml |
| Pencil | ic_pencil.xml |
| Trash2 | ic_trash_2.xml |
| Map | ic_map.xml |
| History | ic_history.xml |
| BarChart3 | ic_bar_chart_3.xml |
| OctagonAlert | ic_octagon_alert.xml |
| Siren | ic_siren.xml |
| BellOff | ic_bell_off.xml |
| BookOpen | ic_book_open.xml |
| CircleCheck | ic_circle_check.xml |
| Building2 | ic_building_2.xml |
| Search | ic_search.xml |
| KeyRound | ic_key_round.xml |
| EyeOff | ic_eye_off.xml |
| Settings | ic_settings.xml |
| X | ic_x.xml |
| LocateFixed | ic_locate_fixed.xml |
| MoreHorizontal | ic_more_horizontal.xml |
| LogoFull | drawable-nodpi/logo_full.png |

`grep -rn logo_full mobile .` solo encontró la constante y una fila de `mobile/third_party_licenses/README.md`
(documento, actualizado) además de historial en `docs/delegated/done/` (no se toca).

### Símbolos de Kotlin

```
grep -rn "\bSLOW_MS\b|\bStaggerMs\b|\bEnterOffset\b" mobile --include=*.kt --include=*.xml --include=*.kts  -> solo la definición en ui/theme/Motion.kt
grep -rn "\bisSeen\b" mobile ...             -> solo la definición en core/auth/WelcomeStore.kt
grep -rn "\bisPreciseGranted\b" mobile ...   -> solo la definición en core/permissions/LocationPermission.kt
```

Borrados: `NpMotion.SLOW_MS`, `NpMotion.StaggerMs`, `NpMotion.EnterOffset` (y el import huérfano
`androidx.compose.ui.unit.dp`), `WelcomeStore.isSeen()`, `LocationPermission.isPreciseGranted()`.

### Sin borrar, solo texto

- Comentario de `RuleEnforcementClient.kt` (KDoc de la clase) ajustado: la app es solo del supervisado.
- `mobile/README.md`: ya no dice que soportará Tutor y Supervisado.

### Decidido NO borrar

Ninguno de los candidatos de T1 tenía uso; no hubo excepciones.

## T2 — Web

```
grep -rnE "ChartCard|card-twilight" frontend --exclude-dir=node_modules --exclude-dir=.next --exclude=tsconfig.tsbuildinfo -I
  -> solo ui/ChartCard.tsx (su propia definición/import del CSS) y la línea del barrel ui/index.ts
grep -rnE "card-twilight|ChartCard" frontend/next.config* frontend/e2e frontend/src (css, ts, tsx, mjs, js)
  -> solo el barrel ui/index.ts (aparte de ChartCard.tsx)
git ls-files | xargs grep -lIE "ChartCard|card-twilight"
  -> fuera de frontend/ solo documentos históricos (sprint-37, sprint-60, plan-frontend, fase-0-informe, tasks)
```

Borrados: `frontend/src/components/ui/ChartCard.tsx`, `ChartCard.module.css`, su línea en `ui/index.ts`,
`frontend/public/brand/card-twilight.jpg`. `docs/tasks.md` W-04 pasa a done.
`npx tsc --noEmit` en `frontend/`: sin errores.

### Decidido NO borrar

`DonutChart` y `MetricCard` (vitrina del sistema de diseño), según las decisiones del sprint.

## T3 — Repo

```
git ls-files | xargs grep -nIE "scripts/check|check\.(sh|ps1)"
  -> 0 referencias (Makefile, README, .github, .claude, docs); las únicas coincidencias fueron
     "shouldRecheck" en Kotlin (falso positivo)
git ls-files | xargs grep -nIE "android-redesign/opencode"
  -> solo docs/android-redesign/sprints/S40-linea-base-y-decisiones.md (líneas 33-34 y 115)
diff docs/android-redesign/opencode/AGENTS.md AGENTS.md ; diff .../opencode.json opencode.json
  -> ambos difieren: la copia de la raíz es la vigente (añade "default_agent": "implementer" y el permiso de adb);
     la carpeta es una versión anterior de lo que S40 pidió instalar en la raíz
```

Borrados: `scripts/check.sh`, `scripts/check.ps1`, `docs/android-redesign/opencode/` (AGENTS.md, opencode.json).
Se quedan `scripts/verify_sprint1.py` y `scripts/verify_turn.sh` (fuera del alcance).

### Decidido NO tocar

`docs/android-redesign/sprints/S40-linea-base-y-decisiones.md` cita la carpeta borrada, pero describe la
instalación hecha en su momento (historia del sprint); no se reescribe. Los archivos vigentes están en la raíz.

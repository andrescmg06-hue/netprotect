# Sprint 27 — Evidencia

Este sprint no cambia código de aplicación; la "evidencia" aquí es la verificación real de que cada
afirmación de `docs/manuals/` es consistente con el código y la configuración actuales, no sólo con
lo que otro documento dice de memoria — y que el commit no rompió nada.

## 1. Verificación de que los archivos citados existen de verdad

```
$ for f in docs/diagrams/01-componentes.md docs/diagrams/02-despliegue.md \
    docs/diagrams/05-modelo-datos-conceptual.md docs/diagrams/06-modelo-datos-fisico-sprint2.md \
    docs/adr/0001-monorepo-modular-monolith.md docs/adr/0002-stack.md \
    backend/app/core/metrics.py backend/app/services/tamper.py backend/app/services/alerts.py \
    backend/scripts/seed_test_session.py frontend/e2e/dashboard.spec.ts perf/load-test.js \
    infra/backup/backup.sh infra/backup/restore.sh infra/caddy/Caddyfile secrets/README.md \
    mobile/app/src/androidTest infra/monitoring/alert_rules.yml \
    infra/monitoring/grafana/provisioning/dashboards/json/netprotect-overview.json; do
    [ -e "$f" ] && echo "OK  $f" || echo "MISSING  $f"
  done
OK  docs/diagrams/01-componentes.md
OK  docs/diagrams/02-despliegue.md
OK  docs/diagrams/05-modelo-datos-conceptual.md
OK  docs/diagrams/06-modelo-datos-fisico-sprint2.md
OK  docs/adr/0001-monorepo-modular-monolith.md
OK  docs/adr/0002-stack.md
OK  backend/app/core/metrics.py
OK  backend/app/services/tamper.py
OK  backend/app/services/alerts.py
OK  backend/scripts/seed_test_session.py
OK  frontend/e2e/dashboard.spec.ts
OK  perf/load-test.js
OK  infra/backup/backup.sh
OK  infra/backup/restore.sh
OK  infra/caddy/Caddyfile
OK  secrets/README.md
OK  mobile/app/src/androidTest
OK  infra/monitoring/alert_rules.yml
OK  infra/monitoring/grafana/provisioning/dashboards/json/netprotect-overview.json
```

19/19. Ningún manual referencia un archivo que no existe en el árbol de trabajo.

## 2. Verificación de valores citados contra el código real, no contra otro documento

Retención de datos (`docs/manuals/politica-privacidad.md` §3, `docs/manuals/analisis-riesgos.md`):

```
$ grep -n "retention_days" backend/app/core/config.py
79:    location_retention_days: int = 7
94:    app_rule_event_retention_days: int = 90
95:    geofence_event_retention_days: int = 90
101:    alert_retention_days: int = 90
```

Coincide con lo escrito: ubicación 7 días, eventos de reglas/geofencing/alertas 90 días.

Enums citados en `docs/manuals/modelo-seguridad.md`/`documento-tecnico.md`:

```
$ grep -n "INFO\|WARNING\|HIGH\|CRITICAL" backend/app/models/alert.py | head -4
INFO = "INFO"
WARNING = "WARNING"
HIGH = "HIGH"
CRITICAL = "CRITICAL"

$ grep -n "ONLINE\|OFFLINE\|SYNCING\|ALERT\|RESTRICTED\|UNLINKED" backend/app/models/device.py | head -6
ONLINE = "ONLINE"
OFFLINE = "OFFLINE"
SYNCING = "SYNCING"
ALERT = "ALERT"
RESTRICTED = "RESTRICTED"
UNLINKED = "UNLINKED"

$ grep -n "ALLOW\|BLOCK\|DAILY_LIMIT\|WEEKLY_LIMIT\|SCHEDULE" backend/app/models/rule.py | head -5
ALLOW = "ALLOW"
BLOCK = "BLOCK"
DAILY_LIMIT = "DAILY_LIMIT"
WEEKLY_LIMIT = "WEEKLY_LIMIT"
SCHEDULE = "SCHEDULE"
```

Las 11 categorías (`docs/manuals/manual-usuario.md` §2.2):

```
$ grep -n "SOCIAL_MEDIA\|GAMES\|STREAMING\|EDUCATION\|PRODUCTIVITY\|COMMUNICATION\|NEWS\|SHOPPING\|FINANCE\|UTILITIES\|ADULT_CONTENT" backend/app/models/category.py
SOCIAL_MEDIA, GAMES, STREAMING, EDUCATION, PRODUCTIVITY, COMMUNICATION, NEWS, SHOPPING, FINANCE,
UTILITIES, ADULT_CONTENT  # 11 valores, confirmado
```

Los ocho secretos y sus nombres exactos (`docs/manuals/manual-administrador.md` §3,
`politica-privacidad.md` §2):

```
$ grep -n "JWT_SECRET\|PAIRING_CODE_PEPPER\|LOCATION_ENCRYPTION_KEY" backend/app/core/config.py
189:                ("JWT_SECRET", "jwt_secret"),
190:                ("PAIRING_CODE_PEPPER", "pairing_code_pepper"),
191:                ("LOCATION_ENCRYPTION_KEY", "location_encryption_key"),
```

Coincide con `secrets/README.md` (`jwt_secret.txt`, `pairing_code_pepper.txt`,
`location_encryption_key.txt`, más `postgres_password.txt`, `redis_password.txt`,
`database_url.txt`, `redis_url.txt`, `grafana_admin_password.txt` — ocho en total).

Nombres reales de pantallas/paneles citados en `manual-usuario.md`:

```
$ ls frontend/src/components
AccountPanel.tsx  AlertsPanel.tsx  AppRulesPanel.tsx  AuditPanel.tsx  DashboardShell.tsx
DeviceApplicationsList.tsx  DeviceCategoriesPanel.tsx  DeviceLocationPanel.tsx
DevicePolicyPanel.tsx  DevicesPanel.tsx  GeofencePanel.tsx  GoogleSignInButton.tsx
HistoryPanel.tsx  OverviewPanel.tsx  PairingPanel.tsx  RemoteViewPanel.tsx  StatisticsPanel.tsx

$ find mobile/app/src/main/java -iname "*Screen*.kt"
HomeScreen.kt  SupervisedScreen.kt  TutorScreen.kt  (+ BlockScreenActivity.kt, ScreenCapture.kt)

$ sed -n '46,55p' docs/sprint-24.md   # las 16 secciones reales, agrupadas
| Cuenta | Perfil y sesión |
| Dispositivos | Resumen · Dispositivos · Vinculación |
| Control | Aplicaciones instaladas · Reglas por aplicación · Política y horario escolar · Categorías |
| Contexto | Ubicación · Geocercas · Historial · Estadísticas |
| Seguridad | Alertas · Silenciadas · Vista remota · Auditoría |
```

17 archivos `.tsx` en `frontend/src/components/` respaldan esas 16 secciones (`AlertsPanel`
implementa tanto "Alertas" como "Silenciadas", más `DashboardShell`/`GoogleSignInButton` de
infraestructura de la UI que no son secciones de navegación propias). `manual-usuario.md` se
corrigió para usar los 16 nombres reales de `dashboardSections.ts`, agrupados igual que en
`docs/sprint-24.md`, en vez de una lista propia con un conteo distinto.

## 3. Verificación de la funcionalidad no construida (control de navegación web)

```
$ grep -n "VpnService" docs/sprint-09.md docs/android/capability-matrix.md
docs/sprint-09.md:94:  filtrado web necesita `VpnService`, cuyo mecanismo todavía no tiene Fase C propia. Se separa a
docs/android/capability-matrix.md:18:| Filtrado de tráfico local | `VpnService` | ... | Evaluar Sprint 10+ (pospuesto explícitamente en el Sprint 9...) |

$ grep -rln "VpnService" mobile/app/src/main/java backend/app 2>/dev/null
(sin resultados)
```

Confirmado en ambas direcciones: los documentos dicen que se pospuso, y no existe código real
(`VpnService`) en ningún módulo de la aplicación — la afirmación en
`docs/manuals/analisis-riesgos.md` (R1) y `docs/manuals/documento-tecnico.md` (§3.1) es exacta, no
una suposición.

## 4. Verificación de que no se tocó código de aplicación

```
$ git diff --stat HEAD~1 -- backend/app frontend/src mobile/app/src/main
(sin salida)
```

Sólo `docs/`, `README.md` y `CLAUDE.md` cambian en este sprint.

## 5. `README.md`/`CLAUDE.md` actualizados

`README.md` ganó su sección `## Alcance del Sprint 27` (misma convención que cada cierre de sprint
anterior — ver la advertencia de `CLAUDE.md` sobre no olvidarla, encontrada real la primera vez en
el cierre del Sprint 14). `CLAUDE.md` actualiza "Dónde está cada cosa" con `docs/manuals/`, su
"Estado actual" a los 27 sprints completos, y reemplaza la línea "Siguiente: Sprint 27" (ya no hay
sprint siguiente en el roadmap).

## 6. CI en GitHub Actions

Commit `a9e26bb` ("feat: add sprint 27 documentation and presentation deliverables"), corrida
[34776859885](https://github.com/andrescmg06-hue/netprotect/actions/runs/34776859885):

```
✓ backend                33s
✓ frontend                31s
✓ integration             1m12s
✓ api-collection          52s
✓ e2e                     1m27s
✓ performance             1m43s
✓ android                 1m51s
✓ android-instrumented    3m39s
```

Los 8 jobs en verde en un runner limpio, sin ningún cambio de código de aplicación de por medio —
consistente con que este sprint es puramente documental (criterio de aceptación #8 de
`docs/sprint-27.md`).

Disparó automáticamente `cd.yml` vía `workflow_run`
([34777054727](https://github.com/andrescmg06-hue/netprotect/actions/runs/34777054727)):
`build-and-push` publicó imágenes nuevas en GHCR (mismo contenido de aplicación que el commit
anterior, sólo re-etiquetadas con el nuevo SHA); `deploy-production` se saltó, como siempre, por no
existir `vars.PROD_HOST`.

Con esto, los 27 sprints del roadmap (`docs/planning/roadmap.md`) quedan cerrados con evidencia
real de principio a fin.

# Sprint 8 — Reglas de aplicaciones y bloqueo

## Objetivo

El tutor define reglas por app en un dispositivo supervisado — bloquear, permitir, límite diario de
minutos u horario — y el propio dispositivo supervisado las hace cumplir localmente, mostrando una
pantalla de bloqueo propia cuando corresponde. Cada bloqueo aplicado queda registrado para que el
tutor pueda confirmar que sus reglas realmente están funcionando.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-028 | Como tutor, quiero bloquear una app específica en el dispositivo supervisado. |
| HU-029 | Como tutor, quiero definir un límite diario de minutos de uso para una app. |
| HU-030 | Como tutor, quiero definir un horario (días y franja) en que una app quede bloqueada. |
| HU-031 | Como tutor, quiero ver, editar y eliminar las reglas que definí para un dispositivo. |
| HU-032 | Como dispositivo supervisado, quiero que la app bloquee automáticamente el acceso a una app cuando corresponda, mostrando una pantalla propia con el motivo. |
| HU-033 | Como tutor, quiero ver un registro de cuándo se aplicó cada bloqueo, para saber si mis reglas están funcionando. |

## Procedimiento obligatorio de la Fase C — verificación antes de codificar

Aplicado y documentado en detalle en `docs/android/capability-matrix.md` (sección "Sprint 8 —
Bloqueo de apps sin device owner") antes de escribir código. Resumen de lo verificado contra fuentes
oficiales actuales:

- `ActivityManager#getRunningTasks()`/`getRunningAppProcesses()` no sirven: deprecados y
  restringidos a la propia app desde Android 5.0. La única vía vigente sin device owner es sondear
  `UsageStatsManager.queryEvents()` (eventos `MOVE_TO_FOREGROUND`/`ACTIVITY_RESUMED`), reutilizando
  el mismo permiso `PACKAGE_USAGE_STATS` ya concedido en el Sprint 7 — no se pide nada nuevo.
- No existe una API push para "esta app pasó a primer plano": hay que sondear periódicamente, lo que
  implica una ventana real (no instantánea) entre que la app aparece y se detecta/bloquea. No se
  encontró un intervalo de sondeo oficial recomendado — se decide empíricamente al implementar (ver
  "Cambios en Android" cuando esa sección se complete con evidencia real).
- Se consideró y descartó `AccessibilityService` (más reactivo) porque el propio Sprint 7 ya verificó
  que Play Protect bloquea la instalación sideloaded "desde internet" de apps que lo declaren; no se
  introduce ese riesgo para ganar algo de latencia.
- Límites explícitos que se documentan también en la propia app (no sólo en este repositorio): el
  mecanismo es reactivo, el usuario puede revocar el permiso especial en Ajustes en cualquier momento,
  y no sustituye a un control real de administrador de dispositivo.

## Criterios de aceptación

1. El tutor puede crear una regla por `(device_id, package_name)` con tipo `BLOCK`, `ALLOW`,
   `DAILY_LIMIT` (con `daily_limit_minutes` > 0) o `SCHEDULE` (con franja horaria en minutos desde
   medianoche y días de la semana); sólo un tutor vinculado puede hacerlo
   (`require_tutor_of_device`), con el mismo 404 uniforme del resto del proyecto para "no existe" y
   "no es tuyo".
2. Sólo existe una regla activa por app y dispositivo: crear una regla para una app que ya tiene una
   la reemplaza (upsert), no la duplica.
3. El tutor puede listar todas las reglas de un dispositivo y eliminar una puntual.
4. El dispositivo supervisado puede obtener su propio conjunto de reglas activas
   (`require_supervised_owner_of_device`) para evaluarlas localmente, sin depender de una consulta al
   backend por cada apertura de app.
5. El dispositivo supervisado, al detectar (por sondeo) que una app pasó a primer plano, evalúa
   localmente: `BLOCK` bloquea siempre; `DAILY_LIMIT` bloquea si el uso ya sincronizado hoy alcanzó
   el límite; `SCHEDULE` bloquea si la hora/día local del dispositivo cae dentro de la franja
   configurada, incluyendo franjas que cruzan la medianoche; `ALLOW` o ausencia de regla no bloquea.
6. Cada vez que el dispositivo aplica un bloqueo, reporta un evento al backend con el tipo de regla y
   el momento; el tutor puede consultar ese registro por dispositivo.
7. Crear, actualizar y eliminar una regla queda auditado en `audit_logs`, igual que renombrar o
   desvincular un dispositivo.
8. La pantalla de bloqueo en Android identifica la app y el motivo del bloqueo (bloqueo total, límite
   diario alcanzado, o fuera de horario permitido).

## Decisiones de diseño relevantes

- **Los 4 tipos de regla de `plan-desarrollo.md` (permitir/bloquear/límite/horario) se construyen
  ahora, no sólo `BLOCK`.** Decisión explícita del dueño del proyecto tras plantear la alternativa de
  reducir el alcance a sólo bloqueo y dejar límite/horario para el Sprint 11 (Tiempo). Se sigue el
  plan literal.
- **`ALLOW` no tiene efecto observable distinto de "no hay regla" en este sprint — y se documenta así,
  no se oculta.** `ALLOW` sólo cobra sentido cuando exista algo más amplio que sobreescribir (una
  regla de categoría o de lista global — Sprints 9 y 10), que todavía no existe. Se acepta y persiste
  vía la API para no tener que migrar el esquema otra vez cuando esos sprints lleguen, pero el motor
  de evaluación local trata `ALLOW` igual que "sin regla" honestamente, en vez de simular un
  comportamiento que no puede tener todavía.
- **Una sola regla activa por `(device_id, package_name)`, no una lista de reglas superpuestas.**
  Evita resolver prioridad entre reglas del mismo alcance (por app) en este sprint. La prioridad
  entre alcances distintos (regla por app vs. lista blanca/negra vs. categoría) es problema del
  Sprint 9, que ya lo tiene como criterio propio ("prioridad de reglas definida y probada") — no se
  adelanta ni se duplica aquí.
- **Evaluación local en el dispositivo, no una consulta al backend por cada apertura de app.** El
  dispositivo descarga su propio conjunto de reglas activas (mismo patrón de sincronización periódica
  ya usado para heartbeat e inventario, sin scheduler en segundo plano) y decide localmente. Es
  obligado por lo verificado en la Fase C: ya hay que sondear `UsageStatsManager` localmente porque no
  existe una API push; añadir una llamada de red por cada detección multiplicaría la latencia real
  del bloqueo, que ya es reactiva por diseño.
- **`DAILY_LIMIT` se compara contra el mismo total diario que ya calcula el inventario del Sprint 7**
  (`device_application_usage` / lo que el `AppInventoryCollector` ya agrega localmente), no un
  contador nuevo y paralelo. Hereda la misma deuda ya declarada en el Sprint 7: el día calendario no
  está normalizado contra la zona horaria real del dispositivo — no es una limitación nueva de este
  sprint, y se resuelve en el Sprint 11 cuando ese sprint ya necesita tocar lo mismo.
- **`SCHEDULE` guarda minutos desde medianoche en hora local del dispositivo, no UTC.** Por la misma
  razón que `usage_date`: normalizar contra una zona horaria real es trabajo del Sprint 11 (horarios y
  modo escolar, según el propio roadmap); esta franja es la primitiva simple sobre la que ese sprint
  construye, no una versión completa adelantada.
- **El registro de bloqueos aplicados (`usage_events`/eventos de regla) es de sólo inserción**, nunca
  se edita ni se recalcula, y es una tabla distinta de `device_application_usage`: una es un agregado
  diario para mostrar uso (Sprint 7), la otra es evidencia de que una regla se cumplió. Mismo criterio
  que "ya no está" vs. "nunca existió" en otras tablas del proyecto: son hechos con propósitos
  distintos y no deben mezclarse en una sola.
- **Borrado físico de una regla al eliminarla, no baja lógica.** A diferencia de vincular/desvincular
  un dispositivo, ninguna otra tabla depende de que la fila de la regla siga existiendo después de
  borrada, y `audit_logs` ya conserva evidencia de que existió y cuándo se eliminó — un soft-delete
  aquí sería ceremonia sin beneficio real, no una protección de integridad referencial como en los
  casos donde sí se usa.
- **Reutiliza `require_tutor_of_device` y `require_supervised_owner_of_device` tal cual**, sin
  variantes nuevas: gestionar reglas es una acción del tutor sobre un dispositivo ajeno; obtenerlas
  para evaluarlas y reportar eventos es una acción del propio dispositivo supervisado — la misma
  distinción que ya gobierna heartbeat e inventario.
- **La pantalla de bloqueo no promete evasión imposible.** Se documenta en la propia app (y ya en
  `capability-matrix.md`) que el mecanismo es reactivo, que el usuario supervisado puede revocar el
  permiso especial en Ajustes, y que esto no sustituye a un control real de administrador de
  dispositivo — siguiendo la instrucción explícita del proyecto de no prometer un bloqueo que Android
  no garantiza a una app sin esos privilegios.

## Cambios de base de datos

Migración `02c8782f69e6`: dos tablas nuevas.

- `app_rules`: una fila por `(device_id, package_name)` — restricción única
  `uq_app_rules_device_package` — con `rule_type` (`ALLOW`/`BLOCK`/`DAILY_LIMIT`/`SCHEDULE`,
  forzado con `CheckConstraint`) y las columnas específicas de cada tipo
  (`daily_limit_minutes`; `schedule_start_minute`/`schedule_end_minute`/`schedule_days_mask`),
  cada una obligatoria sólo para su tipo mediante dos `CheckConstraint` adicionales.
- `app_rule_events`: sólo inserción, sin llave foránea a `app_rules` a propósito (ver decisiones de
  diseño) — registra cada vez que el dispositivo aplicó un bloqueo.

Ciclo upgrade → downgrade → upgrade verificado contra el PostgreSQL de desarrollo real (no sólo el
de pruebas). Además, verificado directamente con `psql` que los `CheckConstraint` realmente
rechazan datos inválidos, no sólo que existen:

```
--- intento inválido: DAILY_LIMIT sin minutos (debe fallar) ---
ERROR:  new row for relation "app_rules" violates check constraint "ck_app_rules_daily_limit_requires_minutes"

--- intento inválido: rule_type fuera del enum (debe fallar) ---
ERROR:  new row for relation "app_rules" violates check constraint "ck_app_rules_type_valid"
```

Nota de entorno encontrada en el camino: esta máquina tiene un PostgreSQL nativo de Windows
(servicios `postgresql-x64-17`/`18`) ocupando el puerto 5432, y la sesión de trabajo no tenía
permisos para detenerlo. Se resolvió con un `compose.override.yaml` local, no versionado (ya
listado en `.gitignore`), que remapea sólo el puerto publicado de `db` en `compose.yaml` a 55432 en
el host — no afecta a `compose.test.yaml` (que no publica puertos al host, corre siempre dentro de
la red de contenedores) ni a CI. Quien retome el proyecto en una máquina con esto mismo puede crear
el mismo archivo:

```yaml
services:
  db:
    ports: !override
      - "127.0.0.1:55432:5432"
```

También encontrado: generar una migración con `alembic revision --autogenerate` escribiendo a un
volumen montado desde Git Bash en Windows falla en dos formas no obvias — (1) el propio contenedor
corre como usuario `netprotect`, no root, así que no puede escribir en `/app/alembic/versions`
aunque el volumen esté bien montado (hace falta `--user root` sólo para ese comando puntual); (2)
Git Bash reescribe cualquier argumento que empiece con `/` como si fuera una ruta de Windows, así
que `-v "$(pwd)/...:/app/alembic/versions"` corrompe silenciosamente el lado del contenedor de ese
mapeo — hace falta `MSYS_NO_PATHCONV=1` antes del comando `docker compose run` para que Git Bash no
lo toque.

## Fuera de alcance

- Prioridad entre listas blanca/negra, categorías y reglas por app — Sprint 9 y 10, que ya lo tienen
  como criterio propio.
- Normalización de zona horaria para `usage_date` y las franjas de `SCHEDULE` — Sprint 11, que ya
  necesita resolver esto de todas formas.
- "Modo escolar" (conjuntos de horarios activables/desactivables como grupo) — Sprint 12.
- Filtrado de navegación web con `VpnService` — evaluado en la matriz de capacidades para Sprint 8/9,
  pero es un mecanismo independiente (nivel de red, no de app en primer plano); se separa para no
  mezclar dos motores de bloqueo distintos en el mismo sprint.
- Cualquier forma de bloqueo a nivel de administrador de dispositivo (device owner) — fuera del MVP,
  ya documentado como tal en `capability-matrix.md`.

## Backend

- `POST /devices/{device_id}/rules` (tutor): upsert por `(device_id, package_name)` — reemplaza la
  regla existente si ya había una, nunca duplica. Valida en el schema (`model_validator`) que
  `DAILY_LIMIT` traiga `daily_limit_minutes` y que `SCHEDULE` traiga las tres columnas de franja,
  antes de tocar la base. Audita `APP_RULE_CREATED` o `APP_RULE_UPDATED` según corresponda.
- `GET /devices/{device_id}/rules` (tutor): lista las reglas del dispositivo.
- `DELETE /devices/{device_id}/rules/{rule_id}` (tutor): 404 si la regla no existe o pertenece a
  otro dispositivo — mismo criterio anti-IDOR que el resto del proyecto. Borrado físico (ver
  decisiones de diseño). Audita `APP_RULE_DELETED`.
- `GET /devices/{device_id}/rules/active` (dispositivo supervisado, `require_supervised_owner_of_device`):
  el propio dispositivo obtiene su conjunto de reglas para evaluarlas localmente. Misma consulta que
  el listado del tutor, sólo con la otra dependencia de autorización — no existe hoy ningún filtro de
  "activa" distinto de "existe", documentado así en el propio código en vez de fingir una distinción
  que no existe todavía.
- `POST /devices/{device_id}/rule-events` (dispositivo supervisado): inserta un evento cada vez que
  el dispositivo aplicó un bloqueo. Sin auditoría (mismo criterio que la sincronización del Sprint 7:
  es evidencia de cumplimiento, no una acción a revisar).
- `GET /devices/{device_id}/rule-events` (tutor): lista los eventos de bloqueo del dispositivo,
  del más reciente al más antiguo, sin paginar todavía — hereda la misma deuda de retención ya
  declarada para `device_application_usage` en el Sprint 7 (ver "Fuera de alcance").

## Ejecución

```bash
docker compose up --build -d

# el tutor crea una regla de bloqueo total
curl -X POST http://localhost:8000/api/v1/devices/<id>/rules \
  -H "Authorization: Bearer <token_tutor>" -H "Content-Type: application/json" \
  -d '{"package_name":"com.instagram.android","rule_type":"BLOCK"}'

# el propio dispositivo supervisado obtiene sus reglas activas
curl http://localhost:8000/api/v1/devices/<id>/rules/active -H "Authorization: Bearer <token_supervisado>"

# el dispositivo reporta que aplicó un bloqueo
curl -X POST http://localhost:8000/api/v1/devices/<id>/rule-events \
  -H "Authorization: Bearer <token_supervisado>" -H "Content-Type: application/json" \
  -d '{"package_name":"com.instagram.android","rule_type_applied":"BLOCK","occurred_at":"2026-09-05T21:00:00Z"}'

# el tutor consulta el historial de bloqueos aplicados
curl http://localhost:8000/api/v1/devices/<id>/rule-events -H "Authorization: Bearer <token_tutor>"
```

## Verificación

Ciclo de migración verificado contra el PostgreSQL de desarrollo real (upgrade → downgrade →
upgrade), incluyendo una comprobación directa con `psql` de que los `CheckConstraint` rechazan
datos inválidos (ver "Cambios de base de datos").

```bash
docker compose -f compose.test.yaml build backend
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

`ruff check app tests alembic` limpio y **96 pruebas pasan** (76 previas + 20 nuevas en
`tests/test_rules_integration.py`, cubriendo los 8 criterios de aceptación: creación con validación
por tipo, upsert, listado, borrado con las dos formas de "no existe", evaluación local vía
`/rules/active`, reporte y listado de eventos de bloqueo, y auditoría de creación/edición/borrado),
verde dos veces seguidas en contenedor limpio.

## Android

- `ForegroundAppDetector`: sondea `UsageStatsManager.queryEvents()` para `MOVE_TO_FOREGROUND`
  (con `@Suppress("DEPRECATION")` documentado: su reemplazo `ACTIVITY_RESUMED` exige API 29+ y este
  proyecto soporta minSdk 26). Sin caché de eventos: cada llamada avanza su propia marca de agua,
  así que un `queryEvents()` nulo transitorio (dispositivo no "unlocked") no reproduce la misma
  ventana en el siguiente sondeo.
- `RuleEvaluator`: función pura (sin I/O) que decide `BLOCK`/`DAILY_LIMIT`/`SCHEDULE`/nada a partir
  de las reglas, el uso de hoy y la hora local — incluye el manejo de franjas que cruzan medianoche.
  11 pruebas unitarias JVM en `RuleEvaluatorTest.kt` (la primera suite de pruebas unitarias reales
  del proyecto Android; hasta este sprint la verificación era sólo compilación + backend).
- `RuleEnforcementService`: foreground service `specialUse` que sondea cada 3 segundos (intervalo
  elegido empíricamente, sin guía oficial — ver capability-matrix.md), refresca las reglas cada 60
  segundos, evalúa con `RuleEvaluator` y lanza `BlockScreenActivity` cuando corresponde, reportando
  el evento al backend. Sin reinicio automático si el proceso muere (documentado como límite real,
  no como pendiente oculto).
- `BlockScreenActivity`: pantalla de bloqueo con el nombre de la app, el motivo, y un texto explícito
  de que no es un bloqueo garantizado (mismo lenguaje que la matriz de capacidades).
- `SupervisedScreen`: arranca y detiene el servicio con un `DisposableEffect` ligado a
  `(state, hasUsageAccess)` — mismo gating que la sincronización del Sprint 7, porque la detección
  usa el mismo permiso. Pide `POST_NOTIFICATIONS` en Android 13+ (no obligatorio para que el
  servicio funcione, pedido igual porque la notificación es deliberadamente visible).
- Manifiesto: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, y la
  declaración del `<service>` con `foregroundServiceType="specialUse"` + la propiedad
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` con la justificación en texto.

### Verificación en ejecución real (no sólo compilación)

Además de `./gradlew test assembleDebug assembleRelease` (11 pruebas unitarias nuevas, ambos APK
empaquetan), se instaló el APK en un emulador Pixel_8 (API 36) real y se verificó en vivo:

- El manifiesto compilado dentro del APK trae bien declarados `BlockScreenActivity`,
  `RuleEnforcementService` y la propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` (confirmado con
  `aapt2 dump xmltree`, no sólo leyendo el XML fuente).
- Arrancar el servicio sin que la app tenga antes una actividad visible falla con
  `Error: app is in background uid null` (restricción real de Android 12+) — confirma que el
  arranque desde `SupervisedScreen` (con la app ya en primer plano) es necesario, no incidental.
- Con la app en primer plano, `dumpsys activity services` confirma `isForeground=true`,
  `types=0x40000000` (`specialUse`) y la notificación `ONGOING_EVENT|FOREGROUND_SERVICE` activa en
  el canal `rule_enforcement`, sin ninguna excepción durante ~40 segundos de sondeo continuo contra
  un backend con credenciales falsas (los fallos de red se tragan con `runCatching`, como se
  diseñó). Detalle completo y comandos en `docs/android/capability-matrix.md`.

Pendiente, explícitamente no hecho todavía — requiere que una persona haga el login real de Google
en el emulador (ningún agente puede elegir una cuenta en el selector): el flujo end-to-end completo
(tutor crea una regla real → dispositivo supervisado la descarga → abre la app bloqueada → aparece
`BlockScreenActivity` → el evento llega al backend). Lo que sí se verificó en ejecución real es que
cada pieza por separado funciona (servicio, notificación, tipo de foreground service, lógica de
evaluación); lo no verificado es específicamente la cadena completa disparada por una detección de
`UsageStatsManager` real con datos reales, no la mecánica de arranque del servicio en sí.

## Web

- `apiClient.ts` gana `AppRule`, `AppRuleEvent`, `listAppRules`, `upsertAppRule`, `deleteAppRule` y
  `listRuleEvents` — mismos tipos y semántica que el backend (upsert por `(device_id, package_name)`,
  no una lista de reglas superpuestas).
- `DeviceRulesPanel` (componente nuevo, mismo patrón que `DeviceApplicationsList` del Sprint 7):
  formulario para crear/reemplazar una regla por app — con campos condicionales según el tipo
  (minutos para `DAILY_LIMIT`; hora de inicio/fin y selector de días con máscara de bits para
  `SCHEDULE`, mismo bit 0 = lunes que el backend y Android) — lista de reglas existentes con botón
  de eliminar, y un historial de bloqueos aplicados que se carga bajo demanda (no en el primer
  render, para no pedir datos que el tutor puede no llegar a ver nunca).
- `DevicesPanel` gana un botón "Gestionar reglas" por dispositivo, junto al ya existente "Ver apps".
- Cada `setState` dentro de un efecto sigue encadenado directamente a un `.then()/.catch()` en el
  cuerpo del efecto (con bandera `cancelled`), nunca delegado a un helper — la misma regla de
  `react-hooks/set-state-in-effect` documentada en `CLAUDE.md` desde el Sprint 3.

## Pendiente para cerrar el sprint

- El flujo end-to-end completo en Android con login real de Google (ver la sección "Android" de
  arriba) — requiere que una persona elija su cuenta en el selector del sistema.
- Verificación visual en navegador de `DevicesPanel`/`DeviceRulesPanel` una vez autenticado: el
  `dev server` se levantó y se confirmó que sirve `/` (200) y que el backend expone los tres
  endpoints de reglas en su `openapi.json`, pero llegar a la pantalla del tutor real exige el mismo
  login de Google — no se hizo clic real ahí dentro. Lo que sí se verificó automáticamente: `lint`
  estricto y `build` de producción sin errores de TypeScript.
- ~~`/security-review` final.~~ Corrido: sin hallazgos de alta confianza. Detalle en
  `docs/sprint-08-evidence.md`.
- Corrida de CI en GitHub Actions con los 4 jobs en verde.

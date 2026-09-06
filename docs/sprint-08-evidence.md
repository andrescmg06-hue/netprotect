# Evidencia de verificación — Sprint 8

Fecha: 05-06/09/2026. Máquina: Windows 11 Home Single Language, Docker Desktop, emulador Android
Pixel_8 (API 36, imagen `google_apis_playstore`).

Este documento cubre backend, Android y el panel web. `/security-review` y la corrida de CI en
GitHub Actions **no se han hecho todavía** — ver "No se marca como verificado" al final.

## Investigación previa (procedimiento obligatorio de la Fase C)

Antes de escribir código de bloqueo se investigó, contra fuentes oficiales actuales (no memoria de
entrenamiento), el mecanismo real para detectar qué app tiene el usuario en primer plano sin ser
device owner, y el requisito de foreground service que eso implica. Detalle completo con citas en
`docs/android/capability-matrix.md` (sección "Sprint 8"). Hallazgos que cambiaron decisiones
concretas del diseño:

- `ActivityManager#getRunningTasks()`/`getRunningAppProcesses()` no sirven (deprecados/restringidos
  desde Android 5.0); la única vía vigente es sondear `UsageStatsManager.queryEvents()` — no existe
  una API push para esto, hay que sondear.
- Se descartó `AccessibilityService` (más reactivo) porque Play Protect ya bloquea, según lo
  verificado en el Sprint 7, la instalación sideloaded "desde internet" de apps que lo declaren.
- Sondear en segundo plano exige un foreground service, y todo foreground service exige notificación
  — esto es un requisito de Android desde la API 26, **no** de la política anti-stalkerware de Play
  (que sigue sin aplicar mientras no se publique). Se corrigió una nota del Sprint 7 que daba esto
  por innecesario hasta publicar — estaba mal, aplica ya.
- Con `compileSdk`/`targetSdk` 36, Android exige declarar un `foregroundServiceType`; ningún tipo
  predefinido encaja en "vigilar la app en primer plano", así que corresponde `specialUse` con la
  propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justificando el uso.
- Se confirmó que `POST_NOTIFICATIONS` no es necesario para que el servicio arranque (si se niega,
  el servicio corre igual, sólo no se ve la notificación en la bandeja) — se pide de todos modos
  porque la notificación es deliberadamente visible.

## Backend

### Migración

```text
alembic upgrade head    → fedd07a82d4e -> 02c8782f69e6, app rules: block/allow/daily-limit/schedule and rule enforcement events
alembic downgrade -1    → revierte limpiamente
alembic upgrade head    → reaplicada
```

Verificado contra el PostgreSQL de desarrollo real. Además, verificado con `psql` que los
`CheckConstraint` realmente rechazan datos inválidos, no sólo que existen en el esquema:

```text
--- intento inválido: DAILY_LIMIT sin minutos (debe fallar) ---
ERROR:  new row for relation "app_rules" violates check constraint "ck_app_rules_daily_limit_requires_minutes"

--- intento inválido: rule_type fuera del enum (debe fallar) ---
ERROR:  new row for relation "app_rules" violates check constraint "ck_app_rules_type_valid"
```

### Suite completa en contenedor

```text
docker compose -f compose.test.yaml build backend
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 96 passed, 3 warnings in 22.29s   (primera corrida)
→ backend-1 | 96 passed, 3 warnings in 19.45s   (segunda corrida, base y Redis recreados)
→ backend-1 exited with code 0
```

20 pruebas nuevas en `tests/test_rules_integration.py` (76 previas + 20 = 96), contra PostgreSQL
real. Cubren los 8 criterios de aceptación: creación de regla con validación específica por tipo
(`DAILY_LIMIT`/`SCHEDULE` rechazadas con 422 si faltan sus campos), upsert (reemplaza en vez de
duplicar), listado, borrado con las dos formas de "no existe" (regla inexistente y regla de otro
dispositivo), evaluación local vía `GET /rules/active` (incluida la franja horaria que cruza
medianoche), reporte y listado de eventos de bloqueo, y auditoría de creación/edición/borrado
(`APP_RULE_CREATED`/`APP_RULE_UPDATED`/`APP_RULE_DELETED` verificados directamente contra la tabla
`audit_logs`).

### `ruff check`

```text
ruff check app tests alembic
All checks passed!
```

## Android

### Compilación y empaquetado

```text
./gradlew compileDebugKotlin                  → BUILD SUCCESSFUL
./gradlew testDebugUnitTest                   → BUILD SUCCESSFUL, 11 pruebas (10 en RuleEvaluatorTest + 1 preexistente)
./gradlew test assembleDebug assembleRelease  → BUILD SUCCESSFUL en 8m 26s, 103 tareas (86 ejecutadas)
```

`RuleEvaluatorTest.kt` es la primera suite de pruebas unitarias JVM real de este proyecto Android
(hasta el Sprint 7 la verificación de lógica era sólo compilación + backend). Cubre: sin regla no
bloquea, `ALLOW` no bloquea, `BLOCK` siempre bloquea, `DAILY_LIMIT` no bloquea por debajo del
límite y bloquea justo al alcanzarlo, `SCHEDULE` dentro/fuera de una franja del mismo día, la franja
que cruza medianoche (22:00-06:00) en ambos lados, y que el `daysMask` excluye correctamente un día
no incluido.

### Verificación en ejecución real (emulador, no sólo compilación)

Instalado el APK de debug en un emulador Pixel_8 (API 36) real y verificado en vivo:

```text
aapt2 dump xmltree app-debug.apk --file AndroidManifest.xml
→ confirma BlockScreenActivity, RuleEnforcementService y la propiedad
  PROPERTY_SPECIAL_USE_FGS_SUBTYPE realmente empaquetados en el manifiesto compilado,
  foregroundServiceType=0x40000000 (specialUse)
```

Para poder arrancar el servicio desde `adb` (normalmente no permitido: es `exported="false"` a
propósito) se lo exportó **temporalmente**, se probó, y se revirtió de inmediato a `false` antes de
la compilación final — el estado final commiteado nunca tuvo el servicio exportado.

```text
adb shell am start-service -n com.netprotect.app/.core.rules.RuleEnforcementService ...
  (sin actividad visible antes) → Error: app is in background uid null

adb shell am start -n com.netprotect.app/.MainActivity   (trae la app a primer plano)
adb shell am start-service -n com.netprotect.app/.core.rules.RuleEnforcementService ...
  → arranca sin error

adb shell dumpsys activity services com.netprotect.app
→ isForeground=true foregroundId=1001 types=0x40000000
  foregroundNoti=Notification(channel=rule_enforcement ... flags=ONGOING_EVENT|FOREGROUND_SERVICE)
```

Sin ninguna `SecurityException` ni `MissingForegroundServiceTypeException`, y sin excepciones en el
proceso (`adb logcat --pid=<pid> | grep -i exception`) durante los ~40 segundos que el servicio
corrió sondeando contra un backend inalcanzable con credenciales falsas — confirma que los fallos de
red se tragan con `runCatching` como se diseñó, sin crashear el servicio.

**Hallazgo real, no anticipado**: el primer intento de arrancar el servicio por `adb` sin que la app
tuviera antes una actividad visible falló con la restricción de Android 12+ a iniciar foreground
services desde segundo plano. Esto no es un problema del código: **confirma en ejecución real que el
diseño ya elegido era necesario**, no una preferencia — `RuleEnforcementService.start()` sólo
funciona porque `SupervisedScreen` lo llama desde un `DisposableEffect` mientras esa pantalla está
en primer plano.

## Panel web

```text
npm install        → 346 paquetes, 0 vulnerabilidades
npm run lint        → eslint . --max-warnings=0 → sin salida (limpio)
npm run build       → Compiled successfully in 6.1s; TypeScript sin errores (22.3s); 3 páginas
                       estáticas generadas
```

Se reconstruyeron `backend`, `web` y `migrate` de `compose.yaml` (el stack persistente) y se
confirmó en caliente, no sólo que compila:

```text
GET http://localhost:8000/api/v1/health/ready → {"status":"ready","backend":"connected",
                                                   "database":"connected","redis":"connected"}
GET http://localhost:3000/ → 200
GET http://localhost:8000/openapi.json → incluye /api/v1/devices/{device_id}/rules,
  /api/v1/devices/{device_id}/rules/active y /api/v1/devices/{device_id}/rules/{rule_id}
```

No se verificó con clic real en un navegador: llegar a `DevicesPanel`/`DeviceRulesPanel` exige
iniciar sesión con una cuenta de Google real y seleccionarla en el selector del sistema — mismo
límite de siempre, no una omisión de esta verificación puntual. Lo que sustituye esa prueba visual:
el contrato entre `apiClient.ts` y el backend (nombres de campos, semántica de upsert) ya está
probado automáticamente por las 20 pruebas de `test_rules_integration.py`, contra la misma API que
consume este panel.

## Problemas reales encontrados en el camino (y cómo se corrigieron)

1. **No existía `.env`** en esta máquina para este checkout. Se creó copiando
   `.env.development.example` con secretos generados con `secrets.token_urlsafe()`, tal como pide
   `CLAUDE.md`; `GOOGLE_WEB_CLIENT_ID` quedó con el valor de ejemplo porque no hacía falta login real
   de Google para este trabajo.
2. **Un PostgreSQL nativo de Windows** (servicios `postgresql-x64-17`/`18`) ya ocupaba el puerto
   5432, y la sesión no tenía permiso para detenerlo. Se resolvió con un `compose.override.yaml`
   local no versionado (agregado a `.gitignore`) que remapea sólo el puerto publicado de `db` en
   `compose.yaml` a 55432 en el host — no afecta a `compose.test.yaml` (no publica puertos al host)
   ni a CI.
3. **Generar la migración con `alembic revision --autogenerate` escribiendo a un volumen montado
   desde Git Bash en Windows falla en dos formas no obvias**: (1) el contenedor corre como usuario
   `netprotect`, no root, así que no puede escribir en `/app/alembic/versions` sin `--user root` para
   ese comando puntual; (2) Git Bash reescribe cualquier argumento que empiece con `/` como ruta de
   Windows, corrompiendo silenciosamente el lado del contenedor de un `-v origen:/destino` — hace
   falta `MSYS_NO_PATHCONV=1` antes de `docker compose run` para evitarlo.
4. **Docker Desktop no estaba corriendo** al empezar; se inició con
   `Start-Process "Docker Desktop.exe"` desde PowerShell y se esperó con un `until docker info` en
   segundo plano antes de continuar.
5. **`mobile/local.properties` no existía**, así que ningún `./gradlew` podía ubicar el SDK. Se creó
   apuntando a `%LOCALAPPDATA%\Android\Sdk` (no versionado, ya en `.gitignore`).
6. **El emulador aparecía como `unauthorized` en `adb`** al arrancar — se resolvió con
   `adb kill-server && adb start-server`, sin tocar el emulador.
7. **Lanzar el emulador desde Git Bash con `nohup ... & disown` no lo desacopla de verdad en
   Windows**: el proceso moría en silencio en cuanto terminaba la invocación de la herramienta de
   shell que lo lanzó (no hay mensaje de error, simplemente deja de existir). Se corrigió lanzándolo
   con `Start-Process` desde PowerShell, que sí lo desacopla del todo. Relevante para cualquiera que
   automatice el emulador desde este entorno en el futuro.
8. **El apagón de la máquina a mitad de sesión** mató el emulador y los procesos en segundo plano en
   curso; se detectó porque `adb devices` dejó de listar el dispositivo y se relanzó todo desde cero
   (con la lección del punto 7 ya aplicada).
9. **`frontend/node_modules` no existía** en este checkout; `npm run lint` fallaba con
   `"eslint" no se reconoce como un comando`. Se corrigió con `npm install` (346 paquetes, 0
   vulnerabilidades) antes de poder correr `lint`/`build`.

## No se marca como verificado

Lo de arriba son comandos ejecutados realmente con salida real, incluidos los hallazgos y sus
correcciones. Limitación honesta, ya conocida desde sprints anteriores: el flujo end-to-end completo
con datos reales — el tutor crea una regla real, el dispositivo supervisado la descarga, alguien
abre de verdad la app bloqueada y `UsageStatsManager` la detecta en una condición real de uso — no
se probó, porque exige un login real de Google (elegir cuenta en el selector del sistema) que ningún
agente puede hacer. Lo que sí se verificó en su lugar, por partes: el motor de reglas del backend
tiene prueba automatizada positiva y negativa para cada criterio; la lógica de evaluación
(`RuleEvaluator`) tiene prueba unitaria real, incluidos los casos límite de horario; y el mecanismo
más nuevo y riesgoso de este sprint — el foreground service `specialUse` — se verificó en ejecución
real contra el sistema operativo, no sólo en el código fuente.

## `/security-review`

Corrido sobre el diff completo del sprint (backend, Android y web). Metodología: un sub-agente
identificó candidatos contra las categorías estándar (IDOR/BOLA, inyección, exposición de
componentes Android, XSS, exposición de datos), comparando explícitamente contra los patrones ya
establecidos del proyecto (`require_tutor_of_device`/`require_supervised_owner_of_device`,
`applications.py` como referencia). Resultado: **sin hallazgos de alta confianza**. Se
identificaron y descartaron dos candidatos de baja severidad (confianza autoevaluada 2/10, muy por
debajo del umbral de reporte):

- Pasar el token de acceso como extra de texto plano de un `Intent` al arrancar
  `RuleEnforcementService` — no explotable porque el servicio es `exported="false"` y ninguna otra
  app puede leer esos extras sin que el dispositivo ya esté comprometido.
- `ReportRuleEventRequest.occurred_at` acepta cualquier fecha sin acotar contra la hora del
  servidor — el dispositivo supervisado ya es el límite de confianza de su propia telemetría (mismo
  modelo que la sincronización de uso del Sprint 7); el impacto quedaría limitado al propio
  historial de ese dispositivo, no cruza ningún límite de tenant o privilegio.

Verificado también explícitamente: las seis rutas nuevas reutilizan el mismo patrón anti-IDOR
404-para-ambos del resto del proyecto (confirmado además por las pruebas automatizadas), todas las
consultas son SQLAlchemy parametrizado, y ningún componente Android nuevo (`RuleEnforcementService`,
`BlockScreenActivity`) es invocable desde otra app.

## CI en GitHub Actions

Run [`34004410772`](https://github.com/andrescmg06-hue/netprotect/actions/runs/34004410772), commit
`356af34`, los 4 jobs en verde: `frontend` (28s), `android` (1m45s), `backend` (17s), `integration`
(51s). Únicas anotaciones: warnings de GitHub sobre la deprecación de Node.js 20 en sus propios
runners (no relacionado con el código de este proyecto).

Con esto, el Sprint 8 queda cerrado según la regla de `CLAUDE.md`: backend, Android y web
verificados con evidencia real, `/security-review` corrido sin hallazgos, y CI en verde en un
runner limpio.

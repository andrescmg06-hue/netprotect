# Sprint 20 — Evidencia de ejecución

Comandos realmente ejecutados y su salida real, incluidos los errores encontrados en el camino.
Fecha: 08/09/2026. Máquina: Windows 11 + Docker Desktop.

## 1. Lint del backend — primer intento fallido (caché de ruff)

```
$ docker compose -f compose.test.yaml run --rm backend ruff check app tests alembic
error: Failed to initialize cache at /app/.ruff_cache: Permission denied (os error 13)
ruff failed
  Cause: Failed to create temporary file
  Cause: No such file or directory (os error 2) at path "/app/.ruff_cache/0.13.3/.tmphi2Jek"
```

El contenedor corre como usuario sin permiso de escritura en `/app`. Se repitió con `--no-cache`
(lo que hace CI también sobre un runner limpio, donde no hay caché previa que reutilizar).

## 2. Lint del backend — errores reales encontrados y corregidos

```
$ docker compose -f compose.test.yaml run --rm backend ruff check --no-cache app tests alembic
S608 Possible SQL injection vector through string-based query construction
  --> alembic/versions/c8a1e5b90d34_alerts_manipulation_signal_types.py:41:16
F401 [*] `app.schemas.alert.AlertResponse` imported but unused
  --> app/api/v1/endpoints/devices.py:19:31
E501 Line too long (106 > 100)
  --> app/models/alert.py:31:101
E501 Line too long (101 > 100)
  --> app/services/tamper.py:88:101
Found 4 errors.
```

Correcciones:

- **S608**: el `downgrade()` construía el `DELETE` con una f-string sobre una constante del propio
  módulo. No era inyectable (no hay entrada de usuario), pero la regla marca el patrón, no el
  origen del dato: se reescribió con la lista literal en línea, que además deja el SQL legible tal
  cual se ejecuta.
- **F401**: `AlertResponse` se importó pensando en devolver la alerta creada desde
  `POST /tamper-events`; el endpoint acabó devolviendo `204 No Content` (la alerta se lee por
  `GET /devices/{id}/alerts`, como todas), así que el import sobraba.
- **E501** ×2: dos comentarios/llamadas por encima de 100 columnas.

Tras las correcciones:

```
$ docker compose -f compose.test.yaml build backend migrate
$ docker compose -f compose.test.yaml run --rm backend ruff check --no-cache app tests alembic
All checks passed!
```

## 3. Migración

```
$ docker compose -f compose.test.yaml run --rm migrate
INFO  [alembic.runtime.migration] Running upgrade b7c3e0d4f1a8 -> d3f6a9c2b5e7, devices.fcm_token: FCM registration token for the real-time wake-up nudge (Sprint 18)
INFO  [alembic.runtime.migration] Running upgrade d3f6a9c2b5e7 -> c8a1e5b90d34, alerts: allow the Sprint 20 manipulation-detection signal types
```

Ciclo inverso y vuelta a subir, sobre la base ya migrada:

```
$ docker compose -f compose.test.yaml run --rm migrate alembic downgrade -1
INFO  [alembic.runtime.migration] Running downgrade c8a1e5b90d34 -> d3f6a9c2b5e7, alerts: allow the Sprint 20 manipulation-detection signal types

$ docker compose -f compose.test.yaml run --rm migrate alembic upgrade head
INFO  [alembic.runtime.migration] Running upgrade d3f6a9c2b5e7 -> c8a1e5b90d34, alerts: allow the Sprint 20 manipulation-detection signal types
```

Nota: el primer intento de `downgrade -1` se hizo sobre una base recién levantada (sin migrar) y
falló con `Relative revision -1 didn't produce 1 migrations` — `compose.test.yaml` corre PostgreSQL
sin volumen persistente a propósito, así que la base estaba vacía. No es un fallo de la migración:
hay que aplicar `upgrade head` antes de poder bajar un escalón, como ya advierte `CLAUDE.md`.

## 4. Suite completa contra PostgreSQL y Redis reales

```
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
backend-1  | 222 passed, 4 warnings in 32.09s
backend-1 exited with code 0
```

222 pruebas (las 205 anteriores más las 17 nuevas de `tests/test_tamper_integration.py`), todas
contra la base y Redis reales en contenedor — sin mocks de base de datos. Los únicos `patch` de la
suite siguen siendo los de la verificación del ID token de Google, que exige una persona real.
Esta corrida ya incluye la prueba añadida tras la revisión de seguridad (punto 5).

Las 17 pruebas nuevas cubren: pérdida de permiso, servicio inactivo, desfase de reloj (fuera y
dentro de tolerancia, y con marca sin zona horaria), silencio anómalo (y un `OFFLINE` normal que
**no** debe alertar), un *heartbeat* sin campos nuevos (APK vieja) que no alerta, recuperación a
`ONLINE` con un latido sano, deduplicación por repetición, dos condiciones en un mismo latido,
silenciado, el endpoint de intento de desinstalación (`204`, nivel `CRITICAL`, estado `ALERT`),
`event_type` desconocido (`422`) y los tres casos de autorización (tutor propio, tutor ajeno y otro
supervisado → `404`).

## 5. Revisión de seguridad (`/security-review`) — un fallo real encontrado y corregido

La revisión no encontró vulnerabilidades de acceso, inyección ni exposición de datos: el endpoint
nuevo va detrás de `require_supervised_owner_of_device` (404 tanto para el tutor del propio
dispositivo como para otro supervisado, ya probado), `event_type` está acotado por un `Literal`,
la migración usa SQL literal sin interpolar nada, el `dedup_key` de estas alertas es una constante
del servidor (no un dato del dispositivo), y el receiver de Android va con
`android:permission="android.permission.BIND_DEVICE_ADMIN"`, que restringe su invocación al
sistema operativo.

Sí encontró un fallo de robustez explotable por cualquier dispositivo supervisado autenticado:

> `HeartbeatRequest.device_time` es un `datetime` a secas (igual que `occurred_at`/`captured_at` en
> el resto de la API), así que Pydantic acepta una marca **sin zona horaria**. Restarla de un `now`
> con zona (`datetime.now(UTC) - device_time`) lanza `TypeError`, es decir un 500 provocable con un
> solo campo mal formado: `{"device_time": "2026-09-08T12:00:00"}`.

Corregido en `app/services/tamper.py` leyendo una marca ingenua como UTC —que es justo lo que
manda el cliente Android, `Instant.toString()` siempre termina en `Z`— en vez de rechazarla, y con
una prueba nueva que lo cubre (`test_a_naive_device_time_is_read_as_utc_instead_of_crashing`). Es
la única corrección que salió de la revisión.

## 6. Frontend

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida: sin errores ni advertencias)

$ npx tsc --noEmit
(sin salida, exit 0)
```

El `switch` de `alertLabel` en `AlertsPanel.tsx` es exhaustivo sobre `AlertType`: TypeScript habría
fallado si se hubiera ampliado el tipo en `apiClient.ts` sin añadir los cinco casos nuevos.

## 7. Android

```
$ ./gradlew compileDebugKotlin
BUILD SUCCESSFUL in 3s
18 actionable tasks: 18 up-to-date

$ ./gradlew test assembleDebug          # el mismo comando que corre CI
BUILD SUCCESSFUL in 4s
76 actionable tasks: 76 up-to-date
```

Ambas corridas dicen `up-to-date` porque una ejecución anterior de `test assembleDebug` ya había
compilado estas fuentes. Comprobado que las clases nuevas existen de verdad, y no que Gradle se
saltara trabajo pendiente:

```
$ find app/build -name "EnforcementLiveness*.class" -o -name "NetProtectDeviceAdminReceiver*.class" \
    -o -name "TamperReportWorker*.class" -o -name "DeviceAdminPermission*.class"
app/build/tmp/kotlin-classes/debug/com/netprotect/app/core/permissions/DeviceAdminPermission.class
app/build/tmp/kotlin-classes/debug/com/netprotect/app/core/rules/EnforcementLiveness.class
app/build/tmp/kotlin-classes/debug/com/netprotect/app/core/tamper/NetProtectDeviceAdminReceiver.class
app/build/tmp/kotlin-classes/debug/com/netprotect/app/core/tamper/TamperReportWorker.class
[…también las variantes release y las clases sintéticas del Companion/doWork]

$ ls -la app/build/outputs/apk/debug/app-debug.apk
-rw-r--r-- 1 crist 197609 15992139 Sep  8 16:06 app/build/outputs/apk/debug/app-debug.apk
```

El APK se empaqueta con el `AndroidManifest.xml` que declara el receiver de Device Administrator y
con `res/xml/device_admin.xml`: un error en cualquiera de los dos (recurso inexistente, `<string>`
sin definir, atributo mal escrito) habría roto `processDebugManifest`/`mergeDebugResources`, no
sólo la compilación de Kotlin.

## 8. CI en GitHub Actions (runner limpio)

```
$ git push
   b176e7e..54e62c1  main -> main

$ gh run watch 34281653772 --exit-status
Run ci (34281653772) has already completed with 'success'

$ gh run view 34281653772 --json jobs -q '.jobs[] | "\(.name): \(.conclusion)"'
integration: success
android: success
frontend: success
backend: success
```

Los 4 jobs en verde sobre un runner limpio, no sólo en esta máquina.

## 9. Lo que NO se verificó

- **Login real de Google**: exige que una persona elija su cuenta en el selector. Igual que en
  todos los sprints anteriores.
- **El recorrido real de Device Administrator en un dispositivo/emulador**: activar el
  administrador desde la pantalla del sistema, comprobar que Android impide desinstalar la app
  mientras está activo, y ver `onDisableRequested()` dispararse de verdad al desactivarlo. El
  contrato se verificó contra la documentación oficial (ver `docs/android/capability-matrix.md`,
  sección Sprint 20) y el código compila, pero nadie hizo ese recorrido a mano en esta sesión.
- **`SERVICE_INACTIVE` disparado por una muerte real del proceso** (deslizar la app fuera de
  Recientes y esperar a que `SyncWorker` corra): la lógica del backend está probada de extremo a
  extremo enviando `service_active: false`, pero el camino Android que produce ese `false` no se
  ejerció en un dispositivo real.
- **La UI de Android** (tarjeta de protección contra desinstalación, etiquetas nuevas en la bandeja
  del tutor): compilada, no recorrida a mano.

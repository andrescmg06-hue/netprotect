# Evidencia de verificación — Sprint 13

Fecha: 07-08/09/2026.

## Fase C (Android) — fuentes consultadas

Ver `docs/android/capability-matrix.md`, sección "Sprint 13 — Geolocalización: verificación
detallada", con las citas textuales de cada fuente oficial de developer.android.com y de Play
Console Help. No se repite aquí; esta sección es sólo evidencia de ejecución (Docker, Gradle, npm).

## Backend

### `ruff check` (local, fuera de contenedor)

```text
$ python -m ruff check app tests alembic
All checks passed!
```

### Migración `ba8b839ae0eb` — aplicada, y ciclo downgrade→upgrade verificado

```text
$ docker compose -f compose.test.yaml build
[...]
 Image netprotect-test-migrate Built
 Image netprotect-test-backend Built

$ docker compose -f compose.test.yaml run --rm migrate
INFO  [alembic.runtime.migration] Running upgrade e4ff038efe89 -> ba8b839ae0eb, device location
reports: encrypted lat/lng insert-only history
```

```text
$ docker compose -f compose.test.yaml run --rm migrate alembic downgrade e4ff038efe89
INFO  [alembic.runtime.migration] Running downgrade ba8b839ae0eb -> e4ff038efe89, device location
reports: encrypted lat/lng insert-only history

$ docker compose -f compose.test.yaml run --rm migrate alembic upgrade head
INFO  [alembic.runtime.migration] Running upgrade e4ff038efe89 -> ba8b839ae0eb, device location
reports: encrypted lat/lng insert-only history
```

Ambos sentidos aplican sin error contra PostgreSQL real (no SQLite, no mock).

### Error real encontrado y corregido: `db` se recreaba vacío entre `migrate` y `up`

Primer intento de correr las pruebas: ejecuté `docker compose -f compose.test.yaml run --rm
migrate` (aplicó la migración correctamente, confirmado en su log) y, en una llamada de shell
**separada**, `docker compose -f compose.test.yaml up ... db redis backend`. El backend falló con:

```text
sqlalchemy.exc.ProgrammingError: relation "users" does not exist
```

`docker ps -a` mostró que los tres contenedores (`db`, `redis`, `backend`) se habían creado apenas
segundos antes de terminar el `up` — es decir, `db` se recreó desde cero, sin el esquema que
`migrate` acababa de aplicarle en la corrida anterior. Diagnóstico confirmado repitiendo la
secuencia con una comprobación intermedia (`docker compose -f compose.test.yaml ps -a` justo
después de `migrate`, antes de `up`): con `db` todavía `Up ... (healthy)` en ese punto, encadenar
`up` inmediatamente después sí reutilizó ese mismo contenedor con el esquema ya aplicado. La causa
raíz no se aisló del todo (posible ventana de limpieza de `docker compose run --rm` sobre sus
dependencias, o una demora entre llamadas de herramienta en esta sesión que le dio tiempo a Compose
a considerarlo huérfano); el efecto práctico y ya documentado en `CLAUDE.md` — "`migrate` corre
aparte, siempre, antes de cada `up` — nunca asumir que la corrida anterior lo dejó aplicado" — se
confirma más estricto de lo que parecía: ni siquiera basta con haberlo corrido antes en la misma
sesión si pasa tiempo entre medio. Corrección aplicada: `down -v` para dejar todo limpio, luego
`migrate` y `up` en sucesión inmediata, sin pasos intermedios.

### Pruebas — error real encontrado y corregido en el propio test nuevo

Primera corrida completa tras la migración: 2 fallos, ambos en pruebas nuevas de
`test_location_integration.py`:

```text
FAILED tests/test_location_integration.py::test_a_device_cannot_report_location_for_another_device
E       assert 401 == 404
FAILED tests/test_location_integration.py::test_reporting_location_for_a_nonexistent_device_is_404
E       assert 401 == 404
```

Causa: `_make_account()` devuelve `(token, email)`, y esas dos pruebas desestructuraban al revés
(`_, other_supervised_token = _make_account(...)`), así que `other_supervised_token` terminaba
siendo el **email**, no el token — el backend correctamente devolvía 401 (token inválido) en vez
del 404 esperado. Corregido a `other_supervised_token, _ = _make_account(...)` en ambos casos.

### Corrida final en verde

```text
$ docker compose -f compose.test.yaml down -v
$ docker compose -f compose.test.yaml run --rm migrate
[... upgrade a ba8b839ae0eb aplicado ...]
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
backend-1  | ........................................................................ [ 48%]
backend-1  | ........................................................................ [ 96%]
backend-1  | ......                                                                   [100%]
backend-1  | 150 passed, 3 warnings in 21.07s
backend-1 exited with code 0
```

150 pruebas totales (138 previas + 12 nuevas en `test_location_integration.py`): reporte por el
dueño, rechazo de un dispositivo ajeno, rechazo de un tutor intentando reportar como si fuera el
dispositivo, 404 para dispositivo inexistente, lectura de `latest`/`history` por el tutor dueño,
`latest` nulo sin reportes, 404 para tutor ajeno en ambos endpoints de lectura, purga real de filas
más viejas que `location_retention_days` al reportar de nuevo, y purga nunca cruza dispositivos
(un reporte viejo en otro dispositivo sobrevive intacto al purgar el primero).

## Android

```text
$ JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew compileDebugKotlin --console=plain
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 56s
17 actionable tasks: 5 executed, 12 up-to-date
```

```text
$ JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew assembleDebug --console=plain
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
[...]
> Task :app:packageDebug
> Task :app:assembleDebug
BUILD SUCCESSFUL in 19s
39 actionable tasks: 6 executed, 33 up-to-date
```

El merge de manifiesto (`processDebugMainManifest`/`processDebugManifest`) pasó sin conflictos con
los permisos y el `<service>` nuevos (`ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE_LOCATION`,
`LocationReportingService` con `foregroundServiceType="location"`), y el APK debug empaqueta
completo (`packageDebug`).

Nota: `JAVA_HOME` no estaba configurado en el shell de esta sesión (`java -version` resolvía a un
JDK 24 incompatible con Gradle/AGP de este proyecto); se usó el JDK 17 ya instalado en la máquina
en `C:\Program Files\Java\jdk-17`.

## Web

```text
$ npm run lint
> eslint . --max-warnings=0
[sin salida — limpio]
```

```text
$ npm run build
▲ Next.js 16.3.3 (Turbopack)
✓ Compiled successfully in 20.5s
  Running TypeScript ...
  Finished TypeScript in 4.9s ...
✓ Generating static pages using 4 workers (3/3) in 1374ms
```

## `/security-review`

Sin hallazgos de alta confianza. Verificado explícitamente: `POST`/`GET .../location*` reutilizan
sin cambios `require_supervised_owner_of_device`/`require_tutor_of_device` (mismo 404 uniforme
anti-IDOR que el resto del proyecto); todas las consultas usan `select`/`delete` de SQLAlchemy con
parámetros ligados, sin SQL armado a mano; `LOCATION_ENCRYPTION_KEY` nunca reutiliza
`JWT_SECRET`/`PAIRING_CODE_PEPPER`, y una clave inválida o vacía rompe el cifrado en vez de usar un
valor débil silenciosamente; `compose.prod.yaml` no exige `LOCATION_ENCRYPTION_KEY` con `:?` pero
eso replica el mismo patrón ya existente para `JWT_SECRET`/`PAIRING_CODE_PEPPER` en ese archivo, no
es una inconsistencia nueva de este sprint; las coordenadas interpoladas en el intent `geo:`
(Android) y en el `iframe` del panel web son números ya validados por Pydantic
(`-90..90`/`-180..180`), no cadenas controlables por un atacante.

## No se marca como verificado

- Ejecución real en emulador/dispositivo (conceder el permiso de ubicación, ver el foreground
  service arrancar, confirmar un reporte llegando al backend en vivo) — sólo se verificó
  compilación real. Mismo límite explicado en `docs/sprint-13.md`.
- Login real de Google (límite recurrente en todo el proyecto).
- `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` real — no existe todavía, pendiente de un humano con cuenta de
  Google Cloud (ver `docs/sprint-13.md`). El fallback de texto sin mapa sí se verificó (es el
  camino que toma el build sin la variable configurada).

## CI en GitHub Actions

Pendiente de push — se actualiza esta sección (o se agrega un commit `docs: record green CI run
for sprint 13`) tras confirmar los 4 jobs en verde con `gh run view`.

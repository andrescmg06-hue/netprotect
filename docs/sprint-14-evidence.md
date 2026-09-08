# Sprint 14 — Evidencia

Comandos ejecutados realmente el 07-08/09/2026, con su salida real.

## ruff (backend, local)

```
$ python -m ruff check app tests alembic
E501 Line too long (101 > 100)  app\api\v1\endpoints\geofences.py:143
E501 Line too long (101 > 100)  tests\test_geofence_integration.py:250
Found 2 errors.
```

Corregidas ambas líneas (envolviendo la firma de `@router.delete` y la del test). Segunda corrida:

```
$ python -m ruff check app tests alembic
All checks passed!
```

## Backend: migración + pruebas de integración en Docker

Docker Desktop no estaba corriendo al iniciar la verificación; se arrancó y se esperó a que el
daemon respondiera antes de continuar (ver nota de rendimiento en `CLAUDE.md`: siempre en
contenedor, nunca contra los puertos publicados desde Windows).

```
$ docker compose -f compose.test.yaml build
 Image netprotect-test-migrate Built
 Image netprotect-test-backend Built

$ docker compose -f compose.test.yaml run --rm migrate
...
INFO  [alembic.runtime.migration] Running upgrade ba8b839ae0eb -> e60012acd532,
  geofences and geofence_events: home-grown zones evaluated against location reports

$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 43%]
backend-1  | ........................................................................ [ 86%]
backend-1  | ......................                                                   [100%]
backend-1  | 166 passed, 4 warnings in 41.75s
backend-1 exited with code 0
```

166 pruebas en verde (150 heredadas del Sprint 13 + 16 nuevas en `test_geofence_integration.py`).
Las 4 advertencias son preexistentes (deprecaciones de `httpx`/`anyio`/FastAPI, no relacionadas con
este sprint).

## Migración: ciclo upgrade → downgrade → upgrade

```
$ docker compose -f compose.test.yaml run --rm migrate sh -c \
    "alembic upgrade head && alembic downgrade -1 && alembic upgrade head"
...
INFO  [alembic.runtime.migration] Running upgrade ba8b839ae0eb -> e60012acd532, geofences and geofence_events...
INFO  [alembic.runtime.migration] Running downgrade e60012acd532 -> ba8b839ae0eb, geofences and geofence_events...
INFO  [alembic.runtime.migration] Running upgrade ba8b839ae0eb -> e60012acd532, geofences and geofence_events...
```

Ciclo completo sin errores. `compose.test.yaml` no usa volumen persistente (a propósito, ver
`CLAUDE.md`), así que la primera corrida siempre parte de `base`; el downgrade/upgrade se
encadenó en una sola invocación de `migrate` sobre la misma base de datos ya en `head`.

```
$ docker compose -f compose.test.yaml down
 Container netprotect-test-db-1 Removed
 Container netprotect-test-redis-1 Removed
 Container netprotect-test-backend-1 Removed
```

## Android: compileDebugKotlin

```
$ ./gradlew.bat compileDebugKotlin
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 1m 29s
17 actionable tasks: 1 executed, 16 up-to-date
```

Sin errores de compilación en `GeofenceClient.kt` ni en los cambios de `TutorScreen.kt`.

## Web: npm run lint / npm run build

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — cero advertencias)

$ npm run build
▲ Next.js 16.3.3 (Turbopack)
✓ Compiled successfully in 22.0s
  Running TypeScript ...
  Finished TypeScript in 5.6s ...
✓ Generating static pages using 4 workers (3/3) in 1446ms
```

Sin errores de tipos ni de lint en `apiClient.ts`, `GeofencePanel.tsx` ni `DevicesPanel.tsx`.

## CI en GitHub Actions (runner limpio)

```
$ git push
$ gh run watch 34224346745 --exit-status
✓ main ci · 34224346745
  ✓ integration in 1m11s
  ✓ android in 1m39s
  ✓ frontend in 29s
  ✓ backend in 21s
```

Los 4 jobs en verde en un runner limpio (no esta máquina), commit `3e88125`.

## No se marca como verificado

Ver la sección homónima en `docs/sprint-14.md`: verificación en dispositivo/emulador real de un
cruce físico de geocerca, y login real de Google.

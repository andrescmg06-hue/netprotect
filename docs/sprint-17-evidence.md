# Sprint 17 — Evidencia

## Backend: ruff

```
$ python -m ruff check app tests alembic
All checks passed!
```

## Migración: ciclo upgrade → downgrade → upgrade en Docker

Primer intento: reconstruir sólo `backend` y correr `run --rm migrate` dejó la base en
`a4d8f9c1e6b2` en vez de la nueva `b7c3e0d4f1a8` — `migrate` tiene su propio bloque `build:` en
`compose.test.yaml`, independiente del de `backend`, exactamente la trampa que el `CLAUDE.md` ya
documentaba para `compose.yaml`. Reconstruir `migrate` también (`docker compose build migrate`)
lo corrigió:

```
$ docker compose -f compose.test.yaml build migrate
...
 Image netprotect-test-migrate Built

$ docker compose -f compose.test.yaml run --rm migrate
...
INFO  [alembic.runtime.migration] Running upgrade a4d8f9c1e6b2 -> b7c3e0d4f1a8, alerts and alert_silences: ...

$ docker compose -f compose.test.yaml run --rm backend python -m alembic downgrade -1
INFO  [alembic.runtime.migration] Running downgrade b7c3e0d4f1a8 -> a4d8f9c1e6b2, ...

$ docker compose -f compose.test.yaml run --rm backend python -m alembic upgrade head
INFO  [alembic.runtime.migration] Running upgrade a4d8f9c1e6b2 -> b7c3e0d4f1a8, ...
```

## Backend: pruebas de integración en Docker (PostgreSQL/Redis reales)

Primera corrida con la suite completa, un fallo trivial propio (no del endpoint):

```
backend-1  | FAILED tests/test_alerts_integration.py::test_repeated_blocks_before_reading_are_deduplicated_into_one_alert
backend-1  | AssertionError: assert '2026-09-08T09:00:00Z' == '2026-09-08T09:00:00+00:00'
backend-1  | 1 failed, 193 passed, 5 warnings in 66.69s
```

La prueba comparaba contra el formato `+00:00` en vez del `Z` que Pydantic realmente serializa.
Corregido el assert (dato correcto, no lógica del endpoint). Segunda corrida:

```
$ docker compose -f compose.test.yaml build
$ docker compose -f compose.test.yaml run --rm migrate
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 37%]
backend-1  | ........................................................................ [ 74%]
backend-1  | ..................................................                       [100%]
backend-1  | 194 passed, 4 warnings in 49.16s
backend-1 exited with code 0
```

194 pruebas (185 preexistentes + 9 nuevas de alertas), todas en verde contra PostgreSQL y Redis
reales en contenedor.

## Incidente de entorno: Docker Desktop se detuvo entre sesiones

A mitad de la verificación, el daemon de Docker dejó de responder
(`failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine`) tras un
reinicio de sesión de Claude Code. Se reinició Docker Desktop y se esperó a que el daemon quedara
listo (`docker info`) antes de continuar — no afectó al código, sólo retrasó la verificación.

## Frontend

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — limpio)

$ npm run build
> next build
✓ Compiled successfully in 27.3s
  Running TypeScript ...
  Finished TypeScript in 7.5s ...
✓ Generating static pages using 4 workers (3/3) in 2.1s
```

## Android

```
$ ./gradlew compileDebugKotlin
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 46s
```

Compila `AlertsClient.kt` y los cambios en `TutorScreen.kt` sin errores.

## Revisión de seguridad

`/security-review` sobre el diff completo: sin hallazgos de alta confianza. El endpoint nuevo
reutiliza `require_tutor_of_device` sin modificarlo; las cinco rutas filtran siempre por
`device_id` + `id` de fila (nunca sólo por `id`), evitando IDOR entre dispositivos; `SilenceAlertRequest.days`
está validado (`gt=0`) por Pydantic; no hay SQL crudo ni interpolación de strings en ninguna
consulta nueva.

`CLAUDE.md` y `README.md` actualizados. Falta el push y la verificación de CI (ver más abajo, se
añade tras confirmarlo).

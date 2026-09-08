# Sprint 15 — Evidencia

Comandos ejecutados realmente el 08/09/2026, con su salida real.

## ruff (backend, local)

```
$ python -m ruff check app tests alembic
All checks passed!
```

## Backend: migración + pruebas de integración en Docker

```
$ docker compose -f compose.test.yaml build
 Image netprotect-test-backend Built
 Image netprotect-test-migrate Built

$ docker compose -f compose.test.yaml run --rm migrate
...
INFO  [alembic.runtime.migration] Running upgrade e60012acd532 -> a4d8f9c1e6b2,
  history: (device_id, occurred_at) indexes on app_rule_events and geofence_events

$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 41%]
backend-1  | ........................................................................ [ 82%]
backend-1  | ..............................                                           [100%]
backend-1  | 174 passed, 4 warnings in 38.23s
backend-1 exited with code 0
```

174 pruebas en verde (166 heredadas del Sprint 14 + 8 nuevas: `test_history_integration.py`
completo, dos pruebas de purga en `test_rules_integration.py` y una en
`test_geofence_integration.py`). Las 4 advertencias son preexistentes (deprecaciones de
`httpx`/`anyio`/FastAPI, no relacionadas con este sprint).

Un primer intento de correr sólo `docker compose up` sin repetir `run --rm migrate` (tras el
`down` implícito que dejó el `up` anterior) produjo 160 fallos — exactamente el error ya
documentado en `CLAUDE.md` (`compose.test.yaml` no tiene volumen persistente). Se repitió la
secuencia completa (`build` tras editar tests → `run --rm migrate` → `up`) y quedó en verde.

## /code-review y /security-review

`/security-review` sobre el diff completo: sin hallazgos (todos los endpoints nuevos reutilizan
`require_tutor_of_device`/`require_supervised_owner_of_device`, las queries son parametrizadas vía
SQLAlchemy ORM, las purgas están acotadas por `device_id`).

`/code-review` encontró una duplicación real: el patrón "purgar filas vencidas de este
`device_id`, luego insertar" estaba repetido casi textual en tres sitios
(`DeviceLocationReport`, `GeofenceEvent`, `AppRuleEvent`), con riesgo de que una cuarta copia
futura olvidara el filtro por `device_id`. Se extrajo `app/services/retention.py
purge_expired_rows()` y se reescribieron los tres sitios para usarlo. Suite completa
re-verificada en Docker tras el cambio:

```
$ docker compose -f compose.test.yaml build
$ docker compose -f compose.test.yaml run --rm migrate
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1 exited with code 0
```

## Migración: ciclo upgrade → downgrade → upgrade

```
$ docker compose -f compose.test.yaml run --rm migrate sh -c \
    "alembic upgrade head && alembic downgrade -1 && alembic upgrade head"
...
INFO  [alembic.runtime.migration] Running upgrade e60012acd532 -> a4d8f9c1e6b2, history: ...
INFO  [alembic.runtime.migration] Running downgrade a4d8f9c1e6b2 -> e60012acd532, history: ...
INFO  [alembic.runtime.migration] Running upgrade e60012acd532 -> a4d8f9c1e6b2, history: ...
```

Ciclo completo sin errores (los dos `CREATE INDEX`/`DROP INDEX` de `a4d8f9c1e6b2`).

## Android: compileDebugKotlin

```
$ ./gradlew compileDebugKotlin
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 42s
17 actionable tasks: 1 executed, 16 up-to-date
```

Sin errores de compilación en `HistoryClient.kt` ni en los cambios de `TutorScreen.kt`.

## Web: npm run lint / tsc / npm run build

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — cero advertencias)

$ npx tsc --noEmit
(sin salida — cero errores de tipos)

$ npm run build
▲ Next.js 16.3.3 (Turbopack)
✓ Compiled successfully in 26.1s
  Running TypeScript ...
  Finished TypeScript in 3.5s ...
✓ Generating static pages using 4 workers (3/3) in 1163ms
```

Sin errores de tipos ni de lint en `apiClient.ts`, `HistoryPanel.tsx` ni `DevicesPanel.tsx`.

## CI en GitHub Actions (runner limpio)

Pendiente: se completa en un commit posterior tras el push, una vez confirmado el run en verde
(mismo patrón que "docs: record green CI run" de los Sprints 13 y 14).

## No se marca como verificado

Ver la sección homónima en `docs/sprint-15.md`: login real de Google, y recorrido manual de la
pantalla de Android (se compiló y se verificó la lógica de consumo de la API contra el backend
real por integración, pero no se navegó la UI a mano en un emulador en esta sesión).

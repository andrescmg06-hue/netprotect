# Sprint 16 — Evidencia

## Backend: ruff

```
$ python -m ruff check tests/test_statistics_integration.py app alembic
All checks passed!
```

Primera corrida encontró 3 líneas largas (E501) en el archivo de pruebas nuevo; corregidas
partiendo las expresiones en varias líneas, sin cambiar la lógica.

## Backend: pruebas de integración en Docker (PostgreSQL/Redis reales)

Secuencia obligatoria del proyecto (build → migrate → up, nunca `up --build` con `migrate` en
`depends_on`):

```
$ docker compose -f compose.test.yaml build backend
...
 Image netprotect-test-backend Built

$ docker compose -f compose.test.yaml run --rm migrate
...
INFO  [alembic.runtime.migration] Running upgrade e60012acd532 -> a4d8f9c1e6b2, history: ...
(sin migración nueva de este sprint — no se tocó ningún modelo)

$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 39%]
backend-1  | ........................................................................ [ 78%]
backend-1  | ...................................F....                                 [100%]
backend-1  | FAILED tests/test_statistics_integration.py::test_category_daily_limit_compliance_sums_usage_across_member_apps
backend-1  | assert 1 == 0
backend-1  | 1 failed, 183 passed, 5 warnings in 41.94s
```

**Error real encontrado y corregido**: el primer intento de
`test_category_daily_limit_compliance_sums_usage_across_member_apps` sembraba 300s + 300s = 600s de
uso contra un límite de 10 minutos (600s) — exactamente en el límite, así que `<=` lo marcaba como
cumplido (`days_compliant == 1`), no como violación. La prueba esperaba `0` por error de cálculo
propio, no del endpoint. Se corrigió el dato sembrado a 400s + 400s = 800s (> 600s), una violación
real, y se documentó en el test por qué (la regla de categoría suma el uso de sus apps miembro,
cada una por debajo del límite individualmente pero no en conjunto).

Segunda corrida, en verde:

```
$ docker compose -f compose.test.yaml build backend
$ docker compose -f compose.test.yaml run --rm migrate
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 39%]
backend-1  | ........................................................................ [ 78%]
backend-1  | ........................................                                 [100%]
backend-1  | 184 passed, 4 warnings in 77.95s (0:01:17)
backend-1 exited with code 0
```

184 pruebas (183 preexistentes + las nuevas de estadísticas), todas en verde contra PostgreSQL y
Redis reales en contenedor.

## Frontend

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — limpio)

$ npm run build
> next build
✓ Compiled successfully in 32.1s
  Running TypeScript ...
  Finished TypeScript in 9.3s ...
✓ Generating static pages using 4 workers (3/3) in 2.3s
```

## Android

```
$ ./gradlew compileDebugKotlin
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 19s
```

Compila `StatisticsClient.kt` y los cambios en `TutorScreen.kt` sin errores.

## Pendiente antes de cerrar el sprint

- `/security-review` sobre el diff completo.
- Push y verificación de los 4 jobs de CI en GitHub Actions en verde.
- Actualizar `CLAUDE.md` ("Estado actual" y "Siguiente sprint").

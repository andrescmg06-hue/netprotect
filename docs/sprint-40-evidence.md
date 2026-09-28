# Sprint 40 — Evidencia

Las mediciones las ejecutó un agente con el rol de `verifier` (sin editar el repo); aquí se resumen
sus salidas. Log completo de Gradle en la carpeta temporal de la sesión, fuera del repo.

## 1. Android — `cd mobile && ./gradlew test assembleDebug lintDebug`

```text
BUILD SUCCESSFUL — 86 tareas (9 ejecutadas, 77 up-to-date), 45 s, exit 0
Tests: 35 de 35 OK, 0 fallos (ExampleUnitTest 1, RuleEvaluatorTest 34)
Lint: 0 errores, 27 avisos
```

Notas honestas: `:app:testDebugUnitTest` y `assembleDebug` salieron UP-TO-DATE (resultados del
27-sep con las mismas entradas); `:app:testReleaseUnitTest` y `lintDebug` sí se ejecutaron ahora y
dieron 35/35. Avisos por tipo: UseKtx 11, GradleDependency 9, NewerVersionAvailable 3,
MissingApplicationIcon 1, DataExtractionRules 1, CredentialManagerMisuse 1, AndroidGradlePluginVersion 1.
Los relevantes están en `docs/tasks.md` (L-01…L-04).

## 2. Backend

```text
ruff check app tests alembic        -> All checks passed!  (exit 0, <1 s)
pytest -q -m "not integration"      -> Interrupted: 21 errors during collection (exit 2)
                                       ModuleNotFoundError: prometheus_client
```

**No hay conteo de tests de backend en local**: falta `prometheus_client` en `backend/.venv`
(`requirements.txt:22`, importado en `app/core/metrics.py:27`). No se instaló nada. Registrado como L-05.

## 3. CI del PR #2 (`sprint-31-design-system` → `main`, commit `f913713`)

```text
backend pass · frontend pass · integration pass · api-collection pass
performance pass · e2e pass · android pending · android-instrumented pending
```

(Estado en el momento de escribir esto; los dos jobs de Android seguían en ejecución. Se anota el
resultado final al cerrar el sprint.)

## 4. No verificado

- Tests instrumentados de Android en local (sin emulador levantado).
- `make test` en local (ver Decisiones en `docs/sprint-40.md`).
- Prueba de `opencode.json` leyendo `.env` desde OpenCode: **pendiente del dueño**.

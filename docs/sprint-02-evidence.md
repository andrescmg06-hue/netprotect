# Evidencia de verificación — Sprint 2

Fecha: 03/09/2026. Equipo: `andre`, Windows 11 Pro. Docker Desktop 29.7.2 (Compose v5.1.4).

## Migración aplicada contra PostgreSQL real

```text
alembic upgrade head
→ Running upgrade  -> e27f40867c61, core schema: users roles devices pairing sessions audit

docker compose exec db psql -U netprotect -d netprotect -c "\dt"
→ audit_logs, device_status, devices, pairing_codes, roles, sessions, tutor_devices, user_roles, users
  (9 tablas + alembic_version)

docker compose exec db psql -U netprotect -d netprotect -c "select * from roles;"
→ TUTOR       | Administra y supervisa dispositivos vinculados.
  SUPERVISADO | Dispositivo vinculado que recibe y aplica políticas del tutor.
```

## Reversibilidad

```text
alembic downgrade -1
→ Running downgrade e27f40867c61 -> , core schema: ...

docker compose exec db psql -U netprotect -d netprotect -c "\dt"
→ sólo alembic_version (las 9 tablas se eliminaron limpiamente)

alembic upgrade head   # vuelto a aplicar para dejar el entorno listo
```

## Pruebas locales

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q
→ 9 passed

ruff check app tests alembic
→ All checks passed!
```

Incluye las dos pruebas nuevas de `tests/test_models_integration.py`:
`test_roles_are_seeded_by_migration` y `test_pairing_and_linking_graph_round_trips` (esta última
inserta usuarios, roles, dispositivo, estado, vínculo tutor-dispositivo y código de vinculación, y los
vuelve a consultar con `selectinload`).

## Flujo completo en Docker (servicio `migrate` antes de `backend`)

Reconstrucción completa desde cero (`docker compose down -v` + `up --build -d`):

```text
Container netprotect-dev-migrate-1  Started → Exited (código 0)
Container netprotect-dev-backend-1  Starting → Healthy (esperó a migrate)

docker inspect netprotect-dev-migrate-1 --format "{{.State.ExitCode}}" → 0
GET /api/v1/health/ready → {"status":"ready", ...}
```

Confirma que `backend` con `depends_on: migrate: condition: service_completed_successfully` realmente
espera a que la migración termine con éxito antes de arrancar.

## compose.test.yaml — hallazgo y corrección

Al ejecutar la integración por primera vez con el nuevo servicio `migrate`, `docker compose -f
compose.test.yaml config` reveló que `DATABASE_URL` y `REDIS_URL` se resolvían con los valores del
`.env` de **desarrollo** (base `netprotect`, no `netprotect_test`), porque Docker Compose carga
automáticamente el `.env` de la raíz del proyecto sin importar qué archivo `-f` se use, y
`compose.test.yaml` usaba `${VAR:-default}` para esos valores. Se corrigió quitando toda interpolación
de variables en `compose.test.yaml`: al ser un stack efímero y desechable, no tiene ninguna razón
legítima para leer el `.env` de nadie.

## `compose.test.yaml` — bug real encontrado en CI, con un diagnóstico local equivocado corregido

Al ejecutar `compose.test.yaml` localmente varias veces seguidas, el resultado fue inconsistente:
2 de 3 corridas pasaron limpio (`9 passed`) y 1 falló con `socket.gaierror: Name or service not known`
al resolver el host `db`. La hipótesis inicial, registrada aquí por error, fue "inestabilidad
intermitente de red de Docker Desktop en Windows". **Esa hipótesis era incorrecta** y se corrige en
este mismo documento en vez de dejarla escrita: al hacer push, el job `integration` de GitHub Actions
—Linux, Docker Engine real, sin Docker Desktop— falló **de forma consistente y determinística**, no
intermitente, revelando la causa real.

**Causa real:** `docker compose up --build --abort-on-container-exit --exit-code-from backend` aborta
todo el stack en cuanto **cualquier** contenedor termina, sin distinguir un servicio que termina por
diseño (`migrate`, que aplica la migración y sale) de uno que debe seguir corriendo. Como `backend`
tenía `depends_on: migrate: condition: service_completed_successfully`, Compose no arrancaba `backend`
hasta que `migrate` terminara — es decir, `migrate` **siempre** termina antes de que `backend` arranque,
lo cual dispara el aborto de inmediato. El log de CI lo muestra sin ambigüedad: `migrate-1` corre y
termina con éxito, aparece "Aborting on container exit...", y `backend-1` nunca llega a imprimir una
sola línea, ni siquiera la cabecera de pytest.

Que localmente pasara 2 de 3 veces fue casualidad de temporización (imágenes ya construidas en caché
hacían que `backend` alcanzara a correr sus 9 pruebas —tardan bajo un segundo— antes de que el
observador de aborto de Compose reaccionara a la salida de `migrate`); en un runner en frío como el de
CI, sin ese margen, la carrera se pierde siempre.

**Corrección:** se quitó `migrate` de `depends_on` en `backend` dentro de `compose.test.yaml`, y la
migración pasó a ejecutarse aparte, con `docker compose -f compose.test.yaml run --rm migrate` — que no
participa del `--abort-on-container-exit` porque no es parte de ese `up` — seguido de
`docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend`,
limitando explícitamente qué servicios arrancan. Actualizado en `.github/workflows/ci.yml`, `Makefile` y
`docs/environments.md`.

Verificado localmente, dos corridas limpias consecutivas con la secuencia corregida:

```text
docker compose -f compose.test.yaml run --rm migrate
→ Running upgrade  -> e27f40867c61, ...

docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
→ backend-1 | 9 passed, 3 warnings in 0.7x s
→ backend-1 exited with code 0
```

`compose.yaml` (desarrollo) no tenía este problema: usa `up --build -d` (sin `--abort-on-container-exit`),
así que `migrate` como dependencia de `backend` siempre funcionó correctamente ahí y no se tocó.

## CI en GitHub Actions

Primer intento (`4d142be`, antes de esta corrección): falló en el job `integration` por la causa
descrita arriba; `android`, `backend` y `frontend` pasaron.

Segundo intento (`560131c`, con la corrección aplicada), run `33822070006`: los 4 jobs en verde.

```text
✓ android       1m07s   ./gradlew test assembleDebug
✓ backend         ~20s  ruff check + pytest
✓ frontend        ~30s  npm ci + lint + build
✓ integration       39s docker compose build → run --rm migrate → up --abort-on-container-exit ...
```

El job `integration` muestra explícitamente los cuatro pasos nuevos en secuencia: `build`,
`run --rm migrate`, el `up --abort-on-container-exit ... db redis backend` acotado, y `down -v`.

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con su salida real, incluido el diagnóstico
inicial equivocado y su corrección — no se oculta el error de análisis, se corrige explícitamente.
Sprint 2 queda cerrado: los 4 jobs de CI en verde en un runner limpio de GitHub Actions, además de la
verificación local completa (migración, reversibilidad, pruebas, ruff).

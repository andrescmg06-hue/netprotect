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

## Ejecución de `compose.test.yaml` — resultado real, con una falla intermitente documentada

Se ejecutó tres veces de forma consecutiva, con `docker compose -f compose.test.yaml down -v` entre
cada corrida:

| Corrida | Resultado | Causa |
|---|---|---|
| 1 | `5 failed, 4 passed` | `socket.gaierror: Name or service not known` al resolver el host `db` desde el contenedor `backend` |
| 2 | `9 passed` | — |
| 3 | `9 passed` | — |

La corrida 1 ocurrió **antes** de corregir la filtración de `.env` descrita arriba, pero al repetir la
misma prueba ya corregida se obtuvo el mismo error una vez más antes de que dos corridas consecutivas
pasaran limpio con exactamente la misma configuración. Esto descarta que la filtración de `.env` fuera
la causa raíz: es un fallo intermitente de resolución DNS del propio Docker Desktop para Windows en este
equipo, que ya se había puesto inestable antes en esta sesión de trabajo (requirió un reinicio manual
documentado en `docs/sprint-01-evidence.md`). El servicio `migrate` —que también resuelve el mismo host
`db`— nunca falló en ninguna de las tres corridas, lo que refuerza que no es un problema de la
configuración de red de Compose sino del motor de Docker Desktop en sí.

No se oculta este resultado: se registra tal cual, y el criterio definitivo de aceptación es la corrida
en GitHub Actions (Linux, Docker Engine real, sin Docker Desktop de por medio), que se registra a
continuación.

## CI en GitHub Actions

Pendiente de push al cierre de este documento; se actualiza esta sección con el resultado del run en
cuanto se ejecute.

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con su salida real, incluida la falla intermitente
de la corrida 1 de `compose.test.yaml`, registrada sin maquillar. La sección de CI se completa sólo
después de que el pipeline corra de verdad.

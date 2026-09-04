# Sprint 2 — Base de datos

## Objetivo

Pasar del esquema conceptual (`docs/diagrams/05-modelo-datos-conceptual.md`) a un esquema físico
versionado y migrable con Alembic, cubriendo las tablas núcleo: `users`, `roles`, `user_roles`,
`devices`, `device_status`, `tutor_devices`, `pairing_codes`, `sessions`, `audit_logs`. El resto de las
25 tablas se crea en el sprint que las use.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-006 | Como equipo, quiero un esquema versionado con migraciones para poder evolucionar la base de datos sin perder datos ni depender de `create_all`. |
| HU-007 | Como desarrollador, quiero los roles TUTOR y SUPERVISADO precargados para no crearlos a mano en cada entorno. |
| HU-008 | Como desarrollador, quiero que las migraciones corran automáticamente antes de que arranque el backend, en desarrollo y en pruebas. |

## Criterios de aceptación

1. `alembic upgrade head` crea las 9 tablas núcleo con sus claves foráneas, índices y restricciones.
2. `alembic downgrade -1` revierte limpiamente, sin dejar tablas huérfanas.
3. El backend no usa `Base.metadata.create_all` en ningún punto.
4. Un servicio `migrate` corre las migraciones antes de que `backend` arranque, tanto en
   `compose.yaml` como en `compose.test.yaml`.
5. Los roles `TUTOR` y `SUPERVISADO` existen tras la migración inicial, sin script aparte.
6. Existe una prueba de integración que inserta un grafo real (usuario, rol, dispositivo, estado,
   vínculo tutor-dispositivo, código de vinculación) y lo consulta de vuelta.
7. `ruff` y la suite de pruebas pasan, incluidas las nuevas.
8. Ninguna URL de conexión ni credencial queda escrita en `alembic.ini`.

## Tareas técnicas y estado

| Tarea | Estado | Evidencia |
|---|---|---|
| Modelos SQLAlchemy 2.0 (9 tablas núcleo) | Implementado | `backend/app/models/` |
| Alembic (plantilla async) | Configurado | `backend/alembic/` |
| `env.py` sin credenciales, URL desde `settings` | Implementado | `backend/alembic/env.py` |
| Migración inicial con siembra de roles | Implementada | `backend/alembic/versions/e27f40867c61_*.py` |
| Servicio `migrate` en Docker Compose (dev y test) | Implementado | `compose.yaml`, `compose.test.yaml` |
| Dockerfile incluye Alembic en `runtime` y `test` | Implementado | `backend/Dockerfile` |
| Prueba de integración de modelos | Implementada | `backend/tests/test_models_integration.py` |
| ER físico documentado | Implementado | `docs/diagrams/06-modelo-datos-fisico-sprint2.md` |
| `pytest-asyncio` en modo `auto` | Configurado | `backend/pyproject.toml` |
| `make migrate` / `make revision` | Implementado | `Makefile` |

## Decisiones de diseño relevantes

Ver `docs/diagrams/06-modelo-datos-fisico-sprint2.md` para el detalle completo. Las más relevantes:

- UUID generados en Python, no en PostgreSQL (sin dependencia de extensiones).
- `device_status` separada de `devices` por frecuencia de escritura (heartbeat vs. atributos estáticos).
- Índice único parcial en `tutor_devices` (sólo vínculos activos), permitiendo re-vincular tras desvincular.
- `pairing_codes.code_hash` deliberadamente **no** único: con sólo 1 000 000 de combinaciones de 6
  dígitos, exigir unicidad global impediría reutilizar un código tras su expiración. La invariante de
  "un código activo a la vez" se decide en la lógica de aplicación del Sprint 5, no en el esquema.
- Ningún `users` se borra en cascada de forma incondicional: `devices.supervised_user_id` usa
  `RESTRICT` para no perder dispositivos supervisados si se elimina la cuenta por error.

## Ejecución

```bash
docker compose up --build -d
```

El servicio `migrate` corre `alembic upgrade head` y debe terminar con código 0 antes de que `backend`
arranque. Para aplicar o revertir migraciones manualmente:

```bash
make migrate
# o, contra una base ya corriendo:
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
alembic upgrade head
alembic downgrade -1
```

Para crear una nueva migración a partir de cambios en los modelos:

```bash
make revision m="descripción del cambio"
```

## Verificación

```bash
docker compose exec db psql -U netprotect -d netprotect -c "\dt"
docker compose exec db psql -U netprotect -d netprotect -c "select * from roles;"
```

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q
ruff check app tests alembic
```

## Seguridad del sprint

- Ninguna URL de conexión ni credencial se escribe en `alembic.ini`; se toma en tiempo de ejecución de
  `app.core.config.settings`, que a su vez lee variables de entorno.
- `sessions.refresh_token_hash` y `pairing_codes.code_hash` almacenan hashes, nunca secretos en texto
  plano (el hashing real se implementa en los sprints 3 y 5, que son quienes generan esos valores).
- El backend nunca ejecuta DDL implícito (`create_all`); todo cambio de esquema queda versionado y
  revisable en `alembic/versions/`.

## Fuera de alcance

Autenticación con Google (Sprint 3), RBAC funcional (Sprint 4), vinculación por código (Sprint 5) y el
resto de las 25 tablas del modelo conceptual, que se crean en el sprint que las necesite.

## Definition of Done del Sprint 2

Todos los criterios de aceptación tienen evidencia ejecutada realmente: migración aplicada y revertida
contra PostgreSQL real, roles sembrados verificados por consulta directa, prueba de integración en
verde, y `ruff` limpio. El flujo completo (`db` → `migrate` → `backend`) se probó de punta a punta en
Docker; una primera versión usaba `migrate` como dependencia de `backend` dentro del mismo
`up --abort-on-container-exit`, lo que aborta el stack en cuanto `migrate` termina —por diseño, antes de
que `backend` llegue a correr sus pruebas— y CI lo detectó de forma consistente. Corregido separando la
migración (`run --rm migrate`) del `up` que vigila el abort; ver el diagnóstico completo, incluida la
hipótesis inicial equivocada que se corrigió, en `docs/sprint-02-evidence.md`. CI en GitHub Actions es
el criterio definitivo.

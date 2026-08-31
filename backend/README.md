# NetProtect Backend — Sprint 1

API FastAPI central de NetProtect.

## Endpoints

- `GET /api/v1/health`: proceso API vivo.
- `GET /api/v1/health/db`: conexión PostgreSQL.
- `GET /api/v1/health/redis`: conexión Redis.
- `GET /api/v1/health/ready`: readiness de API + PostgreSQL + Redis.

## Desarrollo directo

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
pytest -q -m "not integration"
ruff check app tests
uvicorn app.main:app --reload
```

Para ejecutar integración se requieren PostgreSQL y Redis configurados mediante `DATABASE_URL` y `REDIS_URL`.

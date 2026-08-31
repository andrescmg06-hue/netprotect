import os

import pytest
from fastapi.testclient import TestClient

from app.main import app

pytestmark = pytest.mark.integration


@pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION_TESTS") != "1",
    reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
)
def test_database_health_endpoint() -> None:
    with TestClient(app) as client:
        response = client.get("/api/v1/health/db")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "connected"}


@pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION_TESTS") != "1",
    reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
)
def test_redis_health_endpoint() -> None:
    with TestClient(app) as client:
        response = client.get("/api/v1/health/redis")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "redis": "connected"}


@pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION_TESTS") != "1",
    reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
)
def test_readiness_endpoint() -> None:
    with TestClient(app) as client:
        response = client.get("/api/v1/health/ready")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ready",
        "backend": "connected",
        "database": "connected",
        "redis": "connected",
    }

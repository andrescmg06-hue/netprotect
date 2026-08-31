from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_endpoint() -> None:
    response = client.get("/api/v1/health")

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "ok"
    assert payload["service"] == "NetProtect API"
    assert "X-Request-ID" in response.headers


def test_unknown_route_returns_404() -> None:
    response = client.get("/api/v1/does-not-exist")
    assert response.status_code == 404

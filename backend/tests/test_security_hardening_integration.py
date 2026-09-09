"""Sprint 21 — endurecimiento: rate limiting global, cabeceras, HTTPS y errores genéricos.

Needs real Redis: the global limiter and the auth limiters both count in it, and the whole point
of these tests is that the counting really happens (a mocked counter would prove nothing).
"""

import os
import uuid
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
    ),
]


@pytest.fixture
def client():
    """One client per test with its own source host — same reasoning as the pairing suite:
    rate-limit counters live in Redis for minutes, so a shared address would let one test spend
    another's budget, and would carry over between runs."""
    with TestClient(app, client=(f"test-{uuid.uuid4().hex}", 51000)) as test_client:
        yield test_client


@pytest.fixture
def lenient_client():
    """A client whose server errors come back as 500 responses instead of re-raising."""
    with TestClient(
        app,
        client=(f"test-{uuid.uuid4().hex}", 51000),
        raise_server_exceptions=False,
    ) as test_client:
        yield test_client


def _login_body() -> dict[str, str]:
    return {"id_token": "fake"}


def _identity() -> GoogleIdentity:
    unique = uuid.uuid4().hex[:10]
    return GoogleIdentity(
        google_sub=f"google-{unique}",
        email=f"{unique}@example.com",
        display_name="Usuaria de prueba",
        avatar_url=None,
    )


# --------------------------------------------------------------- rate limiting global


def test_the_global_limiter_rejects_once_over_the_budget(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "rate_limit_global_max_per_ip", 3)

    codes = [client.get("/").status_code for _ in range(4)]

    assert codes == [200, 200, 200, 429]


def test_a_rejection_says_when_to_come_back(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "rate_limit_global_max_per_ip", 1)
    monkeypatch.setattr(settings, "rate_limit_global_window_seconds", 45)

    client.get("/")
    denied = client.get("/")

    assert denied.status_code == 429
    assert denied.json()["detail"] == "too_many_requests"
    assert 0 < int(denied.headers["Retry-After"]) <= 45


def test_health_is_never_rate_limited(client, monkeypatch) -> None:
    """Health checks are what an orchestrator polls to decide whether this process is alive.
    Rate-limiting them would make a burst of traffic look like a dead container."""
    monkeypatch.setattr(settings, "rate_limit_global_max_per_ip", 1)

    codes = [client.get("/api/v1/health").status_code for _ in range(5)]

    assert codes == [200] * 5


def test_a_rejected_request_still_carries_the_security_headers(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "rate_limit_global_max_per_ip", 1)

    client.get("/")
    denied = client.get("/")

    assert denied.status_code == 429
    assert "X-Request-ID" in denied.headers
    assert denied.headers["X-Content-Type-Options"] == "nosniff"


# ------------------------------------------------------------------ rate limiting auth


def test_login_is_rate_limited_per_ip(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "auth_login_max_per_ip", 2)

    with patch(
        "app.api.v1.endpoints.auth.verify_google_id_token", side_effect=lambda _: _identity()
    ):
        codes = [
            client.post("/api/v1/auth/google", json=_login_body()).status_code for _ in range(3)
        ]

    assert codes == [200, 200, 429]


def test_token_refresh_is_rate_limited_per_ip(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "auth_refresh_max_per_ip", 1)

    first = client.post("/api/v1/auth/refresh", json={"refresh_token": "not-a-real-token"})
    second = client.post("/api/v1/auth/refresh", json={"refresh_token": "not-a-real-token"})

    # The first is rejected on its merits (401), the second never even gets that far.
    assert first.status_code == 401
    assert second.status_code == 429


# ------------------------------------------------------------------------- cabeceras


def test_a_normal_response_carries_the_defensive_headers(client) -> None:
    response = client.get("/api/v1/health")

    assert response.headers["X-Content-Type-Options"] == "nosniff"
    assert response.headers["X-Frame-Options"] == "DENY"
    assert response.headers["Referrer-Policy"] == "no-referrer"
    assert response.headers["Permissions-Policy"] == "geolocation=(), camera=(), microphone=()"


def test_hsts_and_csp_are_production_only(client, monkeypatch) -> None:
    """Outside production /docs is served and loads its assets from a CDN, which
    `default-src 'none'` would block — so both headers are withheld there on purpose."""
    outside_production = client.get("/api/v1/health")
    assert "Strict-Transport-Security" not in outside_production.headers
    assert "Content-Security-Policy" not in outside_production.headers

    monkeypatch.setattr(settings, "app_env", "production")
    with TestClient(
        app, base_url="https://testserver", client=(f"test-{uuid.uuid4().hex}", 51000)
    ) as https_client:
        in_production = https_client.get("/api/v1/health")

    assert in_production.headers["Strict-Transport-Security"] == (
        "max-age=63072000; includeSubDomains"
    )
    assert in_production.headers["Content-Security-Policy"] == (
        "default-src 'none'; frame-ancestors 'none'"
    )


# ------------------------------------------------------------------- TLS obligatorio


def test_production_refuses_plain_http(client, monkeypatch) -> None:
    monkeypatch.setattr(settings, "app_env", "production")

    response = client.get("/")

    assert response.status_code == 400
    assert response.json()["detail"] == "https_required"


def test_production_accepts_https(monkeypatch) -> None:
    monkeypatch.setattr(settings, "app_env", "production")

    with TestClient(
        app, base_url="https://testserver", client=(f"test-{uuid.uuid4().hex}", 51000)
    ) as https_client:
        response = https_client.get("/")

    assert response.status_code == 200


# ----------------------------------------------------------- errores y validación


def test_an_unexpected_error_never_leaks_its_detail(lenient_client) -> None:
    with patch(
        "app.api.v1.endpoints.auth.verify_google_id_token",
        side_effect=RuntimeError("connection string postgres://user:hunter2@db/netprotect"),
    ):
        response = lenient_client.post("/api/v1/auth/google", json=_login_body())

    assert response.status_code == 500
    assert response.json()["detail"] == "internal_error"
    assert "hunter2" not in response.text
    assert response.json()["request_id"] is not None


def test_an_oversized_id_token_is_rejected_before_anything_touches_it(client) -> None:
    response = client.post("/api/v1/auth/google", json={"id_token": "A" * 4097})

    assert response.status_code == 422


def test_an_oversized_refresh_token_is_rejected(client) -> None:
    response = client.post("/api/v1/auth/refresh", json={"refresh_token": "A" * 513})

    assert response.status_code == 422

import os
import uuid
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL.",
    ),
]


def _login() -> str:
    unique = uuid.uuid4().hex[:8]
    identity = GoogleIdentity(
        google_sub=f"google-sub-{unique}",
        email=f"user-{unique}@example.com",
        display_name="Usuaria de Prueba",
        avatar_url=None,
    )
    with patch("app.api.v1.endpoints.auth.verify_google_id_token", return_value=identity):
        with TestClient(app) as client:
            response = client.post("/api/v1/auth/google", json={"id_token": "fake"})
    assert response.status_code == 200
    return response.json()["access_token"]


def test_roles_require_authentication() -> None:
    with TestClient(app) as client:
        response = client.get("/api/v1/users/me/roles")
    assert response.status_code == 401


def test_new_user_has_no_roles() -> None:
    access_token = _login()
    with TestClient(app) as client:
        response = client.get(
            "/api/v1/users/me/roles", headers={"Authorization": f"Bearer {access_token}"}
        )
    assert response.status_code == 200
    assert response.json()["roles"] == []


def test_selecting_tutor_grants_it_and_is_idempotent() -> None:
    access_token = _login()
    headers = {"Authorization": f"Bearer {access_token}"}

    with TestClient(app) as client:
        first = client.post(
            "/api/v1/users/me/roles", json={"role_code": "TUTOR"}, headers=headers
        )
        assert first.status_code == 200
        assert first.json()["role_code"] == "TUTOR"

        second = client.post(
            "/api/v1/users/me/roles", json={"role_code": "TUTOR"}, headers=headers
        )
        assert second.status_code == 200
        assert second.json()["granted_at"] == first.json()["granted_at"]

        listed = client.get("/api/v1/users/me/roles", headers=headers)
        assert [r["role_code"] for r in listed.json()["roles"]] == ["TUTOR"]


def test_a_user_can_hold_both_roles() -> None:
    access_token = _login()
    headers = {"Authorization": f"Bearer {access_token}"}

    with TestClient(app) as client:
        client.post("/api/v1/users/me/roles", json={"role_code": "TUTOR"}, headers=headers)
        client.post(
            "/api/v1/users/me/roles", json={"role_code": "SUPERVISADO"}, headers=headers
        )
        listed = client.get("/api/v1/users/me/roles", headers=headers)

    codes = {r["role_code"] for r in listed.json()["roles"]}
    assert codes == {"TUTOR", "SUPERVISADO"}


def test_unknown_role_code_is_rejected() -> None:
    access_token = _login()
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/users/me/roles",
            json={"role_code": "ADMIN"},
            headers={"Authorization": f"Bearer {access_token}"},
        )
    assert response.status_code == 400

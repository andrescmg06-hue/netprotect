import os
import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import patch

import jwt
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.config import settings
from app.main import app
from app.models import AuditLog, User
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL.",
    ),
]


def _fake_identity(unique: str) -> GoogleIdentity:
    return GoogleIdentity(
        google_sub=f"google-sub-{unique}",
        email=f"user-{unique}@example.com",
        display_name="Usuaria de Prueba",
        avatar_url="https://example.com/avatar.png",
    )


def _login(unique: str) -> dict:
    identity = _fake_identity(unique)
    with patch(
        "app.api.v1.endpoints.auth.verify_google_id_token", return_value=identity
    ):
        with TestClient(app) as client:
            response = client.post("/api/v1/auth/google", json={"id_token": "fake"})
    assert response.status_code == 200
    return response.json()


async def test_google_login_creates_user_and_issues_tokens(db_session) -> None:
    unique = uuid.uuid4().hex[:8]
    tokens = _login(unique)

    assert set(tokens) == {"access_token", "refresh_token", "token_type", "expires_in"}
    assert tokens["token_type"] == "bearer"  # noqa: S105 -- OAuth2 field value, not a secret

    identity = _fake_identity(unique)
    user = (
        await db_session.execute(select(User).where(User.google_sub == identity.google_sub))
    ).scalar_one()
    assert user.email == identity.email

    audit_row = (
        await db_session.execute(
            select(AuditLog).where(AuditLog.actor_user_id == user.id, AuditLog.action == "LOGIN")
        )
    ).scalar_one_or_none()
    assert audit_row is not None


def test_me_requires_a_valid_access_token() -> None:
    unique = uuid.uuid4().hex[:8]
    tokens = _login(unique)

    with TestClient(app) as client:
        no_auth = client.get("/api/v1/auth/me")
        assert no_auth.status_code == 401

        ok = client.get(
            "/api/v1/auth/me", headers={"Authorization": f"Bearer {tokens['access_token']}"}
        )
        assert ok.status_code == 200
        assert ok.json()["email"] == _fake_identity(unique).email


def test_tampered_and_expired_access_tokens_are_rejected() -> None:
    unique = uuid.uuid4().hex[:8]
    tokens = _login(unique)

    # Flip a character in the middle of the token, not the last one: the tail end of a
    # base64url-encoded HMAC-SHA256 signature has a few bits that don't map to any real
    # signature byte (32 bytes doesn't divide evenly into 6-bit groups), so occasionally
    # "changing" the very last character leaves the decoded signature bytes identical.
    access_token = tokens["access_token"]
    middle = len(access_token) // 2
    replacement = "a" if access_token[middle] != "a" else "b"
    tampered = access_token[:middle] + replacement + access_token[middle + 1 :]

    expired_payload = {
        "sub": str(uuid.uuid4()),
        "type": "access",
        "iat": datetime.now(UTC) - timedelta(hours=1),
        "exp": datetime.now(UTC) - timedelta(minutes=1),
    }
    expired = jwt.encode(expired_payload, settings.jwt_secret, algorithm="HS256")

    with TestClient(app) as client:
        tampered_resp = client.get(
            "/api/v1/auth/me", headers={"Authorization": f"Bearer {tampered}"}
        )
        expired_resp = client.get(
            "/api/v1/auth/me", headers={"Authorization": f"Bearer {expired}"}
        )

    assert tampered_resp.status_code == 401
    assert expired_resp.status_code == 401


def test_refresh_rotates_the_token_and_invalidates_the_old_one() -> None:
    unique = uuid.uuid4().hex[:8]
    tokens = _login(unique)

    with TestClient(app) as client:
        refreshed = client.post(
            "/api/v1/auth/refresh", json={"refresh_token": tokens["refresh_token"]}
        )
        assert refreshed.status_code == 200
        new_tokens = refreshed.json()
        assert new_tokens["refresh_token"] != tokens["refresh_token"]
        assert new_tokens["access_token"] != tokens["access_token"]

        reuse_old = client.post(
            "/api/v1/auth/refresh", json={"refresh_token": tokens["refresh_token"]}
        )
        assert reuse_old.status_code == 401

        works_again = client.post(
            "/api/v1/auth/refresh", json={"refresh_token": new_tokens["refresh_token"]}
        )
        assert works_again.status_code == 200


def test_logout_revokes_the_refresh_token() -> None:
    unique = uuid.uuid4().hex[:8]
    tokens = _login(unique)

    with TestClient(app) as client:
        logout_resp = client.post(
            "/api/v1/auth/logout", json={"refresh_token": tokens["refresh_token"]}
        )
        assert logout_resp.status_code == 204

        refresh_after_logout = client.post(
            "/api/v1/auth/refresh", json={"refresh_token": tokens["refresh_token"]}
        )
        assert refresh_after_logout.status_code == 401

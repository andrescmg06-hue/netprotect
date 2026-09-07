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
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
    ),
]


@pytest.fixture
def client():
    with TestClient(app, client=(f"test-{uuid.uuid4().hex}", 51000)) as test_client:
        yield test_client


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _make_account(client: TestClient, role: str) -> tuple[str, str]:
    unique = uuid.uuid4().hex[:10]
    identity = GoogleIdentity(
        google_sub=f"google-{unique}",
        email=f"{unique}@example.com",
        display_name=f"Usuaria {unique[:4]}",
        avatar_url=None,
    )
    with patch("app.api.v1.endpoints.auth.verify_google_id_token", return_value=identity):
        login = client.post("/api/v1/auth/google", json={"id_token": "fake"})
    assert login.status_code == 200
    token = login.json()["access_token"]

    granted = client.post(
        "/api/v1/users/me/roles", json={"role_code": role}, headers=_auth(token)
    )
    assert granted.status_code == 200
    return token, identity.email


def _link_a_device(client: TestClient, tutor_token: str, supervised_token: str) -> str:
    code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).json()["code"]
    redeemed = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": code,
            "device_instance_id": uuid.uuid4().hex,
            "device_name": "Celular de Juan",
            "platform": "ANDROID",
            "os_version": "16",
            "app_version": "0.1.0",
        },
        headers=_auth(supervised_token),
    )
    assert redeemed.status_code == 200, redeemed.text
    return redeemed.json()["device_id"]


def _setup_linked_device(client: TestClient) -> tuple[str, str, str]:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    return tutor_token, supervised_token, device_id


def _set_school_mode(client: TestClient, token: str, device_id: str, **fields):
    return client.put(
        f"/api/v1/devices/{device_id}/school-mode", json=fields, headers=_auth(token)
    )


def test_a_new_device_has_school_mode_disabled(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))

    assert detail.json()["school_mode"] == {
        "enabled": False,
        "start_minute": None,
        "end_minute": None,
        "days_mask": None,
    }


def test_a_tutor_can_enable_school_mode_with_a_window(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_school_mode(
        client,
        tutor_token,
        device_id,
        enabled=True,
        start_minute=7 * 60,
        end_minute=14 * 60,
        days_mask=0b0011111,
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["enabled"] is True
    assert body["start_minute"] == 7 * 60
    assert body["end_minute"] == 14 * 60


def test_enabling_school_mode_without_a_window_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_school_mode(client, tutor_token, device_id, enabled=True)

    assert response.status_code == 422


def test_disabling_school_mode_clears_the_window(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _set_school_mode(
        client, tutor_token, device_id, enabled=True, start_minute=420, end_minute=840, days_mask=31
    )

    response = _set_school_mode(client, tutor_token, device_id, enabled=False)

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["enabled"] is False
    assert body["start_minute"] is None


def test_a_stranger_tutor_cannot_change_school_mode(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _set_school_mode(
        client,
        stranger_token,
        device_id,
        enabled=True,
        start_minute=420,
        end_minute=840,
        days_mask=31,
    )

    assert response.status_code == 404


def test_active_rules_include_school_mode(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _set_school_mode(
        client, tutor_token, device_id, enabled=True, start_minute=420, end_minute=840, days_mask=31
    )

    active = client.get(
        f"/api/v1/devices/{device_id}/rules/active", headers=_auth(supervised_token)
    )

    assert active.status_code == 200, active.text
    assert active.json()["school_mode"] == {
        "enabled": True,
        "start_minute": 420,
        "end_minute": 840,
        "days_mask": 31,
    }


def test_the_device_can_report_a_school_mode_block_event(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.a",
            "rule_type_applied": "SCHOOL_MODE",
            "occurred_at": "2026-09-07T09:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    assert response.status_code == 200, response.text
    assert response.json()["rule_type_applied"] == "SCHOOL_MODE"

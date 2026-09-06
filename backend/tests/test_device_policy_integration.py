import os
import uuid
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.main import app
from app.models import AuditLog
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


def _set_policy(client: TestClient, token: str, device_id: str, policy: str):
    return client.put(
        f"/api/v1/devices/{device_id}/policy",
        json={"default_app_policy": policy},
        headers=_auth(token),
    )


# ------------------------------------------------------------------------------ default value


def test_a_new_device_defaults_to_allow_policy(client) -> None:
    """The pre-Sprint-9 behavior: an app with no rule runs. Adding the column must not change
    how any already-linked device behaves.
    """
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.get(f"/api/v1/devices/{device_id}/policy", headers=_auth(tutor_token))

    assert response.status_code == 200, response.text
    assert response.json()["default_app_policy"] == "ALLOW"


def test_the_device_detail_reports_the_policy(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))

    assert detail.status_code == 200
    assert detail.json()["default_app_policy"] == "ALLOW"


# ---------------------------------------------------------------------------------- changing


def test_a_tutor_can_switch_the_device_to_allowlist_mode(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_policy(client, tutor_token, device_id, "BLOCK")

    assert response.status_code == 200, response.text
    assert response.json()["default_app_policy"] == "BLOCK"
    reread = client.get(f"/api/v1/devices/{device_id}/policy", headers=_auth(tutor_token))
    assert reread.json()["default_app_policy"] == "BLOCK"


def test_switching_back_to_allow_restores_the_previous_behavior(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _set_policy(client, tutor_token, device_id, "BLOCK")

    response = _set_policy(client, tutor_token, device_id, "ALLOW")

    assert response.status_code == 200
    assert response.json()["default_app_policy"] == "ALLOW"


def test_switching_policy_does_not_touch_existing_rules(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": "com.instagram.android", "rule_type": "BLOCK"},
        headers=_auth(tutor_token),
    )

    _set_policy(client, tutor_token, device_id, "BLOCK")
    _set_policy(client, tutor_token, device_id, "ALLOW")

    rules = client.get(f"/api/v1/devices/{device_id}/rules", headers=_auth(tutor_token)).json()[
        "rules"
    ]
    assert len(rules) == 1
    assert rules[0]["package_name"] == "com.instagram.android"
    assert rules[0]["rule_type"] == "BLOCK"


def test_an_unknown_policy_value_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_policy(client, tutor_token, device_id, "SOMETHING_ELSE")

    assert response.status_code == 422


# ------------------------------------------------------------------------------ authorization


def test_a_stranger_tutor_cannot_change_the_policy(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _set_policy(client, stranger_token, device_id, "BLOCK")

    assert response.status_code == 404


def test_the_supervised_device_cannot_change_its_own_policy(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _set_policy(client, supervised_token, device_id, "ALLOW")

    assert response.status_code == 404


def test_a_stranger_tutor_cannot_read_the_policy(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(f"/api/v1/devices/{device_id}/policy", headers=_auth(stranger_token))

    assert response.status_code == 404


# ------------------------------------------------------------------- what the device receives


def test_the_device_receives_the_policy_with_its_active_rules(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _set_policy(client, tutor_token, device_id, "BLOCK")
    client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": "com.duolingo", "rule_type": "ALLOW"},
        headers=_auth(tutor_token),
    )

    response = client.get(
        f"/api/v1/devices/{device_id}/rules/active", headers=_auth(supervised_token)
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["default_app_policy"] == "BLOCK"
    assert body["rules"][0]["package_name"] == "com.duolingo"
    assert body["rules"][0]["rule_type"] == "ALLOW"


# ------------------------------------------------------------------------------- rule events


def test_the_device_can_report_a_block_caused_by_the_default_policy(client) -> None:
    """DEFAULT_POLICY is not a rule type — the app had no rule at all. The event log has to be
    able to say that, so a tutor can tell it apart from an app they blocked on purpose.
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    reported = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.some.game",
            "rule_type_applied": "DEFAULT_POLICY",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    assert reported.status_code == 200, reported.text
    events = client.get(
        f"/api/v1/devices/{device_id}/rule-events", headers=_auth(tutor_token)
    ).json()["events"]
    assert events[0]["rule_type_applied"] == "DEFAULT_POLICY"


def test_an_unknown_applied_rule_type_is_still_rejected(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.some.game",
            "rule_type_applied": "WHATEVER",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    assert response.status_code == 422


# ------------------------------------------------------------------------------------- audit


async def test_changing_the_policy_is_audited(client, db_session) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    _set_policy(client, tutor_token, device_id, "BLOCK")

    rows = (
        (
            await db_session.execute(
                select(AuditLog).where(
                    AuditLog.action == "DEVICE_POLICY_CHANGED",
                    AuditLog.resource_id == device_id,
                )
            )
        )
        .scalars()
        .all()
    )

    assert len(rows) == 1
    assert rows[0].resource_type == "device"

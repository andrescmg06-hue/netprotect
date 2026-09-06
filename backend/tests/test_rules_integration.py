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


def _create_block_rule(client: TestClient, tutor_token: str, device_id: str, package: str):
    return client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": package, "rule_type": "BLOCK"},
        headers=_auth(tutor_token),
    )


# --------------------------------------------------------------------------------- creating


def test_a_tutor_can_create_a_block_rule(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _create_block_rule(client, tutor_token, device_id, "com.instagram.android")

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["package_name"] == "com.instagram.android"
    assert body["rule_type"] == "BLOCK"
    assert body["daily_limit_minutes"] is None


def test_a_tutor_can_create_a_daily_limit_rule(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={
            "package_name": "com.instagram.android",
            "rule_type": "DAILY_LIMIT",
            "daily_limit_minutes": 60,
        },
        headers=_auth(tutor_token),
    )

    assert response.status_code == 200, response.text
    assert response.json()["daily_limit_minutes"] == 60


def test_a_tutor_can_create_an_overnight_schedule_rule(client) -> None:
    """22:00-06:00 is a valid overnight window: start > end, evaluated with wraparound on the
    device, not rejected by the API.
    """
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={
            "package_name": "com.instagram.android",
            "rule_type": "SCHEDULE",
            "schedule_start_minute": 22 * 60,
            "schedule_end_minute": 6 * 60,
            "schedule_days_mask": 127,
        },
        headers=_auth(tutor_token),
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["schedule_start_minute"] == 22 * 60
    assert body["schedule_end_minute"] == 6 * 60


def test_daily_limit_rule_without_minutes_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": "com.instagram.android", "rule_type": "DAILY_LIMIT"},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 422


def test_schedule_rule_without_window_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": "com.instagram.android", "rule_type": "SCHEDULE"},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 422


def test_a_stranger_tutor_cannot_create_a_rule_for_someone_elses_device(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _create_block_rule(client, stranger_token, device_id, "com.a")

    assert response.status_code == 404


def test_the_supervised_device_cannot_create_a_rule(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _create_block_rule(client, supervised_token, device_id, "com.a")

    assert response.status_code == 404


def test_creating_a_rule_for_an_already_ruled_package_replaces_it(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _create_block_rule(client, tutor_token, device_id, "com.instagram.android")

    replaced = client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={
            "package_name": "com.instagram.android",
            "rule_type": "DAILY_LIMIT",
            "daily_limit_minutes": 30,
        },
        headers=_auth(tutor_token),
    )
    assert replaced.status_code == 200

    rules = client.get(f"/api/v1/devices/{device_id}/rules", headers=_auth(tutor_token)).json()[
        "rules"
    ]
    assert len(rules) == 1
    assert rules[0]["rule_type"] == "DAILY_LIMIT"
    assert rules[0]["daily_limit_minutes"] == 30


# ---------------------------------------------------------------------------------- listing


def test_the_tutor_can_list_rules_for_a_device(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _create_block_rule(client, tutor_token, device_id, "com.a")
    _create_block_rule(client, tutor_token, device_id, "com.b")

    listing = client.get(f"/api/v1/devices/{device_id}/rules", headers=_auth(tutor_token))

    assert listing.status_code == 200
    packages = {rule["package_name"] for rule in listing.json()["rules"]}
    assert packages == {"com.a", "com.b"}


def test_a_stranger_tutor_cannot_list_someone_elses_rules(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(f"/api/v1/devices/{device_id}/rules", headers=_auth(stranger_token))

    assert response.status_code == 404


# --------------------------------------------------------------------------------- deleting


def test_the_tutor_can_delete_a_rule(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    rule_id = _create_block_rule(client, tutor_token, device_id, "com.a").json()["id"]

    response = client.delete(
        f"/api/v1/devices/{device_id}/rules/{rule_id}", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    assert response.json()["package_name"] == "com.a"
    remaining = client.get(f"/api/v1/devices/{device_id}/rules", headers=_auth(tutor_token)).json()
    assert remaining["rules"] == []


def test_deleting_a_nonexistent_rule_returns_404(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.delete(
        f"/api/v1/devices/{device_id}/rules/{uuid.uuid4()}", headers=_auth(tutor_token)
    )

    assert response.status_code == 404


def test_deleting_a_rule_through_another_device_returns_404(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    rule_id = _create_block_rule(client, tutor_token, device_id, "com.a").json()["id"]
    other_device_id = _link_a_device(client, tutor_token, _make_account(client, "SUPERVISADO")[0])

    response = client.delete(
        f"/api/v1/devices/{other_device_id}/rules/{rule_id}", headers=_auth(tutor_token)
    )

    assert response.status_code == 404
    still_there = client.get(
        f"/api/v1/devices/{device_id}/rules", headers=_auth(tutor_token)
    ).json()
    assert len(still_there["rules"]) == 1


# --------------------------------------------------------------------------- device evaluation


def test_the_supervised_device_can_fetch_its_own_active_rules(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_block_rule(client, tutor_token, device_id, "com.instagram.android")

    response = client.get(
        f"/api/v1/devices/{device_id}/rules/active", headers=_auth(supervised_token)
    )

    assert response.status_code == 200, response.text
    assert response.json()["rules"][0]["package_name"] == "com.instagram.android"


def test_the_tutor_cannot_fetch_active_rules_through_the_device_endpoint(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.get(f"/api/v1/devices/{device_id}/rules/active", headers=_auth(tutor_token))

    assert response.status_code == 404


def test_a_different_supervised_account_cannot_fetch_someone_elses_active_rules(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    someone_else_token, _ = _make_account(client, "SUPERVISADO")

    response = client.get(
        f"/api/v1/devices/{device_id}/rules/active", headers=_auth(someone_else_token)
    )

    assert response.status_code == 404


# -------------------------------------------------------------------------------- rule events


def test_the_supervised_device_can_report_a_rule_event(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.instagram.android",
            "rule_type_applied": "BLOCK",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    assert response.status_code == 200, response.text
    assert response.json()["rule_type_applied"] == "BLOCK"


def test_the_tutor_can_list_rule_events_for_a_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.instagram.android",
            "rule_type_applied": "SCHEDULE",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    listing = client.get(f"/api/v1/devices/{device_id}/rule-events", headers=_auth(tutor_token))

    assert listing.status_code == 200
    events = listing.json()["events"]
    assert len(events) == 1
    assert events[0]["package_name"] == "com.instagram.android"
    assert events[0]["rule_type_applied"] == "SCHEDULE"


def test_the_tutor_cannot_report_a_rule_event(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.a",
            "rule_type_applied": "BLOCK",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(tutor_token),
    )

    assert response.status_code == 404


# ---------------------------------------------------------------------------------- audit


async def test_creating_updating_and_deleting_a_rule_is_audited(client, db_session) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    created = _create_block_rule(client, tutor_token, device_id, "com.a").json()
    client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": "com.a", "rule_type": "DAILY_LIMIT", "daily_limit_minutes": 15},
        headers=_auth(tutor_token),
    )
    client.delete(
        f"/api/v1/devices/{device_id}/rules/{created['id']}", headers=_auth(tutor_token)
    )

    rows = (
        (
            await db_session.execute(
                select(AuditLog).where(
                    AuditLog.action.in_(
                        ("APP_RULE_CREATED", "APP_RULE_UPDATED", "APP_RULE_DELETED")
                    ),
                    AuditLog.resource_id == created["id"],
                )
            )
        )
        .scalars()
        .all()
    )

    actions = {row.action for row in rows}
    assert actions == {"APP_RULE_CREATED", "APP_RULE_UPDATED", "APP_RULE_DELETED"}

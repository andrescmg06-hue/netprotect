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


def _assign_category(client: TestClient, token: str, device_id: str, package: str, category: str):
    return client.post(
        f"/api/v1/devices/{device_id}/app-categories",
        json={"package_name": package, "category": category},
        headers=_auth(token),
    )


def _set_category_rule(client: TestClient, token: str, device_id: str, category: str, **fields):
    return client.post(
        f"/api/v1/devices/{device_id}/category-rules",
        json={"category": category, **fields},
        headers=_auth(token),
    )


# ------------------------------------------------------------------------- app-category assignments


def test_a_tutor_can_assign_a_category_to_an_app(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _assign_category(
        client, tutor_token, device_id, "com.instagram.android", "SOCIAL_MEDIA"
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["package_name"] == "com.instagram.android"
    assert body["category"] == "SOCIAL_MEDIA"


def test_an_invalid_category_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _assign_category(client, tutor_token, device_id, "com.a", "NOT_A_REAL_CATEGORY")

    assert response.status_code == 422


def test_reassigning_a_package_replaces_its_category(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _assign_category(client, tutor_token, device_id, "com.a", "GAMES")

    replaced = _assign_category(client, tutor_token, device_id, "com.a", "EDUCATION")
    assert replaced.status_code == 200

    listing = client.get(f"/api/v1/devices/{device_id}/app-categories", headers=_auth(tutor_token))
    assignments = listing.json()["assignments"]
    assert len(assignments) == 1
    assert assignments[0]["category"] == "EDUCATION"


def test_a_stranger_tutor_cannot_assign_a_category(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _assign_category(client, stranger_token, device_id, "com.a", "GAMES")

    assert response.status_code == 404


def test_the_tutor_can_list_and_delete_an_assignment(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    assignment_id = _assign_category(client, tutor_token, device_id, "com.a", "NEWS").json()["id"]

    deleted = client.delete(
        f"/api/v1/devices/{device_id}/app-categories/{assignment_id}", headers=_auth(tutor_token)
    )

    assert deleted.status_code == 200, deleted.text
    remaining = client.get(
        f"/api/v1/devices/{device_id}/app-categories", headers=_auth(tutor_token)
    ).json()
    assert remaining["assignments"] == []


def test_deleting_a_nonexistent_assignment_returns_404(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.delete(
        f"/api/v1/devices/{device_id}/app-categories/{uuid.uuid4()}", headers=_auth(tutor_token)
    )

    assert response.status_code == 404


# --------------------------------------------------------------------------------- category rules


def test_a_tutor_can_set_a_block_rule_for_a_category(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="BLOCK")

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["category"] == "GAMES"
    assert body["rule_type"] == "BLOCK"


def test_a_daily_limit_category_rule_without_minutes_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="DAILY_LIMIT")

    assert response.status_code == 422


def test_a_tutor_can_set_a_weekly_limit_rule_for_a_category(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_category_rule(
        client, tutor_token, device_id, "GAMES", rule_type="WEEKLY_LIMIT", weekly_limit_minutes=300
    )

    assert response.status_code == 200, response.text
    assert response.json()["weekly_limit_minutes"] == 300


def test_a_weekly_limit_category_rule_without_minutes_is_rejected(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="WEEKLY_LIMIT")

    assert response.status_code == 422


def test_setting_a_rule_for_an_already_ruled_category_replaces_it(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="BLOCK")

    replaced = _set_category_rule(
        client, tutor_token, device_id, "GAMES", rule_type="DAILY_LIMIT", daily_limit_minutes=45
    )
    assert replaced.status_code == 200

    rules = client.get(
        f"/api/v1/devices/{device_id}/category-rules", headers=_auth(tutor_token)
    ).json()["category_rules"]
    assert len(rules) == 1
    assert rules[0]["rule_type"] == "DAILY_LIMIT"
    assert rules[0]["daily_limit_minutes"] == 45


def test_a_stranger_tutor_cannot_set_a_category_rule(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _set_category_rule(client, stranger_token, device_id, "GAMES", rule_type="BLOCK")

    assert response.status_code == 404


def test_the_tutor_can_delete_a_category_rule(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    created = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="BLOCK")
    rule_id = created.json()["id"]

    response = client.delete(
        f"/api/v1/devices/{device_id}/category-rules/{rule_id}", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    remaining = client.get(
        f"/api/v1/devices/{device_id}/category-rules", headers=_auth(tutor_token)
    ).json()
    assert remaining["category_rules"] == []


def test_deleting_a_category_rule_through_another_device_returns_404(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    created = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="BLOCK")
    rule_id = created.json()["id"]
    other_supervised_token, _ = _make_account(client, "SUPERVISADO")
    other_device_id = _link_a_device(client, tutor_token, other_supervised_token)

    response = client.delete(
        f"/api/v1/devices/{other_device_id}/category-rules/{rule_id}", headers=_auth(tutor_token)
    )

    assert response.status_code == 404


def test_the_device_can_report_a_category_block_event(client) -> None:
    """End-to-end, not just the CHECK constraint at the DB level: CATEGORY has to survive the
    Pydantic AppliedRuleType Literal and the endpoint before it ever reaches the database.
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.instagram.android",
            "rule_type_applied": "CATEGORY",
            "occurred_at": "2026-09-05T21:00:00Z",
        },
        headers=_auth(supervised_token),
    )

    assert response.status_code == 200, response.text
    assert response.json()["rule_type_applied"] == "CATEGORY"


# --------------------------------------------------------------- rules/active includes categories


def test_active_rules_include_category_assignments_and_rules(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _assign_category(client, tutor_token, device_id, "com.instagram.android", "SOCIAL_MEDIA")
    _set_category_rule(client, tutor_token, device_id, "SOCIAL_MEDIA", rule_type="BLOCK")

    active = client.get(
        f"/api/v1/devices/{device_id}/rules/active", headers=_auth(supervised_token)
    )

    assert active.status_code == 200, active.text
    body = active.json()
    assert body["category_assignments"] == [
        {
            "id": body["category_assignments"][0]["id"],
            "package_name": "com.instagram.android",
            "category": "SOCIAL_MEDIA",
            "created_at": body["category_assignments"][0]["created_at"],
            "updated_at": body["category_assignments"][0]["updated_at"],
        }
    ]
    assert body["category_rules"][0]["category"] == "SOCIAL_MEDIA"
    assert body["category_rules"][0]["rule_type"] == "BLOCK"
    assert body["default_app_policy"] == "ALLOW"


# ---------------------------------------------------------------------------------- audit


async def test_assigning_and_deleting_a_category_is_audited(client, db_session) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    assignment = _assign_category(client, tutor_token, device_id, "com.a", "GAMES").json()
    client.delete(
        f"/api/v1/devices/{device_id}/app-categories/{assignment['id']}", headers=_auth(tutor_token)
    )

    rows = (
        (
            await db_session.execute(
                select(AuditLog).where(
                    AuditLog.action.in_(
                        ("APP_CATEGORY_ASSIGNED", "APP_CATEGORY_UNASSIGNED")
                    ),
                    AuditLog.resource_id == assignment["id"],
                )
            )
        )
        .scalars()
        .all()
    )

    actions = {row.action for row in rows}
    assert actions == {"APP_CATEGORY_ASSIGNED", "APP_CATEGORY_UNASSIGNED"}


async def test_creating_and_deleting_a_category_rule_is_audited(client, db_session) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    rule = _set_category_rule(client, tutor_token, device_id, "GAMES", rule_type="BLOCK").json()
    client.delete(
        f"/api/v1/devices/{device_id}/category-rules/{rule['id']}", headers=_auth(tutor_token)
    )

    rows = (
        (
            await db_session.execute(
                select(AuditLog).where(
                    AuditLog.action.in_(("CATEGORY_RULE_CREATED", "CATEGORY_RULE_DELETED")),
                    AuditLog.resource_id == rule["id"],
                )
            )
        )
        .scalars()
        .all()
    )

    actions = {row.action for row in rows}
    assert actions == {"CATEGORY_RULE_CREATED", "CATEGORY_RULE_DELETED"}

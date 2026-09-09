import csv
import io
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


def _list_audit(client: TestClient, token: str, **params: str):
    return client.get("/api/v1/users/me/audit", headers=_auth(token), params=params)


def test_a_tutor_sees_their_own_audited_actions_in_reverse_chronological_order(client) -> None:
    # _make_account itself audits LOGIN then ROLE_GRANTED, so the two pairing-code actions below
    # are the two most recent entries, not the only ones.
    tutor_token, _ = _make_account(client, "TUTOR")

    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).status_code == 200
    assert (
        client.delete("/api/v1/pairing/codes/current", headers=_auth(tutor_token)).status_code
        == 200
    )

    response = _list_audit(client, tutor_token)

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["total"] == 4
    newest_two = [entry["action"] for entry in body["logs"][:2]]
    assert newest_two == ["PAIRING_CODE_REVOKED", "PAIRING_CODE_GENERATED"]
    assert all(entry["resource_type"] == "pairing_code" for entry in body["logs"][:2])


def test_device_linked_is_recorded_under_the_supervised_actor_not_the_tutor(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    tutor_actions = [e["action"] for e in _list_audit(client, tutor_token).json()["logs"]]
    supervised_logs = _list_audit(client, supervised_token).json()["logs"]
    supervised_actions = [e["action"] for e in supervised_logs]

    assert "DEVICE_LINKED" not in tutor_actions
    assert "PAIRING_CODE_GENERATED" in tutor_actions
    # DEVICE_LINKED is the supervised account's most recent action (it happens after its own
    # LOGIN/ROLE_GRANTED, both also present since this is the account's full trail).
    assert supervised_actions[0] == "DEVICE_LINKED"
    assert supervised_logs[0]["resource_id"] == device_id


def test_filtering_by_action(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).status_code == 200
    assert (
        client.delete("/api/v1/pairing/codes/current", headers=_auth(tutor_token)).status_code
        == 200
    )

    response = _list_audit(client, tutor_token, action="PAIRING_CODE_REVOKED")

    body = response.json()
    assert body["total"] == 1
    assert body["logs"][0]["action"] == "PAIRING_CODE_REVOKED"


def test_filtering_by_resource_type(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    assert (
        client.patch(
            f"/api/v1/devices/{device_id}",
            json={"name": "Nuevo nombre"},
            headers=_auth(tutor_token),
        ).status_code
        == 200
    )

    response = _list_audit(client, tutor_token, resource_type="device")

    body = response.json()
    assert body["total"] == 1
    assert body["logs"][0]["action"] == "DEVICE_RENAMED"
    assert body["logs"][0]["resource_id"] == device_id


def test_pagination_returns_total_count_and_the_correct_page(client) -> None:
    # Scoped with resource_type so the count only reflects the 3 pairing-code actions below, not
    # also this account's own LOGIN/ROLE_GRANTED from _make_account.
    tutor_token, _ = _make_account(client, "TUTOR")
    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).status_code == 200
    assert (
        client.delete("/api/v1/pairing/codes/current", headers=_auth(tutor_token)).status_code
        == 200
    )
    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).status_code == 200

    response = _list_audit(
        client, tutor_token, resource_type="pairing_code", limit="1", offset="1"
    )

    body = response.json()
    assert body["total"] == 3
    assert body["limit"] == 1
    assert body["offset"] == 1
    assert len(body["logs"]) == 1
    assert body["logs"][0]["action"] == "PAIRING_CODE_REVOKED"


def test_a_user_cannot_see_another_users_audit_entries(client) -> None:
    tutor_a_token, _ = _make_account(client, "TUTOR")
    tutor_b_token, _ = _make_account(client, "TUTOR")
    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_a_token)).status_code == 200

    response = _list_audit(client, tutor_b_token)

    # tutor_b has its own LOGIN/ROLE_GRANTED entries (from _make_account) but never tutor_a's
    # pairing-code action.
    actions = [entry["action"] for entry in response.json()["logs"]]
    assert "PAIRING_CODE_GENERATED" not in actions
    assert response.json()["total"] == 2


def test_listing_the_audit_log_without_a_token_is_401(client) -> None:
    response = client.get("/api/v1/users/me/audit")

    assert response.status_code == 401


def test_export_returns_a_csv_with_the_filtered_rows(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    assert client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).status_code == 200
    assert (
        client.delete("/api/v1/pairing/codes/current", headers=_auth(tutor_token)).status_code
        == 200
    )

    response = client.get(
        "/api/v1/users/me/audit/export",
        headers=_auth(tutor_token),
        params={"action": "PAIRING_CODE_REVOKED"},
    )

    assert response.status_code == 200, response.text
    assert response.headers["content-type"].startswith("text/csv")
    rows = list(csv.reader(io.StringIO(response.text)))
    assert rows[0] == ["id", "action", "resource_type", "resource_id", "ip_address", "created_at"]
    assert len(rows) == 2
    assert rows[1][1] == "PAIRING_CODE_REVOKED"

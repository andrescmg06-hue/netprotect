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

CENTER = {"latitude": 4.710989, "longitude": -74.072092}
OUTSIDE = {"latitude": 4.750000, "longitude": -74.072092}


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


def _report_rule_event(client: TestClient, token: str, device_id: str, *, occurred_at: str):
    return client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.instagram.android",
            "rule_type_applied": "BLOCK",
            "occurred_at": occurred_at,
        },
        headers=_auth(token),
    )


def _report_location(
    client: TestClient, token: str, device_id: str, *, captured_at: str, **coordinates
):
    return client.post(
        f"/api/v1/devices/{device_id}/location",
        json={**coordinates, "accuracy_meters": 1500.0, "captured_at": captured_at},
        headers=_auth(token),
    )


def _create_geofence(client: TestClient, tutor_token: str, device_id: str):
    return client.post(
        f"/api/v1/devices/{device_id}/geofences",
        json={"name": "Casa", "radius_meters": 500.0, **CENTER},
        headers=_auth(tutor_token),
    )


def test_the_owning_tutor_sees_rule_and_geofence_events_merged_by_time(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)

    # Oldest: a rule block. Middle: a geofence baseline (no event). Newest: an ENTER.
    rule_event = _report_rule_event(
        client, supervised_token, device_id, occurred_at="2026-09-05T08:00:00Z"
    )
    assert rule_event.status_code == 200, rule_event.text
    baseline = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-06T08:00:00Z", **OUTSIDE
    )
    assert baseline.status_code == 200, baseline.text
    enter = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T08:00:00Z", **CENTER
    )
    assert enter.status_code == 200, enter.text

    response = client.get(f"/api/v1/devices/{device_id}/history", headers=_auth(tutor_token))

    assert response.status_code == 200, response.text
    events = response.json()["events"]
    assert len(events) == 2
    # Most recent first: the ENTER, then the rule block.
    assert events[0]["event_type"] == "GEOFENCE"
    assert events[0]["geofence_event_type"] == "ENTER"
    assert events[0]["geofence_name"] == "Casa"
    assert events[0]["package_name"] is None
    assert events[1]["event_type"] == "APP_RULE"
    assert events[1]["package_name"] == "com.instagram.android"
    assert events[1]["rule_type_applied"] == "BLOCK"
    assert events[1]["geofence_name"] is None


def test_history_is_empty_when_the_device_has_no_events_yet(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.get(f"/api/v1/devices/{device_id}/history", headers=_auth(tutor_token))

    assert response.status_code == 200, response.text
    assert response.json()["events"] == []


def test_a_stranger_tutor_cannot_read_the_device_history(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_rule_event(client, supervised_token, device_id, occurred_at="2026-09-05T08:00:00Z")
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(f"/api/v1/devices/{device_id}/history", headers=_auth(stranger_token))

    assert response.status_code == 404


def test_a_supervised_user_cannot_read_the_device_history(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = client.get(
        f"/api/v1/devices/{device_id}/history", headers=_auth(supervised_token)
    )

    assert response.status_code == 404


def test_reading_history_for_a_nonexistent_device_is_404(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{uuid.uuid4()}/history", headers=_auth(tutor_token)
    )

    assert response.status_code == 404

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


def _report_rule_event(
    client: TestClient, token: str, device_id: str, rule_type: str, *, occurred_at: str
):
    return client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={
            "package_name": "com.instagram.android",
            "rule_type_applied": rule_type,
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


def _list_alerts(client: TestClient, tutor_token: str, device_id: str):
    return client.get(f"/api/v1/devices/{device_id}/alerts", headers=_auth(tutor_token))


def test_a_block_generates_an_info_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    assert (
        _report_rule_event(
            client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T08:00:00Z"
        ).status_code
        == 200
    )

    response = _list_alerts(client, tutor_token, device_id)

    assert response.status_code == 200, response.text
    alerts = response.json()["alerts"]
    assert len(alerts) == 1
    assert alerts[0]["level"] == "INFO"
    assert alerts[0]["alert_type"] == "APP_BLOCKED"
    assert alerts[0]["package_name"] == "com.instagram.android"
    assert alerts[0]["occurrence_count"] == 1


def test_a_daily_limit_generates_a_warning_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    assert (
        _report_rule_event(
            client, supervised_token, device_id, "DAILY_LIMIT", occurred_at="2026-09-08T08:00:00Z"
        ).status_code
        == 200
    )

    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    assert len(alerts) == 1
    assert alerts[0]["level"] == "WARNING"
    assert alerts[0]["alert_type"] == "APP_LIMIT_REACHED"


def test_an_allow_event_generates_no_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    assert (
        _report_rule_event(
            client, supervised_token, device_id, "ALLOW", occurred_at="2026-09-08T08:00:00Z"
        ).status_code
        == 200
    )

    assert _list_alerts(client, tutor_token, device_id).json()["alerts"] == []


def test_geofence_exit_and_enter_generate_alerts_with_matching_levels(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    assert _create_geofence(client, tutor_token, device_id).status_code == 200

    # Baseline inside, then an EXIT, then an ENTER back in.
    assert _report_location(
        client, supervised_token, device_id, captured_at="2026-09-08T07:00:00Z", **CENTER
    ).status_code == 200
    assert _report_location(
        client, supervised_token, device_id, captured_at="2026-09-08T08:00:00Z", **OUTSIDE
    ).status_code == 200
    assert _report_location(
        client, supervised_token, device_id, captured_at="2026-09-08T09:00:00Z", **CENTER
    ).status_code == 200

    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    by_type = {a["alert_type"]: a for a in alerts}
    assert by_type["GEOFENCE_EXIT"]["level"] == "WARNING"
    assert by_type["GEOFENCE_ENTER"]["level"] == "INFO"


def test_repeated_blocks_before_reading_are_deduplicated_into_one_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T08:00:00Z"
    ).status_code == 200
    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T09:00:00Z"
    ).status_code == 200

    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    assert len(alerts) == 1
    assert alerts[0]["occurrence_count"] == 2
    assert alerts[0]["last_occurred_at"] == "2026-09-08T09:00:00Z"


def test_reading_an_alert_lets_the_next_occurrence_open_a_new_one(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T08:00:00Z"
    ).status_code == 200
    alert_id = _list_alerts(client, tutor_token, device_id).json()["alerts"][0]["id"]

    read = client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert_id}/read", headers=_auth(tutor_token)
    )
    assert read.status_code == 200, read.text
    assert read.json()["read_at"] is not None

    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T10:00:00Z"
    ).status_code == 200

    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    assert len(alerts) == 2
    unread = [a for a in alerts if a["read_at"] is None]
    assert len(unread) == 1
    assert unread[0]["occurrence_count"] == 1


def test_silencing_an_alert_stops_future_generation_for_its_dedup_key(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T08:00:00Z"
    ).status_code == 200
    alert_id = _list_alerts(client, tutor_token, device_id).json()["alerts"][0]["id"]

    silence = client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert_id}/silence",
        json={},
        headers=_auth(tutor_token),
    )
    assert silence.status_code == 200, silence.text
    silence_id = silence.json()["id"]

    assert client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert_id}/read", headers=_auth(tutor_token)
    ).status_code == 200
    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T11:00:00Z"
    ).status_code == 200

    # Still just the one (now read) alert — the silenced dedup_key produced nothing new.
    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    assert len(alerts) == 1

    unsilence = client.delete(
        f"/api/v1/devices/{device_id}/alert-silences/{silence_id}", headers=_auth(tutor_token)
    )
    assert unsilence.status_code == 204

    assert _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T12:00:00Z"
    ).status_code == 200
    alerts = _list_alerts(client, tutor_token, device_id).json()["alerts"]
    assert len(alerts) == 2


def test_a_stranger_tutor_cannot_read_the_device_alerts(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_rule_event(
        client, supervised_token, device_id, "BLOCK", occurred_at="2026-09-08T08:00:00Z"
    )
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _list_alerts(client, stranger_token, device_id)

    assert response.status_code == 404


def test_a_supervised_user_cannot_read_the_device_alerts(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _list_alerts(client, supervised_token, device_id)

    assert response.status_code == 404


def test_reading_alerts_for_a_nonexistent_device_is_404(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = _list_alerts(client, tutor_token, str(uuid.uuid4()))

    assert response.status_code == 404

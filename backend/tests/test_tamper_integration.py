import os
import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.models import DeviceStatus
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


def _make_account(client: TestClient, role: str) -> str:
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
    return token


def _setup_linked_device(client: TestClient) -> tuple[str, str, str]:
    tutor_token = _make_account(client, "TUTOR")
    supervised_token = _make_account(client, "SUPERVISADO")
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
    return tutor_token, supervised_token, redeemed.json()["device_id"]


def _heartbeat(client: TestClient, token: str, device_id: str, **payload):
    return client.post(
        f"/api/v1/devices/{device_id}/heartbeat", json=payload, headers=_auth(token)
    )


def _alerts(client: TestClient, tutor_token: str, device_id: str) -> list[dict]:
    listing = client.get(f"/api/v1/devices/{device_id}/alerts", headers=_auth(tutor_token))
    assert listing.status_code == 200, listing.text
    return listing.json()["alerts"]


def _status(client: TestClient, tutor_token: str, device_id: str) -> str:
    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))
    assert detail.status_code == 200, detail.text
    return detail.json()["status"]["status"]


# ------------------------------------------------------------- heartbeat-borne tamper signals


def test_losing_usage_access_raises_a_high_alert_and_flags_the_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    beat = _heartbeat(client, supervised_token, device_id, usage_access_granted=False)

    assert beat.status_code == 200
    assert beat.json()["status"] == "ALERT"
    assert _status(client, tutor_token, device_id) == "ALERT"

    alerts = _alerts(client, tutor_token, device_id)
    assert [(a["alert_type"], a["level"]) for a in alerts] == [("PERMISSION_REVOKED", "HIGH")]


def test_a_heartbeat_reporting_the_service_stopped_raises_service_inactive(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    beat = _heartbeat(client, supervised_token, device_id, service_active=False)

    assert beat.json()["status"] == "ALERT"
    alerts = _alerts(client, tutor_token, device_id)
    assert [(a["alert_type"], a["level"]) for a in alerts] == [("SERVICE_INACTIVE", "HIGH")]


def test_a_device_clock_far_from_the_servers_raises_clock_tampering(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    skewed = datetime.now(UTC) - timedelta(
        seconds=settings.device_clock_skew_alert_seconds + 60
    )

    beat = _heartbeat(client, supervised_token, device_id, device_time=skewed.isoformat())

    assert beat.json()["status"] == "ALERT"
    alerts = _alerts(client, tutor_token, device_id)
    assert [a["alert_type"] for a in alerts] == ["CLOCK_TAMPERING"]


def test_a_device_clock_within_tolerance_raises_nothing(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    close_enough = datetime.now(UTC) - timedelta(
        seconds=settings.device_clock_skew_alert_seconds - 30
    )

    beat = _heartbeat(
        client, supervised_token, device_id, device_time=close_enough.isoformat()
    )

    assert beat.json()["status"] == "ONLINE"
    assert _alerts(client, tutor_token, device_id) == []


def test_a_naive_device_time_is_read_as_utc_instead_of_crashing(client) -> None:
    """A datetime with no offset is valid input for the schema (plain `datetime`, like every
    other device-reported timestamp in this API). Subtracting it from an aware `now` would raise
    TypeError — a 500 any supervised device could trigger with one malformed field.
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    naive_but_current = datetime.now(UTC).replace(tzinfo=None).isoformat()

    beat = _heartbeat(client, supervised_token, device_id, device_time=naive_but_current)

    assert beat.status_code == 200
    assert beat.json()["status"] == "ONLINE"
    assert _alerts(client, tutor_token, device_id) == []


async def test_an_anomalously_long_silence_is_caught_when_the_device_comes_back(
    client, db_session
) -> None:
    """The absence of a heartbeat can't trigger anything by itself — this project has no
    scheduler. The gap is measured against the previous last_seen_at the moment the device
    finally reports again, the same way geofence transitions compare against the previous
    location report (Sprint 14).
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    status_row = await db_session.get(DeviceStatus, uuid.UUID(device_id))
    status_row.last_seen_at = datetime.now(UTC) - timedelta(
        seconds=settings.device_heartbeat_silence_alert_seconds + 60
    )
    await db_session.commit()

    beat = _heartbeat(client, supervised_token, device_id)

    assert beat.json()["status"] == "ALERT"
    alerts = _alerts(client, tutor_token, device_id)
    assert [(a["alert_type"], a["level"]) for a in alerts] == [("HEARTBEAT_SILENCE", "HIGH")]


async def test_an_ordinary_offline_stretch_is_not_treated_as_tampering(
    client, db_session
) -> None:
    """Longer than device_offline_threshold_seconds (so the tutor saw OFFLINE) but far short of
    device_heartbeat_silence_alert_seconds: a normal connectivity gap, not a signal.
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    status_row = await db_session.get(DeviceStatus, uuid.UUID(device_id))
    status_row.last_seen_at = datetime.now(UTC) - timedelta(
        seconds=settings.device_offline_threshold_seconds + 60
    )
    await db_session.commit()

    beat = _heartbeat(client, supervised_token, device_id)

    assert beat.json()["status"] == "ONLINE"
    assert _alerts(client, tutor_token, device_id) == []


def test_a_heartbeat_that_reports_nothing_new_stays_online(client) -> None:
    """An older installed build sends none of the three fields. "No information" must never be
    read as a tamper condition.
    """
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    beat = _heartbeat(client, supervised_token, device_id, app_version="0.2.0")

    assert beat.json()["status"] == "ONLINE"
    assert _alerts(client, tutor_token, device_id) == []


def test_a_healthy_heartbeat_clears_a_previously_flagged_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _heartbeat(client, supervised_token, device_id, usage_access_granted=False)
    assert _status(client, tutor_token, device_id) == "ALERT"

    _heartbeat(client, supervised_token, device_id, usage_access_granted=True)

    assert _status(client, tutor_token, device_id) == "ONLINE"
    # The alert itself survives: the status light is about right now, the bandeja is the record.
    assert [a["alert_type"] for a in _alerts(client, tutor_token, device_id)] == [
        "PERMISSION_REVOKED"
    ]


def test_repeated_tamper_beats_fold_into_one_unread_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    for _ in range(3):
        _heartbeat(client, supervised_token, device_id, usage_access_granted=False)

    alerts = _alerts(client, tutor_token, device_id)
    assert len(alerts) == 1
    assert alerts[0]["occurrence_count"] == 3


def test_two_conditions_in_one_beat_raise_two_alerts(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    skewed = datetime.now(UTC) + timedelta(
        seconds=settings.device_clock_skew_alert_seconds + 60
    )

    _heartbeat(
        client,
        supervised_token,
        device_id,
        usage_access_granted=False,
        device_time=skewed.isoformat(),
    )

    assert {a["alert_type"] for a in _alerts(client, tutor_token, device_id)} == {
        "PERMISSION_REVOKED",
        "CLOCK_TAMPERING",
    }


def test_a_silenced_tamper_signal_stops_generating_alerts(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _heartbeat(client, supervised_token, device_id, usage_access_granted=False)
    alert_id = _alerts(client, tutor_token, device_id)[0]["id"]
    silenced = client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert_id}/silence",
        json={},
        headers=_auth(tutor_token),
    )
    assert silenced.status_code == 200
    client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert_id}/read", headers=_auth(tutor_token)
    )

    _heartbeat(client, supervised_token, device_id, usage_access_granted=False)

    unread = [a for a in _alerts(client, tutor_token, device_id) if a["read_at"] is None]
    assert unread == []


# --------------------------------------------------------------- uninstall-attempt reporting


def _report_uninstall_attempt(client: TestClient, token: str, device_id: str):
    return client.post(
        f"/api/v1/devices/{device_id}/tamper-events",
        json={"event_type": "UNINSTALL_ATTEMPT", "occurred_at": datetime.now(UTC).isoformat()},
        headers=_auth(token),
    )


def test_an_uninstall_attempt_raises_a_critical_alert(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    reported = _report_uninstall_attempt(client, supervised_token, device_id)

    assert reported.status_code == 204
    assert _status(client, tutor_token, device_id) == "ALERT"
    alerts = _alerts(client, tutor_token, device_id)
    assert [(a["alert_type"], a["level"]) for a in alerts] == [
        ("UNINSTALL_ATTEMPT", "CRITICAL")
    ]


def test_an_unknown_tamper_event_type_is_rejected(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/tamper-events",
        json={"event_type": "ROOTED", "occurred_at": datetime.now(UTC).isoformat()},
        headers=_auth(supervised_token),
    )

    assert response.status_code == 422


def test_the_tutor_cannot_report_a_tamper_event_for_the_device(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _report_uninstall_attempt(client, tutor_token, device_id)

    assert response.status_code == 404


def test_another_supervised_account_cannot_report_for_a_device_that_is_not_theirs(
    client,
) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token = _make_account(client, "SUPERVISADO")

    response = _report_uninstall_attempt(client, stranger_token, device_id)

    assert response.status_code == 404


def test_a_stranger_tutor_cannot_see_another_devices_tamper_alerts(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)
    _report_uninstall_attempt(client, supervised_token, device_id)
    stranger_token = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{device_id}/alerts", headers=_auth(stranger_token)
    )

    assert response.status_code == 404

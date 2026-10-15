"""B-12 (Sprint 69): every tutor sees only the geofences they created, and only the events,
timeline entries and alerts derived from them. A geofence carries a named, decrypted point
("Casa", "Colegio"), so a tutor who re-links the same installation after another one must not
inherit it — see docs/sprint-69.md.
"""

import os
import uuid
from datetime import UTC, datetime, timedelta
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

# Same two points as test_geofence_integration.py: ~4.3km apart, so a 500m zone around one never
# contains the other.
CENTER = {"latitude": 4.710989, "longitude": -74.072092}
OUTSIDE = {"latitude": 4.750000, "longitude": -74.072092}


def _recent(minutes_ago: int) -> str:
    """A `captured_at` safely inside location_retention_days regardless of when the suite runs
    (see docs/sprint-26-evidence.md for why a fixed date is not used)."""
    return (datetime.now(UTC) - timedelta(minutes=minutes_ago)).strftime("%Y-%m-%dT%H:%M:%SZ")


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


def _link_a_device(
    client: TestClient, tutor_token: str, supervised_token: str, *, device_instance_id: str
) -> str:
    code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).json()["code"]
    redeemed = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": code,
            "device_instance_id": device_instance_id,
            "device_name": "Celular de Juan",
            "platform": "ANDROID",
            "os_version": "16",
            "app_version": "0.1.0",
        },
        headers=_auth(supervised_token),
    )
    assert redeemed.status_code == 200, redeemed.text
    return redeemed.json()["device_id"]


def _unlink(client: TestClient, tutor_token: str, device_id: str) -> None:
    response = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))
    assert response.status_code == 200, response.text


def _report_location(
    client: TestClient, token: str, device_id: str, *, captured_at: str, **coordinates
) -> None:
    response = client.post(
        f"/api/v1/devices/{device_id}/location",
        json={**coordinates, "accuracy_meters": 1500.0, "captured_at": captured_at},
        headers=_auth(token),
    )
    assert response.status_code == 200, response.text


def _create_geofence(
    client: TestClient, tutor_token: str, device_id: str, *, name: str, center: dict
) -> str:
    response = client.post(
        f"/api/v1/devices/{device_id}/geofences",
        json={"name": name, "radius_meters": 500.0, **center},
        headers=_auth(tutor_token),
    )
    assert response.status_code == 200, response.text
    return response.json()["id"]


def _geofences(client: TestClient, token: str, device_id: str) -> list[dict]:
    response = client.get(f"/api/v1/devices/{device_id}/geofences", headers=_auth(token))
    assert response.status_code == 200, response.text
    return response.json()["geofences"]


def _events(client: TestClient, token: str, device_id: str) -> list[dict]:
    response = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(token)
    )
    assert response.status_code == 200, response.text
    return response.json()["events"]


def _history_geofence_entries(client: TestClient, token: str, device_id: str) -> list[dict]:
    response = client.get(f"/api/v1/devices/{device_id}/history", headers=_auth(token))
    assert response.status_code == 200, response.text
    return [event for event in response.json()["events"] if event["event_type"] == "GEOFENCE"]


def _geofence_alerts(client: TestClient, token: str, device_id: str) -> list[dict]:
    response = client.get(f"/api/v1/devices/{device_id}/alerts", headers=_auth(token))
    assert response.status_code == 200, response.text
    return [
        alert
        for alert in response.json()["alerts"]
        if alert["alert_type"] in ("GEOFENCE_ENTER", "GEOFENCE_EXIT")
    ]


def _put_geofence(client: TestClient, token: str, device_id: str, geofence_id: str):
    return client.put(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}",
        json={"name": "Otra", "radius_meters": 200.0, **OUTSIDE},
        headers=_auth(token),
    )


def _delete_geofence(client: TestClient, token: str, device_id: str, geofence_id: str):
    return client.delete(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}", headers=_auth(token)
    )


def _assert_same_404_as_a_missing_one(missing, foreign) -> None:
    assert foreign.status_code == 404
    assert foreign.status_code == missing.status_code
    assert foreign.json() == missing.json()


# -------------------------------------------------------------- a new tutor after re-linking


def test_a_new_tutor_after_relinking_sees_nothing_of_the_previous_tutors_geofences(
    client,
) -> None:
    tutor_a = _make_account(client, "TUTOR")
    tutor_b = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex

    device_id = _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    casa_id = _create_geofence(client, tutor_a, device_id, name="Casa", center=CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(30), **CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)
    assert len(_events(client, tutor_a, device_id)) == 1
    assert len(_geofence_alerts(client, tutor_a, device_id)) == 1

    _unlink(client, tutor_a, device_id)
    assert _link_a_device(client, tutor_b, supervised, device_instance_id=instance) == device_id

    assert _geofences(client, tutor_b, device_id) == []
    assert _events(client, tutor_b, device_id) == []
    assert _history_geofence_entries(client, tutor_b, device_id) == []
    assert _geofence_alerts(client, tutor_b, device_id) == []

    missing_id = str(uuid.uuid4())
    _assert_same_404_as_a_missing_one(
        _put_geofence(client, tutor_b, device_id, missing_id),
        _put_geofence(client, tutor_b, device_id, casa_id),
    )
    _assert_same_404_as_a_missing_one(
        _delete_geofence(client, tutor_b, device_id, missing_id),
        _delete_geofence(client, tutor_b, device_id, casa_id),
    )

    # Tutor A keeps everything (the rejected DELETE above did not touch it) once linked again.
    _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    assert [g["id"] for g in _geofences(client, tutor_a, device_id)] == [casa_id]
    assert [g["name"] for g in _geofences(client, tutor_a, device_id)] == ["Casa"]
    assert len(_events(client, tutor_a, device_id)) == 1
    assert len(_history_geofence_entries(client, tutor_a, device_id)) == 1
    assert len(_geofence_alerts(client, tutor_a, device_id)) == 1
    # And B still sees none of it, even with A active again on the same device.
    assert _geofences(client, tutor_b, device_id) == []
    assert _events(client, tutor_b, device_id) == []


def test_a_tutor_keeps_the_events_and_alerts_of_a_geofence_they_deleted(client) -> None:
    tutor = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor, supervised, device_instance_id=uuid.uuid4().hex)
    casa_id = _create_geofence(client, tutor, device_id, name="Casa", center=CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(30), **CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)

    assert _delete_geofence(client, tutor, device_id, casa_id).status_code == 200

    events = _events(client, tutor, device_id)
    assert [(e["geofence_name"], e["event_type"]) for e in events] == [("Casa", "EXIT")]
    history = _history_geofence_entries(client, tutor, device_id)
    assert [(h["geofence_name"], h["geofence_event_type"]) for h in history] == [
        ("Casa", "EXIT")
    ]
    alerts = _geofence_alerts(client, tutor, device_id)
    assert [(a["geofence_name"], a["alert_type"]) for a in alerts] == [("Casa", "GEOFENCE_EXIT")]


# ------------------------------------------------------------- two tutors active at the same time


def test_two_active_tutors_each_see_and_are_alerted_only_about_their_own_geofences(
    client,
) -> None:
    tutor_a = _make_account(client, "TUTOR")
    tutor_b = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex
    device_id = _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    assert _link_a_device(client, tutor_b, supervised, device_instance_id=instance) == device_id

    casa_id = _create_geofence(client, tutor_a, device_id, name="Casa", center=CENTER)
    colegio_id = _create_geofence(client, tutor_b, device_id, name="Colegio", center=OUTSIDE)

    assert [g["id"] for g in _geofences(client, tutor_a, device_id)] == [casa_id]
    assert [g["id"] for g in _geofences(client, tutor_b, device_id)] == [colegio_id]

    # Leaving A's "Casa" is, at the same time, arriving at B's "Colegio".
    _report_location(client, supervised, device_id, captured_at=_recent(30), **CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)

    assert [(e["geofence_name"], e["event_type"]) for e in _events(client, tutor_a, device_id)] == [
        ("Casa", "EXIT")
    ]
    assert [(e["geofence_name"], e["event_type"]) for e in _events(client, tutor_b, device_id)] == [
        ("Colegio", "ENTER")
    ]
    assert [h["geofence_id"] for h in _history_geofence_entries(client, tutor_a, device_id)] == [
        casa_id
    ]
    assert [h["geofence_id"] for h in _history_geofence_entries(client, tutor_b, device_id)] == [
        colegio_id
    ]
    assert [a["alert_type"] for a in _geofence_alerts(client, tutor_a, device_id)] == [
        "GEOFENCE_EXIT"
    ]
    assert [a["alert_type"] for a in _geofence_alerts(client, tutor_b, device_id)] == [
        "GEOFENCE_ENTER"
    ]

    # Neither can edit or delete the other's zone, and the refusal looks like a missing one.
    missing_id = str(uuid.uuid4())
    _assert_same_404_as_a_missing_one(
        _put_geofence(client, tutor_a, device_id, missing_id),
        _put_geofence(client, tutor_a, device_id, colegio_id),
    )
    _assert_same_404_as_a_missing_one(
        _delete_geofence(client, tutor_b, device_id, missing_id),
        _delete_geofence(client, tutor_b, device_id, casa_id),
    )
    assert [g["name"] for g in _geofences(client, tutor_a, device_id)] == ["Casa"]
    assert [g["name"] for g in _geofences(client, tutor_b, device_id)] == ["Colegio"]


def test_a_tutor_cannot_mark_read_or_silence_another_tutors_geofence_alert(client) -> None:
    tutor_a = _make_account(client, "TUTOR")
    tutor_b = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex
    device_id = _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    _link_a_device(client, tutor_b, supervised, device_instance_id=instance)
    _create_geofence(client, tutor_a, device_id, name="Casa", center=CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(30), **CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)
    [alert] = _geofence_alerts(client, tutor_a, device_id)

    missing_id = str(uuid.uuid4())
    _assert_same_404_as_a_missing_one(
        client.post(
            f"/api/v1/devices/{device_id}/alerts/{missing_id}/read", headers=_auth(tutor_b)
        ),
        client.post(
            f"/api/v1/devices/{device_id}/alerts/{alert['id']}/read", headers=_auth(tutor_b)
        ),
    )
    _assert_same_404_as_a_missing_one(
        client.post(
            f"/api/v1/devices/{device_id}/alerts/{missing_id}/silence",
            json={},
            headers=_auth(tutor_b),
        ),
        client.post(
            f"/api/v1/devices/{device_id}/alerts/{alert['id']}/silence",
            json={},
            headers=_auth(tutor_b),
        ),
    )

    # Nothing changed for its owner: still unread, and nothing was silenced.
    [unchanged] = _geofence_alerts(client, tutor_a, device_id)
    assert unchanged["read_at"] is None
    silences = client.get(
        f"/api/v1/devices/{device_id}/alert-silences", headers=_auth(tutor_a)
    ).json()["silences"]
    assert silences == []

    # The owner, on the other hand, still can.
    assert client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert['id']}/read", headers=_auth(tutor_a)
    ).status_code == 200


def test_a_tutor_neither_sees_nor_removes_the_silences_of_another_tutors_geofence_alert(
    client,
) -> None:
    tutor_a = _make_account(client, "TUTOR")
    tutor_b = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex
    device_id = _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    _link_a_device(client, tutor_b, supervised, device_instance_id=instance)
    _create_geofence(client, tutor_a, device_id, name="Casa", center=CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(30), **CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)
    [alert] = _geofence_alerts(client, tutor_a, device_id)
    silenced = client.post(
        f"/api/v1/devices/{device_id}/alerts/{alert['id']}/silence",
        json={},
        headers=_auth(tutor_a),
    )
    assert silenced.status_code == 200
    silence_id = silenced.json()["id"]

    # The key embeds the geofence id, so another tutor listing it would learn that it exists.
    listed = client.get(f"/api/v1/devices/{device_id}/alert-silences", headers=_auth(tutor_b))
    assert listed.status_code == 200
    assert listed.json()["silences"] == []

    _assert_same_404_as_a_missing_one(
        client.delete(
            f"/api/v1/devices/{device_id}/alert-silences/{uuid.uuid4()}", headers=_auth(tutor_b)
        ),
        client.delete(
            f"/api/v1/devices/{device_id}/alert-silences/{silence_id}", headers=_auth(tutor_b)
        ),
    )

    # Still there, and still removable by its owner.
    mine = client.get(f"/api/v1/devices/{device_id}/alert-silences", headers=_auth(tutor_a))
    assert [s["id"] for s in mine.json()["silences"]] == [silence_id]
    assert client.delete(
        f"/api/v1/devices/{device_id}/alert-silences/{silence_id}", headers=_auth(tutor_a)
    ).status_code == 204


# ------------------------------------------------------------ a geofence whose creator left


def test_a_geofence_of_an_unlinked_tutor_is_not_evaluated_until_they_link_again(client) -> None:
    tutor = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex
    device_id = _link_a_device(client, tutor, supervised, device_instance_id=instance)
    _create_geofence(client, tutor, device_id, name="Casa", center=CENTER)
    _report_location(client, supervised, device_id, captured_at=_recent(40), **CENTER)

    _unlink(client, tutor, device_id)
    # Would be an EXIT for "Casa" if its creator were still linked. Sprint 73 (T5): with no active
    # link the report itself is refused (uniform 404), so nothing can be evaluated.
    refused = client.post(
        f"/api/v1/devices/{device_id}/location",
        json={**OUTSIDE, "accuracy_meters": 1500.0, "captured_at": _recent(30)},
        headers=_auth(supervised),
    )
    assert refused.status_code == 404

    _link_a_device(client, tutor, supervised, device_instance_id=instance)
    assert _events(client, tutor, device_id) == []
    assert _geofence_alerts(client, tutor, device_id) == []

    # Linked again: the very same geofence is evaluated as before (the last accepted report was
    # inside, so leaving now is its EXIT).
    _report_location(client, supervised, device_id, captured_at=_recent(20), **OUTSIDE)
    assert [(e["geofence_name"], e["event_type"]) for e in _events(client, tutor, device_id)] == [
        ("Casa", "EXIT")
    ]
    assert [a["alert_type"] for a in _geofence_alerts(client, tutor, device_id)] == [
        "GEOFENCE_EXIT"
    ]


def test_the_geofence_cap_counts_only_the_tutors_own_geofences(client) -> None:
    from app.core.config import settings

    tutor_a = _make_account(client, "TUTOR")
    tutor_b = _make_account(client, "TUTOR")
    supervised = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex
    device_id = _link_a_device(client, tutor_a, supervised, device_instance_id=instance)
    _link_a_device(client, tutor_b, supervised, device_instance_id=instance)
    for index in range(settings.max_geofences_per_device):
        _create_geofence(client, tutor_a, device_id, name=f"Zona {index}", center=CENTER)

    # A's hidden zones must neither block B nor reveal how many A has.
    _create_geofence(client, tutor_b, device_id, name="Colegio", center=OUTSIDE)
    over_cap = client.post(
        f"/api/v1/devices/{device_id}/geofences",
        json={"name": "Una más", "radius_meters": 500.0, **CENTER},
        headers=_auth(tutor_a),
    )
    assert over_cap.status_code == 422
    assert over_cap.json()["detail"] == "max_geofences_per_device_reached"

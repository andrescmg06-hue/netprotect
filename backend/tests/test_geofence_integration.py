import os
import uuid
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
    ),
]

# A point far enough from CENTER (below) that it lands outside every radius used in this file
# (max 500m) with room to spare — about 4.3km north, using the ~111.32km/degree-of-latitude
# approximation, which is more than precise enough at this scale.
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


def _report_location(
    client: TestClient,
    token: str,
    device_id: str,
    *,
    latitude: float,
    longitude: float,
    captured_at: str,
    accuracy_meters: float = 1500.0,
):
    return client.post(
        f"/api/v1/devices/{device_id}/location",
        json={
            "latitude": latitude,
            "longitude": longitude,
            "accuracy_meters": accuracy_meters,
            "captured_at": captured_at,
        },
        headers=_auth(token),
    )


def _create_geofence(
    client: TestClient,
    tutor_token: str,
    device_id: str,
    *,
    name: str = "Casa",
    latitude: float = CENTER["latitude"],
    longitude: float = CENTER["longitude"],
    radius_meters: float = 500.0,
):
    return client.post(
        f"/api/v1/devices/{device_id}/geofences",
        json={
            "name": name,
            "latitude": latitude,
            "longitude": longitude,
            "radius_meters": radius_meters,
        },
        headers=_auth(tutor_token),
    )


# ------------------------------------------------------------------------------------------ CRUD


def test_the_owning_tutor_can_create_a_geofence(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _create_geofence(client, tutor_token, device_id)

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["name"] == "Casa"
    assert body["latitude"] == pytest.approx(CENTER["latitude"])
    assert body["longitude"] == pytest.approx(CENTER["longitude"])
    assert body["radius_meters"] == pytest.approx(500.0)


def test_a_stranger_tutor_cannot_create_a_geofence(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _create_geofence(client, stranger_token, device_id)

    assert response.status_code == 404


def test_a_supervised_user_cannot_create_a_geofence(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _create_geofence(client, supervised_token, device_id)

    assert response.status_code == 404


def test_creating_a_geofence_for_a_nonexistent_device_is_404(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = _create_geofence(client, tutor_token, str(uuid.uuid4()))

    assert response.status_code == 404


def test_the_owning_tutor_can_list_geofences(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id, name="Casa")
    _create_geofence(client, tutor_token, device_id, name="Colegio")

    response = client.get(f"/api/v1/devices/{device_id}/geofences", headers=_auth(tutor_token))

    assert response.status_code == 200, response.text
    names = {geofence["name"] for geofence in response.json()["geofences"]}
    assert names == {"Casa", "Colegio"}


def test_a_stranger_tutor_cannot_list_geofences(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(f"/api/v1/devices/{device_id}/geofences", headers=_auth(stranger_token))

    assert response.status_code == 404


def test_the_owning_tutor_can_update_a_geofence(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    geofence_id = _create_geofence(client, tutor_token, device_id).json()["id"]

    response = client.put(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}",
        json={"name": "Casa nueva", "latitude": 4.6, "longitude": -74.1, "radius_meters": 200.0},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["name"] == "Casa nueva"
    assert body["latitude"] == pytest.approx(4.6)
    assert body["radius_meters"] == pytest.approx(200.0)


def test_updating_a_nonexistent_geofence_is_404(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.put(
        f"/api/v1/devices/{device_id}/geofences/{uuid.uuid4()}",
        json={"name": "X", "latitude": 4.6, "longitude": -74.1, "radius_meters": 200.0},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 404


def test_the_owning_tutor_can_delete_a_geofence(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    geofence_id = _create_geofence(client, tutor_token, device_id).json()["id"]

    response = client.delete(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}", headers=_auth(tutor_token)
    )
    assert response.status_code == 200, response.text

    again = client.delete(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}", headers=_auth(tutor_token)
    )
    assert again.status_code == 404

    listed = client.get(f"/api/v1/devices/{device_id}/geofences", headers=_auth(tutor_token))
    assert listed.json()["geofences"] == []


def test_max_geofences_per_device_is_enforced(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    for i in range(settings.max_geofences_per_device):
        created = _create_geofence(client, tutor_token, device_id, name=f"Zona {i}")
        assert created.status_code == 200, created.text

    response = _create_geofence(client, tutor_token, device_id, name="Una de más")

    assert response.status_code == 422


# -------------------------------------------------------------------------------- transitions


def test_the_first_report_against_a_geofence_establishes_a_baseline_without_an_event(
    client,
) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)

    report = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **CENTER
    )
    assert report.status_code == 200, report.text

    events = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(tutor_token)
    )
    assert events.status_code == 200, events.text
    assert events.json()["events"] == []


def test_moving_from_outside_to_inside_fires_an_enter_event(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **OUTSIDE
    )

    response = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:15:00Z", **CENTER
    )
    assert response.status_code == 200, response.text

    events = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(tutor_token)
    ).json()["events"]
    assert len(events) == 1
    assert events[0]["event_type"] == "ENTER"
    assert events[0]["geofence_name"] == "Casa"


def test_moving_from_inside_to_outside_fires_an_exit_event(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **CENTER
    )

    response = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:15:00Z", **OUTSIDE
    )
    assert response.status_code == 200, response.text

    events = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(tutor_token)
    ).json()["events"]
    assert len(events) == 1
    assert events[0]["event_type"] == "EXIT"


def test_staying_inside_across_reports_does_not_duplicate_events(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **OUTSIDE
    )
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:15:00Z", **CENTER
    )

    response = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:30:00Z", **CENTER
    )
    assert response.status_code == 200, response.text

    events = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(tutor_token)
    ).json()["events"]
    assert len(events) == 1  # only the single ENTER from the second report


def test_deleting_a_geofence_keeps_its_past_events_with_a_name_snapshot(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    geofence_id = _create_geofence(client, tutor_token, device_id, name="Casa").json()["id"]
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **OUTSIDE
    )
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:15:00Z", **CENTER
    )

    client.delete(
        f"/api/v1/devices/{device_id}/geofences/{geofence_id}", headers=_auth(tutor_token)
    )

    events = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(tutor_token)
    ).json()["events"]
    assert len(events) == 1
    assert events[0]["geofence_name"] == "Casa"
    assert events[0]["event_type"] == "ENTER"


def test_a_stranger_tutor_cannot_read_geofence_events(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _create_geofence(client, tutor_token, device_id)
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z", **OUTSIDE
    )
    _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:15:00Z", **CENTER
    )
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{device_id}/geofences/events", headers=_auth(stranger_token)
    )

    assert response.status_code == 404

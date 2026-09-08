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


def _report_location(
    client: TestClient,
    token: str,
    device_id: str,
    *,
    latitude: float = 4.710989,
    longitude: float = -74.072092,
    accuracy_meters: float = 1500.0,
    captured_at: str = "2026-09-07T09:00:00Z",
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


# --------------------------------------------------------------------------------- reporting


def test_the_owning_device_can_report_its_location(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _report_location(client, supervised_token, device_id)

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["latitude"] == pytest.approx(4.710989)
    assert body["longitude"] == pytest.approx(-74.072092)
    assert body["accuracy_meters"] == pytest.approx(1500.0)
    assert body["captured_at"] is not None
    assert body["received_at"] is not None


def test_a_device_cannot_report_location_for_another_device(client) -> None:
    _, _, device_id = _setup_linked_device(client)
    other_supervised_token, _ = _make_account(client, "SUPERVISADO")

    response = _report_location(client, other_supervised_token, device_id)

    assert response.status_code == 404


def test_a_tutor_cannot_report_location_as_if_it_were_the_device(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = _report_location(client, tutor_token, device_id)

    assert response.status_code == 404


def test_reporting_location_for_a_nonexistent_device_is_404(client) -> None:
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    response = _report_location(client, supervised_token, str(uuid.uuid4()))

    assert response.status_code == 404


# --------------------------------------------------------------------------------- reading


def test_the_owning_tutor_can_read_the_latest_location(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id)

    response = client.get(
        f"/api/v1/devices/{device_id}/location/latest", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    report = response.json()["report"]
    assert report is not None
    assert report["latitude"] == pytest.approx(4.710989)
    assert report["longitude"] == pytest.approx(-74.072092)


def test_latest_is_null_when_the_device_never_reported(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    response = client.get(
        f"/api/v1/devices/{device_id}/location/latest", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    assert response.json()["report"] is None


def test_a_stranger_tutor_cannot_read_the_latest_location(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{device_id}/location/latest", headers=_auth(stranger_token)
    )

    assert response.status_code == 404


def test_a_stranger_tutor_cannot_read_the_location_history(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(stranger_token)
    )

    assert response.status_code == 404


def test_reading_location_for_a_nonexistent_device_is_404(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{uuid.uuid4()}/location/latest", headers=_auth(tutor_token)
    )

    assert response.status_code == 404


def test_the_owning_tutor_can_read_the_full_history(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id, captured_at="2026-09-06T09:00:00Z")
    _report_location(client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z")

    response = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    reports = response.json()["reports"]
    assert len(reports) == 2
    # Most recent first.
    assert reports[0]["captured_at"] > reports[1]["captured_at"]


# --------------------------------------------------------------------------------- retention


def test_reporting_purges_rows_older_than_the_retention_window(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    # settings.location_retention_days defaults to 7 (see app/core/config.py) — 30 days back is
    # unambiguously outside the window regardless of when this test runs.
    old_report = _report_location(
        client, supervised_token, device_id, captured_at="2026-08-08T09:00:00Z"
    )
    assert old_report.status_code == 200, old_report.text

    before_purge = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )
    assert len(before_purge.json()["reports"]) == 1

    # A fresh report triggers the write endpoint's inline purge (app/api/v1/endpoints/
    # location.py) before inserting itself, deleting the row above.
    fresh_report = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z"
    )
    assert fresh_report.status_code == 200, fresh_report.text

    after_purge = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )
    reports = after_purge.json()["reports"]
    assert len(reports) == 1
    assert reports[0]["captured_at"].startswith("2026-09-07")


def test_purge_never_touches_another_devices_rows(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)
    other_tutor_token, other_supervised_token, other_device_id = _setup_linked_device(client)

    # An old row on a different device must survive this device's own purge — the DELETE in
    # the write endpoint is scoped to `device_id`, never a global sweep (see
    # app/api/v1/endpoints/location.py).
    old_on_other_device = _report_location(
        client, other_supervised_token, other_device_id, captured_at="2026-08-08T09:00:00Z"
    )
    assert old_on_other_device.status_code == 200, old_on_other_device.text

    fresh_on_this_device = _report_location(
        client, supervised_token, device_id, captured_at="2026-09-07T09:00:00Z"
    )
    assert fresh_on_this_device.status_code == 200, fresh_on_this_device.text

    other_history = client.get(
        f"/api/v1/devices/{other_device_id}/location/history", headers=_auth(other_tutor_token)
    )
    assert other_history.status_code == 200, other_history.text
    assert len(other_history.json()["reports"]) == 1

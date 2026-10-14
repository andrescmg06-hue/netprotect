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


def _link_a_device(
    client: TestClient,
    tutor_token: str,
    supervised_token: str,
    device_instance_id: str | None = None,
) -> str:
    code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).json()["code"]
    redeemed = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": code,
            "device_instance_id": device_instance_id or uuid.uuid4().hex,
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


def _recent(minutes_ago: int = 0) -> str:
    """A `captured_at` safely inside location_retention_days (7, see app/core/config.py)
    regardless of when the suite runs — a fixed calendar date eventually ages past that 7-day
    window and starts failing the moment "now" catches up to it (see docs/sprint-26-evidence.md).
    """
    return (datetime.now(UTC) - timedelta(minutes=minutes_ago)).strftime("%Y-%m-%dT%H:%M:%SZ")


def _report_location(
    client: TestClient,
    token: str,
    device_id: str,
    *,
    latitude: float = 4.710989,
    longitude: float = -74.072092,
    accuracy_meters: float = 1500.0,
    captured_at: str | None = None,
):
    return client.post(
        f"/api/v1/devices/{device_id}/location",
        json={
            "latitude": latitude,
            "longitude": longitude,
            "accuracy_meters": accuracy_meters,
            "captured_at": captured_at if captured_at is not None else _recent(),
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
    _report_location(client, supervised_token, device_id, captured_at=_recent(10))
    _report_location(client, supervised_token, device_id, captured_at=_recent(0))

    response = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )

    assert response.status_code == 200, response.text
    reports = response.json()["reports"]
    assert len(reports) == 2
    # Most recent first.
    assert reports[0]["captured_at"] > reports[1]["captured_at"]


# ------------------------------------------------ reads start at the tutor's own link (B-09)


def _read(client: TestClient, tutor_token: str, device_id: str) -> tuple[dict, list]:
    latest = client.get(f"/api/v1/devices/{device_id}/location/latest", headers=_auth(tutor_token))
    history = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )
    assert latest.status_code == 200, latest.text
    assert history.status_code == 200, history.text
    return latest.json(), history.json()["reports"]


def test_a_new_tutor_does_not_see_locations_received_before_their_link(client) -> None:
    """B-09: relinking the same installation reuses the device row. The new tutor must see
    nothing that was received while only the previous tutor was linked."""
    old_tutor, _ = _make_account(client, "TUTOR")
    new_tutor, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex

    device_id = _link_a_device(client, old_tutor, supervised_token, instance)
    _report_location(client, supervised_token, device_id, captured_at=_recent(20))
    _report_location(client, supervised_token, device_id, captured_at=_recent(10))
    unlinked = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(old_tutor))
    assert unlinked.status_code == 200

    assert _link_a_device(client, new_tutor, supervised_token, instance) == device_id

    latest, history = _read(client, new_tutor, device_id)
    assert latest["report"] is None
    assert history == []

    # What arrives after the new link is theirs to see.
    _report_location(client, supervised_token, device_id, captured_at=_recent(0))
    latest, history = _read(client, new_tutor, device_id)
    assert latest["report"] is not None
    assert len(history) == 1


def test_each_tutor_of_the_same_device_sees_from_their_own_link(client) -> None:
    """Two tutors linked at the same time to one device: the one who linked first keeps the
    whole history, the later one starts at their own link."""
    first_tutor, _ = _make_account(client, "TUTOR")
    second_tutor, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex

    device_id = _link_a_device(client, first_tutor, supervised_token, instance)
    _report_location(client, supervised_token, device_id, captured_at=_recent(10))
    assert _link_a_device(client, second_tutor, supervised_token, instance) == device_id
    _report_location(client, supervised_token, device_id, captured_at=_recent(0))

    _, first_history = _read(client, first_tutor, device_id)
    _, second_history = _read(client, second_tutor, device_id)
    assert len(first_history) == 2
    assert len(second_history) == 1


def test_a_tutor_who_relinks_does_not_see_what_arrived_in_between(client) -> None:
    """The rule is "only what was received while the link is active", also for the same tutor."""
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    instance = uuid.uuid4().hex

    device_id = _link_a_device(client, tutor_token, supervised_token, instance)
    _report_location(client, supervised_token, device_id, captured_at=_recent(10))
    unlinked = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))
    assert unlinked.status_code == 200
    _report_location(client, supervised_token, device_id, captured_at=_recent(5))
    assert _link_a_device(client, tutor_token, supervised_token, instance) == device_id
    _report_location(client, supervised_token, device_id, captured_at=_recent(0))

    _, history = _read(client, tutor_token, device_id)
    assert len(history) == 1


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
    fresh_captured_at = _recent(0)
    fresh_report = _report_location(
        client, supervised_token, device_id, captured_at=fresh_captured_at
    )
    assert fresh_report.status_code == 200, fresh_report.text

    after_purge = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )
    reports = after_purge.json()["reports"]
    assert len(reports) == 1
    assert reports[0]["captured_at"] == fresh_captured_at


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
        client, supervised_token, device_id, captured_at=_recent(0)
    )
    assert fresh_on_this_device.status_code == 200, fresh_on_this_device.text

    other_history = client.get(
        f"/api/v1/devices/{other_device_id}/location/history", headers=_auth(other_tutor_token)
    )
    assert other_history.status_code == 200, other_history.text
    assert len(other_history.json()["reports"]) == 1


# ------------------------------------------------------------------ audit of reads (Sprint 47)


def _my_audit(client: TestClient, token: str, action: str) -> list[dict]:
    response = client.get(
        "/api/v1/users/me/audit", params={"action": action, "limit": 100}, headers=_auth(token)
    )
    assert response.status_code == 200, response.text
    return response.json()["logs"]


def test_reading_the_latest_location_is_audited_once_without_location_data(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id, latitude=4.151234, longitude=-73.634567)

    response = client.get(
        f"/api/v1/devices/{device_id}/location/latest", headers=_auth(tutor_token)
    )
    assert response.status_code == 200, response.text

    logs = _my_audit(client, tutor_token, "LOCATION_VIEWED")
    assert len(logs) == 1
    entry = logs[0]
    assert entry["resource_type"] == "device"
    assert entry["resource_id"] == device_id
    # Invariant 7: the audit log is not a second copy of the location.
    serialized = str(entry)
    for leaked in ("4.15", "-73.63", "1500", "latitude", "longitude", "accuracy"):
        assert leaked not in serialized


def test_every_read_is_one_row_even_when_nothing_was_reported(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)

    for _ in range(2):
        response = client.get(
            f"/api/v1/devices/{device_id}/location/latest", headers=_auth(tutor_token)
        )
        assert response.status_code == 200
        assert response.json()["report"] is None

    assert len(_my_audit(client, tutor_token, "LOCATION_VIEWED")) == 2


def test_a_rejected_read_leaves_no_audit_row(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id)
    stranger_token, _ = _make_account(client, "TUTOR")

    for path in ("latest", "history"):
        response = client.get(
            f"/api/v1/devices/{device_id}/location/{path}", headers=_auth(stranger_token)
        )
        assert response.status_code == 404
    response = client.get(
        f"/api/v1/devices/{uuid.uuid4()}/location/latest", headers=_auth(stranger_token)
    )
    assert response.status_code == 404

    assert _my_audit(client, stranger_token, "LOCATION_VIEWED") == []
    assert _my_audit(client, stranger_token, "LOCATION_HISTORY_VIEWED") == []


def test_reading_the_location_history_is_audited_separately(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    _report_location(client, supervised_token, device_id)

    response = client.get(
        f"/api/v1/devices/{device_id}/location/history", headers=_auth(tutor_token)
    )
    assert response.status_code == 200, response.text

    logs = _my_audit(client, tutor_token, "LOCATION_HISTORY_VIEWED")
    assert [(log["resource_type"], log["resource_id"]) for log in logs] == [("device", device_id)]
    assert _my_audit(client, tutor_token, "LOCATION_VIEWED") == []


def test_the_device_reporting_its_location_is_not_audited(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    assert _report_location(client, supervised_token, device_id).status_code == 200

    assert _my_audit(client, supervised_token, "LOCATION_VIEWED") == []

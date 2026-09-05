import os
import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

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


# --------------------------------------------------------------------------------- listing


def test_a_freshly_linked_device_appears_online(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    listing = client.get("/api/v1/devices", headers=_auth(tutor_token))

    assert listing.status_code == 200
    devices = listing.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["id"] == device_id
    assert devices[0]["status"]["status"] == "ONLINE"
    assert devices[0]["name"] == "Celular de Juan"


def test_a_tutor_only_sees_their_own_devices(client) -> None:
    tutor_a, _ = _make_account(client, "TUTOR")
    tutor_b, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    _link_a_device(client, tutor_a, supervised_token)

    listing_b = client.get("/api/v1/devices", headers=_auth(tutor_b))

    assert listing_b.status_code == 200
    assert listing_b.json()["devices"] == []


def test_an_unlinked_device_drops_off_the_list(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))
    listing = client.get("/api/v1/devices", headers=_auth(tutor_token))

    assert listing.json()["devices"] == []


# ---------------------------------------------------------------------------------- detail


def test_a_tutor_can_read_their_devices_detail(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))

    assert detail.status_code == 200
    assert detail.json()["id"] == device_id


def test_a_stranger_cannot_read_someone_elses_device(client) -> None:
    owner_token, _ = _make_account(client, "TUTOR")
    stranger_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, owner_token, supervised_token)

    response = client.get(f"/api/v1/devices/{device_id}", headers=_auth(stranger_token))

    assert response.status_code == 404


# ---------------------------------------------------------------------------------- rename


def test_a_tutor_can_rename_their_device(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    renamed = client.patch(
        f"/api/v1/devices/{device_id}",
        json={"name": "Tablet de la sala"},
        headers=_auth(tutor_token),
    )

    assert renamed.status_code == 200
    assert renamed.json()["name"] == "Tablet de la sala"

    confirmed = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))
    assert confirmed.json()["name"] == "Tablet de la sala"


def test_a_stranger_cannot_rename_someone_elses_device(client) -> None:
    owner_token, _ = _make_account(client, "TUTOR")
    stranger_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, owner_token, supervised_token)

    response = client.patch(
        f"/api/v1/devices/{device_id}",
        json={"name": "Secuestrado"},
        headers=_auth(stranger_token),
    )

    assert response.status_code == 404


def test_an_empty_name_is_rejected(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = client.patch(
        f"/api/v1/devices/{device_id}", json={"name": ""}, headers=_auth(tutor_token)
    )

    assert response.status_code == 422


# ------------------------------------------------------------------------------- heartbeat


def test_the_supervised_device_can_send_its_own_heartbeat(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    beat = client.post(
        f"/api/v1/devices/{device_id}/heartbeat",
        json={"app_version": "0.2.0"},
        headers=_auth(supervised_token),
    )

    assert beat.status_code == 200
    assert beat.json()["status"] == "ONLINE"

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))
    assert detail.json()["app_version"] == "0.2.0"


def test_the_tutor_cannot_send_a_heartbeat_for_the_device(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = client.post(
        f"/api/v1/devices/{device_id}/heartbeat", json={}, headers=_auth(tutor_token)
    )

    assert response.status_code == 404


def test_another_supervised_account_cannot_heartbeat_a_device_that_is_not_theirs(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    owner_token, _ = _make_account(client, "SUPERVISADO")
    someone_else_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, owner_token)

    response = client.post(
        f"/api/v1/devices/{device_id}/heartbeat",
        json={},
        headers=_auth(someone_else_token),
    )

    assert response.status_code == 404


async def test_a_stale_heartbeat_reports_offline_without_a_background_job(
    client, db_session
) -> None:
    """No scheduler exists yet: staleness is computed when the tutor reads the device, not
    written by a periodic sweep."""
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    status_row = await db_session.get(DeviceStatus, uuid.UUID(device_id))
    status_row.last_seen_at = datetime.now(UTC) - timedelta(hours=1)
    await db_session.commit()

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))

    assert detail.json()["status"]["status"] == "OFFLINE"
    # The underlying fact (when it was last actually seen) is preserved, not overwritten.
    assert detail.json()["status"]["last_seen_at"] is not None


# ------------------------------------------------------------------------------ devices/me


def test_an_unlinked_supervised_account_gets_404_from_devices_me(client) -> None:
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    response = client.get("/api/v1/devices/me", headers=_auth(supervised_token))

    assert response.status_code == 404
    assert response.json()["detail"] == "not_linked"


def test_a_tutor_cannot_call_devices_me(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = client.get("/api/v1/devices/me", headers=_auth(tutor_token))

    assert response.status_code == 403


def test_a_linked_supervised_device_sees_itself_and_its_tutor(client) -> None:
    tutor_token, tutor_email = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    mine = client.get("/api/v1/devices/me", headers=_auth(supervised_token))

    assert mine.status_code == 200
    body = mine.json()
    assert body["device_id"] == device_id
    assert body["device_name"] == "Celular de Juan"
    assert body["status"]["status"] == "ONLINE"
    assert len(body["tutors"]) == 1
    assert body["tutors"][0]["email"] == tutor_email


def test_devices_me_lists_every_active_tutor(client) -> None:
    tutor_a, email_a = _make_account(client, "TUTOR")
    tutor_b, email_b = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_instance_id = uuid.uuid4().hex

    first_code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_a)).json()["code"]
    first_link = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": first_code,
            "device_instance_id": device_instance_id,
            "device_name": "Celular de Juan",
            "platform": "ANDROID",
            "os_version": "16",
            "app_version": "0.1.0",
        },
        headers=_auth(supervised_token),
    )
    assert first_link.status_code == 200, first_link.text
    device_id = first_link.json()["device_id"]

    # Same physical phone (same device_instance_id) redeems a second tutor's code, so this
    # must attach a second tutor to the existing device rather than creating a new one.
    second_code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_b)).json()["code"]
    second_link = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": second_code,
            "device_instance_id": device_instance_id,
            "device_name": "Celular de Juan",
            "platform": "ANDROID",
            "os_version": "16",
            "app_version": "0.1.0",
        },
        headers=_auth(supervised_token),
    )
    assert second_link.status_code == 200, second_link.text
    assert second_link.json()["device_id"] == device_id

    mine = client.get("/api/v1/devices/me", headers=_auth(supervised_token))

    assert mine.status_code == 200
    tutor_emails = {tutor["email"] for tutor in mine.json()["tutors"]}
    assert tutor_emails == {email_a, email_b}


def test_devices_me_drops_a_tutor_after_they_unlink(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))
    mine = client.get("/api/v1/devices/me", headers=_auth(supervised_token))

    assert mine.status_code == 200
    assert mine.json()["tutors"] == []

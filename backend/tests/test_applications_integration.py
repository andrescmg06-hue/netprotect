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


def _sync(
    client: TestClient,
    token: str,
    device_id: str,
    usage_date: str,
    apps: list[tuple[str, str, bool]],
    usage: list[tuple[str, int]] | None = None,
):
    return client.post(
        f"/api/v1/devices/{device_id}/applications/sync",
        json={
            "usage_date": usage_date,
            "installed_apps": [
                {"package_name": pkg, "app_label": label, "is_system_app": system}
                for pkg, label, system in apps
            ],
            "daily_usage": [
                {"package_name": pkg, "foreground_seconds": seconds}
                for pkg, seconds in (usage or [])
            ],
        },
        headers=_auth(token),
    )


# ------------------------------------------------------------------------------------- sync


def test_a_supervised_device_can_sync_its_own_applications(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = _sync(
        client,
        supervised_token,
        device_id,
        "2026-09-05",
        apps=[
            ("com.instagram.android", "Instagram", False),
            ("com.android.settings", "Ajustes", True),
        ],
        usage=[("com.instagram.android", 1800)],
    )

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["apps_synced"] == 2
    assert body["apps_marked_uninstalled"] == 0
    assert body["usage_rows_synced"] == 1


def test_a_tutor_cannot_sync_applications_for_the_device(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = _sync(client, tutor_token, device_id, "2026-09-05", apps=[])

    assert response.status_code == 404


def test_a_different_supervised_account_cannot_sync_someone_elses_device(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    owner_token, _ = _make_account(client, "SUPERVISADO")
    someone_else_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, owner_token)

    response = _sync(client, someone_else_token, device_id, "2026-09-05", apps=[])

    assert response.status_code == 404


def test_syncing_updates_the_device_status_last_sync_at(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    _sync(client, supervised_token, device_id, "2026-09-05", apps=[])

    detail = client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))
    assert detail.json()["status"]["last_sync_at"] is not None


def test_negative_usage_seconds_are_rejected(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = _sync(
        client,
        supervised_token,
        device_id,
        "2026-09-05",
        apps=[("com.instagram.android", "Instagram", False)],
        usage=[("com.instagram.android", -5)],
    )

    assert response.status_code == 422


# ------------------------------------------------------------------------------------ listing


def test_the_tutor_sees_synced_apps_with_their_latest_usage(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(
        client,
        supervised_token,
        device_id,
        "2026-09-05",
        apps=[("com.instagram.android", "Instagram", False)],
        usage=[("com.instagram.android", 1800)],
    )

    listing = client.get(f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token))

    assert listing.status_code == 200
    apps = listing.json()["applications"]
    assert len(apps) == 1
    assert apps[0]["package_name"] == "com.instagram.android"
    assert apps[0]["app_label"] == "Instagram"
    assert apps[0]["uninstalled_at"] is None
    assert apps[0]["latest_usage"] == {"usage_date": "2026-09-05", "foreground_seconds": 1800}


def test_a_stranger_tutor_cannot_list_someone_elses_applications(client) -> None:
    owner_token, _ = _make_account(client, "TUTOR")
    stranger_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, owner_token, supervised_token)
    _sync(client, supervised_token, device_id, "2026-09-05", apps=[("com.a", "A", False)])

    response = client.get(
        f"/api/v1/devices/{device_id}/applications", headers=_auth(stranger_token)
    )

    assert response.status_code == 404


def test_the_supervised_device_itself_cannot_list_applications(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)

    response = client.get(
        f"/api/v1/devices/{device_id}/applications", headers=_auth(supervised_token)
    )

    assert response.status_code == 404


def test_an_app_with_no_usage_yet_reports_null_latest_usage(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(client, supervised_token, device_id, "2026-09-05", apps=[("com.a", "A", False)])

    listing = client.get(f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token))

    assert listing.json()["applications"][0]["latest_usage"] is None


# --------------------------------------------------------------------------- reconciliation


def test_re_syncing_the_same_package_updates_it_instead_of_duplicating(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(
        client, supervised_token, device_id, "2026-09-05",
        apps=[("com.instagram.android", "Instagram", False)],
    )

    _sync(
        client, supervised_token, device_id, "2026-09-06",
        apps=[("com.instagram.android", "Instagram (renamed)", False)],
    )

    apps = client.get(
        f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token)
    ).json()["applications"]
    assert len(apps) == 1
    assert apps[0]["app_label"] == "Instagram (renamed)"


def test_an_app_missing_from_a_later_sync_is_marked_uninstalled(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(
        client, supervised_token, device_id, "2026-09-05",
        apps=[("com.a", "A", False), ("com.b", "B", False)],
    )

    _sync(client, supervised_token, device_id, "2026-09-06", apps=[("com.a", "A", False)])

    apps = {
        app["package_name"]: app
        for app in client.get(
            f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token)
        ).json()["applications"]
    }
    assert apps["com.a"]["uninstalled_at"] is None
    assert apps["com.b"]["uninstalled_at"] is not None


def test_a_reinstalled_app_is_no_longer_marked_uninstalled(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    both_apps = [("com.a", "A", False), ("com.b", "B", False)]
    _sync(client, supervised_token, device_id, "2026-09-05", apps=both_apps)
    _sync(client, supervised_token, device_id, "2026-09-06", apps=[("com.a", "A", False)])

    _sync(client, supervised_token, device_id, "2026-09-07", apps=both_apps)

    apps = {
        app["package_name"]: app
        for app in client.get(
            f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token)
        ).json()["applications"]
    }
    assert apps["com.b"]["uninstalled_at"] is None


# -------------------------------------------------------------------------------------- usage


def test_resyncing_the_same_day_overwrites_rather_than_accumulates(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(
        client, supervised_token, device_id, "2026-09-05",
        apps=[("com.a", "A", False)], usage=[("com.a", 100)],
    )

    _sync(
        client, supervised_token, device_id, "2026-09-05",
        apps=[("com.a", "A", False)], usage=[("com.a", 150)],
    )

    apps = client.get(
        f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token)
    ).json()["applications"]
    assert apps[0]["latest_usage"]["foreground_seconds"] == 150


def test_the_most_recent_usage_day_wins_as_latest(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    _sync(
        client, supervised_token, device_id, "2026-09-05",
        apps=[("com.a", "A", False)], usage=[("com.a", 100)],
    )

    _sync(
        client, supervised_token, device_id, "2026-09-06",
        apps=[("com.a", "A", False)], usage=[("com.a", 200)],
    )

    apps = client.get(
        f"/api/v1/devices/{device_id}/applications", headers=_auth(tutor_token)
    ).json()["applications"]
    assert apps[0]["latest_usage"] == {"usage_date": "2026-09-06", "foreground_seconds": 200}

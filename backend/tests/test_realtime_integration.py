import os
import uuid
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

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
    tutor_token = _make_account(client, "TUTOR")
    supervised_token = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    return tutor_token, supervised_token, device_id


# --------------------------------------------------------------------------------- handshake


def test_ws_rejects_a_missing_token(client) -> None:
    _tutor_token, _supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as ws:
        ws.send_json({"not_token": "x"})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()


def test_ws_rejects_an_invalid_token(client) -> None:
    _tutor_token, _supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as ws:
        ws.send_json({"token": "garbage"})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()


def test_ws_rejects_a_tutor_who_is_not_linked_to_this_device(client) -> None:
    _tutor_token, _supervised_token, device_id = _setup_linked_device(client)
    other_tutor_token = _make_account(client, "TUTOR")

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as ws:
        ws.send_json({"token": other_tutor_token})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()


def test_ws_rejects_a_device_id_that_does_not_exist(client) -> None:
    tutor_token, _supervised_token, _device_id = _setup_linked_device(client)
    random_device_id = str(uuid.uuid4())

    with client.websocket_connect(f"/api/v1/devices/{random_device_id}/ws") as ws:
        ws.send_json({"token": tutor_token})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()


def test_ws_accepts_the_owning_tutor_and_the_supervised_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
        tutor_ws.send_json({"token": tutor_token})

        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
            device_ws.send_json({"token": supervised_token})
            # Neither close(): reaching this line without an exception is the assertion —
            # both connections stayed open after presenting a valid, authorized token.


# ---------------------------------------------------------------------- end-to-end propagation


def test_a_rule_change_is_pushed_live_to_the_connected_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        device_ws.send_json({"token": supervised_token})

        response = client.post(
            f"/api/v1/devices/{device_id}/rules",
            json={"package_name": "com.instagram.android", "rule_type": "BLOCK"},
            headers=_auth(tutor_token),
        )
        assert response.status_code == 200, response.text

        message = device_ws.receive_json()
        assert message["event"] == "rules_changed"
        assert message["device_id"] == device_id


def test_a_policy_change_is_pushed_live_to_a_connected_tutor_viewer(client) -> None:
    tutor_token, _supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
        tutor_ws.send_json({"token": tutor_token})

        response = client.put(
            f"/api/v1/devices/{device_id}/policy",
            json={"default_app_policy": "BLOCK"},
            headers=_auth(tutor_token),
        )
        assert response.status_code == 200, response.text

        message = tutor_ws.receive_json()
        assert message["event"] == "rules_changed"


# --------------------------------------------------------------------------------- push token


def test_the_supervised_device_can_register_its_own_push_token(client) -> None:
    _tutor_token, supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/push-token",
        json={"fcm_token": "a-real-looking-fcm-token"},
        headers=_auth(supervised_token),
    )

    assert response.status_code == 200, response.text
    assert response.json()["device_id"] == device_id


def test_a_tutor_cannot_register_a_push_token_for_the_device_they_supervise(client) -> None:
    tutor_token, _supervised_token, device_id = _setup_linked_device(client)

    response = client.post(
        f"/api/v1/devices/{device_id}/push-token",
        json={"fcm_token": "should-not-work"},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 404


# --------------------------------------------------------------------- FCM wake-up fallback


def test_a_rule_change_wakes_a_disconnected_device_with_a_registered_token(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    client.post(
        f"/api/v1/devices/{device_id}/push-token",
        json={"fcm_token": "a-real-looking-fcm-token"},
        headers=_auth(supervised_token),
    )

    with patch(
        "app.services.realtime.send_rule_change_wake", new_callable=AsyncMock
    ) as wake_mock:
        response = client.post(
            f"/api/v1/devices/{device_id}/rules",
            json={"package_name": "com.instagram.android", "rule_type": "BLOCK"},
            headers=_auth(tutor_token),
        )
        assert response.status_code == 200, response.text

    wake_mock.assert_awaited_once()
    awaited_device_id = wake_mock.await_args.args[0]
    assert str(awaited_device_id) == device_id


def test_a_rule_change_does_not_wake_a_device_that_is_already_connected(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    client.post(
        f"/api/v1/devices/{device_id}/push-token",
        json={"fcm_token": "a-real-looking-fcm-token"},
        headers=_auth(supervised_token),
    )

    with patch(
        "app.services.realtime.send_rule_change_wake", new_callable=AsyncMock
    ) as wake_mock:
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
            device_ws.send_json({"token": supervised_token})

            response = client.post(
                f"/api/v1/devices/{device_id}/rules",
                json={"package_name": "com.instagram.android", "rule_type": "BLOCK"},
                headers=_auth(tutor_token),
            )
            assert response.status_code == 200, response.text
            device_ws.receive_json()

    wake_mock.assert_not_awaited()

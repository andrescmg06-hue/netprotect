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


def _authenticate_ws(ws, token: str) -> None:
    """Sends the required first frame and waits for the server to acknowledge it.

    Waiting matters, it is not politeness: the acknowledgement is sent right after the connection
    is registered, so a test that has received it knows this socket can be relayed to. Without
    that barrier, a peer connecting second could send before the first one finished registering,
    and the frame would be relayed to nobody.
    """
    ws.send_json({"token": token})
    assert ws.receive_json()["event"] == "connected"


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
        _authenticate_ws(tutor_ws, tutor_token)

        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
            _authenticate_ws(device_ws, supervised_token)
            # Neither close(): reaching this line without an exception is the assertion —
            # both connections stayed open after presenting a valid, authorized token.


# ---------------------------------------------------------------------- end-to-end propagation


def test_a_rule_change_is_pushed_live_to_the_connected_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)

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
        _authenticate_ws(tutor_ws, tutor_token)

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
        "app.services.realtime.send_fcm_wake", new_callable=AsyncMock
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
        "app.services.realtime.send_fcm_wake", new_callable=AsyncMock
    ) as wake_mock:
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
            _authenticate_ws(device_ws, supervised_token)

            response = client.post(
                f"/api/v1/devices/{device_id}/rules",
                json={"package_name": "com.instagram.android", "rule_type": "BLOCK"},
                headers=_auth(tutor_token),
            )
            assert response.status_code == 200, response.text
            device_ws.receive_json()

    wake_mock.assert_not_awaited()


# ------------------------------------------------- screen sharing: signalling relay (Sprint 23)


def _audit_actions(client: TestClient, token: str) -> list[str]:
    response = client.get("/api/v1/users/me/audit?limit=100", headers=_auth(token))
    assert response.status_code == 200, response.text
    return [entry["action"] for entry in response.json()["logs"]]


def test_a_tutor_request_reaches_the_connected_device(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)
            tutor_ws.send_json({"type": "screen_share_request"})

            assert device_ws.receive_json() == {"type": "screen_share_request"}

    assert "SCREEN_SHARE_REQUESTED" in _audit_actions(client, tutor_token)


def test_the_full_offer_answer_exchange_is_relayed_between_the_two_peers(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)

            tutor_ws.send_json({"type": "screen_share_request"})
            assert device_ws.receive_json()["type"] == "screen_share_request"

            device_ws.send_json({"type": "screen_share_consent", "granted": True})
            consent = tutor_ws.receive_json()
            assert consent == {"type": "screen_share_consent", "granted": True}

            device_ws.send_json({"type": "screen_share_offer", "sdp": "v=0\r\no=- offer"})
            assert tutor_ws.receive_json()["sdp"] == "v=0\r\no=- offer"

            tutor_ws.send_json({"type": "screen_share_answer", "sdp": "v=0\r\no=- answer"})
            assert device_ws.receive_json()["sdp"] == "v=0\r\no=- answer"

            device_ws.send_json(
                {
                    "type": "screen_share_ice_candidate",
                    "candidate": "candidate:1 1 udp 2130706431 10.0.2.15 54321 typ host",
                    "sdp_mid": "0",
                    "sdp_m_line_index": 0,
                }
            )
            relayed = tutor_ws.receive_json()
            assert relayed["type"] == "screen_share_ice_candidate"
            assert relayed["sdp_mid"] == "0"

            tutor_ws.send_json({"type": "screen_share_stop", "reason": "tutor_closed"})
            assert device_ws.receive_json()["reason"] == "tutor_closed"

    tutor_actions = _audit_actions(client, tutor_token)
    supervised_actions = _audit_actions(client, supervised_token)
    assert "SCREEN_SHARE_REQUESTED" in tutor_actions
    assert "SCREEN_SHARE_STOPPED" in tutor_actions
    # The consent and the moment sharing actually started are the supervised user's own actions,
    # so they land in *their* audit trail, not the tutor's — audit is scoped by actor (Sprint 22).
    assert "SCREEN_SHARE_CONSENT_GRANTED" in supervised_actions
    assert "SCREEN_SHARE_STARTED" in supervised_actions


def test_a_denied_consent_is_relayed_and_audited(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)

            # The request is what opens the session; a device answering out of the blue has no
            # session to answer into, so it has to come first.
            tutor_ws.send_json({"type": "screen_share_request"})
            assert device_ws.receive_json()["type"] == "screen_share_request"

            device_ws.send_json({"type": "screen_share_consent", "granted": False})
            assert tutor_ws.receive_json() == {"type": "screen_share_consent", "granted": False}

    assert "SCREEN_SHARE_CONSENT_DENIED" in _audit_actions(client, supervised_token)


def test_a_peer_cannot_send_a_frame_that_belongs_to_the_other_role(client) -> None:
    """A supervised user must not be able to fake the tutor's half of the conversation (or vice
    versa): the role is the one fixed at authentication, not anything inside the frame."""
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)

            tutor_ws.send_json({"type": "screen_share_request"})
            assert device_ws.receive_json()["type"] == "screen_share_request"

            # The device tries to speak as the tutor, and the tutor as the device.
            device_ws.send_json({"type": "screen_share_request"})
            tutor_ws.send_json({"type": "screen_share_consent", "granted": True})

            # Neither is relayed; the next legitimate frame is what actually arrives, proving
            # nothing was queued ahead of it.
            device_ws.send_json({"type": "screen_share_offer", "sdp": "v=0\r\no=- offer"})
            assert tutor_ws.receive_json()["type"] == "screen_share_offer"

    assert "SCREEN_SHARE_REQUESTED" not in _audit_actions(client, supervised_token)
    assert "SCREEN_SHARE_CONSENT_GRANTED" not in _audit_actions(client, tutor_token)


def test_a_malformed_frame_is_ignored_without_closing_the_channel(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)

            tutor_ws.send_text("ping")  # the pre-Sprint-23 liveness ping, still valid
            tutor_ws.send_json({"type": "screen_share_offer", "sdp": ""})  # fails validation
            tutor_ws.send_json({"type": "not_a_real_signal"})
            tutor_ws.send_json({"type": "screen_share_answer", "sdp": "x" * 20000})  # too long

            tutor_ws.send_json({"type": "screen_share_request"})
            assert device_ws.receive_json()["type"] == "screen_share_request"


def test_a_request_wakes_a_device_that_is_not_connected(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    client.post(
        f"/api/v1/devices/{device_id}/push-token",
        json={"fcm_token": "a-real-looking-fcm-token"},
        headers=_auth(supervised_token),
    )

    with patch("app.services.realtime.send_fcm_wake", new_callable=AsyncMock) as wake_mock:
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)
            tutor_ws.send_json({"type": "screen_share_request"})
            # Round-trips a frame the server *does* answer, so the request above is guaranteed
            # to have been processed before the patch is lifted.
            tutor_ws.send_text("ping")
            client.get(f"/api/v1/devices/{device_id}", headers=_auth(tutor_token))

    wake_mock.assert_awaited()
    assert wake_mock.await_args.args[2] == "screen_share_requested"


def test_the_stream_only_reaches_the_tutor_who_asked_for_it(client) -> None:
    """A device can have several active tutors, all with the channel open. The device's offer
    carries the credentials for the media connection, so it must reach only the tutor whose
    request the supervised person actually agreed to — not whoever else happens to be listening.
    """
    tutor_token = _make_account(client, "TUTOR")
    second_tutor_token = _make_account(client, "TUTOR")
    supervised_token = _make_account(client, "SUPERVISADO")

    # The same physical device redeems both tutors' codes — same device_instance_id, so both
    # links land on one device row, which is exactly the situation this test is about.
    device_instance_id = uuid.uuid4().hex
    device_id = None
    for token in (tutor_token, second_tutor_token):
        code = client.post("/api/v1/pairing/codes", headers=_auth(token)).json()["code"]
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
        device_id = device_id or redeemed.json()["device_id"]
        assert redeemed.json()["device_id"] == device_id

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as asking_ws:
            _authenticate_ws(asking_ws, tutor_token)
            with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as bystander_ws:
                _authenticate_ws(bystander_ws, second_tutor_token)

                asking_ws.send_json({"type": "screen_share_request"})
                assert device_ws.receive_json()["type"] == "screen_share_request"

                device_ws.send_json({"type": "screen_share_offer", "sdp": "v=0\r\no=- offer"})
                assert asking_ws.receive_json()["type"] == "screen_share_offer"

                # The bystander tutor cannot answer a session it was never given, and its frame
                # is not relayed to the device either.
                bystander_ws.send_json({"type": "screen_share_answer", "sdp": "v=0\r\no=- theirs"})
                asking_ws.send_json({"type": "screen_share_answer", "sdp": "v=0\r\no=- mine"})
                assert device_ws.receive_json()["sdp"] == "v=0\r\no=- mine"


def test_an_unlinked_tutor_cannot_start_a_session_on_a_socket_it_already_had_open(client) -> None:
    """The socket outlives the access token that opened it, and nothing closes it when the tutor
    is unlinked — so the frame that starts a session re-checks the grant against the database."""
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as device_ws:
        _authenticate_ws(device_ws, supervised_token)
        with client.websocket_connect(f"/api/v1/devices/{device_id}/ws") as tutor_ws:
            _authenticate_ws(tutor_ws, tutor_token)

            unlinked = client.delete(
                f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token)
            )
            assert unlinked.status_code == 200, unlinked.text

            tutor_ws.send_json({"type": "screen_share_request"})
            device_ws.send_text("ping")

    assert "SCREEN_SHARE_REQUESTED" not in _audit_actions(client, tutor_token)


# ------------------------------------------------------------------------------ WebRTC config


def test_both_peers_can_read_the_webrtc_config(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)

    for token in (tutor_token, supervised_token):
        response = client.get(
            f"/api/v1/devices/{device_id}/webrtc-config", headers=_auth(token)
        )
        assert response.status_code == 200, response.text
        assert response.json()["ice_servers"]


def test_an_unrelated_tutor_cannot_read_the_webrtc_config(client) -> None:
    _tutor_token, _supervised_token, device_id = _setup_linked_device(client)
    other_tutor_token = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{device_id}/webrtc-config", headers=_auth(other_tutor_token)
    )

    assert response.status_code == 404

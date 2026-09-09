import asyncio
import json
import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect
from pydantic import TypeAdapter, ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_device_participant, require_supervised_owner_of_device
from app.core.config import settings
from app.core.security import InvalidAccessTokenError, decode_access_token
from app.db.session import get_db
from app.models import Device, TutorDevice, User
from app.schemas.realtime import (
    RegisterPushTokenRequest,
    RegisterPushTokenResponse,
    ScreenShareSignal,
    WebRtcConfigResponse,
)
from app.services.audit import record_audit_event
from app.services.realtime import (
    ROLE_DEVICE,
    ROLE_TUTOR,
    connection_manager,
    notify_screen_share_requested,
)

router = APIRouter(tags=["realtime"])

# Close codes above 4000 are the private-use range the WebSocket spec reserves for
# applications. 4401/4404 deliberately echo the HTTP status codes this project's REST
# endpoints already use for the same two situations (see require_tutor_of_device's
# docstring): "you never proved who you are" vs. "this device isn't yours, and we won't say
# whether it's because it doesn't exist" (anti-IDOR/BOLA, same reasoning as every REST
# device endpoint).
_WS_UNAUTHENTICATED = 4401
_WS_DEVICE_NOT_FOUND = 4404


# Which role is allowed to *send* each signalling frame. Direction is authorization, not
# bookkeeping: without it a supervised user could send `screen_share_answer` (impersonating the
# viewer half of their own session) or a tutor could send `screen_share_consent` and have the
# panel show a consent the supervised person never gave. The role compared here is the one fixed
# at authentication time, never a field inside the frame.
_SIGNAL_SENDER_ROLES: dict[str, frozenset[str]] = {
    "screen_share_request": frozenset({ROLE_TUTOR}),
    "screen_share_consent": frozenset({ROLE_DEVICE}),
    "screen_share_offer": frozenset({ROLE_DEVICE}),
    "screen_share_answer": frozenset({ROLE_TUTOR}),
    "screen_share_ice_candidate": frozenset({ROLE_TUTOR, ROLE_DEVICE}),
    "screen_share_stop": frozenset({ROLE_TUTOR, ROLE_DEVICE}),
}

_signal_adapter: TypeAdapter[ScreenShareSignal] = TypeAdapter(ScreenShareSignal)


async def _resolve_role(device_id: uuid.UUID, user_id: uuid.UUID, db: AsyncSession) -> str | None:
    """The device's own user, one of its active tutors, or neither.

    Takes a plain id rather than a loaded User on purpose: it is also called after expiring the
    session's identity map (see _still_authorized), where touching an ORM instance's attributes
    would trigger a lazy reload at exactly the wrong moment.
    """
    supervised_user_id = (
        await db.execute(select(Device.supervised_user_id).where(Device.id == device_id))
    ).scalar_one_or_none()
    if supervised_user_id is None:
        return None
    if supervised_user_id == user_id:
        return ROLE_DEVICE

    is_tutor = (
        await db.execute(
            select(TutorDevice.id).where(
                TutorDevice.device_id == device_id,
                TutorDevice.tutor_user_id == user_id,
                TutorDevice.unlinked_at.is_(None),
            )
        )
    ).first() is not None
    return ROLE_TUTOR if is_tutor else None


async def _still_authorized(
    device_id: uuid.UUID, user_id: uuid.UUID, role: str, db: AsyncSession
) -> bool:
    """Re-checks, against the database, that this connection's grant still holds.

    A WebSocket outlives the access token that opened it, and nothing closes one when a tutor is
    unlinked or an account is deactivated. That was harmless while the channel only pushed
    `rules_changed`; it is not harmless now that the same socket can ask to watch a child's
    screen. Rather than tracking revocations across the app, the one frame that starts a session
    pays for a fresh check — the frames that follow can only exist inside a session it allowed.

    expire_all() matters: this session has been open since the handshake, so without it the
    tutor link it already loaded would be answered from the identity map, and a revocation that
    happened since would be invisible here.
    """
    db.expire_all()
    is_active = (
        await db.execute(select(User.is_active).where(User.id == user_id))
    ).scalar_one_or_none()
    if not is_active:
        return False
    return await _resolve_role(device_id, user_id, db) == role


async def _authenticate(
    device_id: uuid.UUID, websocket: WebSocket, db: AsyncSession
) -> tuple[uuid.UUID, str] | None:
    """Waits for the required first frame, `{"token": "<access token>"}`, and resolves it to
    the caller's role on this device's channel. Returns None (after closing the socket
    itself) on any failure — nothing here is a header, because browsers cannot set custom
    headers on a WebSocket handshake, so authentication has to happen as the first message
    instead, uniformly for the web panel, Android and tests alike.
    """
    try:
        raw = await asyncio.wait_for(
            websocket.receive_json(), timeout=settings.ws_auth_timeout_seconds
        )
    except (TimeoutError, WebSocketDisconnect, ValueError):
        await websocket.close(code=_WS_UNAUTHENTICATED, reason="auth_timeout_or_invalid_frame")
        return None

    token = raw.get("token") if isinstance(raw, dict) else None
    if not token:
        await websocket.close(code=_WS_UNAUTHENTICATED, reason="missing_token")
        return None

    try:
        user_id = decode_access_token(token)
    except InvalidAccessTokenError:
        await websocket.close(code=_WS_UNAUTHENTICATED, reason="invalid_access_token")
        return None

    user = await db.get(User, user_id)
    if user is None or not user.is_active:
        await websocket.close(code=_WS_UNAUTHENTICATED, reason="invalid_access_token")
        return None

    role = await _resolve_role(device_id, user.id, db)
    if role is None:
        await websocket.close(code=_WS_DEVICE_NOT_FOUND, reason="device_not_found")
        return None
    # Only the id travels on: _still_authorized() expires this session's identity map on every
    # session-opening frame, and a User instance held across that would reload itself the next
    # time any attribute was read — inside a WebSocket task, where that failure is invisible.
    return user.id, role


def _audit_action_for(signal: ScreenShareSignal) -> str | None:
    """Which signalling frames leave a permanent trace, and which are just plumbing.

    Audited: the tutor's request, the supervised user's answer to it, the moment sharing really
    starts, and the moment it stops — the four points a person reviewing the log would care
    about ("who asked to watch, did they agree, did it actually happen, how long did it last").

    Not audited: SDP answers and ICE candidates. They carry no decision — dozens of ICE frames
    can cross a single session — and logging them would bury the four events above in noise.

    `screen_share_offer` stands in for "sharing started" because it is the first thing the device
    sends *after* Android's own capture dialog was accepted: the backend cannot observe that
    dialog (it is an OS screen), so the offer is the closest honest proxy it has.
    """
    if signal.type == "screen_share_request":
        return "SCREEN_SHARE_REQUESTED"
    if signal.type == "screen_share_consent":
        return "SCREEN_SHARE_CONSENT_GRANTED" if signal.granted else "SCREEN_SHARE_CONSENT_DENIED"
    if signal.type == "screen_share_offer":
        return "SCREEN_SHARE_STARTED"
    if signal.type == "screen_share_stop":
        return "SCREEN_SHARE_STOPPED"
    return None


async def _handle_signal(
    device_id: uuid.UUID,
    raw_text: str,
    websocket: WebSocket,
    user_id: uuid.UUID,
    role: str,
    db: AsyncSession,
) -> None:
    """Validates one inbound frame and relays it to the other peer, if it is a signalling frame.

    Anything that isn't valid signalling is dropped silently and the socket stays open: liveness
    pings (the only inbound traffic this channel carried before Sprint 23) still have to work,
    and a malformed frame from one peer is not a reason to tear down a session for both.

    A session belongs to *one* tutor connection: the one whose request opened it. Frames from any
    other tutor connection are dropped, and the device's own frames go only to that connection —
    see ConnectionManager.relay_to_screen_share_peer for why fanning them out to every tutor
    would hand the stream to someone the supervised person never agreed to.
    """
    try:
        signal = _signal_adapter.validate_python(json.loads(raw_text))
    except (ValueError, ValidationError):
        return

    if role not in _SIGNAL_SENDER_ROLES[signal.type]:
        return

    if signal.type == "screen_share_request":
        if not await _still_authorized(device_id, user_id, role, db):
            return
        connection_manager.begin_screen_share(device_id, websocket)
    elif role == ROLE_TUTOR and not connection_manager.is_screen_share_peer(device_id, websocket):
        return

    action = _audit_action_for(signal)
    if action is not None:
        await record_audit_event(
            db,
            actor_user_id=user_id,
            action=action,
            resource_type="device",
            resource_id=str(device_id),
        )
        await db.commit()

    message = signal.model_dump(mode="json")
    if role == ROLE_TUTOR:
        delivered = await connection_manager.relay_to_device(device_id, message)
    else:
        delivered = await connection_manager.relay_to_screen_share_peer(device_id, message)

    if signal.type == "screen_share_stop":
        connection_manager.end_screen_share(device_id)

    if signal.type == "screen_share_request" and delivered == 0:
        device = await db.get(Device, device_id)
        await notify_screen_share_requested(device_id, device.fcm_token if device else None)


@router.websocket("/devices/{device_id}/ws")
async def device_realtime_channel(
    device_id: uuid.UUID,
    websocket: WebSocket,
    db: AsyncSession = Depends(get_db),
) -> None:
    await websocket.accept()

    authenticated = await _authenticate(device_id, websocket, db)
    if authenticated is None:
        return
    user_id, role = authenticated

    connection_manager.register(device_id, websocket, role)
    # Sprint 23: acknowledge the handshake. Before this, a client could only infer that its token
    # was accepted from the *absence* of a close frame, which is not something a peer can wait
    # for — and screen sharing has to wait for it: a tutor whose request is relayed before the
    # device's own socket finished registering would have that request silently dropped, with
    # nobody left to answer it. Existing clients ignore frames they don't recognise, so this is
    # additive.
    await websocket.send_json({"event": "connected", "device_id": str(device_id), "role": role})
    try:
        while True:
            # Until Sprint 23 this channel was server -> client only and everything inbound was
            # discarded as a liveness ping. It now also carries WebRTC signalling between the two
            # peers (screen sharing), so each frame is parsed — and anything that isn't valid
            # signalling still falls through as a ping, exactly as before.
            raw_text = await websocket.receive_text()
            await _handle_signal(device_id, raw_text, websocket, user_id, role, db)
    except WebSocketDisconnect:
        pass
    finally:
        connection_manager.unregister(device_id, websocket)


@router.get("/devices/{device_id}/webrtc-config", response_model=WebRtcConfigResponse)
async def get_webrtc_config(
    _device: Device = Depends(require_device_participant),
) -> WebRtcConfigResponse:
    """The ICE servers both peers must agree on before they can try to connect.

    Served from the backend rather than compiled into each client so that adding a TURN server
    later is a config change, not an Android release plus a web deploy. Not audited: reading a
    connection parameter is not an action to review later — the session events in
    _audit_action_for are.
    """
    return WebRtcConfigResponse(ice_servers=settings.webrtc_stun_urls_list)


@router.post("/devices/{device_id}/push-token", response_model=RegisterPushTokenResponse)
async def register_push_token(
    device_id: uuid.UUID,
    payload: RegisterPushTokenRequest,
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_supervised_owner_of_device),
) -> RegisterPushTokenResponse:
    """The supervised install registers (or refreshes) its own FCM token — same ownership
    rule as the heartbeat. No audit entry, same reasoning as rename_device's neighbors that
    *are* audited doesn't quite apply here either way: unlike heartbeat this isn't a
    high-frequency ping, but it also isn't a tutor-facing action a tutor would ever review —
    it's the device configuring its own delivery channel, so device_status-style silence fits
    better than audit_log.
    """
    now = datetime.now(UTC)
    device.fcm_token = payload.fcm_token
    device.fcm_token_updated_at = now
    await db.commit()

    return RegisterPushTokenResponse(device_id=device_id, updated_at=now)

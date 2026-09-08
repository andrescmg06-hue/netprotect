import asyncio
import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_supervised_owner_of_device
from app.core.config import settings
from app.core.security import InvalidAccessTokenError, decode_access_token
from app.db.session import get_db
from app.models import Device, TutorDevice, User
from app.schemas.realtime import RegisterPushTokenRequest, RegisterPushTokenResponse
from app.services.realtime import ROLE_DEVICE, ROLE_TUTOR, connection_manager

router = APIRouter(tags=["realtime"])

# Close codes above 4000 are the private-use range the WebSocket spec reserves for
# applications. 4401/4404 deliberately echo the HTTP status codes this project's REST
# endpoints already use for the same two situations (see require_tutor_of_device's
# docstring): "you never proved who you are" vs. "this device isn't yours, and we won't say
# whether it's because it doesn't exist" (anti-IDOR/BOLA, same reasoning as every REST
# device endpoint).
_WS_UNAUTHENTICATED = 4401
_WS_DEVICE_NOT_FOUND = 4404


async def _authenticate(
    device_id: uuid.UUID, websocket: WebSocket, db: AsyncSession
) -> tuple[User, str] | None:
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

    device = await db.get(Device, device_id)
    if device is not None and device.supervised_user_id == user.id:
        return user, ROLE_DEVICE

    is_tutor = (
        await db.execute(
            select(TutorDevice.id).where(
                TutorDevice.device_id == device_id,
                TutorDevice.tutor_user_id == user.id,
                TutorDevice.unlinked_at.is_(None),
            )
        )
    ).first() is not None
    if is_tutor:
        return user, ROLE_TUTOR

    await websocket.close(code=_WS_DEVICE_NOT_FOUND, reason="device_not_found")
    return None


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
    _user, role = authenticated

    connection_manager.register(device_id, websocket, role)
    try:
        while True:
            # This channel is server -> client only (rules_changed pushes); any inbound frame
            # after auth is just a liveness ping to keep intermediaries from timing out the
            # connection, so it's read and discarded rather than interpreted.
            await websocket.receive_text()
    except WebSocketDisconnect:
        pass
    finally:
        connection_manager.unregister(device_id, websocket)


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

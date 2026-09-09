import uuid
from datetime import UTC, datetime

from fastapi import WebSocket

from app.services.push import send_fcm_wake

ROLE_DEVICE = "DEVICE"
ROLE_TUTOR = "TUTOR"


class ConnectionManager:
    """Per-device WebSocket registry, held in the process's own memory.

    The backend runs as a single uvicorn worker (see backend/Dockerfile — no `--workers`
    flag), so a plain in-memory dict is a correct broadcast bus today, not a shortcut taken
    for lack of time: there is only ever one process holding any given socket. Scaling the
    backend to more than one worker/replica would need a shared bus (Redis pub/sub, which
    this project already depends on for rate limiting) instead of this dict — deliberately
    deferred until that's a real requirement, same "don't build for a load this project
    doesn't have yet" reasoning as everywhere else no scheduler exists.

    Each connection is tagged with the role it authenticated as (the supervised device
    itself, or one of its tutors) because the two need different treatment: everybody in a
    device's channel gets broadcast a change, but only the *device* connection being open
    means the FCM wake-up nudge in notify_rules_changed() can be skipped — a tutor watching
    the web panel live is not the thing that needs waking up.
    """

    def __init__(self) -> None:
        self._connections: dict[uuid.UUID, dict[WebSocket, str]] = {}
        # Sprint 23: which tutor connection, if any, is the current screen-sharing peer for a
        # device. A device can have several active tutors (two parents, a co-tutor), and all of
        # them may have the channel open at once — so "the other role" is not the same thing as
        # "the peer in this session". Without pinning it, the device's offer would go to every
        # connected tutor, and whichever answered first would get the video: someone the
        # supervised person never agreed to, invisible in the audit trail.
        self._screen_share_peers: dict[uuid.UUID, WebSocket] = {}

    def register(self, device_id: uuid.UUID, websocket: WebSocket, role: str) -> None:
        self._connections.setdefault(device_id, {})[websocket] = role

    def unregister(self, device_id: uuid.UUID, websocket: WebSocket) -> None:
        if self._screen_share_peers.get(device_id) is websocket:
            del self._screen_share_peers[device_id]
        sockets = self._connections.get(device_id)
        if sockets is None:
            return
        sockets.pop(websocket, None)
        if not sockets:
            del self._connections[device_id]

    def begin_screen_share(self, device_id: uuid.UUID, tutor_socket: WebSocket) -> None:
        self._screen_share_peers[device_id] = tutor_socket

    def end_screen_share(self, device_id: uuid.UUID) -> None:
        self._screen_share_peers.pop(device_id, None)

    def is_screen_share_peer(self, device_id: uuid.UUID, websocket: WebSocket) -> bool:
        return self._screen_share_peers.get(device_id) is websocket

    def has_device_connection(self, device_id: uuid.UUID) -> bool:
        sockets = self._connections.get(device_id, {})
        return any(role == ROLE_DEVICE for role in sockets.values())

    async def broadcast(self, device_id: uuid.UUID, message: dict) -> None:
        sockets = list(self._connections.get(device_id, {}))
        for socket in sockets:
            try:
                await socket.send_json(message)
            except Exception:  # noqa: BLE001 -- a dead socket must not break the other listeners
                self.unregister(device_id, socket)

    async def relay_to_device(self, device_id: uuid.UUID, message: dict) -> int:
        """Tutor -> device. Goes to every connection holding the device role, because the
        supervised install legitimately holds more than one (the enforcement service's channel
        and the screen's own), and only one of them is listening for any given frame.
        """
        return await self._send_to_all(
            device_id,
            [
                socket
                for socket, role in self._connections.get(device_id, {}).items()
                if role == ROLE_DEVICE
            ],
            message,
        )

    async def relay_to_screen_share_peer(self, device_id: uuid.UUID, message: dict) -> int:
        """Device -> the one tutor connection that opened this session, never "all tutors".

        Deliberately not a broadcast: the device's offer carries the credentials for the media
        connection, so sending it to every connected tutor would let a tutor who was not the one
        consented to answer first and receive the stream instead — while the audit trail still
        named the tutor who asked.
        """
        peer = self._screen_share_peers.get(device_id)
        return await self._send_to_all(device_id, [peer] if peer is not None else [], message)

    async def _send_to_all(
        self, device_id: uuid.UUID, targets: list[WebSocket], message: dict
    ) -> int:
        delivered = 0
        for socket in targets:
            try:
                await socket.send_json(message)
                delivered += 1
            except Exception:  # noqa: BLE001 -- a dead socket must not break the other listeners
                self.unregister(device_id, socket)
        return delivered


connection_manager = ConnectionManager()


async def notify_rules_changed(device_id: uuid.UUID, fcm_token: str | None) -> None:
    """Fires after a rule-affecting write commits — see callers in rules.py/categories.py.

    Two independent delivery paths:
    1. Broadcast over the device's WebSocket channel — reaches the supervised device itself
       *and* any tutor viewers connected to the same channel, instantly, if either is open.
    2. If the device specifically isn't connected (a tutor viewer being connected doesn't
       count — see ConnectionManager's docstring), fall back to an FCM wake-up nudge so the
       device reconnects and picks up the change on its own instead of waiting for whatever
       polling interval it would otherwise be on.

    The message itself only says a change happened, not what changed: the receiver already has
    a single endpoint that returns its whole current rule set (`GET /devices/{id}/rules/active`),
    so re-fetching that is simpler and less failure-prone than trying to keep a push payload's
    shape in sync with it.
    """
    message = {
        "event": "rules_changed",
        "device_id": str(device_id),
        "changed_at": datetime.now(UTC).isoformat(),
    }

    await connection_manager.broadcast(device_id, message)

    if not connection_manager.has_device_connection(device_id) and fcm_token:
        await send_fcm_wake(device_id, fcm_token, "rules_changed")


async def notify_screen_share_requested(device_id: uuid.UUID, fcm_token: str | None) -> None:
    """Same wake-up fallback as notify_rules_changed, for the one signalling frame that can
    arrive while the device is asleep: a tutor asking to see the screen.

    Only this frame gets it. Everything after it (consent, offer, answer, ICE) only ever happens
    inside an already-established two-way exchange, so a peer that isn't connected for those is a
    session that ended, not one that needs waking. Nothing is queued for later delivery either:
    screen viewing is inherently live — a request redelivered ten minutes on would be a stranger
    asking to watch, not the tutor who is looking at the panel right now. If the device was
    asleep, the tutor asks again.
    """
    if connection_manager.has_device_connection(device_id) or not fcm_token:
        return
    await send_fcm_wake(device_id, fcm_token, "screen_share_requested")

import uuid
from datetime import UTC, datetime

from fastapi import WebSocket

from app.services.push import send_rule_change_wake

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

    def register(self, device_id: uuid.UUID, websocket: WebSocket, role: str) -> None:
        self._connections.setdefault(device_id, {})[websocket] = role

    def unregister(self, device_id: uuid.UUID, websocket: WebSocket) -> None:
        sockets = self._connections.get(device_id)
        if sockets is None:
            return
        sockets.pop(websocket, None)
        if not sockets:
            del self._connections[device_id]

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
        await send_rule_change_wake(device_id, fcm_token)

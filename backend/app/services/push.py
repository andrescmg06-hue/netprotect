import logging
import uuid

from google.auth.exceptions import GoogleAuthError
from google.auth.transport.requests import Request as GoogleAuthRequest
from google.oauth2.service_account import Credentials
from requests import RequestException, post

from app.core.config import settings

logger = logging.getLogger(__name__)

_FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
_FCM_SEND_TIMEOUT_SECONDS = 5


def is_push_configured() -> bool:
    return bool(settings.fcm_project_id and settings.fcm_service_account_file)


def _get_access_token() -> str:
    """Exchanges the service-account key for a short-lived OAuth2 bearer token.

    Not cached across calls: FCM wake-ups are rare (only when a rule changes while the
    target device has no open WebSocket), so the extra round trip is cheaper than the
    complexity of tracking token expiry for a call path this infrequent.
    """
    credentials = Credentials.from_service_account_file(
        settings.fcm_service_account_file, scopes=[_FCM_SCOPE]
    )
    credentials.refresh(GoogleAuthRequest())
    return credentials.token


def _post_fcm_message(access_token: str, payload: dict) -> None:
    """The one function that actually talks to Google — kept separate so tests can patch
    exactly this call, the same `unittest.mock.patch` pattern the project already uses for
    Google ID token verification (docs/sprint-03.md), instead of mocking this project's own
    logic.
    """
    url = f"https://fcm.googleapis.com/v1/projects/{settings.fcm_project_id}/messages:send"
    response = post(
        url,
        json=payload,
        headers={"Authorization": f"Bearer {access_token}"},
        timeout=_FCM_SEND_TIMEOUT_SECONDS,
    )
    response.raise_for_status()


async def send_rule_change_wake(device_id: uuid.UUID, fcm_token: str) -> None:
    """Best-effort: a tutor's rule change must succeed whether or not this nudge is delivered
    (see notify_rules_changed's docstring), so every failure here is logged and swallowed
    rather than propagated into the request that triggered it.

    A pure data message (no `notification` block) — this only needs to wake the app so it
    reconnects its WebSocket and re-fetches its rules; there is nothing here a human should
    see as a system notification.
    """
    if not is_push_configured():
        logger.info("fcm_push_skipped device_id=%s reason=not_configured", device_id)
        return

    payload = {
        "message": {
            "token": fcm_token,
            "data": {"event": "rules_changed", "device_id": str(device_id)},
            "android": {"priority": "high"},
        }
    }

    try:
        access_token = _get_access_token()
        _post_fcm_message(access_token, payload)
    except (GoogleAuthError, RequestException, OSError, ValueError) as exc:
        logger.warning("fcm_push_failed device_id=%s error=%s", device_id, exc)

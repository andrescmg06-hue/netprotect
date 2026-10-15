from dataclasses import dataclass

from google.auth import exceptions as google_exceptions
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token

from app.core.config import settings

_google_request = google_requests.Request()


class InvalidGoogleTokenError(Exception):
    pass


@dataclass(frozen=True)
class GoogleIdentity:
    google_sub: str
    email: str
    display_name: str | None
    avatar_url: str | None


def _rejection_reason(exc: ValueError) -> str:
    """Map a google-auth error to a fixed label; never return any text from ``exc``."""
    message = str(exc)
    if message.startswith("Token expired"):
        return "token expired"
    if message.startswith("Token used too early"):
        return "token not yet valid"
    if message.startswith("Token has wrong audience"):
        return "wrong audience"
    if message.startswith("Could not verify token signature"):
        return "invalid signature"
    if isinstance(exc, google_exceptions.MalformedError):
        return "malformed token"
    return "token rejected"


def verify_google_id_token(token: str) -> GoogleIdentity:
    if not settings.google_web_client_id:
        raise InvalidGoogleTokenError("GOOGLE_WEB_CLIENT_ID is not configured")

    try:
        claims = id_token.verify_oauth2_token(
            token, _google_request, settings.google_web_client_id
        )
    except ValueError as exc:
        # google-auth embeds the token (or its payload: email, sub, name) in the message of
        # these errors, so only a fixed label may leave this module. `from None` drops the
        # original exception from the chain for the same reason.
        raise InvalidGoogleTokenError(_rejection_reason(exc)) from None

    if claims.get("iss") not in ("accounts.google.com", "https://accounts.google.com"):
        raise InvalidGoogleTokenError("unexpected token issuer")

    if not claims.get("email_verified"):
        raise InvalidGoogleTokenError("Google account email is not verified")

    email = claims.get("email")
    google_sub = claims.get("sub")
    if not email or not google_sub:
        raise InvalidGoogleTokenError("token is missing required claims")

    return GoogleIdentity(
        google_sub=google_sub,
        email=email,
        display_name=claims.get("name"),
        avatar_url=claims.get("picture"),
    )

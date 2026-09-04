import hashlib
import secrets
import uuid
from datetime import UTC, datetime, timedelta

import jwt

from app.core.config import settings

_ALGORITHM = "HS256"
_TOKEN_TYPE_ACCESS = "access"  # noqa: S105 -- a token-type label, not a secret


class InvalidAccessTokenError(Exception):
    pass


def create_access_token(user_id: uuid.UUID) -> tuple[str, datetime]:
    now = datetime.now(UTC)
    expires_at = now + timedelta(minutes=settings.access_token_ttl_minutes)
    payload = {
        "sub": str(user_id),
        "type": _TOKEN_TYPE_ACCESS,
        "jti": uuid.uuid4().hex,
        "iat": now,
        "exp": expires_at,
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm=_ALGORITHM)
    return token, expires_at


def decode_access_token(token: str) -> uuid.UUID:
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[_ALGORITHM])
    except jwt.PyJWTError as exc:
        raise InvalidAccessTokenError("invalid or expired access token") from exc

    if payload.get("type") != _TOKEN_TYPE_ACCESS:
        raise InvalidAccessTokenError("wrong token type")

    try:
        return uuid.UUID(payload["sub"])
    except (KeyError, ValueError) as exc:
        raise InvalidAccessTokenError("malformed subject claim") from exc


def generate_refresh_token() -> str:
    return secrets.token_urlsafe(48)


def hash_refresh_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def refresh_token_expiry() -> datetime:
    return datetime.now(UTC) + timedelta(days=settings.refresh_token_ttl_days)

import hashlib
import hmac
import secrets
import uuid
from datetime import UTC, datetime, timedelta

import jwt

from app.core.config import settings

PAIRING_CODE_LENGTH = 6
_PAIRING_CODE_UPPER_BOUND = 10**PAIRING_CODE_LENGTH

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


def generate_pairing_code() -> str:
    """A 6-digit code from the CSPRNG.

    `secrets.randbelow` is uniform over [0, 10^6), so no digit combination is more likely
    than another — unlike `random` (not cryptographically secure) or `% 1000000` on a wider
    random integer (modulo bias).
    """
    return f"{secrets.randbelow(_PAIRING_CODE_UPPER_BOUND):0{PAIRING_CODE_LENGTH}d}"


def hash_pairing_code(code: str) -> str:
    """HMAC-SHA256 of the code under a server-side key.

    Not a bare hash: with only 10^6 possible codes, an attacker who can read
    `pairing_codes` could brute-force a plain (or per-row-salted) hash back to the digits
    almost instantly, and a per-row salt doesn't help because the salt lives in the same
    row. The key lives in application config instead, so recovering a code requires
    compromising both the database and the application's secrets.
    """
    return hmac.new(
        settings.pairing_code_pepper.encode("utf-8"),
        code.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()


def pairing_code_expiry() -> datetime:
    return datetime.now(UTC) + timedelta(seconds=settings.pairing_code_ttl_seconds)

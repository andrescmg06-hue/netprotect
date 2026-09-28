"""Short-lived TURN credentials (Sprint 28, docs/planning/plan-turn.md D2/D3).

coturn runs with `use-auth-secret`, the "TURN REST API" scheme: the username is
`<expiry unix time>:<anything>` and the password is base64(HMAC-SHA1(shared secret, username)).
coturn recomputes the HMAC itself and rejects an expired timestamp, so there is no credential
store, nothing to revoke, and no call from coturn back to this backend.

The part after the colon is a random nonce, never the user's id: it ends up in coturn's logs, and
a fresh value per issuance also makes coturn's per-user allocation quota a per-credential one.
"""

import base64
import hashlib
import hmac
import secrets
from dataclasses import dataclass
from datetime import datetime

from app.core.config import settings


@dataclass(frozen=True)
class TurnCredential:
    urls: list[str]
    username: str
    credential: str


def sign_turn_username(secret: str, username: str) -> str:
    # SHA-1 because it is what coturn verifies by default for this scheme; as an HMAC key
    # derivation it is not exposed to SHA-1's collision weaknesses.
    digest = hmac.new(secret.encode(), username.encode(), hashlib.sha1).digest()
    return base64.b64encode(digest).decode()


def issue_turn_credential(now: datetime) -> TurnCredential | None:
    """None when no TURN URL is configured, so the caller serves STUN only instead of failing."""
    urls = settings.turn_urls_list
    if not urls:
        return None
    expires_at = int(now.timestamp()) + settings.turn_credential_ttl_seconds
    username = f"{expires_at}:{secrets.token_urlsafe(12)}"
    return TurnCredential(
        urls=urls,
        username=username,
        credential=sign_turn_username(settings.turn_shared_secret, username),
    )

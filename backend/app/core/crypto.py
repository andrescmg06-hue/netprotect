from functools import lru_cache

from cryptography.fernet import Fernet, InvalidToken

from app.core.config import settings


class LocationDecryptionError(Exception):
    """A stored ciphertext could not be decrypted with the current key — a corrupted row or a
    key rotation without re-encrypting old data. Callers must not swallow this silently, since
    that would hide a real data-integrity problem from the tutor as if the device simply had no
    location yet.
    """


@lru_cache
def _fernet() -> Fernet:
    """Built lazily (and cached) rather than at import time: Fernet validates its key's exact
    format (32 url-safe base64-encoded bytes) eagerly, so a placeholder value in
    .env.production.example that nobody has replaced yet must not crash the whole application at
    startup — only the location endpoints, which nothing else on the backend depends on, fail
    when actually used with a bad key.
    """
    return Fernet(settings.location_encryption_key.encode("utf-8"))


def encrypt_coordinate(value: float) -> str:
    """Encrypts one latitude/longitude value for storage.

    Column-level, not disk/volume-level: PostgreSQL's own storage encryption (if enabled) would
    protect against someone stealing the physical disk, but not against a plain SQL dump, a
    misconfigured backup, or a read-only credential leak — all of which would still show the
    coordinates in the clear without this. Each value is encrypted independently (not the pair
    together) so a query never needs to decrypt one to filter on it, though nothing here
    currently does that.
    """
    return _fernet().encrypt(repr(value).encode("utf-8")).decode("utf-8")


def decrypt_coordinate(ciphertext: str) -> float:
    try:
        return float(_fernet().decrypt(ciphertext.encode("utf-8")).decode("utf-8"))
    except InvalidToken as exc:
        raise LocationDecryptionError(
            "stored location ciphertext could not be decrypted with the current key"
        ) from exc

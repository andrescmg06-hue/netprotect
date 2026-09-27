"""Sprint 28: the TURN credential format coturn's `use-auth-secret` expects.

Whether coturn actually accepts these credentials is proven separately, against a real coturn, by
the integration step in docs/sprint-28-evidence.md — this file only pins the contract.
"""

import base64
import hashlib
import hmac
from datetime import UTC, datetime
from unittest.mock import patch

from app.core.config import settings
from app.services.turn import issue_turn_credential, sign_turn_username

_NOW = datetime(2026, 9, 27, 12, 0, 0, tzinfo=UTC)


def _configured(**overrides):
    values = {
        "turn_urls": "turn:relay.example:3478?transport=udp, turn:relay.example:3478?transport=tcp",
        "turn_shared_secret": "unit-test-turn-secret",
        "turn_credential_ttl_seconds": 3600,
    }
    values.update(overrides)
    return patch.multiple(settings, **values)


def test_no_turn_urls_means_no_credential() -> None:
    with _configured(turn_urls=""):
        assert issue_turn_credential(_NOW) is None


def test_username_is_expiry_colon_nonce_and_expires_after_the_ttl() -> None:
    with _configured():
        issued = issue_turn_credential(_NOW)

    assert issued is not None
    expiry, nonce = issued.username.split(":", 1)
    assert int(expiry) == int(_NOW.timestamp()) + 3600
    assert nonce


def test_credential_is_base64_hmac_sha1_of_the_username_with_the_shared_secret() -> None:
    with _configured():
        issued = issue_turn_credential(_NOW)

    assert issued is not None
    expected = hmac.new(b"unit-test-turn-secret", issued.username.encode(), hashlib.sha1).digest()
    assert base64.b64decode(issued.credential) == expected


def test_a_different_secret_gives_a_different_credential() -> None:
    assert sign_turn_username("secret-a", "1790000000:x") != sign_turn_username(
        "secret-b", "1790000000:x"
    )


def test_every_issuance_gets_its_own_nonce() -> None:
    # A fresh username per request is what makes coturn's per-user quota a per-credential one.
    with _configured():
        first = issue_turn_credential(_NOW)
        second = issue_turn_credential(_NOW)

    assert first is not None and second is not None
    assert first.username != second.username


def test_urls_are_split_and_trimmed() -> None:
    with _configured():
        issued = issue_turn_credential(_NOW)

    assert issued is not None
    assert issued.urls == [
        "turn:relay.example:3478?transport=udp",
        "turn:relay.example:3478?transport=tcp",
    ]

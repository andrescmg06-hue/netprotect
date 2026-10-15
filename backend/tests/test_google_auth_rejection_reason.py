"""The rejection reason of a Google ID token must be a fixed label, never library text.

google-auth embeds the offending token (or its payload: email, sub, name) in the message of
the errors it raises for malformed tokens. That text would end up in logs and is attacker
controlled, so ``verify_google_id_token`` must only surface fixed labels.
"""

import pytest
from google.auth import exceptions as google_exceptions

from app.core.config import settings
from app.services import google_auth
from app.services.google_auth import InvalidGoogleTokenError, verify_google_id_token

SECRET = "SECRET"  # noqa: S105 -- sentinel planted in fake token text, not a real secret


@pytest.fixture(autouse=True)
def _configured_client_id(monkeypatch):
    monkeypatch.setattr(settings, "google_web_client_id", "client-id")


def _verify_raising(monkeypatch, error: Exception):
    def fake_verify(*_args, **_kwargs):
        raise error

    monkeypatch.setattr(google_auth.id_token, "verify_oauth2_token", fake_verify)


@pytest.mark.parametrize(
    ("error", "expected"),
    [
        (google_exceptions.InvalidValue("Token expired, 100 < 200"), "token expired"),
        (
            google_exceptions.InvalidValue(
                "Token used too early, 1 < 2. Check that your computer's clock is set correctly."
            ),
            "token not yet valid",
        ),
        (
            google_exceptions.InvalidValue(
                "Token has wrong audience SECRET-AUD, expected one of ['client-id']"
            ),
            "wrong audience",
        ),
        (
            google_exceptions.MalformedError("Could not verify token signature."),
            "invalid signature",
        ),
        (
            google_exceptions.MalformedError(
                "Wrong number of segments in token: SECRET.TOKEN.PAYLOAD"
            ),
            "malformed token",
        ),
        (
            google_exceptions.MalformedError(
                "Payload segment should be a JSON object: SECRET-EMAIL@example.com"
            ),
            "malformed token",
        ),
        (ValueError("anything with SECRET-TEXT"), "token rejected"),
    ],
)
def test_rejection_reason_is_a_fixed_label_without_token_text(monkeypatch, error, expected):
    _verify_raising(monkeypatch, error)

    with pytest.raises(InvalidGoogleTokenError) as caught:
        verify_google_id_token("any.token.value")

    assert str(caught.value) == expected
    assert SECRET not in str(caught.value)
    assert SECRET not in repr(caught.value)
    assert caught.value.__cause__ is None


@pytest.mark.parametrize(
    ("claims", "expected"),
    [
        ({"iss": "evil.example.com"}, "unexpected token issuer"),
        (
            {"iss": "accounts.google.com", "email_verified": False},
            "Google account email is not verified",
        ),
        (
            {"iss": "accounts.google.com", "email_verified": True, "email": "a@example.com"},
            "token is missing required claims",
        ),
    ],
)
def test_existing_fixed_messages_are_unchanged(monkeypatch, claims, expected):
    monkeypatch.setattr(
        google_auth.id_token, "verify_oauth2_token", lambda *_args, **_kwargs: claims
    )

    with pytest.raises(InvalidGoogleTokenError) as caught:
        verify_google_id_token("any.token.value")

    assert str(caught.value) == expected

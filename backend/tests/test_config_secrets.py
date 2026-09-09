"""Sprint 21: production must refuse to boot on the development default secrets.

No infrastructure needed — `Settings` is constructed directly with explicit kwargs, which
override both the `.env` file and the process environment, so nothing here touches the real
configuration or the module-level `settings` singleton.
"""

import pytest
from pydantic import ValidationError

from app.core.config import Settings

_REAL_ENOUGH = {
    "database_url": "postgresql+asyncpg://netprotect:s3cr3t@db:5432/netprotect",
    "redis_url": "redis://:an0ther@redis:6379/0",
    "jwt_secret": "a-real-production-jwt-secret-of-sufficient-length-xyz",
    "pairing_code_pepper": "a-real-production-pairing-pepper-of-sufficient-len",
    "location_encryption_key": "KkxP1iS9ZDOzTx6NGgiAftFSQrsczN4j7bHr8_yd7VI=",
}


def test_production_with_real_secrets_is_accepted() -> None:
    settings = Settings(app_env="production", **_REAL_ENOUGH)

    assert settings.app_env == "production"


@pytest.mark.parametrize("field", sorted(_REAL_ENOUGH))
def test_production_refuses_any_single_development_default(field: str) -> None:
    # Passed explicitly rather than omitted: the test container sets DATABASE_URL and friends in
    # the environment, which would otherwise fill the gap and hide what this test is checking.
    overrides = dict(_REAL_ENOUGH)
    overrides[field] = Settings.model_fields[field].default

    with pytest.raises(ValidationError) as raised:
        Settings(app_env="production", **overrides)

    assert field.upper() in str(raised.value)


def test_development_still_boots_on_the_development_defaults() -> None:
    """The whole point of the defaults: `docker compose up` works with no secrets ceremony."""
    settings = Settings(app_env="development")

    assert "change_me" in settings.jwt_secret

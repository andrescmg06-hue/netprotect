"""Sprint 21: production must refuse to boot on the development default secrets.

Sprint 71 widened the same check: production must also refuse an empty required value, a
placeholder (`REPLACE_WITH_...` or a reserved example domain) in any text setting, and a loopback
or wildcard entry in CORS_ORIGINS/ALLOWED_HOSTS — each error naming the variable to fix.

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
    "turn_shared_secret": "a-real-production-turn-shared-secret-long-enough",
}

# Everything else production needs, passed explicitly for the same reason as _REAL_ENOUGH: the
# test container sets CORS_ORIGINS/ALLOWED_HOSTS to localhost values in the environment.
_PRODUCTION = {
    **_REAL_ENOUGH,
    "google_web_client_id": "123456789012-abcdefg.apps.googleusercontent.com",
    "cors_origins": "https://panel.netprotect-prod.net",
    "allowed_hosts": "api.netprotect-prod.net",
    "turn_urls": "turn:turn.netprotect-prod.net:3478?transport=udp",
    "fcm_project_id": "",
    "fcm_service_account_file": "",
}


def _refused_message(**overrides: str) -> str:
    with pytest.raises(ValidationError) as raised:
        Settings(app_env="production", **{**_PRODUCTION, **overrides})
    return str(raised.value)


def test_production_with_real_secrets_is_accepted() -> None:
    settings = Settings(app_env="production", **_PRODUCTION)

    assert settings.app_env == "production"


@pytest.mark.parametrize("field", sorted(_REAL_ENOUGH))
def test_production_refuses_any_single_development_default(field: str) -> None:
    # Passed explicitly rather than omitted: the test container sets DATABASE_URL and friends in
    # the environment, which would otherwise fill the gap and hide what this test is checking.
    message = _refused_message(**{field: Settings.model_fields[field].default})

    assert field.upper() in message


def test_development_still_boots_on_the_development_defaults() -> None:
    """The whole point of the defaults: `docker compose up` works with no secrets ceremony."""
    settings = Settings(app_env="development")

    assert "change_me" in settings.jwt_secret


def test_non_production_environments_skip_every_production_check() -> None:
    """Integration tests run with APP_ENV=test and localhost CORS/hosts; they must keep booting."""
    settings = Settings(
        app_env="test",
        google_web_client_id="",
        cors_origins="http://localhost:3000",
        allowed_hosts="localhost,127.0.0.1,testserver",
        turn_urls="",
    )

    assert settings.app_env == "test"


@pytest.mark.parametrize(
    "field", ["google_web_client_id", "cors_origins", "allowed_hosts", "turn_urls"]
)
@pytest.mark.parametrize("blank", ["", "   ", " , "])
def test_production_refuses_a_blank_required_setting(field: str, blank: str) -> None:
    message = _refused_message(**{field: blank})

    assert field.upper() in message


@pytest.mark.parametrize(
    "field",
    [
        "google_web_client_id",
        "jwt_secret",
        "pairing_code_pepper",
        "turn_shared_secret",
        "turn_urls",
        "fcm_project_id",
        "database_url",
    ],
)
def test_production_refuses_a_replace_with_placeholder(field: str) -> None:
    message = _refused_message(**{field: "REPLACE_WITH_A_REAL_VALUE"})

    assert field.upper() in message


def test_placeholder_marker_is_case_insensitive() -> None:
    message = _refused_message(google_web_client_id="replace_with_client_id")

    assert "GOOGLE_WEB_CLIENT_ID" in message


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("cors_origins", "https://panel.example.com"),
        ("cors_origins", "https://panel.netprotect-prod.net,https://web.example.org"),
        ("allowed_hosts", "api.example.net"),
        ("allowed_hosts", "api.netprotect.example"),
        ("turn_urls", "turn:turn.netprotect.invalid:3478?transport=udp"),
        ("database_url", "postgresql+asyncpg://u:p@db.example.com:5432/netprotect"),
        ("google_web_client_id", "client-id.example.com"),
    ],
)
def test_production_refuses_a_reserved_example_domain(field: str, value: str) -> None:
    message = _refused_message(**{field: value})

    assert field.upper() in message


def test_example_lookalike_domains_are_not_mistaken_for_placeholders() -> None:
    """Only the reserved names (RFC 2606) are placeholders, not any host containing 'example'."""
    settings = Settings(
        app_env="production",
        **{
            **_PRODUCTION,
            "cors_origins": "https://panel.myexample.com",
            "allowed_hosts": "api.examples.net",
        },
    )

    assert settings.cors_origins_list == ["https://panel.myexample.com"]


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("cors_origins", "http://localhost:3000"),
        ("cors_origins", "https://panel.netprotect-prod.net,http://127.0.0.1:3000"),
        ("cors_origins", "http://[::1]:3000"),
        ("cors_origins", "*"),
        ("allowed_hosts", "localhost"),
        ("allowed_hosts", "api.netprotect-prod.net,127.0.0.1"),
        ("allowed_hosts", "*"),
        ("allowed_hosts", "*.netprotect-prod.net"),
    ],
)
def test_production_refuses_loopback_or_wildcard_cors_and_hosts(field: str, value: str) -> None:
    message = _refused_message(**{field: value})

    assert field.upper() in message


def test_local_production_rehearsal_on_dot_localhost_subdomains_still_boots() -> None:
    """docs/sprint-26-evidence.md rehearses compose.prod.yaml (APP_ENV=production) with
    WEB_DOMAIN=web.localhost/API_DOMAIN=api.localhost and Caddy's internal CA. Those names can
    never get a public certificate, so they cannot be shipped by mistake; only bare `localhost`
    and loopback IPs are refused."""
    settings = Settings(
        app_env="production",
        **{
            **_PRODUCTION,
            "cors_origins": "https://web.localhost",
            "allowed_hosts": "api.localhost",
        },
    )

    assert settings.allowed_hosts_list == ["api.localhost"]


def test_production_with_fcm_fully_configured_is_accepted(tmp_path) -> None:
    key_file = tmp_path / "fcm_service_account.json"
    key_file.write_text('{"type": "service_account"}', encoding="utf-8")

    settings = Settings(
        app_env="production",
        **{
            **_PRODUCTION,
            "fcm_project_id": "netprotect-prod-1a2b3",
            "fcm_service_account_file": str(key_file),
        },
    )

    assert settings.fcm_project_id == "netprotect-prod-1a2b3"


@pytest.mark.parametrize("key_file", ["missing.json", "empty.json", "/dev/null"])
def test_production_refuses_an_fcm_key_file_that_is_missing_or_empty(
    tmp_path, key_file: str
) -> None:
    """compose.prod.yaml mounts /dev/null when no key file is given; enabling FCM that way, or
    with a path that isn't there, would only surface as failed pushes in the logs."""
    (tmp_path / "empty.json").write_text("", encoding="utf-8")
    path = key_file if key_file.startswith("/") else str(tmp_path / key_file)

    message = _refused_message(
        fcm_project_id="netprotect-prod-1a2b3", fcm_service_account_file=path
    )

    assert "FCM_SERVICE_ACCOUNT_FILE" in message


@pytest.mark.parametrize(
    ("set_field", "missing_field"),
    [
        ("fcm_project_id", "fcm_service_account_file"),
        ("fcm_service_account_file", "fcm_project_id"),
    ],
)
def test_production_refuses_half_configured_fcm(set_field: str, missing_field: str) -> None:
    """Push only runs with both values (app/services/push.py); one alone would silently disable
    it while the operator believes it is on."""
    values = {
        "fcm_project_id": "netprotect-prod-1a2b3",
        "fcm_service_account_file": "/run/secrets/fcm_service_account.json",
    }
    message = _refused_message(**{set_field: values[set_field], missing_field: ""})

    assert missing_field.upper() in message


def test_every_problem_is_reported_at_once() -> None:
    """An operator fixing a fresh host should see the whole list, not one variable per restart."""
    message = _refused_message(
        google_web_client_id="",
        cors_origins="http://localhost:3000",
        jwt_secret=Settings.model_fields["jwt_secret"].default,
    )

    for env_var in ("GOOGLE_WEB_CLIENT_ID", "CORS_ORIGINS", "JWT_SECRET"):
        assert env_var in message


def test_a_refused_production_config_does_not_echo_the_loaded_secrets() -> None:
    # The error ends up in the startup log; it must name what to fix, never print what was loaded.
    # Pydantic truncates the input it echoes to its first and last characters, so checking for
    # whole secrets would pass while a fragment of one still leaks: no input may be echoed at all.
    message = _refused_message(google_web_client_id="")

    assert "GOOGLE_WEB_CLIENT_ID" in message
    assert "input_value" not in message

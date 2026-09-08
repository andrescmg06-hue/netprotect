from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "NetProtect API"
    app_env: Literal["development", "test", "production"] = "development"
    app_version: str = "0.1.0-sprint1"
    api_v1_prefix: str = "/api/v1"

    database_url: str = (
        "postgresql+asyncpg://netprotect:change_me_dev_only@localhost:5432/netprotect"
    )
    redis_url: str = "redis://:change_me_redis_dev_only@localhost:6379/0"
    cors_origins: str = "http://localhost:3000"
    allowed_hosts: str = "localhost,127.0.0.1,testserver"
    log_level: str = "INFO"

    google_web_client_id: str = ""
    # Dev-only placeholder, kept >=32 bytes to avoid PyJWT's HS256 key-length warning.
    jwt_secret: str = "change_me_dev_only_jwt_secret_at_least_32_bytes_long"  # noqa: S105
    access_token_ttl_minutes: int = 15
    refresh_token_ttl_days: int = 30

    # Deliberately a different secret from jwt_secret: leaking one must not leak the other.
    # This one is the HMAC key ("pepper") for pairing codes — a 6-digit code has only 10^6
    # possible values, so a plain or per-row-salted hash is brute-forceable in microseconds
    # by anyone who can read the table. Keeping the key outside the database means a
    # database-only compromise cannot recover any code.
    pairing_code_pepper: str = "change_me_dev_only_pairing_pepper_at_least_32_bytes"  # noqa: S105
    pairing_code_ttl_seconds: int = 180

    pairing_generate_max_per_tutor: int = 10
    pairing_generate_window_seconds: int = 3600
    pairing_redeem_max_per_user: int = 10
    pairing_redeem_max_per_ip: int = 30
    pairing_redeem_window_seconds: int = 900

    # A device that hasn't sent a heartbeat within this window is reported OFFLINE even
    # though its stored status still says ONLINE — computed at read time, not written by a
    # background sweep, since no scheduler exists yet in this project.
    device_offline_threshold_seconds: int = 300

    # Fernet symmetric key for encrypting latitude/longitude at rest (Sprint 13). Deliberately
    # NOT generated with secrets.token_urlsafe(48) like the project's other secrets: Fernet
    # requires an exact format (32 raw bytes, url-safe base64-encoded), so this dev-only default
    # is a real, validly-formatted key (generated the equivalent way Fernet.generate_key() would)
    # rather than an arbitrary string — an invalid format would crash the first encrypt/decrypt
    # call, not just look like a placeholder. Its own name, never shared with jwt_secret or
    # pairing_code_pepper — leaking one secret must not leak location data too.
    location_encryption_key: str = "_is8T-L3jDCCJY6nqQvQk6Dlt1v9-pUa5IGiZMsLbVE="  # noqa: S105

    # How long a device's location history is kept before the write endpoint purges it (Sprint
    # 13) — see docs/sprint-13.md for why 7 days: this is the most sensitive data category the
    # project stores (a minor's whereabouts), captured automatically every ~15 minutes, so a
    # short window bounds both the privacy exposure and the row count without needing a
    # scheduled job (the purge runs inline on each write, same "no background scheduler yet"
    # reasoning as device_offline_threshold_seconds above).
    location_retention_days: int = 7

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @property
    def cors_origins_list(self) -> list[str]:
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]

    @property
    def allowed_hosts_list(self) -> list[str]:
        return [item.strip() for item in self.allowed_hosts.split(",") if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()

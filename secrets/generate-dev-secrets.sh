#!/bin/sh
# Sprint 26: fills secrets/*.txt with fresh, random, single-use values so compose.prod.yaml can be
# verified locally without a real secret manager. See secrets/README.md — this script is NOT how
# real production secrets get generated; it exists only for the local verification in
# docs/sprint-26-evidence.md.
set -eu

cd "$(dirname "$0")"

random() {
    python3 -c "import secrets; print(secrets.token_urlsafe(${1:-32}))"
}

fernet_key() {
    # A Fernet key is just 32 raw random bytes, url-safe base64-encoded (see
    # app/core/config.py's location_encryption_key comment) — stdlib `secrets`+`base64` produce a
    # validly-formatted one without needing the `cryptography` package importable on whatever
    # `python3` happens to be on PATH here, which this being a *host-side* dev script (unlike the
    # backend container, which always has it) can't assume.
    python3 -c "import base64, secrets; print(base64.urlsafe_b64encode(secrets.token_bytes(32)).decode())"
}

: "${POSTGRES_USER:=netprotect_app}"
: "${POSTGRES_DB:=netprotect}"

postgres_password=$(random 32)
redis_password=$(random 32)

printf '%s' "$postgres_password" > postgres_password.txt
printf '%s' "$redis_password" > redis_password.txt
printf '%s' "postgresql+asyncpg://${POSTGRES_USER}:${postgres_password}@db:5432/${POSTGRES_DB}" > database_url.txt
printf '%s' "redis://:${redis_password}@redis:6379/0" > redis_url.txt
printf '%s' "$(random 48)" > jwt_secret.txt
printf '%s' "$(random 48)" > pairing_code_pepper.txt
printf '%s' "$(fernet_key)" > location_encryption_key.txt
printf '%s' "$(random 24)" > grafana_admin_password.txt

chmod 600 ./*.txt
echo "secrets/*.txt generados (POSTGRES_USER=${POSTGRES_USER}, POSTGRES_DB=${POSTGRES_DB})"

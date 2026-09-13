#!/bin/sh
# Sprint 26: the same "_FILE" convention the official postgres/mysql images already use for
# secrets — for every FOO_FILE env var pointing at a mounted file, export FOO with that file's
# content, then exec the real command. Lets compose.prod.yaml hand the backend/migrate containers
# Docker Compose `secrets:` (files under /run/secrets, never a `.env` baked into the image or an
# env var visible in `docker inspect`) instead of plaintext env vars for JWT_SECRET,
# PAIRING_CODE_PEPPER, LOCATION_ENCRYPTION_KEY, DATABASE_URL and REDIS_URL, without app/core/
# config.py needing to know anything about where a secret actually came from — it only ever reads
# a plain env var. A plain FOO env var set directly still wins if both are present (pydantic-
# settings style precedence: read here, not in config.py, so the rule lives in exactly one place).
set -eu

# FCM_SERVICE_ACCOUNT_FILE predates this convention (Sprint 18) and means something different on
# purpose: app/services/push.py opens that path itself as a service-account JSON key file, it is
# never meant to be inlined into an env var. Found by /code-review before this sprint closed: the
# blanket loop below would otherwise `cat` a real Firebase private key into a new
# FCM_SERVICE_ACCOUNT env var that nothing reads — leaking a credential into `docker exec ... env`
# and the process's /proc/<pid>/environ, defeating this entire mechanism's own purpose for that
# one variable. Any future FOO_FILE that is genuinely a path, not a secret value to inline, must
# be added here too.
_PATH_TYPED_FILE_VARS=" FCM_SERVICE_ACCOUNT_FILE "

for file_var in $(env | grep -E '^[A-Za-z_][A-Za-z0-9_]*_FILE=' | cut -d= -f1); do
    case "$_PATH_TYPED_FILE_VARS" in
        *" $file_var "*) continue ;;
    esac
    var="${file_var%_FILE}"
    current_value=$(eval "printf '%s' \"\${${var}:-}\"")
    if [ -n "$current_value" ]; then
        continue
    fi
    file_path=$(eval "printf '%s' \"\$${file_var}\"")
    if [ -f "$file_path" ]; then
        value=$(cat "$file_path")
        export "$var=$value"
    fi
done

exec "$@"

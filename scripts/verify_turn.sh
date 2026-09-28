#!/bin/sh
# Sprint 28: proves that a TURN credential issued by the backend's own code
# (app/services/turn.py, unmocked) is one the real, hardened coturn accepts, and that an expired
# or tampered one is refused. pytest can only check the credential's shape; only coturn can say
# whether it would really let a client relay. Run from the repo root, after
# `docker compose -f compose.test.yaml build` (CI job `integration` does exactly that).
set -eu

# Git Bash on Windows rewrites container paths like /etc/... into Windows paths unless told not
# to, which once made a coturn run silently start without its config (docs/sprint-28-evidence.md).
export MSYS_NO_PATHCONV=1

compose="docker compose -f compose.test.yaml"
relay_ip=172.30.98.10
network=netprotect-test_turn_test

$compose up -d coturn >/dev/null
trap '$compose rm -sf coturn >/dev/null 2>&1 || true' EXIT
sleep 3

# One Python call, three credentials, one per line: "<label> <username> <password>". Built with
# the backend's own functions and settings (TURN_SHARED_SECRET from compose.test.yaml).
credentials=$($compose run --rm --no-deps -T backend python -c '
import time
from datetime import UTC, datetime
from app.core.config import settings
from app.services.turn import issue_turn_credential, sign_turn_username

valid = issue_turn_credential(datetime.now(UTC))
print("valid", valid.username, valid.credential)

expired_user = f"{int(time.time()) - 60}:expired"
print("expired", expired_user, sign_turn_username(settings.turn_shared_secret, expired_user))

# Change a character in the middle, not the last one: the last base64 character can carry only
# padding bits, so altering it may leave the decoded HMAC unchanged.
middle = len(valid.credential) // 2
flipped = "A" if valid.credential[middle] != "A" else "B"
print("tampered", valid.username, valid.credential[:middle] + flipped + valid.credential[middle + 1:])
')

relay() {
    docker run --rm --network "$network" --entrypoint turnutils_uclient coturn/coturn:4.7.0 \
        -y -u "$1" -w "$2" -n 5 -m 1 -l 100 "$relay_ip" 2>&1
}

failures=0
echo "$credentials" | while read -r label user password; do
    output=$(relay "$user" "$password")
    if echo "$output" | grep -q "tot_recv_msgs=20"; then
        result="relayed"
    elif echo "$output" | grep -q "Cannot complete Allocation"; then
        result="rejected"
    else
        result="unknown"
    fi

    case "$label:$result" in
        valid:relayed | expired:rejected | tampered:rejected)
            echo "OK   $label credential -> $result" ;;
        *)
            echo "FAIL $label credential -> $result"
            echo "$output" | tail -5
            exit 1 ;;
    esac
done || failures=1

if [ "$failures" -ne 0 ]; then
    echo "TURN credential verification failed"
    exit 1
fi
echo "TURN credential verification passed"

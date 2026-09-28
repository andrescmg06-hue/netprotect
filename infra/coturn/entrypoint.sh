#!/bin/sh
# Sprint 28: assembles coturn's runtime config from the versioned turnserver.conf plus the values
# that differ per environment, then execs turnserver.
#
# Why a generated file instead of `--static-auth-secret=...` on the command line: coturn has no
# `_FILE` convention of its own, and an argv value is readable by anyone who can list processes.
# Writing it into a 0600 file under /tmp keeps the secret out of both argv and `docker inspect`
# in production, where it arrives as a Compose secret (TURN_SHARED_SECRET_FILE), the same
# convention backend/docker-entrypoint.sh follows since Sprint 26.
set -eu

secret="${TURN_SHARED_SECRET:-}"
if [ -z "$secret" ] && [ -n "${TURN_SHARED_SECRET_FILE:-}" ] && [ -f "$TURN_SHARED_SECRET_FILE" ]; then
    secret=$(cat "$TURN_SHARED_SECRET_FILE")
fi
if [ -z "$secret" ]; then
    echo "coturn: TURN_SHARED_SECRET (or TURN_SHARED_SECRET_FILE) is required" >&2
    exit 1
fi

: "${TURN_REALM:?coturn: TURN_REALM is required}"
# The relay's own address on its Compose network. Pinned (ipv4_address) because allowed-peer-ip
# has to name it exactly: see Spike R1 in docs/sprint-28-evidence.md.
: "${TURN_RELAY_IP:?coturn: TURN_RELAY_IP is required}"

conf=/tmp/turnserver.conf
umask 077
{
    cat /etc/coturn/netprotect/turnserver.conf
    echo "realm=$TURN_REALM"
    echo "relay-ip=$TURN_RELAY_IP"
    echo "allowed-peer-ip=$TURN_RELAY_IP"
    if [ -n "${TURN_EXTERNAL_IP:-}" ]; then
        # Production only: the public address clients are told to send to, mapped onto the
        # container's own relay address.
        echo "external-ip=$TURN_EXTERNAL_IP/$TURN_RELAY_IP"
    fi
    echo "static-auth-secret=$secret"
} > "$conf"

exec turnserver -c "$conf"

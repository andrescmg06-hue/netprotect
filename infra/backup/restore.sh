#!/bin/sh
# Sprint 26: restores one dump produced by backup.sh. Deliberately a separate, explicit,
# human-triggered script rather than something that ever runs automatically — restoring is
# destructive (it drops and recreates every object already in the target database) and this
# project's rule for anything destructive is a person decides when, never a container on a timer.
#
# Usage: docker compose -f compose.prod.yaml run --rm backup /restore.sh /backups/netprotect-<ts>.dump
set -eu

: "${PGHOST:=db}"
: "${PGPORT:=5432}"

if [ -f "${PGPASSWORD_FILE:-}" ]; then
	PGPASSWORD=$(cat "$PGPASSWORD_FILE")
	export PGPASSWORD
fi

dump_file="${1:?usage: restore.sh <path-to-dump-file>}"

if [ ! -f "$dump_file" ]; then
	echo "[restore] $dump_file not found" >&2
	exit 1
fi

echo "[restore] restoring $dump_file into ${PGDATABASE}@${PGHOST}:${PGPORT} (this drops existing objects first)"
pg_restore -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" --clean --if-exists --no-owner "$dump_file"
echo "[restore] done"

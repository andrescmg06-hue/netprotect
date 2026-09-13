#!/bin/sh
# Sprint 26: pg_dump'd once at startup and then again every BACKUP_INTERVAL_SECONDS (compose.prod
# runs this as a long-lived container's command, not cron — no extra package to install on top of
# the postgres:18.6-alpine image, which already ships pg_dump matching the server's own version).
# Custom format (-Fc) rather than plain SQL: it's compressed on its own and lets restore.sh use
# pg_restore's --jobs for a parallel restore, and --clean/--if-exists to drop existing objects
# first, so a restore onto a non-empty database (this project's actual failure mode: recovering
# onto a freshly-provisioned instance that already ran migrations) doesn't collide with them.
set -eu

: "${PGHOST:=db}"
: "${PGPORT:=5432}"
: "${BACKUP_DIR:=/backups}"
: "${BACKUP_RETENTION_DAYS:=7}"
: "${BACKUP_INTERVAL_SECONDS:=86400}"

if [ -f "${PGPASSWORD_FILE:-}" ]; then
	PGPASSWORD=$(cat "$PGPASSWORD_FILE")
	export PGPASSWORD
fi

mkdir -p "$BACKUP_DIR"

run_backup() {
	timestamp=$(date -u +%Y%m%dT%H%M%SZ)
	target="$BACKUP_DIR/netprotect-${timestamp}.dump"
	echo "[backup] starting pg_dump -> $target"
	pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -Fc -f "$target.tmp"
	mv "$target.tmp" "$target"
	echo "[backup] wrote $target ($(du -h "$target" | cut -f1))"

	find "$BACKUP_DIR" -name 'netprotect-*.dump' -mtime "+${BACKUP_RETENTION_DAYS}" -print -delete \
		| sed 's/^/[backup] pruned /'
}

# ONESHOT=1 runs a single backup and exits — used by the restore round-trip test in
# docs/sprint-26-evidence.md and by anyone triggering an out-of-band backup manually
# (`docker compose -f compose.prod.yaml run --rm -e ONESHOT=1 backup`).
if [ "${ONESHOT:-0}" = "1" ]; then
	run_backup
	exit 0
fi

while true; do
	run_backup
	sleep "$BACKUP_INTERVAL_SECONDS"
done

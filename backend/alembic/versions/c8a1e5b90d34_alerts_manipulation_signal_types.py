"""alerts: allow the Sprint 20 manipulation-detection signal types

Revision ID: c8a1e5b90d34
Revises: d3f6a9c2b5e7
Create Date: 2026-09-08 12:00:00.000000

"""
from collections.abc import Sequence

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'c8a1e5b90d34'
down_revision: str | Sequence[str] | None = 'd3f6a9c2b5e7'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# Alembic's autogenerate does not detect a change to an existing CHECK constraint's body (same
# known limitation already hit in 709f0e4bf5a9) — written by hand.
_OLD_TYPES = "'APP_BLOCKED', 'APP_LIMIT_REACHED', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT'"
_NEW_TYPES = (
    "'APP_BLOCKED', 'APP_LIMIT_REACHED', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT', "
    "'PERMISSION_REVOKED', 'SERVICE_INACTIVE', 'HEARTBEAT_SILENCE', 'CLOCK_TAMPERING', "
    "'UNINSTALL_ATTEMPT'"
)


def upgrade() -> None:
    """Upgrade schema."""
    op.drop_constraint("ck_alerts_type_valid", "alerts", type_="check")
    op.create_check_constraint(
        "ck_alerts_type_valid", "alerts", f"alert_type IN ({_NEW_TYPES})"
    )


def downgrade() -> None:
    """Downgrade schema."""
    # Rows carrying one of the new types would violate the narrower constraint being restored;
    # they only exist because Sprint 20 ran, so dropping them is the correct inverse of this
    # migration rather than a data loss the operator didn't ask for.
    op.execute(
        "DELETE FROM alerts WHERE alert_type NOT IN "
        "('APP_BLOCKED', 'APP_LIMIT_REACHED', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT')"
    )
    op.execute(
        "DELETE FROM alert_silences WHERE dedup_key IN "
        "('PERMISSION_REVOKED', 'SERVICE_INACTIVE', 'HEARTBEAT_SILENCE', 'CLOCK_TAMPERING', "
        "'UNINSTALL_ATTEMPT')"
    )
    op.drop_constraint("ck_alerts_type_valid", "alerts", type_="check")
    op.create_check_constraint(
        "ck_alerts_type_valid", "alerts", f"alert_type IN ({_OLD_TYPES})"
    )

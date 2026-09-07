"""app rule events: allow CATEGORY as an applied rule type

Revision ID: 709f0e4bf5a9
Revises: 9600bc530f75
Create Date: 2026-09-07 03:37:12.243696

"""
from collections.abc import Sequence

from alembic import op

# revision identifiers, used by Alembic.
revision: str = '709f0e4bf5a9'
down_revision: str | Sequence[str] | None = '9600bc530f75'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# Alembic's autogenerate does not detect a change to an existing CHECK constraint's body (a
# known limitation, not specific to this project) — this migration was written by hand.
_OLD_TYPES = "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'SCHEDULE', 'DEFAULT_POLICY'"
_NEW_TYPES = "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'SCHEDULE', 'DEFAULT_POLICY', 'CATEGORY'"


def upgrade() -> None:
    """Upgrade schema."""
    op.drop_constraint(
        "ck_app_rule_events_type_valid", "app_rule_events", type_="check"
    )
    op.create_check_constraint(
        "ck_app_rule_events_type_valid",
        "app_rule_events",
        f"rule_type_applied IN ({_NEW_TYPES})",
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_constraint(
        "ck_app_rule_events_type_valid", "app_rule_events", type_="check"
    )
    op.create_check_constraint(
        "ck_app_rule_events_type_valid",
        "app_rule_events",
        f"rule_type_applied IN ({_OLD_TYPES})",
    )

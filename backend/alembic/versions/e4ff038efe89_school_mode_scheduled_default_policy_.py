"""school mode: scheduled default-policy override per device

Revision ID: e4ff038efe89
Revises: 9fa64cf938a5
Create Date: 2026-09-07 04:45:08.262821

"""
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'e4ff038efe89'
down_revision: str | Sequence[str] | None = '9fa64cf938a5'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# Autogenerate picks up the new columns fine but not the two hand-written CHECK constraints
# below — same limitation hit in every migration since Sprint 10.
_OLD_EVENT_TYPES = "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'WEEKLY_LIMIT', 'SCHEDULE', 'DEFAULT_POLICY', 'CATEGORY'"
_NEW_EVENT_TYPES = (
    "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'WEEKLY_LIMIT', 'SCHEDULE', 'DEFAULT_POLICY', "
    "'CATEGORY', 'SCHOOL_MODE'"
)


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column(
        'devices',
        sa.Column('school_mode_enabled', sa.Boolean(), server_default='false', nullable=False),
    )
    op.add_column('devices', sa.Column('school_mode_start_minute', sa.Integer(), nullable=True))
    op.add_column('devices', sa.Column('school_mode_end_minute', sa.Integer(), nullable=True))
    op.add_column('devices', sa.Column('school_mode_days_mask', sa.Integer(), nullable=True))
    op.create_check_constraint(
        'ck_devices_school_mode_requires_window',
        'devices',
        "(NOT school_mode_enabled) OR ("
        "school_mode_start_minute IS NOT NULL AND school_mode_start_minute BETWEEN 0 AND 1439 "
        "AND school_mode_end_minute IS NOT NULL AND school_mode_end_minute BETWEEN 0 AND 1439 "
        "AND school_mode_days_mask IS NOT NULL AND school_mode_days_mask BETWEEN 1 AND 127"
        ")",
    )

    op.drop_constraint('ck_app_rule_events_type_valid', 'app_rule_events', type_='check')
    op.create_check_constraint(
        'ck_app_rule_events_type_valid',
        'app_rule_events',
        f'rule_type_applied IN ({_NEW_EVENT_TYPES})',
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_constraint('ck_app_rule_events_type_valid', 'app_rule_events', type_='check')
    op.create_check_constraint(
        'ck_app_rule_events_type_valid',
        'app_rule_events',
        f'rule_type_applied IN ({_OLD_EVENT_TYPES})',
    )

    op.drop_constraint('ck_devices_school_mode_requires_window', 'devices', type_='check')
    op.drop_column('devices', 'school_mode_days_mask')
    op.drop_column('devices', 'school_mode_end_minute')
    op.drop_column('devices', 'school_mode_start_minute')
    op.drop_column('devices', 'school_mode_enabled')

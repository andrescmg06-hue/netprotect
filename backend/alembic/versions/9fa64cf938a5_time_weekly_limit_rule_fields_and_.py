"""time: weekly limit rule fields and device timezone

Revision ID: 9fa64cf938a5
Revises: 709f0e4bf5a9
Create Date: 2026-09-07 04:12:41.176128

"""
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = '9fa64cf938a5'
down_revision: str | Sequence[str] | None = '709f0e4bf5a9'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# Alembic's autogenerate detects new columns fine but not a change to an existing CHECK
# constraint's body — same limitation already hit in the Sprint 10 migration
# (709f0e4bf5a9_app_rule_events_allow_category_as_an_.py). The rule_type widening and the two
# new weekly-limit-requires-minutes constraints below were written by hand.
_OLD_RULE_TYPES = "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'SCHEDULE'"
_NEW_RULE_TYPES = "'ALLOW', 'BLOCK', 'DAILY_LIMIT', 'WEEKLY_LIMIT', 'SCHEDULE'"


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column('app_rules', sa.Column('weekly_limit_minutes', sa.Integer(), nullable=True))
    op.add_column('category_rules', sa.Column('weekly_limit_minutes', sa.Integer(), nullable=True))
    op.add_column('devices', sa.Column('timezone', sa.String(length=64), nullable=True))

    op.drop_constraint('ck_app_rules_type_valid', 'app_rules', type_='check')
    op.create_check_constraint(
        'ck_app_rules_type_valid', 'app_rules', f'rule_type IN ({_NEW_RULE_TYPES})'
    )
    op.create_check_constraint(
        'ck_app_rules_weekly_limit_requires_minutes',
        'app_rules',
        "(rule_type != 'WEEKLY_LIMIT') OR (weekly_limit_minutes IS NOT NULL AND weekly_limit_minutes > 0)",
    )

    op.drop_constraint('ck_category_rules_type_valid', 'category_rules', type_='check')
    op.create_check_constraint(
        'ck_category_rules_type_valid', 'category_rules', f'rule_type IN ({_NEW_RULE_TYPES})'
    )
    op.create_check_constraint(
        'ck_category_rules_weekly_limit_requires_minutes',
        'category_rules',
        "(rule_type != 'WEEKLY_LIMIT') OR (weekly_limit_minutes IS NOT NULL AND weekly_limit_minutes > 0)",
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_constraint(
        'ck_category_rules_weekly_limit_requires_minutes', 'category_rules', type_='check'
    )
    op.drop_constraint('ck_category_rules_type_valid', 'category_rules', type_='check')
    op.create_check_constraint(
        'ck_category_rules_type_valid', 'category_rules', f'rule_type IN ({_OLD_RULE_TYPES})'
    )

    op.drop_constraint('ck_app_rules_weekly_limit_requires_minutes', 'app_rules', type_='check')
    op.drop_constraint('ck_app_rules_type_valid', 'app_rules', type_='check')
    op.create_check_constraint(
        'ck_app_rules_type_valid', 'app_rules', f'rule_type IN ({_OLD_RULE_TYPES})'
    )

    op.drop_column('devices', 'timezone')
    op.drop_column('category_rules', 'weekly_limit_minutes')
    op.drop_column('app_rules', 'weekly_limit_minutes')

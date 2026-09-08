"""history: (device_id, occurred_at) indexes on app_rule_events and geofence_events

Revision ID: a4d8f9c1e6b2
Revises: e60012acd532
Create Date: 2026-09-08 06:00:00.000000

"""
from collections.abc import Sequence

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'a4d8f9c1e6b2'
down_revision: str | Sequence[str] | None = 'e60012acd532'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_index(
        'ix_app_rule_events_device_occurred',
        'app_rule_events',
        ['device_id', 'occurred_at'],
        unique=False,
    )
    op.create_index(
        'ix_geofence_events_device_occurred',
        'geofence_events',
        ['device_id', 'occurred_at'],
        unique=False,
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index('ix_geofence_events_device_occurred', table_name='geofence_events')
    op.drop_index('ix_app_rule_events_device_occurred', table_name='app_rule_events')

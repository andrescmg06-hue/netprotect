"""alerts and alert_silences: tutor-facing notifications generated from existing events

Revision ID: b7c3e0d4f1a8
Revises: a4d8f9c1e6b2
Create Date: 2026-09-08 06:00:00.000000

"""
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'b7c3e0d4f1a8'
down_revision: str | Sequence[str] | None = 'a4d8f9c1e6b2'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table('alerts',
    sa.Column('device_id', sa.Uuid(), nullable=False),
    sa.Column('level', sa.String(length=8), nullable=False),
    sa.Column('alert_type', sa.String(length=32), nullable=False),
    sa.Column('dedup_key', sa.String(length=300), nullable=False),
    sa.Column('package_name', sa.String(length=255), nullable=True),
    sa.Column('geofence_id', sa.Uuid(), nullable=True),
    sa.Column('geofence_name', sa.String(length=100), nullable=True),
    sa.Column('occurrence_count', sa.Integer(), nullable=False),
    sa.Column('first_occurred_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('last_occurred_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('read_at', sa.DateTime(timezone=True), nullable=True),
    sa.Column('id', sa.Uuid(), nullable=False),
    sa.CheckConstraint("level IN ('INFO', 'WARNING', 'HIGH', 'CRITICAL')", name='ck_alerts_level_valid'),
    sa.CheckConstraint(
        "alert_type IN ('APP_BLOCKED', 'APP_LIMIT_REACHED', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT')",
        name='ck_alerts_type_valid',
    ),
    sa.ForeignKeyConstraint(['device_id'], ['devices.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_alerts_device_id'), 'alerts', ['device_id'], unique=False)
    op.create_index(
        'ix_alerts_device_last_occurred', 'alerts', ['device_id', 'last_occurred_at'], unique=False
    )

    op.create_table('alert_silences',
    sa.Column('device_id', sa.Uuid(), nullable=False),
    sa.Column('dedup_key', sa.String(length=300), nullable=False),
    sa.Column('silenced_until', sa.DateTime(timezone=True), nullable=True),
    sa.Column('id', sa.Uuid(), nullable=False),
    sa.ForeignKeyConstraint(['device_id'], ['devices.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('device_id', 'dedup_key', name='uq_alert_silences_device_dedup_key')
    )
    op.create_index(
        op.f('ix_alert_silences_device_id'), 'alert_silences', ['device_id'], unique=False
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index(op.f('ix_alert_silences_device_id'), table_name='alert_silences')
    op.drop_table('alert_silences')

    op.drop_index('ix_alerts_device_last_occurred', table_name='alerts')
    op.drop_index(op.f('ix_alerts_device_id'), table_name='alerts')
    op.drop_table('alerts')

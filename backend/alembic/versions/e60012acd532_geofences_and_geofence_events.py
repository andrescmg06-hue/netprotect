"""geofences and geofence_events: home-grown zones evaluated against location reports

Revision ID: e60012acd532
Revises: ba8b839ae0eb
Create Date: 2026-09-07 06:00:00.000000

"""
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'e60012acd532'
down_revision: str | Sequence[str] | None = 'ba8b839ae0eb'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table('geofences',
    sa.Column('device_id', sa.Uuid(), nullable=False),
    sa.Column('name', sa.String(length=100), nullable=False),
    sa.Column('latitude_ciphertext', sa.Text(), nullable=False),
    sa.Column('longitude_ciphertext', sa.Text(), nullable=False),
    sa.Column('radius_meters', sa.Float(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.Column('id', sa.Uuid(), nullable=False),
    sa.CheckConstraint('radius_meters > 0', name='ck_geofences_radius_positive'),
    sa.ForeignKeyConstraint(['device_id'], ['devices.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_geofences_device_id'), 'geofences', ['device_id'], unique=False)

    op.create_table('geofence_events',
    sa.Column('device_id', sa.Uuid(), nullable=False),
    sa.Column('geofence_id', sa.Uuid(), nullable=False),
    sa.Column('geofence_name', sa.String(length=100), nullable=False),
    sa.Column('event_type', sa.String(length=8), nullable=False),
    sa.Column('occurred_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('received_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.Column('id', sa.Uuid(), nullable=False),
    sa.CheckConstraint("event_type IN ('ENTER', 'EXIT')", name='ck_geofence_events_type_valid'),
    sa.ForeignKeyConstraint(['device_id'], ['devices.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_geofence_events_device_id'), 'geofence_events', ['device_id'], unique=False)


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index(op.f('ix_geofence_events_device_id'), table_name='geofence_events')
    op.drop_table('geofence_events')

    op.drop_index(op.f('ix_geofences_device_id'), table_name='geofences')
    op.drop_table('geofences')

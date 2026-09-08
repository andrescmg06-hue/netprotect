"""devices.fcm_token: FCM registration token for the real-time wake-up nudge (Sprint 18)

Revision ID: d3f6a9c2b5e7
Revises: b7c3e0d4f1a8
Create Date: 2026-09-08 09:00:00.000000

"""
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'd3f6a9c2b5e7'
down_revision: str | Sequence[str] | None = 'b7c3e0d4f1a8'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column('devices', sa.Column('fcm_token', sa.String(length=255), nullable=True))
    op.add_column(
        'devices', sa.Column('fcm_token_updated_at', sa.DateTime(timezone=True), nullable=True)
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_column('devices', 'fcm_token_updated_at')
    op.drop_column('devices', 'fcm_token')

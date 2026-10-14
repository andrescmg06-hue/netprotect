"""geofences, geofence_events, alerts: owner tutor per geofence (B-12, Sprint 69)

Revision ID: f2b6c9d1e4a7
Revises: c8a1e5b90d34
Create Date: 2026-10-14 12:00:00.000000

"""
import logging
from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = 'f2b6c9d1e4a7'
down_revision: str | Sequence[str] | None = 'c8a1e5b90d34'
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

logger = logging.getLogger("alembic.runtime.migration")

# Devices that have only ever had ONE tutor (counting unlinked links too) who is still linked
# now, and that tutor. Only then is the owner of a pre-existing row unambiguous. A device that
# was re-linked from tutor A to tutor B has exactly one active tutor today, but its geofences
# were drawn by A: giving them to B would hand the new tutor the named places of the previous
# one, which is the very leak B-12 closes. So with several tutors in its history, or none
# active, the rows keep a NULL owner: invisible to every tutor and never evaluated again,
# without deleting anything. Hiding a minor's labelled place beats guessing who it belongs to
# (docs/sprint-69.md).
_SOLE_TUTOR_EVER = """
    SELECT device_id, (array_agg(tutor_user_id))[1] AS tutor_user_id
    FROM tutor_devices
    GROUP BY device_id
    HAVING COUNT(DISTINCT tutor_user_id) = 1
       AND bool_or(unlinked_at IS NULL)
"""

_GEOFENCE_ALERT_TYPES = "('GEOFENCE_ENTER', 'GEOFENCE_EXIT')"


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column('geofences', sa.Column('created_by_user_id', sa.Uuid(), nullable=True))
    op.create_foreign_key(
        'geofences_created_by_user_id_fkey', 'geofences', 'users',
        ['created_by_user_id'], ['id'], ondelete='CASCADE',
    )
    op.create_index(
        op.f('ix_geofences_created_by_user_id'), 'geofences', ['created_by_user_id'],
        unique=False,
    )

    op.add_column('geofence_events', sa.Column('tutor_user_id', sa.Uuid(), nullable=True))
    op.create_foreign_key(
        'geofence_events_tutor_user_id_fkey', 'geofence_events', 'users',
        ['tutor_user_id'], ['id'], ondelete='CASCADE',
    )
    op.create_index(
        op.f('ix_geofence_events_tutor_user_id'), 'geofence_events', ['tutor_user_id'],
        unique=False,
    )

    op.add_column('alerts', sa.Column('geofence_owner_user_id', sa.Uuid(), nullable=True))
    op.create_foreign_key(
        'alerts_geofence_owner_user_id_fkey', 'alerts', 'users',
        ['geofence_owner_user_id'], ['id'], ondelete='CASCADE',
    )
    op.create_index(
        op.f('ix_alerts_geofence_owner_user_id'), 'alerts', ['geofence_owner_user_id'],
        unique=False,
    )

    # 1. Geofences: the device's sole active tutor, if there is exactly one.
    op.execute(f"""
        UPDATE geofences AS g
        SET created_by_user_id = sole.tutor_user_id
        FROM ({_SOLE_TUTOR_EVER}) AS sole
        WHERE g.device_id = sole.device_id
    """)  # noqa: S608 - interpolates internal constants only

    # 2. Events whose geofence still exists inherit its owner (NULL stays NULL).
    op.execute("""
        UPDATE geofence_events AS e
        SET tutor_user_id = g.created_by_user_id
        FROM geofences AS g
        WHERE g.id = e.geofence_id AND g.device_id = e.device_id
    """)
    # 3. Events of an already deleted geofence: same sole-active-tutor rule as step 1.
    op.execute(f"""
        UPDATE geofence_events AS e
        SET tutor_user_id = sole.tutor_user_id
        FROM ({_SOLE_TUTOR_EVER}) AS sole
        WHERE e.device_id = sole.device_id
          AND NOT EXISTS (
              SELECT 1 FROM geofences AS g
              WHERE g.id = e.geofence_id AND g.device_id = e.device_id
          )
    """)  # noqa: S608 - interpolates internal constants only

    # 4 and 5. Geofence alerts, same two steps as the events. Other alert types keep NULL: they
    # are not tied to a geofence and stay visible to every tutor of the device.
    op.execute(f"""
        UPDATE alerts AS a
        SET geofence_owner_user_id = g.created_by_user_id
        FROM geofences AS g
        WHERE a.alert_type IN {_GEOFENCE_ALERT_TYPES}
          AND g.id = a.geofence_id AND g.device_id = a.device_id
    """)  # noqa: S608 - interpolates internal constants only
    op.execute(f"""
        UPDATE alerts AS a
        SET geofence_owner_user_id = sole.tutor_user_id
        FROM ({_SOLE_TUTOR_EVER}) AS sole
        WHERE a.alert_type IN {_GEOFENCE_ALERT_TYPES}
          AND a.device_id = sole.device_id
          AND NOT EXISTS (
              SELECT 1 FROM geofences AS g
              WHERE g.id = a.geofence_id AND g.device_id = a.device_id
          )
    """)  # noqa: S608 - interpolates internal constants only

    # Counts only (no ids, names or coordinates), so the run itself documents what was hidden.
    bind = op.get_bind()
    for table, column, extra in (
        ('geofences', 'created_by_user_id', ''),
        ('geofence_events', 'tutor_user_id', ''),
        ('alerts', 'geofence_owner_user_id', f' AND alert_type IN {_GEOFENCE_ALERT_TYPES}'),
    ):
        orphans = bind.execute(
            sa.text(f"SELECT COUNT(*) FROM {table} WHERE {column} IS NULL{extra}")  # noqa: S608
        ).scalar_one()
        logger.info("B-12 backfill: %s rows left without an owner in %s", orphans, table)


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index(op.f('ix_alerts_geofence_owner_user_id'), table_name='alerts')
    op.drop_constraint('alerts_geofence_owner_user_id_fkey', 'alerts', type_='foreignkey')
    op.drop_column('alerts', 'geofence_owner_user_id')

    op.drop_index(op.f('ix_geofence_events_tutor_user_id'), table_name='geofence_events')
    op.drop_constraint(
        'geofence_events_tutor_user_id_fkey', 'geofence_events', type_='foreignkey'
    )
    op.drop_column('geofence_events', 'tutor_user_id')

    op.drop_index(op.f('ix_geofences_created_by_user_id'), table_name='geofences')
    op.drop_constraint('geofences_created_by_user_id_fkey', 'geofences', type_='foreignkey')
    op.drop_column('geofences', 'created_by_user_id')

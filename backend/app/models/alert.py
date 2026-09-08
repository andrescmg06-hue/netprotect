import uuid
from datetime import datetime

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, UUIDPrimaryKeyMixin

INFO = "INFO"
WARNING = "WARNING"
HIGH = "HIGH"
CRITICAL = "CRITICAL"
# HIGH/CRITICAL have no generator yet in this sprint — reserved for the manipulation-detection
# signals of Sprint 20, same reservation already made for DeviceStatus.ALERT
# (app/models/device.py). Kept in the catalog now so that sprint only needs a new alert_type, not
# a level migration.
ALERT_LEVELS = (INFO, WARNING, HIGH, CRITICAL)
_LEVEL_LIST_SQL = ", ".join(f"'{value}'" for value in ALERT_LEVELS)

APP_BLOCKED = "APP_BLOCKED"
APP_LIMIT_REACHED = "APP_LIMIT_REACHED"
GEOFENCE_ENTER = "GEOFENCE_ENTER"
GEOFENCE_EXIT = "GEOFENCE_EXIT"
ALERT_TYPES = (APP_BLOCKED, APP_LIMIT_REACHED, GEOFENCE_ENTER, GEOFENCE_EXIT)
_TYPE_LIST_SQL = ", ".join(f"'{value}'" for value in ALERT_TYPES)


class Alert(UUIDPrimaryKeyMixin, Base):
    """A tutor-facing notification generated from a signal that already existed — an
    AppRuleEvent (Sprint 8) or a GeofenceEvent (Sprint 14) — not a new detection pipeline (Sprint
    17). See app/services/alerts.py for the generation rules.

    Deduplication has no arbitrary time window: while an alert with the same dedup_key is still
    unread (read_at is None), a repeat of the same signal bumps occurrence_count/last_occurred_at
    on that row instead of inserting a new one. Marking it read closes that window — the next
    occurrence opens a fresh alert. dedup_key is f"{alert_type}:{package_name-or-geofence_id}",
    computed by the caller (app/services/alerts.py), not stored redundantly with those fields.

    package_name is populated for APP_BLOCKED/APP_LIMIT_REACHED, geofence_id/geofence_name for
    GEOFENCE_ENTER/GEOFENCE_EXIT — same flat-shape-with-nulls pattern as AppRuleEvent/GeofenceEvent
    already merged into HistoryEventResponse (Sprint 15).

    Retention (90 days by default, settings.alert_retention_days): purged at write time via the
    shared purge_expired_rows() helper (app/services/retention.py, Sprint 15), scoped to
    device_id, on last_occurred_at — same "purge at write, no scheduler" pattern as every other
    insert-heavy table in this project.
    """

    __tablename__ = "alerts"
    __table_args__ = (
        Index("ix_alerts_device_last_occurred", "device_id", "last_occurred_at"),
        CheckConstraint(f"level IN ({_LEVEL_LIST_SQL})", name="ck_alerts_level_valid"),
        CheckConstraint(f"alert_type IN ({_TYPE_LIST_SQL})", name="ck_alerts_type_valid"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    level: Mapped[str] = mapped_column(String(8))
    alert_type: Mapped[str] = mapped_column(String(32))
    dedup_key: Mapped[str] = mapped_column(String(300))
    package_name: Mapped[str | None] = mapped_column(String(255))
    geofence_id: Mapped[uuid.UUID | None] = mapped_column()
    geofence_name: Mapped[str | None] = mapped_column(String(100))
    occurrence_count: Mapped[int] = mapped_column(Integer, default=1)
    first_occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    last_occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    read_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class AlertSilence(UUIDPrimaryKeyMixin, Base):
    """A tutor-configured mute for one dedup_key on one device — created from an existing alert
    (POST /devices/{id}/alerts/{alert_id}/silence), checked by app/services/alerts.py before
    generating a new one. silenced_until is null for "indefinite, until the tutor removes it".
    """

    __tablename__ = "alert_silences"
    __table_args__ = (
        UniqueConstraint("device_id", "dedup_key", name="uq_alert_silences_device_dedup_key"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    dedup_key: Mapped[str] = mapped_column(String(300))
    silenced_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

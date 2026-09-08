import uuid
from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, Float, ForeignKey, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, UUIDPrimaryKeyMixin

ENTER = "ENTER"
EXIT = "EXIT"
GEOFENCE_EVENT_TYPES = (ENTER, EXIT)
_EVENT_TYPE_LIST_SQL = ", ".join(f"'{value}'" for value in GEOFENCE_EVENT_TYPES)


class Geofence(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    """A tutor-defined circular zone on one device — "alta, baja y edición" (Sprint 14). Centre
    coordinates are encrypted the same way as DeviceLocationReport (Fernet, column-level,
    app/core/crypto.py): a named geofence ("Casa", "Colegio") is if anything MORE sensitive than
    a raw location fix, since the tutor has explicitly labelled what that point means.

    No Android/GMS Geofencing API involved — see docs/sprint-14.md for why: that API requires
    ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION and the play-services-location dependency,
    all three deliberately avoided since Sprint 13. Instead, transitions are detected here in the
    backend by comparing consecutive DeviceLocationReport rows against this table (see
    app/api/v1/endpoints/location.py) — no new Android permission or dependency needed.
    """

    __tablename__ = "geofences"
    __table_args__ = (
        CheckConstraint("radius_meters > 0", name="ck_geofences_radius_positive"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    name: Mapped[str] = mapped_column(String(100))
    latitude_ciphertext: Mapped[str] = mapped_column(Text)
    longitude_ciphertext: Mapped[str] = mapped_column(Text)
    radius_meters: Mapped[float] = mapped_column(Float)


class GeofenceEvent(UUIDPrimaryKeyMixin, Base):
    """Insert-only record of a device crossing a geofence boundary — evidence of a transition,
    same reasoning as AppRuleEvent (Sprint 8). Not foreign-keyed to Geofence and carries its own
    geofence_name snapshot: a geofence can be renamed or deleted later, but the fact that the
    device entered/left "Casa" at a given moment must survive that (same pattern AppRuleEvent
    uses to survive an AppRule being deleted).

    Detected inline in POST /devices/{id}/location by comparing this device's two most recent
    location reports against each active geofence — see docs/sprint-14.md for why the very first
    report against a given geofence never fires an event (no prior point to compare against
    means no transition can be established, only a baseline).
    """

    __tablename__ = "geofence_events"
    __table_args__ = (
        CheckConstraint(
            f"event_type IN ({_EVENT_TYPE_LIST_SQL})", name="ck_geofence_events_type_valid"
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    geofence_id: Mapped[uuid.UUID] = mapped_column()
    geofence_name: Mapped[str] = mapped_column(String(100))
    event_type: Mapped[str] = mapped_column(String(8))
    # The device's own clock for the location report that triggered detection — distinct from
    # received_at, same split as AppRuleEvent.occurred_at/received_at.
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    received_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

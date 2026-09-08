import uuid
from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, Float, ForeignKey, Index, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, UUIDPrimaryKeyMixin


class DeviceLocationReport(UUIDPrimaryKeyMixin, Base):
    """One approximate location fix reported by a supervised device — insert-only, same
    reasoning as AppRuleEvent (Sprint 8): this is evidence the device captured at a moment in
    time, not a row a tutor or the device ever edits.

    latitude/longitude are stored encrypted (Fernet, app/core/crypto.py) rather than as plain
    Float columns: this is precise-enough-to-identify-a-home location for a minor, the most
    sensitive data category this project has stored so far, and a plain SQL dump or a leaked
    read-only credential must not expose it in the clear. accuracy_meters is not sensitive on
    its own (a radius without a center means nothing) and is kept queryable/plain.

    Retention is enforced by the write endpoint, not a scheduled job (this project has no
    background scheduler yet — see devices.timezone / device_offline_threshold_seconds for the
    same "compute/purge at request time" pattern): every POST /location call deletes this
    device's own rows older than settings.location_retention_days before inserting the new one.
    See docs/sprint-13.md for why this window's length was chosen.
    """

    __tablename__ = "device_location_reports"
    __table_args__ = (
        Index("ix_device_location_reports_device_captured", "device_id", "captured_at"),
        CheckConstraint("accuracy_meters >= 0", name="ck_device_location_reports_accuracy_valid"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    latitude_ciphertext: Mapped[str] = mapped_column(Text)
    longitude_ciphertext: Mapped[str] = mapped_column(Text)
    # Meters, as reported by Android's Location#getAccuracy() (radius of 68% confidence).
    # Deliberately not encrypted — see class docstring.
    accuracy_meters: Mapped[float] = mapped_column(Float)
    # The device's own clock when the fix was captured — distinct from received_at, same split
    # already used for AppRuleEvent.occurred_at/received_at.
    captured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    received_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

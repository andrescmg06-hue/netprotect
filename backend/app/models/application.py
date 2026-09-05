import uuid
from datetime import date, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, UUIDPrimaryKeyMixin


class DeviceApplication(UUIDPrimaryKeyMixin, Base):
    """Catalog of apps a supervised device has reported. Soft-deleted (uninstalled_at) rather
    than removed when a sync no longer reports a package, so a tutor can see "this was
    uninstalled" instead of the app silently disappearing from the list.
    """

    __tablename__ = "device_applications"
    __table_args__ = (
        UniqueConstraint("device_id", "package_name", name="uq_device_applications_device_package"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    package_name: Mapped[str] = mapped_column(String(255))
    app_label: Mapped[str] = mapped_column(String(255))
    is_system_app: Mapped[bool] = mapped_column(default=False, server_default="false")
    first_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    last_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    uninstalled_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class DeviceApplicationUsage(UUIDPrimaryKeyMixin, Base):
    """Daily foreground-time totals per app, one row per calendar day the device reported.

    Deliberately not foreign-keyed to DeviceApplication: usage history must survive an app
    being uninstalled later, so the two tables are linked only by (device_id, package_name),
    not by a hard relationship whose lifecycle would delete history with the catalog row.
    """

    __tablename__ = "device_application_usage"
    __table_args__ = (
        UniqueConstraint(
            "device_id", "package_name", "usage_date", name="uq_device_application_usage_day"
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    package_name: Mapped[str] = mapped_column(String(255))
    # The device's local calendar day at sync time. Not normalized against a device timezone
    # yet (that lands with the rest of the timezone-aware scheduling in Sprint 11); treated
    # here as an opaque label the device assigns, not something the server recomputes.
    usage_date: Mapped[date] = mapped_column(Date)
    foreground_seconds: Mapped[int] = mapped_column(Integer)
    synced_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

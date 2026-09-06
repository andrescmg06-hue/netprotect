import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, String, func, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, UUIDPrimaryKeyMixin

if TYPE_CHECKING:
    from app.models.user import User

ONLINE = "ONLINE"
OFFLINE = "OFFLINE"
SYNCING = "SYNCING"
ALERT = "ALERT"
RESTRICTED = "RESTRICTED"
UNLINKED = "UNLINKED"

DEVICE_STATUSES = (ONLINE, OFFLINE, SYNCING, ALERT, RESTRICTED, UNLINKED)
_STATUS_LIST_SQL = ", ".join(f"'{value}'" for value in DEVICE_STATUSES)

# Default app policy: what happens to an app with no rule of its own. ALLOW is the blocklist
# model (everything runs unless a rule blocks it); BLOCK is the allowlist model (nothing runs
# unless a rule allows it). ALLOW is the default so existing devices keep behaving exactly as
# they did before this column existed.
POLICY_ALLOW = "ALLOW"
POLICY_BLOCK = "BLOCK"

DEFAULT_APP_POLICIES = (POLICY_ALLOW, POLICY_BLOCK)
_POLICY_LIST_SQL = ", ".join(f"'{value}'" for value in DEFAULT_APP_POLICIES)


class Device(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    __tablename__ = "devices"
    __table_args__ = (
        # Scoped to the supervised user rather than global: one account's app must not be
        # able to claim an identifier another account is already using.
        Index(
            "uq_devices_instance_per_supervised_user",
            "supervised_user_id",
            "device_instance_id",
            unique=True,
            postgresql_where=text("device_instance_id IS NOT NULL"),
        ),
        CheckConstraint(
            f"default_app_policy IN ({_POLICY_LIST_SQL})", name="ck_devices_default_policy_valid"
        ),
    )

    name: Mapped[str] = mapped_column(String(255))
    platform: Mapped[str] = mapped_column(String(32), default="ANDROID", server_default="ANDROID")
    os_version: Mapped[str | None] = mapped_column(String(64))
    app_version: Mapped[str | None] = mapped_column(String(32))
    # Stable identifier generated once by the installed app, so re-pairing the same phone
    # (or pairing it to a second tutor) reuses this row instead of creating a duplicate.
    device_instance_id: Mapped[str | None] = mapped_column(String(64))
    # Shared by every tutor of this device, like the rules themselves: a device can have more
    # than one tutor, and "one says allowlist, the other says blocklist" has no good answer.
    # Who changed it is in audit_logs.
    default_app_policy: Mapped[str] = mapped_column(
        String(16), default=POLICY_ALLOW, server_default=POLICY_ALLOW
    )
    supervised_user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="RESTRICT"), index=True
    )

    supervised_user: Mapped["User"] = relationship(back_populates="supervised_devices")
    status: Mapped["DeviceStatus"] = relationship(
        back_populates="device", uselist=False, cascade="all, delete-orphan"
    )
    tutor_links: Mapped[list["TutorDevice"]] = relationship(back_populates="device")


class DeviceStatus(Base):
    __tablename__ = "device_status"
    __table_args__ = (
        CheckConstraint(f"status IN ({_STATUS_LIST_SQL})", name="ck_device_status_status_valid"),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), primary_key=True
    )
    status: Mapped[str] = mapped_column(String(16), default=UNLINKED, server_default=UNLINKED)
    last_seen_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    last_sync_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    device: Mapped["Device"] = relationship(back_populates="status")


class TutorDevice(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "tutor_devices"
    __table_args__ = (
        Index(
            "uq_tutor_devices_active_link",
            "tutor_user_id",
            "device_id",
            unique=True,
            postgresql_where=text("unlinked_at IS NULL"),
        ),
    )

    tutor_user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    linked_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    unlinked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    tutor: Mapped["User"] = relationship(
        back_populates="tutor_links", foreign_keys=[tutor_user_id]
    )
    device: Mapped["Device"] = relationship(back_populates="tutor_links")

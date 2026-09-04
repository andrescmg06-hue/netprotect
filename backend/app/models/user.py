from typing import TYPE_CHECKING

from sqlalchemy import Boolean, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, UUIDPrimaryKeyMixin

if TYPE_CHECKING:
    from app.models.device import Device, TutorDevice
    from app.models.role import UserRole


class User(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    __tablename__ = "users"

    email: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    google_sub: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    display_name: Mapped[str | None] = mapped_column(String(255))
    avatar_url: Mapped[str | None] = mapped_column(String(2048))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, server_default="true")

    roles: Mapped[list["UserRole"]] = relationship(back_populates="user")
    tutor_links: Mapped[list["TutorDevice"]] = relationship(
        back_populates="tutor", foreign_keys="TutorDevice.tutor_user_id"
    )
    supervised_devices: Mapped[list["Device"]] = relationship(back_populates="supervised_user")

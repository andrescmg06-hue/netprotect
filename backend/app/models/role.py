import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, UUIDPrimaryKeyMixin

if TYPE_CHECKING:
    from app.models.user import User

TUTOR = "TUTOR"
SUPERVISADO = "SUPERVISADO"


class Role(Base):
    __tablename__ = "roles"

    code: Mapped[str] = mapped_column(String(32), primary_key=True)
    description: Mapped[str] = mapped_column(String(255))

    user_roles: Mapped[list["UserRole"]] = relationship(back_populates="role")


class UserRole(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "user_roles"
    __table_args__ = (UniqueConstraint("user_id", "role_code", name="uq_user_roles_user_role"),)

    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    role_code: Mapped[str] = mapped_column(ForeignKey("roles.code", ondelete="RESTRICT"))
    granted_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    user: Mapped["User"] = relationship(back_populates="roles")
    role: Mapped["Role"] = relationship(back_populates="user_roles")

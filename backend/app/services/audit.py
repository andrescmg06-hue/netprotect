import uuid

from sqlalchemy.ext.asyncio import AsyncSession

from app.models import AuditLog


async def record_audit_event(
    session: AsyncSession,
    *,
    actor_user_id: uuid.UUID | None,
    action: str,
    resource_type: str | None = None,
    resource_id: str | None = None,
    ip_address: str | None = None,
) -> None:
    session.add(
        AuditLog(
            actor_user_id=actor_user_id,
            action=action,
            resource_type=resource_type,
            resource_id=resource_id,
            ip_address=ip_address,
        )
    )

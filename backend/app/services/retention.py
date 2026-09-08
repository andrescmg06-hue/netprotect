import uuid
from datetime import UTC, datetime, timedelta

from sqlalchemy import delete
from sqlalchemy.ext.asyncio import AsyncSession


async def purge_expired_rows(
    db: AsyncSession,
    model: type,
    time_column,
    device_id: uuid.UUID,
    retention_days: int,
) -> None:
    """Deletes this device's own rows of `model` older than `retention_days`, scoped to
    `device_id` — never a global sweep across devices. Shared by every "purge at write time, no
    scheduler" call site (DeviceLocationReport since Sprint 13, AppRuleEvent and GeofenceEvent
    since Sprint 15) so the device_id scoping can't be silently dropped by a future copy-paste.
    """
    cutoff = datetime.now(UTC) - timedelta(days=retention_days)
    await db.execute(
        delete(model).where(model.device_id == device_id, time_column < cutoff)
    )

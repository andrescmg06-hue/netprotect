import uuid
from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.alert import Alert, AlertSilence
from app.services.retention import purge_expired_rows

# rule_type_applied values (app/models/rule.py) that mean "a time budget ran out", as opposed to
# every other value, which means "this app didn't run at all" for one reason or another.
_LIMIT_RULE_TYPES = frozenset({"DAILY_LIMIT", "WEEKLY_LIMIT"})
# ALLOW is the one rule_type_applied value that isn't a block of any kind — Sprint 8 lets a device
# report it, but there is nothing to alert a tutor about when nothing was blocked.
_NO_ALERT_RULE_TYPES = frozenset({"ALLOW"})


async def record_alert_for_rule_event(
    db: AsyncSession,
    device_id: uuid.UUID,
    package_name: str,
    rule_type_applied: str,
    occurred_at: datetime,
) -> None:
    if rule_type_applied in _NO_ALERT_RULE_TYPES:
        return
    level = "WARNING" if rule_type_applied in _LIMIT_RULE_TYPES else "INFO"
    alert_type = "APP_LIMIT_REACHED" if rule_type_applied in _LIMIT_RULE_TYPES else "APP_BLOCKED"
    await _record_alert(
        db,
        device_id=device_id,
        level=level,
        alert_type=alert_type,
        dedup_key=f"{alert_type}:{package_name}",
        occurred_at=occurred_at,
        package_name=package_name,
    )


async def record_alert_for_geofence_event(
    db: AsyncSession,
    device_id: uuid.UUID,
    geofence_id: uuid.UUID,
    geofence_name: str,
    event_type: str,
    occurred_at: datetime,
) -> None:
    alert_type = "GEOFENCE_EXIT" if event_type == "EXIT" else "GEOFENCE_ENTER"
    level = "WARNING" if event_type == "EXIT" else "INFO"
    await _record_alert(
        db,
        device_id=device_id,
        level=level,
        alert_type=alert_type,
        dedup_key=f"{alert_type}:{geofence_id}",
        occurred_at=occurred_at,
        geofence_id=geofence_id,
        geofence_name=geofence_name,
    )


async def _record_alert(
    db: AsyncSession,
    *,
    device_id: uuid.UUID,
    level: str,
    alert_type: str,
    dedup_key: str,
    occurred_at: datetime,
    package_name: str | None = None,
    geofence_id: uuid.UUID | None = None,
    geofence_name: str | None = None,
) -> None:
    """Generates a tutor-facing alert from a signal that just happened, or folds it into an
    already-open one — see Alert's docstring (app/models/alert.py) for the no-time-window
    deduplication rule this implements. A silenced dedup_key produces nothing at all: the tutor
    asked not to hear about this again (yet), so it's not even recorded as a suppressed
    occurrence.
    """
    await purge_expired_rows(
        db, Alert, Alert.last_occurred_at, device_id, settings.alert_retention_days
    )

    now = datetime.now(UTC)
    silence = (
        await db.execute(
            select(AlertSilence).where(
                AlertSilence.device_id == device_id, AlertSilence.dedup_key == dedup_key
            )
        )
    ).scalar_one_or_none()
    if silence is not None and (silence.silenced_until is None or silence.silenced_until > now):
        return

    open_alert = (
        await db.execute(
            select(Alert).where(
                Alert.device_id == device_id,
                Alert.dedup_key == dedup_key,
                Alert.read_at.is_(None),
            )
        )
    ).scalar_one_or_none()
    if open_alert is not None:
        open_alert.occurrence_count += 1
        open_alert.last_occurred_at = occurred_at
        return

    db.add(
        Alert(
            device_id=device_id,
            level=level,
            alert_type=alert_type,
            dedup_key=dedup_key,
            package_name=package_name,
            geofence_id=geofence_id,
            geofence_name=geofence_name,
            occurrence_count=1,
            first_occurred_at=occurred_at,
            last_occurred_at=occurred_at,
        )
    )

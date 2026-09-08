import uuid
from datetime import UTC, datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.alert import CRITICAL, HIGH
from app.models.device import ONLINE, DeviceStatus
from app.services.alerts import record_alert_for_tamper_signal

PERMISSION_REVOKED = "PERMISSION_REVOKED"
SERVICE_INACTIVE = "SERVICE_INACTIVE"
HEARTBEAT_SILENCE = "HEARTBEAT_SILENCE"
CLOCK_TAMPERING = "CLOCK_TAMPERING"
UNINSTALL_ATTEMPT = "UNINSTALL_ATTEMPT"

# The only tamper signal reported through its own endpoint (POST /devices/{id}/tamper-events)
# rather than folded into every heartbeat — see app/api/v1/endpoints/devices.py. Deliberately
# CRITICAL, one level above the other four: those are conditions the device can recover from on
# its own next beat (permission re-granted, clock fixed); this one means the supervised user is
# actively trying to remove the app's protection altogether.
TAMPER_EVENT_TYPES = (UNINSTALL_ATTEMPT,)

_LEVELS = {
    PERMISSION_REVOKED: HIGH,
    SERVICE_INACTIVE: HIGH,
    HEARTBEAT_SILENCE: HIGH,
    CLOCK_TAMPERING: HIGH,
    UNINSTALL_ATTEMPT: CRITICAL,
}


async def evaluate_heartbeat_tamper_signals(
    db: AsyncSession,
    status_row: DeviceStatus,
    *,
    usage_access_granted: bool | None,
    service_active: bool | None,
    device_time: datetime | None,
    now: datetime,
) -> list[str]:
    """Reads the three optional self-reported fields a heartbeat can carry (Sprint 20) plus the
    device's *previous* last_seen_at (still on status_row — the caller has not overwritten it
    yet) and returns every tamper signal this beat exhibits, having already recorded an Alert for
    each one via app/services/alerts.py.

    Each field is `None` when the installed app build doesn't know how to report it yet (an older
    APK) or the caller genuinely has no information (e.g. SyncWorker's first run before
    RuleEnforcementService has ever posted a liveness marker) — treated as "no signal", never as
    a tamper condition on its own, so a device that simply hasn't been updated isn't flagged.
    """
    detected: list[str] = []

    if usage_access_granted is False:
        detected.append(PERMISSION_REVOKED)

    if service_active is False:
        detected.append(SERVICE_INACTIVE)

    if device_time is not None:
        # A naive datetime is accepted by the schema (plain `datetime`, same as occurred_at in
        # every other device-reported payload) but subtracting one from an aware `now` raises
        # TypeError — a 500 any supervised device could trigger with one malformed field. Read as
        # UTC, which is what the Android client sends (Instant.toString() is always "...Z").
        reported_at = (
            device_time if device_time.tzinfo is not None else device_time.replace(tzinfo=UTC)
        )
        if abs((now - reported_at).total_seconds()) > settings.device_clock_skew_alert_seconds:
            detected.append(CLOCK_TAMPERING)

    if (
        status_row.status == ONLINE
        and status_row.last_seen_at is not None
        and (now - status_row.last_seen_at).total_seconds()
        > settings.device_heartbeat_silence_alert_seconds
    ):
        detected.append(HEARTBEAT_SILENCE)

    for signal in detected:
        await record_alert_for_tamper_signal(db, status_row.device_id, signal, _LEVELS[signal], now)

    return detected


async def record_tamper_event(
    db: AsyncSession, device_id: uuid.UUID, event_type: str, occurred_at: datetime
) -> None:
    """The write side of POST /devices/{id}/tamper-events — currently only UNINSTALL_ATTEMPT.
    No dedicated event-log table (unlike AppRuleEvent/GeofenceEvent): the Alert this generates
    already carries first/last_occurred_at and occurrence_count, which is enough history within
    alert_retention_days, and there was no pre-existing signal here to repurpose the way Sprint 17
    repurposed those two tables.
    """
    await record_alert_for_tamper_signal(
        db, device_id, event_type, _LEVELS[event_type], occurred_at
    )

import uuid
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, require_tutor_of_device
from app.db.session import get_db
from app.models import Alert, AlertSilence, Device, User
from app.schemas.alert import (
    MAX_ALERTS,
    AlertListResponse,
    AlertResponse,
    AlertSilenceListResponse,
    AlertSilenceResponse,
    SilenceAlertRequest,
)
from app.services.audit import record_audit_event

router = APIRouter(tags=["alerts"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _to_alert_response(alert: Alert) -> AlertResponse:
    return AlertResponse(
        id=alert.id,
        level=alert.level,
        alert_type=alert.alert_type,
        dedup_key=alert.dedup_key,
        package_name=alert.package_name,
        geofence_id=alert.geofence_id,
        geofence_name=alert.geofence_name,
        occurrence_count=alert.occurrence_count,
        first_occurred_at=alert.first_occurred_at,
        last_occurred_at=alert.last_occurred_at,
        read_at=alert.read_at,
    )


@router.get("/devices/{device_id}/alerts", response_model=AlertListResponse)
async def list_alerts(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AlertListResponse:
    alerts = (
        await db.execute(
            select(Alert)
            .where(Alert.device_id == device_id)
            .order_by(Alert.last_occurred_at.desc())
            .limit(MAX_ALERTS)
        )
    ).scalars().all()
    return AlertListResponse(alerts=[_to_alert_response(alert) for alert in alerts])


@router.post("/devices/{device_id}/alerts/{alert_id}/read", response_model=AlertResponse)
async def mark_alert_read(
    device_id: uuid.UUID,
    alert_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AlertResponse:
    """Marking an alert read also closes its deduplication window (Alert's docstring,
    app/models/alert.py): the next occurrence of the same signal opens a fresh alert instead of
    bumping this one's occurrence_count.
    """
    alert = (
        await db.execute(select(Alert).where(Alert.id == alert_id, Alert.device_id == device_id))
    ).scalar_one_or_none()
    if alert is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="alert_not_found")

    alert.read_at = datetime.now(UTC)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="ALERT_READ",
        resource_type="alert",
        resource_id=str(alert_id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(alert)
    return _to_alert_response(alert)


@router.post(
    "/devices/{device_id}/alerts/{alert_id}/silence", response_model=AlertSilenceResponse
)
async def silence_alert(
    device_id: uuid.UUID,
    alert_id: uuid.UUID,
    payload: SilenceAlertRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AlertSilenceResponse:
    """Silences future alerts sharing this alert's dedup_key, not just this one row — e.g. every
    future APP_BLOCKED alert for the same package, not just today's. Upserts AlertSilence: a
    second silence call on the same dedup_key replaces the previous expiry rather than stacking.
    """
    alert = (
        await db.execute(select(Alert).where(Alert.id == alert_id, Alert.device_id == device_id))
    ).scalar_one_or_none()
    if alert is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="alert_not_found")

    silenced_until = (
        datetime.now(UTC) + timedelta(days=payload.days) if payload.days is not None else None
    )
    existing = (
        await db.execute(
            select(AlertSilence).where(
                AlertSilence.device_id == device_id, AlertSilence.dedup_key == alert.dedup_key
            )
        )
    ).scalar_one_or_none()
    if existing is not None:
        existing.silenced_until = silenced_until
        silence = existing
    else:
        silence = AlertSilence(
            device_id=device_id, dedup_key=alert.dedup_key, silenced_until=silenced_until
        )
        db.add(silence)

    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="ALERT_SILENCED",
        resource_type="alert_silence",
        resource_id=alert.dedup_key,
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(silence)
    return AlertSilenceResponse(
        id=silence.id, dedup_key=silence.dedup_key, silenced_until=silence.silenced_until
    )


@router.get("/devices/{device_id}/alert-silences", response_model=AlertSilenceListResponse)
async def list_alert_silences(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AlertSilenceListResponse:
    silences = (
        await db.execute(select(AlertSilence).where(AlertSilence.device_id == device_id))
    ).scalars().all()
    return AlertSilenceListResponse(
        silences=[
            AlertSilenceResponse(id=s.id, dedup_key=s.dedup_key, silenced_until=s.silenced_until)
            for s in silences
        ]
    )


@router.delete(
    "/devices/{device_id}/alert-silences/{silence_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def delete_alert_silence(
    device_id: uuid.UUID,
    silence_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> None:
    silence = (
        await db.execute(
            select(AlertSilence).where(
                AlertSilence.id == silence_id, AlertSilence.device_id == device_id
            )
        )
    ).scalar_one_or_none()
    if silence is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="alert_silence_not_found")

    dedup_key = silence.dedup_key
    await db.delete(silence)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="ALERT_SILENCE_REMOVED",
        resource_type="alert_silence",
        resource_id=dedup_key,
        ip_address=_client_ip(request),
    )
    await db.commit()

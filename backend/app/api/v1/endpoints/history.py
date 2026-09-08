import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_tutor_of_device
from app.db.session import get_db
from app.models import AppRuleEvent, Device, GeofenceEvent
from app.schemas.history import MAX_HISTORY_EVENTS, DeviceHistoryResponse, HistoryEventResponse

router = APIRouter(tags=["history"])


@router.get("/devices/{device_id}/history", response_model=DeviceHistoryResponse)
async def get_device_history(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> DeviceHistoryResponse:
    """A single chronological timeline for a tutor reviewing what happened on a device (Sprint
    15), merging the two event logs that already existed: AppRuleEvent (Sprint 8, rule
    enforcement) and GeofenceEvent (Sprint 14, zone crossings). Both are queried independently
    (each already indexed on (device_id, occurred_at) for this), merged in Python and re-sorted,
    then capped at MAX_HISTORY_EVENTS — simpler than a SQL UNION across two differently-shaped
    tables for a response size this endpoint is already going to bound anyway.

    Deliberately doesn't build a new "history" table: both source tables are already insert-only
    with their own retention (app_rule_event_retention_days / geofence_event_retention_days,
    enforced by their own write endpoints), so this endpoint only reads and merges what already
    exists rather than duplicating it.
    """
    rule_events = (
        await db.execute(
            select(AppRuleEvent)
            .where(AppRuleEvent.device_id == device_id)
            .order_by(AppRuleEvent.occurred_at.desc())
            .limit(MAX_HISTORY_EVENTS)
        )
    ).scalars().all()
    geofence_events = (
        await db.execute(
            select(GeofenceEvent)
            .where(GeofenceEvent.device_id == device_id)
            .order_by(GeofenceEvent.occurred_at.desc())
            .limit(MAX_HISTORY_EVENTS)
        )
    ).scalars().all()

    events = [
        HistoryEventResponse(
            id=event.id,
            event_type="APP_RULE",
            occurred_at=event.occurred_at,
            received_at=event.received_at,
            package_name=event.package_name,
            rule_type_applied=event.rule_type_applied,
        )
        for event in rule_events
    ] + [
        HistoryEventResponse(
            id=event.id,
            event_type="GEOFENCE",
            occurred_at=event.occurred_at,
            received_at=event.received_at,
            geofence_id=event.geofence_id,
            geofence_name=event.geofence_name,
            geofence_event_type=event.event_type,
        )
        for event in geofence_events
    ]
    events.sort(key=lambda event: event.occurred_at, reverse=True)

    return DeviceHistoryResponse(events=events[:MAX_HISTORY_EVENTS])

import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, require_tutor_of_device
from app.core.config import settings
from app.core.crypto import decrypt_coordinate, encrypt_coordinate
from app.db.session import get_db
from app.models import Device, Geofence, GeofenceEvent, User
from app.schemas.geofence import (
    MAX_GEOFENCE_EVENTS,
    DeleteGeofenceResponse,
    GeofenceEventListResponse,
    GeofenceEventResponse,
    GeofenceListResponse,
    GeofenceResponse,
    UpsertGeofenceRequest,
)
from app.services.audit import record_audit_event

router = APIRouter(tags=["geofences"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _to_response(geofence: Geofence) -> GeofenceResponse:
    return GeofenceResponse(
        id=geofence.id,
        name=geofence.name,
        latitude=decrypt_coordinate(geofence.latitude_ciphertext),
        longitude=decrypt_coordinate(geofence.longitude_ciphertext),
        radius_meters=geofence.radius_meters,
        created_at=geofence.created_at,
        updated_at=geofence.updated_at,
    )


@router.post("/devices/{device_id}/geofences", response_model=GeofenceResponse)
async def create_geofence(
    device_id: uuid.UUID,
    payload: UpsertGeofenceRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> GeofenceResponse:
    """Always creates a new row — unlike AppRule, a geofence has no natural per-device unique key
    to upsert against (a tutor may legitimately want two zones with the same name someday), so
    editing is a separate PUT by id instead.
    """
    existing_count = (
        await db.execute(
            select(func.count()).select_from(Geofence).where(Geofence.device_id == device_id)
        )
    ).scalar_one()
    if existing_count >= settings.max_geofences_per_device:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="max_geofences_per_device_reached",
        )

    geofence = Geofence(
        device_id=device_id,
        name=payload.name,
        latitude_ciphertext=encrypt_coordinate(payload.latitude),
        longitude_ciphertext=encrypt_coordinate(payload.longitude),
        radius_meters=payload.radius_meters,
    )
    db.add(geofence)
    await db.flush()
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="GEOFENCE_CREATED",
        resource_type="geofence",
        resource_id=str(geofence.id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(geofence)

    return _to_response(geofence)


@router.get("/devices/{device_id}/geofences", response_model=GeofenceListResponse)
async def list_geofences(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> GeofenceListResponse:
    geofences = (
        await db.execute(
            select(Geofence).where(Geofence.device_id == device_id).order_by(Geofence.created_at)
        )
    ).scalars().all()

    return GeofenceListResponse(geofences=[_to_response(geofence) for geofence in geofences])


@router.put("/devices/{device_id}/geofences/{geofence_id}", response_model=GeofenceResponse)
async def update_geofence(
    device_id: uuid.UUID,
    geofence_id: uuid.UUID,
    payload: UpsertGeofenceRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> GeofenceResponse:
    geofence = (
        await db.execute(
            select(Geofence).where(Geofence.id == geofence_id, Geofence.device_id == device_id)
        )
    ).scalar_one_or_none()
    if geofence is None:
        # Same 404-for-both reasoning as require_tutor_of_device: a geofence belonging to
        # another device must look identical to one that doesn't exist.
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="geofence_not_found")

    geofence.name = payload.name
    geofence.latitude_ciphertext = encrypt_coordinate(payload.latitude)
    geofence.longitude_ciphertext = encrypt_coordinate(payload.longitude)
    geofence.radius_meters = payload.radius_meters
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="GEOFENCE_UPDATED",
        resource_type="geofence",
        resource_id=str(geofence.id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(geofence)

    return _to_response(geofence)


@router.delete(
    "/devices/{device_id}/geofences/{geofence_id}", response_model=DeleteGeofenceResponse
)
async def delete_geofence(
    device_id: uuid.UUID,
    geofence_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> DeleteGeofenceResponse:
    geofence = (
        await db.execute(
            select(Geofence).where(Geofence.id == geofence_id, Geofence.device_id == device_id)
        )
    ).scalar_one_or_none()
    if geofence is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="geofence_not_found")

    now = datetime.now(UTC)
    name = geofence.name
    # Hard delete, same as AppRule: GeofenceEvent keeps its own geofence_name snapshot (not a
    # foreign key), so past ENTER/EXIT history reads correctly forever regardless of this.
    await db.delete(geofence)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="GEOFENCE_DELETED",
        resource_type="geofence",
        resource_id=str(geofence_id),
        ip_address=_client_ip(request),
    )
    await db.commit()

    return DeleteGeofenceResponse(geofence_id=geofence_id, name=name, deleted_at=now)


@router.get("/devices/{device_id}/geofences/events", response_model=GeofenceEventListResponse)
async def list_geofence_events(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> GeofenceEventListResponse:
    events = (
        await db.execute(
            select(GeofenceEvent)
            .where(GeofenceEvent.device_id == device_id)
            .order_by(GeofenceEvent.occurred_at.desc())
            .limit(MAX_GEOFENCE_EVENTS)
        )
    ).scalars().all()

    return GeofenceEventListResponse(
        events=[
            GeofenceEventResponse(
                id=event.id,
                geofence_id=event.geofence_id,
                geofence_name=event.geofence_name,
                event_type=event.event_type,
                occurred_at=event.occurred_at,
                received_at=event.received_at,
            )
            for event in events
        ]
    )

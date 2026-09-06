import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.api.deps import (
    get_current_user,
    require_role,
    require_supervised_owner_of_device,
    require_tutor_of_device,
)
from app.db.session import get_db
from app.models import Device, DeviceStatus, TutorDevice, User
from app.models.device import ONLINE, UNLINKED
from app.models.role import SUPERVISADO, TUTOR
from app.schemas.device import (
    DeviceListResponse,
    DeviceResponse,
    DeviceStatusResponse,
    HeartbeatRequest,
    HeartbeatResponse,
    MyDeviceResponse,
    RenameDeviceRequest,
    SupervisingTutorResponse,
)
from app.services.audit import record_audit_event
from app.services.device_status import compute_effective_status

router = APIRouter(tags=["devices"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _to_response(device: Device, linked_at: datetime) -> DeviceResponse:
    status_row = device.status
    stored_status = status_row.status if status_row else UNLINKED
    last_seen_at = status_row.last_seen_at if status_row else None

    return DeviceResponse(
        id=device.id,
        name=device.name,
        platform=device.platform,
        os_version=device.os_version,
        app_version=device.app_version,
        linked_at=linked_at,
        status=DeviceStatusResponse(
            status=compute_effective_status(stored_status, last_seen_at),
            last_seen_at=last_seen_at,
            last_sync_at=status_row.last_sync_at if status_row else None,
        ),
        default_app_policy=device.default_app_policy,
    )


async def _load_response_for_tutor(
    db: AsyncSession, device_id: uuid.UUID, tutor_user_id: uuid.UUID
) -> DeviceResponse:
    link = (
        await db.execute(
            select(TutorDevice).where(
                TutorDevice.device_id == device_id,
                TutorDevice.tutor_user_id == tutor_user_id,
                TutorDevice.unlinked_at.is_(None),
            )
        )
    ).scalar_one()
    device = (
        await db.execute(
            select(Device).where(Device.id == device_id).options(selectinload(Device.status))
        )
    ).scalar_one()
    return _to_response(device, link.linked_at)


@router.get("/devices", response_model=DeviceListResponse)
async def list_my_devices(
    current_user: User = Depends(require_role(TUTOR)),
    db: AsyncSession = Depends(get_db),
) -> DeviceListResponse:
    result = await db.execute(
        select(TutorDevice)
        .where(
            TutorDevice.tutor_user_id == current_user.id, TutorDevice.unlinked_at.is_(None)
        )
        .options(selectinload(TutorDevice.device).selectinload(Device.status))
        .order_by(TutorDevice.linked_at.desc())
    )
    links = result.scalars().all()
    return DeviceListResponse(
        devices=[_to_response(link.device, link.linked_at) for link in links]
    )


@router.get("/devices/me", response_model=MyDeviceResponse)
async def get_my_own_device(
    current_user: User = Depends(require_role(SUPERVISADO)),
    db: AsyncSession = Depends(get_db),
) -> MyDeviceResponse:
    """What this supervised install can find out about itself: is it linked, and to whom.

    Registered before /devices/{device_id} on purpose — FastAPI matches routes in
    registration order, and a path parameter would otherwise swallow the literal "me".
    """
    device = (
        await db.execute(
            select(Device)
            .where(Device.supervised_user_id == current_user.id)
            .options(selectinload(Device.status))
        )
    ).scalar_one_or_none()

    if device is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="not_linked")

    tutor_links = (
        await db.execute(
            select(TutorDevice)
            .where(TutorDevice.device_id == device.id, TutorDevice.unlinked_at.is_(None))
            .options(selectinload(TutorDevice.tutor))
        )
    ).scalars().all()

    status_row = device.status
    return MyDeviceResponse(
        device_id=device.id,
        device_name=device.name,
        status=DeviceStatusResponse(
            status=compute_effective_status(
                status_row.status if status_row else UNLINKED,
                status_row.last_seen_at if status_row else None,
            ),
            last_seen_at=status_row.last_seen_at if status_row else None,
            last_sync_at=status_row.last_sync_at if status_row else None,
        ),
        tutors=[
            SupervisingTutorResponse(display_name=link.tutor.display_name, email=link.tutor.email)
            for link in tutor_links
        ],
    )


@router.get("/devices/{device_id}", response_model=DeviceResponse)
async def get_device(
    device_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _authorized: Device = Depends(require_tutor_of_device),
) -> DeviceResponse:
    return await _load_response_for_tutor(db, device_id, current_user.id)


@router.patch("/devices/{device_id}", response_model=DeviceResponse)
async def rename_device(
    device_id: uuid.UUID,
    payload: RenameDeviceRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> DeviceResponse:
    device.name = payload.name
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="DEVICE_RENAMED",
        resource_type="device",
        resource_id=str(device.id),
        ip_address=_client_ip(request),
    )
    await db.commit()

    return await _load_response_for_tutor(db, device_id, current_user.id)


@router.post("/devices/{device_id}/heartbeat", response_model=HeartbeatResponse)
async def send_heartbeat(
    device_id: uuid.UUID,
    payload: HeartbeatRequest,
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_supervised_owner_of_device),
) -> HeartbeatResponse:
    """No audit entry here on purpose: this fires every few minutes for as long as the app
    is open, and the audit trail is for actions someone would want to review, not a liveness
    ping. last_seen_at itself is that history.
    """
    now = datetime.now(UTC)

    has_active_tutor = (
        await db.execute(
            select(TutorDevice.id).where(
                TutorDevice.device_id == device_id, TutorDevice.unlinked_at.is_(None)
            )
        )
    ).first() is not None

    status_row = await db.get(DeviceStatus, device_id)
    if status_row is None:
        status_row = DeviceStatus(device_id=device_id)
        db.add(status_row)

    status_row.last_seen_at = now
    status_row.status = ONLINE if has_active_tutor else UNLINKED

    if payload.app_version:
        device.app_version = payload.app_version
    if payload.os_version:
        device.os_version = payload.os_version

    await db.commit()

    return HeartbeatResponse(status=status_row.status, last_seen_at=now)

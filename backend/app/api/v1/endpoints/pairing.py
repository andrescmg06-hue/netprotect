import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, require_role, require_tutor_of_device
from app.cache.redis_client import RateLimitBackendError, hit_rate_limit
from app.core.config import settings
from app.core.security import (
    generate_pairing_code,
    hash_pairing_code,
    pairing_code_expiry,
)
from app.db.session import get_db
from app.models import Device, DeviceStatus, PairingCode, TutorDevice, User
from app.models.device import ONLINE, UNLINKED
from app.models.role import SUPERVISADO, TUTOR
from app.schemas.pairing import (
    LinkedTutorResponse,
    PairingCodeResponse,
    RedeemPairingCodeRequest,
    RedeemPairingCodeResponse,
    RevokePairingCodeResponse,
    UnlinkDeviceResponse,
)
from app.services.audit import record_audit_event

router = APIRouter(tags=["pairing"])

_MAX_CODE_GENERATION_ATTEMPTS = 5


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"


async def _enforce_rate_limit(key: str, *, limit: int, window_seconds: int) -> None:
    """Counts the attempt and rejects with 429 once over the limit.

    Fails closed on a Redis outage (503 rather than letting the attempt through): brute-force
    protection that silently disappears when the cache is down is not protection.
    """
    try:
        result = await hit_rate_limit(key, limit=limit, window_seconds=window_seconds)
    except RateLimitBackendError as exc:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, detail="rate_limit_unavailable"
        ) from exc

    if not result.allowed:
        raise HTTPException(
            status.HTTP_429_TOO_MANY_REQUESTS,
            detail="too_many_attempts",
            headers={"Retry-After": str(result.retry_after_seconds)},
        )


def _invalid_code() -> HTTPException:
    """One response for every failure mode.

    Unknown code, expired code, already-used code and revoked code all answer exactly the
    same, so someone guessing digits cannot learn that a guess hit a real code.
    """
    return HTTPException(status.HTTP_401_UNAUTHORIZED, detail="invalid_or_expired_code")


def _active_code_clause(code_hash: str):
    return (
        PairingCode.code_hash == code_hash,
        PairingCode.used_at.is_(None),
        PairingCode.revoked_at.is_(None),
        PairingCode.expires_at > datetime.now(UTC),
    )


@router.post("/pairing/codes", response_model=PairingCodeResponse)
async def generate_pairing_code_endpoint(
    request: Request,
    current_user: User = Depends(require_role(TUTOR)),
    db: AsyncSession = Depends(get_db),
) -> PairingCodeResponse:
    await _enforce_rate_limit(
        f"ratelimit:pairing:generate:user:{current_user.id}",
        limit=settings.pairing_generate_max_per_tutor,
        window_seconds=settings.pairing_generate_window_seconds,
    )

    now = datetime.now(UTC)

    # One live code per tutor: issuing a new one retires the previous, so a code read over
    # someone's shoulder stops working the moment the tutor asks for another.
    previous = await db.execute(
        select(PairingCode).where(
            PairingCode.tutor_user_id == current_user.id,
            PairingCode.used_at.is_(None),
            PairingCode.revoked_at.is_(None),
            PairingCode.expires_at > now,
        )
    )
    for stale in previous.scalars().all():
        stale.revoked_at = now

    code: str | None = None
    code_hash: str | None = None
    for _ in range(_MAX_CODE_GENERATION_ATTEMPTS):
        candidate = generate_pairing_code()
        candidate_hash = hash_pairing_code(candidate)
        clash = await db.execute(select(PairingCode.id).where(*_active_code_clause(candidate_hash)))
        if clash.first() is None:
            code, code_hash = candidate, candidate_hash
            break

    if code is None or code_hash is None:
        # Every attempt collided with another tutor's live code. Handing out a duplicate
        # would make redemption ambiguous, so refuse rather than guess.
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, detail="could_not_allocate_code"
        )

    expires_at = pairing_code_expiry()
    db.add(
        PairingCode(
            tutor_user_id=current_user.id,
            code_hash=code_hash,
            expires_at=expires_at,
        )
    )
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="PAIRING_CODE_GENERATED",
        resource_type="pairing_code",
        ip_address=_client_ip(request),
    )
    await db.commit()

    return PairingCodeResponse(
        code=code,
        expires_at=expires_at,
        expires_in_seconds=settings.pairing_code_ttl_seconds,
    )


@router.delete("/pairing/codes/current", response_model=RevokePairingCodeResponse)
async def revoke_current_pairing_code(
    request: Request,
    current_user: User = Depends(require_role(TUTOR)),
    db: AsyncSession = Depends(get_db),
) -> RevokePairingCodeResponse:
    now = datetime.now(UTC)
    result = await db.execute(
        select(PairingCode).where(
            PairingCode.tutor_user_id == current_user.id,
            PairingCode.used_at.is_(None),
            PairingCode.revoked_at.is_(None),
            PairingCode.expires_at > now,
        )
    )
    active = result.scalars().all()
    if not active:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="no_active_code")

    for code_row in active:
        code_row.revoked_at = now

    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="PAIRING_CODE_REVOKED",
        resource_type="pairing_code",
        ip_address=_client_ip(request),
    )
    await db.commit()

    return RevokePairingCodeResponse(revoked_at=now)


@router.post("/pairing/redeem", response_model=RedeemPairingCodeResponse)
async def redeem_pairing_code(
    payload: RedeemPairingCodeRequest,
    request: Request,
    current_user: User = Depends(require_role(SUPERVISADO)),
    db: AsyncSession = Depends(get_db),
) -> RedeemPairingCodeResponse:
    client_ip = _client_ip(request)
    await _enforce_rate_limit(
        f"ratelimit:pairing:redeem:user:{current_user.id}",
        limit=settings.pairing_redeem_max_per_user,
        window_seconds=settings.pairing_redeem_window_seconds,
    )
    await _enforce_rate_limit(
        f"ratelimit:pairing:redeem:ip:{client_ip}",
        limit=settings.pairing_redeem_max_per_ip,
        window_seconds=settings.pairing_redeem_window_seconds,
    )

    if not payload.code.isdigit():
        raise _invalid_code()

    code_hash = hash_pairing_code(payload.code)

    # FOR UPDATE serialises concurrent redemptions of the same code: the second request
    # blocks here and then sees used_at already set, instead of both linking a device.
    result = await db.execute(
        select(PairingCode).where(PairingCode.code_hash == code_hash).with_for_update()
    )
    code_row = result.scalar_one_or_none()

    now = datetime.now(UTC)
    if (
        code_row is None
        or code_row.used_at is not None
        or code_row.revoked_at is not None
        or code_row.expires_at <= now
    ):
        raise _invalid_code()

    tutor = await db.get(User, code_row.tutor_user_id)
    if tutor is None or not tutor.is_active:
        raise _invalid_code()

    device = (
        await db.execute(
            select(Device).where(
                Device.supervised_user_id == current_user.id,
                Device.device_instance_id == payload.device_instance_id,
            )
        )
    ).scalar_one_or_none()

    if device is None:
        device = Device(
            name=payload.device_name,
            platform=payload.platform,
            os_version=payload.os_version,
            app_version=payload.app_version,
            device_instance_id=payload.device_instance_id,
            supervised_user_id=current_user.id,
        )
        db.add(device)
        await db.flush()
        db.add(DeviceStatus(device_id=device.id, status=ONLINE, last_seen_at=now))
    else:
        device.os_version = payload.os_version
        device.app_version = payload.app_version
        status_row = await db.get(DeviceStatus, device.id)
        if status_row is None:
            db.add(DeviceStatus(device_id=device.id, status=ONLINE, last_seen_at=now))
        else:
            status_row.status = ONLINE
            status_row.last_seen_at = now

    existing_link = (
        await db.execute(
            select(TutorDevice).where(
                TutorDevice.tutor_user_id == tutor.id,
                TutorDevice.device_id == device.id,
                TutorDevice.unlinked_at.is_(None),
            )
        )
    ).scalar_one_or_none()

    if existing_link is None:
        link = TutorDevice(tutor_user_id=tutor.id, device_id=device.id)
        db.add(link)
        await db.flush()
    else:
        # Already linked to this tutor (a second code from the same tutor, or a retry).
        # Consuming the code and reporting the existing link is the honest outcome.
        link = existing_link

    code_row.used_at = now
    code_row.device_id = device.id

    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="DEVICE_LINKED",
        resource_type="device",
        resource_id=str(device.id),
        ip_address=client_ip,
    )
    await db.commit()

    return RedeemPairingCodeResponse(
        device_id=device.id,
        device_name=device.name,
        linked_at=link.linked_at,
        tutor=LinkedTutorResponse(display_name=tutor.display_name, email=tutor.email),
    )


@router.delete("/devices/{device_id}/link", response_model=UnlinkDeviceResponse)
async def unlink_device(
    device_id: uuid.UUID,
    request: Request,
    device: Device = Depends(require_tutor_of_device),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> UnlinkDeviceResponse:
    now = datetime.now(UTC)

    link = (
        await db.execute(
            select(TutorDevice).where(
                TutorDevice.tutor_user_id == current_user.id,
                TutorDevice.device_id == device.id,
                TutorDevice.unlinked_at.is_(None),
            )
        )
    ).scalar_one_or_none()

    if link is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="device_not_found")

    link.unlinked_at = now

    remaining = (
        await db.execute(
            select(TutorDevice.id).where(
                TutorDevice.device_id == device.id,
                TutorDevice.unlinked_at.is_(None),
                TutorDevice.id != link.id,
            )
        )
    ).first()

    if remaining is None:
        status_row = await db.get(DeviceStatus, device.id)
        if status_row is not None:
            status_row.status = UNLINKED

    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="DEVICE_UNLINKED",
        resource_type="device",
        resource_id=str(device.id),
        ip_address=_client_ip(request),
    )
    await db.commit()

    return UnlinkDeviceResponse(device_id=device.id, unlinked_at=now)

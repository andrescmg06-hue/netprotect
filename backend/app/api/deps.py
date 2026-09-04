import uuid
from collections.abc import Callable, Coroutine

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.security import InvalidAccessTokenError, decode_access_token
from app.db.session import get_db
from app.models import Device, TutorDevice, User, UserRole

_bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="not_authenticated")

    try:
        user_id = decode_access_token(credentials.credentials)
    except InvalidAccessTokenError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="invalid_access_token") from exc

    user = await db.get(User, user_id)
    if user is None or not user.is_active:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="user_not_found_or_inactive")

    return user


def require_role(*allowed_role_codes: str) -> Callable[..., Coroutine[None, None, User]]:
    """Dependency factory: only lets through users holding at least one of the given roles.

    Holding a role is not, by itself, a grant of access to any specific resource — a TUTOR
    only administers the devices linked to them (see require_tutor_of_device). This just
    gates role-scoped *actions* that don't target a particular resource.
    """

    async def _check(
        current_user: User = Depends(get_current_user),
        db: AsyncSession = Depends(get_db),
    ) -> User:
        result = await db.execute(
            select(UserRole.role_code).where(UserRole.user_id == current_user.id)
        )
        held_roles = {row[0] for row in result.all()}
        if held_roles.isdisjoint(allowed_role_codes):
            raise HTTPException(status.HTTP_403_FORBIDDEN, detail="insufficient_role")
        return current_user

    return _check


async def require_tutor_of_device(
    device_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> Device:
    """Loads a device only if the caller is its active tutor.

    Returns 404 — not 403 — when the device exists but belongs to someone else, so a caller
    probing device IDs can't distinguish "not yours" from "doesn't exist" (anti-IDOR/BOLA).
    """
    result = await db.execute(
        select(Device)
        .join(TutorDevice, TutorDevice.device_id == Device.id)
        .where(
            Device.id == device_id,
            TutorDevice.tutor_user_id == current_user.id,
            TutorDevice.unlinked_at.is_(None),
        )
    )
    device = result.scalar_one_or_none()
    if device is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="device_not_found")
    return device

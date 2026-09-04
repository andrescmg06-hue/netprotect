from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models import User, UserRole
from app.schemas.role import (
    ASSIGNABLE_ROLE_CODES,
    GrantedRoleResponse,
    MyRolesResponse,
    RoleSelectionRequest,
)
from app.services.audit import record_audit_event

router = APIRouter(prefix="/users/me/roles", tags=["roles"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


@router.get("", response_model=MyRolesResponse)
async def list_my_roles(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MyRolesResponse:
    result = await db.execute(select(UserRole).where(UserRole.user_id == current_user.id))
    return MyRolesResponse(
        roles=[
            GrantedRoleResponse(role_code=row.role_code, granted_at=row.granted_at)
            for row in result.scalars().all()
        ]
    )


@router.post("", response_model=GrantedRoleResponse)
async def select_role(
    payload: RoleSelectionRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> GrantedRoleResponse:
    """The client requests a role; the backend is the one that decides to grant it.

    Today that decision is unconditional for both roles — holding TUTOR or SUPERVISADO by
    itself grants no access to any device. What matters is which devices show up in
    tutor_devices / devices.supervised_user_id once pairing (Sprint 5) exists, and
    require_tutor_of_device already enforces that per-resource check.
    """
    if payload.role_code not in ASSIGNABLE_ROLE_CODES:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="unknown_role_code")

    existing = await db.execute(
        select(UserRole).where(
            UserRole.user_id == current_user.id,
            UserRole.role_code == payload.role_code,
        )
    )
    role_row = existing.scalar_one_or_none()

    if role_row is None:
        role_row = UserRole(user_id=current_user.id, role_code=payload.role_code)
        db.add(role_row)
        await db.flush()
        await record_audit_event(
            db,
            actor_user_id=current_user.id,
            action="ROLE_GRANTED",
            resource_type="role",
            resource_id=payload.role_code,
            ip_address=_client_ip(request),
        )
        await db.commit()
        await db.refresh(role_row)

    return GrantedRoleResponse(role_code=role_row.role_code, granted_at=role_row.granted_at)

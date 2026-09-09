import csv
import io
from datetime import datetime

from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import ColumnElement

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models import AuditLog, User
from app.schemas.audit import (
    DEFAULT_AUDIT_PAGE_SIZE,
    MAX_AUDIT_EXPORT_ROWS,
    MAX_AUDIT_PAGE_SIZE,
    AuditLogListResponse,
    AuditLogResponse,
)

router = APIRouter(tags=["audit"])


def _filters(
    current_user: User,
    action: str | None,
    resource_type: str | None,
    from_date: datetime | None,
    to_date: datetime | None,
) -> list[ColumnElement[bool]]:
    """Scoped to the caller's own actions (actor_user_id == current_user.id), not to a device or
    a role. AuditLog has no device_id column — resource_id is a free-form string whose meaning
    depends on resource_type (a device UUID for "device", a role code for "role", nothing at all
    for "pairing_code") — so there's no clean way to ask "everything that happened to my
    devices" without also deciding what to do with rows like LOGIN/ROLE_GRANTED that don't name a
    device at all. "My own actions" is what the table actually models directly, mirrors the
    account-activity-log pattern of other products (GitHub, Google), and is simple to reason
    about: a caller can never see another user's actions regardless of role.

    One real gap this leaves: DEVICE_LINKED is recorded with the *supervised* user as actor
    (app/api/v1/endpoints/pairing.py), so a tutor won't see their own devices' linking events
    here. Accepted because the tutor already sees link state directly in the devices list
    (Sprint 6) — this view is for reviewing actions, not a substitute for that.
    """
    filters: list[ColumnElement[bool]] = [AuditLog.actor_user_id == current_user.id]
    if action is not None:
        filters.append(AuditLog.action == action)
    if resource_type is not None:
        filters.append(AuditLog.resource_type == resource_type)
    if from_date is not None:
        filters.append(AuditLog.created_at >= from_date)
    if to_date is not None:
        filters.append(AuditLog.created_at <= to_date)
    return filters


def _to_response(entry: AuditLog) -> AuditLogResponse:
    return AuditLogResponse(
        id=entry.id,
        action=entry.action,
        resource_type=entry.resource_type,
        resource_id=entry.resource_id,
        ip_address=entry.ip_address,
        created_at=entry.created_at,
    )


@router.get("/users/me/audit", response_model=AuditLogListResponse)
async def list_my_audit_log(
    action: str | None = Query(default=None),
    resource_type: str | None = Query(default=None),
    from_date: datetime | None = Query(default=None),
    to_date: datetime | None = Query(default=None),
    limit: int = Query(default=DEFAULT_AUDIT_PAGE_SIZE, ge=1, le=MAX_AUDIT_PAGE_SIZE),
    offset: int = Query(default=0, ge=0),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> AuditLogListResponse:
    """Real limit/offset paging, not a fixed response cap like history/alerts — see
    MAX_AUDIT_PAGE_SIZE's docstring for why: this table has no retention purge, so a fixed cap
    would permanently hide anything older once an account passes it.

    Neither this endpoint nor the export below calls record_audit_event: reading your own audit
    trail is exactly the kind of action the project already treats as unaudited (list_alerts,
    get_device_history) — the trail records actions worth reviewing later, not the review itself.
    """
    filters = _filters(current_user, action, resource_type, from_date, to_date)

    total = (
        await db.execute(select(func.count()).select_from(AuditLog).where(*filters))
    ).scalar_one()
    entries = (
        await db.execute(
            select(AuditLog)
            .where(*filters)
            .order_by(AuditLog.created_at.desc())
            .limit(limit)
            .offset(offset)
        )
    ).scalars().all()

    return AuditLogListResponse(
        logs=[_to_response(entry) for entry in entries],
        total=total,
        limit=limit,
        offset=offset,
    )


@router.get("/users/me/audit/export")
async def export_my_audit_log(
    action: str | None = Query(default=None),
    resource_type: str | None = Query(default=None),
    from_date: datetime | None = Query(default=None),
    to_date: datetime | None = Query(default=None),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> StreamingResponse:
    """CSV export of the same filtered set list_my_audit_log exposes, without paging — capped at
    MAX_AUDIT_EXPORT_ROWS instead, since there's no UI here to page through if the true count is
    larger.
    """
    filters = _filters(current_user, action, resource_type, from_date, to_date)
    entries = (
        await db.execute(
            select(AuditLog)
            .where(*filters)
            .order_by(AuditLog.created_at.desc())
            .limit(MAX_AUDIT_EXPORT_ROWS)
        )
    ).scalars().all()

    buffer = io.StringIO()
    writer = csv.writer(buffer)
    writer.writerow(["id", "action", "resource_type", "resource_id", "ip_address", "created_at"])
    for entry in entries:
        writer.writerow(
            [
                entry.id,
                entry.action,
                entry.resource_type or "",
                entry.resource_id or "",
                entry.ip_address or "",
                entry.created_at.isoformat(),
            ]
        )

    return StreamingResponse(
        iter([buffer.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": 'attachment; filename="audit-log.csv"'},
    )

import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_supervised_owner_of_device, require_tutor_of_device
from app.db.session import get_db
from app.models import Device, DeviceApplication, DeviceApplicationUsage, DeviceStatus
from app.schemas.application import (
    DeviceApplicationListResponse,
    DeviceApplicationResponse,
    LatestUsageResponse,
    SyncApplicationsRequest,
    SyncApplicationsResponse,
)

router = APIRouter(tags=["applications"])


@router.post(
    "/devices/{device_id}/applications/sync",
    response_model=SyncApplicationsResponse,
)
async def sync_applications(
    device_id: uuid.UUID,
    payload: SyncApplicationsRequest,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_supervised_owner_of_device),
) -> SyncApplicationsResponse:
    """The supervised device reports its full installed-app list plus one day's usage totals.

    The app catalog is reconciled against what's reported: anything missing gets
    uninstalled_at set (soft delete), so a tutor sees "removed" instead of the app silently
    vanishing. Usage rows are upserted per (device, package, day) — UsageStatsManager already
    returns a running total for the day, so a re-sync overwrites rather than accumulates.
    """
    now = datetime.now(UTC)

    existing_apps = (
        await db.execute(select(DeviceApplication).where(DeviceApplication.device_id == device_id))
    ).scalars().all()
    existing_apps_by_package = {row.package_name: row for row in existing_apps}

    reported_packages = {item.package_name for item in payload.installed_apps}
    apps_synced = 0
    for item in payload.installed_apps:
        row = existing_apps_by_package.get(item.package_name)
        if row is None:
            db.add(
                DeviceApplication(
                    device_id=device_id,
                    package_name=item.package_name,
                    app_label=item.app_label,
                    is_system_app=item.is_system_app,
                    first_seen_at=now,
                    last_seen_at=now,
                )
            )
        else:
            row.app_label = item.app_label
            row.is_system_app = item.is_system_app
            row.last_seen_at = now
            row.uninstalled_at = None
        apps_synced += 1

    apps_marked_uninstalled = 0
    for package_name, row in existing_apps_by_package.items():
        if package_name not in reported_packages and row.uninstalled_at is None:
            row.uninstalled_at = now
            apps_marked_uninstalled += 1

    existing_usage = (
        await db.execute(
            select(DeviceApplicationUsage).where(
                DeviceApplicationUsage.device_id == device_id,
                DeviceApplicationUsage.usage_date == payload.usage_date,
            )
        )
    ).scalars().all()
    existing_usage_by_package = {row.package_name: row for row in existing_usage}

    usage_rows_synced = 0
    for item in payload.daily_usage:
        usage_row = existing_usage_by_package.get(item.package_name)
        if usage_row is None:
            db.add(
                DeviceApplicationUsage(
                    device_id=device_id,
                    package_name=item.package_name,
                    usage_date=payload.usage_date,
                    foreground_seconds=item.foreground_seconds,
                    synced_at=now,
                )
            )
        else:
            usage_row.foreground_seconds = item.foreground_seconds
            usage_row.synced_at = now
        usage_rows_synced += 1

    status_row = await db.get(DeviceStatus, device_id)
    if status_row is not None:
        status_row.last_sync_at = now

    await db.commit()

    return SyncApplicationsResponse(
        apps_synced=apps_synced,
        apps_marked_uninstalled=apps_marked_uninstalled,
        usage_rows_synced=usage_rows_synced,
        synced_at=now,
    )


@router.get(
    "/devices/{device_id}/applications",
    response_model=DeviceApplicationListResponse,
)
async def list_device_applications(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> DeviceApplicationListResponse:
    apps = (
        await db.execute(
            select(DeviceApplication)
            .where(DeviceApplication.device_id == device_id)
            .order_by(DeviceApplication.app_label)
        )
    ).scalars().all()

    # DISTINCT ON (Postgres-specific, deliberate: this project has no cross-DB portability
    # goal) picks the latest usage_date row per package in one indexed query, instead of
    # loading the device's entire usage history to reduce it in Python.
    usage_rows = (
        await db.execute(
            select(DeviceApplicationUsage)
            .distinct(DeviceApplicationUsage.package_name)
            .where(DeviceApplicationUsage.device_id == device_id)
            .order_by(DeviceApplicationUsage.package_name, DeviceApplicationUsage.usage_date.desc())
        )
    ).scalars().all()
    latest_usage_by_package = {row.package_name: row for row in usage_rows}

    return DeviceApplicationListResponse(
        applications=[
            DeviceApplicationResponse(
                package_name=app.package_name,
                app_label=app.app_label,
                is_system_app=app.is_system_app,
                first_seen_at=app.first_seen_at,
                last_seen_at=app.last_seen_at,
                uninstalled_at=app.uninstalled_at,
                latest_usage=(
                    LatestUsageResponse(
                        usage_date=latest.usage_date,
                        foreground_seconds=latest.foreground_seconds,
                    )
                    if (latest := latest_usage_by_package.get(app.package_name))
                    else None
                ),
            )
            for app in apps
        ]
    )

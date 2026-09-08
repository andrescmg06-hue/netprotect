import uuid
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_supervised_owner_of_device, require_tutor_of_device
from app.core.config import settings
from app.core.crypto import decrypt_coordinate, encrypt_coordinate
from app.db.session import get_db
from app.models import Device, DeviceLocationReport
from app.schemas.location import (
    MAX_HISTORY_REPORTS,
    LatestLocationResponse,
    LocationHistoryResponse,
    LocationReportResponse,
    ReportLocationRequest,
)

router = APIRouter(tags=["location"])


def _to_response(report: DeviceLocationReport) -> LocationReportResponse:
    return LocationReportResponse(
        id=report.id,
        latitude=decrypt_coordinate(report.latitude_ciphertext),
        longitude=decrypt_coordinate(report.longitude_ciphertext),
        accuracy_meters=report.accuracy_meters,
        captured_at=report.captured_at,
        received_at=report.received_at,
    )


@router.post("/devices/{device_id}/location", response_model=LocationReportResponse)
async def report_location(
    device_id: uuid.UUID,
    payload: ReportLocationRequest,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_supervised_owner_of_device),
) -> LocationReportResponse:
    """Insert-only, same reasoning as the Sprint 8 rule-events report: this is the device's own
    telemetry, not a tutor action, so it isn't audited (see AppRuleEvent).

    Retention is enforced here, not by a scheduled job (this project has none yet): every report
    first deletes this device's own rows older than location_retention_days, then inserts the
    new one. Scoped to this device_id only, never a global sweep — a supervised device reporting
    its own location must not be able to trigger cleanup work for anyone else's rows.
    """
    cutoff = datetime.now(UTC) - timedelta(days=settings.location_retention_days)
    await db.execute(
        delete(DeviceLocationReport).where(
            DeviceLocationReport.device_id == device_id,
            DeviceLocationReport.captured_at < cutoff,
        )
    )

    report = DeviceLocationReport(
        device_id=device_id,
        latitude_ciphertext=encrypt_coordinate(payload.latitude),
        longitude_ciphertext=encrypt_coordinate(payload.longitude),
        accuracy_meters=payload.accuracy_meters,
        captured_at=payload.captured_at,
    )
    db.add(report)
    await db.commit()
    await db.refresh(report)

    return _to_response(report)


@router.get("/devices/{device_id}/location/latest", response_model=LatestLocationResponse)
async def get_latest_location(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> LatestLocationResponse:
    report = (
        await db.execute(
            select(DeviceLocationReport)
            .where(DeviceLocationReport.device_id == device_id)
            .order_by(DeviceLocationReport.captured_at.desc())
            .limit(1)
        )
    ).scalar_one_or_none()

    return LatestLocationResponse(report=_to_response(report) if report is not None else None)


@router.get("/devices/{device_id}/location/history", response_model=LocationHistoryResponse)
async def get_location_history(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> LocationHistoryResponse:
    """Everything still inside the retention window for this device, most recent first. No
    separate "expired" filter needed here beyond the window itself — the write endpoint already
    purges anything older than location_retention_days, so nothing stale can be sitting in the
    table to read back.
    """
    reports = (
        await db.execute(
            select(DeviceLocationReport)
            .where(DeviceLocationReport.device_id == device_id)
            .order_by(DeviceLocationReport.captured_at.desc())
            .limit(MAX_HISTORY_REPORTS)
        )
    ).scalars().all()

    return LocationHistoryResponse(reports=[_to_response(report) for report in reports])

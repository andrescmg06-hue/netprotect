import uuid
from datetime import datetime

from pydantic import BaseModel, Field

# A device reporting every 15 minutes (the frequency chosen in docs/sprint-13.md) produces at
# most 4/hour; this just bounds a single history response, it doesn't drive the retention window
# (that's settings.location_retention_days, enforced by the write endpoint itself).
MAX_HISTORY_REPORTS = 1000


class ReportLocationRequest(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    # Meters, as reported by Android's Location#getAccuracy(). Not an upper bound in practice
    # (a network-based fix can legitimately be several kilometers off), only rejecting the
    # nonsensical negative case.
    accuracy_meters: float = Field(ge=0)
    captured_at: datetime


class LocationReportResponse(BaseModel):
    id: uuid.UUID
    latitude: float
    longitude: float
    accuracy_meters: float
    captured_at: datetime
    received_at: datetime


class LatestLocationResponse(BaseModel):
    """None when the device has never reported a location, or its only reports have already
    aged out of the retention window — the two look the same to a tutor, and both are correctly
    described as "no ubicación reciente disponible" rather than an error.
    """

    report: LocationReportResponse | None


class LocationHistoryResponse(BaseModel):
    reports: list[LocationReportResponse]

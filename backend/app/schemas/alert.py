import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

AlertLevel = Literal["INFO", "WARNING", "HIGH", "CRITICAL"]
AlertType = Literal["APP_BLOCKED", "APP_LIMIT_REACHED", "GEOFENCE_ENTER", "GEOFENCE_EXIT"]

# Response-size cap, same reasoning as MAX_HISTORY_EVENTS/MAX_GEOFENCE_EVENTS.
MAX_ALERTS = 200


class AlertResponse(BaseModel):
    id: uuid.UUID
    level: AlertLevel
    alert_type: AlertType
    dedup_key: str
    package_name: str | None
    geofence_id: uuid.UUID | None
    geofence_name: str | None
    occurrence_count: int
    first_occurred_at: datetime
    last_occurred_at: datetime
    read_at: datetime | None


class AlertListResponse(BaseModel):
    alerts: list[AlertResponse]


class SilenceAlertRequest(BaseModel):
    # None = indefinite, until the tutor removes the silence explicitly.
    days: int | None = Field(default=None, gt=0)


class AlertSilenceResponse(BaseModel):
    id: uuid.UUID
    dedup_key: str
    silenced_until: datetime | None


class AlertSilenceListResponse(BaseModel):
    silences: list[AlertSilenceResponse]

import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

GeofenceEventType = Literal["ENTER", "EXIT"]

# Bounds a single events-history response, same reasoning as MAX_HISTORY_REPORTS
# (backend/app/schemas/location.py) — not a retention policy, just a response-size sanity cap.
MAX_GEOFENCE_EVENTS = 500


class UpsertGeofenceRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    # No enforced minimum beyond >0: a tutor may reasonably want a small radius and accept the
    # trade-off, and this project doesn't get to impose an arbitrary floor without evidence (the
    # real accuracy limitation of ACCESS_COARSE_LOCATION is documented in docs/sprint-14.md
    # instead of silently encoded as a validation rule). The upper bound only rejects
    # nonsensical values (a "zone" wider than a small country).
    radius_meters: float = Field(gt=0, le=100_000)


class GeofenceResponse(BaseModel):
    id: uuid.UUID
    name: str
    latitude: float
    longitude: float
    radius_meters: float
    created_at: datetime
    updated_at: datetime


class GeofenceListResponse(BaseModel):
    geofences: list[GeofenceResponse]


class DeleteGeofenceResponse(BaseModel):
    geofence_id: uuid.UUID
    name: str
    deleted_at: datetime


class GeofenceEventResponse(BaseModel):
    id: uuid.UUID
    geofence_id: uuid.UUID
    geofence_name: str
    event_type: GeofenceEventType
    occurred_at: datetime
    received_at: datetime


class GeofenceEventListResponse(BaseModel):
    events: list[GeofenceEventResponse]

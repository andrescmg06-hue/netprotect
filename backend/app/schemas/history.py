import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel

from app.schemas.geofence import GeofenceEventType
from app.schemas.rule import AppliedRuleType

HistoryEventType = Literal["APP_RULE", "GEOFENCE"]

# Same sanity-cap reasoning as MAX_HISTORY_REPORTS / MAX_GEOFENCE_EVENTS: bounds a single
# response, not a retention policy (that's app_rule_event_retention_days /
# geofence_event_retention_days, enforced by each source endpoint's own write path).
MAX_HISTORY_EVENTS = 500


class HistoryEventResponse(BaseModel):
    """One entry in the tutor's unified timeline, merging AppRuleEvent and GeofenceEvent rows.

    Deliberately not a union of two response models: a single flat shape with type-specific
    fields left null lets the frontend/Android render one list without a type-switch on the
    envelope itself, only on which fields are populated. Raw location fixes are not merged in
    here — they already have their own dedicated view (Sprint 13's history endpoint and map),
    and interleaving up to 96 fixes/day with these discrete events would bury them in noise
    rather than help a tutor scan what happened.
    """

    id: uuid.UUID
    event_type: HistoryEventType
    occurred_at: datetime
    received_at: datetime
    # Populated only when event_type == "APP_RULE".
    package_name: str | None = None
    rule_type_applied: AppliedRuleType | None = None
    # Populated only when event_type == "GEOFENCE".
    geofence_id: uuid.UUID | None = None
    geofence_name: str | None = None
    geofence_event_type: GeofenceEventType | None = None


class DeviceHistoryResponse(BaseModel):
    events: list[HistoryEventResponse]

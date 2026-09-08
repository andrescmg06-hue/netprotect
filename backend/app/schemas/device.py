import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, model_validator


class DeviceStatusResponse(BaseModel):
    status: str
    last_seen_at: datetime | None
    last_sync_at: datetime | None


class SchoolModeResponse(BaseModel):
    enabled: bool
    start_minute: int | None
    end_minute: int | None
    days_mask: int | None


class DeviceResponse(BaseModel):
    id: uuid.UUID
    name: str
    platform: str
    os_version: str | None
    app_version: str | None
    timezone: str | None
    linked_at: datetime
    status: DeviceStatusResponse
    default_app_policy: str
    school_mode: SchoolModeResponse


class DeviceListResponse(BaseModel):
    devices: list[DeviceResponse]


class RenameDeviceRequest(BaseModel):
    name: str = Field(min_length=1, max_length=255)


class HeartbeatRequest(BaseModel):
    app_version: str | None = Field(default=None, max_length=32)
    os_version: str | None = Field(default=None, max_length=64)
    # IANA identifier (e.g. "America/Bogota"), reported by the device so the tutor can correctly
    # interpret what they see (Sprint 11) — see Device.timezone in app/models/device.py.
    timezone: str | None = Field(default=None, max_length=64)
    # Manipulation detection (Sprint 20, app/services/tamper.py). All three optional and `None`
    # by default: an older installed build simply won't send them, and "no information" must
    # never be read as "tampered". None of the three is stored raw on Device/DeviceStatus — only
    # the alert/status they produce is persisted, same as every other derived-not-stored value in
    # this project (e.g. compute_effective_status).
    usage_access_granted: bool | None = Field(default=None)
    service_active: bool | None = Field(default=None)
    device_time: datetime | None = Field(default=None)


class HeartbeatResponse(BaseModel):
    status: str
    last_seen_at: datetime


class SupervisingTutorResponse(BaseModel):
    display_name: str | None
    email: str


class UpdateSchoolModeRequest(BaseModel):
    """Same validation shape as a SCHEDULE rule (app/schemas/rule.py): minutes since local
    midnight, bit 0 = Monday. Required together when enabling; irrelevant (and ignored) when
    disabling — see Device.school_mode_enabled's CHECK constraint on the DB side.
    """

    enabled: bool
    start_minute: int | None = Field(default=None, ge=0, le=1439)
    end_minute: int | None = Field(default=None, ge=0, le=1439)
    days_mask: int | None = Field(default=None, ge=1, le=127)

    @model_validator(mode="after")
    def _require_window_when_enabled(self) -> "UpdateSchoolModeRequest":
        if self.enabled and (
            self.start_minute is None or self.end_minute is None or self.days_mask is None
        ):
            raise ValueError(
                "start_minute, end_minute and days_mask are all required when enabling school mode"
            )
        return self


class ReportTamperEventRequest(BaseModel):
    """POST /devices/{id}/tamper-events (Sprint 20) — see app/services/tamper.py for why this is
    the one manipulation signal with its own endpoint instead of a heartbeat field: it's a
    discrete event (a user tried to deactivate Device Administrator, the required first step
    before uninstalling), not a per-beat condition.
    """

    event_type: Literal["UNINSTALL_ATTEMPT"]
    occurred_at: datetime


class MyDeviceResponse(BaseModel):
    """What the supervised side sees about its own device — never anyone else's."""

    device_id: uuid.UUID
    device_name: str
    status: DeviceStatusResponse
    tutors: list[SupervisingTutorResponse]

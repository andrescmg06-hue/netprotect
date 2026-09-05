import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class DeviceStatusResponse(BaseModel):
    status: str
    last_seen_at: datetime | None
    last_sync_at: datetime | None


class DeviceResponse(BaseModel):
    id: uuid.UUID
    name: str
    platform: str
    os_version: str | None
    app_version: str | None
    linked_at: datetime
    status: DeviceStatusResponse


class DeviceListResponse(BaseModel):
    devices: list[DeviceResponse]


class RenameDeviceRequest(BaseModel):
    name: str = Field(min_length=1, max_length=255)


class HeartbeatRequest(BaseModel):
    app_version: str | None = Field(default=None, max_length=32)
    os_version: str | None = Field(default=None, max_length=64)


class HeartbeatResponse(BaseModel):
    status: str
    last_seen_at: datetime


class SupervisingTutorResponse(BaseModel):
    display_name: str | None
    email: str


class MyDeviceResponse(BaseModel):
    """What the supervised side sees about its own device — never anyone else's."""

    device_id: uuid.UUID
    device_name: str
    status: DeviceStatusResponse
    tutors: list[SupervisingTutorResponse]

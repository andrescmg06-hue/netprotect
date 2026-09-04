import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.core.security import PAIRING_CODE_LENGTH


class PairingCodeResponse(BaseModel):
    """The only time the plaintext code is ever returned. It is never stored in the clear
    and never written to the audit log."""

    code: str
    expires_at: datetime
    expires_in_seconds: int


class RedeemPairingCodeRequest(BaseModel):
    code: str = Field(min_length=PAIRING_CODE_LENGTH, max_length=PAIRING_CODE_LENGTH)
    device_instance_id: str = Field(min_length=8, max_length=64)
    device_name: str = Field(min_length=1, max_length=255)
    platform: str = Field(default="ANDROID", max_length=32)
    os_version: str | None = Field(default=None, max_length=64)
    app_version: str | None = Field(default=None, max_length=32)


class LinkedTutorResponse(BaseModel):
    """Returned to the supervised device on success: whoever is now supervising this phone
    has a name and an address, and the person carrying it is entitled to see them."""

    display_name: str | None
    email: str


class RedeemPairingCodeResponse(BaseModel):
    device_id: uuid.UUID
    device_name: str
    linked_at: datetime
    tutor: LinkedTutorResponse


class UnlinkDeviceResponse(BaseModel):
    device_id: uuid.UUID
    unlinked_at: datetime


class RevokePairingCodeResponse(BaseModel):
    revoked_at: datetime

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class RegisterPushTokenRequest(BaseModel):
    fcm_token: str = Field(min_length=1, max_length=255)


class RegisterPushTokenResponse(BaseModel):
    device_id: uuid.UUID
    updated_at: datetime

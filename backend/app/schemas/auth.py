import uuid

from pydantic import BaseModel


class GoogleLoginRequest(BaseModel):
    id_token: str


class RefreshRequest(BaseModel):
    refresh_token: str


class LogoutRequest(BaseModel):
    refresh_token: str


class TokenPairResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"  # noqa: S105 -- the OAuth2 token_type field, not a secret
    expires_in: int


class CurrentUserResponse(BaseModel):
    id: uuid.UUID
    email: str
    display_name: str | None
    avatar_url: str | None

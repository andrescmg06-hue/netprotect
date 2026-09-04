from datetime import datetime

from pydantic import BaseModel

from app.models.role import SUPERVISADO, TUTOR

ASSIGNABLE_ROLE_CODES = (TUTOR, SUPERVISADO)


class RoleSelectionRequest(BaseModel):
    role_code: str


class GrantedRoleResponse(BaseModel):
    role_code: str
    granted_at: datetime


class MyRolesResponse(BaseModel):
    roles: list[GrantedRoleResponse]

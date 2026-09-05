from fastapi import APIRouter

from app.api.v1.endpoints.auth import router as auth_router
from app.api.v1.endpoints.devices import router as devices_router
from app.api.v1.endpoints.health import router as health_router
from app.api.v1.endpoints.pairing import router as pairing_router
from app.api.v1.endpoints.roles import router as roles_router

api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(auth_router)
api_router.include_router(roles_router)
api_router.include_router(pairing_router)
api_router.include_router(devices_router)

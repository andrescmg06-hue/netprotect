from fastapi import APIRouter

from app.api.v1.endpoints.alerts import router as alerts_router
from app.api.v1.endpoints.applications import router as applications_router
from app.api.v1.endpoints.audit import router as audit_router
from app.api.v1.endpoints.auth import router as auth_router
from app.api.v1.endpoints.categories import router as categories_router
from app.api.v1.endpoints.devices import router as devices_router
from app.api.v1.endpoints.geofences import router as geofences_router
from app.api.v1.endpoints.health import router as health_router
from app.api.v1.endpoints.history import router as history_router
from app.api.v1.endpoints.location import router as location_router
from app.api.v1.endpoints.pairing import router as pairing_router
from app.api.v1.endpoints.realtime import router as realtime_router
from app.api.v1.endpoints.roles import router as roles_router
from app.api.v1.endpoints.rules import router as rules_router
from app.api.v1.endpoints.statistics import router as statistics_router

api_router = APIRouter()
api_router.include_router(alerts_router)
api_router.include_router(audit_router)
api_router.include_router(health_router)
api_router.include_router(auth_router)
api_router.include_router(roles_router)
api_router.include_router(pairing_router)
api_router.include_router(devices_router)
api_router.include_router(applications_router)
api_router.include_router(rules_router)
api_router.include_router(categories_router)
api_router.include_router(location_router)
api_router.include_router(geofences_router)
api_router.include_router(history_router)
api_router.include_router(statistics_router)
api_router.include_router(realtime_router)

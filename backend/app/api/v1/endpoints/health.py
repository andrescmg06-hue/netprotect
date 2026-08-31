from fastapi import APIRouter, HTTPException, status
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.cache.redis_client import RedisHealthError, ping_redis
from app.core.config import settings
from app.db.session import get_engine
from app.schemas.health import (
    DatabaseHealthResponse,
    HealthResponse,
    ReadinessResponse,
    RedisHealthResponse,
)

router = APIRouter(prefix="/health", tags=["health"])


@router.get("", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service=settings.app_name,
        environment=settings.app_env,
        version=settings.app_version,
    )


async def _check_database() -> None:
    try:
        async with get_engine().connect() as connection:
            await connection.execute(text("SELECT 1"))
    except SQLAlchemyError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="database_unavailable",
        ) from exc


async def _check_redis() -> None:
    try:
        await ping_redis()
    except RedisHealthError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="redis_unavailable",
        ) from exc


@router.get("/db", response_model=DatabaseHealthResponse)
async def database_health() -> DatabaseHealthResponse:
    await _check_database()
    return DatabaseHealthResponse(status="ok", database="connected")


@router.get("/redis", response_model=RedisHealthResponse)
async def redis_health() -> RedisHealthResponse:
    await _check_redis()
    return RedisHealthResponse(status="ok", redis="connected")


@router.get("/ready", response_model=ReadinessResponse)
async def readiness() -> ReadinessResponse:
    await _check_database()
    await _check_redis()
    return ReadinessResponse(
        status="ready",
        backend="connected",
        database="connected",
        redis="connected",
    )

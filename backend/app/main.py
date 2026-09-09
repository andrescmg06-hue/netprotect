import logging
from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.responses import JSONResponse

from app.api.v1.router import api_router
from app.cache.redis_client import RateLimitBackendError, close_redis, hit_rate_limit
from app.core.config import settings
from app.core.rate_limit import client_ip
from app.db.session import dispose_engine

logger = logging.getLogger(__name__)

_HEALTH_PATH_PREFIX = f"{settings.api_v1_prefix}/health"


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    await close_redis()
    await dispose_engine()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
    docs_url=None if settings.app_env == "production" else "/docs",
    redoc_url=None if settings.app_env == "production" else "/redoc",
    openapi_url=None if settings.app_env == "production" else "/openapi.json",
)

app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=settings.allowed_hosts_list,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
)


@app.middleware("http")
async def enforcement_middleware(request: Request, call_next):
    """Blanket per-IP rate limiting plus HTTPS enforcement, applied to every route.

    Registered before request_id_middleware, which makes request_id_middleware the outer layer
    at runtime — so an early rejection here still comes back through it and gets X-Request-ID
    and the rest of the security headers attached, same as any other response.
    """
    if settings.app_env == "production" and request.url.scheme != "https":
        # uvicorn runs with --proxy-headers, so request.url.scheme already reflects a trusted
        # X-Forwarded-Proto. This is enforcement, not termination: the real TLS certificate and
        # reverse proxy are still Paso 25's job (docs/planning/plan-desarrollo.md) — this repo
        # has no production domain to provision one for yet.
        return JSONResponse(
            {"detail": "https_required"}, status_code=status.HTTP_400_BAD_REQUEST
        )

    if not request.url.path.startswith(_HEALTH_PATH_PREFIX):
        ip = client_ip(request)
        try:
            result = await hit_rate_limit(
                f"ratelimit:global:ip:{ip}",
                limit=settings.rate_limit_global_max_per_ip,
                window_seconds=settings.rate_limit_global_window_seconds,
            )
        except RateLimitBackendError:
            # Fails OPEN, unlike enforce_rate_limit() in app/core/rate_limit.py: this is a
            # blanket layer across the whole API, and an outage in the rate-limit backend must
            # not take the entire API down with it. The auth/pairing limiters fail closed
            # instead, because each of those guards one specific brute-force target where
            # availability is not the overriding concern.
            logger.warning("global_rate_limit_backend_unavailable")
        else:
            if not result.allowed:
                return JSONResponse(
                    {"detail": "too_many_requests"},
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    headers={"Retry-After": str(result.retry_after_seconds)},
                )

    return await call_next(request)


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = str(uuid4())
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Permissions-Policy"] = "geolocation=(), camera=(), microphone=()"
    # Sprint 21, found by an OWASP ZAP baseline scan against this same app (docs/sprint-21-
    # evidence.md): every response — including ones carrying tokens or a minor's device data —
    # was cacheable/storable by default, and had no Cross-Origin-Resource-Policy at all. This is
    # a JSON API with allow_credentials=False; nothing it returns should ever sit in a shared
    # cache or be readable cross-origin as a raw resource.
    response.headers["Cache-Control"] = "no-store"
    response.headers["Cross-Origin-Resource-Policy"] = "same-origin"
    if settings.app_env == "production":
        # HSTS only means anything once a real TLS terminator sits in front (Paso 25), but
        # sending it now costs nothing and is what a browser needs the first time it does see
        # this origin over HTTPS.
        response.headers["Strict-Transport-Security"] = "max-age=63072000; includeSubDomains"
        # default-src 'none' only in production: /docs and /redoc stay enabled in dev/test
        # (see docs_url above) and load their JS/CSS from a CDN, which this would break.
        response.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none'"
    return response


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Logs the real error server-side, returns a generic body that leaks nothing about it.

    FastAPI dispatches HTTPException to its own, more specific handler first — this one only
    ever sees exceptions nobody expected, which is exactly the case where a stack trace or a
    driver's error message must never reach the client (OWASP API Top 10, A08: Security
    Misconfiguration).
    """
    request_id = getattr(request.state, "request_id", None)
    logger.error("unhandled_exception request_id=%s", request_id, exc_info=exc)
    return JSONResponse(
        {"detail": "internal_error", "request_id": request_id},
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
    )


@app.get("/", include_in_schema=False)
async def root() -> dict[str, str]:
    return {"service": settings.app_name, "status": "ok"}


app.include_router(api_router, prefix=settings.api_v1_prefix)

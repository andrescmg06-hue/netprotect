from fastapi import HTTPException, Request, status

from app.cache.redis_client import RateLimitBackendError, hit_rate_limit


def client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"


async def enforce_rate_limit(key: str, *, limit: int, window_seconds: int) -> None:
    """Counts the attempt and rejects with 429 once over the limit.

    Fails closed on a Redis outage (503 rather than letting the attempt through): brute-force
    protection that silently disappears when the cache is down is not protection. Shared by
    every endpoint whose rate limit guards a high-value target (pairing codes, login, token
    refresh) — contrast with the global per-IP limiter in app/main.py, which fails open, because
    an outage there would otherwise take down the entire API rather than one specific flow.
    """
    try:
        result = await hit_rate_limit(key, limit=limit, window_seconds=window_seconds)
    except RateLimitBackendError as exc:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE, detail="rate_limit_unavailable"
        ) from exc

    if not result.allowed:
        raise HTTPException(
            status.HTTP_429_TOO_MANY_REQUESTS,
            detail="too_many_attempts",
            headers={"Retry-After": str(result.retry_after_seconds)},
        )

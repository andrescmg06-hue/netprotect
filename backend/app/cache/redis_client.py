from dataclasses import dataclass

import redis.asyncio as redis
from redis.exceptions import RedisError

from app.core.config import settings


class RedisHealthError(RuntimeError):
    """Raised when the configured Redis service cannot answer a health probe."""


class RateLimitBackendError(RuntimeError):
    """Raised when a rate-limit counter cannot be read or written."""


_client: redis.Redis | None = None

# INCR followed by EXPIRE only on the first hit, in one round trip. Doing it as two
# separate commands leaves a window where a counter exists with no TTL — if the process
# dies in between, that key never expires and the caller stays locked out forever.
_INCREMENT_WITH_TTL = """
local current = redis.call('INCR', KEYS[1])
if current == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return {current, redis.call('TTL', KEYS[1])}
"""


@dataclass(frozen=True)
class RateLimitResult:
    allowed: bool
    current: int
    limit: int
    retry_after_seconds: int


def get_redis() -> redis.Redis:
    global _client
    if _client is None:
        _client = redis.from_url(
            settings.redis_url,
            decode_responses=True,
            socket_connect_timeout=3,
            socket_timeout=3,
            health_check_interval=30,
        )
    return _client


async def ping_redis() -> None:
    try:
        if not await get_redis().ping():
            raise RedisHealthError("redis_unavailable")
    except (RedisError, OSError) as exc:
        raise RedisHealthError("redis_unavailable") from exc


async def hit_rate_limit(key: str, *, limit: int, window_seconds: int) -> RateLimitResult:
    """Counts one attempt against `key` and reports whether it is still within `limit`.

    Fails closed: if Redis is unreachable the caller gets an error rather than an
    unlimited allowance, so an outage can't silently disable brute-force protection.

    `RuntimeError` is included alongside the obvious `RedisError`/`OSError`: a connection whose
    transport was torn down by something outside redis-py's own bookkeeping (the cross-loop
    teardown race documented on `close_redis()` below) surfaces as a bare asyncio
    `RuntimeError`, not a `RedisError`. Without this, that one failure mode would crash the
    caller instead of being reported as the same "backend unavailable" condition every other
    connection problem already is.
    """
    try:
        current, ttl = await get_redis().eval(_INCREMENT_WITH_TTL, 1, key, window_seconds)
    except (RedisError, OSError, RuntimeError) as exc:
        raise RateLimitBackendError("rate_limit_backend_unavailable") from exc

    retry_after = ttl if ttl and ttl > 0 else window_seconds
    return RateLimitResult(
        allowed=current <= limit,
        current=current,
        limit=limit,
        retry_after_seconds=retry_after,
    )


async def reset_rate_limit(key: str) -> None:
    try:
        await get_redis().delete(key)
    except (RedisError, OSError) as exc:
        raise RateLimitBackendError("rate_limit_backend_unavailable") from exc


async def close_redis() -> None:
    """Best-effort shutdown.

    The client is a process-wide singleton bound to whichever event loop first used it. In
    the server that is the only loop there is, but anywhere a second loop closes it (tests,
    scripts) the underlying socket teardown raises about a future "attached to a different
    loop". Failing to close a socket at shutdown is never actionable for the caller, so it
    is swallowed instead of propagating out of the application lifespan.
    """
    global _client
    client, _client = _client, None
    if client is None:
        return
    try:
        # Sprint 21: disconnect the pool first, INCLUDING connections currently checked out.
        # Plain aclose() alone only returns idle connections to the pool and closes those —
        # a connection still in flight (e.g. a request racing the app's own shutdown) survives
        # aclose() and would otherwise still be reachable from the next event loop, which is
        # exactly the "attached to a different loop" crash this function's docstring warns
        # about. This only started showing up under test once the global rate limiter made
        # every request touch Redis, instead of only the handful of pairing endpoints.
        await client.connection_pool.disconnect(inuse_connections=True)
        await client.aclose()
    except (RedisError, OSError, RuntimeError):
        pass

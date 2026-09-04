import os
import uuid

import pytest

from app.cache.redis_client import close_redis, hit_rate_limit, reset_rate_limit

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide Redis.",
    ),
]


@pytest.fixture
async def limit_key():
    key = f"test:ratelimit:{uuid.uuid4().hex}"
    yield key
    await reset_rate_limit(key)
    await close_redis()


async def test_attempts_are_allowed_up_to_the_limit_then_denied(limit_key) -> None:
    results = [
        await hit_rate_limit(limit_key, limit=3, window_seconds=60) for _ in range(4)
    ]

    assert [r.allowed for r in results] == [True, True, True, False]
    assert [r.current for r in results] == [1, 2, 3, 4]


async def test_the_counter_carries_an_expiry_so_a_lockout_is_temporary(limit_key) -> None:
    result = await hit_rate_limit(limit_key, limit=1, window_seconds=45)

    # The Lua script sets the TTL in the same round trip as the first INCR: a counter must
    # never be left without one, or a caller would stay locked out permanently.
    assert 0 < result.retry_after_seconds <= 45


async def test_resetting_clears_the_counter(limit_key) -> None:
    await hit_rate_limit(limit_key, limit=1, window_seconds=60)
    denied = await hit_rate_limit(limit_key, limit=1, window_seconds=60)
    assert denied.allowed is False

    await reset_rate_limit(limit_key)

    assert (await hit_rate_limit(limit_key, limit=1, window_seconds=60)).allowed is True


async def test_separate_keys_do_not_share_a_budget(limit_key) -> None:
    other_key = f"{limit_key}:other"
    try:
        await hit_rate_limit(limit_key, limit=1, window_seconds=60)
        assert (await hit_rate_limit(limit_key, limit=1, window_seconds=60)).allowed is False

        assert (await hit_rate_limit(other_key, limit=1, window_seconds=60)).allowed is True
    finally:
        await reset_rate_limit(other_key)

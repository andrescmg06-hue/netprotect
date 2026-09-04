import pytest
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from app.core.config import settings


@pytest.fixture
async def db_session():
    """A database session on an engine built inside this test's own event loop.

    The application keeps a process-wide engine, and `TestClient` runs the app on its own
    portal loop. Borrowing that same engine from a pytest-asyncio test means handing
    connections opened on one loop to another, which asyncpg refuses ("attached to a
    different loop") — reliably so inside a container, and only intermittently on a slower
    machine, which makes it exactly the kind of failure that hides until CI.

    NullPool keeps nothing open past the test, so no connection outlives the loop it was
    created on.
    """
    engine = create_async_engine(settings.database_url, poolclass=NullPool)
    factory = async_sessionmaker(bind=engine, expire_on_commit=False)
    try:
        async with factory() as session:
            yield session
    finally:
        await engine.dispose()

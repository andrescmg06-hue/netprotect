"""Sprint 25: mints a real tutor session for tools that cannot run inside the Python process and
so cannot use `unittest.mock.patch` the way pytest's `_make_account()` fixture does (see
test_realtime_integration.py) — namely Newman (a black-box HTTP runner) and Playwright's browser
process. Both need a valid access/refresh token pair to exercise anything past `/auth/google`, and
neither can make Google verify a fake ID token.

This is NOT a backend authentication bypass: it changes nothing under `app/`, adds no endpoint,
and touches no request path a real client could reach. It calls the project's own, unmocked
`create_access_token`/`hash_refresh_token`/session-persistence code directly — the exact same
functions a real `/auth/google` login would call after Google's verification succeeds — the same
way `docs/sprint-24-evidence.md` already did by hand for a one-off visual check. This script just
makes that repeatable instead of re-typing it in a shell each time.

Not part of the application: not imported by `app/`, not shipped in the Docker image users would
run in production (only copied into the `backend`/`migrate` build stages' `tests`/`scripts`
directories that CI's throwaway containers use). Prints one JSON object to stdout so a CI step (or
a human) can pipe it straight into `jq` or a Newman/Playwright environment file.

Usage (inside the backend container, against its own database):
    python scripts/seed_test_session.py
"""

import asyncio
import json
import sys
import uuid

from app.core.security import (
    create_access_token,
    generate_refresh_token,
    hash_refresh_token,
    refresh_token_expiry,
)
from app.db.session import get_session_factory
from app.models import User, UserRole, UserSession


async def _make_user_with_role(db, *, label: str, role_code: str) -> User:
    unique = uuid.uuid4().hex[:10]
    user = User(
        email=f"{label}-{unique}@example.com",
        google_sub=f"seed-{label}-{unique}",
        display_name=f"Sprint 25 seed ({label})",
    )
    db.add(user)
    await db.flush()
    db.add(UserRole(user_id=user.id, role_code=role_code))
    return user


async def main() -> None:
    session_factory = get_session_factory()
    async with session_factory() as db:
        tutor = await _make_user_with_role(db, label="tutor", role_code="TUTOR")
        supervised = await _make_user_with_role(db, label="supervised", role_code="SUPERVISADO")
        # A second, unrelated tutor — for the API collection's anti-IDOR case (a tutor who never
        # linked this device must get 404, not the device's data, on every device-scoped route).
        stranger_tutor = await _make_user_with_role(db, label="stranger-tutor", role_code="TUTOR")
        await db.flush()

        tutor_refresh_token = generate_refresh_token()
        db.add(
            UserSession(
                user_id=tutor.id,
                refresh_token_hash=hash_refresh_token(tutor_refresh_token),
                expires_at=refresh_token_expiry(),
            )
        )
        await db.commit()

        tutor_access_token, _ = create_access_token(tutor.id)
        supervised_access_token, _ = create_access_token(supervised.id)
        stranger_tutor_access_token, _ = create_access_token(stranger_tutor.id)

        json.dump(
            {
                "tutor_user_id": str(tutor.id),
                "tutor_access_token": tutor_access_token,
                "tutor_refresh_token": tutor_refresh_token,
                "supervised_user_id": str(supervised.id),
                "supervised_access_token": supervised_access_token,
                "stranger_tutor_access_token": stranger_tutor_access_token,
            },
            sys.stdout,
        )


asyncio.run(main())

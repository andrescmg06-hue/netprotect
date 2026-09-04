import os
import uuid
from datetime import UTC, datetime

import pytest
from fastapi import HTTPException

from app.api.deps import require_role, require_tutor_of_device
from app.models import Device, TutorDevice, User, UserRole

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL.",
    ),
]


async def _make_user(db_session, unique: str) -> User:
    user = User(email=f"{unique}@example.com", google_sub=f"google-{unique}")
    db_session.add(user)
    await db_session.flush()
    return user


async def test_require_role_allows_a_user_holding_one_of_the_roles(db_session) -> None:
    user = await _make_user(db_session, uuid.uuid4().hex[:8])
    db_session.add(UserRole(user_id=user.id, role_code="TUTOR"))
    await db_session.commit()

    checker = require_role("TUTOR", "SUPERVISADO")
    result = await checker(current_user=user, db=db_session)

    assert result.id == user.id


async def test_require_role_rejects_a_user_without_any_matching_role(db_session) -> None:
    user = await _make_user(db_session, uuid.uuid4().hex[:8])
    db_session.add(UserRole(user_id=user.id, role_code="SUPERVISADO"))
    await db_session.commit()

    checker = require_role("TUTOR")
    with pytest.raises(HTTPException) as exc_info:
        await checker(current_user=user, db=db_session)

    assert exc_info.value.status_code == 403


async def test_tutor_can_load_their_own_linked_device(db_session) -> None:
    unique = uuid.uuid4().hex[:8]
    tutor = await _make_user(db_session, f"tutor-{unique}")
    supervised = await _make_user(db_session, f"supervised-{unique}")
    device = Device(name="Celular de prueba", supervised_user_id=supervised.id)
    db_session.add(device)
    await db_session.flush()
    db_session.add(TutorDevice(tutor_user_id=tutor.id, device_id=device.id))
    await db_session.commit()

    loaded = await require_tutor_of_device(device_id=device.id, current_user=tutor, db=db_session)

    assert loaded.id == device.id


async def test_a_tutor_cannot_load_another_tutors_device(db_session) -> None:
    """The IDOR/BOLA case the RBAC sprint exists to close: tutor A must not read tutor B's
    device."""
    unique = uuid.uuid4().hex[:8]
    tutor_a = await _make_user(db_session, f"tutor-a-{unique}")
    tutor_b = await _make_user(db_session, f"tutor-b-{unique}")
    supervised = await _make_user(db_session, f"supervised-{unique}")
    device = Device(name="Dispositivo de B", supervised_user_id=supervised.id)
    db_session.add(device)
    await db_session.flush()
    db_session.add(TutorDevice(tutor_user_id=tutor_b.id, device_id=device.id))
    await db_session.commit()

    with pytest.raises(HTTPException) as exc_info:
        await require_tutor_of_device(device_id=device.id, current_user=tutor_a, db=db_session)

    # 404, not 403: tutor_a must not be able to tell "not yours" from "doesn't exist".
    assert exc_info.value.status_code == 404


async def test_require_tutor_of_device_404s_for_a_device_that_does_not_exist(db_session) -> None:
    tutor = await _make_user(db_session, uuid.uuid4().hex[:8])
    await db_session.commit()

    with pytest.raises(HTTPException) as exc_info:
        await require_tutor_of_device(device_id=uuid.uuid4(), current_user=tutor, db=db_session)

    assert exc_info.value.status_code == 404


async def test_unlinking_a_device_revokes_the_tutors_access(db_session) -> None:
    unique = uuid.uuid4().hex[:8]
    tutor = await _make_user(db_session, f"tutor-{unique}")
    supervised = await _make_user(db_session, f"supervised-{unique}")
    device = Device(name="Se va a desvincular", supervised_user_id=supervised.id)
    db_session.add(device)
    await db_session.flush()
    link = TutorDevice(tutor_user_id=tutor.id, device_id=device.id)
    db_session.add(link)
    await db_session.commit()

    still_linked = await require_tutor_of_device(
        device_id=device.id, current_user=tutor, db=db_session
    )
    assert still_linked.id == device.id

    link.unlinked_at = datetime.now(UTC)
    await db_session.commit()

    with pytest.raises(HTTPException) as exc_info:
        await require_tutor_of_device(device_id=device.id, current_user=tutor, db=db_session)

    assert exc_info.value.status_code == 404

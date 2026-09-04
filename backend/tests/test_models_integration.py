import os
import uuid
from datetime import UTC, datetime, timedelta

import pytest
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.models import Device, DeviceStatus, PairingCode, Role, TutorDevice, User, UserRole

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL.",
    ),
]


async def test_roles_are_seeded_by_migration(db_session) -> None:
    result = await db_session.execute(select(Role.code).order_by(Role.code))
    codes = {row[0] for row in result.all()}

    assert {"TUTOR", "SUPERVISADO"}.issubset(codes)


async def test_pairing_and_linking_graph_round_trips(db_session) -> None:
    unique = uuid.uuid4().hex[:8]

    tutor = User(email=f"tutor-{unique}@example.com", google_sub=f"google-tutor-{unique}")
    supervised = User(
        email=f"supervised-{unique}@example.com", google_sub=f"google-supervised-{unique}"
    )
    db_session.add_all([tutor, supervised])
    await db_session.flush()

    db_session.add(UserRole(user_id=tutor.id, role_code="TUTOR"))
    db_session.add(UserRole(user_id=supervised.id, role_code="SUPERVISADO"))

    device = Device(name="Celular de prueba", supervised_user_id=supervised.id)
    db_session.add(device)
    await db_session.flush()

    db_session.add(DeviceStatus(device_id=device.id, status="ONLINE"))
    db_session.add(TutorDevice(tutor_user_id=tutor.id, device_id=device.id))
    db_session.add(
        PairingCode(
            tutor_user_id=tutor.id,
            code_hash="a" * 64,
            expires_at=datetime.now(UTC) + timedelta(minutes=3),
            used_at=datetime.now(UTC),
            device_id=device.id,
        )
    )
    await db_session.commit()

    stored_device = (
        await db_session.execute(
            select(Device)
            .where(Device.id == device.id)
            .options(selectinload(Device.status), selectinload(Device.tutor_links))
        )
    ).scalar_one()

    assert stored_device.supervised_user_id == supervised.id
    assert stored_device.status.status == "ONLINE"
    assert stored_device.tutor_links[0].tutor_user_id == tutor.id

    tutor_role_count = await db_session.scalar(
        select(UserRole).where(UserRole.user_id == tutor.id, UserRole.role_code == "TUTOR")
    )
    assert tutor_role_count is not None

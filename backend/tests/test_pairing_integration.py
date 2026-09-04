import os
import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.security import hash_pairing_code
from app.main import app
from app.models import AuditLog, Device, DeviceStatus, PairingCode, TutorDevice
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
    ),
]


@pytest.fixture
def client():
    """One client per test, with a source host unique to that test.

    Unique host: the per-IP redemption limiter keeps counters in Redis for 15 minutes, so a
    shared address would let one test spend another test's budget — and would carry over
    between runs of the suite.

    One client: every TestClient exit runs the app's lifespan shutdown, which disposes the
    global connection pools; doing that ten times per test costs seconds each time.
    """
    with TestClient(app, client=(f"test-{uuid.uuid4().hex}", 51000)) as test_client:
        yield test_client


def _make_account(client: TestClient, role: str) -> tuple[str, str]:
    """Logs a fresh Google user in and grants it `role`. Returns (access_token, email)."""
    unique = uuid.uuid4().hex[:10]
    identity = GoogleIdentity(
        google_sub=f"google-{unique}",
        email=f"{unique}@example.com",
        display_name=f"Usuaria {unique[:4]}",
        avatar_url=None,
    )
    with patch("app.api.v1.endpoints.auth.verify_google_id_token", return_value=identity):
        login = client.post("/api/v1/auth/google", json={"id_token": "fake"})
    assert login.status_code == 200
    token = login.json()["access_token"]

    granted = client.post(
        "/api/v1/users/me/roles", json={"role_code": role}, headers=_auth(token)
    )
    assert granted.status_code == 200
    return token, identity.email


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _redeem_body(code: str, instance_id: str | None = None) -> dict:
    return {
        "code": code,
        "device_instance_id": instance_id or uuid.uuid4().hex,
        "device_name": "Celular de prueba",
        "platform": "ANDROID",
        "os_version": "16",
        "app_version": "0.1.0",
    }


def _generate_code(client: TestClient, tutor_token: str) -> str:
    response = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token))
    assert response.status_code == 200, response.text
    return response.json()["code"]


# --------------------------------------------------------------------------- happy path


def test_a_generated_code_is_six_digits_and_short_lived(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token))

    body = response.json()
    assert response.status_code == 200
    assert len(body["code"]) == 6
    assert body["code"].isdigit()
    assert body["expires_in_seconds"] == 180


async def test_redeeming_links_the_device_to_the_tutor(client, db_session) -> None:
    tutor_token, tutor_email = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    response = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(supervised_token)
    )

    assert response.status_code == 200, response.text
    body = response.json()
    # The supervised person is told who is now supervising them.
    assert body["tutor"]["email"] == tutor_email

    device_id = uuid.UUID(body["device_id"])
    link = (
        await db_session.execute(
            select(TutorDevice).where(
                TutorDevice.device_id == device_id, TutorDevice.unlinked_at.is_(None)
            )
        )
    ).scalar_one()
    assert link is not None

    device_status = await db_session.get(DeviceStatus, device_id)
    assert device_status is not None
    assert device_status.status == "ONLINE"


async def test_linking_is_audited_without_ever_recording_the_code(client, db_session) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    response = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(supervised_token)
    )
    device_id = response.json()["device_id"]

    rows = (
        (
            await db_session.execute(
                select(AuditLog).where(
                    AuditLog.action.in_(("DEVICE_LINKED", "PAIRING_CODE_GENERATED"))
                )
            )
        )
        .scalars()
        .all()
    )

    assert any(row.resource_id == device_id for row in rows), "the link should be audited"
    # The plaintext code must never reach the audit trail.
    assert all(code not in (row.resource_id or "") for row in rows)


# ------------------------------------------------------------------- single use / expiry


def test_a_code_works_only_once(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    first_token, _ = _make_account(client, "SUPERVISADO")
    second_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    first = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(first_token)
    )
    second = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(second_token)
    )

    assert first.status_code == 200
    assert second.status_code == 401


async def test_an_expired_code_is_refused(client, db_session) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    row = (
        await db_session.execute(
            select(PairingCode).where(PairingCode.code_hash == hash_pairing_code(code))
        )
    ).scalar_one()
    row.expires_at = datetime.now(UTC) - timedelta(seconds=1)
    await db_session.commit()

    response = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(supervised_token)
    )

    assert response.status_code == 401


def test_generating_a_new_code_retires_the_previous_one(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    first_code = _generate_code(client, tutor_token)
    second_code = _generate_code(client, tutor_token)

    stale = client.post(
        "/api/v1/pairing/redeem",
        json=_redeem_body(first_code),
        headers=_auth(supervised_token),
    )
    current = client.post(
        "/api/v1/pairing/redeem",
        json=_redeem_body(second_code),
        headers=_auth(supervised_token),
    )

    assert stale.status_code == 401
    assert current.status_code == 200


def test_a_tutor_can_revoke_their_active_code(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    revoked = client.delete("/api/v1/pairing/codes/current", headers=_auth(tutor_token))
    after = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(supervised_token)
    )

    assert revoked.status_code == 200
    assert after.status_code == 401


async def test_every_rejection_looks_identical_from_outside(client, db_session) -> None:
    """Unknown, expired, used and revoked codes must be indistinguishable to a guesser."""
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    expired_tutor, _ = _make_account(client, "TUTOR")
    expired_code = _generate_code(client, expired_tutor)

    revoked_tutor, _ = _make_account(client, "TUTOR")
    revoked_code = _generate_code(client, revoked_tutor)

    used_tutor, _ = _make_account(client, "TUTOR")
    used_code = _generate_code(client, used_tutor)

    for code_value, field in (
        (expired_code, "expires_at"),
        (revoked_code, "revoked_at"),
        (used_code, "used_at"),
    ):
        row = (
            await db_session.execute(
                select(PairingCode).where(PairingCode.code_hash == hash_pairing_code(code_value))
            )
        ).scalar_one()
        if field == "expires_at":
            row.expires_at = datetime.now(UTC) - timedelta(seconds=1)
        else:
            setattr(row, field, datetime.now(UTC))
    await db_session.commit()

    responses = [
        client.post(
            "/api/v1/pairing/redeem", json=_redeem_body(candidate), headers=_auth(supervised_token)
        )
        for candidate in ("000000", expired_code, revoked_code, used_code)
    ]

    assert {r.status_code for r in responses} == {401}
    assert {r.json()["detail"] for r in responses} == {"invalid_or_expired_code"}


# ------------------------------------------------------------------------------- roles


def test_only_a_tutor_can_generate_a_code(client) -> None:
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    response = client.post("/api/v1/pairing/codes", headers=_auth(supervised_token))

    assert response.status_code == 403


def test_only_a_supervised_user_can_redeem(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    code = _generate_code(client, tutor_token)

    response = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(tutor_token)
    )

    assert response.status_code == 403


# ------------------------------------------------------------------- device identity


async def test_the_same_phone_pairing_twice_reuses_one_device_row(client, db_session) -> None:
    first_tutor, _ = _make_account(client, "TUTOR")
    second_tutor, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    instance_id = uuid.uuid4().hex

    first = client.post(
        "/api/v1/pairing/redeem",
        json=_redeem_body(_generate_code(client, first_tutor), instance_id),
        headers=_auth(supervised_token),
    )
    second = client.post(
        "/api/v1/pairing/redeem",
        json=_redeem_body(_generate_code(client, second_tutor), instance_id),
        headers=_auth(supervised_token),
    )

    assert first.status_code == 200, first.text
    assert second.status_code == 200, second.text
    # One phone, one device row, now supervised by two tutors.
    assert first.json()["device_id"] == second.json()["device_id"]

    device_id = uuid.UUID(first.json()["device_id"])
    links = (
        (
            await db_session.execute(
                select(TutorDevice).where(
                    TutorDevice.device_id == device_id, TutorDevice.unlinked_at.is_(None)
                )
            )
        )
        .scalars()
        .all()
    )
    assert len(links) == 2

    devices = (
        (await db_session.execute(select(Device).where(Device.device_instance_id == instance_id)))
        .scalars()
        .all()
    )
    assert len(devices) == 1


# ------------------------------------------------------------------------------ unlink


async def test_a_tutor_can_unlink_and_loses_access(client, db_session) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    code = _generate_code(client, tutor_token)

    linked = client.post(
        "/api/v1/pairing/redeem", json=_redeem_body(code), headers=_auth(supervised_token)
    )
    assert linked.status_code == 200, linked.text
    device_id = linked.json()["device_id"]

    unlinked = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))
    again = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(tutor_token))

    assert unlinked.status_code == 200
    # Once unlinked the device is no longer reachable by that tutor at all.
    assert again.status_code == 404

    device_status = await db_session.get(DeviceStatus, uuid.UUID(device_id))
    assert device_status is not None
    assert device_status.status == "UNLINKED"


def test_a_tutor_cannot_unlink_someone_elses_device(client) -> None:
    owner_token, _ = _make_account(client, "TUTOR")
    stranger_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    linked = client.post(
        "/api/v1/pairing/redeem",
        json=_redeem_body(_generate_code(client, owner_token)),
        headers=_auth(supervised_token),
    )
    assert linked.status_code == 200, linked.text
    device_id = linked.json()["device_id"]

    response = client.delete(f"/api/v1/devices/{device_id}/link", headers=_auth(stranger_token))

    assert response.status_code == 404


# -------------------------------------------------------------------------- brute force


def test_repeated_guesses_are_rate_limited(client) -> None:
    supervised_token, _ = _make_account(client, "SUPERVISADO")

    statuses = []
    for _ in range(12):
        response = client.post(
            "/api/v1/pairing/redeem",
            json=_redeem_body("999999"),
            headers=_auth(supervised_token),
        )
        statuses.append(response.status_code)
        if response.status_code == 429:
            assert "Retry-After" in response.headers
            break

    assert 429 in statuses, "guessing must be throttled before the code space is exhausted"

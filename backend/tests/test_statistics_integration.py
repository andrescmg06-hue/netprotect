import os
import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.google_auth import GoogleIdentity

pytestmark = [
    pytest.mark.integration,
    pytest.mark.skipif(
        os.getenv("RUN_INTEGRATION_TESTS") != "1",
        reason="Set RUN_INTEGRATION_TESTS=1 and provide PostgreSQL and Redis.",
    ),
]

TODAY = datetime.now(UTC).date()


def _iso(days_ago: int) -> str:
    return (TODAY - timedelta(days=days_ago)).isoformat()


@pytest.fixture
def client():
    with TestClient(app, client=(f"test-{uuid.uuid4().hex}", 51000)) as test_client:
        yield test_client


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _make_account(client: TestClient, role: str) -> tuple[str, str]:
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


def _link_a_device(client: TestClient, tutor_token: str, supervised_token: str) -> str:
    code = client.post("/api/v1/pairing/codes", headers=_auth(tutor_token)).json()["code"]
    redeemed = client.post(
        "/api/v1/pairing/redeem",
        json={
            "code": code,
            "device_instance_id": uuid.uuid4().hex,
            "device_name": "Celular de Juan",
            "platform": "ANDROID",
            "os_version": "16",
            "app_version": "0.1.0",
        },
        headers=_auth(supervised_token),
    )
    assert redeemed.status_code == 200, redeemed.text
    return redeemed.json()["device_id"]


def _setup_linked_device(client: TestClient) -> tuple[str, str, str]:
    tutor_token, _ = _make_account(client, "TUTOR")
    supervised_token, _ = _make_account(client, "SUPERVISADO")
    device_id = _link_a_device(client, tutor_token, supervised_token)
    return tutor_token, supervised_token, device_id


def _sync(
    client: TestClient,
    token: str,
    device_id: str,
    usage_date: str,
    apps: list[tuple[str, str, bool]],
    usage: list[tuple[str, int]] | None = None,
):
    return client.post(
        f"/api/v1/devices/{device_id}/applications/sync",
        json={
            "usage_date": usage_date,
            "installed_apps": [
                {"package_name": pkg, "app_label": label, "is_system_app": system}
                for pkg, label, system in apps
            ],
            "daily_usage": [
                {"package_name": pkg, "foreground_seconds": seconds}
                for pkg, seconds in (usage or [])
            ],
        },
        headers=_auth(token),
    )


def _assign_category(client: TestClient, token: str, device_id: str, package: str, category: str):
    return client.post(
        f"/api/v1/devices/{device_id}/app-categories",
        json={"package_name": package, "category": category},
        headers=_auth(token),
    )


def _set_app_rule(client: TestClient, token: str, device_id: str, package: str, **fields):
    return client.post(
        f"/api/v1/devices/{device_id}/rules",
        json={"package_name": package, **fields},
        headers=_auth(token),
    )


def _set_category_rule(client: TestClient, token: str, device_id: str, category: str, **fields):
    return client.post(
        f"/api/v1/devices/{device_id}/category-rules",
        json={"category": category, **fields},
        headers=_auth(token),
    )


def _report_rule_event(
    client: TestClient,
    token: str,
    device_id: str,
    package: str,
    rule_type: str,
    *,
    occurred_at: str,
):
    return client.post(
        f"/api/v1/devices/{device_id}/rule-events",
        json={"package_name": package, "rule_type_applied": rule_type, "occurred_at": occurred_at},
        headers=_auth(token),
    )


def _get_statistics(client: TestClient, token: str, device_id: str, period: str = "today"):
    return client.get(
        f"/api/v1/devices/{device_id}/statistics",
        params={"period": period},
        headers=_auth(token),
    )


def test_top_apps_are_summed_across_the_period_and_ranked_descending(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    apps = [("com.instagram.android", "Instagram", False), ("com.spotify.music", "Spotify", False)]
    assert _sync(
        client, supervised_token, device_id, _iso(0), apps,
        usage=[("com.instagram.android", 600), ("com.spotify.music", 100)],
    ).status_code == 200
    assert _sync(
        client, supervised_token, device_id, _iso(1), apps,
        usage=[("com.instagram.android", 300)],
    ).status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="7d")

    assert response.status_code == 200, response.text
    body = response.json()
    assert body["period"] == "7d"
    top_apps = body["top_apps"]
    assert top_apps[0] == {
        "package_name": "com.instagram.android", "app_label": "Instagram", "total_seconds": 900
    }
    assert top_apps[1] == {
        "package_name": "com.spotify.music", "app_label": "Spotify", "total_seconds": 100
    }


def test_today_period_excludes_usage_from_other_days(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    apps = [("com.instagram.android", "Instagram", False)]
    assert _sync(
        client, supervised_token, device_id, _iso(0), apps, usage=[("com.instagram.android", 500)]
    ).status_code == 200
    assert _sync(
        client, supervised_token, device_id, _iso(1), apps, usage=[("com.instagram.android", 999)]
    ).status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="today")

    assert response.status_code == 200, response.text
    assert response.json()["top_apps"] == [
        {"package_name": "com.instagram.android", "app_label": "Instagram", "total_seconds": 500}
    ]


def test_a_package_without_a_category_assignment_groups_under_null(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    apps = [("com.a", "App A", False), ("com.b", "App B", False)]
    assert _sync(
        client, supervised_token, device_id, _iso(0), apps,
        usage=[("com.a", 200), ("com.b", 300)],
    ).status_code == 200
    assert _assign_category(client, tutor_token, device_id, "com.a", "GAMES").status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="today")

    assert response.status_code == 200, response.text
    categories = {
        entry["category"]: entry["total_seconds"] for entry in response.json()["categories"]
    }
    assert categories == {"GAMES": 200, None: 300}


def test_blocks_are_counted_by_reason_within_the_period(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    now = datetime.now(UTC)
    assert _report_rule_event(
        client, supervised_token, device_id, "com.a", "BLOCK",
        occurred_at=now.isoformat(),
    ).status_code == 200
    assert _report_rule_event(
        client, supervised_token, device_id, "com.b", "BLOCK",
        occurred_at=now.isoformat(),
    ).status_code == 200
    assert _report_rule_event(
        client, supervised_token, device_id, "com.c", "SCHOOL_MODE",
        occurred_at=now.isoformat(),
    ).status_code == 200
    old = now - timedelta(days=45)
    assert _report_rule_event(
        client, supervised_token, device_id, "com.d", "BLOCK", occurred_at=old.isoformat()
    ).status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="30d")

    assert response.status_code == 200, response.text
    counts = {
        entry["rule_type_applied"]: entry["count"]
        for entry in response.json()["blocks_by_reason"]
    }
    assert counts == {"BLOCK": 2, "SCHOOL_MODE": 1}


def test_daily_limit_compliance_counts_only_days_with_reported_usage(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    apps = [("com.instagram.android", "Instagram", False)]
    assert _set_app_rule(
        client, tutor_token, device_id, "com.instagram.android",
        rule_type="DAILY_LIMIT", daily_limit_minutes=10,
    ).status_code == 200
    # Compliant day: 5 minutes, under the 10-minute limit.
    assert _sync(
        client, supervised_token, device_id, _iso(0), apps, usage=[("com.instagram.android", 300)]
    ).status_code == 200
    # Non-compliant day: 15 minutes, over the limit.
    assert _sync(
        client, supervised_token, device_id, _iso(1), apps, usage=[("com.instagram.android", 900)]
    ).status_code == 200
    # A third day in range with no usage reported at all must not count either way.

    response = _get_statistics(client, tutor_token, device_id, period="7d")

    assert response.status_code == 200, response.text
    entries = response.json()["compliance"]
    assert len(entries) == 1
    entry = entries[0]
    assert entry["scope"] == "APP"
    assert entry["package_name"] == "com.instagram.android"
    assert entry["daily_limit_minutes"] == 10
    assert entry["days_evaluated"] == 2
    assert entry["days_compliant"] == 1
    assert entry["compliance_rate"] == 0.5


def test_category_daily_limit_compliance_sums_usage_across_member_apps(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    apps = [("com.a", "App A", False), ("com.b", "App B", False)]
    assert _assign_category(client, tutor_token, device_id, "com.a", "GAMES").status_code == 200
    assert _assign_category(client, tutor_token, device_id, "com.b", "GAMES").status_code == 200
    assert _set_category_rule(
        client, tutor_token, device_id, "GAMES", rule_type="DAILY_LIMIT", daily_limit_minutes=10
    ).status_code == 200
    assert _sync(
        client, supervised_token, device_id, _iso(0), apps,
        usage=[("com.a", 400), ("com.b", 400)],
    ).status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="today")

    assert response.status_code == 200, response.text
    entries = [e for e in response.json()["compliance"] if e["scope"] == "CATEGORY"]
    assert len(entries) == 1
    assert entries[0]["category"] == "GAMES"
    assert entries[0]["days_evaluated"] == 1
    # 400 + 400 = 800s > 600s (10-minute limit): each app alone stays under the limit, but the
    # category rule sums usage across its member apps, so the day is a violation.
    assert entries[0]["days_compliant"] == 0


def test_weekly_limit_and_schedule_rules_do_not_appear_in_compliance(client) -> None:
    tutor_token, supervised_token, device_id = _setup_linked_device(client)
    assert _set_app_rule(
        client, tutor_token, device_id, "com.a", rule_type="WEEKLY_LIMIT", weekly_limit_minutes=60
    ).status_code == 200
    assert _set_app_rule(
        client, tutor_token, device_id, "com.b", rule_type="SCHEDULE",
        schedule_start_minute=0, schedule_end_minute=60, schedule_days_mask=127,
    ).status_code == 200

    response = _get_statistics(client, tutor_token, device_id, period="today")

    assert response.status_code == 200, response.text
    assert response.json()["compliance"] == []


def test_a_stranger_tutor_cannot_read_the_device_statistics(client) -> None:
    tutor_token, _, device_id = _setup_linked_device(client)
    stranger_token, _ = _make_account(client, "TUTOR")

    response = _get_statistics(client, stranger_token, device_id)

    assert response.status_code == 404


def test_a_supervised_user_cannot_read_the_device_statistics(client) -> None:
    _, supervised_token, device_id = _setup_linked_device(client)

    response = _get_statistics(client, supervised_token, device_id)

    assert response.status_code == 404


def test_reading_statistics_for_a_nonexistent_device_is_404(client) -> None:
    tutor_token, _ = _make_account(client, "TUTOR")

    response = client.get(
        f"/api/v1/devices/{uuid.uuid4()}/statistics",
        params={"period": "today"},
        headers=_auth(tutor_token),
    )

    assert response.status_code == 404

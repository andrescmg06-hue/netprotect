from datetime import UTC, datetime, timedelta

from app.core.config import settings
from app.services.device_status import compute_effective_status


def test_online_within_the_window_stays_online() -> None:
    recent = datetime.now(UTC) - timedelta(seconds=5)
    assert compute_effective_status("ONLINE", recent) == "ONLINE"


def test_online_past_the_window_is_reported_offline() -> None:
    stale = datetime.now(UTC) - timedelta(
        seconds=settings.device_offline_threshold_seconds + 1
    )
    assert compute_effective_status("ONLINE", stale) == "OFFLINE"


def test_online_with_no_heartbeat_ever_is_offline() -> None:
    assert compute_effective_status("ONLINE", None) == "OFFLINE"


def test_non_online_states_are_never_second_guessed() -> None:
    ancient = datetime.now(UTC) - timedelta(days=30)
    for explicit_status in ("ALERT", "RESTRICTED", "UNLINKED", "SYNCING"):
        assert compute_effective_status(explicit_status, ancient) == explicit_status
        assert compute_effective_status(explicit_status, None) == explicit_status

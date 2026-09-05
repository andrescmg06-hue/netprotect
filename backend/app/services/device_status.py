from datetime import UTC, datetime

from app.core.config import settings
from app.models.device import OFFLINE, ONLINE


def compute_effective_status(stored_status: str, last_seen_at: datetime | None) -> str:
    """The status shown to a tutor, derived at read time.

    Only ONLINE is ever second-guessed: ALERT, RESTRICTED and UNLINKED are explicit facts set
    by their own triggers (manipulation detection, schedule rules, unlinking — none of which
    exist yet) and heartbeat silence doesn't get to override them. ONLINE is different: it's
    only true as long as a heartbeat keeps confirming it, so a stale one is downgraded to
    OFFLINE here rather than waiting on a background sweep this project doesn't have yet.
    """
    if stored_status != ONLINE:
        return stored_status

    if last_seen_at is None:
        return OFFLINE

    age_seconds = (datetime.now(UTC) - last_seen_at).total_seconds()
    if age_seconds > settings.device_offline_threshold_seconds:
        return OFFLINE

    return ONLINE

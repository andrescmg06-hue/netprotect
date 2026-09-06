import uuid
from datetime import datetime

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, UUIDPrimaryKeyMixin

ALLOW = "ALLOW"
BLOCK = "BLOCK"
DAILY_LIMIT = "DAILY_LIMIT"
SCHEDULE = "SCHEDULE"

RULE_TYPES = (ALLOW, BLOCK, DAILY_LIMIT, SCHEDULE)
_RULE_TYPE_LIST_SQL = ", ".join(f"'{value}'" for value in RULE_TYPES)


class AppRule(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    """One rule per (device, package). Creating a rule for an app that already has one
    replaces it (upsert) rather than stacking rules — resolving priority between overlapping
    rules of the *same* scope (per-app) is out of scope for this sprint; priority across scopes
    (per-app vs. allow/block lists vs. categories) belongs to Sprint 9, which owns that as its
    own acceptance criterion.

    ALLOW is accepted and stored like the other three types (Sprint 8 builds the full rule_type
    set from plan-desarrollo.md), but has no observable effect yet: it only matters once a
    broader-scope rule exists to override (an allow/block list or a category, from Sprints 9-10),
    which doesn't exist today. The local evaluator treats ALLOW the same as "no rule" honestly,
    rather than simulating behavior it can't have yet.
    """

    __tablename__ = "app_rules"
    __table_args__ = (
        UniqueConstraint("device_id", "package_name", name="uq_app_rules_device_package"),
        CheckConstraint(f"rule_type IN ({_RULE_TYPE_LIST_SQL})", name="ck_app_rules_type_valid"),
        CheckConstraint(
            "(rule_type != 'DAILY_LIMIT') "
            "OR (daily_limit_minutes IS NOT NULL AND daily_limit_minutes > 0)",
            name="ck_app_rules_daily_limit_requires_minutes",
        ),
        CheckConstraint(
            "(rule_type != 'SCHEDULE') OR ("
            "schedule_start_minute IS NOT NULL AND schedule_start_minute BETWEEN 0 AND 1439 "
            "AND schedule_end_minute IS NOT NULL AND schedule_end_minute BETWEEN 0 AND 1439 "
            "AND schedule_days_mask IS NOT NULL AND schedule_days_mask BETWEEN 1 AND 127"
            ")",
            name="ck_app_rules_schedule_requires_window",
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    package_name: Mapped[str] = mapped_column(String(255))
    rule_type: Mapped[str] = mapped_column(String(16))
    # Only meaningful when rule_type == DAILY_LIMIT.
    daily_limit_minutes: Mapped[int | None] = mapped_column(Integer)
    # Only meaningful when rule_type == SCHEDULE. Minutes since local midnight on the
    # device's own clock, not UTC — normalizing against the device's real timezone is
    # Sprint 11's job (same reasoning as device_application_usage.usage_date in Sprint 7).
    # start > end is a valid overnight window (e.g. 22:00-06:00), evaluated with wraparound.
    schedule_start_minute: Mapped[int | None] = mapped_column(Integer)
    schedule_end_minute: Mapped[int | None] = mapped_column(Integer)
    # Bitmask, bit 0 = Monday ... bit 6 = Sunday (1-127, never 0: a schedule with no days
    # would silently never apply, which is indistinguishable from a bug).
    schedule_days_mask: Mapped[int | None] = mapped_column(Integer)


class AppRuleEvent(UUIDPrimaryKeyMixin, Base):
    """Insert-only record of a rule actually being enforced on the device — evidence that a
    block happened, not a usage total. Deliberately separate from device_application_usage
    (Sprint 7): one is a daily aggregate for display, this is an audit trail for "did my rules
    actually work", and the two must not be conflated into a single table with two purposes.

    Not foreign-keyed to AppRule: a rule can be edited or deleted after the fact, but the record
    that it fired at a given moment must survive that, the same way usage history survives an
    app being uninstalled.
    """

    __tablename__ = "app_rule_events"
    __table_args__ = (
        CheckConstraint(
            f"rule_type_applied IN ({_RULE_TYPE_LIST_SQL})", name="ck_app_rule_events_type_valid"
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    package_name: Mapped[str] = mapped_column(String(255))
    rule_type_applied: Mapped[str] = mapped_column(String(16))
    # The device's own clock at the moment it blocked — distinct from received_at, which is
    # when this row landed on the server (same split as first_seen_at vs. synced_at elsewhere).
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    received_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

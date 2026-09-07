import uuid

from sqlalchemy import (
    CheckConstraint,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, UUIDPrimaryKeyMixin

# Fixed catalog, not a table: the project has no requirement yet for a tutor to create custom
# categories, and a table with deletable rows would let a category be removed out from under
# existing assignments/rules. See docs/sprint-10.md for why these specific 11 — the original
# spec's "11 categorías del enunciado" isn't in this repo, so this is a documented team decision,
# not a transcription of a source we don't have.
SOCIAL_MEDIA = "SOCIAL_MEDIA"
GAMES = "GAMES"
STREAMING = "STREAMING"
EDUCATION = "EDUCATION"
PRODUCTIVITY = "PRODUCTIVITY"
COMMUNICATION = "COMMUNICATION"
NEWS = "NEWS"
SHOPPING = "SHOPPING"
FINANCE = "FINANCE"
UTILITIES = "UTILITIES"
ADULT_CONTENT = "ADULT_CONTENT"

CATEGORIES = (
    SOCIAL_MEDIA,
    GAMES,
    STREAMING,
    EDUCATION,
    PRODUCTIVITY,
    COMMUNICATION,
    NEWS,
    SHOPPING,
    FINANCE,
    UTILITIES,
    ADULT_CONTENT,
)
_CATEGORY_LIST_SQL = ", ".join(f"'{value}'" for value in CATEGORIES)

# Mirrors app/models/rule.py's RULE_TYPES exactly — category_rules is the same shape as
# app_rules, just keyed by category instead of package_name (see CategoryRule below).
ALLOW = "ALLOW"
BLOCK = "BLOCK"
DAILY_LIMIT = "DAILY_LIMIT"
WEEKLY_LIMIT = "WEEKLY_LIMIT"
SCHEDULE = "SCHEDULE"
_CATEGORY_RULE_TYPES = (ALLOW, BLOCK, DAILY_LIMIT, WEEKLY_LIMIT, SCHEDULE)
_CATEGORY_RULE_TYPE_LIST_SQL = ", ".join(f"'{value}'" for value in _CATEGORY_RULE_TYPES)


class AppCategoryAssignment(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    """Which category a package belongs to, on a given device. Separate from CategoryRule
    because a tutor reassigns an app's category and edits a category's rule independently — see
    docs/sprint-10.md.
    """

    __tablename__ = "app_category_assignments"
    __table_args__ = (
        UniqueConstraint(
            "device_id", "package_name", name="uq_app_category_assignments_device_package"
        ),
        CheckConstraint(
            f"category IN ({_CATEGORY_LIST_SQL})", name="ck_app_category_assignments_valid"
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    package_name: Mapped[str] = mapped_column(String(255))
    category: Mapped[str] = mapped_column(String(32))


class CategoryRule(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    """One rule per (device, category) — same shape and CHECK constraints as AppRule
    (app/models/rule.py), just keyed by category. Evaluated only when the foreground package has
    no AppRule of its own; a per-app rule always wins (see RuleEvaluator on Android).
    """

    __tablename__ = "category_rules"
    __table_args__ = (
        UniqueConstraint("device_id", "category", name="uq_category_rules_device_category"),
        CheckConstraint(
            f"category IN ({_CATEGORY_LIST_SQL})", name="ck_category_rules_category_valid"
        ),
        CheckConstraint(
            f"rule_type IN ({_CATEGORY_RULE_TYPE_LIST_SQL})", name="ck_category_rules_type_valid"
        ),
        CheckConstraint(
            "(rule_type != 'DAILY_LIMIT') "
            "OR (daily_limit_minutes IS NOT NULL AND daily_limit_minutes > 0)",
            name="ck_category_rules_daily_limit_requires_minutes",
        ),
        CheckConstraint(
            "(rule_type != 'WEEKLY_LIMIT') "
            "OR (weekly_limit_minutes IS NOT NULL AND weekly_limit_minutes > 0)",
            name="ck_category_rules_weekly_limit_requires_minutes",
        ),
        CheckConstraint(
            "(rule_type != 'SCHEDULE') OR ("
            "schedule_start_minute IS NOT NULL AND schedule_start_minute BETWEEN 0 AND 1439 "
            "AND schedule_end_minute IS NOT NULL AND schedule_end_minute BETWEEN 0 AND 1439 "
            "AND schedule_days_mask IS NOT NULL AND schedule_days_mask BETWEEN 1 AND 127"
            ")",
            name="ck_category_rules_schedule_requires_window",
        ),
    )

    device_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("devices.id", ondelete="CASCADE"), index=True
    )
    category: Mapped[str] = mapped_column(String(32))
    rule_type: Mapped[str] = mapped_column(String(16))
    daily_limit_minutes: Mapped[int | None] = mapped_column(Integer)
    weekly_limit_minutes: Mapped[int | None] = mapped_column(Integer)
    schedule_start_minute: Mapped[int | None] = mapped_column(Integer)
    schedule_end_minute: Mapped[int | None] = mapped_column(Integer)
    schedule_days_mask: Mapped[int | None] = mapped_column(Integer)

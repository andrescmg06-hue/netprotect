from datetime import date
from typing import Literal

from pydantic import BaseModel

from app.schemas.category import Category
from app.schemas.rule import AppliedRuleType

StatisticsPeriod = Literal["today", "7d", "30d"]

# Bounds a single response, same "response size cap, not a retention policy" reasoning as
# MAX_HISTORY_EVENTS / MAX_GEOFENCE_EVENTS (app/schemas/history.py, app/schemas/geofence.py).
MAX_TOP_APPS = 10


class TopAppEntry(BaseModel):
    package_name: str
    # None when the device never synced a catalog row for this package (usage rows aren't
    # foreign-keyed to DeviceApplication — see DeviceApplicationUsage's docstring — so a label
    # lookup can miss even though a usage total exists).
    app_label: str | None
    total_seconds: int


class CategoryTotalEntry(BaseModel):
    # None groups every package with usage but no row in AppCategoryAssignment, instead of
    # silently dropping it from the breakdown.
    category: Category | None
    total_seconds: int


class BlockCountEntry(BaseModel):
    rule_type_applied: AppliedRuleType
    count: int


ComplianceScope = Literal["APP", "CATEGORY"]


class ComplianceEntry(BaseModel):
    """One DAILY_LIMIT rule's track record over the period. WEEKLY_LIMIT and SCHEDULE rules never
    appear here: a weekly total isn't a per-day check, and a schedule isn't a quantity limit —
    same exclusion Sprint 15 already applied when scoping its own retention work to what a
    per-day/per-window rule actually measures.

    days_evaluated only counts days the device actually reported usage for the app (or, for a
    category, for at least one of its assigned apps) — a day with no reported usage is neither a
    violation nor a compliance, it's missing data.
    """

    scope: ComplianceScope
    package_name: str | None
    category: Category | None
    daily_limit_minutes: int
    days_evaluated: int
    days_compliant: int
    compliance_rate: float | None


class DeviceStatisticsResponse(BaseModel):
    period: StatisticsPeriod
    range_start: date
    range_end: date
    top_apps: list[TopAppEntry]
    categories: list[CategoryTotalEntry]
    blocks_by_reason: list[BlockCountEntry]
    compliance: list[ComplianceEntry]

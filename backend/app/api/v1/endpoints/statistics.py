import uuid
from collections import defaultdict
from datetime import UTC, date, datetime, timedelta

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import require_tutor_of_device
from app.db.session import get_db
from app.models import (
    AppCategoryAssignment,
    AppRule,
    AppRuleEvent,
    CategoryRule,
    Device,
    DeviceApplication,
    DeviceApplicationUsage,
)
from app.schemas.statistics import (
    MAX_TOP_APPS,
    BlockCountEntry,
    CategoryTotalEntry,
    ComplianceEntry,
    DeviceStatisticsResponse,
    StatisticsPeriod,
    TopAppEntry,
)

router = APIRouter(tags=["statistics"])

_PERIOD_DAYS = {"today": 1, "7d": 7, "30d": 30}


def _period_range(period: StatisticsPeriod) -> tuple[date, date]:
    today = datetime.now(UTC).date()
    range_start = today - timedelta(days=_PERIOD_DAYS[period] - 1)
    return range_start, today


@router.get("/devices/{device_id}/statistics", response_model=DeviceStatisticsResponse)
async def get_device_statistics(
    device_id: uuid.UUID,
    period: StatisticsPeriod = Query(default="today"),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> DeviceStatisticsResponse:
    """Aggregates for a tutor reviewing a device: most-used apps, a category breakdown, how many
    times a rule actually blocked something, and — for DAILY_LIMIT rules only — how many of the
    period's reported days stayed under the configured limit (Sprint 16).

    No new aggregation table: DeviceApplicationUsage is already a daily total per app (Sprint 7),
    so a period is just a GROUP BY over at most 30 rows per app — the same reasoning Sprint 15
    used to justify not building a dedicated history table. Periods are calendar dates in the
    server's own UTC "today", not the device's local day: usage_date has been an opaque,
    device-assigned label the server never recomputes since Sprint 7, and this endpoint keeps
    that rather than inventing timezone normalization just for statistics.
    """
    range_start, range_end = _period_range(period)

    usage_rows = (
        await db.execute(
            select(DeviceApplicationUsage).where(
                DeviceApplicationUsage.device_id == device_id,
                DeviceApplicationUsage.usage_date >= range_start,
                DeviceApplicationUsage.usage_date <= range_end,
            )
        )
    ).scalars().all()

    totals_by_package: dict[str, int] = defaultdict(int)
    usage_by_package_and_day: dict[str, dict[date, int]] = defaultdict(dict)
    for row in usage_rows:
        totals_by_package[row.package_name] += row.foreground_seconds
        usage_by_package_and_day[row.package_name][row.usage_date] = row.foreground_seconds

    apps = (
        await db.execute(
            select(DeviceApplication).where(DeviceApplication.device_id == device_id)
        )
    ).scalars().all()
    label_by_package = {app.package_name: app.app_label for app in apps}

    top_apps = [
        TopAppEntry(
            package_name=package_name,
            app_label=label_by_package.get(package_name),
            total_seconds=total_seconds,
        )
        for package_name, total_seconds in sorted(
            totals_by_package.items(), key=lambda item: item[1], reverse=True
        )[:MAX_TOP_APPS]
    ]

    assignments = (
        await db.execute(
            select(AppCategoryAssignment).where(AppCategoryAssignment.device_id == device_id)
        )
    ).scalars().all()
    category_by_package = {row.package_name: row.category for row in assignments}
    packages_by_category: dict[str | None, list[str]] = defaultdict(list)
    for package_name in totals_by_package:
        packages_by_category[category_by_package.get(package_name)].append(package_name)

    category_totals: dict[str | None, int] = defaultdict(int)
    for package_name, total_seconds in totals_by_package.items():
        category_totals[category_by_package.get(package_name)] += total_seconds
    categories = [
        CategoryTotalEntry(category=category, total_seconds=total_seconds)
        for category, total_seconds in sorted(
            category_totals.items(), key=lambda item: item[1], reverse=True
        )
    ]

    range_start_dt = datetime.combine(range_start, datetime.min.time(), tzinfo=UTC)
    range_end_dt = datetime.combine(range_end + timedelta(days=1), datetime.min.time(), tzinfo=UTC)
    rule_events = (
        await db.execute(
            select(AppRuleEvent).where(
                AppRuleEvent.device_id == device_id,
                AppRuleEvent.occurred_at >= range_start_dt,
                AppRuleEvent.occurred_at < range_end_dt,
            )
        )
    ).scalars().all()
    block_counts: dict[str, int] = defaultdict(int)
    for event in rule_events:
        block_counts[event.rule_type_applied] += 1
    blocks_by_reason = [
        BlockCountEntry(rule_type_applied=rule_type, count=count)
        for rule_type, count in sorted(block_counts.items(), key=lambda item: item[1], reverse=True)
    ]

    compliance: list[ComplianceEntry] = []

    app_rules = (
        await db.execute(
            select(AppRule).where(
                AppRule.device_id == device_id, AppRule.rule_type == "DAILY_LIMIT"
            )
        )
    ).scalars().all()
    for rule in app_rules:
        daily_seconds = usage_by_package_and_day.get(rule.package_name, {})
        days_evaluated = len(daily_seconds)
        days_compliant = sum(
            1
            for seconds in daily_seconds.values()
            if seconds <= rule.daily_limit_minutes * 60
        )
        compliance.append(
            ComplianceEntry(
                scope="APP",
                package_name=rule.package_name,
                category=None,
                daily_limit_minutes=rule.daily_limit_minutes,
                days_evaluated=days_evaluated,
                days_compliant=days_compliant,
                compliance_rate=(days_compliant / days_evaluated) if days_evaluated else None,
            )
        )

    category_rules = (
        await db.execute(
            select(CategoryRule).where(
                CategoryRule.device_id == device_id, CategoryRule.rule_type == "DAILY_LIMIT"
            )
        )
    ).scalars().all()
    for rule in category_rules:
        member_packages = packages_by_category.get(rule.category, [])
        totals_by_day: dict[date, int] = defaultdict(int)
        for package_name in member_packages:
            for usage_date, seconds in usage_by_package_and_day.get(package_name, {}).items():
                totals_by_day[usage_date] += seconds
        days_evaluated = len(totals_by_day)
        days_compliant = sum(
            1 for seconds in totals_by_day.values() if seconds <= rule.daily_limit_minutes * 60
        )
        compliance.append(
            ComplianceEntry(
                scope="CATEGORY",
                package_name=None,
                category=rule.category,
                daily_limit_minutes=rule.daily_limit_minutes,
                days_evaluated=days_evaluated,
                days_compliant=days_compliant,
                compliance_rate=(days_compliant / days_evaluated) if days_evaluated else None,
            )
        )

    return DeviceStatisticsResponse(
        period=period,
        range_start=range_start,
        range_end=range_end,
        top_apps=top_apps,
        categories=categories,
        blocks_by_reason=blocks_by_reason,
        compliance=compliance,
    )

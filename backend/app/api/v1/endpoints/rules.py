import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import (
    get_current_user,
    require_supervised_owner_of_device,
    require_tutor_of_device,
)
from app.db.session import get_db
from app.models import AppRule, AppRuleEvent, Device, User
from app.schemas.rule import (
    ActiveRulesResponse,
    AppRuleEventListResponse,
    AppRuleEventResponse,
    AppRuleListResponse,
    AppRuleResponse,
    DeleteAppRuleResponse,
    DevicePolicyResponse,
    ReportRuleEventRequest,
    UpdateDevicePolicyRequest,
    UpsertAppRuleRequest,
)
from app.services.audit import record_audit_event

router = APIRouter(tags=["rules"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _to_rule_response(rule: AppRule) -> AppRuleResponse:
    return AppRuleResponse(
        id=rule.id,
        package_name=rule.package_name,
        rule_type=rule.rule_type,
        daily_limit_minutes=rule.daily_limit_minutes,
        schedule_start_minute=rule.schedule_start_minute,
        schedule_end_minute=rule.schedule_end_minute,
        schedule_days_mask=rule.schedule_days_mask,
        created_at=rule.created_at,
        updated_at=rule.updated_at,
    )


@router.post("/devices/{device_id}/rules", response_model=AppRuleResponse)
async def upsert_app_rule(
    device_id: uuid.UUID,
    payload: UpsertAppRuleRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AppRuleResponse:
    """Creating a rule for a package that already has one replaces it (upsert), not a new row
    — see AppRule's docstring for why only one rule per (device, package) exists in this
    sprint.
    """
    existing = (
        await db.execute(
            select(AppRule).where(
                AppRule.device_id == device_id, AppRule.package_name == payload.package_name
            )
        )
    ).scalar_one_or_none()

    if existing is None:
        rule = AppRule(
            device_id=device_id,
            package_name=payload.package_name,
            rule_type=payload.rule_type,
            daily_limit_minutes=payload.daily_limit_minutes,
            schedule_start_minute=payload.schedule_start_minute,
            schedule_end_minute=payload.schedule_end_minute,
            schedule_days_mask=payload.schedule_days_mask,
        )
        db.add(rule)
        action = "APP_RULE_CREATED"
    else:
        rule = existing
        rule.rule_type = payload.rule_type
        rule.daily_limit_minutes = payload.daily_limit_minutes
        rule.schedule_start_minute = payload.schedule_start_minute
        rule.schedule_end_minute = payload.schedule_end_minute
        rule.schedule_days_mask = payload.schedule_days_mask
        action = "APP_RULE_UPDATED"

    await db.flush()
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action=action,
        resource_type="app_rule",
        resource_id=str(rule.id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(rule)

    return _to_rule_response(rule)


@router.get("/devices/{device_id}/rules", response_model=AppRuleListResponse)
async def list_app_rules(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AppRuleListResponse:
    rules = (
        await db.execute(
            select(AppRule).where(AppRule.device_id == device_id).order_by(AppRule.package_name)
        )
    ).scalars().all()

    return AppRuleListResponse(rules=[_to_rule_response(rule) for rule in rules])


@router.delete("/devices/{device_id}/rules/{rule_id}", response_model=DeleteAppRuleResponse)
async def delete_app_rule(
    device_id: uuid.UUID,
    rule_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> DeleteAppRuleResponse:
    rule = (
        await db.execute(
            select(AppRule).where(AppRule.id == rule_id, AppRule.device_id == device_id)
        )
    ).scalar_one_or_none()
    if rule is None:
        # Same 404-for-both reasoning as require_tutor_of_device: a rule that belongs to
        # another device must look identical to one that doesn't exist.
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="app_rule_not_found")

    now = datetime.now(UTC)
    package_name = rule.package_name
    await db.delete(rule)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="APP_RULE_DELETED",
        resource_type="app_rule",
        resource_id=str(rule_id),
        ip_address=_client_ip(request),
    )
    await db.commit()

    return DeleteAppRuleResponse(rule_id=rule_id, package_name=package_name, deleted_at=now)


@router.get("/devices/{device_id}/rules/active", response_model=ActiveRulesResponse)
async def list_active_app_rules_for_device(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_supervised_owner_of_device),
) -> ActiveRulesResponse:
    """The supervised device pulls its own rule set to evaluate locally — no round-trip per
    app open. Every stored rule is "active" (no soft-delete/expiry concept exists for rules
    yet), so the rule query is the same as the tutor's list, just gated by the other
    dependency. The default policy rides along because an app with no rule still needs an
    answer, and asking for it separately would let the two drift apart between calls.
    """
    rules = (
        await db.execute(
            select(AppRule).where(AppRule.device_id == device_id).order_by(AppRule.package_name)
        )
    ).scalars().all()

    return ActiveRulesResponse(
        rules=[_to_rule_response(rule) for rule in rules],
        default_app_policy=device.default_app_policy,
    )


@router.get("/devices/{device_id}/policy", response_model=DevicePolicyResponse)
async def get_device_policy(
    device_id: uuid.UUID,
    device: Device = Depends(require_tutor_of_device),
) -> DevicePolicyResponse:
    return DevicePolicyResponse(
        device_id=device_id, default_app_policy=device.default_app_policy
    )


@router.put("/devices/{device_id}/policy", response_model=DevicePolicyResponse)
async def update_device_policy(
    device_id: uuid.UUID,
    payload: UpdateDevicePolicyRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> DevicePolicyResponse:
    """Switching to BLOCK turns the device into allowlist mode: apps with no rule stop working.
    Existing rules are untouched either way, so flipping back restores the previous behavior
    exactly — and a tutor can pre-approve apps with ALLOW rules before flipping.
    """
    device.default_app_policy = payload.default_app_policy
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="DEVICE_POLICY_CHANGED",
        resource_type="device",
        resource_id=str(device_id),
        ip_address=_client_ip(request),
    )
    await db.commit()

    return DevicePolicyResponse(
        device_id=device_id, default_app_policy=device.default_app_policy
    )


@router.post("/devices/{device_id}/rule-events", response_model=AppRuleEventResponse)
async def report_rule_event(
    device_id: uuid.UUID,
    payload: ReportRuleEventRequest,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_supervised_owner_of_device),
) -> AppRuleEventResponse:
    """Insert-only: the device reports that it just enforced a rule. No audit entry — this is
    evidence of enforcement for the tutor to review, not an action to review after the fact
    (same reasoning as the Sprint 7 usage sync).
    """
    event = AppRuleEvent(
        device_id=device_id,
        package_name=payload.package_name,
        rule_type_applied=payload.rule_type_applied,
        occurred_at=payload.occurred_at,
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)

    return AppRuleEventResponse(
        id=event.id,
        package_name=event.package_name,
        rule_type_applied=event.rule_type_applied,
        occurred_at=event.occurred_at,
        received_at=event.received_at,
    )


@router.get("/devices/{device_id}/rule-events", response_model=AppRuleEventListResponse)
async def list_rule_events(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> AppRuleEventListResponse:
    events = (
        await db.execute(
            select(AppRuleEvent)
            .where(AppRuleEvent.device_id == device_id)
            .order_by(AppRuleEvent.occurred_at.desc())
        )
    ).scalars().all()

    return AppRuleEventListResponse(
        events=[
            AppRuleEventResponse(
                id=event.id,
                package_name=event.package_name,
                rule_type_applied=event.rule_type_applied,
                occurred_at=event.occurred_at,
                received_at=event.received_at,
            )
            for event in events
        ]
    )

import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, require_tutor_of_device
from app.db.session import get_db
from app.models import AppCategoryAssignment, CategoryRule, Device, User
from app.schemas.category import (
    CategoryAssignmentListResponse,
    CategoryAssignmentResponse,
    CategoryRuleListResponse,
    CategoryRuleResponse,
    DeleteCategoryAssignmentResponse,
    DeleteCategoryRuleResponse,
    UpsertCategoryAssignmentRequest,
    UpsertCategoryRuleRequest,
)
from app.services.audit import record_audit_event
from app.services.realtime import notify_rules_changed

router = APIRouter(tags=["categories"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _to_assignment_response(assignment: AppCategoryAssignment) -> CategoryAssignmentResponse:
    return CategoryAssignmentResponse(
        id=assignment.id,
        package_name=assignment.package_name,
        category=assignment.category,
        created_at=assignment.created_at,
        updated_at=assignment.updated_at,
    )


def _to_category_rule_response(rule: CategoryRule) -> CategoryRuleResponse:
    return CategoryRuleResponse(
        id=rule.id,
        category=rule.category,
        rule_type=rule.rule_type,
        daily_limit_minutes=rule.daily_limit_minutes,
        weekly_limit_minutes=rule.weekly_limit_minutes,
        schedule_start_minute=rule.schedule_start_minute,
        schedule_end_minute=rule.schedule_end_minute,
        schedule_days_mask=rule.schedule_days_mask,
        created_at=rule.created_at,
        updated_at=rule.updated_at,
    )


# ------------------------------------------------------------------------- app-category assignments


@router.post(
    "/devices/{device_id}/app-categories", response_model=CategoryAssignmentResponse
)
async def upsert_app_category(
    device_id: uuid.UUID,
    payload: UpsertCategoryAssignmentRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> CategoryAssignmentResponse:
    """Assigning a category to a package that already has one replaces it (upsert) — same
    reasoning as AppRule: an app has at most one category per device.
    """
    existing = (
        await db.execute(
            select(AppCategoryAssignment).where(
                AppCategoryAssignment.device_id == device_id,
                AppCategoryAssignment.package_name == payload.package_name,
            )
        )
    ).scalar_one_or_none()

    if existing is None:
        assignment = AppCategoryAssignment(
            device_id=device_id, package_name=payload.package_name, category=payload.category
        )
        db.add(assignment)
        action = "APP_CATEGORY_ASSIGNED"
    else:
        assignment = existing
        assignment.category = payload.category
        action = "APP_CATEGORY_REASSIGNED"

    await db.flush()
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action=action,
        resource_type="app_category_assignment",
        resource_id=str(assignment.id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(assignment)
    await notify_rules_changed(device_id, device.fcm_token)

    return _to_assignment_response(assignment)


@router.get(
    "/devices/{device_id}/app-categories", response_model=CategoryAssignmentListResponse
)
async def list_app_categories(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> CategoryAssignmentListResponse:
    assignments = (
        await db.execute(
            select(AppCategoryAssignment)
            .where(AppCategoryAssignment.device_id == device_id)
            .order_by(AppCategoryAssignment.package_name)
        )
    ).scalars().all()

    return CategoryAssignmentListResponse(
        assignments=[_to_assignment_response(assignment) for assignment in assignments]
    )


@router.delete(
    "/devices/{device_id}/app-categories/{assignment_id}",
    response_model=DeleteCategoryAssignmentResponse,
)
async def delete_app_category(
    device_id: uuid.UUID,
    assignment_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> DeleteCategoryAssignmentResponse:
    assignment = (
        await db.execute(
            select(AppCategoryAssignment).where(
                AppCategoryAssignment.id == assignment_id,
                AppCategoryAssignment.device_id == device_id,
            )
        )
    ).scalar_one_or_none()
    if assignment is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="app_category_assignment_not_found")

    now = datetime.now(UTC)
    package_name = assignment.package_name
    await db.delete(assignment)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="APP_CATEGORY_UNASSIGNED",
        resource_type="app_category_assignment",
        resource_id=str(assignment_id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await notify_rules_changed(device_id, device.fcm_token)

    return DeleteCategoryAssignmentResponse(
        assignment_id=assignment_id, package_name=package_name, deleted_at=now
    )


# --------------------------------------------------------------------------------- category rules


@router.post("/devices/{device_id}/category-rules", response_model=CategoryRuleResponse)
async def upsert_category_rule(
    device_id: uuid.UUID,
    payload: UpsertCategoryRuleRequest,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> CategoryRuleResponse:
    existing = (
        await db.execute(
            select(CategoryRule).where(
                CategoryRule.device_id == device_id, CategoryRule.category == payload.category
            )
        )
    ).scalar_one_or_none()

    if existing is None:
        rule = CategoryRule(
            device_id=device_id,
            category=payload.category,
            rule_type=payload.rule_type,
            daily_limit_minutes=payload.daily_limit_minutes,
            weekly_limit_minutes=payload.weekly_limit_minutes,
            schedule_start_minute=payload.schedule_start_minute,
            schedule_end_minute=payload.schedule_end_minute,
            schedule_days_mask=payload.schedule_days_mask,
        )
        db.add(rule)
        action = "CATEGORY_RULE_CREATED"
    else:
        rule = existing
        rule.rule_type = payload.rule_type
        rule.daily_limit_minutes = payload.daily_limit_minutes
        rule.weekly_limit_minutes = payload.weekly_limit_minutes
        rule.schedule_start_minute = payload.schedule_start_minute
        rule.schedule_end_minute = payload.schedule_end_minute
        rule.schedule_days_mask = payload.schedule_days_mask
        action = "CATEGORY_RULE_UPDATED"

    await db.flush()
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action=action,
        resource_type="category_rule",
        resource_id=str(rule.id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await db.refresh(rule)
    await notify_rules_changed(device_id, device.fcm_token)

    return _to_category_rule_response(rule)


@router.get("/devices/{device_id}/category-rules", response_model=CategoryRuleListResponse)
async def list_category_rules(
    device_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _device: Device = Depends(require_tutor_of_device),
) -> CategoryRuleListResponse:
    rules = (
        await db.execute(
            select(CategoryRule)
            .where(CategoryRule.device_id == device_id)
            .order_by(CategoryRule.category)
        )
    ).scalars().all()

    return CategoryRuleListResponse(
        category_rules=[_to_category_rule_response(rule) for rule in rules]
    )


@router.delete(
    "/devices/{device_id}/category-rules/{category_rule_id}",
    response_model=DeleteCategoryRuleResponse,
)
async def delete_category_rule(
    device_id: uuid.UUID,
    category_rule_id: uuid.UUID,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    device: Device = Depends(require_tutor_of_device),
) -> DeleteCategoryRuleResponse:
    rule = (
        await db.execute(
            select(CategoryRule).where(
                CategoryRule.id == category_rule_id, CategoryRule.device_id == device_id
            )
        )
    ).scalar_one_or_none()
    if rule is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="category_rule_not_found")

    now = datetime.now(UTC)
    category = rule.category
    await db.delete(rule)
    await record_audit_event(
        db,
        actor_user_id=current_user.id,
        action="CATEGORY_RULE_DELETED",
        resource_type="category_rule",
        resource_id=str(category_rule_id),
        ip_address=_client_ip(request),
    )
    await db.commit()
    await notify_rules_changed(device_id, device.fcm_token)

    return DeleteCategoryRuleResponse(
        category_rule_id=category_rule_id, category=category, deleted_at=now
    )

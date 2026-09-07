import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, model_validator

Category = Literal[
    "SOCIAL_MEDIA",
    "GAMES",
    "STREAMING",
    "EDUCATION",
    "PRODUCTIVITY",
    "COMMUNICATION",
    "NEWS",
    "SHOPPING",
    "FINANCE",
    "UTILITIES",
    "ADULT_CONTENT",
]

# Deliberately not reusing app.schemas.rule.RuleType: category rules don't have ALLOW's Sprint-8
# "no effect yet" history, but the wire values are identical, so a shared Literal alias keeps the
# two from silently drifting apart while staying two distinct, independently-editable schemas.
CategoryRuleType = Literal["ALLOW", "BLOCK", "DAILY_LIMIT", "SCHEDULE"]


class UpsertCategoryAssignmentRequest(BaseModel):
    package_name: str = Field(min_length=1, max_length=255)
    category: Category


class CategoryAssignmentResponse(BaseModel):
    id: uuid.UUID
    package_name: str
    category: Category
    created_at: datetime
    updated_at: datetime


class CategoryAssignmentListResponse(BaseModel):
    assignments: list[CategoryAssignmentResponse]


class DeleteCategoryAssignmentResponse(BaseModel):
    assignment_id: uuid.UUID
    package_name: str
    deleted_at: datetime


class UpsertCategoryRuleRequest(BaseModel):
    """Same validation shape as UpsertAppRuleRequest (app/schemas/rule.py) — category_rules is
    the same rule shape, just keyed by category instead of package_name.
    """

    category: Category
    rule_type: CategoryRuleType
    daily_limit_minutes: int | None = Field(default=None, gt=0)
    schedule_start_minute: int | None = Field(default=None, ge=0, le=1439)
    schedule_end_minute: int | None = Field(default=None, ge=0, le=1439)
    schedule_days_mask: int | None = Field(default=None, ge=1, le=127)

    @model_validator(mode="after")
    def _require_fields_for_rule_type(self) -> "UpsertCategoryRuleRequest":
        if self.rule_type == "DAILY_LIMIT" and self.daily_limit_minutes is None:
            raise ValueError("daily_limit_minutes is required when rule_type is DAILY_LIMIT")
        if self.rule_type == "SCHEDULE" and (
            self.schedule_start_minute is None
            or self.schedule_end_minute is None
            or self.schedule_days_mask is None
        ):
            raise ValueError(
                "schedule_start_minute, schedule_end_minute and schedule_days_mask are all "
                "required when rule_type is SCHEDULE"
            )
        return self


class CategoryRuleResponse(BaseModel):
    id: uuid.UUID
    category: Category
    rule_type: CategoryRuleType
    daily_limit_minutes: int | None
    schedule_start_minute: int | None
    schedule_end_minute: int | None
    schedule_days_mask: int | None
    created_at: datetime
    updated_at: datetime


class CategoryRuleListResponse(BaseModel):
    category_rules: list[CategoryRuleResponse]


class DeleteCategoryRuleResponse(BaseModel):
    category_rule_id: uuid.UUID
    category: Category
    deleted_at: datetime

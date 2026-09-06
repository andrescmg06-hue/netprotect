import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, model_validator

RuleType = Literal["ALLOW", "BLOCK", "DAILY_LIMIT", "SCHEDULE"]

# What a device does with an app that has no rule: ALLOW = blocklist mode (Sprint 8 behavior),
# BLOCK = allowlist mode (only approved apps run).
DefaultAppPolicy = Literal["ALLOW", "BLOCK"]

# Includes DEFAULT_POLICY, which is not a rule type: an app can be blocked by the device's
# default policy without any rule of its own. See AppRuleEvent in app/models/rule.py.
AppliedRuleType = Literal["ALLOW", "BLOCK", "DAILY_LIMIT", "SCHEDULE", "DEFAULT_POLICY"]


class UpsertAppRuleRequest(BaseModel):
    """Creating a rule for a package that already has one replaces it — see AppRule's docstring
    for why this project doesn't stack multiple rules per (device, package) yet.
    """

    package_name: str = Field(min_length=1, max_length=255)
    rule_type: RuleType
    daily_limit_minutes: int | None = Field(default=None, gt=0)
    # Minutes since local midnight on the device's own clock — see AppRule for why this isn't
    # normalized to UTC yet. start > end is a valid overnight window (e.g. 22:00-06:00).
    schedule_start_minute: int | None = Field(default=None, ge=0, le=1439)
    schedule_end_minute: int | None = Field(default=None, ge=0, le=1439)
    # Bitmask, bit 0 = Monday ... bit 6 = Sunday. Never 0: a schedule with no days would
    # silently never apply.
    schedule_days_mask: int | None = Field(default=None, ge=1, le=127)

    @model_validator(mode="after")
    def _require_fields_for_rule_type(self) -> "UpsertAppRuleRequest":
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


class AppRuleResponse(BaseModel):
    id: uuid.UUID
    package_name: str
    rule_type: RuleType
    daily_limit_minutes: int | None
    schedule_start_minute: int | None
    schedule_end_minute: int | None
    schedule_days_mask: int | None
    created_at: datetime
    updated_at: datetime


class AppRuleListResponse(BaseModel):
    rules: list[AppRuleResponse]


class ActiveRulesResponse(BaseModel):
    """What the supervised device needs to evaluate locally: the rules plus the fallback for
    any app that has none.
    """

    rules: list[AppRuleResponse]
    default_app_policy: DefaultAppPolicy


class DeleteAppRuleResponse(BaseModel):
    rule_id: uuid.UUID
    package_name: str
    deleted_at: datetime


class UpdateDevicePolicyRequest(BaseModel):
    default_app_policy: DefaultAppPolicy


class DevicePolicyResponse(BaseModel):
    device_id: uuid.UUID
    default_app_policy: DefaultAppPolicy


class ReportRuleEventRequest(BaseModel):
    package_name: str = Field(min_length=1, max_length=255)
    rule_type_applied: AppliedRuleType
    occurred_at: datetime


class AppRuleEventResponse(BaseModel):
    id: uuid.UUID
    package_name: str
    rule_type_applied: AppliedRuleType
    occurred_at: datetime
    received_at: datetime


class AppRuleEventListResponse(BaseModel):
    events: list[AppRuleEventResponse]

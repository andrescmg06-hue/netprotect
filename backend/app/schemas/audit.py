import uuid
from datetime import datetime

from pydantic import BaseModel

# Unlike MAX_HISTORY_EVENTS/MAX_ALERTS (response-size caps backstopped by a 90-day retention
# purge on their source tables), AuditLog has no retention at all — the plan calls it an
# "immutable record" (docs/planning/plan-desarrollo.md, Paso 21) — so a fixed cap without real
# paging would silently hide older rows forever as the table grows. Real limit/offset paging
# instead, capped at a sane page size.
MAX_AUDIT_PAGE_SIZE = 200
DEFAULT_AUDIT_PAGE_SIZE = 50

# Export has no paging UI, so it needs its own hard ceiling instead of trusting a caller-supplied
# limit — a filterless export from an account with years of activity shouldn't be free to stream
# an unbounded CSV.
MAX_AUDIT_EXPORT_ROWS = 10_000


class AuditLogResponse(BaseModel):
    """One row of the caller's own audit trail (actor_user_id == current_user.id — see
    app/api/v1/endpoints/audit.py for why the endpoint is scoped that way). actor_user_id itself
    is deliberately not included here: since the endpoint already filters to "my own actions",
    every row's actor is the caller, so echoing it back would be redundant.
    """

    id: uuid.UUID
    action: str
    resource_type: str | None
    resource_id: str | None
    ip_address: str | None
    created_at: datetime


class AuditLogListResponse(BaseModel):
    logs: list[AuditLogResponse]
    total: int
    limit: int
    offset: int

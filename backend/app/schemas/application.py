from datetime import date, datetime

from pydantic import BaseModel, Field

# A real phone realistically has a few hundred packages installed, system apps included.
# This caps a malformed or malicious sync payload without constraining any real device.
MAX_APPLICATIONS_PER_SYNC = 2000


class InstalledAppPayload(BaseModel):
    package_name: str = Field(min_length=1, max_length=255)
    app_label: str = Field(min_length=1, max_length=255)
    is_system_app: bool = False


class DailyUsagePayload(BaseModel):
    package_name: str = Field(min_length=1, max_length=255)
    foreground_seconds: int = Field(ge=0)


class SyncApplicationsRequest(BaseModel):
    """The device's local calendar day for every entry in daily_usage — one sync call reports
    one day's totals, recomputed and overwritten on each call rather than accumulated, since
    UsageStatsManager already returns a running total for the day, not a delta.
    """

    usage_date: date
    installed_apps: list[InstalledAppPayload] = Field(max_length=MAX_APPLICATIONS_PER_SYNC)
    daily_usage: list[DailyUsagePayload] = Field(max_length=MAX_APPLICATIONS_PER_SYNC)


class SyncApplicationsResponse(BaseModel):
    apps_synced: int
    apps_marked_uninstalled: int
    usage_rows_synced: int
    synced_at: datetime


class LatestUsageResponse(BaseModel):
    usage_date: date
    foreground_seconds: int


class DeviceApplicationResponse(BaseModel):
    package_name: str
    app_label: str
    is_system_app: bool
    first_seen_at: datetime
    last_seen_at: datetime
    uninstalled_at: datetime | None
    latest_usage: LatestUsageResponse | None


class DeviceApplicationListResponse(BaseModel):
    applications: list[DeviceApplicationResponse]

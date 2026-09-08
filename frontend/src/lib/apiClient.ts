export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";

export type TokenPair = {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
};

export type CurrentUser = {
  id: string;
  email: string;
  display_name: string | null;
  avatar_url: string | null;
};

async function parseJsonOrThrow<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

export function loginWithGoogle(idToken: string): Promise<TokenPair> {
  return fetch(`${API_BASE_URL}/api/v1/auth/google`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id_token: idToken }),
  }).then((response) => parseJsonOrThrow<TokenPair>(response));
}

export function refreshTokens(refreshToken: string): Promise<TokenPair> {
  return fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refresh_token: refreshToken }),
  }).then((response) => parseJsonOrThrow<TokenPair>(response));
}

export function logout(refreshToken: string): Promise<void> {
  return fetch(`${API_BASE_URL}/api/v1/auth/logout`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refresh_token: refreshToken }),
  }).then((response) => {
    if (!response.ok && response.status !== 204) {
      throw new Error(`HTTP ${response.status}`);
    }
  });
}

export function fetchCurrentUser(accessToken: string): Promise<CurrentUser> {
  return fetch(`${API_BASE_URL}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  }).then((response) => parseJsonOrThrow<CurrentUser>(response));
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

async function requestJson<T>(
  path: string,
  init: RequestInit,
  accessToken: string
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { ...init.headers, Authorization: `Bearer ${accessToken}` },
  });
  const bodyText = await response.text();
  const body = bodyText ? JSON.parse(bodyText) : null;

  if (!response.ok) {
    const detail = body && typeof body.detail === "string" ? body.detail : `HTTP ${response.status}`;
    throw new ApiError(detail, response.status);
  }

  return body as T;
}

export type DeviceStatus = {
  status: string;
  last_seen_at: string | null;
  last_sync_at: string | null;
};

/** What happens to an app with no rule: ALLOW is blocklist mode, BLOCK is allowlist mode
 * (only apps the tutor approved run).
 */
export type DefaultAppPolicy = "ALLOW" | "BLOCK";

/** A scheduled override of default_app_policy to BLOCK (Sprint 12) — same window shape as a
 * SCHEDULE rule. Doesn't touch AppRule/CategoryRule: an explicit rule still applies during
 * school hours exactly as it already does against the plain default policy.
 */
export type SchoolMode = {
  enabled: boolean;
  start_minute: number | null;
  end_minute: number | null;
  days_mask: number | null;
};

export type Device = {
  id: string;
  name: string;
  platform: string;
  os_version: string | null;
  app_version: string | null;
  timezone: string | null;
  linked_at: string;
  status: DeviceStatus;
  default_app_policy: DefaultAppPolicy;
  school_mode: SchoolMode;
};

export type GrantedRole = {
  role_code: string;
  granted_at: string;
};

/** The web panel is the tutor's dashboard: granting TUTOR is a transparent step here, not a
 * choice presented to the user (unlike Android, which is installed on both tutor and
 * supervised devices). The backend grant is unconditional and idempotent either way.
 */
export function ensureTutorRole(accessToken: string): Promise<GrantedRole> {
  return requestJson<GrantedRole>(
    "/api/v1/users/me/roles",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ role_code: "TUTOR" }),
    },
    accessToken
  );
}

export function listDevices(accessToken: string): Promise<{ devices: Device[] }> {
  return requestJson<{ devices: Device[] }>("/api/v1/devices", { method: "GET" }, accessToken);
}

export function renameDevice(accessToken: string, deviceId: string, name: string): Promise<Device> {
  return requestJson<Device>(
    `/api/v1/devices/${deviceId}`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name }),
    },
    accessToken
  );
}

export function unlinkDevice(
  accessToken: string,
  deviceId: string
): Promise<{ device_id: string; unlinked_at: string }> {
  return requestJson(`/api/v1/devices/${deviceId}/link`, { method: "DELETE" }, accessToken);
}

export type LatestUsage = {
  usage_date: string;
  foreground_seconds: number;
};

export type DeviceApplication = {
  package_name: string;
  app_label: string;
  is_system_app: boolean;
  first_seen_at: string;
  last_seen_at: string;
  uninstalled_at: string | null;
  latest_usage: LatestUsage | null;
};

export function listDeviceApplications(
  accessToken: string,
  deviceId: string
): Promise<{ applications: DeviceApplication[] }> {
  return requestJson<{ applications: DeviceApplication[] }>(
    `/api/v1/devices/${deviceId}/applications`,
    { method: "GET" },
    accessToken
  );
}

export type RuleType = "ALLOW" | "BLOCK" | "DAILY_LIMIT" | "WEEKLY_LIMIT" | "SCHEDULE";

export type AppRule = {
  id: string;
  package_name: string;
  rule_type: RuleType;
  daily_limit_minutes: number | null;
  weekly_limit_minutes: number | null;
  schedule_start_minute: number | null;
  schedule_end_minute: number | null;
  schedule_days_mask: number | null;
  created_at: string;
  updated_at: string;
};

export type UpsertAppRuleInput = {
  package_name: string;
  rule_type: RuleType;
  daily_limit_minutes?: number;
  weekly_limit_minutes?: number;
  schedule_start_minute?: number;
  schedule_end_minute?: number;
  schedule_days_mask?: number;
};

export function listAppRules(
  accessToken: string,
  deviceId: string
): Promise<{ rules: AppRule[] }> {
  return requestJson<{ rules: AppRule[] }>(
    `/api/v1/devices/${deviceId}/rules`,
    { method: "GET" },
    accessToken
  );
}

/** Creating a rule for a package that already has one replaces it — same upsert semantics as
 * the backend (app/models/rule.py): only one active rule per (device, package) exists.
 */
export function upsertAppRule(
  accessToken: string,
  deviceId: string,
  input: UpsertAppRuleInput
): Promise<AppRule> {
  return requestJson<AppRule>(
    `/api/v1/devices/${deviceId}/rules`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    },
    accessToken
  );
}

export function deleteAppRule(
  accessToken: string,
  deviceId: string,
  ruleId: string
): Promise<{ rule_id: string; package_name: string; deleted_at: string }> {
  return requestJson(`/api/v1/devices/${deviceId}/rules/${ruleId}`, { method: "DELETE" }, accessToken);
}

export function updateDevicePolicy(
  accessToken: string,
  deviceId: string,
  defaultAppPolicy: DefaultAppPolicy
): Promise<{ device_id: string; default_app_policy: DefaultAppPolicy }> {
  return requestJson(
    `/api/v1/devices/${deviceId}/policy`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ default_app_policy: defaultAppPolicy }),
    },
    accessToken
  );
}

export type UpdateSchoolModeInput = {
  enabled: boolean;
  start_minute?: number;
  end_minute?: number;
  days_mask?: number;
};

export function updateSchoolMode(
  accessToken: string,
  deviceId: string,
  input: UpdateSchoolModeInput
): Promise<SchoolMode> {
  return requestJson<SchoolMode>(
    `/api/v1/devices/${deviceId}/school-mode`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    },
    accessToken
  );
}

/** DEFAULT_POLICY, CATEGORY and SCHOOL_MODE are not rule types of their own: the app had no
 * AppRule, and was blocked by its category's rule, the device's default policy, or a scheduled
 * school-mode window. Kept separate so a tutor can tell these apart from a deliberate rule.
 */
export type AppliedRuleType = RuleType | "DEFAULT_POLICY" | "CATEGORY" | "SCHOOL_MODE";

export type AppRuleEvent = {
  id: string;
  package_name: string;
  rule_type_applied: AppliedRuleType;
  occurred_at: string;
  received_at: string;
};

export function listRuleEvents(
  accessToken: string,
  deviceId: string
): Promise<{ events: AppRuleEvent[] }> {
  return requestJson<{ events: AppRuleEvent[] }>(
    `/api/v1/devices/${deviceId}/rule-events`,
    { method: "GET" },
    accessToken
  );
}

/** Fixed 11-category catalog — see backend app/models/category.py and docs/sprint-10.md for why
 * these specific 11 (the original spec's list isn't in this repo, this is a documented decision).
 */
export type Category =
  | "SOCIAL_MEDIA"
  | "GAMES"
  | "STREAMING"
  | "EDUCATION"
  | "PRODUCTIVITY"
  | "COMMUNICATION"
  | "NEWS"
  | "SHOPPING"
  | "FINANCE"
  | "UTILITIES"
  | "ADULT_CONTENT";

export type CategoryAssignment = {
  id: string;
  package_name: string;
  category: Category;
  created_at: string;
  updated_at: string;
};

export function listAppCategories(
  accessToken: string,
  deviceId: string
): Promise<{ assignments: CategoryAssignment[] }> {
  return requestJson<{ assignments: CategoryAssignment[] }>(
    `/api/v1/devices/${deviceId}/app-categories`,
    { method: "GET" },
    accessToken
  );
}

/** Assigning a category to a package that already has one replaces it — an app has at most one
 * category per device, same upsert semantics as an AppRule.
 */
export function upsertAppCategory(
  accessToken: string,
  deviceId: string,
  packageName: string,
  category: Category
): Promise<CategoryAssignment> {
  return requestJson<CategoryAssignment>(
    `/api/v1/devices/${deviceId}/app-categories`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ package_name: packageName, category }),
    },
    accessToken
  );
}

export function deleteAppCategory(
  accessToken: string,
  deviceId: string,
  assignmentId: string
): Promise<{ assignment_id: string; package_name: string; deleted_at: string }> {
  return requestJson(
    `/api/v1/devices/${deviceId}/app-categories/${assignmentId}`,
    { method: "DELETE" },
    accessToken
  );
}

/** Same shape and validation as AppRule — a category rule only applies to a package that has no
 * AppRule of its own (see docs/sprint-10.md for the priority chain).
 */
export type CategoryRule = {
  id: string;
  category: Category;
  rule_type: RuleType;
  daily_limit_minutes: number | null;
  weekly_limit_minutes: number | null;
  schedule_start_minute: number | null;
  schedule_end_minute: number | null;
  schedule_days_mask: number | null;
  created_at: string;
  updated_at: string;
};

export type UpsertCategoryRuleInput = {
  category: Category;
  rule_type: RuleType;
  daily_limit_minutes?: number;
  weekly_limit_minutes?: number;
  schedule_start_minute?: number;
  schedule_end_minute?: number;
  schedule_days_mask?: number;
};

export function listCategoryRules(
  accessToken: string,
  deviceId: string
): Promise<{ category_rules: CategoryRule[] }> {
  return requestJson<{ category_rules: CategoryRule[] }>(
    `/api/v1/devices/${deviceId}/category-rules`,
    { method: "GET" },
    accessToken
  );
}

export function upsertCategoryRule(
  accessToken: string,
  deviceId: string,
  input: UpsertCategoryRuleInput
): Promise<CategoryRule> {
  return requestJson<CategoryRule>(
    `/api/v1/devices/${deviceId}/category-rules`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    },
    accessToken
  );
}

export function deleteCategoryRule(
  accessToken: string,
  deviceId: string,
  categoryRuleId: string
): Promise<{ category_rule_id: string; category: Category; deleted_at: string }> {
  return requestJson(
    `/api/v1/devices/${deviceId}/category-rules/${categoryRuleId}`,
    { method: "DELETE" },
    accessToken
  );
}

/** Sprint 13. latitude/longitude arrive already decrypted (app/core/crypto.py decrypts
 * server-side before the response is built) — the web panel never handles ciphertext.
 */
export type LocationReport = {
  id: string;
  latitude: number;
  longitude: number;
  accuracy_meters: number;
  captured_at: string;
  received_at: string;
};

/** null means the device has never reported a location, or every report has aged out of the
 * backend's retention window (location_retention_days) — both are shown the same way, there is
 * no way (and no need) for a tutor to tell them apart.
 */
export function getLatestLocation(
  accessToken: string,
  deviceId: string
): Promise<{ report: LocationReport | null }> {
  return requestJson<{ report: LocationReport | null }>(
    `/api/v1/devices/${deviceId}/location/latest`,
    { method: "GET" },
    accessToken
  );
}

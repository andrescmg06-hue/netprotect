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

export type Device = {
  id: string;
  name: string;
  platform: string;
  os_version: string | null;
  app_version: string | null;
  linked_at: string;
  status: DeviceStatus;
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

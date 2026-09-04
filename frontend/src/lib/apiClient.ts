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

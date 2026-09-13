/** Sprint 25: prepares real backend state for the E2E spec to point a browser at.
 *
 * `/auth/google` needs a real Google ID token, which nothing in this setup can produce (same
 * limit every sprint's auth work has documented) — so the *session* comes from
 * `backend/scripts/seed_test_session.py`, run inside the backend container by the CI step before
 * Playwright starts (see .github/workflows/ci.yml, job `e2e`), which mints a session the exact
 * same way a real login would (this project's own unmocked token/session code), just without
 * Google's network call in front of it.
 *
 * Everything from here on IS real HTTP against the real backend — no mocking, no stubbed
 * responses: this file logs in as that tutor and supervised pair over plain `fetch`, generates a
 * pairing code, redeems it, and creates one rule, so the dashboard the spec drives has real
 * content to assert on instead of empty states only.
 */
import { writeFileSync } from "node:fs";
import path from "node:path";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";

interface SeedOutput {
  tutor_access_token: string;
  tutor_refresh_token: string;
  supervised_access_token: string;
}

async function postJson(pathname: string, token: string, body?: unknown) {
  const response = await fetch(`${API_BASE_URL}/api/v1${pathname}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`${pathname} -> ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

export default async function globalSetup(): Promise<void> {
  const rawSeed = process.env.E2E_SEED_JSON;
  if (!rawSeed) {
    throw new Error(
      "E2E_SEED_JSON is not set — run backend/scripts/seed_test_session.py first and export " +
        "its stdout into that variable (see .github/workflows/ci.yml, job `e2e`)."
    );
  }
  const seed: SeedOutput = JSON.parse(rawSeed);

  await postJson("/users/me/roles", seed.tutor_access_token, { role_code: "TUTOR" });
  await postJson("/users/me/roles", seed.supervised_access_token, { role_code: "SUPERVISADO" });

  const { code } = await postJson("/pairing/codes", seed.tutor_access_token);
  const deviceName = `Playwright E2E ${Date.now()}`;
  const { device_id: deviceId } = await postJson("/pairing/redeem", seed.supervised_access_token, {
    code,
    device_instance_id: crypto.randomUUID(),
    device_name: deviceName,
    platform: "ANDROID",
    os_version: "16",
    app_version: "0.1.0",
  });

  const packageName = "com.instagram.android";
  await postJson(`/devices/${deviceId}/rules`, seed.tutor_access_token, {
    package_name: packageName,
    rule_type: "BLOCK",
  });

  // Handed to the spec via a plain JSON file rather than process.env: global-setup and the test
  // process are not guaranteed to share environment in every Playwright runner configuration,
  // but they do share the filesystem.
  writeFileSync(
    path.join(__dirname, ".session.json"),
    JSON.stringify({
      tutorRefreshToken: seed.tutor_refresh_token,
      deviceName,
      packageName,
    })
  );
}

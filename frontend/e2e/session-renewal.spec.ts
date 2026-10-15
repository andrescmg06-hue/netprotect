import { type Page, type Request, type Route, expect, test } from "@playwright/test";

/** Sprint 70 (B-05): the failure paths of AuthContext's scheduled access token renewal.
 *
 * Unlike dashboard.spec.ts, the backend here is SIMULATED with `page.route`, on purpose and only
 * for this file: a revoked refresh token and a network failure in the middle of a live session
 * cannot be produced against the real backend without spending the single rotating tutor session
 * global-setup seeds (dashboard.spec.ts needs it). The happy path — a real renewal against the
 * real backend — is covered there. Every `/api/v1/**` call is answered here, so nothing in this
 * file touches the backend or the seeded token.
 */

const STORAGE_KEY = "netprotect.refresh_token";

type RefreshReply = "ok" | "network_error" | "unauthorized";

function nav(page: Page) {
  return page.getByRole("navigation", { name: "Secciones del panel" });
}

function storedRefreshToken(page: Page) {
  return page.evaluate((key) => window.sessionStorage.getItem(key), STORAGE_KEY);
}

async function fakeBackend(page: Page, replies: RefreshReply[]) {
  const refreshRequests: Request[] = [];
  let issued = 0;

  await page.route("**/api/v1/**", async (route: Route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/auth/refresh") {
      refreshRequests.push(request);
      const reply = replies[refreshRequests.length - 1] ?? "unauthorized";
      if (reply === "network_error") {
        return route.abort("internetdisconnected");
      }
      if (reply === "unauthorized") {
        return route.fulfill({ status: 401, json: { detail: "invalid_refresh_token" } });
      }
      issued += 1;
      return route.fulfill({
        json: {
          access_token: `access-${issued}`,
          refresh_token: `refresh-${issued}`,
          token_type: "bearer",
          expires_in: 900,
        },
      });
    }
    if (pathname === "/api/v1/auth/me") {
      return route.fulfill({
        json: { id: "tutor-1", email: "tutor@example.com", display_name: "Tutor", avatar_url: null },
      });
    }
    if (pathname === "/api/v1/users/me/roles") {
      return route.fulfill({ json: { role_code: "TUTOR", granted_at: new Date(0).toISOString() } });
    }
    if (pathname === "/api/v1/devices") {
      return route.fulfill({ json: { devices: [] } });
    }
    return route.fulfill({ status: 404, json: { detail: "not_simulated" } });
  });

  return refreshRequests;
}

function sentRefreshToken(request: Request) {
  return (request.postDataJSON() as { refresh_token: string }).refresh_token;
}

async function openDashboard(page: Page) {
  await page.clock.install();
  await page.addInitScript((key) => {
    // Only the very first load starts from the seeded value; the app owns it afterwards.
    if (window.sessionStorage.getItem(key) === null) {
      window.sessionStorage.setItem(key, "refresh-0");
    }
  }, STORAGE_KEY);
  await page.goto("/");
  await expect(nav(page)).toBeVisible();
  await expect.poll(() => storedRefreshToken(page)).toBe("refresh-1");
}

test("a network failure while renewing keeps the session and retries", async ({ page }) => {
  const refreshRequests = await fakeBackend(page, ["ok", "network_error", "ok"]);
  await openDashboard(page);

  // Just past the renewal (due at 14:00), and well before its 30-second retry.
  await page.clock.runFor("14:05");
  await expect.poll(() => refreshRequests.length).toBe(2);
  // Not signed out, and the still-unspent refresh token is kept.
  await expect(nav(page)).toBeVisible();
  expect(await storedRefreshToken(page)).toBe("refresh-1");

  await page.clock.runFor("00:20");
  expect(refreshRequests, "the retry waits 30 s").toHaveLength(2);
  await page.clock.runFor("00:20");
  await expect.poll(() => refreshRequests.length).toBe(3);
  expect(sentRefreshToken(refreshRequests[2])).toBe("refresh-1");
  await expect.poll(() => storedRefreshToken(page)).toBe("refresh-2");
  await expect(nav(page)).toBeVisible();
});

test("a revoked refresh token returns to the login screen without looping", async ({ page }) => {
  const refreshRequests = await fakeBackend(page, ["ok", "unauthorized"]);
  await openDashboard(page);

  await page.clock.runFor("14:30");
  await expect.poll(() => refreshRequests.length).toBe(2);
  expect(sentRefreshToken(refreshRequests[1])).toBe("refresh-1");

  await expect(page.getByRole("heading", { name: "Inicia sesión" })).toBeVisible();
  await expect(nav(page)).toBeHidden();
  expect(await storedRefreshToken(page)).toBeNull();

  // Signed out for good: no further renewal attempts.
  await page.clock.runFor("30:00");
  expect(refreshRequests).toHaveLength(2);
});

test("returning to a tab whose token expired while asleep renews at once", async ({ page }) => {
  const refreshRequests = await fakeBackend(page, ["ok", "ok"]);
  await openDashboard(page);

  // A backgrounded tab: wall-clock time passes but its timers did not get to fire.
  await page.clock.setSystemTime(Date.now() + 20 * 60_000);
  expect(refreshRequests).toHaveLength(1);
  await page.evaluate(() => document.dispatchEvent(new Event("visibilitychange")));

  await expect.poll(() => refreshRequests.length).toBe(2);
  expect(sentRefreshToken(refreshRequests[1])).toBe("refresh-1");
  await expect.poll(() => storedRefreshToken(page)).toBe("refresh-2");
  await expect(nav(page)).toBeVisible();
});

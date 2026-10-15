import { readFileSync } from "node:fs";
import path from "node:path";

import { type Page, type Request, expect, test } from "@playwright/test";

/** Sprint 25: real browser, real backend (see global-setup.ts), against the production build
 * (`next build && node .next/standalone/server.js` — see playwright.config.ts). Seeds a tutor
 * session the same way a returning login would: `sessionStorage["netprotect.refresh_token"]` set
 * before any page script runs, so AuthContext's own mount effect drives the real
 * refresh -> fetchCurrentUser flow — nothing here talks to the backend directly except through
 * the rendered page.
 *
 * One test, not three: AuthContext's refresh token is rotating and single-use (Sprint 3 — each
 * `/auth/refresh` call invalidates the token it was given and issues a new one). The first
 * `page.goto("/")` in a fresh browser context spends the one seeded token; a second `goto` in a
 * *different* test/context would try to spend the already-rotated one and get
 * `invalid_refresh_token`, landing back on the login screen instead of the dashboard. Splitting
 * this into separate `test()` blocks that each re-seed sessionStorage looked right and failed for
 * exactly that reason the first time this file was written — staying in one continuous session
 * (`test.step` for readable reporting) is what the token's own lifecycle requires, not a
 * stylistic choice.
 */
const session = JSON.parse(
  readFileSync(path.join(__dirname, ".session.json"), "utf-8")
) as { tutorRefreshToken: string; deviceName: string; packageName: string };

/** Sprint 32: located by role, not by CSS class, so a visual redesign doesn't break the test. */
function nav(page: Page) {
  return page.getByRole("navigation", { name: "Secciones del panel" });
}

function title(page: Page) {
  return page.getByRole("heading", { level: 1 });
}

test("a returning tutor can navigate the whole dashboard and see real backend data", async ({
  page,
}) => {
  // Sprint 70 (B-05): the last step fast-forwards the browser clock past the access token's
  // 15-minute lifetime, so the clock is installed before the app loads. Until then it flows in
  // real time, so the earlier steps run exactly as before.
  await page.clock.install();

  const refreshRequests: Request[] = [];
  const alertResponses: { bearer: string | undefined; status: number }[] = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith("/api/v1/auth/refresh")) {
      refreshRequests.push(request);
    }
  });
  page.on("response", (response) => {
    const request = response.request();
    if (request.method() === "GET" && /\/api\/v1\/devices\/[^/]+\/alerts$/.test(request.url())) {
      alertResponses.push({ bearer: request.headers()["authorization"], status: response.status() });
    }
  });

  await page.addInitScript((token) => {
    window.sessionStorage.setItem("netprotect.refresh_token", token);
  }, session.tutorRefreshToken);

  await test.step("lands on the dashboard, not the login screen", async () => {
    await page.goto("/");
    await expect(nav(page)).toBeVisible();
    await expect(title(page)).toHaveText("Inicio");
  });

  await test.step("the sidebar switches sections and shows the real paired device", async () => {
    await nav(page).getByRole("button", { name: "Dispositivos", exact: true }).click();
    await expect(title(page)).toHaveText("Dispositivos");
    // The device paired in global-setup shows up for real — not a fixture, an actual row read
    // back from the backend this test's own setup wrote to. Sprint 34's master-detail layout
    // shows the name twice (list row, detail card) plus once more, hidden, in the unlink
    // <dialog>'s title — by role+name instead of by text so the row alone matches.
    await expect(page.getByRole("button", { name: session.deviceName })).toBeVisible();

    await nav(page).getByRole("button", { name: "Vinculación", exact: true }).click();
    await expect(title(page)).toHaveText("Vinculación");
  });

  await test.step("the rule created for that device is visible in its own panel", async () => {
    await nav(page).getByRole("button", { name: "Reglas por aplicación", exact: true }).click();
    await expect(title(page)).toHaveText("Reglas por aplicación");

    // "Reglas por aplicación" is per-device (dashboardSections.ts) — the device selector
    // defaults to whichever device loaded first, and global-setup paired exactly one, so no
    // selection is needed before the rule for it shows up. Sprint 35's own form repeats the same
    // package name inside its "Ej. ..." hint, so match the rule row's exact text, not the hint's.
    await expect(page.getByText(session.packageName, { exact: true })).toBeVisible();
  });

  // Sprint 70 (B-05): the panel used to spend the refresh token once, on mount, and never again —
  // 15 minutes later every call (the minute-by-minute alerts poll included) got 401. The backend
  // checks token expiry against its own real clock, so the old token would still be accepted
  // here: what this step proves is that the page itself renews, once per cycle, and switches its
  // calls to the new token, without reloading.
  await test.step("renews the access token before it expires and keeps the session", async () => {
    await expect.poll(() => alertResponses.length).toBeGreaterThan(0);
    expect(refreshRequests, "only the mount-time refresh so far").toHaveLength(1);

    for (let cycle = 1; cycle <= 2; cycle++) {
      const bearerBefore = alertResponses[alertResponses.length - 1].bearer;
      const seenAlerts = alertResponses.length;

      // expires_in is 900 s; the renewal is due 60 s before that.
      await page.clock.runFor("13:00");
      expect(refreshRequests, `cycle ${cycle}: no renewal before it is due`).toHaveLength(cycle);
      await page.clock.runFor("02:30");
      await expect
        .poll(() => refreshRequests.length, {
          message: `cycle ${cycle}: POST /auth/refresh after the access token's lifetime`,
          timeout: 10_000,
        })
        .toBe(cycle + 1);

      // The alerts poll re-runs with the new token (its effect depends on accessToken).
      await expect
        .poll(
          () => alertResponses.slice(seenAlerts).find((entry) => entry.bearer !== bearerBefore)?.status,
          { message: `cycle ${cycle}: alerts fetched with a new bearer token`, timeout: 10_000 }
        )
        .toBe(200);
      await expect(nav(page)).toBeVisible();

      // The rotated refresh token replaces the spent one in sessionStorage.
      const issued = (await (await refreshRequests[cycle].response())!.json()) as { refresh_token: string };
      expect(
        await page.evaluate(() => window.sessionStorage.getItem("netprotect.refresh_token"))
      ).toBe(issued.refresh_token);
      // One renewal per cycle: nothing else spent the token meanwhile.
      expect(refreshRequests).toHaveLength(cycle + 1);
    }
  });
});

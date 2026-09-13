import { readFileSync } from "node:fs";
import path from "node:path";

import { expect, test } from "@playwright/test";

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

test("a returning tutor can navigate the whole dashboard and see real backend data", async ({
  page,
}) => {
  await page.addInitScript((token) => {
    window.sessionStorage.setItem("netprotect.refresh_token", token);
  }, session.tutorRefreshToken);

  await test.step("lands on the dashboard, not the login screen", async () => {
    await page.goto("/");
    await expect(page.locator(".dashboard")).toBeVisible();
    await expect(page.locator(".dashboardTitle")).toHaveText("Resumen");
  });

  await test.step("the sidebar switches sections and shows the real paired device", async () => {
    await page.getByRole("button", { name: "Dispositivos", exact: true }).click();
    await expect(page.locator(".dashboardTitle")).toHaveText("Dispositivos");
    // The device paired in global-setup shows up for real — not a fixture, an actual row read
    // back from the backend this test's own setup wrote to.
    await expect(page.getByText(session.deviceName)).toBeVisible();

    await page.getByRole("button", { name: "Vinculación", exact: true }).click();
    await expect(page.locator(".dashboardTitle")).toHaveText("Vinculación");
  });

  await test.step("the rule created for that device is visible in its own panel", async () => {
    await page.getByRole("button", { name: "Reglas por aplicación", exact: true }).click();
    await expect(page.locator(".dashboardTitle")).toHaveText("Reglas por aplicación");

    // "Reglas por aplicación" is per-device (dashboardSections.ts) — the device switcher
    // defaults to whichever device loaded first, and global-setup paired exactly one, so no
    // selection is needed before the rule for it shows up.
    await expect(page.getByText(session.packageName)).toBeVisible();
  });
});

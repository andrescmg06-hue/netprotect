import { readFileSync } from "node:fs";
import path from "node:path";

import { type Page, expect, test } from "@playwright/test";

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
});

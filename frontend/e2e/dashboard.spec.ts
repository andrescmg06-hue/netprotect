import { readFileSync } from "node:fs";
import path from "node:path";

import { type Locator, type Page, expect, test } from "@playwright/test";

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
 * stylistic choice. The steps added in Sprint 60 (keyboard focus ring, every section opens, the
 * remote-view notice on a phone) share that same session for the same reason: they are further
 * steps of this one test, not separate tests.
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

/** Presses Tab until `target` is the focused element, so `:focus-visible` really applies (it does
 * for keyboard focus; `locator.focus()` does not guarantee it). Fails loudly after `maxTabs`
 * presses instead of looping or silently passing. */
async function tabTo(page: Page, target: Locator, maxTabs: number) {
  const isFocused = () => target.evaluate((element) => element === document.activeElement);
  for (let presses = 0; presses < maxTabs; presses += 1) {
    if (await isFocused()) return;
    await page.keyboard.press("Tab");
  }
  if (await isFocused()) return;
  throw new Error(`tabTo: the target was not reached with the keyboard after ${maxTabs} Tab presses`);
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

  await test.step("a primary button shows the keyboard focus ring, not just its resting shadow", async () => {
    // Regression for the Sprint 60 fix: `Button.primary` has its own resting box-shadow
    // (`0 1px 2px rgba(23, 105, 255, .25)`) that used to override the global `:focus-visible`
    // ring. Now `.primary:focus-visible` sets `--focus-ring`
    // (`0 0 0 2px #f5f3ee, 0 0 0 4px #1769ff`). Computed, the ring serialises with the opaque
    // `rgb(23, 105, 255)` while the resting shadow only ever carries that colour with alpha
    // (`rgba(...)` / `color(srgb ...)`), so containing the opaque form means the ring won.
    // "Generar código" is the primary button of Vinculación when no code is active (the idle
    // state, which is always the case on a fresh mount); it is only focused, never clicked, so
    // no pairing code is generated.
    await nav(page).getByRole("button", { name: "Vinculación", exact: true }).click();
    await expect(title(page)).toHaveText("Vinculación");

    const primary = page.getByRole("main").getByRole("button", { name: "Generar código", exact: true });
    await expect(primary).toBeVisible();
    await tabTo(page, primary, 80);

    const boxShadow = await primary.evaluate((element) => getComputedStyle(element).boxShadow);
    expect(boxShadow).toContain("rgb(23, 105, 255)");
    expect(boxShadow).not.toMatch(/0px 1px 2px/);
  });

  await test.step("every section of the sidebar opens with its own title", async () => {
    // Only titles (and the absence of an error alert) are asserted: the seeded device has no
    // location, history or alerts, so no data is expected. The alert check waits for the network
    // to settle first so a late failed request cannot slip past it. Sections already visited by
    // the steps above (Inicio, Dispositivos, Vinculación, Reglas por aplicación) are not repeated.
    // "Alertas" is matched by prefix because its nav button gains a ", N sin leer" suffix when
    // the device has unread alerts; every other label is matched exactly.
    const sections: { label: string; name: string | RegExp }[] = [
      { label: "Perfil y sesión", name: "Perfil y sesión" },
      { label: "Apps del dispositivo", name: "Apps del dispositivo" },
      { label: "Política y horario escolar", name: "Política y horario escolar" },
      { label: "Categorías", name: "Categorías" },
      { label: "Geocercas", name: "Geocercas" },
      { label: "Ubicación", name: "Ubicación" },
      { label: "Historial", name: "Historial" },
      { label: "Estadísticas", name: "Estadísticas" },
      { label: "Alertas", name: /^Alertas(,|$)/ },
      { label: "Silenciadas", name: "Silenciadas" },
      { label: "Auditoría", name: "Auditoría" },
      // Last on purpose: the next step continues on this section.
      { label: "Vista remota", name: "Vista remota" },
    ];

    for (const { label, name } of sections) {
      await nav(page)
        .getByRole("button", { name, exact: typeof name === "string" })
        .click();
      await expect(title(page), `section "${label}"`).toHaveText(label);
      await page.waitForLoadState("networkidle");
      await expect(page.getByRole("main").getByRole("alert"), `section "${label}" shows an error`).toHaveCount(0);
    }
  });

  await test.step("the remote view keeps its consent notice and request button on a phone", async () => {
    // A Sprint 59 security review could not rule out that some CSS hides the consent notice on
    // small screens, which would let a tutor request a screen share without seeing that the
    // supervised person must accept. Resized in place (no navigation, same session). The check
    // is `toBeInViewport` after scrolling, which, unlike `toBeVisible`, also fails when an
    // ancestor clips the element away.
    const original = page.viewportSize();
    await page.setViewportSize({ width: 390, height: 844 });
    try {
      const notice = page.getByText("La transmisión solo empieza si la persona supervisada acepta");
      const request = page.getByRole("button", { name: "Solicitar ver pantalla", exact: true });

      for (const element of [notice, request]) {
        await element.scrollIntoViewIfNeeded();
        await expect(element).toBeVisible();
        await expect(element).toBeInViewport();
      }
    } finally {
      if (original) await page.setViewportSize(original);
    }
  });
});

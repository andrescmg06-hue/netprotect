import { expect, test } from "@playwright/test";

/** Sprint 60: the signed-out landing view, in a real browser against the production build.
 *
 * Deliberately seeds nothing: dashboard.spec.ts has to spend the one rotating, single-use refresh
 * token global-setup minted, so this test must never touch it. With empty sessionStorage the app
 * has no session to restore and renders the login screen.
 *
 * It does not assert the Google button: the CI web build has no NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID,
 * so GoogleButton renders its "missing configuration" alert instead (a real Google sign-in cannot
 * be exercised here anyway). Elements are located by role and name, not by CSS class.
 */
test("a visitor without a session sees the login screen, not the dashboard", async ({ page }) => {
  await page.goto("/");

  // The <h1> is "Inicia <br /> sesión", so tolerate any whitespace between the words.
  await expect(page.getByRole("heading", { level: 1, name: /Inicia\s+sesión/ })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Secciones del panel" })).toHaveCount(0);
});

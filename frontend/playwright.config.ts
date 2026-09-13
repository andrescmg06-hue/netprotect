import { defineConfig } from "@playwright/test";

/** Sprint 25: E2E tests run against a production build (`npm run build && npm run start`), not
 * `next dev` — `next dev` compiles on demand and injects HMR, so a test that passes there but
 * fails against what actually gets deployed (Sprint 26) would be worse than no test at all. CI
 * starts that server itself (see .github/workflows/ci.yml, job `e2e`); `webServer` here lets a
 * human run `npx playwright test` locally without repeating that setup by hand.
 */
export default defineConfig({
  testDir: "./e2e",
  globalSetup: "./e2e/global-setup.ts",
  timeout: 30_000,
  fullyParallel: false,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
  },
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        // `next.config.ts` sets `output: "standalone"` (Sprint 24, for a lean production
        // Docker image — see frontend/Dockerfile) — with that flag, `next start`/`npm run
        // start` refuses to run at all ("does not work with standalone configuration"). The
        // standalone server also doesn't bundle its own static assets or `public/`; the
        // Dockerfile copies both next to `server.js` before running it, and this reproduces
        // that exact copy by hand so the E2E run serves the same artifact production would.
        command:
          "npm run build && " +
          "node -e \"const fs=require('fs');" +
          "fs.cpSync('public','.next/standalone/public',{recursive:true});" +
          "fs.cpSync('.next/static','.next/standalone/.next/static',{recursive:true})\" && " +
          "node .next/standalone/server.js",
        url: "http://localhost:3000",
        reuseExistingServer: false,
        timeout: 120_000,
        env: { PORT: "3000", HOSTNAME: "0.0.0.0" },
      },
});

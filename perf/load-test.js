import http from "k6/http";
import { check, sleep } from "k6";

/** Sprint 25: a repeatable reference load profile against the real backend in Docker, not a
 * breaking-point stress test — see docs/sprint-25.md's design decisions for why. This project has
 * no production infrastructure yet (Sprint 26), so any number from a real stress run would
 * describe this dev machine's container, not the eventual deployment; what's useful today is a
 * fixed, reusable script whose output can be diffed against a later run after Sprint 26, not a
 * verdict on capacity.
 *
 * Moderate, mixed read traffic against endpoints a tutor's session actually calls: the device
 * list, one device's detail, and that device's active rules (the same query the supervised app
 * polls). BASE_URL/ACCESS_TOKEN/DEVICE_ID come from the environment because minting a real
 * session needs backend/scripts/seed_test_session.py run inside the backend container first (see
 * perf/README.md) — this script only drives HTTP traffic, it doesn't authenticate on its own.
 */
const BASE_URL = __ENV.K6_BASE_URL || "http://localhost:8000";
const TUTOR_ACCESS_TOKEN = __ENV.K6_TUTOR_ACCESS_TOKEN;
// GET .../rules/active is the supervised device's own endpoint (require_supervised_owner_of_device
// — a tutor gets 404 from it, same anti-IDOR check as everywhere else), so it needs the
// supervised account's token, not the tutor's.
const SUPERVISED_ACCESS_TOKEN = __ENV.K6_SUPERVISED_ACCESS_TOKEN;
const DEVICE_ID = __ENV.K6_DEVICE_ID;

if (!TUTOR_ACCESS_TOKEN || !SUPERVISED_ACCESS_TOKEN || !DEVICE_ID) {
  throw new Error(
    "K6_TUTOR_ACCESS_TOKEN, K6_SUPERVISED_ACCESS_TOKEN and K6_DEVICE_ID must be set — see " +
      "perf/README.md for how to seed them."
  );
}

export const options = {
  stages: [
    { duration: "10s", target: 10 }, // ramp up
    { duration: "30s", target: 10 }, // hold — the actual measurement window
    { duration: "5s", target: 0 }, // ramp down
  ],
  thresholds: {
    // Informational trip-wires, not a pass/fail gate on this dev machine's hardware: a real
    // regression (a query that lost its index, say) should still show up as a loud failure here
    // rather than a number nobody reads in docs/sprint-25-evidence.md.
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

const tutorHeaders = { headers: { Authorization: `Bearer ${TUTOR_ACCESS_TOKEN}` } };
const supervisedHeaders = { headers: { Authorization: `Bearer ${SUPERVISED_ACCESS_TOKEN}` } };

export default function () {
  const list = http.get(`${BASE_URL}/api/v1/devices`, tutorHeaders);
  check(list, { "list devices: 200": (r) => r.status === 200 });

  const detail = http.get(`${BASE_URL}/api/v1/devices/${DEVICE_ID}`, tutorHeaders);
  check(detail, { "device detail: 200": (r) => r.status === 200 });

  const activeRules = http.get(
    `${BASE_URL}/api/v1/devices/${DEVICE_ID}/rules/active`,
    supervisedHeaders
  );
  check(activeRules, { "active rules: 200": (r) => r.status === 200 });

  sleep(1);
}

#!/usr/bin/env bash
# Sprint 25: seeds a real tutor+supervised pair and a linked device (same mechanism as
# api-tests/ and frontend/e2e/ — see backend/scripts/seed_test_session.py's docstring), then runs
# the k6 load profile in load-test.js against them. Assumes compose.test.yaml's db/redis/api_server
# are already up (see docs/sprint-25.md and .github/workflows/ci.yml, job `performance`).
set -euo pipefail

cd "$(dirname "$0")/.."

BASE_URL="${K6_BASE_URL:-http://localhost:8000}"

seed=$(docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py)
tutor_token=$(echo "$seed" | node -e "process.stdout.write(JSON.parse(require('fs').readFileSync(0)).tutor_access_token)")
supervised_token=$(echo "$seed" | node -e "process.stdout.write(JSON.parse(require('fs').readFileSync(0)).supervised_access_token)")

curl -sf -X POST "$BASE_URL/api/v1/users/me/roles" \
  -H "Authorization: Bearer $tutor_token" -H "Content-Type: application/json" \
  -d '{"role_code": "TUTOR"}' >/dev/null

curl -sf -X POST "$BASE_URL/api/v1/users/me/roles" \
  -H "Authorization: Bearer $supervised_token" -H "Content-Type: application/json" \
  -d '{"role_code": "SUPERVISADO"}' >/dev/null

code=$(curl -sf -X POST "$BASE_URL/api/v1/pairing/codes" -H "Authorization: Bearer $tutor_token" \
  | node -e "process.stdout.write(JSON.parse(require('fs').readFileSync(0)).code)")

device_id=$(curl -sf -X POST "$BASE_URL/api/v1/pairing/redeem" \
  -H "Authorization: Bearer $supervised_token" -H "Content-Type: application/json" \
  -d "{\"code\": \"$code\", \"device_instance_id\": \"$(node -e 'process.stdout.write(crypto.randomUUID())')\", \"device_name\": \"k6 perf device\", \"platform\": \"ANDROID\", \"os_version\": \"16\", \"app_version\": \"0.1.0\"}" \
  | node -e "process.stdout.write(JSON.parse(require('fs').readFileSync(0)).device_id)")

echo "Seeded device $device_id — running k6 against $BASE_URL"

K6_BASE_URL="$BASE_URL" \
K6_TUTOR_ACCESS_TOKEN="$tutor_token" \
K6_SUPERVISED_ACCESS_TOKEN="$supervised_token" \
K6_DEVICE_ID="$device_id" \
  "${K6_BIN:-k6}" run perf/load-test.js

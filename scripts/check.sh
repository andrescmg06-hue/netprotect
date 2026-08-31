#!/usr/bin/env sh
set -eu

python -m compileall backend/app backend/tests
python -m json.tool frontend/package.json >/dev/null

echo "Static Sprint 1 checks passed."

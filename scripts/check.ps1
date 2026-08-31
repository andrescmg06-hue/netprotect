$ErrorActionPreference = "Stop"

python -m compileall backend/app backend/tests
Get-Content frontend/package.json -Raw | ConvertFrom-Json | Out-Null

Write-Host "Static Sprint 1 checks passed."

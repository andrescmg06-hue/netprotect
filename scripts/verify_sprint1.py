#!/usr/bin/env python3
import json
import sys
import urllib.error
import urllib.request

CHECKS = {
    "API": "http://localhost:8000/api/v1/health",
    "Infraestructura": "http://localhost:8000/api/v1/health/ready",
    "Web": "http://localhost:3000",
}


def get(url: str) -> tuple[int, str]:
    request = urllib.request.Request(url, headers={"User-Agent": "NetProtect-Sprint1-Check/1.0"})
    with urllib.request.urlopen(request, timeout=5) as response:
        return response.status, response.read().decode("utf-8")


def main() -> int:
    failed = False
    for name, url in CHECKS.items():
        try:
            status, body = get(url)
            if status != 200:
                raise RuntimeError(f"HTTP {status}")
            if name == "Infraestructura":
                payload = json.loads(body)
                expected = {
                    "status": "ready",
                    "backend": "connected",
                    "database": "connected",
                    "redis": "connected",
                }
                if payload != expected:
                    raise RuntimeError(f"respuesta inesperada: {payload}")
            print(f"[OK] {name}: {url}")
        except (urllib.error.URLError, RuntimeError, json.JSONDecodeError) as exc:
            failed = True
            print(f"[ERROR] {name}: {exc}")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

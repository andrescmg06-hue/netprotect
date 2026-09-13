FROM python:3.13-slim AS base

# Sprint 25: PYTHONPATH=/app is for scripts/seed_test_session.py — without it,
# `python scripts/seed_test_session.py` fails with ModuleNotFoundError on `app`, because a script
# run by path gets its own directory on sys.path, not the working directory. uvicorn/alembic/
# pytest were never affected (each resolves `app` its own way), which is why this was never
# needed until a plain script showed up.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONPATH=/app

WORKDIR /app

RUN groupadd --system netprotect && useradd --system --gid netprotect --create-home netprotect

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["docker-entrypoint.sh"]

FROM base AS test
COPY requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements-dev.txt
COPY pyproject.toml ./
COPY app ./app
COPY alembic.ini ./
COPY alembic ./alembic
COPY tests ./tests
# Sprint 25: Newman/Playwright cannot mock Google's verification the way pytest's fixtures do
# (see scripts/seed_test_session.py's docstring) — this script is their only way to get a real
# session. Test-stage only: the runtime image below (what actually gets deployed) never sees it.
COPY scripts ./scripts
USER netprotect
CMD ["pytest", "-q"]

FROM base AS runtime
COPY app ./app
COPY alembic.ini ./
COPY alembic ./alembic
USER netprotect
EXPOSE 8000
# Sprint 26: --forwarded-allow-ips=* was verified for real to be *necessary*, not just belt-and-
# suspenders — uvicorn's --proxy-headers only trusts X-Forwarded-Proto from 127.0.0.1 by default,
# and Caddy (compose.prod.yaml) reaches backend over the `edge` Docker network under its own
# container IP, not loopback. Without this flag every request arrived at
# enforcement_middleware (app/main.py) looking like plain HTTP even though Caddy had already
# terminated real TLS in front of it, and the `https_required` check rejected 100% of production
# traffic (found by actually curling through Caddy in docs/sprint-26-evidence.md, not by reading
# the flag's docs). "*" is safe specifically because backend has no other ingress in this
# topology: compose.prod.yaml gives it no published host port, so the only things that can ever
# connect to it are other containers already on `private`/`edge` — Caddy, and Prometheus for
# /metrics (which never sends this header at all).
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers", "--forwarded-allow-ips=*"]

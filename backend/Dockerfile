FROM python:3.13-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

RUN groupadd --system netprotect && useradd --system --gid netprotect --create-home netprotect

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

FROM base AS test
COPY requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements-dev.txt
COPY pyproject.toml ./
COPY app ./app
COPY alembic.ini ./
COPY alembic ./alembic
COPY tests ./tests
USER netprotect
CMD ["pytest", "-q"]

FROM base AS runtime
COPY app ./app
COPY alembic.ini ./
COPY alembic ./alembic
USER netprotect
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]

# NetProtect

**NetProtect: Aplicación de seguridad informática para la protección y supervisión de la navegación web**

NetProtect será una plataforma de seguridad digital y control parental con una **única aplicación Android** que posteriormente operará como Tutor o Supervisado según la identidad y autorización verificadas por el backend. El tutor también dispondrá de un panel web conectado a la misma API.

Este repositorio corresponde al **Sprint 1 — Arquitectura y entorno**.

## Incremento funcional del Sprint 1

```text
Android ───────┐
               ├──> FastAPI ───> PostgreSQL
Web ───────────┘       │
                       └───────> Redis
```

La aplicación Android y la web consultan `/api/v1/health/ready`. El backend sólo devuelve `ready` cuando puede acceder a PostgreSQL y Redis. Por tanto, el sprint permite demostrar las rutas:

- `Android → Backend → PostgreSQL`.
- `Web → Backend → PostgreSQL`.
- `Backend → Redis` como infraestructura temporal preparada.

## Estructura

```text
netprotect/
├── backend/              FastAPI, configuración y pruebas
├── frontend/             Next.js + TypeScript
├── mobile/               Android Kotlin + Jetpack Compose
├── database/             documentación y futuras migraciones
├── docs/                 arquitectura, diagramas, planificación y seguridad
├── infra/                infraestructura cloud futura
├── docker/               documentación de contenedores
├── scripts/              verificaciones locales
├── tests/                estrategia de pruebas transversales
├── .github/workflows/    CI
├── compose.yaml          desarrollo
├── compose.test.yaml     integración/pruebas
└── compose.prod.yaml     base de producción
```

## Stack base

- Android: Kotlin, Android Studio, Jetpack Compose.
- Web: React, TypeScript, Next.js.
- Backend: Python, FastAPI, Pydantic, SQLAlchemy.
- Persistencia: PostgreSQL.
- Cache/datos temporales: Redis.
- Tiempo real futuro: WebSockets + Firebase Cloud Messaging.
- Autenticación futura: Google Identity / OIDC.
- Contenedores: Docker.
- CI/CD: GitHub Actions.

## Requisitos

Para la ruta recomendada:

- Git.
- Docker Desktop o Docker Engine con Compose v2.
- Android Studio para ejecutar la aplicación móvil.

Herramientas usadas por CI/desarrollo directo:

- Python 3.13.
- Node.js 22.
- JDK 17+.
- Gradle 8.13 si se compila Android por CLI sin wrapper.
- Android SDK API 36.

## Inicio rápido

```bash
cp .env.development.example .env
```

PowerShell:

```powershell
Copy-Item .env.development.example .env
```

Cambie al menos `POSTGRES_PASSWORD` y `REDIS_PASSWORD`, y actualice `DATABASE_URL` y `REDIS_URL` para que usen las mismas credenciales.

Luego:

```bash
docker compose up --build
```

Compruebe:

```text
Web:                 http://localhost:3000
API:                 http://localhost:8000/api/v1/health
PostgreSQL:          http://localhost:8000/api/v1/health/db
Redis:               http://localhost:8000/api/v1/health/redis
Readiness completo:  http://localhost:8000/api/v1/health/ready
Swagger desarrollo:  http://localhost:8000/docs
```

Puede ejecutar:

```bash
python scripts/verify_sprint1.py
```

## Android

Abra `mobile/` en Android Studio y ejecute el build `debug` en un emulador. Por defecto el cliente utiliza:

```text
http://10.0.2.2:8000
```

`10.0.2.2` es la ruta del emulador hacia el host local. Para un equipo físico configure `NETPROTECT_API_BASE_URL` en `mobile/local.properties`. Consulte `mobile/local.properties.example`.

El build `debug` permite cleartext exclusivamente para este escenario local. El build `release` lo deshabilita y debe usar HTTPS.

## Documentación previa a la implementación

La actualización del proyecto exige dejar establecida la arquitectura antes de avanzar en funcionalidades de negocio. Los artefactos están en:

- `docs/architecture.md`.
- `docs/diagrams/01-componentes.md`.
- `docs/diagrams/02-despliegue.md`.
- `docs/diagrams/03-flujo-autenticacion.md`.
- `docs/diagrams/04-flujo-vinculacion.md`.
- `docs/diagrams/05-modelo-datos-conceptual.md`.
- `docs/android/capability-matrix.md`.
- `docs/planning/product-backlog.md`.
- `docs/planning/user-stories.md`.
- `docs/planning/prioritization.md`.
- `docs/planning/roadmap.md`.
- `docs/planning/definition-of-done.md`.

## Alcance del Sprint 1

Incluye repositorio, proyectos base, PostgreSQL, Redis, Docker, ambientes, variables, CI/CD inicial, conexiones de infraestructura y documentación. No implementa todavía Google Login, RBAC, vinculación, reglas, ubicación, geocercas, tiempo real ni supervisión remota.

El detalle de criterios de aceptación y verificación se encuentra en `docs/sprint-01.md`.

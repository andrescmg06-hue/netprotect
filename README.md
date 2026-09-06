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
- `docs/diagrams/06-modelo-datos-fisico-sprint2.md`.
- `docs/android/capability-matrix.md`.
- `docs/planning/product-backlog.md`.
- `docs/planning/user-stories.md`.
- `docs/planning/prioritization.md`.
- `docs/planning/roadmap.md`.
- `docs/planning/definition-of-done.md`.

## Alcance del Sprint 1

Incluye repositorio, proyectos base, PostgreSQL, Redis, Docker, ambientes, variables, CI/CD inicial, conexiones de infraestructura y documentación. No implementa todavía Google Login, RBAC, vinculación, reglas, ubicación, geocercas, tiempo real ni supervisión remota.

El detalle de criterios de aceptación y verificación se encuentra en `docs/sprint-01.md`.

## Alcance del Sprint 2

Esquema de base de datos versionado con Alembic: `users`, `roles`, `user_roles`, `devices`, `device_status`, `tutor_devices`, `pairing_codes`, `sessions`, `audit_logs`. Las migraciones corren automáticamente (servicio `migrate`) antes de que el backend arranque, tanto en desarrollo como en pruebas. Google Login, RBAC funcional y vinculación real siguen pendientes de los sprints 3 a 5.

El detalle está en `docs/sprint-02.md`.

## Alcance del Sprint 3

Login con Google en los tres frentes: el backend verifica el ID token de Google y emite sus propios tokens (access JWT corto + refresh rotativo); la web usa Google Identity Services; Android usa Credential Manager y cifra el refresh token con una clave del Android Keystore. Requiere `GOOGLE_WEB_CLIENT_ID` y `JWT_SECRET` en `.env` (ver `.env.development.example`). RBAC funcional y vinculación por código siguen pendientes de los sprints 4 y 5.

El detalle está en `docs/sprint-03.md`.

## Alcance del Sprint 4

Selección de rol (TUTOR / SUPERVISADO) y la autorización real: `require_role` para acciones sin recurso y `require_tutor_of_device` para todo lo que apunte a un dispositivo concreto, que responde 404 tanto si el dispositivo no existe como si no es tuyo. Poseer un rol no concede acceso a nada por sí solo. Matriz de permisos en `docs/security-baseline.md`.

El detalle está en `docs/sprint-04.md`.

## Alcance del Sprint 5

Vinculación por código de 6 dígitos: generación con CSPRNG, vigencia de 3 minutos, un solo uso, revocación y desvinculación. El código se guarda como HMAC con una clave de servidor (`PAIRING_CODE_PEPPER`), nunca en claro; todos los fallos devuelven la misma respuesta; y los intentos se limitan por cuenta y por IP con Redis. Requiere `PAIRING_CODE_PEPPER` en `.env`.

El detalle está en `docs/sprint-05.md`.

## Alcance del Sprint 6

Gestión de dispositivos con estado real: listado, detalle, renombrado y desvinculación, disponibles en la app del tutor (Android) y en el panel web. El heartbeat del dispositivo supervisado actualiza `last_seen_at`; el estado `OFFLINE` se calcula al leer, comparándolo contra `device_offline_threshold_seconds`, sin ningún job en segundo plano. `GET /devices/me` permite al dispositivo supervisado confirmar su propio vínculo y quién lo supervisa, para no depender sólo de una caché local que podría quedar obsoleta. En Android, `HomeScreen` reemplaza a la pantalla de diagnóstico del Sprint 1 como punto de entrada: inicio de sesión → selección de rol → modo Tutor o modo Supervisado.

El detalle está en `docs/sprint-06.md`.

## Alcance del Sprint 7

Inventario de aplicaciones: el dispositivo supervisado reporta qué apps tiene instaladas (sólo las que tienen ícono propio) y cuánto se usó cada una hoy, visible en la app del tutor y en el panel web. Requiere dos permisos de Android verificados contra fuentes oficiales antes de implementar (`QUERY_ALL_PACKAGES` y `PACKAGE_USAGE_STATS`, éste último concedido por el usuario en Ajustes, no por un diálogo runtime) — ver `docs/android/capability-matrix.md` para el detalle, incluida la aclaración de que las políticas de Google Play sobre estos permisos sólo aplican si la app se publica en la tienda, no al instalarla por sideload como en este proyecto. Una app que deja de reportarse se marca como desinstalada sin borrar su historial de uso.

El detalle está en `docs/sprint-07.md`.

## Alcance del Sprint 8

Reglas de aplicaciones y bloqueo: el tutor define, por dispositivo y por app, una regla de
bloquear, permitir, límite diario de minutos u horario; el propio dispositivo supervisado la
descarga y la hace cumplir localmente, mostrando una pantalla de bloqueo propia y reportando cada
bloqueo aplicado al backend. Sin device owner, detectar qué app está en primer plano exige sondear
`UsageStatsManager` desde un foreground service (`specialUse`, verificado contra fuentes oficiales
antes de implementar) — ver `docs/android/capability-matrix.md` para el mecanismo completo y sus
límites explícitos (es reactivo, no preventivo, y el usuario puede revocarlo). Gestión de reglas e
historial de bloqueos disponibles en el panel web.

El detalle está en `docs/sprint-08.md`.

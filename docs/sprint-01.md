# Sprint 1 — Arquitectura y entorno

## Objetivo

Dejar construida y verificable la infraestructura sobre la que se desarrollará NetProtect, con repositorio, proyectos Android/Web/Backend, PostgreSQL, Redis, Docker, ambientes, variables de entorno, CI/CD y documentación de arquitectura.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-001 | Como equipo, quiero un monorepo modular para versionar todos los componentes de forma consistente. |
| HU-002 | Como desarrollador, quiero ambientes reproducibles para reducir diferencias entre equipos. |
| HU-003 | Como desarrollador Android, quiero comprobar la API y su acceso a PostgreSQL para validar la cadena móvil. |
| HU-004 | Como desarrollador web, quiero comprobar la API y su acceso a PostgreSQL para validar la cadena web. |
| HU-005 | Como equipo de seguridad, quiero separar secretos/configuración del código para evitar exposición accidental. |

## Criterios de aceptación

1. Existe un monorepo con `backend`, `frontend`, `mobile`, `database`, `docs`, `infra`, `tests` y `.github`.
2. `docker compose up --build` inicia PostgreSQL, Redis, backend y web.
3. `GET /api/v1/health` devuelve HTTP 200.
4. `GET /api/v1/health/db` confirma PostgreSQL.
5. `GET /api/v1/health/redis` confirma Redis.
6. `GET /api/v1/health/ready` sólo devuelve `ready` si PostgreSQL y Redis están disponibles.
7. Web consume `/api/v1/health/ready` y muestra el estado de infraestructura.
8. Android consume el mismo endpoint y muestra `Android → Backend`, `Backend → PostgreSQL` y `Backend → Redis`.
9. El build Android de desarrollo permite HTTP local; el build release lo deshabilita.
10. Desarrollo, pruebas y producción tienen configuraciones separadas.
11. CI ejecuta backend, frontend, Android e integración de infraestructura.
12. No existen credenciales reales versionadas.

## Tareas técnicas y estado

| Tarea | Estado | Evidencia |
|---|---|---|
| Arquitectura definitiva | Implementado | `docs/architecture.md` |
| Diagrama de componentes | Implementado | `docs/diagrams/01-componentes.md` |
| Diagrama de despliegue | Implementado | `docs/diagrams/02-despliegue.md` |
| Flujo de autenticación | Diseñado | `docs/diagrams/03-flujo-autenticacion.md` |
| Flujo de vinculación | Diseñado | `docs/diagrams/04-flujo-vinculacion.md` |
| ER conceptual | Diseñado | `docs/diagrams/05-modelo-datos-conceptual.md` |
| Matriz Android | Elaborada | `docs/android/capability-matrix.md` |
| Product Backlog | Elaborado | `docs/planning/product-backlog.md` |
| Historias/priorización/roadmap/DoD | Elaborado | `docs/planning/` |
| Proyecto FastAPI | Implementado | `backend/` |
| Proyecto Next.js | Implementado | `frontend/` |
| Proyecto Android Kotlin/Compose | Implementado | `mobile/` |
| PostgreSQL | Configurado | `compose*.yaml` |
| Redis | Configurado | `compose*.yaml` |
| Variables de entorno | Configuradas | `.env.*.example` |
| Ambientes dev/test/prod | Configurados | `compose*.yaml` |
| CI/CD inicial | Configurado | `.github/workflows/ci.yml` |
| Android → Backend → PostgreSQL | Implementado | app llama `/health/ready` |
| Web → Backend → PostgreSQL | Implementado | web llama `/health/ready` |

## Incremento

```text
                 ┌──────────────┐
                 │ PostgreSQL   │
                 └──────▲───────┘
                        │
Android ─────┐      ┌───┴───────┐
             ├─────►│ FastAPI   │
Web ─────────┘      └───┬───────┘
                        │
                 ┌──────▼───────┐
                 │ Redis        │
                 └──────────────┘
```

## Ejecución

```bash
cp .env.development.example .env
# Cambiar las contraseñas locales y mantener consistentes DATABASE_URL/REDIS_URL.
docker compose up --build
```

En Windows PowerShell:

```powershell
Copy-Item .env.development.example .env
docker compose up --build
```

## Verificación backend/web

```text
http://localhost:8000/api/v1/health
http://localhost:8000/api/v1/health/db
http://localhost:8000/api/v1/health/redis
http://localhost:8000/api/v1/health/ready
http://localhost:3000
```

También puede ejecutarse:

```bash
python scripts/verify_sprint1.py
```

## Verificación Android

1. Levantar Docker Compose.
2. Abrir `mobile/` en Android Studio.
3. Usar un emulador Android.
4. Ejecutar el build `debug`.
5. La API por defecto es `http://10.0.2.2:8000`.
6. La pantalla debe mostrar los tres estados `CONNECTED`.

Para un dispositivo físico, agregar `NETPROTECT_API_BASE_URL=http://IP_DEL_PC:8000` a `mobile/local.properties` y garantizar que el backend sea accesible desde la LAN de forma controlada.

### Combinación de versiones verificada (03/09/2026)

Compilado con éxito (`./gradlew test assembleDebug` y `assembleRelease`) y ejecutado en un emulador
Pixel 8 API 36 (Google APIs, x86_64) contra la pila de `compose.yaml`:

| Herramienta | Versión |
|---|---|
| Gradle (wrapper) | 9.3.0 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.06.00 |
| JDK | 21 (Temurin/Oracle) |
| compileSdk / targetSdk | 36 |

No fue necesario ajustar ninguna versión: la combinación que ya estaba en el repositorio compiló a la
primera. Esta tabla se actualiza si en un sprint futuro cambia alguna de estas versiones.

## Pruebas

Backend unitario:

```bash
cd backend
python -m venv .venv
source .venv/bin/activate   # Linux/macOS
pip install -r requirements.txt -r requirements-dev.txt
pytest -q -m "not integration"
ruff check app tests
```

Integración Docker:

```bash
docker compose -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend
docker compose -f compose.test.yaml down -v
```

Frontend:

```bash
cd frontend
npm install
npm run lint
npm run build
```

Android:

```bash
cd mobile
gradle test assembleDebug
```

## Seguridad del sprint

- No se implementan permisos sensibles de control parental todavía.
- Android sólo solicita `INTERNET`.
- Cleartext se permite únicamente en `debug` para desarrollo local.
- Producción requiere HTTPS.
- Redis y PostgreSQL no se exponen públicamente en la composición de producción.
- CORS y hosts son explícitos.
- Contenedores Web/Backend ejecutan procesos sin root.
- Los secretos reales deben ir en un gestor de secretos en producción.

## Fuera de alcance

Google Login, RBAC, vinculación, tablas funcionales del dominio, reglas, ubicación, geocercas, FCM/WebSockets y supervisión remota pertenecen a sprints posteriores.

## Definition of Done del Sprint 1

La implementación está preparada para cumplir el DoD. El cierre formal requiere ejecutar las verificaciones en un equipo con Docker y Android SDK/Gradle disponibles y registrar la evidencia de build/ejecución.

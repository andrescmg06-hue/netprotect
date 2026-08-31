# ADR 0002 — Stack tecnológico base

## Estado

Aceptado para el MVP.

## Decisión

- Android: Kotlin + Jetpack Compose, minSdk 26, target/compile SDK 36.
- Web: Next.js + React + TypeScript.
- Backend: Python + FastAPI + Pydantic + SQLAlchemy async.
- Persistencia: PostgreSQL.
- Cache/efímero: Redis.
- Tiempo real futuro: WebSockets + Firebase Cloud Messaging.
- Contenedores: Docker/Compose.
- CI/CD: GitHub Actions.

## Razón

El stack ofrece separación clara entre clientes y API, tipado/validación, acceso relacional robusto, soporte asíncrono y herramientas maduras. Redis se reserva para información temporal y coordinación; PostgreSQL conserva la autoridad de los datos de negocio.

## Consecuencias

El equipo debe mantener contratos de API estables, evitar lógica de autorización exclusiva en clientes y no introducir microservicios antes de que exista una necesidad demostrable de escala o aislamiento.

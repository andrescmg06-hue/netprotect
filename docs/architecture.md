# Arquitectura definitiva de NetProtect

## Decisión arquitectónica

NetProtect se construirá inicialmente como un **monolito modular** con una API central FastAPI. La aplicación Android y el panel web serán clientes de la misma API. PostgreSQL será la fuente de verdad persistente y Redis se utilizará únicamente para cache, datos efímeros, rate limiting, coordinación y presencia/estado temporal cuando esos módulos sean implementados.

Esta decisión evita la complejidad operacional prematura de microservicios y permite mantener límites de dominio claros para una futura extracción si la escala lo exige.

## Principios

- Una sola aplicación Android para Tutor y Supervisado.
- Autorización validada siempre en backend.
- PostgreSQL como fuente de verdad.
- Redis nunca contendrá el único ejemplar de un dato de negocio persistente.
- API versionada bajo `/api/v1`.
- Security by Design y Privacy by Design.
- Mínimo privilegio y defensa en profundidad.
- Secretos fuera del repositorio.
- Ambientes separados: desarrollo, pruebas y producción.
- Capacidades Android sujetas a APIs, permisos, restricciones y políticas oficiales.

## Componentes

1. `mobile/`: Android nativo con Kotlin y Jetpack Compose.
2. `frontend/`: Next.js + TypeScript para el panel del tutor.
3. `backend/`: FastAPI, Pydantic y SQLAlchemy.
4. PostgreSQL: datos persistentes y relaciones.
5. Redis: información temporal y coordinación.
6. FCM/WebSockets: tiempo real en sprints posteriores.
7. Google Identity: autenticación en Sprint 3.
8. Google Maps/Location/Geofencing: ubicación y geocercas en sprints posteriores.

## Límites de dominio previstos en backend

- identity
- authorization
- pairing
- devices
- applications
- web_control
- rules
- schedules
- location
- geofences
- activity
- statistics
- alerts
- synchronization
- security_events
- audit
- supervision

El Sprint 1 no implementa esos dominios de negocio; deja la infraestructura que los soportará.

## Flujo del Sprint 1

```text
Android ───────┐
               ├──> FastAPI ───> PostgreSQL
Web ───────────┘       │
                       └───────> Redis
```

Los clientes ejecutan una comprobación de infraestructura contra `/api/v1/health/ready`. El backend sólo responde `ready` si puede alcanzar PostgreSQL y Redis.

## Seguridad base

- CORS explícito.
- Trusted Hosts.
- Request ID por solicitud.
- Cabeceras defensivas iniciales.
- Swagger/OpenAPI deshabilitado en producción.
- Contenedores de aplicación sin usuario root.
- PostgreSQL y Redis en red privada en la composición de producción.
- Contraseña de Redis en todos los ambientes Compose.
- HTTP cleartext permitido sólo en el build Android `debug` para desarrollo local; `release` lo deshabilita.
- `android:allowBackup="false"`.
- Sin permisos Android sensibles en Sprint 1 salvo `INTERNET`.

## Escalabilidad

La API es stateless respecto de sesión de proceso. El estado persistente se delega a PostgreSQL y el estado efímero a Redis. Esto permite ejecutar varias réplicas del backend detrás de un balanceador en producción cuando sea necesario.

## Decisiones no tomadas todavía

No se decide todavía el proveedor cloud, el WAF, el balanceador, el servicio administrado de PostgreSQL/Redis ni el gestor de secretos. Se definirán en el Sprint 26 de despliegue con criterios de costo, disponibilidad, región, cifrado, backup y observabilidad.

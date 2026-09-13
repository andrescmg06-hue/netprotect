# Documento técnico — NetProtect

Audiencia: quien evalúa o retoma el proyecto sin haber vivido los 26 sprints anteriores. Cada
afirmación cita su fuente (`docs/sprint-NN.md`, `docs/sprint-NN-evidence.md` o un archivo de
código) para quien necesite el detalle completo o la evidencia real de ejecución.

## 1. Qué es NetProtect

Una plataforma de control parental con tres componentes, todos clientes de la misma API:

- Una única app Android (Kotlin + Jetpack Compose) que actúa como **Tutor** o **Supervisado** según
  lo decide el backend en tiempo de ejecución (`HomeScreen`, Sprint 6) — no hay dos APKs distintos.
- Un panel web (Next.js + TypeScript + App Router) sólo para el rol Tutor.
- Un backend (FastAPI + PostgreSQL + Redis) que es la única fuente de verdad para ambos clientes;
  ninguna decisión de autorización se toma en el cliente (`docs/architecture.md`).

## 2. Arquitectura

### 2.1 Decisión arquitectónica

Monolito modular en monorepo (ADR-0001, `docs/adr/0001-monorepo-modular-monolith.md`), no
microservicios: menor costo operacional para un proyecto académico que de todos modos necesita
Android + web + API + base de datos + infraestructura, con límites de dominio internos
(`identity`, `authorization`, `pairing`, `devices`, `applications`, `rules`, `schedules`,
`location`, `geofences`, `activity`, `statistics`, `alerts`, `synchronization`, `security_events`,
`audit`, `supervision`) que permitirían una extracción futura si la escala lo exigiera.

### 2.2 Stack (ADR-0002, `docs/adr/0002-stack.md`)

| Capa | Tecnología |
|---|---|
| Android | Kotlin, Jetpack Compose, minSdk 26, compile/target SDK 36 |
| Web | Next.js (App Router), React, TypeScript |
| Backend | Python, FastAPI, Pydantic, SQLAlchemy 2.0 async |
| Persistencia | PostgreSQL (fuente de verdad de todo dato de negocio) |
| Cache/efímero | Redis (nunca contiene el único ejemplar de un dato persistente) |
| Tiempo real | WebSockets (Sprint 18) + Firebase Cloud Messaging (estructura lista, sin proyecto real, Sprint 18) |
| Contenedores | Docker / Docker Compose |
| CI/CD | GitHub Actions (`ci.yml`, `cd.yml`) |
| Reverse proxy / TLS | Caddy (Sprint 26) |
| Observabilidad | Prometheus + Grafana + Loki + Promtail (Sprint 26) |

### 2.3 Diagrama de componentes

```text
Android (Tutor o Supervisado, un solo APK)  ─┐
                                              ├──> Caddy (TLS) ──> FastAPI (/api/v1) ──> PostgreSQL
Next.js (panel web, sólo Tutor)  ────────────┘                         │
                                                                        └──> Redis
```

En producción (Sprint 26), Caddy es el único punto de entrada — backend y web no publican puerto al
host (`compose.prod.yaml`, `docs/environments.md`). Ver `docs/diagrams/01-componentes.md` y
`docs/diagrams/02-despliegue.md` para el detalle gráfico completo.

### 2.4 Modelo de datos

Modelo físico versionado con Alembic desde el Sprint 2 (`backend/app/models/`, migraciones en
`backend/alembic/versions/`); nunca `Base.metadata.create_all` (`CLAUDE.md`, convenciones de
código). Claves primarias UUID, `created_at`/`updated_at` con zona horaria. Ver
`docs/diagrams/05-modelo-datos-conceptual.md` (conceptual) y
`docs/diagrams/06-modelo-datos-fisico-sprint2.md` (físico, con la justificación de cada tabla).

Tablas principales por dominio (no exhaustivo — ver el ER real para el detalle de columnas):

| Dominio | Tablas | Sprint de origen |
|---|---|---|
| Identidad y sesión | `users`, `sessions`, `audit_logs` | 2, 3 |
| Roles | `roles`, `user_roles` | 2, 4 |
| Vinculación y dispositivos | `pairing_codes`, `devices`, `tutor_devices` | 2, 5, 6 |
| Aplicaciones | `applications`, `device_application_usage` | 7 |
| Reglas | `app_rules`, `app_rule_events` | 8 |
| Categorías | `app_category_assignments`, `category_rules` | 10 |
| Ubicación y geocercas | `device_location_reports`, `geofences`, `geofence_events` | 13, 14 |
| Alertas | `alerts`, `alert_silences` | 17 |

### 2.5 API

Versionada bajo `/api/v1` (`backend/app/api/v1/router.py`). Grupos de endpoints
(`backend/app/api/v1/endpoints/`): `auth`, `roles`, `pairing`, `devices`, `applications`, `rules`,
`categories`, `location`, `geofences`, `history`, `statistics`, `alerts`, `audit`, `realtime`
(WebSocket), `health`. `GET /` y `GET /metrics` son las únicas rutas públicas fuera de ese prefijo,
ambas exentas de autenticación por diseño documentado (`docs/sprint-25.md`,
`docs/sprint-26.md`) — nunca por descuido: `test_route_authorization_sweep.py` (Sprint 25) falla si
aparece una tercera.

## 3. Funcionalidades entregadas, sprint por sprint

Resumen de una línea por sprint — `docs/sprint-NN.md` tiene el objetivo completo, las historias de
usuario, los criterios de aceptación y las decisiones de diseño de cada uno.

| # | Incremento | Resumen |
|---:|---|---|
| 1 | Arquitectura y entorno | Los tres carriles (backend/web/Android) arrancan y verifican conectividad a PostgreSQL/Redis. |
| 2 | Base de datos | Modelo físico inicial, Alembic, semillas de roles. |
| 3 | Google Login | OAuth/OIDC real (backend valida ID token contra JWKS de Google), tokens propios (access corto + refresh rotativo cifrado). |
| 4 | Roles | RBAC: `require_role` para acciones sin recurso, autorización por fila para recursos concretos. |
| 5 | Vinculación | Código de 6 dígitos con HMAC, TTL, un solo uso, rate limiting. |
| 6 | Dispositivos | Listado/detalle/renombrado/desvinculación con estado por heartbeat. |
| 7 | Aplicaciones | Inventario de apps instaladas y tiempo de uso diario. |
| 8 | Reglas de aplicaciones | Bloqueo local vía `UsageStatsManager` + foreground service + pantalla de bloqueo propia (sin device owner). |
| 9 | Lista blanca/negra | Política por dispositivo `ALLOW`/`BLOCK` que invierte el comportamiento por defecto, con lista de apps nunca bloqueables. |
| 10 | Categorías | 11 categorías (decisión propia, ver `docs/sprint-10.md`) con regla por categoría. |
| 11 | Tiempo | Límite semanal, `schedule_days_mask`, zona horaria del dispositivo. |
| 12 | Modo escolar | Vigencia horaria sobre la política por defecto. |
| 13 | Ubicación | Reporte cada ~15 min, cifrado Fernet, retención 7 días, sin `ACCESS_BACKGROUND_LOCATION`. |
| 14 | Geocercas | Detección ENTER/EXIT por Haversine comparando reportes consecutivos — sin la Geofencing API de Google. |
| 15 | Historial | Línea de tiempo unificada de bloqueos y geocercas, retención 90 días. |
| 16 | Estadísticas | Agregaciones hoy/7d/30d calculadas al vuelo. |
| 17 | Alertas | Bandeja INFO/WARNING/HIGH/CRITICAL con deduplicación y silenciado. |
| 18 | Tiempo real | WebSocket por dispositivo + intento de despertar por FCM (sin proyecto Firebase real). |
| 19 | Offline | Caché Room + cola de eventos pendientes + `SyncWorker`. |
| 20 | Detección de manipulación | 5 señales que alertan, nunca bloquean. |
| 21 | Seguridad integral | Rate limiting global, cabeceras, TLS a nivel de aplicación, ZAP + MobSF. |
| 22 | Auditoría | Consulta y exportación del propio registro de acciones. |
| 23 | Supervisión remota | Captura de pantalla vía WebRTC, señalización por el WebSocket existente. |
| 24 | Panel web completo | 16 secciones (decisión propia, ver `docs/sprint-24.md`). |
| 25 | Pruebas integrales | Barrido de autorización, Room instrumentado, Newman, Playwright, k6. |
| 26 | Despliegue | Caddy con TLS real, secretos como archivo, backups probados, monitorización, CD a GHCR. |
| 27 | Documentación | Este conjunto de documentos. |

### 3.1 Funcionalidad del plan original no construida

`docs/planning/plan-desarrollo.md` (Paso 8) pedía control de navegación web mediante `VpnService`
con filtrado de dominios por DNS. Se evaluó (`docs/android/capability-matrix.md`, línea 18) y se
pospuso explícitamente en el Sprint 9 por necesitar su propia fase de trabajo
(`docs/sprint-09.md`, línea 94); nunca se retomó en los sprints 10-26. A diferencia de
cámara/micrófono en supervisión remota (Sprint 23, marcados "V2" desde su propio sprint como
alcance consciente), esto es un paso completo del plan que quedó sin construir. Ver
`docs/manuals/analisis-riesgos.md` para el riesgo asociado.

## 4. Decisiones de diseño propias (no del enunciado original)

El documento de 48 secciones que originó este proyecto queda fuera del repositorio; donde el equipo
tuvo que interpretar o decidir sin esa referencia, quedó documentado como decisión propia, no como
cita:

- Las 11 categorías de aplicación (Sprint 10, `docs/sprint-10.md`).
- Las 16 secciones del panel web (Sprint 24, `docs/sprint-24.md`).
- La interpretación de "lista blanca/negra" como política de dispositivo `ALLOW`/`BLOCK` en vez de
  una tabla de listas separada (Sprint 9, `docs/sprint-09.md`).
- El mecanismo de detección de geocercas por comparación de reportes en vez de la Geofencing API de
  Android (Sprint 14, para evitar `ACCESS_BACKGROUND_LOCATION` y `play-services-location`).

## 5. Cómo ejecutar y verificar

Ver `docs/manuals/manual-instalacion.md` (desarrollo local) y `docs/manuals/manual-despliegue.md`
(producción real). Ninguno de los dos repite comandos aquí — ambos citan exactamente los ya
verificados en `docs/environments.md` y `docs/sprint-01-evidence.md`/`docs/sprint-26-evidence.md`.

## 6. Dónde seguir leyendo

| Pregunta | Documento |
|---|---|
| ¿Cómo instalo el proyecto? | `docs/manuals/manual-instalacion.md` |
| ¿Cómo uso la app o el panel? | `docs/manuals/manual-usuario.md` |
| ¿Cómo administro una instancia real? | `docs/manuals/manual-administrador.md` |
| ¿Cómo se prueba esto? | `docs/manuals/plan-pruebas.md` |
| ¿Qué riesgos tiene el proyecto? | `docs/manuals/analisis-riesgos.md` |
| ¿Cuál es el modelo de seguridad? | `docs/manuals/modelo-seguridad.md` |
| ¿Qué pasa con los datos de un menor? | `docs/manuals/politica-privacidad.md` |
| ¿Cómo despliego a producción? | `docs/manuals/manual-despliegue.md` |
| ¿Qué existe exactamente y por qué? | `docs/sprint-01.md` … `docs/sprint-26.md`, `CLAUDE.md` |

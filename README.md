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

## Alcance del Sprint 9

Lista blanca y negra: cada dispositivo tiene una política por defecto que decide qué pasa con una app
que no tiene regla propia. `ALLOW` (el valor por defecto, idéntico al comportamiento previo) permite
todo salvo lo bloqueado; `BLOCK` invierte eso y sólo deja funcionar las apps que el tutor aprobó con
una regla `ALLOW` — que hasta este sprint no tenía efecto propio. El launcher, la app de teléfono y
Ajustes nunca se bloquean en ningún modo, resueltos en tiempo de ejecución (ver
`docs/android/capability-matrix.md`), para no dejar el dispositivo inutilizable ni estorbar una
llamada de emergencia. Un bloqueo por política se registra como `DEFAULT_POLICY`, distinto de un
bloqueo que el tutor pidió explícitamente.

El detalle está en `docs/sprint-09.md`.

## Alcance del Sprint 10

Categorías: el tutor asigna una de 11 categorías fijas (Redes sociales, Juegos, Streaming,
Educación, Productividad, Comunicación, Noticias, Compras, Finanzas, Utilidades, Contenido para
adultos) a una app en un dispositivo, y define una regla (bloquear/permitir/límite diario/horario)
para toda la categoría a la vez. Prioridad probada explícitamente: una regla puesta directamente
sobre una app siempre gana sobre la de su categoría, y una app sin categoría ni regla propia sigue
la política por defecto del dispositivo (Sprint 9). El catálogo de categorías es una decisión
propia del equipo, no una cita del enunciado original — ver `docs/sprint-10.md`.

El detalle está en `docs/sprint-10.md`.

## Alcance del Sprint 11

Tiempo: el tutor pone un límite semanal de minutos a una app o a una categoría, además del diario
que ya existía. El dispositivo supervisado reporta su zona horaria real (IANA) en cada heartbeat,
visible junto a su estado en la app del tutor y en el panel web — salda la deuda de zona horaria
anotada desde el Sprint 7. "Modo escolar" queda para el Sprint 12.

El detalle está en `docs/sprint-11.md`.

## Alcance del Sprint 12

Modo escolar: el tutor activa una franja horaria (07:00-14:00 por defecto, configurable) en la que
el dispositivo bloquea automáticamente todo lo no aprobado, sin tocar ninguna regla existente — es
una vigencia horaria sobre la política por defecto (Sprint 9), no un sistema de perfiles paralelo.
Una regla de app o de categoría con `ALLOW` sigue aprobando esa app también en horario escolar.
Interpretación del alcance documentada explícitamente, ver `docs/sprint-12.md`.

El detalle está en `docs/sprint-12.md`.

## Alcance del Sprint 13

Ubicación aproximada: el dispositivo supervisado reporta su posición cada ~15 minutos mediante un
foreground service (`LocationReportingService`, sólo `ACCESS_COARSE_LOCATION`, sin permiso de
segundo plano — un foreground service tipo `location` ya cuenta como "en primer plano" para el
sistema de permisos de ubicación de Android), cifrada en la base de datos (Fernet) con retención de
7 días y purga inline al reportar. El tutor ve la última ubicación conocida en el panel web (mapa
embebido si hay clave de Google Maps configurada, texto si no) y en Android (texto + botón que abre
un mapa externo vía intent, sin SDK nativo de Maps).

El detalle está en `docs/sprint-13.md`.

## Alcance del Sprint 14

Geocercas: el tutor crea, edita y elimina zonas circulares (nombre, centro cifrado, radio) por
dispositivo desde el panel web; el backend detecta automáticamente las entradas y salidas
comparando cada nuevo reporte de ubicación contra el anterior del mismo dispositivo, sin usar la
Geofencing API de Android/GMS — esa API exige `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
y la dependencia `play-services-location`, revirtiendo tres decisiones de minimización de permisos
ya tomadas en el Sprint 13 (ver `docs/android/capability-matrix.md`). Historial de entradas/salidas
consultable en el panel web y, en modo sólo lectura, en la app del tutor en Android.

El detalle está en `docs/sprint-14.md`.

## Alcance del Sprint 15

Historial: los dos registros de eventos que ya existían (bloqueos de reglas, entradas/salidas de
geocercas) ganan una política de retención uniforme (90 días) con purga automática al escribir,
igual que la ubicación ya tenía desde el Sprint 13, y una línea de tiempo unificada
(`GET /devices/{id}/history`) que los combina ordenados por fecha, disponible en el panel web y en
la app del tutor en Android. No se crea una tabla "historial" genérica ni se implementa captura de
navegación web (nunca existió en este proyecto) ni alertas (Sprint 17).

El detalle está en `docs/sprint-15.md`.

## Alcance del Sprint 16

Estadísticas: agregaciones por hoy/7 días/30 días calculadas al vuelo sobre datos que ya existían
(uso diario por app, categoría asignada, bloqueos aplicados, límites configurados) — sin tabla de
agregación nueva, sin scheduler. `GET /devices/{id}/statistics?period=...` devuelve las apps más
usadas, el desglose por categoría, el conteo de bloqueos por motivo y, para las reglas
`DAILY_LIMIT` (de app o de categoría), el cumplimiento diario del periodo, disponible en el panel
web y en la app del tutor en Android. Sin librería de gráficos nueva: se mantiene la misma estética
de listas de texto que el resto de paneles.

El detalle está en `docs/sprint-16.md`.

## Alcance del Sprint 17

Alertas: una bandeja para el tutor generada a partir de señales que ya existían (bloqueos de
reglas, entradas/salidas de geocercas), sin pipeline de detección nuevo. Un límite de tiempo
agotado genera nivel `WARNING`; un bloqueo o una salida de geocerca, según el caso, `INFO` o
`WARNING`; `HIGH`/`CRITICAL` quedan en el catálogo reservados para la detección de manipulación
del Sprint 20. Deduplicación mientras la alerta siga sin leer (sin ventana de tiempo arbitraria) y
silenciado por tipo de alerta (no por alerta suelta), disponible en el panel web (con acciones de
marcar leída/silenciar) y, en modo sólo lectura, en la app del tutor en Android.

El detalle está en `docs/sprint-17.md`.

## Alcance del Sprint 18

Tiempo real: un canal WebSocket por dispositivo (`WS /devices/{id}/ws`), autenticado con un primer
frame `{"token": ...}` en vez de una cabecera (un navegador no puede fijar cabeceras en el
*handshake*), al que se conectan tanto el tutor activo como el dispositivo supervisado dueño de
ese dispositivo. Cada cambio de regla (app, categoría, política por defecto, horario escolar)
difunde `{"event": "rules_changed"}` a quien esté escuchando — probado de extremo a extremo contra
el backend real. Si el dispositivo no tiene el canal abierto, y tiene un token FCM registrado
(`POST /devices/{id}/push-token`), el backend intenta una notificación de despertar vía la API HTTP
v1 de Firebase Cloud Messaging; sin proyecto Firebase real en este repo (pendiente de que un humano
lo cree, igual que `GOOGLE_WEB_CLIENT_ID` en su momento), ese envío se omite hoy sin fallar el
cambio de regla que lo disparó. En Android, `RuleEnforcementService` usa el aviso para adelantar su
refresco de reglas en vez de esperar su sondeo periódico habitual; en el panel web,
`DeviceRulesPanel` recarga en vivo mientras está abierto.

El detalle está en `docs/sprint-18.md`.

## Alcance del Sprint 19

Funcionamiento offline: Room como caché local, en el dispositivo supervisado, de las reglas por
app, las categorías y sus reglas, la política por defecto y el horario escolar — reemplazada por
completo (nunca fusionada) en cada `GET /rules/active` exitoso, así que la estrategia de conflicto
es simplemente "gana el último fetch que respondió". Un arranque en frío sin conectividad evalúa
contra ese caché en vez de contra "todo permitido"; un bloqueo que no se pudo reportar
(`POST /rule-events`) queda en una cola (`pending_rule_events`) hasta que un fetch exitoso o
`SyncWorker` lo vacíen, en vez de perderse. `SyncWorker` (WorkManager, cada 15 minutos —el piso de
la plataforma—, sólo con conectividad) envía *heartbeat* y sincroniza uso de apps aunque la app no
esté en primer plano, complementando (no reemplazando) los sondeos ya existentes de
`SupervisedScreen`. Tanto `RuleEnforcementService` como `SyncWorker` renuevan su propio token de
acceso contra el `refresh_token` cifrado ya almacenado (Sprint 3) en vez de depender de un token
fijo que expira a los 15 minutos. Room se integró vía KSP, no `kapt`: el backend `kapt` de
`room-compiler` no soporta el formato de metadatos que emite el Kotlin 2.3.21 de este proyecto.

El detalle está en `docs/sprint-19.md`.

## Alcance del Sprint 20

Detección de manipulación, sólo con señales legítimas y sin ocultar nada: se registra el evento y
se alerta al tutor, nunca se impide la acción. Cuatro señales viajan como campos opcionales del
*heartbeat* que ya existía (`usage_access_granted`, `service_active`, `device_time`) y producen
alertas `HIGH` — pérdida del permiso de acceso a uso, servicio de reglas detenido, hora del
dispositivo desfasada respecto del servidor, y silencio anómalo del *heartbeat* (medido contra el
`last_seen_at` anterior cuando el dispositivo vuelve a reportarse, sin *scheduler*, igual que las
transiciones de geocerca del Sprint 14). La quinta es un evento discreto con endpoint propio
(`POST /devices/{id}/tamper-events`) y nivel `CRITICAL`: el intento de desinstalación, detectado
registrando la app como **Device Administrator** — no device owner —, porque Android exige
desactivar ese registro antes de poder desinstalar y esa desactivación dispara
`onDisableRequested()`. Cualquier señal deja el dispositivo en estado `ALERT`, que se recalcula en
cada latido (un latido sano lo devuelve a `ONLINE`; el registro duradero es la alerta en la
bandeja del Sprint 17, con su deduplicación y silenciado). "Revocación de la VPN", que menciona el
enunciado, queda explícitamente fuera: este proyecto no tiene componente VPN. Ninguna tabla nueva;
la migración toca un único `CHECK`, el de `alert_type`.

El detalle está en `docs/sprint-20.md`.

## Alcance del Sprint 21

Seguridad integral: repaso sistemático OWASP Top 10/API Top 10/ASVS, rate limiting global,
validación estricta, cabeceras, TLS obligatorio a nivel de aplicación, gestión de secretos, y
escaneo real con OWASP ZAP y MobSF. Antes sólo `/pairing/*` tenía límite de frecuencia; ahora
`/auth/google` y `/auth/refresh` también lo tienen (fallan *cerrado*, mismo criterio que pairing),
y toda ruta salvo `/api/v1/health*` cuenta contra un límite global por IP que falla *abierto* — una
caída de Redis no debe tumbar el 100% de la API. `Settings` ahora rehúsa arrancar en producción si
cualquier secreto (`JWT_SECRET`, `PAIRING_CODE_PEPPER`, `DATABASE_URL`, `REDIS_URL`,
`LOCATION_ENCRYPTION_KEY`) sigue en su valor de desarrollo, y un `exception_handler` genérico
evita que un error no manejado filtre su mensaje original. Cabeceras ampliadas en backend y
frontend (`Cache-Control: no-store`, `Cross-Origin-Resource-Policy`, y `Strict-Transport-Security`/
`Content-Security-Policy` sólo en producción); en producción, una petición que no llega como HTTPS
recibe 400 antes de procesar nada (la terminación TLS real sigue pendiente de un dominio, Paso 25).
`pip-audit` y `npm audit` corren ahora en CI. Un escaneo real de OWASP ZAP contra el backend
encontró y corrigió dos hallazgos reales (caché y `Cross-Origin-Resource-Policy`); un escaneo real
de MobSF contra el APK (debug y release) no encontró hallazgos `HIGH` en el build de release. Sin
certificate pinning todavía (no hay certificado de producción real contra el cual fijarlo).

El detalle está en `docs/sprint-21.md` y la evidencia completa (comandos y salida real, incluidos
los reportes de ZAP/MobSF) en `docs/sprint-21-evidence.md`.

## Alcance del Sprint 22

Auditoría: `GET /users/me/audit` y `GET /users/me/audit/export` exponen, con filtros por acción,
tipo de recurso y rango de fechas, el registro de `audit_logs` que ya existía desde el Sprint 2 y
se escribía desde el Sprint 3 pero nunca se consultaba. Alcance deliberado — "mis propias
acciones", no "todo lo que pasó en mis dispositivos" — porque `AuditLog` no tiene columna
`device_id`; cualquier usuario autenticado ve sólo las filas cuyo `actor_user_id` es el suyo, sin
que el rol conceda ni restrinja nada adicional. Paginación real (`limit`/`offset` con `total`), a
diferencia del tope fijo de historial/alertas, porque esta tabla no tiene retención — el plan la
llama "registro inmutable". Disponible con filtros, paginación y exportación a CSV en el panel
web; en modo sólo lectura y sin filtros en la app del tutor en Android.

El detalle está en `docs/sprint-22.md` y la evidencia en `docs/sprint-22-evidence.md`.

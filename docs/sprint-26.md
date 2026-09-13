# Sprint 26 — Despliegue

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 25) pide: "Cuenta cloud, dominio, HTTPS con Let's
Encrypt, reverse proxy, secretos gestionados, backups de PostgreSQL, monitorización y logs, y
separación real de dev/test/prod. CD desde GitHub Actions con aprobación manual para producción."

Antes de escribir una sola línea se le preguntó al dueño del proyecto si ya existía una cuenta
cloud y un dominio reales para este sprint — no existían. Un dominio y un certificado Let's
Encrypt reales exigen un servidor público de verdad, que ningún agente puede provisionar por
cuenta propia. La decisión tomada con el dueño del proyecto: construir toda la infraestructura
como código y verificar todo lo que **sí** se puede probar sin dominio público (un reverse proxy
real terminando TLS con la CA interna de Caddy, backups reales contra PostgreSQL real,
monitorización real con Prometheus/Grafana/Loki, un pipeline de CD real que construye y publica
imágenes), dejando lo que exige cuenta/dominio real documentado como pendiente de un humano — el
mismo patrón ya usado para Firebase (Sprint 18) y Google Maps (Sprint 13).

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-076 | Como responsable técnico, quiero que el tráfico público llegue siempre por HTTPS real, con el certificado gestionado automáticamente, para no depender de un `enforcement_middleware` que sólo rechaza HTTP sin terminar TLS de verdad. |
| HU-077 | Como responsable técnico, quiero que los secretos de producción vivan como archivos inyectados por el orquestador, no como variables de entorno en un `.env` persistente, para que `docker inspect` o una fuga de configuración no los exponga. |
| HU-078 | Como responsable técnico, quiero un backup automático de PostgreSQL con una restauración probada de verdad, para no descubrir un backup corrupto el día que haga falta. |
| HU-079 | Como responsable técnico, quiero métricas y logs centralizados del backend, para detectar una caída o una tasa de error alta sin entrar a cada contenedor a mano. |
| HU-080 | Como responsable técnico, quiero que cada cambio en `main` publique una imagen versionada, y que un despliegue a producción real exija la aprobación explícita de una persona. |

## Criterios de aceptación

1. Un reverse proxy real (no una simulación) termina TLS y redirige HTTP→HTTPS, verificado con una
   cadena de certificado validada de extremo a extremo (`curl --cacert`, nunca `-k`).
2. Ningún secreto de producción aparece como variable de entorno en texto plano en
   `compose.prod.yaml` ni en `.env.production.example`.
3. Un backup se toma, los datos originales se destruyen deliberadamente, y una restauración real
   los recupera — con salida real documentada, no descrita de memoria.
4. Prometheus scrapea el backend real y una regla de alerta (`BackendDown`) pasa de inactiva a
   disparada y de vuelta a inactiva en una corrida real, no simulada. Grafana consulta datos reales
   a través de sus datasources provisionados. Loki recibe logs reales de los contenedores.
5. `.github/workflows/cd.yml` construye y publica imágenes reales en GHCR; el job de despliegue a
   producción queda detrás de un *environment* de GitHub con aprobación manual, y se salta
   (no falla) mientras no exista un servidor real configurado.
6. La suite completa de backend (unitaria + integración, 264 pruebas) sigue en verde en Docker
   después de todos los cambios, incluida la del barrido de autorización del Sprint 25.
7. Lo que de verdad exige una cuenta cloud, un dominio o permisos de administrador sobre el
   repositorio de GitHub queda explícitamente documentado como pendiente, no dado por hecho.

## Decisiones de diseño y su motivo

### Por qué Caddy y no Traefik

Caddy gestiona HTTPS automático (Let's Encrypt) sin configuración adicional, y —decisivo para
poder verificar este sprint sin dominio propio— trata cualquier nombre no resoluble públicamente
(`localhost`, `*.localhost`, IPs) emitiendo desde su propia CA interna en vez de intentar ACME
contra un nombre que nunca podría pasar un desafío HTTP-01/TLS-ALPN-01. La topología de producción
usa dos subdominios (`WEB_DOMAIN`/`API_DOMAIN`, ya implícitos en `CORS_ORIGINS`/`ALLOWED_HOSTS`/
`NEXT_PUBLIC_API_BASE_URL` desde el Sprint 21) en vez de un solo dominio con rutas `/api`: es la
convención que este repositorio ya tenía adoptada, cambiarla habría sido alcance no pedido.

### Hallazgo real: `uvicorn --proxy-headers` no bastaba

Verificado end-to-end contra el stack completo (no en aislamiento): con Caddy ya terminando TLS
real y reenviando a `backend:8000` en HTTP simple dentro de la red Docker, cada petición seguía
recibiendo `400 https_required`. Causa: `uvicorn --proxy-headers` sólo confía en
`X-Forwarded-Proto` desde `127.0.0.1` por defecto, y Caddy llega desde su propia IP de contenedor
en la red `edge`, no desde loopback. Corregido añadiendo `--forwarded-allow-ips=*` al `CMD` del
`Dockerfile` — seguro específicamente porque backend no tiene ningún otro punto de entrada en esta
topología (sin puerto publicado al host). Sin este hallazgo, el primer despliegue real habría
rechazado el 100% del tráfico.

### Hallazgo real: un `reverse_proxy` sin restricción de ruta expone todo, `/metrics` incluido

El diseño original asumía que `infra/caddy/Caddyfile` "no tenía ruta a `/metrics`" simplemente por
no mencionarlo. Falso: un bloque `reverse_proxy backend:8000` sin matcher reenvía **todas** las
rutas de ese dominio, `/metrics` incluido — verificado con un `curl` real que devolvió 200 en vez
de un 404 esperado. Corregido con un matcher explícito (`@metrics path /metrics` + `respond 404`)
antes del `reverse_proxy`. La lección, ya aplicada también a los comentarios del código que hacían
la misma afirmación incorrecta: la segmentación de red (backend sin puerto publicado) seguía
siendo cierta y suficiente por sí sola, pero el comentario sobre Caddy no lo era hasta este fix.

### Hallazgo real: Prometheus también necesitaba una excepción a `https_required`

Con el fix anterior aplicado, Prometheus seguía sin poder scrapear: se conecta a
`backend:8000/metrics` en HTTP simple dentro de la red `private`, sin pasar nunca por Caddy, así
que nunca presenta un `X-Forwarded-Proto` de confianza. La alerta `BackendDown` llegó a dispararse
de verdad por esta causa la primera vez que se levantó el stack completo — evidencia real, no
buscada a propósito, de que el mecanismo de alertas funciona. Corregido extendiendo la excepción
que ya eximía a `/health*` del chequeo HTTPS (`_OPERATIONAL_PATH_PREFIXES` en `app/main.py`) para
incluir también `/metrics`: ambos caminos ya estaban fuera de Internet por segmentación de red, no
por HTTPS, así que exigirles HTTPS internamente no añadía seguridad real. Cubierto por
`test_production_exempts_health_and_metrics_from_https_required` (parametrizado, nuevo).
`/code-review` encontró, antes de cerrar el sprint, que la excepción separada del limitador de
tasa global seguía comprobando sólo `/health*` — `/metrics` quedaba eximido de HTTPS pero no del
límite de 300 peticiones/60s por IP, inconsistencia real aunque de bajo impacto con los valores por
defecto. Corregido unificando ambas comprobaciones bajo la misma constante
`_OPERATIONAL_PATH_PREFIXES`, con `test_health_and_metrics_are_never_rate_limited` (parametrizado)
cubriendo ahora los dos casos.

### Por qué las métricas son código propio y no `prometheus-fastapi-instrumentator`

Se intentó primero la librería estándar. Falla en el primer request real: su resolución de nombre
de ruta asume que todo lo que `app.routes` resuelve es un `Route`/`APIRoute` plano con atributo
`.path` — el mismo cambio interno de FastAPI 0.141 que `test_route_authorization_sweep.py` (Sprint
25) ya tuvo que sortear con *duck-typing* rompe también a esta librería, con
`AttributeError: '_IncludedRouter' object has no attribute 'path'` en **cada** petición, no sólo en
`/metrics`. Dos librerías rotas por el mismo cambio interno en un mismo sprint no es coincidencia.
Reemplazada por `app/core/metrics.py`, sobre `prometheus_client` directamente: en vez de volver a
derivar la ruta desde `app.routes`, reconstruye la ruta completa y con plantilla a partir de
`request.url.path` (ya viene con el prefijo correcto) sustituyendo cada valor de
`request.path_params` por su nombre — verificado con una petición real a una ruta anidada
(`/api/v1/devices/{device_id}`) mostrando la etiqueta correcta, cosa que `request.scope["route"]`
por sí solo no daba (mostraba sólo `/health`, sin el prefijo `/api/v1`).

### Por qué el status en las métricas es `"5xx"` y no el código exacto

Verificado contra la salida real de `/metrics`: el label agrupa por clase (`"2xx"`, `"4xx"`,
`"5xx"`), no por código HTTP exacto. Las reglas de Prometheus y el dashboard de Grafana usan
comparación exacta de string (`status="5xx"`), no una expresión regular sobre códigos individuales
— una regex como `5..` nunca habría coincidido con la etiqueta real.

### Por qué el patrón `_FILE` y no `secrets_dir` de pydantic-settings

`pydantic-settings` sí soporta un `secrets_dir`, pero sólo resuelve valores atómicos — no ayuda con
`DATABASE_URL`/`REDIS_URL`, que este proyecto ensambla combinando usuario/host/contraseña. El
patrón `_FILE` (ya usado por las imágenes oficiales de PostgreSQL/Redis) resuelve ambos casos por
igual: `backend/docker-entrypoint.sh` exporta cualquier `FOO_FILE` como `FOO` antes de `exec`,
sin que `app/core/config.py` necesite saber de dónde vino el valor. Verificado con un secreto real
montado por archivo, confirmando que el proceso arrancado realmente ve el valor esperado.

### Por qué Prometheus + Grafana + Loki + Promtail y no sólo `docker compose logs`

Los cuatro corren enteramente en Docker, sin cuenta cloud ni credencial externa — a diferencia de
Firebase o Google Maps, esto sí era construible y verificable de punta a punta en este sprint. Se
usó una regla de alerta real (`BackendDown`) para confirmar el ciclo completo: forzar la condición
de fallo, ver la alerta pasar a `firing`, corregir la causa, verla volver a `inactive` — sin ese
ciclo real, una regla de alerta nunca probada no es evidencia de nada. Hallazgo real menor:
`datasources.yml` no fijaba un `uid` explícito, así que Grafana asignaba uno aleatorio en cada
reprovisión que no coincidía con el `uid` fijo que el dashboard JSON esperaba — cada panel habría
mostrado "datasource not found". Corregido fijando `uid: prometheus`/`uid: loki` en ambos lados.

### Por qué el job de despliegue real se salta en vez de fallar

`vars.PROD_HOST` no está configurado — no hay servidor real. `deploy-production` en
`.github/workflows/cd.yml` usa ese valor como condición (`if: vars.PROD_HOST != ''`): mientras no
exista, el job aparece como *skipped*, nunca como fallido, mismo patrón ya usado por
`app/services/push.py` con `fcm_project_id` vacío. El *environment* `production` con revisor
obligatorio (aprobación manual) **no se pudo configurar** en este sprint: crear o modificar un
*environment* de GitHub exige permisos de administrador sobre el repositorio, y el token usado en
este sprint sólo tenía permisos de colaborador — ver `docs/sprint-26-evidence.md`. El flujo de
trabajo ya está escrito y listo; falta que alguien con esos permisos (el dueño del repositorio)
configure el revisor desde Settings → Environments → production, o conceda temporalmente esos
permisos para hacerlo por API.

### Por qué `build-and-push` se encadena con `workflow_run` y no con `push`

Un pipeline de CD que aprueba código roto por haber llegado antes que la corrida de pruebas
anularía el sentido de la aprobación manual en `deploy-production`. `workflow_run` engancha
`cd.yml` a la finalización real del workflow `ci`, comprobando su `conclusion`, en vez de correr en
paralelo a ciegas.

## Qué queda pendiente de un humano

- Cuenta cloud/VPS y dominio real (Paso 25) — sin esto, Caddy sigue emitiendo desde su CA interna
  en vez de Let's Encrypt real.
- Elegir un gestor de secretos cloud concreto y apuntar `secrets/*.txt` a él en el servidor real —
  el mecanismo (`secrets/README.md`) ya es agnóstico del proveedor.
- `vars.PROD_HOST`/`PROD_SSH_USER` y `secrets.PROD_SSH_KEY` en el repositorio de GitHub.
- Configurar el *environment* `production` con un revisor obligatorio — exige permisos de
  administrador sobre el repositorio que este sprint no tenía.
- TURN server real para WebRTC (Sprint 23) y `GOOGLE_MAPS`/Firebase (Sprints 13/18) — sin cambios,
  siguen pendientes de sus propios humanos con cuenta.

Ver `docs/sprint-26-evidence.md` para los comandos ejecutados y su salida real.

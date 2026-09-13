# Sprint 26 — Evidencia

Todo lo que sigue se ejecutó realmente en esta máquina (Windows, Docker Desktop) durante el cierre
de este sprint. Comandos y salidas reales, recortadas donde son repetitivas. Sin dominio ni cuenta
cloud reales (ver `docs/sprint-26.md`): la verificación usa `WEB_DOMAIN=web.localhost`/
`API_DOMAIN=api.localhost` y secretos generados por `secrets/generate-dev-secrets.sh` — nunca
valores de producción real.

## 1. Backend: build, ruff y pytest tras añadir métricas y el patrón de secretos por archivo

```
docker build --target test -t netprotect-backend:sprint26-test ./backend
docker run --rm --entrypoint sh netprotect-backend:sprint26-test -c \
  "ruff check --no-cache app tests; pytest -q -p no:cacheprovider -m 'not integration'"
```

```
All checks passed!
RUFF_EXIT=0
14 passed, 248 deselected, 2 warnings in 2.25s
```

## 2. Hallazgo real: `prometheus-fastapi-instrumentator` rompe con FastAPI 0.141

Primer intento de instrumentación usó esa librería. `pytest tests/test_health.py` falló en el
primer request real:

```
File ".../prometheus_fastapi_instrumentator/routing.py", line 55, in _get_route_name
    route_name = route.path
                 ^^^^^^^^^^
AttributeError: '_IncludedRouter' object has no attribute 'path'
```

Mismo cambio interno de FastAPI 0.141 que `test_route_authorization_sweep.py` (Sprint 25) ya tuvo
que sortear con *duck-typing*. Reemplazada por `app/core/metrics.py`, hecho a mano sobre
`prometheus_client` — ver `docs/sprint-26.md`.

## 3. El patrón de secretos por archivo (`docker-entrypoint.sh`) funciona de verdad

```
docker run --rm \
  -e JWT_SECRET_FILE=/run/secrets/jwt_secret \
  -v "$(pwd)/secrets/_manual_test_jwt_secret.txt:/run/secrets/jwt_secret:ro" \
  --entrypoint /bin/sh netprotect-backend:sprint26-check \
  /usr/local/bin/docker-entrypoint.sh env | grep '^JWT_SECRET='
```

```
JWT_SECRET=test_jwt_secret_from_file_value_1234567890ab
```

(Primer intento con un archivo montado desde `/tmp` de Git Bash en Windows creó un *directorio*
vacío en `/run/secrets/jwt_secret` en vez de un archivo — problema del entorno de prueba en
Windows/Git Bash, no del script: montar el mismo archivo desde una ruta dentro del propio
repositorio, que Docker Desktop sí comparte, lo resolvió.)

## 4. Suite de integración completa (Postgres/Redis reales en Docker) tras todos los cambios

```
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

```
264 passed, 4 warnings in 73.17s (0:01:13)
```

(264, no 262: las dos pruebas nuevas parametrizadas de `test_production_exempts_health_and_metrics_from_https_required`, sección 7.)

## 5. Levantar `compose.prod.yaml` completo sin dominio ni cuenta cloud

```
./secrets/generate-dev-secrets.sh
docker compose --env-file .env.production -f compose.prod.yaml build
docker compose --env-file .env.production -f compose.prod.yaml run --rm migrate
docker compose --env-file .env.production -f compose.prod.yaml up -d
```

Los once contenedores (`db`, `redis`, `backend`, `web`, `caddy`, `backup`, `prometheus`, `grafana`,
`loki`, `promtail`, más `migrate` que termina por diseño) arrancan y quedan `Up`/`healthy`.

## 6. Caddy: TLS real terminado, verificado con cadena de certificado validada (no `-k`)

Log real de Caddy al arrancar — nótese `"issuer":"local"`, la CA interna, elegida automáticamente
porque `*.localhost` nunca podría pasar un desafío ACME público:

```
{"logger":"http","msg":"enabling automatic TLS certificate management","domains":["api.localhost","web.localhost"]}
{"logger":"tls.obtain","msg":"certificate obtained successfully","identifier":"web.localhost","issuer":"local"}
{"logger":"tls.obtain","msg":"certificate obtained successfully","identifier":"api.localhost","issuer":"local"}
```

Cadena de certificado extraída y validada de verdad (no `curl -k`):

```
docker compose ... exec caddy cat /data/caddy/pki/authorities/local/root.crt > ca.crt
docker run --rm --network netprotect-prod_edge -v "$(pwd)/ca.crt:/ca.crt:ro" curlimages/curl:8.11.0 \
  curl -sS --cacert /ca.crt --connect-to web.localhost:443:caddy:443 -D - https://web.localhost/
```

```
HTTP/2 200
strict-transport-security: max-age=63072000; includeSubDomains
via: 1.1 Caddy
```

Redirección HTTP→HTTPS real:

```
curl --resolve web.localhost:80:127.0.0.1 -D - http://web.localhost/
HTTP/1.1 308 Permanent Redirect
Location: https://web.localhost/
```

## 7. Hallazgo real: `https_required` rechazaba el 100% del tráfico a través de Caddy

Primer intento contra `api.localhost` (TLS real terminado por Caddy, backend detrás en HTTP simple
dentro de Docker):

```
curl ... --connect-to api.localhost:443:caddy:443 https://api.localhost/
HTTP/2 400
{"detail":"https_required"}
```

Causa: `uvicorn --proxy-headers` sólo confía en `X-Forwarded-Proto` desde `127.0.0.1`; Caddy llega
desde su propia IP de contenedor. Corregido con `--forwarded-allow-ips=*` en el `CMD` del
`Dockerfile`. Tras reconstruir:

```
curl ... --connect-to api.localhost:443:caddy:443 https://api.localhost/api/v1/health
HTTP/2 200
{"status":"ok","service":"NetProtect API","environment":"production",...}
```

## 8. Hallazgo real: el `reverse_proxy` de Caddy exponía `/metrics` públicamente

```
curl ... --connect-to api.localhost:443:caddy:443 https://api.localhost/metrics
200   # inesperado — /metrics no debía ser público
```

Corregido añadiendo un matcher explícito (`@metrics path /metrics` + `respond 404`) antes del
`reverse_proxy` en `infra/caddy/Caddyfile`. Tras el fix:

```
curl -o /dev/null -w "%{http_code}\n" ... https://api.localhost/metrics
404
```

## 9. Hallazgo real: Prometheus tampoco podía scrapear (mismo `https_required`)

Con el fix de la sección 7 ya aplicado, Prometheus seguía sin poder leer las métricas — se conecta
en HTTP simple, sin pasar por Caddy, dentro de la red `private`:

```
GET /api/v1/targets
"scrapeUrl":"http://backend:8000/metrics","lastError":"server returned HTTP status 400 Bad Request","health":"down"
```

Corregido extendiendo `_OPERATIONAL_PATH_PREFIXES` en `app/main.py` para incluir `/metrics` junto
a `/health*`. Consecuencia real, no buscada a propósito: la regla de alerta `BackendDown` ya
**estaba disparada** en ese momento — evidencia real de que el mecanismo de alertas funciona:

```
GET /api/v1/rules
{"name":"BackendDown","state":"firing", ...}
```

Tras reconstruir el backend con el fix y esperar dos ciclos de scrape:

```
GET /api/v1/targets  → "health":"up"
GET /api/v1/rules    → BackendDown: "inactive", HighErrorRate: "inactive"
```

Ciclo completo disparo→resolución verificado de extremo a extremo, no simulado.

## 10. Grafana: hallazgo real de UID de datasource, y dashboard consultando datos reales

Primer intento — `datasources.yml` sin `uid:` explícito dejaba que Grafana asignara uno aleatorio:

```
GET /api/datasources → uid: "PBFA97CFB590B2093"   # el dashboard esperaba "prometheus"
```

Corregido fijando `uid: prometheus`/`uid: loki` en `datasources.yml` (y ajustando el dashboard
JSON, que ya usaba esos valores). Recreado el volumen de Grafana para una reprovisión limpia, y
verificado con una consulta real a través del proxy de Grafana:

```
GET /api/datasources/proxy/uid/prometheus/api/v1/query?query=up{job="netprotect-backend"}
{"status":"success","data":{"result":[{"metric":{...},"value":[...,"1"]}]}}
```

## 11. Loki/Promtail: logs reales de contenedor, no simulados

```
GET /loki/api/v1/query_range?query={compose_service="backend"}
```

```
INFO:     172.18.0.5:40962 - "GET /metrics HTTP/1.1" 200 OK
```

## 12. Backup y restauración: round-trip real con pérdida de datos simulada

```sql
CREATE TABLE sprint26_backup_marker (id serial primary key, note text);
INSERT INTO sprint26_backup_marker (note) VALUES ('marker-before-backup');
```

```
docker compose ... run --rm -e ONESHOT=1 backup
[backup] wrote /backups/netprotect-20260913T055312Z.dump (56.0K)
```

```sql
DROP TABLE sprint26_backup_marker;
SELECT * FROM sprint26_backup_marker;
ERROR:  relation "sprint26_backup_marker" does not exist
```

```
docker compose ... run --rm --entrypoint /scripts/restore.sh backup /backups/netprotect-20260913T055312Z.dump
[restore] done
```

```sql
SELECT * FROM sprint26_backup_marker;
 id |         note
----+----------------------
  1 | marker-before-backup
(1 row)

SELECT * FROM roles;
    code     |                          description
-------------+------------------------------------------------------------------
 TUTOR       | Administra y supervisa dispositivos vinculados.
 SUPERVISADO | Dispositivo vinculado que recibe y aplica políticas del tutor.
(2 rows)
```

La fila marcadora y el esquema completo (incluidas las semillas de `roles`) sobrevivieron el
ciclo destrucción→restauración intactos.

## 13. `compose.prod.yaml config` valida sin advertencias tras añadir `image:`/GHCR

```
docker compose --env-file .env.production.example -f compose.prod.yaml config --quiet
```

Sin salida — configuración válida.

## 14. GitHub: no se pudo configurar el *environment* `production` con revisor obligatorio

```
gh api -X PUT repos/andrescmg06-hue/netprotect/environments/production \
  -f 'reviewers[][type]=User' -F 'reviewers[][id]=198416310'
```

```
{"message":"Must have admin rights to Repository.","status":"403"}
```

El token autenticado (`cristian-dzgr00`) es colaborador del repositorio pero no administrador —
crear o modificar un *environment* con reglas de protección exige permisos de administrador. El
workflow (`.github/workflows/cd.yml`) ya referencia `environment: production`; falta que alguien
con esos permisos (el dueño del repositorio, `andrescmg06-hue`) añada el revisor obligatorio desde
Settings → Environments → production, o conceda temporalmente permisos de administrador para
hacerlo por API.

## 15. Limpieza tras la verificación

```
docker compose --env-file .env.production -f compose.prod.yaml down -v
rm -f .env.production secrets/*.txt
```

Ningún secreto de verificación ni certificado de prueba quedó en el árbol de trabajo — `secrets/`
sólo conserva `README.md` y `generate-dev-secrets.sh`, ambos versionados a propósito.

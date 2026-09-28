---
paths:
  - "compose*.yaml"
  - "infra/**"
  - ".github/**"
  - "scripts/**"
  - "docker/**"
---

# Infra, Docker Compose, CI/CD

- Las pruebas de integración corren en segundos dentro de un contenedor y en varios minutos desde
  Windows contra los puertos publicados (~1.4s de más por petición en el proxy de Docker Desktop).
  Verificar siempre en contenedor — ver el bloque exacto más abajo.
- `export MSYS_NO_PATHCONV=1` antes de cualquier comando `docker` con rutas de contenedor en Git
  Bash sobre Windows.
- Nunca `docker compose -f compose.prod.yaml` ni `docker compose down -v` sobre `compose.yaml`
  (borra los datos de desarrollo) sin que se pida explícitamente.
- Reconstruir `backend`, `web` y `migrate` juntos cuando cambie cualquiera de los dos — comparten
  Dockerfile pero son imágenes independientes (ver el error de migración desactualizada en
  `.claude/rules/database.md`).

---

### Nota de rendimiento en Windows

Las pruebas de integración corren en segundos dentro de un contenedor y en varios minutos si se
ejecutan desde Windows contra los puertos publicados (cada petición paga ~1.4s en el proxy de
puertos de Docker Desktop). Para verificar, usar siempre:

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

No `docker compose -f compose.test.yaml up --build ...` con `migrate` en `depends_on`: ese flag
aborta todo el stack en cuanto cualquier contenedor termina, y `migrate` termina por diseño — mató
`backend` antes de que corriera sus pruebas la primera vez que se intentó (ver `docs/sprint-02-evidence.md`).

**Nota del Sprint 26, válida para cualquier sprint futuro que toque el reverse proxy, los secretos
de producción o el pipeline de CD**: no había cuenta cloud ni dominio real al empezar este sprint
(confirmado con el dueño del proyecto antes de escribir código) — la decisión tomada fue construir
toda la infraestructura como código y verificar todo lo que sí se puede probar sin dominio público,
dejando lo que exige cuenta/dominio real documentado como pendiente, mismo patrón ya usado para
Firebase/Maps. Caddy (`infra/caddy/Caddyfile`) termina TLS real: Let's Encrypt automático contra un
dominio público (`WEB_DOMAIN`/`API_DOMAIN`, ya implícitos en `CORS_ORIGINS`/`ALLOWED_HOSTS`/
`NEXT_PUBLIC_API_BASE_URL` desde el Sprint 21), o su propia CA interna contra `*.localhost` sin
dominio propio — verificado con una cadena de certificado validada de extremo a extremo, nunca
`curl -k`. Tres hallazgos reales, los tres encontrados corriendo el stack **completo**, no en
aislamiento: `uvicorn --proxy-headers` sólo confía en `X-Forwarded-Proto` desde `127.0.0.1`, no
desde la IP de contenedor de Caddy, así que todo el tráfico llegaba rechazado con
`https_required` hasta sumar `--forwarded-allow-ips=*` al `CMD` del `Dockerfile` (seguro
específicamente porque backend no tiene otro punto de entrada en esta topología: sin puerto
publicado al host); un `reverse_proxy` de Caddy sin restricción de ruta reenviaba también
`/metrics` al dominio público, cerrado con un matcher explícito (`@metrics path /metrics` +
`respond 404`) antes del `reverse_proxy`; y Prometheus, que scrapea `backend:8000/metrics` en HTTP
simple dentro de la red `private` sin pasar nunca por Caddy, chocaba con el mismo
`https_required` — la alerta `BackendDown` llegó a **dispararse de verdad** por esta causa la
primera vez que se levantó el stack completo, y a resolverse de verdad tras el fix
(`_OPERATIONAL_PATH_PREFIXES` en `app/main.py`, que ya eximía a `/health*` desde antes — luego
unificada con la excepción, antes separada, del limitador de tasa global, que `/code-review`
encontró que seguía sin cubrir `/metrics`). Los ocho
secretos de producción (`JWT_SECRET`, `DATABASE_URL`, etc.) pasan de variables de entorno en texto
plano a archivos (`secrets/README.md`), con el mismo patrón `_FILE` que ya usan las imágenes
oficiales de PostgreSQL/Redis (`backend/docker-entrypoint.sh` los exporta antes de `exec`) — elegido
sobre el `secrets_dir` propio de `pydantic-settings` porque este último no ayuda con valores
ensamblados como `DATABASE_URL`/`REDIS_URL`. Métricas (`GET /metrics`) son código propio sobre
`prometheus_client`, no `prometheus-fastapi-instrumentator`: esa librería rompe **cada** request
con `AttributeError: '_IncludedRouter' object has no attribute 'path'`, el mismo cambio interno de
FastAPI 0.141 que `test_route_authorization_sweep.py` (Sprint 25) ya tuvo que sortear con
*duck-typing* — dos librerías rotas por el mismo cambio en un sprint no es coincidencia.
Prometheus + Grafana + Loki + Promtail corren enteramente en Docker (sin cuenta cloud) en la red
`private`; sólo Grafana publica un puerto, y sólo a `127.0.0.1` del host (túnel SSH, no dominio
público). Backups de PostgreSQL (`infra/backup/backup.sh`/`restore.sh`) verificados con un ciclo
real de pérdida de datos: insertar una fila marcador, respaldar, destruir la tabla, restaurar,
confirmar que vuelve. `.github/workflows/cd.yml` construye y publica imágenes en GHCR encadenado
con `workflow_run` a la finalización real (no en paralelo) del workflow `ci`; `deploy-production`
queda detrás de un *environment* de GitHub llamado `production` y se salta (nunca falla) mientras
`vars.PROD_HOST` no exista, mismo patrón que `fcm_project_id` vacío en `app/services/push.py`. Ese
*environment* **no se pudo configurar con revisor obligatorio** en este sprint: crear o modificar
un *environment* de GitHub exige permisos de administrador sobre el repositorio, y el token usado
sólo tenía permisos de colaborador — pendiente de que el dueño del repositorio
(`andrescmg06-hue`) lo configure desde Settings → Environments → production. Ver
`docs/sprint-26.md` y `docs/sprint-26-evidence.md`.

**Nota del Sprint 28, válida para cualquier trabajo futuro sobre coturn o las credenciales TURN**:
coturn (`infra/coturn/`) corre en los tres compose **solo en su propia red y con IP fija**, porque
`denied-peer-ip` (obligatorio, para que el relay no sirva para llegar a PostgreSQL/Redis) también
corta el relay entre dos clientes del mismo coturn, y la única salida verificada es
`allowed-peer-ip` apuntando a esa IP exacta (spike R1, `docs/sprint-28-evidence.md`). No bajar
`user-quota` de 16 sin medir: con 4 una sesión legítima ya fallaba con `486`, porque una conexión
WebRTC abre una asignación por interfaz × URL × transporte. El backend emite credenciales efímeras
(`app/services/turn.py`, esquema `use-auth-secret` de coturn, 1 hora, nonce aleatorio y nunca el
`user_id`) en el campo nuevo `turn_servers` de `webrtc-config`; `ice_servers` no se toca por
compatibilidad con la APK instalada. `scripts/verify_turn.sh`, paso del job `integration` de CI, es
la prueba real contra coturn: pytest sólo puede verificar la forma de la credencial. Ver
`docs/sprint-28.md`.

- Git Bash en Windows reescribe rutas del contenedor en los argumentos de `docker run`/`docker
  compose` (`/etc/coturn/...` pasó a ser `C:/Program Files/Git/etc/...`). En el Sprint 28 coturn
  arrancó **sin su configuración**, sin autenticación ni bloqueos, y la prueba dio un "éxito" falso.
  Exportar `MSYS_NO_PATHCONV=1` antes de cualquier comando de Docker con rutas del contenedor, y
  confirmar con `docker inspect` qué comando se ejecutó realmente antes de creer un resultado.
- Una prueba que no puede fallar no demuestra nada. Para verificaciones contra servicios reales
  (coturn en el Sprint 28), incluir un control negativo que **deba** fallar, por ejemplo una
  credencial firmada con otro secreto, y comprobar que falla.
- `compose.test.yaml` corre PostgreSQL sin volumen persistente a propósito. Reconstruir sólo
  `backend` y volver a hacer `up` sin repetir `run --rm migrate` primero deja una base sin tablas
  (falla con `relation "..." does not exist` en casi todas las pruebas, no sólo las nuevas). `migrate`
  corre aparte, siempre, antes de cada `up` — nunca asumir que la corrida anterior lo dejó aplicado.

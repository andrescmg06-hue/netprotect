# Ambientes

## Desarrollo

Archivo de ejemplo: `.env.development.example`.

Objetivo: feedback rápido y ejecución local. PostgreSQL se expone únicamente sobre `127.0.0.1:5432`; API y web también se enlazan a loopback por defecto.

Comando:

```bash
cp .env.development.example .env
docker compose up --build
```

## Pruebas

Archivo de ejemplo: `.env.test.example`.

`compose.test.yaml` usa PostgreSQL temporal en `tmpfs`, aplica las migraciones con el servicio
`migrate` y ejecuta `pytest`, incluyendo la prueba de conectividad cuando `RUN_INTEGRATION_TESTS=1`.

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

`migrate` se ejecuta aparte, con `run --rm`, y no como dependencia de `backend`: `--abort-on-container-exit`
detiene todo el stack en cuanto cualquier contenedor termina, y `migrate` termina por diseño en cuanto
aplica las migraciones — si fuera una dependencia de `backend` dentro del mismo `up`, abortaría la
ejecución antes de que las pruebas llegaran a correr.

## Producción (Sprint 26)

Archivo de ejemplo: `.env.production.example`. Los ocho secretos (contraseñas, `DATABASE_URL`/
`REDIS_URL`, `JWT_SECRET`, etc.) ya no viven ahí — `compose.prod.yaml` los lee como archivos
(`secrets/`, ver `secrets/README.md`), nunca como variables de entorno en texto plano.

Lo que ya existe y está verificado (ver `docs/sprint-26-evidence.md` para la evidencia real):

- **Reverse proxy y TLS**: Caddy (`infra/caddy/Caddyfile`) termina HTTPS y redirige HTTP→HTTPS
  automáticamente. Con `WEB_DOMAIN`/`API_DOMAIN` apuntando a un dominio público real, emite
  Let's Encrypt real sin configuración adicional; con `*.localhost` (el valor por defecto de
  `.env.production.example`, para verificar sin dominio propio) emite desde su propia CA interna.
  Ni backend ni web publican puerto al host — Caddy es el único punto de entrada.
- **Secretos como archivo**: `secrets/README.md` documenta los ocho archivos esperados;
  `secrets/generate-dev-secrets.sh` los genera con valores aleatorios sólo para verificación local
  (nunca para producción real — ahí cada archivo sale del gestor de secretos que elija el Paso 25).
- **Backups**: `infra/backup/backup.sh` respalda PostgreSQL en un contenedor propio con retención
  configurable; `infra/backup/restore.sh` restaura, deliberadamente a mano.
- **Monitorización y logs**: Prometheus + Grafana + Loki + Promtail (`infra/monitoring/`), todos en
  la red `private` salvo Grafana (sólo en `127.0.0.1` del host, vía túnel SSH). Dashboard y reglas
  de alerta (`BackendDown`, `HighErrorRate`) provisionados automáticamente.
- **CD**: `.github/workflows/cd.yml` construye y publica las imágenes en GHCR tras cada `ci` verde
  en `main`; el despliegue real a un servidor queda detrás de un *environment* de GitHub llamado
  `production` con aprobación manual, y no hace nada (se salta, no falla) mientras
  `vars.PROD_HOST` no esté configurado.

Pendiente de un humano con infraestructura real (Paso 25, `docs/planning/plan-desarrollo.md`):
cuenta cloud/VPS, dominio propio (para que Caddy emita Let's Encrypt real en vez de su CA interna),
el gestor de secretos concreto del proveedor elegido, `vars.PROD_HOST`/`PROD_SSH_USER`/
`secrets.PROD_SSH_KEY` en el repositorio de GitHub, y un revisor con permisos de administrador que
configure el *environment* `production` con aprobación manual (ver `docs/sprint-26-evidence.md` —
el token usado en este sprint no tenía permisos de admin sobre el repositorio para hacerlo).

Comandos de referencia:

```bash
# Verificación local sin dominio ni cuenta cloud (ver docs/sprint-26-evidence.md)
./secrets/generate-dev-secrets.sh
docker compose --env-file .env.production -f compose.prod.yaml build
docker compose --env-file .env.production -f compose.prod.yaml run --rm migrate
docker compose --env-file .env.production -f compose.prod.yaml up -d

# Despliegue real (servidor ya provisto, secrets/ ya poblado por el gestor de secretos elegido)
docker compose --env-file .env.production -f compose.prod.yaml pull backend web migrate
docker compose --env-file .env.production -f compose.prod.yaml run --rm migrate
docker compose --env-file .env.production -f compose.prod.yaml up -d
```

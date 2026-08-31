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

`compose.test.yaml` usa PostgreSQL temporal en `tmpfs` y ejecuta `pytest`, incluyendo la prueba de conectividad cuando `RUN_INTEGRATION_TESTS=1`.

```bash
docker compose -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend
```

## Producción base

Archivo de ejemplo: `.env.production.example`.

El archivo es deliberadamente incompleto respecto de infraestructura final. En producción real:

- Los secretos deben provenir del gestor de secretos del proveedor, no de un `.env` persistente.
- PostgreSQL no debe exponerse a Internet.
- API y web deben quedar detrás de un reverse proxy/ingress con TLS.
- Deben configurarse backups, restauración probada, monitoreo y alertas.
- Deben definirse dominios definitivos antes de fijar CORS y Trusted Hosts.

Comando de referencia para una máquina protegida:

```bash
docker compose --env-file .env.production -f compose.prod.yaml up -d --build
```

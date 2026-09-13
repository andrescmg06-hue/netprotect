# Infraestructura

Sprint 1 define Docker Compose para desarrollo, pruebas y una base endurecida de producción.
Sprint 26 añade lo que faltaba de esa base, todo construido y verificado sin depender de un
proveedor cloud ni un dominio real todavía (ver `docs/sprint-26.md`):

- `caddy/` — reverse proxy y terminación TLS (Let's Encrypt automático contra un dominio público,
  CA interna propia contra `*.localhost` para verificar sin uno).
- `backup/` — respaldo y restauración de PostgreSQL (`backup.sh`/`restore.sh`).
- `monitoring/` — Prometheus, Grafana, Loki y Promtail (métricas, dashboard, logs y alertas).

La selección del proveedor cloud concreto y el dominio real siguen pendientes de un humano
(Paso 25, `docs/planning/plan-desarrollo.md`) — ver `secrets/README.md` para el estado de la
gestión de secretos.

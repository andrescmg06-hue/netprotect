# Manual de administrador — NetProtect

Audiencia: quien opera día a día una instancia de NetProtect ya desplegada — no cómo instalarla por
primera vez (`docs/manuals/manual-instalacion.md`) ni cómo desplegarla por primera vez
(`docs/manuals/manual-despliegue.md`), sino cómo mantenerla corriendo, vigilarla y responder a
incidentes. NetProtect no tiene un rol de negocio "administrador" dentro de la app (sólo Tutor y
Supervisado, ver `docs/manuals/documento-tecnico.md` §4) — este manual es para quien administra la
infraestructura.

## 1. Monitorización (Sprint 26)

Prometheus + Grafana + Loki + Promtail corren en Docker en la red `private`, sin cuenta cloud
(`infra/monitoring/`). Sólo Grafana publica un puerto, y sólo a `127.0.0.1` del host — se accede vía
túnel SSH, nunca por dominio público.

- **Grafana**: dashboard `netprotect-overview` (`infra/monitoring/grafana/provisioning/dashboards/
  json/netprotect-overview.json`) provisionado automáticamente, con datasources fijos
  (`uid: prometheus`, `uid: loki` — necesario para que el dashboard los encuentre tras cada
  reprovisión).
- **Prometheus**: scrapea `backend:8000/metrics` (código propio sobre `prometheus_client`,
  `backend/app/core/metrics.py`) cada pocos segundos. Reglas de alerta en
  `infra/monitoring/alert_rules.yml`, incluida `BackendDown`.
- **Loki/Promtail**: logs reales de los contenedores, consultables desde Grafana.

Qué revisar y cuándo:

| Señal | Dónde | Acción |
|---|---|---|
| Alerta `BackendDown` disparada | Grafana / Prometheus | Verificar que `backend` esté corriendo (`docker compose ps`); revisar logs en Loki. |
| Tasa de error 5xx elevada | Dashboard, métrica `http_requests_total{status="5xx"}` | Revisar logs del backend por `request_id`. |
| Latencia p95 alta | Dashboard, `http_request_duration_seconds` | Comparar contra la referencia de `docs/sprint-25-evidence.md` (k6). |

## 2. Backups de PostgreSQL (Sprint 26)

```bash
# Backup (automatizable por cron/systemd timer en el servidor real)
./infra/backup/backup.sh

# Restauración — deliberadamente manual, nunca automática (restaurar es destructivo)
./infra/backup/restore.sh <archivo-de-backup>
```

`backup.sh` usa `pg_dump -Fc` y purga backups más viejos que `BACKUP_RETENTION_DAYS`. El ciclo
completo (insertar fila marcadora → backup → destruir tabla → restaurar → confirmar que la fila
vuelve) está verificado con datos reales en `docs/sprint-26-evidence.md`, sección 12 — repetir ese
mismo procedimiento periódicamente como prueba de que los backups siguen siendo restaurables, no
sólo que se están generando.

## 3. Secretos (Sprint 26)

Los ocho secretos de producción (`JWT_SECRET`, `DATABASE_URL`, `REDIS_URL`,
`PAIRING_CODE_PEPPER`, `LOCATION_ENCRYPTION_KEY`, contraseñas de PostgreSQL/Redis/Grafana) viven
como archivos montados (`secrets/README.md`), nunca como variables de entorno en texto plano.

- **Rotar un secreto**: generar el valor nuevo (`secrets/README.md` documenta el comando exacto
  para cada uno, p. ej. `secrets.token_urlsafe(48)` para `JWT_SECRET`), reemplazar el archivo en el
  servidor y reiniciar el servicio afectado. Rotar `JWT_SECRET` invalida todas las sesiones activas
  — comunicarlo antes si es una rotación planeada, no de emergencia.
- **Nunca** reutilizar un secreto para otro propósito "porque ya existe" (`CLAUDE.md`,
  convenciones de código) — cada uno tiene su propia variable y su propio valor.
- El mecanismo (patrón `_FILE`, `backend/docker-entrypoint.sh`) es agnóstico del proveedor: en el
  servidor real, cada archivo sale del gestor de secretos cloud elegido (Vault, el del proveedor,
  SOPS/age), nunca de `secrets/generate-dev-secrets.sh` (sólo para verificación local).

## 4. Certificados TLS (Sprint 26)

Caddy (`infra/caddy/Caddyfile`) gestiona el certificado automáticamente:

- Con `WEB_DOMAIN`/`API_DOMAIN` apuntando a un dominio público real: Let's Encrypt real, renovación
  automática, sin intervención manual.
- Sin dominio propio (`*.localhost`): CA interna propia de Caddy — sólo para verificación, nunca
  para producción real de cara a usuarios.

No hay gestión manual de certificados que hacer en operación normal — si Caddy no puede renovar
(dominio dejó de resolver, puerto 80/443 bloqueado), lo registra en sus logs (visibles en Loki).

## 5. CI/CD y aprobación de despliegue (Sprint 26)

`.github/workflows/cd.yml` construye y publica imágenes en GHCR automáticamente tras cada `ci`
verde en `main` (`build-and-push`). El job `deploy-production` queda detrás de un *environment* de
GitHub llamado `production`:

- Mientras `vars.PROD_HOST` no esté configurado, el job se salta (nunca falla) — no hay servidor
  real todavía.
- Cuando exista un servidor real, quien administre el repositorio debe configurar ese
  *environment* con un revisor obligatorio (Settings → Environments → production) — exige permisos
  de administrador sobre el repositorio de GitHub, no sólo de colaborador (ver
  `docs/sprint-26-evidence.md`, sección 14).
- Una vez configurado, cada despliegue a producción real queda pendiente de que ese revisor lo
  apruebe manualmente en la pestaña Actions de GitHub antes de ejecutarse.

## 6. Responder a alertas de manipulación (Sprint 20)

Estas alertas llegan a la bandeja del tutor (`AlertsPanel`/`TutorScreen`), no a un panel de
administrador separado — como administrador de infraestructura, tu rol es asegurarte de que el
canal de alertas (WebSocket, Sprint 18) y el propio backend estén operativos para que esas alertas
lleguen. Si un tutor reporta que dejó de recibir alertas, revisar:

1. `GET /api/v1/health/ready` — el backend está sano.
2. Logs del backend (Loki) por errores en `app/services/tamper.py` o `app/services/alerts.py`.
3. Que el dispositivo del tutor mantenga la conexión WebSocket activa (Sprint 18) — si no, cae al
   sondeo periódico normal de la app, con más latencia pero sin pérdida de alertas.

## 7. Solicitudes de datos de usuarios (privacidad)

Ver `docs/manuals/politica-privacidad.md` para qué datos existen y dónde. Para una solicitud de
eliminación de cuenta o de datos de un dispositivo:

- Desvincular un dispositivo (`DELETE` sobre el vínculo tutor-dispositivo) no borra su historial
  retroactivamente — sólo corta el acceso desde ese momento (ver
  `docs/security-baseline.md`, regla de diseño "Desvincular revoca el acceso de inmediato").
- Los datos de ubicación y eventos purgan automáticamente por su propia retención (7/90 días, ver
  política de privacidad) — no existe hoy un endpoint de "borrar todo de este usuario ya" bajo
  demanda; es un hueco real, documentado en `docs/manuals/analisis-riesgos.md`.

## 8. Qué NO puede hacer un administrador de infraestructura

Por diseño (mínimo privilegio, `docs/security-baseline.md`): un administrador de infraestructura
que no es Tutor de un dispositivo no puede leer sus reglas, ubicación ni historial a través de la
API — esas rutas exigen `require_tutor_of_device`. El acceso operacional (Docker, base de datos,
Grafana) es un privilegio de infraestructura separado del modelo de autorización de la aplicación,
y debe tratarse con el mismo cuidado que cualquier acceso directo a datos de menores: sólo cuando
sea estrictamente necesario para diagnosticar un incidente.

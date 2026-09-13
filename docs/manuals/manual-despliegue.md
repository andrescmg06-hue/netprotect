# Manual de despliegue — NetProtect

Audiencia: quien lleva NetProtect a un servidor real por primera vez. Todos los comandos de este
manual ya están verificados — sin dominio ni cuenta cloud reales — en
`docs/sprint-26-evidence.md`; este manual los organiza como procedimiento, no inventa ninguno
nuevo. Para operación continua una vez desplegado, ver `docs/manuals/manual-administrador.md`.

## 1. Qué existe ya, listo para un servidor real

Construido y verificado en el Sprint 26 (`docs/sprint-26.md`, `docs/environments.md`):

- Reverse proxy con TLS real (Caddy, Let's Encrypt automático con dominio propio, CA interna sin
  él).
- Secretos como archivos, nunca como variables de entorno en texto plano.
- Backups de PostgreSQL con restauración probada.
- Monitorización (Prometheus + Grafana + Loki + Promtail).
- CD desde GitHub Actions (`cd.yml`) que construye y publica imágenes en GHCR, con el despliegue
  real detrás de aprobación manual.

## 2. Qué falta antes de desplegar a un dominio real (pendiente de un humano)

Ninguno de estos pasos puede completarlo un agente — requieren una cuenta, un dominio, o permisos
de administrador sobre el repositorio de GitHub (`docs/sprint-26.md`, "Qué queda pendiente de un
humano"):

1. Contratar una cuenta cloud/VPS y un dominio real.
2. Elegir un gestor de secretos cloud concreto (Vault, el del proveedor, o SOPS/age) y materializar
   ahí los ocho archivos que `secrets/README.md` documenta.
3. Configurar en el repositorio de GitHub: `vars.PROD_HOST`, `vars.PROD_SSH_USER`,
   `secrets.PROD_SSH_KEY`.
4. Configurar el *environment* `production` de GitHub con un revisor obligatorio (Settings →
   Environments → production) — exige permisos de administrador sobre el repositorio.

## 3. Procedimiento de despliegue (una vez resuelto el paso 2)

### 3.1 Preparar el servidor

- Instalar Docker y Docker Compose en el servidor.
- Apuntar los registros DNS de `WEB_DOMAIN`/`API_DOMAIN` al servidor.
- Copiar `.env.production.example` a `.env.production` en el servidor y completar `WEB_DOMAIN`,
  `API_DOMAIN`, `GOOGLE_WEB_CLIENT_ID`, y los cinco valores que deben coincidir entre sí
  (`WEB_DOMAIN`, `API_DOMAIN`, `CORS_ORIGINS`, `ALLOWED_HOSTS`, `NEXT_PUBLIC_API_BASE_URL` —
  un `/code-review` real durante el Sprint 26 encontró una versión donde estos cinco no coincidían,
  ver `docs/sprint-26-evidence.md`).
- Materializar los ocho archivos de `secrets/` en el servidor desde el gestor de secretos elegido
  (nunca copiarlos desde una máquina de desarrollo).

### 3.2 Primer arranque

```bash
docker compose --env-file .env.production -f compose.prod.yaml pull backend web migrate
docker compose --env-file .env.production -f compose.prod.yaml run --rm migrate
docker compose --env-file .env.production -f compose.prod.yaml up -d
```

Verificar la cadena de certificado real (nunca `curl -k`):

```bash
curl --cacert <ca-si-aplica> https://<API_DOMAIN>/api/v1/health
```

Con un dominio público real, Caddy emite Let's Encrypt automáticamente en cuanto el DNS resuelve —
no hace falta ningún paso manual adicional de certificado.

### 3.3 Configurar CD para despliegues siguientes

1. En GitHub: Settings → Secrets and variables → Actions → agregar `PROD_HOST`, `PROD_SSH_USER`
   (variables) y `PROD_SSH_KEY` (secreto).
2. Settings → Environments → crear/editar `production` → agregar un revisor obligatorio.
3. Cada push a `main` que pase `ci` en verde dispara `cd.yml` → `build-and-push` publica la imagen
   en GHCR automáticamente → `deploy-production` queda pendiente de que el revisor lo apruebe desde
   la pestaña Actions antes de ejecutarse.

### 3.4 Verificar backups y monitorización tras el primer arranque

```bash
./infra/backup/backup.sh   # confirma que puede conectarse y producir un dump real
```

Abrir Grafana vía túnel SSH (`ssh -L 3000:127.0.0.1:3000 usuario@servidor`, luego
`http://localhost:3000`) y confirmar que el dashboard `netprotect-overview` muestra datos reales del
backend recién desplegado.

## 4. Rollback

Si un despliegue nuevo falla:

```bash
docker compose --env-file .env.production -f compose.prod.yaml pull backend web migrate  # a un tag anterior conocido en GHCR
docker compose --env-file .env.production -f compose.prod.yaml up -d
```

Las migraciones de Alembic de este proyecto no tienen un `downgrade` automatizado como parte del
pipeline — un rollback de esquema (no sólo de imagen) exige revisar la migración específica antes
de aplicar `alembic downgrade`, nunca a ciegas.

## 5. Checklist de verificación de un despliegue nuevo

- [ ] `GET https://<API_DOMAIN>/api/v1/health/ready` responde 200 sobre HTTPS real.
- [ ] El certificado es de una CA pública reconocida (Let's Encrypt), no la CA interna de Caddy.
- [ ] `GET https://<API_DOMAIN>/metrics` responde 404 (no debe ser alcanzable públicamente).
- [ ] El panel web permite iniciar sesión con Google sobre el dominio real.
- [ ] Grafana muestra el backend como `UP` y sin la alerta `BackendDown` disparada.
- [ ] Un backup real se ejecutó sin error tras el despliegue.
- [ ] El *environment* `production` de GitHub exige aprobación manual (verificar intentando un
      despliegue y confirmando que queda pendiente de revisor).

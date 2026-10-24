# Lista de salida a producción — NetProtect

Audiencia: la persona que pone NetProtect en uso real (el dueño del proyecto). Es la **única** lista
que hay que seguir, en orden, de principio a fin. Cada paso dice quién lo hace, qué ejecutar y cómo
comprobar que quedó bien. Lo que el repositorio ya resuelve está marcado como **Repo**; todo lo
demás exige una persona con una cuenta, un dominio o un dispositivo (**Dueño**).

NetProtect guarda datos de menores (ubicación, uso de apps, alertas). Ante cualquier comprobación
que falle, **no se sigue al paso siguiente**.

Convenciones: `<WEB_DOMAIN>` es el dominio del panel (p. ej. `panel.tudominio`), `<API_DOMAIN>` el
de la API (p. ej. `api.tudominio`), `<IP>` la IPv4 pública del servidor. Los comandos de servidor se
ejecutan en `/opt/netprotect`, la ruta que usa `.github/workflows/cd.yml`.

## Qué ya protege el repositorio (Repo, Sprint 71)

No hace falta hacer nada; sirve para entender los errores que pueden aparecer en los pasos de abajo.

- El backend con `APP_ENV=production` **no arranca** si falta `GOOGLE_WEB_CLIENT_ID`, `CORS_ORIGINS`,
  `ALLOWED_HOSTS` o `TURN_URLS`; si cualquier valor contiene `REPLACE_WITH`, `example.com`/`.net`/
  `.org`, `.example` o `.invalid`; si `CORS_ORIGINS`/`ALLOWED_HOSTS` incluyen `localhost`,
  `127.0.0.1`, `::1` o `*`; si sigue usando un secreto de desarrollo; o si FCM está a medias o su
  archivo de clave falta o está vacío. El error nombra cada variable (`backend/app/core/config.py`).
- `./gradlew assembleRelease`/`bundleRelease` fallan si falta la URL de la API, el client ID de
  Google o cualquiera de las cuatro variables de firma (`mobile/app/build.gradle.kts`).
- `cd.yml` no publica imágenes si faltan `PROD_NEXT_PUBLIC_API_BASE_URL` o
  `PROD_GOOGLE_WEB_CLIENT_ID`, y el despliegue falla si el backend no queda sano en 2 minutos.
- `compose.prod.yaml`: `web` y `caddy` esperan a un backend sano (`/api/v1/health/ready`), cada
  servicio tiene límite de memoria y Caddy rechaza cuerpos de más de 10 MB con `413`.

## 0. Decisiones del dueño antes de empezar

| # | Decisión | Contexto |
|---|---|---|
| 0.1 | **D-01: filtrado web por VPN no construido.** Salir sin él, o construirlo antes. | `docs/tasks.md` (D-01). El nombre del proyecto habla de «navegación web», pero hoy NetProtect no filtra sitios: controla apps, horarios, ubicación y vista remota. Si se sale sin él, hay que decirlo en la política de privacidad y a los tutores. |
| 0.2 | **Política de privacidad y consentimiento.** | `docs/manuals/politica-privacidad.md` describe lo que el sistema hace, pero es académica y no es un aviso legal. Antes de tratar datos de menores reales: revisarla con la normativa aplicable, publicarla en una URL estable, definir quién responde las solicitudes y cómo (no existe borrado inmediato a pedido, riesgo R17 de `docs/manuals/analisis-riesgos.md`) y obtener el consentimiento del tutor legal. |
| 0.3 | **Certificate pinning en Android: pendiente.** | No está construido (`docs/security-baseline.md`, «Controles diferidos»): la app confía en las CA del sistema. Aceptarlo como riesgo para la salida, o construirlo después de tener el certificado real. |
| 0.4 | **Destino de alertas.** | Hoy las reglas `BackendDown` y `HighErrorRate` se ven en Prometheus/Grafana, pero **no avisan a nadie**: no hay Alertmanager ni punto de contacto. Elegir a quién y por qué canal (correo, Telegram…) antes del paso 10. |

## 1. Dominio y DNS (Dueño)

1. Comprar o elegir un dominio y decidir dos subdominios: `<WEB_DOMAIN>` y `<API_DOMAIN>`.
2. Crear dos registros `A` hacia `<IP>` (después del paso 2, cuando exista la IP).

Verificar (desde cualquier equipo):

```bash
dig +short <WEB_DOMAIN>    # debe imprimir <IP>
dig +short <API_DOMAIN>    # debe imprimir <IP>
```

**Estado real (24-oct-2026):** dominio `netprotectapp.online`, comprado en Hostinger por 1 año
(caduca el 2027-10-24), con privacidad WHOIS incluida. `<WEB_DOMAIN>` = `panel.netprotectapp.online`,
`<API_DOMAIN>` = `api.netprotectapp.online`, `<IP>` = `194.163.167.22`. Los dos registros `A`
(TTL 300) se crearon en la zona DNS de Hostinger, sin proxy: coturn necesita UDP directo. Se
comprobó que los servidores de nombres de Hostinger y los resolvedores 8.8.8.8 y 9.9.9.9
devuelven `194.163.167.22` para ambos nombres; 1.1.1.1 aún guardaba la respuesta negativa de una
consulta hecha antes de la propagación.

- **Renovación:** el dominio se renueva a COP 117.900 al año (el primer año fue COP 4.736). Se
  eligió `.online` por precio al ser un proyecto académico; con uso real conviene un `.com`.
  Cambiar de dominio después obliga a reconstruir la imagen web (la URL de la API y la CSP se
  hornean al compilar) y la app Android. Decidir antes de esa fecha si se renueva.
- Pendiente de verificar: que la autorrenovación quedó desactivada en Hostinger.

## 2. Servidor (Dueño)

1. Contratar un VPS Linux con IPv4 pública fija. Memoria: la suma de los límites de
   `compose.prod.yaml` es de unos 5,5 GB, pero son techos, no consumo; 4 GB de RAM es un mínimo
   razonable para empezar (medir después en Grafana).
2. Instalar Docker Engine con el plugin Compose v2 y Git.
3. Abrir en el cortafuegos del proveedor **y** del sistema solo estos puertos:

   | Puerto | Protocolo | Para qué |
   |---|---|---|
   | 22 | TCP | SSH (idealmente restringido a tu IP) |
   | 80, 443 | TCP | Caddy (HTTP→HTTPS y HTTPS) |
   | 3478 | UDP y TCP | TURN (coturn) |
   | 49160-49200 | UDP | Rango de relay de TURN (`infra/coturn/turnserver.conf`) |

   Grafana (3000) **no** se abre: solo escucha en `127.0.0.1` del servidor.
4. Clonar el repositorio donde lo espera el despliegue:

   ```bash
   sudo mkdir -p /opt/netprotect && sudo chown "$USER" /opt/netprotect
   git clone https://github.com/andrescmg06-hue/netprotect.git /opt/netprotect
   ```

5. Si los paquetes de GHCR del repositorio son privados, iniciar sesión con un token de GitHub de
   solo lectura (`read:packages`): `docker login ghcr.io -u <usuario>`.

Verificar: `docker compose version` responde v2; `ss -tulpn` no muestra nada escuchando en 3000
fuera de `127.0.0.1` una vez arrancado el stack.

**Estado real (24-oct-2026), servidor `194.163.167.22`:**

- **Proveedor y plan:** Contabo, Cloud VPS 4 (4 vCPU, 8 GB de RAM, 100 GB), Ubuntu 24.04, región UE.
  Contrato **mensual** (US$9,00 al mes); uno de 12 o 24 meses sale más barato, pero conviene
  decidir antes la región: desde Europa, el relay TURN añade latencia a usuarios en América.
- **Acceso:** SSH solo con llave, usuario `deploy` (con `sudo` sin contraseña). El login por
  contraseña y el de `root` están desactivados en
  `/etc/ssh/sshd_config.d/00-netprotect-hardening.conf`. El prefijo `00-` importa: SSH usa el
  primer valor que lee y `50-cloud-init.conf` activa las contraseñas. Comprobado desde fuera:
  `root` y la contraseña se rechazan con `Permission denied (publickey)`. No se restringió el
  puerto 22 a una IP porque la del dueño puede cambiar; lo protegen la llave y `fail2ban`.
- **Firewall del sistema:** `ufw` con entrada denegada por defecto y solo los puertos de la tabla;
  `fail2ban` con la regla de SSH.
- **Firewall de Contabo:** `netprotect`, asignado al servidor, con las mismas seis reglas de
  permiso y `Block all traffic` al final. Docker publica puertos saltándose `ufw`, así que este
  es el filtro que cuenta para ellos. Comprobado desde fuera: 22 abierto; 80, 443 y 3478/TCP
  rechazan la conexión (pasan el firewall, aún sin servicio); 8080, 5432 y 6379 no responden.
- **Software:** Docker Engine 29.9.2, Compose v5.6.1, Git 2.43; repositorio clonado en
  `/opt/netprotect` (rama `main`) como `deploy`.
- **Docker sin `sudo`:** `deploy` está en el grupo `docker` (`sudo usermod -aG docker deploy`),
  porque `cd.yml` ejecuta `docker compose` por SSH sin `sudo`. Equivale a darle root, pero
  `deploy` ya tiene `sudo` sin contraseña, así que no amplía el riesgo. Comprobado en una sesión
  nueva: `id` incluye `docker` y `docker ps` responde sin `sudo`.

**Pendiente en el servidor:**

- Desactivar **VNC** en el panel de Contabo (sigue habilitado y no va cifrado).
- Verificar **UDP 3478 y 49160-49200**: no se pueden comprobar desde fuera mientras coturn no
  esté corriendo (paso 9).
- La copia de seguridad automática de Contabo no está contratada (paso 11).

## 3. Google OAuth de producción, cliente web (Dueño)

En Google Cloud Console → APIs y servicios → Credenciales:

1. Crear (o reutilizar) un **ID de cliente OAuth de tipo «Aplicación web»**. En «Orígenes de
   JavaScript autorizados» poner exactamente `https://<WEB_DOMAIN>`. Nada de `localhost`.
2. Pantalla de consentimiento: si queda en modo «Prueba», solo los usuarios de prueba agregados ahí
   pueden entrar. Para uso real, publicarla (puede exigir verificación de Google).
3. Anotar el ID de cliente (termina en `.apps.googleusercontent.com`). El **mismo** valor va en
   cuatro sitios: `GOOGLE_WEB_CLIENT_ID` y `NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID` (paso 5),
   `PROD_GOOGLE_WEB_CLIENT_ID` (paso 7) y `NETPROTECT_GOOGLE_WEB_CLIENT_ID` (paso 12). El backend
   verifica que cada token de Google fue emitido para ese ID (`backend/app/services/google_auth.py`).

El cliente **Android** se crea en el paso 12, porque necesita la huella del keystore.

## 4. Secretos en `secrets/` (Dueño, en el servidor)

`compose.prod.yaml` lee estos archivos de `/opt/netprotect/secrets/`. Se generan **en el
servidor**, nunca se copian desde una máquina de desarrollo ni se suben al repositorio.

| Archivo | Contenido |
|---|---|
| `postgres_password.txt` | Contraseña de PostgreSQL |
| `redis_password.txt` | Contraseña de Redis |
| `database_url.txt` | `postgresql+asyncpg://<POSTGRES_USER>:<contraseña de postgres>@db:5432/<POSTGRES_DB>` |
| `redis_url.txt` | `redis://:<contraseña de redis>@redis:6379/0` |
| `jwt_secret.txt` | Secreto aleatorio |
| `pairing_code_pepper.txt` | Secreto aleatorio, distinto |
| `location_encryption_key.txt` | Clave Fernet (formato propio, ver abajo) |
| `turn_shared_secret.txt` | Secreto aleatorio, distinto |
| `grafana_admin_password.txt` | Contraseña de Grafana |
| `fcm_service_account.json` | **Opcional**, solo con Firebase (paso 6) |

Generar cada secreto aleatorio con la propia imagen del backend (ya trae Python y `cryptography`),
sin salto de línea final:

```bash
cd /opt/netprotect && mkdir -p secrets && chmod 700 secrets
IMG=ghcr.io/andrescmg06-hue/netprotect/backend:latest
docker pull "$IMG"
gen() { docker run --rm "$IMG" python -c "import secrets; print(secrets.token_urlsafe(48), end='')"; }
for f in postgres_password redis_password jwt_secret pairing_code_pepper turn_shared_secret grafana_admin_password; do
  gen > "secrets/$f.txt"
done
docker run --rm "$IMG" python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode(), end='')" \
  > secrets/location_encryption_key.txt
printf 'postgresql+asyncpg://%s:%s@db:5432/%s' netprotect "$(cat secrets/postgres_password.txt)" netprotect > secrets/database_url.txt
printf 'redis://:%s@redis:6379/0' "$(cat secrets/redis_password.txt)" > secrets/redis_url.txt
chmod 644 secrets/*.txt   # legibles por los usuarios de los contenedores; el directorio queda en 700
```

(En `database_url.txt` el usuario y la base, aquí `netprotect`, deben coincidir con `POSTGRES_USER`
y `POSTGRES_DB` del paso 5.)

**Guardar una copia de `location_encryption_key.txt` fuera del servidor** (gestor de contraseñas o
de secretos): sin ella, las ubicaciones de un backup restaurado son ilegibles.

Verificar: `ls secrets/` muestra los nueve `.txt`; ninguno está vacío (`wc -c secrets/*.txt`).

**Estado real (24-oct-2026):** los nueve `.txt` se generaron en el servidor con la imagen del
backend, sin imprimir ni leer ningún valor. Carpeta en `700` (`deploy`), archivos en `644`, ninguno
vacío, y `git` los ignora (comprobado con `git check-ignore` y `git status`). Tamaños coherentes:
64 bytes los secretos aleatorios, 44 la clave Fernet. **Pendiente del dueño:** copiar
`location_encryption_key.txt` a un gestor de contraseñas fuera del servidor, y guardar también la
contraseña de Grafana.

## 5. Variables en `.env.production` (Dueño, en el servidor)

Copiar `.env.production.example` a `/opt/netprotect/.env.production` y completar. Variables que
lee `compose.prod.yaml`:

| Variable | Valor |
|---|---|
| `WEB_DOMAIN`, `API_DOMAIN` | Los dos subdominios del paso 1 |
| `CORS_ORIGINS` | `https://<WEB_DOMAIN>` |
| `ALLOWED_HOSTS` | `<API_DOMAIN>,backend` — `backend` es necesario: Prometheus consulta `backend:8000/metrics` por la red interna y el filtro de hosts del backend se aplica a todas las rutas |
| `NEXT_PUBLIC_API_BASE_URL` | `https://<API_DOMAIN>` |
| `GOOGLE_WEB_CLIENT_ID`, `NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID` | El ID del paso 3 |
| `POSTGRES_DB`, `POSTGRES_USER` | Los mismos que en `database_url.txt` |
| `APP_VERSION` | Versión desplegada (p. ej. `1.0.0`) |
| `TURN_EXTERNAL_IP` | `<IP>` |
| `TURN_REALM` | Tu dominio (p. ej. `tudominio`) |
| `TURN_URLS` | `turn:<IP>:3478?transport=udp,turn:<IP>:3478?transport=tcp` |
| `GRAFANA_ADMIN_USER` | Opcional (por defecto `admin`) |
| `BACKUP_RETENTION_DAYS`, `BACKUP_INTERVAL_SECONDS` | Opcionales (7 días, cada 24 h) |
| `FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_SECRET_FILE` | Solo con el paso 6; si no, vacíos |

Los cinco valores de dominio (`WEB_DOMAIN`, `API_DOMAIN`, `CORS_ORIGINS`, `ALLOWED_HOSTS`,
`NEXT_PUBLIC_API_BASE_URL`) deben coincidir entre sí.

Verificar que Compose resuelve todo (no imprime valores):

```bash
docker compose --env-file .env.production -f compose.prod.yaml config -q && echo OK
```

**Estado real (24-oct-2026):** `.env.production` escrito en el servidor (`600`, ignorado por `git`)
con los valores no secretos: dominios `panel.`/`api.netprotectapp.online`, el ID de cliente del
cliente OAuth web de producción, `POSTGRES_DB`/`POSTGRES_USER` = `netprotect`, `APP_VERSION=1.0.0`,
`TURN_EXTERNAL_IP=194.163.167.22`, `TURN_REALM=netprotectapp.online` y `TURN_URLS` por UDP y TCP.
FCM queda vacío. Añade además `BACKEND_IMAGE=netprotect-backend:local` y
`WEB_IMAGE=netprotect-web:local` (ver paso 8). `docker compose ... config -q` resolvió todo.

## 6. Firebase / FCM (Dueño, opcional)

FCM sirve para despertar a un teléfono sin el canal en tiempo real abierto. **Hoy la app Android
no tiene cliente de FCM** (no incluye el SDK de Firebase ni registra su token), así que configurar
FCM en el servidor **no tiene efecto práctico todavía**: la app ya refresca por sondeo. Se puede
dejar vacío sin perder nada.

Si aun así se configura: crear el proyecto en Firebase, generar una clave de cuenta de servicio,
guardarla como `secrets/fcm_service_account.json` (`chmod 644`) y poner en `.env.production`
`FCM_PROJECT_ID=<id del proyecto>` y `FCM_SERVICE_ACCOUNT_SECRET_FILE=./secrets/fcm_service_account.json`.
Si falta el archivo, el backend no arranca y lo dice.

## 7. GitHub: variables, secreto y entorno `production` (Dueño, con permisos de administrador)

**Hacer este paso antes de fusionar el Sprint 71 en `main`**: desde ese sprint, `cd.yml` falla en
cada push a `main` mientras falten las dos variables `PROD_*` de la web.

Settings → Secrets and variables → Actions:

| Tipo | Nombre | Valor |
|---|---|---|
| Variable | `PROD_NEXT_PUBLIC_API_BASE_URL` | `https://<API_DOMAIN>` |
| Variable | `PROD_GOOGLE_WEB_CLIENT_ID` | El ID del paso 3 |
| Variable | `PROD_HOST` | `<IP>` o un nombre que resuelva a ella |
| Variable | `PROD_SSH_USER` | Usuario SSH del servidor con acceso a Docker y a `/opt/netprotect` (en el servidor real, `deploy`, que ya está en el grupo `docker`; ver paso 2) |
| Secreto | `PROD_SSH_KEY` | Clave privada SSH de despliegue (una propia para esto, no la personal) |

La llave que hoy autoriza a `deploy` es la de administración del dueño; para `PROD_SSH_KEY` hay que
generar **otra** (`ssh-keygen -t ed25519`) y añadir solo su parte pública a
`/home/deploy/.ssh/authorized_keys`, de modo que se pueda revocar sin perder el acceso de
administración.

Settings → Environments → `production` → «Required reviewers»: agregarte (pendiente H-04 en
`docs/tasks.md`; exige ser administrador del repositorio).

Verificar: Actions → `cd` → «Run workflow» sobre `main`. `build-and-push` termina en verde y
`deploy-production` queda **esperando aprobación**. Si `build-and-push` falla en su primer paso,
el mensaje dice qué variable falta.

## 8. Primer despliegue (Dueño)

Aprobar `deploy-production` en Actions, o bien en el servidor:

```bash
cd /opt/netprotect
docker compose --env-file .env.production -f compose.prod.yaml pull backend web migrate
docker compose --env-file .env.production -f compose.prod.yaml run --rm migrate
docker compose --env-file .env.production -f compose.prod.yaml up -d
```

Verificar:

```bash
docker compose --env-file .env.production -f compose.prod.yaml ps      # backend, web y caddy «healthy»
curl -sS https://<API_DOMAIN>/api/v1/health/ready                       # {"status":"ready",...}
curl -s -o /dev/null -w '%{http_code}\n' https://<API_DOMAIN>/metrics   # 404
curl -sv https://<WEB_DOMAIN>/ 2>&1 | grep -i issuer                    # Let's Encrypt, no la CA interna de Caddy
```

Sin `-k`: si `curl` no valida el certificado, algo está mal (DNS o puertos 80/443). Si el backend
no queda sano: `docker compose --env-file .env.production -f compose.prod.yaml logs backend` — un
error de configuración nombra la variable.

Después, iniciar sesión con Google en `https://<WEB_DOMAIN>` con una cuenta real.

**Estado real (24-oct-2026), primer despliegue hecho a mano en el servidor:**

- **Las imágenes se construyeron en el servidor** desde el clon (`183d18a`) con
  `docker build`, y no se descargaron de GHCR. Las de GHCR eran del 15-oct, sin etiqueta de
  commit, y la web hornea `NEXT_PUBLIC_API_BASE_URL` al compilarse (aquí
  `https://api.netprotectapp.online`), así que no se podía garantizar que fueran las correctas. Por
  eso `.env.production` apunta a `netprotect-backend:local` y `netprotect-web:local`. Mientras no
  existan las variables `PROD_*` de GitHub (paso 7), el CI no publica imágenes propias.
- **Migraciones aplicadas** completas; los 11 servicios arrancaron (`backend`, `web`, `caddy`, `db`
  y `redis` en `healthy`).
- **Comprobado desde fuera, sin `-k`:** `GET /api/v1/health/ready` devuelve `ready` con base de datos
  y Redis conectados; `/metrics` devuelve 404; el panel responde 200; los certificados de
  `api.` y `panel.` los emite Let's Encrypt; 22, 80, 443 y 3478/TCP están abiertos; coturn contestó a
  una petición STUN por UDP 3478 (`Binding Success`).
- **Sin comprobar todavía:** el rango de relay UDP 49160-49200 (hace falta una asignación TURN real,
  paso 13), el inicio de sesión con Google en el panel real, y la vista remota.
- Al volver a desplegar con estas imágenes locales hay que reconstruirlas a mano (`docker build`
  de `backend` y `web`) antes del `up -d`; un `pull` no las actualiza.

## 9. TURN (Dueño)

Verificar desde fuera del servidor que los puertos responden (por ejemplo con
`nc -vzu <IP> 3478` y `nc -vz <IP> 3478`), y la prueba real en el paso 13: una vista remota con el
teléfono en datos móviles y el panel en otra red. Sin TURN funcionando, la vista remota solo sirve
cuando ambos extremos se alcanzan directamente.

## 10. Monitorización y alertas (Dueño)

1. Grafana por túnel SSH, nunca por una URL pública:

   ```bash
   ssh -L 3000:127.0.0.1:3000 <PROD_SSH_USER>@<IP>
   ```

   Abrir `http://localhost:3000`, entrar con `GRAFANA_ADMIN_USER` y `secrets/grafana_admin_password.txt`.

   **Hallazgo del servidor real (24-oct-2026):** con Docker 29.9.2 el puerto `127.0.0.1:3000` de
   Grafana **no se publica**, a pesar del comentario de `compose.prod.yaml`: la red `private` es
   `internal` y `docker port` no devuelve nada. La causa exacta no está verificada; la evidencia es
   la red interna y el `HTTP 000` en `127.0.0.1:3000`. Grafana sí funciona (`HTTP 200` por la IP
   del contenedor, p. ej. `172.18.0.9`) y desde fuera el 3000 está cerrado. Mientras no se
   corrija el compose, el túnel debe apuntar a esa IP interna:

   ```bash
   IP=$(docker inspect netprotect-prod-grafana-1 --format '{{(index .NetworkSettings.Networks "netprotect-prod_private").IPAddress}}')
   ssh -L 3000:$IP:3000 <PROD_SSH_USER>@<IP_DEL_SERVIDOR>
   ```

   La IP del contenedor puede cambiar al recrearlo: hay que volver a consultarla.
2. Verificar que el panel `netprotect-overview` muestra datos del backend y que en Prometheus
   (Grafana → Explore) `up{job="netprotect-backend"}` vale `1`.
3. Destino de alertas (decisión 0.4): **pendiente de trabajo en el repositorio**. Hoy no hay
   Alertmanager, ni node-exporter (sin métrica de disco) ni métrica de backups, así que no existen
   alertas de disco lleno ni de backup fallido (`infra/monitoring/alert_rules.yml`). Mientras no se
   construya, revisar a mano cada semana: `df -h` en el servidor y el último backup (paso 11).

## 11. Backups fuera del servidor (Dueño)

El contenedor `backup` hace un `pg_dump` al arrancar y cada 24 h en el volumen
`netprotect-prod_backups_data`, **en el mismo servidor**: si el servidor se pierde, se pierden con él.

1. Comprobar que existe el primero:

   ```bash
   docker compose --env-file .env.production -f compose.prod.yaml logs backup | tail -5   # «[backup] wrote ...»
   docker run --rm -v netprotect-prod_backups_data:/b alpine ls -l /b
   ```

2. Copiarlos fuera con regularidad (otro servidor, almacenamiento de objetos…), **cifrados**: tienen
   datos de menores. Junto a ellos, pero aparte, la clave `location_encryption_key.txt` (paso 4).
3. Probar una restauración al menos una vez, **nunca en el servidor de producción**: `restore.sh`
   borra y recrea la base del proyecto de Compose donde se ejecuta, y en el servidor real ese es
   `netprotect-prod`. Hacerlo en otra máquina con su propio checkout y su propio `.env.production`,
   tras copiar allí el `.dump` (y la clave `location_encryption_key.txt`) al volumen de backups:

   ```bash
   docker compose --env-file .env.production -f compose.prod.yaml run --rm --entrypoint /scripts/restore.sh backup /backups/netprotect-<fecha>.dump
   ```

## 12. App Android de release (Dueño, en su PC)

1. **Keystore** (una sola vez; guardarlo con copia de seguridad fuera del repositorio: si se pierde,
   no se puede actualizar la app instalada):

   ```bash
   keytool -genkeypair -v -keystore netprotect-release.jks -alias netprotect -keyalg RSA -keysize 4096 -validity 10000
   ```

2. **Huella SHA-1** y **cliente OAuth Android**: en la misma consola del paso 3, crear un ID de
   cliente de tipo «Android» con el paquete `com.netprotect.app` y la SHA-1 de:

   ```bash
   keytool -list -v -keystore netprotect-release.jks -alias netprotect
   ```

3. **Variables** (entorno, o `mobile/local.properties`, que no se versiona). Ruta del keystore
   **absoluta** (una relativa se resuelve desde `mobile/app/`):

   | Variable | Valor |
   |---|---|
   | `NETPROTECT_RELEASE_API_BASE_URL` | `https://<API_DOMAIN>` |
   | `NETPROTECT_GOOGLE_WEB_CLIENT_ID` | El ID **web** del paso 3 (no el Android) |
   | `NETPROTECT_RELEASE_KEYSTORE_FILE` | Ruta absoluta al `.jks` |
   | `NETPROTECT_RELEASE_KEYSTORE_PASSWORD`, `NETPROTECT_RELEASE_KEY_ALIAS`, `NETPROTECT_RELEASE_KEY_PASSWORD` | Los del paso 1 |
   | `NETPROTECT_VERSION_CODE` | Entero, **mayor que el de la versión instalada** (la primera puede ser `2`; debug usa `1`) |
   | `NETPROTECT_VERSION_NAME` | p. ej. `1.0.0` |

4. Compilar y comprobar la firma:

   ```bash
   cd mobile
   ./gradlew assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk   # SHA-1 = la del paso 2
   ```

   Si falta algo, Gradle se detiene con la lista de variables que faltan.
5. Instalar en el teléfono del menor: `adb install -r app/build/outputs/apk/release/app-release.apk`
   (sideload; publicar en Google Play exige además sus políticas de permisos y anti-stalkerware,
   ver `docs/android/capability-matrix.md`).

## 13. Pruebas en dispositivo y con personas (Dueño)

Con la release del paso 12 contra producción, no con la build de debug:

1. Recorrido completo en el teléfono: iniciar sesión, vincular con un código del panel, conceder
   permisos, bloquear una app desde el panel y comprobarlo, ubicación y geocercas, vista remota de
   punta a punta con un tutor real (en datos móviles, para probar TURN), desvincular.
2. Accesibilidad y casos reales en el Galaxy S25 FE:
   `docs/android-redesign/supervisado-v2/06_GUIA_VERIFICACION_GALAXY.md`.
3. Prueba con cinco personas: `docs/android-redesign/supervisado-v2/05_PRUEBA_USUARIOS.md`.

Anotar el resultado de cada prueba en el sprint correspondiente (`docs/sprint-NN-evidence.md`); lo
que no se pudo probar se declara pendiente.

## 14. Salida

- [ ] Decisiones 0.1–0.4 tomadas y escritas.
- [ ] Pasos 1–12 verificados con su comprobación.
- [ ] Pruebas del paso 13 hechas o declaradas pendientes, con el riesgo aceptado por escrito.
- [ ] Política de privacidad publicada y consentimiento del tutor legal obtenido.
- [ ] Una copia de backup y la clave de ubicación fuera del servidor.

Operación diaria después de la salida: `docs/manuals/manual-administrador.md`. Rollback de una
versión: `docs/manuals/manual-despliegue.md`, sección 4.

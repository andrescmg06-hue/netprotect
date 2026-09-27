# Sprint 28 — Evidencia

## Preparación: correcciones previas con CI en verde

PR [#1](https://github.com/andrescmg06-hue/netprotect/pull/1), rama `sprint-28-turn`, commits
`ff58063` (correcciones de API de Android y CSP de Google Identity Services) y `58d60b4` (plan de
los Sprints 28–30). CI: **8/8 jobs en verde** (`backend`, `frontend`, `android`, `integration`,
`android-instrumented`, `api-collection`, `e2e`, `performance`).

## Spike R1 — `denied-peer-ip` frente al relay entre dos asignaciones del mismo coturn

**Pregunta:** si coturn bloquea las redes privadas para que el TURN no sirva para alcanzar la red
interna del servidor (PostgreSQL, Redis), ¿bloquea también el relay entre dos clientes del mismo
TURN? La dirección de relay de coturn está dentro de la red de Docker.

**Montaje:** `coturn/coturn:4.7.0` en una red Docker aislada (`172.19.0.0/16`), con
`use-auth-secret` y `denied-peer-ip` para 0/8, 10/8, 100.64/10, 127/8, 169.254/16, 172.16/12 y
192.168/16. Un contenedor `turnutils_peer` hace de "servicio interno" en `172.19.0.3:3480`. El
cliente es `turnutils_uclient`, incluido en la misma imagen.

### Tropiezo real: la primera corrida no era válida

La primera corrida dio "éxito" en todo, incluido el relay hacia el host interno. Revisando
`docker inspect` se vio que Git Bash había reescrito el argumento `-c /etc/coturn/turnserver.conf`
como `-c C:/Program Files/Git/etc/coturn/turnserver.conf` (conversión automática de rutas de
MSYS). coturn arrancó **sin la configuración**: sin autenticación y sin bloqueos. Se repitió con
`MSYS_NO_PATHCONV=1` y se confirmó el comando real (`cmd: [-c /etc/coturn/turnserver.conf]`) antes
de aceptar ningún resultado.

**Lección para las tareas siguientes:** cualquier comando de Docker con rutas absolutas del
contenedor, lanzado desde Git Bash en Windows, necesita `MSYS_NO_PATHCONV=1`. Si no, la prueba puede
pasar en verde con una configuración que no se cargó.

### Resultados válidos (configuración cargada)

| Caso | Salida real | Resultado |
|---|---|---|
| 0. Allocate sin credencial | `ERROR: Cannot complete Allocation` / log `error 401: Unauthorized` | Rechazado, correcto |
| 1. Relay entre dos clientes del mismo coturn | `channel bind: error 403 (Forbidden IP)` | **Bloqueado: se confirma el riesgo R1** |
| 2. Relay hacia el host interno `172.19.0.3` | `channel bind: error 403 (Forbidden IP)`; log `A peer IP 172.19.0.3 denied in the range: 172.16.0.0-172.31.255.255` | Bloqueado, correcto |

### Mitigación verificada: `allowed-peer-ip` sólo para la IP propia de coturn

Se añadió `allowed-peer-ip=172.19.0.2` (la IP del propio contenedor coturn):

| Caso | Salida real | Resultado |
|---|---|---|
| 1b. Relay entre dos clientes | `tot_send_msgs=20, tot_recv_msgs=20`, `Total lost packets 0 (0.000000%)` | Funciona |
| 2b. Relay hacia el host interno | `channel bind: error 403 (Forbidden IP)` | Sigue bloqueado |

### Decisión que se deriva del spike (aplicada en la tarea 2)

coturn necesita una **IP fija** dentro de su red de Docker (`ipv4_address` en compose), y
`allowed-peer-ip` apunta sólo a esa dirección. Una IP dinámica rompería el relay en cuanto el
contenedor se recree con otra dirección, o obligaría a abrir todo el rango, con lo que la
protección de la red interna perdería su sentido.

## Tarea 2 — coturn en dev, test y prod (`infra/coturn/` y los tres `compose*.yaml`)

**Qué se agregó:** `infra/coturn/turnserver.conf`, versionado y sin secretos ni IPs, y
`infra/coturn/entrypoint.sh`, que arma la configuración de ejecución en `/tmp/turnserver.conf`
(permisos `0600`) con el secreto, el realm y `relay-ip`/`allowed-peer-ip` fijos; en producción
agrega además `external-ip`. El servicio `coturn` está en `compose.yaml`, `compose.test.yaml` y
`compose.prod.yaml`, cada uno **sólo en su propia red con IP fija** (172.30.99.10 / .98.10 /
.97.10), nunca en la red de PostgreSQL/Redis. En producción el secreto llega como Compose secret
(`secrets/turn_shared_secret.txt`, generado por `generate-dev-secrets.sh`).

Antes de escribir la configuración se comprobó, con `turnserver --help` de la imagen fijada
(`coturn/coturn:4.7.0`), que cada opción usada existe (22 de 22). También se comprobó que el
contenedor corre como `nobody` y puede escribir en `/tmp`.

### Validación de los tres compose

`docker compose -f compose.yaml config --quiet` y `docker compose -f compose.test.yaml config
--quiet` pasan. `compose.prod.yaml` con `--env-file .env.production.example` también pasa, y el
servicio resuelve `TURN_SHARED_SECRET_FILE: /run/secrets/turn_shared_secret`, el relay en
`172.30.97.10` y publica 3478 UDP/TCP y el rango 49160–49200/udp.

### Tropiezo real: `user-quota=4` rechazaba una sesión legítima

La primera corrida contra el coturn de desarrollo dio `error 486 (Allocation Quota Reached)` con
una credencial válida. Se aisló levantando el mismo coturn con y sin las líneas de cuota:

| Variante | Resultado |
|---|---|
| Configuración real (`user-quota=4`, `total-quota=40`) | `error 486 (Allocation Quota Reached)` |
| Sin ninguna cuota | `tot_send_msgs=20, tot_recv_msgs=20` |
| Sin `user-quota` | `tot_send_msgs=20, tot_recv_msgs=20` |
| Sin `total-quota` | `error 486 (Allocation Quota Reached)` |

Con `verbose` se vio el motivo: una sola sesión de prueba abre varias asignaciones con la misma
credencial (varias líneas `ALLOCATE processed, success` bajo el mismo `user <...:np>`). Un
navegador hace lo mismo: una asignación por interfaz de red local × URL de TURN × transporte, y un
host Windows con adaptadores de Docker/WSL tiene varias. Umbral medido: `user-quota=4` falla, y
`8`, `16`, `32` y `64` pasan. Se fijó **16**, con el motivo escrito en el propio
`turnserver.conf`. Sin esta prueba, el video real habría fallado en silencio con un 486.

### Verificación funcional (configuración del repo, entorno de desarrollo)

| Caso | Salida real | Resultado |
|---|---|---|
| Sin credencial | `ERROR: Cannot complete Allocation` | Rechazado |
| Secreto equivocado | `ERROR: Cannot complete Allocation` | Rechazado |
| Credencial válida, relay entre clientes | `tot_send_msgs=20, tot_recv_msgs=20`, `Total lost packets 0` | Funciona |
| Credencial válida, host interno `172.30.99.2` | `channel bind: error 403 (Forbidden IP)` | Bloqueado |

Arranque:

- el log muestra `Relay address to use: 172.30.99.10`, `Default realm: netprotect.local` y
  `--no-tcp-relay: TCP relay endpoints are not allowed`;
- los argumentos del proceso 1 son `turnserver -c /tmp/turnserver.conf`, sin
  `static-auth-secret` (conteo 0);
- `/tmp/turnserver.conf` tiene permisos `-rw------- nobody nogroup`.

### Entorno de pruebas (`compose.test.yaml`)

En la red `netprotect-test_turn_test`: sin credencial → `Cannot complete Allocation`; con el
secreto de test → `tot_send_msgs=20, tot_recv_msgs=20`.

### Camino de producción (secreto por archivo + `external-ip`)

coturn arrancado con `TURN_SHARED_SECRET_FILE` apuntando a un archivo montado y
`TURN_EXTERNAL_IP=203.0.113.10`:

- el valor del secreto no aparece en las variables del contenedor (`docker inspect`, conteo 0);
- se generan `realm=prod.example`, `relay-ip=172.30.91.10`, `allowed-peer-ip=172.30.91.10` y
  `external-ip=203.0.113.10/172.30.91.10`; el log dice
  `Whitelisting external-ip private part: 172.30.91.10`;
- con el secreto del archivo, la asignación se acepta (el cliente llega a la fase de envío,
  `tot_send_bytes ~ 200`); con un secreto distinto, `ERROR: Cannot complete Allocation`.

**Sin secreto, coturn se niega a arrancar** (`coturn: TURN_SHARED_SECRET (or
TURN_SHARED_SECRET_FILE) is required`, exit 1), en vez de quedar abierto sin autenticación.

**Lo que esta tarea todavía no verifica:** el acceso por el puerto publicado, desde el host (el
navegador) y desde el emulador (`10.0.2.2`). Eso se prueba con los clientes reales en el Sprint 29.

## Tareas 3–5 — Credenciales efímeras en el backend

**Qué se agregó:**

- `app/core/config.py`: `turn_shared_secret` (con valor de desarrollo `change_me_*`), `turn_urls`
  (vacío = sin relay) y `turn_credential_ttl_seconds=3600`. `TURN_SHARED_SECRET` entró en la
  lista de secretos que producción se niega a usar con su valor de desarrollo.
- `app/services/turn.py`: `issue_turn_credential()` devuelve
  `username = "<expira_unix>:<nonce aleatorio>"` y `credential = base64(HMAC-SHA1(secreto,
  username))`. El nonce es aleatorio en cada emisión, nunca el `user_id`.
- `WebRtcConfigResponse` ganó `turn_servers: [{urls, username, credential}]`. `ice_servers`
  quedó exactamente igual que en el Sprint 23, para que la APK instalada siga funcionando.
- Los tres compose pasan `TURN_SHARED_SECRET`/`TURN_URLS` al backend; en producción el secreto es
  el mismo Compose secret que usa coturn (`TURN_SHARED_SECRET_FILE`).

### Suite completa del backend (`compose.test.yaml`, PostgreSQL y Redis reales)

```text
backend-1  | 273 passed, 4 warnings in 36.27s
backend-1 exited with code 0
```

Antes del sprint eran 265. Las 8 nuevas son:

- 6 unitarias (`tests/test_turn_credentials.py`): sin URLs no hay credencial; formato
  `expira:nonce` con expiración = ahora + TTL; credencial = HMAC-SHA1 en base64; otro secreto da
  otra credencial; nonce distinto en cada emisión; URLs separadas y recortadas.
- 1 de integración (`test_webrtc_config_hands_both_peers_a_turn_credential_coturn_can_verify`):
  tutor y supervisado reciben `ice_servers` sin cambios y un `turn_servers` cuya credencial
  corresponde al secreto de coturn.
- El nuevo caso parametrizado de `test_production_refuses_any_single_development_default` para
  `turn_shared_secret`.

`ruff check --no-cache app tests alembic`: `All checks passed!`. La primera corrida sin
`--no-cache` falló porque ruff no tenía permiso para crear su caché dentro del contenedor, un
problema del entorno, no del código.

## Tarea 6 — Credencial del backend contra el coturn real (`scripts/verify_turn.sh`)

El script levanta el coturn endurecido de `compose.test.yaml`, pide tres credenciales al código
propio del backend (sin mocks, dentro de su contenedor, con su configuración real) y las prueba
con `turnutils_uclient` en la red `turn_test`:

```text
OK   valid credential -> relayed
OK   expired credential -> rejected
OK   tampered credential -> rejected
TURN credential verification passed
```

La credencial alterada cambia un carácter **del medio**, no el último, siguiendo la lección ya
registrada en `CLAUDE.md`: el último carácter de base64 puede llevar sólo bits de relleno.

**Control negativo** (para comprobar que la prueba puede fallar): el backend emitió una
credencial con `TURN_SHARED_SECRET=a-secret-coturn-does-not-know` y coturn respondió
`ERROR: Cannot complete Allocation`. El "relayed" de la credencial válida demuestra, por lo
tanto, que backend y coturn comparten de verdad el secreto.

El script quedó agregado como paso del job `integration` de CI, después de pytest.

### Compose revalidado

`compose.yaml` y `compose.test.yaml` pasan `config --quiet`. En `compose.prod.yaml` el backend
resuelve `TURN_SHARED_SECRET_FILE: /run/secrets/turn_shared_secret`, `TURN_URLS` desde el
entorno y el secret `turn_shared_secret` montado.

## `/security-review` del sprint

Se corrió sobre `git diff main...HEAD` más los cambios sin commit. **Resultado: ninguna
vulnerabilidad de alta confianza (≥ 8/10).** Candidatos revisados y descartados:

| Candidato | Confianza | Motivo del descarte |
|---|---|---|
| Usar el relay para llegar a servicios internos | 2/10 | coturn no comparte red con db, redis ni backend. `denied-peer-ip` cubre los rangos privados, loopback, link-local, CGNAT e IPv6; `allowed-peer-ip` sólo re-permite la IP propia de coturn, donde únicamente corre turnserver y no hay CLI. |
| Falsificar o alargar credenciales | 1/10 | El HMAC cubre el usuario completo y la expiración la fija el servidor; coturn recalcula ambos. Lo confirma `verify_turn.sh`: caducada y alterada se rechazan. |
| Credenciales para quien no participa | 1/10 | `require_device_participant` sigue en el endpoint (404 uniforme). La respuesta nunca incluye el secreto. |
| Exposición del secreto TURN | 2/10 | Fuera de argv (archivo `0600`); en producción llega como Compose secret; está en la lista de valores de desarrollo que producción rechaza. |
| Cambio de CSP de Google Identity Services | 1/10 | Sólo las entradas `accounts.google.com/gsi` que Google documenta. `'unsafe-inline'` ya existía antes. |

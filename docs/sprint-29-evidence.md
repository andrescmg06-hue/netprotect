# Sprint 29 — Evidencia

## Diagnóstico en vivo: tres fallos reales antes de que el video llegara

El código de este sprint (uso de `turn_servers` en el panel web y en Android) ya existía sin
commit al empezar la verificación. La primera prueba real, con el stack de desarrollo completo
(`docker compose -f compose.yaml`, incluido `coturn` desde el Sprint 28) y el emulador Pixel 8
(API 36), no mostró video en el panel. Se investigó con el backend real (logs de
`netprotect-dev-backend-1`), `chrome://webrtc-internals` y Logcat del emulador — nunca simulado.

### Fallo 1 (descartado): `EGL_BAD_ATTRIBUTE` en Logcat

El primer intento de diagnóstico se centró en una línea real de Logcat:

```
E/EGL_emulation: eglQueryContext 32c0  EGL_BAD_ATTRIBUTE
E/EGL_emulation: tid 3972: eglQueryContext(2159): error 0x3004 (EGL_BAD_ATTRIBUTE)
```

Se probó cambiando `Graphics acceleration` del AVD de "Automatic" a "Hardware" — el error
persistió, idéntico, en el mismo punto (`SurfaceTextureHelper: Setting listener to
org.webrtc.ScreenCapturerAndroid@...`). Con la vista remota funcionando en un intento posterior sin
que este mensaje cambiara, y con el emulador mostrando la grabación activa en todo momento (el
indicador rojo del sistema con el cronómetro), quedó descartado como causa: es un aviso propio del
emulador con WebRTC en la mayoría de sesiones de captura de pantalla, no un fallo de la app.
Registrado aquí para que ningún sprint futuro repita esta misma pista falsa.

### Fallo 2 (real): la oferta de video se enviaba antes que el token de autenticación

Con las herramientas de red del navegador (`chrome://devtools` → Network → WS → Messages) se
confirmó que, tras `screen_share_consent: true`, nunca llegaba un quinto mensaje
`screen_share_offer` — la conversación de señalización se cortaba ahí. Los logs del backend, en el
mismo instante, mostraban:

```
GET /api/v1/devices/{id}/webrtc-config  200 OK   (Android pide su configuración ICE)
WebSocket /api/v1/devices/{id}/ws  [accepted]    (el socket propio de ScreenShareService)
connection open
...
connection closed                                 (~20 s después, sin ningún mensaje relayado)
```

Causa: `RealtimeClient.connect()` (Android) enviaba `{"token": accessToken}` desde el callback
`onOpen` de OkHttp. `ScreenShareService.startSession()` llama a `connect()` y, muy poco después
(tras crear el `PeerConnectionFactory` y arrancar la captura), manda su oferta por el mismo socket.
OkHttp encola los `send()` en el orden en que se llaman, complete o no el *handshake* — así que la
oferta podía quedar encolada antes que el token si `onOpen` tardaba en dispararse. El backend
(`_authenticate()`, `backend/app/api/v1/endpoints/realtime.py`) exige que el primer frame recibido
sea el token válido y cierra la conexión si no lo es — la oferta se perdía sin dejar ningún rastro
observable, ni en el backend ni en el cliente.

**Corrección:** `RealtimeClient.kt` ahora encola el frame del token inmediatamente después de crear
el `WebSocket`, como parte de `connect()`, en vez de esperar al callback `onOpen`. Se agregó además
un registro en el backend (`screen_share_signal_dropped`, ver más abajo) para que un fallo de este
tipo deje rastro la próxima vez, en vez de un socket que se cierra en silencio.

### Fallo 3 (real): token vencido específico de la vista remota

Corregido el fallo 2, la sesión seguía sin mostrar video. Los logs del backend mostraban
`GET /webrtc-config` respondiendo **401** para Android, mientras las mismas llamadas desde
`RuleEnforcementService`/`SyncWorker` seguían en 200 en el mismo segundo. Causa:
`ScreenShareService.start()` recibe el `accessToken` que `SupervisedScreen` (Compose) tiene
capturado en su propio estado — el mismo token de sesión en primer plano que `CLAUDE.md` (nota del
Sprint 19) ya señalaba como el único componente sin refresco automático. Con la app abierta más de
15 minutos (el TTL del token), ese valor está vencido; el `runCatching` ya existente en
`ScreenShareService` convertía el 401 en "sin servidores ICE" sin ningún aviso, así que la app
seguía intentando conectar, pero sin STUN ni TURN — el mismo síntoma que el Sprint 28 se construyó
para resolver, reintroducido por esta vía distinta.

**Corrección:** `ScreenShareService.startSession()` ahora llama a
`BackgroundTokenRefresher.refresh()` al empezar cada sesión (con el token del llamador como
respaldo si el refresco falla), mismo mecanismo que Sprint 19 ya usa en los otros dos servicios de
fondo.

## Verificación en vivo — criterio de aceptación #1

Corregidos los dos fallos, la prueba se repitió completa (`docker exec` reconstruyendo el backend,
`./gradlew installDebug` reinstalando la app):

**Auditoría real** (`audit_logs`, consulta directa a `netprotect-dev-db-1`):

```
2026-09-28 00:53:50.11  SCREEN_SHARE_REQUESTED
2026-09-28 00:53:53.68  SCREEN_SHARE_CONSENT_GRANTED
2026-09-28 00:53:58.45  SCREEN_SHARE_STARTED
```

**`chrome://webrtc-internals`**, misma sesión:

- Configuración de ICE servers recibida por el navegador:
  `{"iceServers":[{"urls":["stun:stun.l.google.com:19302"]},{"urls":["turn:localhost:3478?transport=udp","turn:10.0.2.2:3478?transport=udp"]}]}`
- `Signaling state: new => "have-remote-offer" => "stable"`
- `ICE connection state: new => "checking" => "connected"`
- Par de candidatos ganador: `candidate-pair (state=succeeded, id=CP5JYZekrK_gTeuU/Oq)`
  - `local-candidate (candidateType=relay, relayProtocol=udp, id=I5JYZekrK)`
  - `remote-candidate (candidateType=relay, id=IgTeuU/Oq)`
  - **Ambos extremos del par que ganó son de tipo `relay`** — el video pasó por coturn en las dos
    direcciones, no por un candidato directo (`host`) ni reflexivo (`srflx`), aunque ambos también
    se ofrecieron.
- `transport (iceState=connected, dtlsState=connected)`
- `inbound-rtp (kind=video, frameHeight=1280, contentType=screenshare, codec=VP8 (96),
  decoderImplementation=libvpx)` — video real decodificándose, no una negociación vacía.

**Panel web:** el video de la pantalla del emulador (la propia app de NetProtect en "Modo
Supervisado") visible en vivo en "Vista remota", confirmado visualmente por el dueño del proyecto.

Con esto, el criterio de aceptación #1 del Sprint 29 queda cumplido con evidencia real, no
inferida: captura del panel con video, y par de candidatos `relay` en ambos extremos.

## Registro de frames de señalización descartados (backend)

`backend/app/api/v1/endpoints/realtime.py`, `_handle_signal()`: antes de este sprint, cualquier
frame con un tipo inválido, sin `type`, o enviado por el rol equivocado, se descartaba sin dejar
ningún rastro — decisión correcta para no tumbar una sesión por ruido, pero que hizo mucho más
lento diagnosticar el Fallo 2. Ahora un `logger.warning("screen_share_signal_dropped", ...)`
registra `device_id`, `role`, el tipo de frame (truncado a 40 caracteres), el tamaño en bytes y,
para errores de validación de esquema, un resumen de qué campo falló y por qué (`loc`/`type` de
Pydantic, truncado a 200 caracteres) — nunca `msg` ni `input`, así que el contenido de una oferta
SDP o de un candidato ICE nunca llega al log. Verificado leyendo el código que arma el mensaje: no
hay ninguna ruta donde el cuerpo del frame se incluya.

Durante las pruebas de este sprint, el contador de `screen_share_signal_dropped` se mantuvo en 0
en la corrida final — confirma que, corregidos los Fallos 2 y 3, ningún frame se está descartando.

## Verificaciones de cierre

### Backend

```
$ MSYS_NO_PATHCONV=1 docker compose -f compose.test.yaml run --rm --no-deps --entrypoint "" backend sh -c "ruff check app tests alembic"
All checks passed!

$ docker compose -f compose.test.yaml run --rm migrate
[...migraciones aplicadas sin error...]

$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
274 passed, 4 warnings in 32.28s
backend-1 exited with code 0
```

### Frontend

```
$ npx eslint .
(sin salida — limpio)

$ npx tsc --noEmit
(sin salida — limpio)
```

E2E de Playwright, contra el backend real (`compose.test.yaml`, servicio `api_server`) y una
sesión sembrada con `scripts/seed_test_session.py` (mismo mecanismo que usa CI):

```
$ npx playwright test
Running 1 test using 1 worker
  ✓  1 e2e\dashboard.spec.ts:27:5 › a returning tutor can navigate the whole dashboard and see real backend data (937ms)
  1 passed (12.0s)
```

### Android

```
$ ./gradlew testDebugUnitTest lintDebug assembleDebug
Wrote HTML report to .../lint-results-debug.html
BUILD SUCCESSFUL
```

Antes de la corrección de `LocationReportingService.kt`, `lintDebug` fallaba con 2 errores
(`MissingPermission`, ver `docs/sprint-29.md`); tras la corrección, 0 errores.

`./gradlew installDebug` — instalado y verificado en vivo en el emulador (`Pixel 8 API 36`), no
sólo compilado.

### Seguridad

`/security-review` sobre el diff completo del sprint (los 4 archivos con cambios de contenido:
`realtime.py`, `RealtimeClient.kt`, `ScreenShareService.kt`, `LocationReportingService.kt`, más los
cambios de copy en `RemoteViewPanel.tsx`/`apiClient.ts`): **sin hallazgos.** Revisó específicamente
que los nuevos `logger.warning`/`Log.w` no filtran el contenido de una oferta SDP, un candidato ICE
ni un token, y que reordenar el envío del token en `RealtimeClient` no debilita la autenticación
(el backend sigue exigiendo y validando el mismo primer frame). El detalle de qué se revisó y se
descartó está en el propio análisis, no repetido aquí.

### CI en GitHub Actions

Pendiente: los pasos anteriores se verificaron localmente, contra los mismos comandos y el mismo
`compose.test.yaml` que usa cada job de CI, pero la corrida real en un runner limpio de GitHub
Actions queda pendiente hasta el push de este commit.

## Archivos modificados en este sprint

```
backend/app/api/v1/endpoints/realtime.py           | 31 +++++++++++++++++--
frontend/src/components/RemoteViewPanel.tsx        | 22 ++++++++-----
frontend/src/lib/apiClient.ts                      |  9 ++++++
mobile/.../location/LocationReportingService.kt    |  6 ++++
mobile/.../network/RealtimeClient.kt               | 16 +++++-----
mobile/.../network/WebRtcConfigClient.kt           | 36 +++++++++++++++++++---
mobile/.../screenshare/ScreenShareService.kt        | 36 +++++++++++++++++-----
```

(El resto de archivos que aparecen en `git status` —`docker-entrypoint.sh`, `infra/backup/*.sh`,
`infra/coturn/entrypoint.sh`, `gradlew`, `scripts/*`, `secrets/generate-dev-secrets.sh`— son sólo
cambios de permiso de ejecución (755→644), ruido de Windows/Git Bash sin ningún cambio de
contenido; no se incluyen en el commit de este sprint.)

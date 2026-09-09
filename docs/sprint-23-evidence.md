# Sprint 23 — Evidencia de ejecución

Fecha: 09/09/2026. Máquina: Windows 11 + Docker Desktop, Android Studio (SDK 36).

Este documento recoge lo que **se ejecutó de verdad**, incluidos los errores encontrados por el
camino, y separa explícitamente lo que **no se pudo verificar sin una persona**.

## 1. Verificación previa de la API (Fase C)

Antes de escribir código Android se consultaron las fuentes oficiales sobre `MediaProjection` y
sobre la disponibilidad real de una librería WebRTC para Android. Los hallazgos y sus citas
textuales quedaron en `docs/android/capability-matrix.md`, sección "Sprint 23". Dos de ellos
cambiaron el código antes de escribirlo:

1. `MediaProjection.Callback` **debe** registrarse antes de `createVirtualDisplay()`, o Android
   lanza `IllegalStateException`. Por eso `ScreenShareService` lo pasa al constructor de
   `ScreenCapturerAndroid` y lo usa además para enterarse de que el sistema cortó la captura.
2. El consentimiento **no es reutilizable**: un `MediaProjection` sirve para una sola sesión. Eso
   descartó de entrada cualquier diseño con "permiso guardado", y es lo que hace que la tarjeta de
   consentimiento tenga que aparecer en cada solicitud.

## 2. Backend — suite completa en Docker

Procedimiento de `CLAUDE.md` (build, `migrate` aparte, luego `up`):

```
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

Resultado real:

```
backend-1  | 260 passed, 4 warnings in 111.49s (0:01:51)
```

### 2.1. Error real encontrado: la suite se colgó, no falló

**Primera ejecución: pytest se detuvo en el 55% y se quedó colgado ocho horas** (contenedor
`netprotect-test-backend-1` con `Up 8 hours` y la barra de progreso parada en `[ 55%]`). No fue un
fallo de aserción: fue un bloqueo indefinido.

Causa: una condición de carrera real, no un problema del test. El relevo de señalización busca los
sockets del rol contrario **en el momento en que llega el frame**. Un test conectaba el socket del
dispositivo, e inmediatamente conectaba el del tutor y enviaba `screen_share_request`. Como la
autenticación del primer socket se procesa de forma asíncrona, el servidor podía no haberlo
registrado todavía: la solicitud se relevaba a **cero** destinatarios, se descartaba en silencio, y
el `receive_json()` del test esperaba para siempre un mensaje que nunca iba a llegar.

Corrección: el servidor ahora envía `{"event": "connected", ...}` justo después de registrar la
conexión (`app/api/v1/endpoints/realtime.py`). No es un parche para los tests — arregla también el
caso real: antes, **ningún** cliente tenía forma de saber que su token fue aceptado salvo por la
ausencia de un cierre, y eso no es algo a lo que se pueda esperar. Los clientes que ya existían
ignoran los eventos que no conocen, así que el cambio es aditivo.

Tras la corrección, la misma suite pasa completa en 72 segundos.

### 2.2. Hallazgos de `/security-review` — dos vulnerabilidades reales, corregidas

La revisión de seguridad obligatoria antes de cerrar sprint encontró **dos problemas reales**
introducidos por este sprint. No eran teóricos; ambos se corrigieron y ambos tienen ahora una
prueba propia que falla si se reintroducen.

**1. La oferta llegaba a todos los tutores conectados, no al que la pidió (Media).** Un dispositivo
puede tener varios tutores activos a la vez (dos padres, o un co-tutor a punto de ser revocado), y
todos pueden tener el canal abierto. `relay_to_peer()` enviaba al *rol* contrario, así que la
oferta del dispositivo —que lleva las credenciales de la conexión de medios— se difundía a todos:
el primero en responder se quedaba con el video. Peor, la auditoría seguía nombrando al tutor que
había pedido, no al que estaba mirando. La tarjeta de consentimiento tampoco dice qué tutor pide,
así que la persona supervisada no podía notar la diferencia.

Corrección: la sesión queda **anclada a la conexión concreta** que envió la solicitud
(`ConnectionManager.begin_screen_share` / `relay_to_screen_share_peer`). Los frames del dispositivo
van sólo a esa conexión, y un frame de cualquier otro tutor se descarta. Prueba:
`test_the_stream_only_reaches_the_tutor_who_asked_for_it`.

**2. Un tutor desvinculado seguía pudiendo abrir sesiones con el socket que ya tenía abierto
(Media).** La autenticación ocurría una sola vez, en el *handshake*, y nada cierra un WebSocket
cuando se desvincula a un tutor o se desactiva una cuenta. Eso era inofensivo mientras el canal
sólo empujaba `rules_changed`; deja de serlo cuando ese mismo socket puede pedir ver la pantalla de
un menor.

Corrección: el frame que **abre** una sesión (`screen_share_request`) revalida contra la base de
datos que el permiso sigue vigente (`_still_authorized`); los demás frames sólo pueden existir
dentro de una sesión que esa comprobación permitió. Prueba:
`test_an_unlinked_tutor_cannot_start_a_session_on_a_socket_it_already_had_open`.

### 2.3. Segundo error real: un fallo invisible dentro de una tarea WebSocket

Al añadir la revalidación, la suite volvió a colgarse — esta vez sin traza ninguna. La causa:
`_still_authorized()` llama a `db.expire_all()` (necesario, o la sesión respondería con el vínculo
que ya tenía cargado en memoria y no vería la revocación), y el objeto `User` que la conexión venía
arrastrando desde el *handshake* quedaba caducado. Leer cualquier atributo suyo después —incluido
`user.id` para la fila de auditoría— dispara una recarga perezosa que, dentro de una tarea de
WebSocket, **muere en silencio**: sin excepción visible en la salida de pytest, sin cierre del
socket, y con el otro extremo esperando para siempre un mensaje que ya nunca iba a llegar.

Se localizó instrumentando temporalmente la función con `print(..., flush=True)` para ver qué rama
tomaba; confirmó que la autorización devolvía `TUTOR` correctamente y que el problema estaba
después. Corrección: la conexión guarda **sólo el `user_id`** (`_authenticate` devuelve
`tuple[uuid.UUID, str]`), nunca la instancia ORM, así que no hay nada que recargar.

Lección aplicable a cualquier sprint futuro que toque este canal: **una excepción dentro de la
tarea de un WebSocket no se ve**; se manifiesta como un test colgado, no como un test en rojo.

### 2.4. Pruebas de señalización, en detalle

```
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml run --rm --entrypoint "" backend \
  sh -c "python -m pytest tests/test_realtime_integration.py -v"
```

```
tests/test_realtime_integration.py::test_ws_rejects_a_missing_token PASSED [  4%]
tests/test_realtime_integration.py::test_ws_rejects_an_invalid_token PASSED [  9%]
tests/test_realtime_integration.py::test_ws_rejects_a_tutor_who_is_not_linked_to_this_device PASSED [ 14%]
tests/test_realtime_integration.py::test_ws_rejects_a_device_id_that_does_not_exist PASSED [ 19%]
tests/test_realtime_integration.py::test_ws_accepts_the_owning_tutor_and_the_supervised_device PASSED [ 23%]
tests/test_realtime_integration.py::test_a_rule_change_is_pushed_live_to_the_connected_device PASSED [ 28%]
tests/test_realtime_integration.py::test_a_policy_change_is_pushed_live_to_a_connected_tutor_viewer PASSED [ 33%]
tests/test_realtime_integration.py::test_the_supervised_device_can_register_its_own_push_token PASSED [ 38%]
tests/test_realtime_integration.py::test_a_tutor_cannot_register_a_push_token_for_the_device_they_supervise PASSED [ 42%]
tests/test_realtime_integration.py::test_a_rule_change_wakes_a_disconnected_device_with_a_registered_token PASSED [ 47%]
tests/test_realtime_integration.py::test_a_rule_change_does_not_wake_a_device_that_is_already_connected PASSED [ 52%]
tests/test_realtime_integration.py::test_a_tutor_request_reaches_the_connected_device PASSED [ 57%]
tests/test_realtime_integration.py::test_the_full_offer_answer_exchange_is_relayed_between_the_two_peers PASSED [ 61%]
tests/test_realtime_integration.py::test_a_denied_consent_is_relayed_and_audited PASSED [ 66%]
tests/test_realtime_integration.py::test_a_peer_cannot_send_a_frame_that_belongs_to_the_other_role PASSED [ 71%]
tests/test_realtime_integration.py::test_a_malformed_frame_is_ignored_without_closing_the_channel PASSED [ 76%]
tests/test_realtime_integration.py::test_a_request_wakes_a_device_that_is_not_connected PASSED [ 80%]
tests/test_realtime_integration.py::test_the_stream_only_reaches_the_tutor_who_asked_for_it PASSED [ 85%]
tests/test_realtime_integration.py::test_an_unlinked_tutor_cannot_start_a_session_on_a_socket_it_already_had_open PASSED [ 90%]
tests/test_realtime_integration.py::test_both_peers_can_read_the_webrtc_config PASSED [ 95%]
tests/test_realtime_integration.py::test_an_unrelated_tutor_cannot_read_the_webrtc_config PASSED [100%]
======================= 21 passed, 3 warnings in 14.00s ========================
```

Las diez pruebas nuevas (de `test_a_tutor_request_reaches_the_connected_device` en adelante) corren
contra PostgreSQL y Redis reales en contenedor, con **dos clientes WebSocket** simultáneos —uno
autenticado como tutor y otro como dispositivo supervisado— sobre el backend real. Lo que
verifican, además del relevo en sí:

- El intercambio completo (solicitud → consentimiento → oferta → respuesta → ICE → parada) llega
  al extremo correcto en cada dirección.
- Las filas de auditoría aparecen con el **actor correcto**: la solicitud y la parada en el
  registro del tutor; el consentimiento y el inicio real, en el de la persona supervisada.
- Un extremo no puede enviar un frame que le corresponde al otro rol (el dispositivo intentando
  emitir `screen_share_request`, el tutor intentando emitir `screen_share_consent`): ninguno se
  releva y ninguno se audita.
- Un frame malformado, uno con SDP vacío, uno con SDP de 20 000 caracteres y un *ping* de texto
  plano se ignoran **sin cerrar el canal** — el siguiente frame legítimo sigue llegando.
- Un tutor ajeno recibe 404 en `GET /devices/{id}/webrtc-config` (mismo criterio anti-IDOR del
  resto del proyecto).

### 2.5. Ruff

```
python -m ruff check app tests alembic
All checks passed!
```

## 3. Android — compilación real

```
./gradlew.bat compileDebugKotlin
BUILD SUCCESSFUL in 26s

./gradlew.bat assembleDebug
BUILD SUCCESSFUL in 1m 8s
40 actionable tasks: 22 executed, 18 up-to-date
```

Detalle relevante de la salida de `assembleDebug`, que confirma que la dependencia de WebRTC se
resolvió y sus binarios nativos quedaron empaquetados en el APK:

```
> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are:
libandroidx.graphics.path.so, libjingle_peerconnection_so.so.
```

`libjingle_peerconnection_so.so` es la biblioteca nativa de libwebrtc. El manifiesto con el
`<service>` de tipo `mediaProjection` y el permiso `FOREGROUND_SERVICE_MEDIA_PROJECTION` se fusionó
sin errores (`processDebugMainManifest`, `processDebugManifest`).

## 4. Web — lint y build

```
npm run lint
> eslint . --max-warnings=0
(sin hallazgos)

npm run build
✓ Compiled successfully in 25.6s
  Running TypeScript ...
  Finished TypeScript in 9.3s
✓ Generating static pages using 4 workers (3/3) in 2.2s
```

El *build* de Next.js incluye la verificación de TypeScript, así que cubre también los tipos del
visor WebRTC nuevo (`RTCPeerConnection`, `RTCIceCandidateInit`).

## 5. Lo que NO se verificó — pendiente de una persona

Esto se declara explícitamente en vez de darlo por bueno, siguiendo la regla central de
`CLAUDE.md`.

### 5.1. El diálogo de consentimiento de Android

`MediaProjectionManager.createScreenCaptureIntent()` abre una pantalla del **sistema operativo**.
Ningún agente puede pulsar "Iniciar ahora" ahí, igual que no puede elegir una cuenta en el selector
de Google (Sprint 3). No es una limitación de este proyecto: es el punto exacto en el que Android
exige una persona.

### 5.2. La conexión WebRTC extremo a extremo (video real llegando al navegador)

Exige el emulador/dispositivo **y** un navegador reales negociando entre sí. No se ejecutó.

Pasos exactos para comprobarlo manualmente:

1. Levantar el stack de desarrollo: `docker compose -f compose.yaml up -d` (y, si se tocó alguna
   migración, reconstruir los tres servicios juntos: `docker compose -f compose.yaml build backend
   web migrate`).
2. Instalar el APK en el emulador y entrar como la cuenta **supervisada**, con el dispositivo ya
   vinculado. Dejar la app abierta en la pantalla del supervisado.
3. En el panel web, entrar como el **tutor** de ese dispositivo, desplegar el dispositivo y pulsar
   "Ver pantalla" → "Solicitar ver pantalla".
4. En el emulador debe aparecer la tarjeta "Tu tutor quiere ver esta pantalla". Pulsar "Aceptar".
5. Android muestra entonces su propio diálogo de captura. Confirmarlo.
6. Comprobar: (a) el video aparece en el panel web; (b) el emulador muestra una notificación
   permanente con el botón "Detener"; (c) pulsar "Detener" corta la transmisión y el panel web
   muestra "La persona supervisada detuvo la transmisión".
7. Repetir pulsando "Ahora no" en el paso 4: el panel debe mostrar "La persona supervisada no
   aceptó la solicitud".
8. Comprobar la auditoría: `GET /api/v1/users/me/audit` con el token del tutor debe mostrar
   `SCREEN_SHARE_REQUESTED` y `SCREEN_SHARE_STOPPED`; con el token del supervisado,
   `SCREEN_SHARE_CONSENT_GRANTED` / `SCREEN_SHARE_CONSENT_DENIED` y `SCREEN_SHARE_STARTED`.

**Aviso honesto sobre el paso 6**: emulador y navegador corren en la misma máquina, que es
justamente el escenario en que STUN sin TURN sí puede funcionar. Si el paso 6 falla con el mensaje
"No se pudo establecer la conexión directa…", eso **no** es necesariamente un error del código: es
el límite ya documentado (ver `docs/sprint-23.md`, sección "Límites"), y confirmarlo en un
dispositivo físico con datos móviles requeriría un servidor TURN que este proyecto no tiene.

### 5.3. Rendimiento y latencia reales

No medidos. Se eligieron 15 fps y un máximo de 1280 px de lado largo por criterio (legibilidad
frente a ancho de banda), no a partir de mediciones — queda anotado como pendiente en
`docs/android/capability-matrix.md`.

## 6. CI

Pendiente de la corrida en GitHub Actions con los cuatro jobs (`backend`, `frontend`, `android`,
`integration`) tras el push de este sprint. Se registra aquí cuando esté en verde.

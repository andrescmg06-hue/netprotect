# Sprint 29 — Clientes web y Android usando el TURN

## Objetivo

Segundo sprint del plan `docs/planning/plan-turn.md`. El Sprint 28 dejó coturn corriendo y el
backend emitiendo credenciales efímeras, pero ningún cliente las usaba todavía. Este sprint hace
que el panel web y la app Android construyan su `RTCPeerConnection` con `turn_servers` además de
`ice_servers`, y verifica en vivo que la situación que fallaba desde el 24/09/2026 (el tutor pide
ver la pantalla y el video nunca llega) ahora funciona.

## Historias de usuario

| ID | Historia |
|---|---|
| HU-092 | Como tutor en el panel web, quiero que el visor use el TURN cuando no hay camino directo, para ver la pantalla aunque estemos en redes distintas. |
| HU-093 | Como persona supervisada, quiero que mi dispositivo pueda transmitir a través del TURN, sin perder el consentimiento en dos pasos ni el aviso permanente. |

## Criterios de aceptación y resultado

| # | Criterio | Resultado |
|---|---|---|
| 1 | Con el emulador y el navegador del host, el tutor ve la pantalla. Evidencia: captura del panel y par de candidatos `relay` en `chrome://webrtc-internals`. | **Cumplido**, verificado en vivo. Ver evidencia, sección "Verificación en vivo". |
| 2 | La APK anterior (sólo STUN) sigue funcionando igual contra el backend nuevo. | **Pendiente de verificación real.** No se conserva un build anterior instalable para probar contra el backend de este sprint. La compatibilidad se sostiene por diseño (`turn_servers` es un campo aditivo que un cliente que sólo conoce `ice_servers` ignora — decisión D6 del Sprint 28, sin cambios aquí), no por una prueba ejecutada. Se anota como pendiente en vez de darla por buena, según la regla de `CLAUDE.md`. |
| 3 | ESLint, `tsc`, la compilación de Android, la E2E de Playwright y CI pasan en verde. | ESLint, `tsc`, `./gradlew testDebugUnitTest lintDebug assembleDebug` y la E2E de Playwright (`npx playwright test`, contra el backend real de `compose.test.yaml`) verificados localmente, todos en verde. CI en GitHub Actions queda pendiente de la corrida real tras el push — ver evidencia. |

## Verificación en vivo

`RemoteViewPanel.tsx` ya traía el código para usar `turn_servers` (trabajo previo, sin commit,
encontrado al empezar este sprint) y `ScreenShareService.kt`/`WebRtcConfigClient.kt` también, pero
nunca se había probado de punta a punta. La primera prueba real, con el stack de desarrollo
completo (`compose.yaml`, incluido `coturn`) y el emulador, no mostró video — tres fallos reales
encontrados y corregidos en el camino antes de que funcionara. Ver `docs/sprint-29-evidence.md`
para el diagnóstico completo, con capturas y logs reales.

## Decisiones de diseño y su motivo

### Por qué el token va encolado dentro de `connect()`, no en el callback `onOpen`

`RealtimeClient.kt` (Android) enviaba `{"token": accessToken}` desde el callback `onOpen` de
OkHttp, que se dispara cuando termina el *handshake*. `ScreenShareService` abre su propia conexión
y, muy poco después de llamar a `connect()`, manda su oferta de video por el mismo socket. OkHttp
encola los mensajes en el orden en que se llama a `send()`, handshake terminado o no — así que
mandar el token desde `onOpen` en vez de inmediatamente después de crear el socket dejaba una
ventana real donde la oferta podía encolarse *antes* que el token. El backend cierra cualquier
conexión cuyo primer frame no sea el token válido (`_authenticate()`), así que la oferta se perdía
sin ningún rastro y la conexión se cerraba — visto en vivo, con el emulador mostrando la grabación
activa y el panel nunca recibiendo nada. La corrección es encolar el frame del token como parte de
`connect()`, antes de que la función retorne, para que quede garantizado por delante de cualquier
envío posterior del llamador — no depende de en qué callback se dispare.

### Por qué `ScreenShareService` pide un token nuevo en cada sesión

`ScreenShareService.start()` recibía el `accessToken` que `SupervisedScreen` tenía capturado en su
propio estado de Compose — el mismo token de sesión en primer plano que `CLAUDE.md` (Sprint 19) ya
documentaba como el único componente que *no* se refresca solo, a diferencia de
`RuleEnforcementService`/`SyncWorker`. Un token de acceso vive 15 minutos; con la pantalla abierta
más tiempo que eso, `GET /webrtc-config` respondía 401 y el `runCatching` existente lo convertía en
"sin servidores ICE" en silencio — la app seguía intentando conectar, pero sin STUN ni TURN,
reproduciendo exactamente el fallo que el Sprint 28 se construyó para resolver. La corrección
reutiliza `BackgroundTokenRefresher` (ya existente desde el Sprint 19) al iniciar cada sesión de
transmisión, con el token del llamador como respaldo si el refresco falla — mismo patrón ya
aceptado, aplicado al único componente de Sprint 23 que no lo tenía.

### Por qué el backend ahora registra los frames de señalización que descarta

`_handle_signal()` descartaba en silencio cualquier frame que no fuera un tipo de señalización
válido o que llegara del rol equivocado — decisión correcta para no tumbar una sesión por un ping
malformado, pero que también escondió el fallo anterior: no había ninguna forma de distinguir "no
llegó ningún frame" de "llegó un frame y se descartó por una razón concreta". Se agregó un
`logger.warning("screen_share_signal_dropped", ...)` con `device_id`, `role`, el tipo de frame, su
tamaño en bytes y (para errores de validación) un resumen truncado del campo que falló — nunca el
contenido de la oferta SDP ni de un candidato ICE. `/security-review` confirmó que ningún dato
sensible llega al log con este cambio.

### El hallazgo de `LocationReportingService.kt` no es de este sprint, pero bloqueaba su cierre

`CLAUDE.md` ya advertía que `./gradlew lintDebug` no corre en CI y es el único paso que detecta
ciertas categorías de error real. Al correrlo antes de cerrar este sprint, encontró 2 errores
(`MissingPermission`) preexistentes en un archivo que este sprint no toca: el analizador estático
de lint no puede seguir la comprobación de permiso a través de la función propia
`LocationPermission.isGranted(this)` que usa este proyecto (a diferencia de una llamada directa a
`ContextCompat.checkSelfPermission`). El código ya comprobaba el permiso antes de las dos llamadas
señaladas, y ambas ya estaban en `runCatching { }.getOrNull()`. Se corrigió con
`@SuppressLint("MissingPermission")` y un comentario que explica por qué, sin tocar la lógica —
necesario para que la compilación de Android de este sprint quede realmente en verde.

## Qué queda para los sprints siguientes

- **Sprint 30:** prueba con retransmisión forzada (`iceTransportPolicy: "relay"`), `compose.prod.yaml`
  con coturn en producción, y los manuales — según el plan original.
- **Backlog, encontrado durante este sprint, sin corregir aquí:**
  - `GET /devices/me` responde 500 (`MultipleResultsFound`) cuando la cuenta supervisada tiene más
    de un dispositivo registrado — pasa con la cuenta de prueba de este entorno, que tiene dos.
    Decisión pendiente sobre qué hacer con dispositivos duplicados de una misma cuenta.
  - `RealtimeClient` (Android) no reintenta la conexión si el socket se cae por una interrupción de
    red real — se observó en vivo durante este sprint. Hasta ahora el canal en tiempo real siempre
    se toleró como "mejor esfuerzo" (Sprint 18), pero para la vista remota una reconexión evitaría
    tener que reabrir la pantalla para volver a recibir una solicitud.
  - Mensaje claro cuando la misma cuenta es tutor y supervisado del mismo dispositivo (heredado del
    Sprint 28, sigue sin resolver).

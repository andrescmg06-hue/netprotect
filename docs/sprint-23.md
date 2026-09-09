# Sprint 23 — Supervisión remota viable

Fecha: 09/09/2026.

## Objetivo

Que el tutor pueda ver **en vivo** la pantalla del dispositivo supervisado, sin que eso se
convierta en vigilancia encubierta: la persona supervisada tiene que aceptar cada vez, ve que está
ocurriendo mientras ocurre, y puede cortarlo.

El Paso 22 de `docs/planning/plan-desarrollo.md` pide exactamente eso: evaluar formalmente
`MediaProjection` y WebRTC como transporte, implementar **sólo lo que las APIs y las políticas
permiten**, con consentimiento visible y auditado, y decidir cámara/micrófono/notificaciones con el
mismo criterio (pudiendo quedar como V2).

La palabra que da nombre al sprint es "viable", y este sprint la toma en serio: parte del entregable
es decir con precisión **dónde deja de funcionar** esto y por qué (ver "Límites" abajo).

## Historias de usuario

1. Como tutor, quiero pedir ver la pantalla del dispositivo de mi hijo para entender qué está
   haciendo cuando algo no me cuadra.
2. Como persona supervisada, quiero decidir cada vez si acepto que vean mi pantalla, y poder cortar
   la transmisión en cualquier momento sin pedir permiso a nadie.
3. Como tutor, quiero que quede registrado quién pidió ver la pantalla, si se aceptó y cuándo
   terminó, para que la supervisión sea revisable y no una caja negra.

## Criterios de aceptación

1. El tutor puede solicitar la vista remota desde el panel web y ve el estado de su solicitud
   (esperando, aceptada, rechazada, transmitiendo, terminada).
2. El dispositivo supervisado muestra la solicitud y sólo transmite tras dos aceptaciones: la del
   propio usuario en la app y la del diálogo del sistema operativo.
3. Mientras transmite, hay una notificación permanente con un botón para detenerla.
4. Rechazar es una acción de primera clase: el tutor recibe el rechazo explícitamente, no un
   silencio.
5. La solicitud, el consentimiento (otorgado o negado), el inicio real y el fin quedan en
   `audit_logs` con su actor correcto.
6. Nadie puede suplantar el rol del otro extremo en la señalización.
7. El video no se graba ni se persiste en ninguna parte.

## Decisiones de diseño

### El dispositivo ofrece, el navegador responde

El extremo Android es el *offerer* (captura y envía) y el navegador del tutor es el *answerer*
(recibe y muestra). Esa asimetría no es casual: un navegador ya trae WebRTC nativo, así que
poniendo el visor en el panel web **la dependencia de WebRTC queda sólo en Android**, sin librería
nueva en el frontend ni un segundo camino de código que mantener.

Consecuencia aceptada: la app **Android del tutor** no tiene visor en este sprint. Es sólo panel
web, mismo precedente que el Sprint 22 (auditoría) para funcionalidad nueva compleja.

### Señalización sobre el WebSocket que ya existía, sin tabla nueva

El canal por dispositivo del Sprint 18 (`WS /devices/{id}/ws`) ya autentica a los dos extremos
correctos: el dispositivo supervisado y sus tutores activos. Añadirle relevo de mensajes WebRTC es
mucho menos superficie que inventar un canal aparte. El backend **no interpreta** SDP ni ICE: es un
relevo, no un par.

Hasta este sprint el canal era sólo servidor→cliente y todo lo entrante se descartaba como *ping*.
Ahora que se actúa sobre lo entrante, esos frames son **entrada no confiable sobre un socket
autenticado** y reciben el mismo trato que cualquier cuerpo de petición: conjunto cerrado de tipos
(`app/schemas/realtime.py`), tope de longitud en cada string, y **dirección permitida por rol**
(`_SIGNAL_SENDER_ROLES`). El rol es el que se fijó al autenticar, nunca uno que venga dentro del
mensaje: sin eso, un supervisado podría fabricar el "consentimiento otorgado" que él mismo no dio,
o un tutor podría responder por el dispositivo.

`relay_to_peer()` envía sólo al rol **contrario**, no un broadcast: una negociación WebRTC es una
conversación entre dos, y devolverle su propia oferta al emisor (o mandarla a una segunda pestaña
del tutor) produciría una segunda respuesta para la misma oferta.

**Sin estado de sesión en el backend, y sin tabla nueva.** Una sesión de video es intrínsecamente
efímera; persistirla no aportaría nada que la auditoría no registre ya. Misma línea que el resto
del proyecto desde el Sprint 15: no se crea esquema para algo que no lo necesita.

### Confirmación de conexión (`connected`)

Se añadió un frame de confirmación tras autenticar. No es cosmético: antes, un cliente sólo podía
deducir que su token fue aceptado por la **ausencia** de un cierre, y eso no es algo a lo que un
par pueda esperar. Sin esa barrera, una solicitud relevada antes de que el socket del otro extremo
terminara de registrarse se perdía en silencio, sin nadie que la contestara. Se descubrió como un
cuelgue real en la suite de pruebas, no en teoría — ver `docs/sprint-23-evidence.md`.

### Qué se audita y qué no

Se auditan cuatro momentos: la solicitud del tutor, la respuesta del supervisado (otorgada o
negada), el inicio real de la transmisión y su fin. No se auditan las respuestas SDP ni los
candidatos ICE: no contienen ninguna decisión, hay decenas por sesión, y enterrarían los cuatro
eventos que sí importan.

Detalle honesto: "inicio real" se registra al recibir la **oferta** del dispositivo, porque es lo
primero que éste envía *después* de que el diálogo del sistema fue aceptado. El backend no puede
observar ese diálogo — es una pantalla del sistema operativo —, así que la oferta es la
aproximación más fiel de la que dispone, y se documenta como tal en vez de fingir certeza.

Esto rompe deliberadamente la norma del proyecto de **no** auditar eventos originados por el
dispositivo (heartbeat, tamper-events): aquí el enunciado pide consentimiento auditado, y el
consentimiento es, por definición, una acción de la persona supervisada. Como la auditoría está
scopeada por actor (Sprint 22), el consentimiento aparece en el registro **de quien lo dio**, no en
el del tutor.

### Configuración de ICE servida por el backend

`GET /devices/{id}/webrtc-config` (nueva dependencia `require_device_participant`: sirve a los dos
extremos, porque los dos son pares de la misma conexión). Así, añadir un TURN el día que exista es
un cambio de configuración y no un APK nuevo más un despliegue web.

### Cámara y micrófono: V2 explícito

No se implementan. Son permisos nuevos (`CAMERA`, `RECORD_AUDIO`) y, a diferencia de la pantalla
—que el sistema anuncia y el usuario puede cortar—, no aportan nada al caso de uso que la pantalla
no cubra. Decisión, no olvido.

## Límites (declarados, no descubiertos después)

- **Sin TURN, esto no funciona en la mayoría de redes móviles reales.** Sólo hay STUN público
  configurado. STUN sirve para descubrir la dirección pública; atravesar un NAT de operadora
  necesita un servidor que releve el tráfico, y este proyecto no tiene ninguno. El panel del tutor
  muestra ese caso con su propio mensaje en vez de un error genérico. Levantar coturn queda para el
  Paso 25.
- **El consentimiento no se puede reutilizar**, por diseño de Android: cada sesión exige pasar otra
  vez por el diálogo del sistema. Ver `docs/android/capability-matrix.md` (Sprint 23).
- **Android corta la transmisión al bloquearse la pantalla** (Android 15 QPR1+) y ofrece siempre un
  chip del sistema para detenerla. No se intenta evitar ninguna de las dos cosas.
- **Si la app supervisada no está abierta, la solicitud no se puede contestar.** Se intenta
  despertarla por FCM, que en este repo sigue sin proyecto Firebase real (Sprint 18). No hay cola
  de solicitudes pendientes a propósito: una solicitud entregada diez minutos tarde ya no es la
  misma solicitud.
- **Una sesión por vez.** No se contempla que dos tutores miren a la vez.
- **El backend no impone un tiempo máximo de sesión**: al no haber estado de sesión, el corte lo
  detecta el propio estado ICE de la conexión (`onIceConnectionChange`) o un `screen_share_stop`.

## Cambios

- Backend: relevo tipado y auditado en `app/api/v1/endpoints/realtime.py`, `relay_to_peer()` en
  `app/services/realtime.py`, schemas de señalización en `app/schemas/realtime.py`,
  `require_device_participant` en `app/api/deps.py`, `webrtc_stun_urls` en `app/core/config.py`,
  y `send_rule_change_wake` generalizado a `send_fcm_wake(device_id, token, event)` en
  `app/services/push.py`.
- Android: `core/screenshare/ScreenShareService.kt` y `core/screenshare/ScreenCapture.kt` (nuevos),
  `core/network/WebRtcConfigClient.kt` (nuevo), `RealtimeClient` generalizado (recibe cualquier
  evento y ahora también envía), tarjeta de consentimiento en `SupervisedScreen`, permiso y
  `<service>` de tipo `mediaProjection` en el manifiesto, dependencia
  `io.getstream:stream-webrtc-android`.
- Web: `components/RemoteViewPanel.tsx` (nuevo), montado en `DevicesPanel`, y
  `fetchWebRtcConfig` en `lib/apiClient.ts`.
- Sin migración: ninguna tabla nueva ni columna nueva.

## Pendiente de una persona

El diálogo de captura de Android sólo lo puede confirmar un ser humano, igual que el selector de
cuenta de Google en el Sprint 3. La verificación extremo a extremo del video real está documentada
como pendiente, con los pasos exactos, en `docs/sprint-23-evidence.md`.

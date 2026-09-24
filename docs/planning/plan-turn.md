# Plan Scrum — Servidor TURN para la vista remota (Sprints 28–30)

## Por qué existe este plan

La vista remota del Sprint 23 funciona de punta a punta en su señalización, verificado en vivo el
24/09/2026 contra el stack de desarrollo real: la auditoría registró `SCREEN_SHARE_REQUESTED`,
`SCREEN_SHARE_CONSENT_GRANTED` y `SCREEN_SHARE_STARTED`, y Android arrancó la captura (chip rojo del
sistema visible). Lo que falla es el **transporte del video**: a los ~15 s el estado ICE pasa a
`failed` y los dos lados cortan, como está diseñado.

La causa ya estaba declarada como límite desde el Sprint 23 (`docs/sprint-23.md`,
`docs/manuals/analisis-riesgos.md`): el proyecto sólo tiene STUN público (`WEBRTC_STUN_URLS`), sin
TURN. STUN sólo sirve cuando existe un camino directo entre los dos pares, y en este caso no lo hay:

- el emulador vive en su propia red virtual (`10.0.2.15`), inalcanzable desde el navegador del host;
- Chrome oculta sus candidatos locales detrás de nombres mDNS `.local` que el emulador no resuelve;
- en redes móviles reales (CGNAT de operadora) pasa lo mismo por otras razones.

Un servidor TURN retransmite el tráfico cuando no hay camino directo. Los dos pares se conectan
*hacia* él, así que funciona en cualquier red donde se pueda alcanzar al servidor. El video sigue
cifrado de extremo a extremo (DTLS-SRTP): el TURN reenvía paquetes, pero no puede descifrarlos.

## Producto y alcance

**Objetivo del producto:** que el tutor vea la pantalla del dispositivo supervisado aunque los dos
estén en redes sin camino directo entre ellas, sin debilitar ninguna garantía de consentimiento ni
de seguridad que el Sprint 23 ya estableció.

**Dentro del alcance:** coturn en los tres entornos (dev, test, prod), credenciales TURN efímeras
emitidas por el backend, los dos clientes (web y Android) usándolas, endurecimiento del servidor,
verificación real con retransmisión forzada, documentación y CI.

**Fuera del alcance (backlog, no se toca aquí):**

- Mensaje explícito cuando la misma cuenta es tutor y supervisado del mismo dispositivo. Hoy el
  backend descarta la solicitud en silencio y el panel queda en "Esperando respuesta…".
  Descubierto el 24/09/2026. Candidato a un sprint propio.
- `turns:` (TURN sobre TLS en 5349/443). Ver la decisión D5.
- Cámara y micrófono (siguen siendo V2, igual que en el Sprint 23).

## Preparación (antes del Sprint 28)

Hay tres correcciones de la sesión del 24/09/2026 que siguen sin commit. Deben entrar en su propio
commit, con CI en verde, **antes** de empezar el Sprint 28, para que el sprint parta de una línea
base limpia:

1. `UsageAccessPermission.kt`: `checkOpNoThrow` con `attributionTag` sólo existe desde la API 36.
   En Android 8 a 15 la app se cerraba al abrir el Modo Supervisado.
2. `ProtectedPackages.kt`: `systemDialerPackage` sólo existe desde la API 29. En Android 8 y 9 el
   bloqueo de apps se cerraba.
3. `frontend/next.config.ts`: la CSP del Sprint 21 bloqueaba el script de Google Identity Services,
   así que el botón de inicio de sesión del panel no se dibujaba.

## Definición de terminado (aplica a los tres sprints)

Tomada de `CLAUDE.md`. Un sprint no está cerrado hasta que:

1. Cada criterio de aceptación tiene evidencia real ejecutada en `docs/sprint-NN-evidence.md`,
   con su salida real, incluidos los tropiezos y cómo se corrigieron.
2. La suite completa del backend pasa en `compose.test.yaml`, contra PostgreSQL y Redis reales.
3. `ruff`, ESLint, `tsc` y `./gradlew test assembleDebug` pasan limpios.
4. Se corrió `/security-review` sobre el diff del sprint y sus hallazgos se corrigieron o se
   documentaron.
5. CI en GitHub Actions pasa en todos sus jobs, en un runner limpio.
6. Están actualizados `docs/sprint-NN.md`, `README.md` (`## Alcance del Sprint NN`), los manuales
   de `docs/manuals/` que describan algo que cambió y `CLAUDE.md` (estado actual).
7. Lo que sólo puede verificar una persona queda escrito como pendiente, no se da por bueno.

## Decisiones de diseño (se confirman en el Sprint 28)

**D1 — coturn, imagen oficial `coturn/coturn`, con versión fijada.** Es el servidor TURN de
referencia, mantenido y empaquetado en Docker. Además trae `turnutils_uclient`, un cliente TURN
real que permite probar credenciales y asignaciones sin escribir un cliente propio (ver D7).

**D2 — Credenciales efímeras (mecanismo "TURN REST API", `use-auth-secret`), no usuario fijo.** Un
usuario y contraseña fijos terminarían en cada APK y en cada navegador, y no se podrían revocar.
Con este mecanismo el backend comparte un secreto con coturn y emite, en cada
`GET /devices/{id}/webrtc-config`, una credencial que caduca:

- `username = "<expira_unix>:<id opaco>"`;
- `credential = base64(HMAC-SHA1(TURN_SHARED_SECRET, username))`.

coturn la valida sin llamar al backend. Sólo la recibe quien ya es participante del dispositivo,
porque el endpoint sigue detrás de `require_device_participant`. El id opaco **no** es el `user_id`:
así no queda un identificador de la cuenta en los logs del TURN.

**D3 — Vigencia de la credencial: 1 hora, configurable (`TURN_CREDENTIAL_TTL_SECONDS`).** coturn
revalida la credencial al refrescar la asignación. Si la vigencia fuera más corta que la sesión,
el video se cortaría a mitad de camino. Una hora cubre una sesión larga y sigue limitando cuánto
sirve una credencial filtrada.

**D4 — Secreto propio y con nombre propio: `TURN_SHARED_SECRET`.** Se genera con
`secrets.token_urlsafe(48)`, se documenta en los tres `.env.*.example` y en producción usa el patrón
`_FILE` del Sprint 26. Nunca se reutiliza `JWT_SECRET` (convención de `CLAUDE.md`).

**D5 — `turn:` en 3478 por UDP y TCP. `turns:` (TLS) queda diferido, con su motivo.** El contenido
ya viaja cifrado con DTLS-SRTP, así que TLS sobre TURN sólo aporta atravesar firewalls que
únicamente dejan pasar el 443. Implementarlo exige compartir el certificado de Caddy con coturn y
un dominio real, lo mismo que dejó pendiente el Sprint 26. Se documenta como pendiente, no como
hecho.

**D6 — Contrato compatible hacia atrás.** `ice_servers: list[str]` se queda tal cual, porque la
APK ya instalada lo parsea como una lista de textos. Se agrega un campo nuevo:

```text
turn_servers: [{ urls: [...], username, credential }]
```

Una APK vieja lo ignora y sigue como hoy, sólo con STUN. Los clientes nuevos usan los dos campos.

**D7 — Endurecimiento de coturn: parte del sprint, no un extra.**

- `no-cli`, `fingerprint`, `lt-cred-mech`, `realm` propio y `stale-nonce`.
- Cuotas por usuario y totales, y rango de puertos de relay acotado.
- `no-multicast-peers` y `denied-peer-ip` para las redes privadas (10/8, 172.16/12, 192.168/16,
  127/8, 169.254/16). Sin esto, el TURN serviría para llegar a la red interna del servidor, por
  ejemplo a PostgreSQL o Redis.

**Riesgo abierto (spike R1):** cuando los dos pares usan el mismo TURN, el relay de uno le envía al
relay del otro, que es una dirección de la propia red de Docker. `denied-peer-ip` podría bloquear
justo ese tráfico. Se resuelve verificándolo en vivo en el Sprint 28 (por ejemplo, con
`allowed-peer-ip` sólo para la IP de relay del propio coturn), no suponiéndolo.

**D8 — Varias URL de TURN en desarrollo.** El navegador del host alcanza a coturn en `localhost` y
el emulador en `10.0.2.2`. El backend no sabe qué cliente pide la configuración, así que envía las
dos y cada cliente usa la que le responde. En producción es una sola URL pública.

## Product backlog del plan

| ID | Historia | Puntos | Sprint |
|---|---|---:|---|
| HU-090 | Como responsable de infraestructura, quiero un servidor TURN corriendo en dev, test y prod, con configuración versionada y endurecida, para que la retransmisión exista en los tres entornos. | 5 | 28 |
| HU-091 | Como responsable de seguridad, quiero que el backend emita credenciales TURN efímeras sólo a los participantes del dispositivo, para que ninguna credencial fija viaje en la APK ni en el navegador. | 5 | 28 |
| HU-092 | Como tutor en el panel web, quiero que el visor use el TURN cuando no hay camino directo, para ver la pantalla aunque estemos en redes distintas. | 3 | 29 |
| HU-093 | Como persona supervisada, quiero que mi dispositivo pueda transmitir a través del TURN, sin perder el consentimiento en dos pasos ni el aviso permanente. | 3 | 29 |
| HU-094 | Como responsable de calidad, quiero una prueba real que demuestre que el video pasa por el TURN (retransmisión forzada), no sólo que el servidor arranca. | 5 | 30 |
| HU-095 | Como quien despliega NetProtect, quiero el TURN integrado en `compose.prod.yaml` y en los manuales, con lo que exige un servidor real marcado como pendiente. | 3 | 30 |

Total: 24 puntos en 3 sprints (8, 6 y 8 puntos).

## Sprint 28 — Servidor TURN y credenciales efímeras

**Objetivo del sprint:** que coturn corra en los tres entornos y que el backend emita credenciales
efímeras que coturn acepte de verdad.

**Historias:** HU-090 y HU-091.

**Tareas:**

1. Spike R1: levantar coturn en `compose.yaml` y confirmar en vivo cómo se comportan
   `denied-peer-ip`/`allowed-peer-ip` con dos asignaciones en la misma instancia.
2. Servicio `coturn` en `compose.yaml`, `compose.test.yaml` y `compose.prod.yaml`, con
   `infra/coturn/turnserver.conf` versionado y endurecido (D7).
3. `TURN_SHARED_SECRET`, `TURN_URLS`, `TURN_REALM` y `TURN_CREDENTIAL_TTL_SECONDS` en
   `app/core/config.py` y en los tres `.env.*.example`. En producción, el secreto usa el patrón
   `_FILE`.
4. Función propia y pura que genera la credencial (D2), probada por separado.
5. Campo `turn_servers` en `WebRtcConfigResponse` y en el endpoint (D6). Si `TURN_URLS` está vacío,
   el campo va vacío: mismo patrón que `FCM_PROJECT_ID` vacío.
6. Pruebas:
   - de la función de credencial;
   - del endpoint: sólo participantes; un tercero recibe 404, como en el barrido del Sprint 25;
   - de integración real: el backend emite la credencial y `turnutils_uclient`, dentro de
     `compose.test.yaml`, hace un Allocate contra coturn con ella. Una credencial caducada o
     alterada es rechazada.

**Criterios de aceptación:**

1. coturn arranca en los tres entornos y su configuración está versionada, sin secretos en el repo.
2. Una credencial emitida por el backend logra un Allocate real en coturn, y una caducada o
   alterada no lo logra. La evidencia es la salida real de `turnutils_uclient`.
3. Un usuario que no participa en el dispositivo no obtiene credenciales (404).
4. Con el TURN apuntando a una IP privada, el relay rechaza al peer (D7). Verificado, no supuesto.
5. La suite completa del backend y CI pasan en verde.

## Sprint 29 — Clientes web y Android usando el TURN

**Objetivo del sprint:** que el panel y la app construyan su `RTCPeerConnection` con el TURN, sin
romper a los clientes que no lo conocen.

**Historias:** HU-092 y HU-093.

**Tareas:**

1. Web: `WebRtcConfig` en `lib/apiClient.ts` y `RemoteViewPanel.tsx` construyen `iceServers` con
   STUN y además TURN con `username`/`credential`.
2. Android: `WebRtcConfigClient.kt` parsea `turn_servers`, y `ScreenShareService.kt` usa
   `PeerConnection.IceServer.builder(urls).setUsername().setPassword()`.
3. Compatibilidad: se verifica que la APK anterior siga conectando por STUN contra el backend nuevo
   (D6).
4. Se verifica que el consentimiento, la notificación permanente y la auditoría de los cuatro
   momentos no cambiaron.

**Criterios de aceptación:**

1. Con el emulador y el navegador del host, la situación que hoy falla, el tutor **ve la pantalla**.
   Evidencia: captura del panel y par de candidatos `relay` en `chrome://webrtc-internals`.
2. La APK anterior sigue funcionando igual que antes contra el backend nuevo.
3. ESLint, `tsc`, la compilación de Android, la E2E de Playwright y CI pasan en verde.

## Sprint 30 — Verificación forzada, producción y documentación

**Objetivo del sprint:** demostrar, con retransmisión forzada, que el video pasa por el TURN, y
dejar el despliegue y los manuales al día.

**Historias:** HU-094 y HU-095.

**Tareas:**

1. Prueba con retransmisión forzada (`iceTransportPolicy: "relay"`), sólo en la prueba y nunca en
   el código de producción. Si llega video, pasó por el TURN por definición.
2. Prueba con un celular Android real en otra red (datos móviles). **Requiere a una persona**; si no
   se puede hacer, queda como pendiente humano, como el resto de pruebas con dispositivo real.
3. `compose.prod.yaml`: coturn con puertos publicados (3478 UDP/TCP y el rango de relay),
   `external-ip` y los requisitos de firewall documentados. Lo que exige un servidor y un dominio
   reales queda como pendiente (igual que en el Sprint 26).
4. Documentación: `docs/sprint-28/29/30.md` y sus `-evidence.md`, `README.md`,
   `docs/manuals/manual-despliegue.md`, `analisis-riesgos.md` (se retira "sin TURN" como riesgo
   abierto), `modelo-seguridad.md`, `docs/android/capability-matrix.md` y `CLAUDE.md`.
5. `/security-review` final sobre los tres sprints.

**Criterios de aceptación:**

1. Con la retransmisión forzada, el video llega. Evidencia: captura y estadísticas de WebRTC.
2. El despliegue de producción está documentado, y cada comando citado se ejecutó al menos
   localmente.
3. Todos los documentos están actualizados y CI está en verde en todos los jobs.

## Riesgos del plan

| ID | Riesgo | Mitigación |
|---|---|---|
| R1 | `denied-peer-ip` bloquea el relay entre dos asignaciones del mismo coturn. | Spike al inicio del Sprint 28, verificado en vivo. |
| R2 | Docker Desktop en Windows maneja mal UDP en un rango grande de puertos publicados. | Rango de relay acotado y `turn:` también por TCP como respaldo. |
| R3 | El TURN consume ancho de banda de salida en producción. | Cuotas de coturn (D7), métricas y el límite documentado en el manual de administrador. |
| R4 | Un servidor y un dominio reales siguen pendientes (Sprint 26). | Se verifica todo lo posible en local y lo demás queda escrito como pendiente humano. |

## Ceremonias (adaptadas a este proyecto)

- **Planificación:** al empezar cada sprint se confirma su objetivo, las historias y las decisiones
  de diseño con el dueño del proyecto antes de escribir código.
- **Seguimiento:** se avanza tarea por tarea y cada una se verifica antes de pasar a la siguiente.
- **Revisión:** demostración real del incremento (en vivo en el panel y el emulador) y evidencia
  escrita en `sprint-NN-evidence.md`.
- **Retrospectiva:** los tropiezos reales se documentan en la evidencia y, si aplican a futuro, en
  "Errores que ya se cometieron una vez" de `CLAUDE.md`.

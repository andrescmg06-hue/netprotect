---
paths:
  - "mobile/**"
---

# Android (Kotlin/Compose)

- **Sin Hilt/DI ni ViewModel todavía** — decisión deliberada, el proyecto es pequeño y se ha
  mantenido así a propósito; no introducir esas dependencias sin que el tamaño del proyecto lo
  justifique.
- Antes de asumir que una función de control parental es técnicamente viable, consultar
  `docs/android/capability-matrix.md` — tiene la verificación contra fuente oficial de cada
  decisión de plataforma ya tomada (foreground services, ubicación, geofencing, overlays, etc.).
- Un `.kt` nuevo no está verificado hasta que `./gradlew compileDebugKotlin` corre sobre él.
- `./gradlew lintDebug` antes de cerrar cualquier cambio — es el único que detecta APIs por encima
  de `minSdk 26`, y no corre en CI.

---

**Nota importante descubierta en el Sprint 7, válida para cualquier sprint futuro que toque
permisos Android sensibles**: las políticas de Google Play (formulario de declaración de permisos,
divulgación destacada, política anti-stalkerware para apps de control parental) sólo se activan si
la app se **publica** en la tienda. Este proyecto se instala por sideload (Android Studio/`adb`)
para el trabajo de la universidad, así que esas políticas quedan documentadas pero no aplican hoy.
Sí sigue aplicando siempre, publicado o no: el permiso mismo debe existir, declararse correctamente
y (si es especial) concederse por el mecanismo real de Android — sideload no exime de eso. Ver el
detalle verificado en `docs/android/capability-matrix.md`.

**Nota del Sprint 8, válida para cualquier sprint futuro que toque foreground services**: un
foreground service que sondea en segundo plano exige notificación persistente por requisito del
propio Android desde la API 26 — no es sólo la política anti-stalkerware de Play (que sigue
aplicando sólo si se publica). Con `targetSdk` 34+ además hace falta declarar un
`foregroundServiceType`; si el caso de uso no encaja en ninguno predefinido, `specialUse` es el
único que sirve, con su propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justificando el uso (revisada
por Google sólo al publicar). Verificado también en ejecución real: Android 12+ rechaza arrancar
un foreground service si la app no tiene antes una actividad visible — hay que arrancarlo siempre
desde un efecto de una pantalla en primer plano, nunca desde un contexto de fondo.

**Nota del Sprint 9, válida para cualquier sprint que amplíe el bloqueo**: existe una lista de apps
que nunca se bloquean (`ProtectedPackages`: launcher, teléfono, Ajustes y la propia app), resuelta en
tiempo de ejecución porque los nombre s de paquete varían entre fabricantes. No es una preferencia del
tutor, es una barrera de seguridad — bloquear el teléfono podría estorbar una llamada de emergencia y
bloquear Ajustes dejaría al usuario sin forma de revocar el permiso. Cualquier mecanismo de bloqueo
nuevo debe respetarla.

**Nota del Sprint 12**: "modo escolar" se implementó como una vigencia horaria sobre la política por
defecto (`devices.school_mode_*`), no como un sistema de perfiles de reglas paralelo — reutiliza
`isWithinSchedule()` tal cual. Una `AppRule`/`CategoryRule` con `ALLOW` sigue aprobando esa app en
horario escolar, exactamente igual que ya aprueba contra la política por defecto simple (Sprint 9).
Decisión documentada como interpretación propia en `docs/sprint-12.md`, no como la única lectura
posible del enunciado.

**Nota del Sprint 13**: un foreground service con `foregroundServiceType="location"` cuenta como
"en primer plano" para el sistema de permisos de ubicación de Android mientras corre (verificado en
fuente oficial, ver `docs/android/capability-matrix.md`) — este proyecto explota eso a propósito
para reportar ubicación en segundo plano **sin pedir nunca `ACCESS_BACKGROUND_LOCATION`**, con el
mismo patrón de arranque-sólo-desde-Activity-en-primer-plano ya usado por `RuleEnforcementService`
desde el Sprint 8. Consecuencia aceptada: si el proceso muere (swipe en Recientes, sistema bajo
presión de memoria), el reporte se detiene hasta reabrir la app — no hay reinicio automático.
Decisión de diseño separada, también documentada: Android **no** integra el SDK nativo de Google
Maps — la pantalla del tutor delega a la app de mapas ya instalada vía un intent `geo:`, así que
`GOOGLE_MAPS_ANDROID_API_KEY` no existe como variable de este proyecto. El panel web sí necesita su
propia clave (`NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`, Maps Embed API) para el mapa embebido — pendiente
de que un humano con cuenta de Google Cloud la genere y la restrinja por referer HTTP (mismo caso
que `GOOGLE_WEB_CLIENT_ID`, ver `docs/sprint-13.md`); sin ella, el panel muestra coordenadas en
texto y un enlace a Google Maps.

**Nota del Sprint 14, válida para cualquier sprint futuro que toque la Geofencing API de Android o
considere añadir `play-services-location`**: se verificó la Geofencing API real de Android
(`GeofencingClient`, Google Play Services) contra la documentación oficial antes de escribir código
— exige `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (para que los eventos ENTER/EXIT
lleguen con la app en segundo plano — el truco del Sprint 13 de que un foreground service
`location`-typed cuenta como "en primer plano" **no aplica** a los callbacks de geofencing, que
llegan desde un proceso de Play Services, no desde nuestro propio servicio) y la dependencia
`play-services-location`, que este proyecto evita a propósito desde el Sprint 13. Se decidió, con
el dueño del proyecto, **no** adoptarla: en su lugar, el backend detecta ENTER/EXIT comparando
cada nuevo reporte de ubicación (ya enviado por `LocationReportingService` cada ~15 minutos) contra
el anterior del mismo dispositivo, para cada geocerca (`backend/app/services/geofencing.py`, sin
scheduler, mismo patrón "evaluar al escribir" que la purga de retención del Sprint 13). Ningún
permiso, dependencia ni servicio nuevo se añadió a Android. Costo aceptado: latencia de detección
de ~15 minutos en vez de los 2-6 minutos que ofrecería la API real. Ver
`docs/android/capability-matrix.md` (sección Sprint 14) para la verificación completa.

**Nota del Sprint 19, válida para cualquier sprint futuro que toque Room o WorkManager en
Android**: Room se integra vía KSP (`com.google.devtools.ksp`, versión `2.3.11`, independiente de
la del compilador desde que KSP dejó el esquema `<kotlin>-<ksp>`), **no** vía `kapt` — se intentó
primero y falló: el backend `javac` de `room-compiler:2.7.1` empaqueta su propio
`kotlin-metadata-jvm` fijo, que sólo entiende metadatos hasta el formato 2.2, y el compilador
Kotlin 2.3.21 de este proyecto emite formato 2.3. Ver `docs/sprint-19.md`. La estrategia de
conflicto elegida es la más simple posible: el dispositivo supervisado nunca edita una regla
localmente, así que cada fetch exitoso de `/rules/active` reemplaza el caché de Room por completo
(`RulesCacheStore.replaceAll`), sin fusión ni número de versión — no hay nada real con lo que
fusionar. La cola de eventos pendientes (`pending_rule_events`) se limitó a los bloqueos de reglas
(`AppRuleEvent`): es la única señal del dispositivo cuya pérdida no queda reemplazada después por
la siguiente lectura (a diferencia de heartbeat/ubicación/uso). `SyncWorker` complementa los
sondeos en primer plano de `SupervisedScreen` (no los reemplaza — el piso de WorkManager, 15
minutos, es más lento que ellos) y es, además, la única vía que sigue entregando *heartbeat*/uso/
cola pendiente si el proceso de `RuleEnforcementService` muere; el bloqueo de apps en sí sigue sin
reiniciarse solo (límite aceptado desde el Sprint 8, sin cambios). Se agregó
`BackgroundTokenRefresher` porque ambos componentes corren mucho más de los 15 minutos de vida de
un *access token*: sin renovarlo habrían empezado a fallar en silencio con 401 en cualquier sesión
medianamente larga, no sólo durante un corte de red real — reutiliza el mismo `refresh_token`
cifrado del Keystore que ya usa `AuthRepository` (Sprint 3), sin tocar el modelo de sesión de la
UI en primer plano (que tiene el mismo problema de fondo, sin resolver, fuera del alcance de este
sprint — anotado en `docs/sprint-19.md`).

**Nota del Sprint 20, válida para cualquier sprint futuro que toque detección de manipulación,
Device Administrator o el estado `ALERT`**: sí, `DeviceStatus.ALERT` y los niveles
`HIGH`/`CRITICAL` eran el lugar natural — no se creó ningún sistema de notificación paralelo ni
tabla de eventos propia (la `Alert` generada ya guarda `first_occurred_at`/`occurrence_count`/
`read_at`, suficiente historial dentro de `alert_retention_days`). Cuatro de las cinco señales son
**condiciones** que viajan como campos opcionales del *heartbeat* (`usage_access_granted`,
`service_active`, `device_time`): pérdida de permiso, servicio detenido, desfase de reloj y
silencio anómalo — este último medido contra el `last_seen_at` anterior en el momento en que el
dispositivo vuelve a reportarse, mismo patrón "comparar contra el reporte previo, sin scheduler"
del Sprint 14; consecuencia aceptada: un dispositivo que se calla **para siempre** no genera esa
alerta (el tutor sigue viendo `OFFLINE`, como desde el Sprint 6). La quinta es un **evento**
discreto con endpoint propio (`POST /devices/{id}/tamper-events`, `CRITICAL`): el intento de
desinstalación. `null` en cualquiera de los tres campos significa "sin información", nunca
"manipulado" — importa para una APK vieja y para `SyncWorker` antes de que
`RuleEnforcementService` haya sellado nunca su marca de vida. `ALERT` se **recalcula en cada
latido** (un latido sano devuelve a `ONLINE`), así que no hay ni hace falta un endpoint para que
el tutor lo limpie; lo que el docstring de `compute_effective_status` prohíbe —y sigue
prohibido— es que la *antigüedad* de un latido degrade `ALERT` a `OFFLINE`, no que un latido
nuevo con datos frescos lo reevalúe. Dos decisiones más, documentadas en `docs/sprint-20.md`:
**la "revocación de la VPN" del enunciado no se implementó** porque este proyecto no tiene
componente VPN (`VpnService` sigue pospuesto desde el Sprint 9) y no se iba a construir la
vulnerabilidad para poder venderle la alarma; y el intento de desinstalación se detecta
registrando la app como **Device Administrator** (no device owner, no MDM: un permiso que el
usuario concede desde una pantalla del sistema y puede retirar), con `<uses-policies>` **vacío**
en `res/xml/device_admin.xml` porque el registro existe sólo por el *callback*
`onDisableRequested()` — que no puede vetar nada, sólo avisar y reportar (vía WorkManager, nunca
red en el hilo principal del receiver). `EnforcementLiveness` es una marca cooperativa en
`SharedPreferences` porque desde Android 5.0 no hay forma soportada de preguntarle al sistema si
un servicio propio sigue vivo desde otro proceso; se sella también en
`RuleEnforcementService.start()` (síncrono) para que el primer latido de cada sesión no reporte un
`SERVICE_INACTIVE` falso, y se borra en `stop()` porque una parada deliberada no es manipulación.

**Nota del Sprint 23, válida para cualquier sprint futuro que toque captura de pantalla, WebRTC o
el canal en tiempo real**: el consentimiento de `MediaProjection` es **por sesión y no
reutilizable** — verificado en fuente oficial: un `MediaProjection` sirve para una sola
`createVirtualDisplay()`, y reutilizar el `Intent` de `createScreenCaptureIntent()` lanza
`SecurityException` desde Android 14; además, sin registrar un `MediaProjection.Callback` la
captura ni siquiera arranca (`IllegalStateException`). No existe forma soportada de guardar un
permiso de captura para usarlo luego en silencio, así que la tarjeta de consentimiento reaparece
en cada solicitud por obligación de la plataforma, no sólo por decisión de producto. Android 15
QPR1+ corta la proyección al bloquearse la pantalla y da al usuario un chip del sistema para
detenerla; no se intenta evitar ninguna de las dos cosas. El visor va **sólo en el panel web**
(mismo precedente del Sprint 22): el dispositivo es el *offerer* y el navegador el *answerer*, lo
que deja la dependencia `io.getstream:stream-webrtc-android` únicamente en Android — Google ya no
publica un AAR propio de WebRTC, y aquí sí se aceptó una dependencia pesada porque, a diferencia
de `play-services-location` (Sprints 13/14), la plataforma **no ofrece alternativa**. La
señalización reutiliza el WebSocket del Sprint 18: hasta ahora era sólo servidor→cliente y todo lo
entrante se descartaba, así que lo entrante pasó a ser entrada no confiable — conjunto cerrado de
tipos, tope de longitud por campo y **dirección permitida por rol** (`_SIGNAL_SENDER_ROLES`),
tomando el rol del que se fijó al autenticar y nunca del cuerpo del mensaje. `relay_to_peer()`
envía sólo al rol contrario, no hace broadcast. Se añadió un frame `{"event": "connected"}` tras
autenticar: sin él, una solicitud relevada antes de que el otro socket terminara de registrarse se
perdía en silencio (se manifestó como un cuelgue real de la suite, ver
`docs/sprint-23-evidence.md`). Sin tabla nueva y sin estado de sesión en el backend: el corte lo
detecta el propio estado ICE o un `screen_share_stop`. Se auditan cuatro momentos (solicitud,
consentimiento otorgado/negado, inicio real, fin) — **excepción deliberada** a la norma de no
auditar eventos originados por el dispositivo, porque el consentimiento es por definición una
acción del supervisado; el "inicio real" se registra al llegar la oferta, la aproximación más fiel
que el backend tiene, porque no puede observar el diálogo del sistema. Límite declarado: sólo STUN
público (`WEBRTC_STUN_URLS`), sin TURN — detrás del NAT de una operadora normalmente no conectará;
coturn queda para el Paso 25. Cámara y micrófono siguen siendo V2. Dos cosas más que
`/security-review` encontró y que ya están corregidas y cubiertas por pruebas: una sesión está
**anclada a la conexión del tutor que la pidió** (un dispositivo puede tener varios tutores activos
con el canal abierto a la vez, y difundir la oferta al *rol* entero dejaba que otro respondiera
primero y se llevara el video, con la auditoría nombrando a quien lo pidió); y el frame que abre
una sesión **revalida el permiso contra la base de datos**, porque un WebSocket sobrevive al token
que lo abrió y nada lo cierra al desvincular a un tutor. Ver `docs/sprint-23.md`.

**Nota del Sprint 29, válida para cualquier trabajo futuro sobre `RealtimeClient`, el canal de
señalización de vista remota, o el ciclo de vida del token de acceso en Android**: el panel web y
Android ya usan `turn_servers`, verificado en vivo (par de candidatos `relay` en ambos extremos,
`docs/sprint-29-evidence.md`) — pero llegar ahí exigió corregir dos fallos reales sin relación con
coturn en sí. Primero, `RealtimeClient.connect()` mandaba el frame `{"token": ...}` desde el
callback `onOpen` de OkHttp; `ScreenShareService` abre su propio socket y manda su oferta muy poco
después de llamar a `connect()`, y OkHttp encola los envíos en el orden en que se llama a `send()`
sin esperar a que el *handshake* termine — así que la oferta podía encolarse antes que el token, el
backend cerraba la conexión por no reconocer un token como primer frame, y la oferta se perdía sin
ningún rastro. El token ahora se encola como parte de `connect()`, antes de que la función retorne,
en vez de depender del callback. Segundo, `ScreenShareService` usaba el `accessToken` que
`SupervisedScreen` (Compose) tenía capturado en su propio estado — el mismo token de sesión en
primer plano que la nota del Sprint 19 ya señalaba como el único sin refresco automático; con la
app abierta más de los 15 minutos que vive un token, `GET /webrtc-config` daba 401 y el
`runCatching` ya existente lo convertía en silencio en "sin servidores ICE", reproduciendo el
fallo que el Sprint 28 se construyó para resolver. Ahora llama a `BackgroundTokenRefresher.refresh()`
al iniciar cada sesión, igual que `RuleEnforcementService`/`SyncWorker` desde el Sprint 19. El
backend, además, ahora registra (`screen_share_signal_dropped`) cualquier frame de señalización
que descarte por tipo inválido o rol equivocado — nunca el contenido de una oferta SDP ni de un
candidato ICE, verificado con `/security-review` — porque antes de este sprint ese descarte era
silencioso y fue lo que más retrasó encontrar el primer fallo. Ver `docs/sprint-29.md`.

- Un archivo Kotlin nuevo no está verificado hasta que `./gradlew compileDebugKotlin` (o más)
  corre sobre él al menos una vez. Un import que falta (p. ej. `Modifier.width` sin
  `androidx.compose.foundation.layout.width`) no lo marca ningún editor por sí solo; sólo el
  compilador real. No declarar un archivo Android terminado sin haberlo compilado.
- Un `Service` en primer plano ya **no** puede abrir una `Activity` con `startActivity()` directo
  en Android moderno: el sistema lo rechaza como *Background Activity Launch*
  (`ActivityTaskManager`: "Background activity launch blocked!") en cuanto la app no tiene ya una
  actividad visible. Verificado en vivo (18/09/2026, emulador API 36) probando el bloqueo del
  Sprint 8 con la app en segundo plano: la pantalla de bloqueo dejó de aparecer, sin ningún error
  visible para nadie. La notificación de pantalla completa (`setFullScreenIntent`, el mecanismo
  que sí usan llamadas/alarmas) tampoco basta de reemplazo: sólo lanza la Activity automáticamente
  con el dispositivo **bloqueado** — desbloqueado y en uso, que es justo el momento en que hace
  falta bloquear de verdad, se degrada a un simple aviso que hay que tocar. La solución real es
  dibujar la pantalla de bloqueo como ventana superpuesta (`SYSTEM_ALERT_WINDOW`,
  `TYPE_APPLICATION_OVERLAY`), el único mecanismo exento de ambas restricciones — permiso especial
  propio (`OverlayPermission`, con su tarjeta en `SupervisedScreen`, mismo patrón que el acceso de
  uso), y con la salvedad de que `WindowManager`/`LifecycleRegistry` sólo aceptan hilo principal:
  invocarlos desde la corrutina en `Dispatchers.Default` del *polling loop* crasheaba el proceso al
  instante. Ver `BlockOverlayController.kt`, que cae de vuelta a la notificación cuando el permiso
  de superposición no está concedido (sigue funcionando con el dispositivo bloqueado).
- `gradlew lintDebug` no corre en CI y es el único que detecta llamadas a APIs de Android más
  nuevas que `minSdk 26`. En el emulador (API 36) nunca fallan, y en un teléfono real con
  Android 8–15 cierran la app (`NoSuchMethodError`). Así se encontraron dos el 24/09/2026:
  `checkOpNoThrow` con `attributionTag` y `systemDialerPackage`. Correrlo antes de cerrar cualquier
  sprint que toque Android.
- `EGL_emulation: eglQueryContext ... EGL_BAD_ATTRIBUTE` en Logcat, en el punto donde WebRTC arma
  `SurfaceTextureHelper` para capturar pantalla, es ruido propio del emulador — no significa que la
  captura falló. Se investigó como causa real de un video que no llegaba (Sprint 29), incluido
  cambiar `Graphics acceleration` del AVD, sin que el mensaje cambiara ni tuviera relación con el
  fallo real (ver más abajo). Antes de perseguir este error, confirmar primero si el indicador rojo
  de grabación del sistema aparece — si aparece, la captura sí está funcionando.
- Un `WebSocket` de OkHttp (Android) encola cada `send()` en el orden en que se llama, sin esperar
  a que termine el *handshake* — así que mandar el frame de autenticación desde el callback `onOpen`
  en vez de justo al crear el socket deja una ventana real donde un `send()` posterior del mismo
  llamador puede colarse antes. El backend de este proyecto exige que el primer frame de cada canal
  sea el token (`_authenticate()`); si algo se adelanta, cierra la conexión sin dejar rastro visible
  para quien la abrió. Encolar el frame de autenticación de forma síncrona, antes de que la función
  de conexión retorne, es lo que garantiza el orden — no el callback en el que se dispare. Encontrado
  en el Sprint 29 (`RealtimeClient.kt`/`ScreenShareService.kt`), ver `docs/sprint-29-evidence.md`.

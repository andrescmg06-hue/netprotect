# NetProtect — guía para quien continúe este proyecto con Claude Code

Este archivo se carga automáticamente cada vez que Claude Code abre este repositorio. Léelo también
tú si eres humano y estás retomando el proyecto: explica cómo se ha construido hasta ahora y cómo
seguir sin romper las reglas que lo mantienen honesto.

## Qué es esto

NetProtect es una plataforma de control parental: una única app Android (Kotlin/Compose) que opera
como Tutor o Supervisado según lo decide el backend, un panel web (Next.js) para el tutor, y un
backend FastAPI + PostgreSQL + Redis que es la única fuente de verdad para ambos. El documento
original con el alcance completo (48 secciones, 27 sprints) está fuera de este repo; lo que importa
de él ya quedó traducido a artefactos versionados — ver la sección "Dónde está cada cosa" abajo.

## La regla que gobierna todo lo demás

**Nada se marca como terminado sin evidencia real de que se ejecutó.** No mocks disfrazados de
pruebas, no "debería funcionar", no marcar un criterio de aceptación como cumplido por inspección del
código. Si algo no se pudo probar (por ejemplo, un login real de Google exige que una persona elija
su cuenta en un selector — ningún agente puede hacer eso), se dice explícitamente que quedó
pendiente, en vez de darlo por bueno.

Esto se sostiene con un patrón de dos documentos por sprint:

- `docs/sprint-NN.md` — objetivo, historias de usuario, criterios de aceptación, decisiones de
  diseño y **por qué** se tomaron (no sólo qué se hizo).
- `docs/sprint-NN-evidence.md` — comandos ejecutados realmente, con su salida real. Incluye los
  errores encontrados en el camino y cómo se corrigieron, no sólo el resultado final feliz.

Antes de decir que un sprint está cerrado: la suite de pruebas corre en verde dentro de contenedores
Docker reales (no mocks de base de datos), y **CI en GitHub Actions pasa en los 4 jobs**
(`backend`, `frontend`, `android`, `integration`) en un runner limpio — eso es lo que de verdad
certifica que algo funciona independientemente de esta máquina.

## Cómo se construye, sprint por sprint

1. Explicar el objetivo del sprint.
2. Historias de usuario y criterios de aceptación.
3. Decisiones de diseño — especialmente las de seguridad, explicadas con su motivo.
4. Cambios de base de datos (modelos SQLAlchemy + migración Alembic).
5. Backend.
6. Cliente Android y/o web, cuando el sprint los toque.
7. Pruebas de integración contra PostgreSQL/Redis reales en Docker — nunca mockeadas salvo la pieza
   que exige una persona real (p. ej. la verificación de Google se simula con
   `unittest.mock.patch` sobre la función que llama a Google, no sobre la lógica propia).
8. Verificación local, luego en `compose.test.yaml` dentro de contenedor (ver nota de rendimiento
   abajo), luego push y CI.
9. Documentar (`sprint-NN.md` + `sprint-NN-evidence.md`), incluyendo cualquier hallazgo o error real
   encontrado en el camino — no se ocultan los tropiezos, se documentan y se corrigen.

No se avanza al siguiente sprint sin cerrar el anterior con CI en verde, salvo que quede pendiente
explícitamente algo que sólo un humano puede hacer (y quede anotado como tal).

## Dónde está cada cosa

- `docs/planning/plan-desarrollo.md` — el plan completo de 27 pasos, con qué instalar y cuándo.
- `docs/planning/roadmap.md` — la lista de sprints y su incremento.
- `docs/sprint-01.md` … `docs/sprint-07.md` (+ sus `-evidence.md`) — el historial real, sprint por
  sprint. Son la fuente de verdad de qué existe y por qué; no lo repitas de memoria, léelos.
- `docs/security-baseline.md` — controles de seguridad aplicados y la matriz de permisos (quién
  puede hacer qué, y por qué se decidió así).
- `docs/diagrams/06-modelo-datos-fisico-sprint2.md` — el ER real del esquema, con las decisiones de
  diseño de cada tabla.
- `docs/android/capability-matrix.md` — qué es técnicamente viable en Android y qué no, con
  referencias oficiales. Antes de asumir que una función de control parental es posible, mirar aquí.

## Estado actual (08/09/2026)

Sprints 1 a 20 completos y verificados en CI.
Existe: arquitectura y Docker; base de datos con migraciones; login
con Google (backend + web + Android); roles y autorización por recurso (`require_tutor_of_device`,
404 uniforme para "no existe" y "no es tuyo"); vinculación por código de 6 dígitos con HMAC, límite
de intentos y revocación; listado/detalle/renombrado/desvinculación de dispositivos con estado
calculado por heartbeat, en Android (app del tutor y del supervisado) y en el panel web; inventario
de apps instaladas y tiempo de uso diario, reportado por el dispositivo supervisado y visible en la
app del tutor y en el panel web; reglas por app (bloquear/permitir/límite diario/horario) definidas
por el tutor en el panel web y aplicadas localmente por el dispositivo supervisado mediante un
foreground service que sondea `UsageStatsManager` y muestra una pantalla de bloqueo propia,
reportando cada bloqueo aplicado; y una política por dispositivo que invierte el defecto
(`ALLOW` = todo permitido salvo lo bloqueado, `BLOCK` = sólo apps aprobadas), con una lista de apps
que nunca se bloquean; y categorías (11 fijas, decisión propia — ver Sprint 10) con regla por
categoría, evaluada entre la regla por app y la política del dispositivo. Android tiene un router
real (`HomeScreen`: sesión → rol → modo Tutor/Supervisado); la pantalla de diagnóstico del Sprint 1
(`SprintOneScreen`) ya no existe, su chequeo de infraestructura vive ahora en la pantalla de sesión
cerrada; y ubicación aproximada (Sprint 13): el dispositivo supervisado reporta su posición cada
~15 minutos mediante un foreground service (`LocationReportingService`, sólo
`ACCESS_COARSE_LOCATION`, sin permiso de segundo plano), cifrada en la base de datos (Fernet) con
retención de 7 días y purga inline al reportar; el tutor ve la última ubicación conocida en el
panel web (mapa embebido si hay clave de Google Maps configurada, texto si no) y en Android (texto
+ botón que abre un mapa externo vía intent, sin SDK nativo de Maps); y geocercas (Sprint 14): el
tutor crea/edita/elimina zonas circulares (nombre, centro cifrado, radio) desde el panel web, y el
backend detecta entradas/salidas comparando cada nuevo reporte de ubicación contra el anterior del
mismo dispositivo (fórmula de Haversine, sin la Geofencing API de Android/GMS — ver nota del Sprint
14 abajo), con historial consultable desde el panel web y, en modo sólo lectura, desde la app del
tutor en Android; historial unificado (Sprint 15): los dos registros de eventos que ya existían
(bloqueos de reglas, entradas/salidas de geocercas) ganaron una retención uniforme de 90 días con
purga al escribir, y una línea de tiempo combinada (`GET /devices/{id}/history`) en el panel web y
en Android; y estadísticas (Sprint 16): agregaciones por hoy/7/30 días — apps más usadas,
desglose por categoría, conteo de bloqueos y cumplimiento de límites diarios — calculadas al vuelo
sobre datos ya existentes, sin tabla de agregación nueva, en el panel web y en Android; y alertas
(Sprint 17): una bandeja para el tutor generada a partir de señales que ya existían (bloqueos de
reglas, entradas/salidas de geocercas), con niveles INFO/WARNING/HIGH/CRITICAL (los dos últimos
reservados aún sin generador propio), deduplicación mientras la alerta siga sin leer y silenciado
por tipo, en el panel web (con acciones de marcar leída/silenciar) y en modo sólo lectura en
Android; y tiempo real (Sprint 18): un canal WebSocket por dispositivo
(`WS /devices/{id}/ws`, autenticado con un primer frame `{"token": ...}` en vez de una cabecera,
porque un navegador no puede fijar cabeceras en el *handshake*), al que se conectan el tutor activo
y el dispositivo supervisado dueño de ese dispositivo; cada cambio de regla (app, categoría,
política por defecto, horario escolar) difunde `rules_changed` a quien esté escuchando, verificado
de extremo a extremo contra el backend real; si el dispositivo no tiene el canal abierto y tiene un
token FCM registrado, el backend intenta despertarlo vía la API HTTP v1 de Firebase Cloud
Messaging (sin proyecto Firebase real en este repo todavía — pendiente de un humano, ver más
abajo); en Android, `RuleEnforcementService` usa el aviso para adelantar su refresco de reglas en
vez de esperar su sondeo periódico; en el panel web, `DeviceRulesPanel` recarga en vivo mientras
está abierto; y funcionamiento offline (Sprint 19): Room cachea en el dispositivo supervisado las
reglas/categorías/política/horario escolar (reemplazo total en cada fetch exitoso, sin fusión —
el servidor es la única fuente de verdad y el dispositivo nunca edita una regla localmente), un
arranque en frío sin red evalúa contra ese caché en vez de "todo permitido", un bloqueo que no se
pudo reportar queda en cola (`pending_rule_events`) hasta que un fetch exitoso o `SyncWorker` lo
vacíen, y `SyncWorker` (WorkManager, cada 15 min, sólo con conectividad) manda *heartbeat* y
sincroniza uso de apps aunque la app no esté en primer plano; tanto `RuleEnforcementService` como
`SyncWorker` renuevan su propio *access token* contra el `refresh_token` cifrado ya existente en
vez de depender de uno fijo que expira a los 15 minutos; y detección de manipulación (Sprint 20):
cinco señales legítimas —permiso de acceso a uso revocado, servicio de reglas detenido, hora del
dispositivo desfasada respecto del servidor, silencio anómalo del *heartbeat* e intento de
desinstalación (detectado registrando la app como Device Administrator, **no** device owner)— que
generan alertas `HIGH`/`CRITICAL` en la bandeja ya existente del Sprint 17 y dejan el dispositivo
en estado `ALERT`, sin impedir ninguna de esas acciones: se registra y se alerta, no se bloquea.
Las cuatro primeras viajan como campos opcionales del *heartbeat*; la quinta tiene endpoint propio
(`POST /devices/{id}/tamper-events`). Ninguna tabla nueva.

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

**Nota del Sprint 10**: el "enunciado" con las 11 categorías originales no existe en este repo (el
documento de 48 secciones queda fuera). El catálogo usado (`SOCIAL_MEDIA`, `GAMES`, `STREAMING`,
`EDUCATION`, `PRODUCTIVITY`, `COMMUNICATION`, `NEWS`, `SHOPPING`, `FINANCE`, `UTILITIES`,
`ADULT_CONTENT`) es una decisión propia confirmada con el dueño del proyecto, no una cita de la
fuente original — ver `docs/sprint-10.md`. Cualquier sprint futuro que necesite el enunciado real
debe pedírselo directamente, no asumir que ya está resuelto.

**Nota del Sprint 11**: `weekly_limit_minutes` es columna propia (en `app_rules` y
`category_rules`), no un rename de `daily_limit_minutes` — evita tocar la API y las suites de los
Sprints 8-10 sin necesidad. La semana es calendario (lunes 00:00 hora local), no una ventana móvil
de 7 días, coherente con `schedule_days_mask` (bit 0 = lunes). `devices.timezone` (IANA, reportado
en cada heartbeat) es sólo para que el tutor interprete lo que ve — la evaluación en Android ya
usaba correctamente la hora local del dispositivo desde el Sprint 8, no hacía falta reescribirla.

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

**Nota del Sprint 15**: no se creó ninguna tabla de "historial" genérica — `AppRuleEvent` y
`GeofenceEvent` ya eran insert-only, así que sólo ganaron retención (90 días, purga al escribir,
mismo patrón que `DeviceLocationReport` desde el Sprint 13) y un endpoint que los lee y combina
(`app/api/v1/endpoints/history.py`), sin persistir nada nuevo. Ubicación cruda queda fuera de la
línea de tiempo unificada a propósito: ya tiene su propia vista (Sprint 13) y mezclar hasta 96
puntos/día con eventos discretos enterraría la señal. Ver `docs/sprint-15.md`.

**Nota del Sprint 16**: mismo criterio que el Sprint 15 — sin tabla de agregación nueva, todo se
calcula al vuelo con `GROUP BY` en Python sobre `DeviceApplicationUsage`/`AppCategoryAssignment`/
`AppRuleEvent`/`AppRule`/`CategoryRule`, que ya existían. Los periodos (hoy/7d/30d) son fechas UTC
del servidor, no del huso horario del dispositivo — mismo criterio ya aceptado para `usage_date`
desde el Sprint 7 (etiqueta opaca que el servidor no recalcula). El cumplimiento de límites sólo
aplica a reglas `DAILY_LIMIT` (de app o de categoría); `WEEKLY_LIMIT` y `SCHEDULE` no tienen un
"día cumplido/incumplido" que calcular. Un día sin uso reportado no cuenta ni a favor ni en contra.
Ver `docs/sprint-16.md`.

**Nota del Sprint 17**: sin pipeline de detección nuevo — la alerta se genera inline en los dos
puntos de escritura que ya existían (`POST /rule-events`, `POST /location`, justo después de
`evaluate_geofence_transitions()`), mismo patrón "evaluar/purgar al escribir, sin scheduler" del
resto del proyecto. Deduplicación sin ventana de tiempo arbitraria: mientras una alerta con la
misma `dedup_key` siga sin leer, una repetición sólo le suma `occurrence_count`; marcarla leída
abre la puerta a que la siguiente ocurrencia sea una alerta nueva — evita inventar un umbral de
minutos/horas sin base en ningún enunciado. El silenciado se guarda por `(device_id, dedup_key)`,
no por alerta suelta: silenciar detiene *todo* bloqueo futuro de esa app o *toda* entrada/salida
futura de esa geocerca, no sólo la fila que se estaba viendo. `HIGH`/`CRITICAL` quedan en el
catálogo (con su propio `CHECK` constraint) sin generador propio todavía — igual que
`DeviceStatus.ALERT`, esperan las señales de manipulación del Sprint 20. Ver `docs/sprint-17.md`.

**Nota del Sprint 18, válida para cualquier sprint futuro que toque el canal en tiempo real o
FCM**: `google-services.json`/el SDK de Firebase Messaging **no** se agregaron a Android — el
plugin de Gradle `com.google.gms.google-services` rompe la compilación completa si ese archivo no
existe, y no hay proyecto Firebase real en este repo (pendiente de un humano con cuenta de Google
Cloud, igual que `GOOGLE_WEB_CLIENT_ID` en su momento — ver `docs/planning/plan-desarrollo.md`,
fila "Paso 17"). Lo que sí existe y funciona sin esa credencial: `devices.fcm_token`, el endpoint
para registrarlo, y `app/services/push.py`, que con `FCM_PROJECT_ID` vacío (el valor por defecto)
omite el envío con un log en vez de fallar el cambio de regla que lo disparó — la llamada real a
Google está aislada en una función (`_post_fcm_message`) para poder simularla con
`unittest.mock.patch`, mismo patrón que la verificación de ID tokens de Google desde el Sprint 3.
El registro de conexiones WebSocket vive en memoria del proceso backend, no en Redis: correcto hoy
porque el backend corre como un único *worker* de uvicorn (`backend/Dockerfile`, sin `--workers`);
necesitaría Redis pub/sub el día que corra en más de una réplica. En Android se agregó OkHttp
(`RealtimeClient.kt`) sólo para el WebSocket — el resto de los clientes de red sigue en
`java.net.HttpURLConnection` (ver `HttpJsonClient.kt`), porque `java.net` no tiene cliente de
WebSocket en absoluto y `java.net.http.WebSocket` sólo llegó a Android en la API 34, por encima del
`minSdk 26` de este proyecto. Ver `docs/sprint-18.md`.

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

**Siguiente: Sprint 21 — Seguridad integral.** Repaso de OWASP Top 10 y OWASP API Top 10 sobre lo
construido, con OWASP ASVS como lista de comprobación; rate limiting global, validación estricta,
cabeceras, CORS mínimo, gestión de secretos y TLS obligatorio; escaneo con OWASP ZAP contra el
entorno propio y MobSF sobre el APK. Empezar leyendo `docs/security-baseline.md`, que ya lleva la
matriz de permisos y los controles aplicados hasta hoy.

## Entorno de trabajo

- Windows con Docker Desktop, Android Studio (SDK 36 + un AVD con Google APIs) y Node/Python locales
  para correr pruebas fuera de contenedor cuando hace falta iterar rápido.
- `docs/sprint-01-paso-0.md` tiene el detalle de cómo se dejó Android Studio y Docker funcionando en
  esta máquina la primera vez, por si hay que replicarlo en otra.
- Variables de entorno: copiar `.env.development.example` a `.env` y completar los valores marcados
  como `change_me_*` o `your-*`. El `.env` real **nunca** se versiona.
- Credenciales de Google Cloud (`GOOGLE_WEB_CLIENT_ID`) ya existen para este proyecto en la cuenta de
  Google Cloud del dueño original; pídeselas directamente o crea un proyecto de Google Cloud propio
  siguiendo `docs/sprint-03.md` (sección de autenticación) — la app está en modo "Prueba", así que
  cualquier cuenta que use el login debe estar agregada como *tester* en la pantalla de
  consentimiento OAuth.

### Nota de rendimiento en Windows

Las pruebas de integración corren en segundos dentro de un contenedor y en varios minutos si se
ejecutan desde Windows contra los puertos publicados (cada petición paga ~1.4s en el proxy de
puertos de Docker Desktop). Para verificar, usar siempre:

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

No `docker compose -f compose.test.yaml up --build ...` con `migrate` en `depends_on`: ese flag
aborta todo el stack en cuanto cualquier contenedor termina, y `migrate` termina por diseño — mató
`backend` antes de que corriera sus pruebas la primera vez que se intentó (ver `docs/sprint-02-evidence.md`).

## Herramientas de apoyo instaladas

- **Impeccable** (`/impeccable`) — guía de diseño para el acabado visual de la web y de Android.
  Instalado a nivel de proyecto en `.claude/`, licencia Apache-2.0, corre local sin claves ni red.
  Las definiciones (skill, referencias, agentes) están versionadas; el binario del detector y
  `settings.local.json` no — en una máquina nueva se reinstala con `npx impeccable install`.
  Empezar con `/impeccable init` una sola vez para fijar el contexto de diseño del producto.
- **Skills de Emil Kowalski** (MIT, `emilkowalski/skills`) — `emil-design-eng` (criterio de diseño),
  `animate`, `review-animations`, `improve-animations`, `find-animation-opportunities`,
  `animation-vocabulary`, `apple-design` y `pick-ui-library`. Markdown puro, versionados. Se
  reinstalan con `npx skills@latest add emilkowalski/skills -s <nombre> -a claude-code --copy -y`.
  No se instalaron `animate-expo`, `write-swift` ni `ask-sonner`: este proyecto no usa React Native,
  ni Swift, ni Sonner. Ojo: estos skills están pensados para web (React/CSS); para las pantallas de
  Compose sirve el criterio de diseño, no el código de las recetas.
- **`/security-review`** — viene incluido con Claude Code, no hay que instalar nada. Revisa los
  cambios pendientes de la rama buscando vulnerabilidades. Correrlo **antes de cerrar cada sprint**,
  junto con las pruebas: este proyecto maneja datos sensibles de menores (inventario de apps, uso,
  y más adelante ubicación), así que la revisión de seguridad es parte del cierre, no un extra.
- **`/code-review`** — también incluido. Revisa el diff buscando errores de corrección y
  simplificaciones. Útil antes de un commit grande de sprint.

## Convenciones de código

- Backend: FastAPI + SQLAlchemy 2.0 (`Mapped`/`mapped_column`) + Alembic. `ruff check app tests
  alembic` debe pasar limpio (config en `backend/pyproject.toml`, con `B008` ignorado a propósito:
  es el patrón `Depends(...)` de FastAPI, no el bug que esa regla busca).
- Nunca `Base.metadata.create_all`: todo cambio de esquema es una migración de Alembic versionada.
- Secretos: cada uno con su propio nombre de variable y su propio valor — nunca reutilizar
  `JWT_SECRET` para otra cosa "porque ya existe". Si algo necesita un secreto nuevo, generarlo con
  `secrets.token_urlsafe(48)` y documentarlo en los tres `.env.*.example`.
- Frontend: Next.js App Router, componentes cliente (`"use client"`). Ver `src/contexts/AuthContext.tsx`
  para el patrón de sesión actual (en memoria + `sessionStorage`, no cookie `HttpOnly` — decisión
  documentada y deliberadamente pospuesta en `docs/sprint-03.md`).
- Android: sin Hilt/DI ni ViewModel todavía — el proyecto es pequeño y se ha mantenido así a
  propósito; no introducir esas dependencias sin que el tamaño del proyecto lo justifique.
- Commits: mensajes explicando el *por qué*, no sólo el qué. Co-autoría con el modelo que hizo el
  trabajo (revisar la guía de atribución vigente en cada sesión).

## Errores que ya se cometieron una vez — no repetirlos

- Un JWT firmado con HS256 puede coincidir carácter por carácter con otro si sólo cambia el último
  byte de la firma (relleno de Base64). Para pruebas que "alteran" un token, tocar un carácter del
  medio, no el último.
- Los tokens de acceso necesitan un `jti` aleatorio: sin él, dos emitidos en el mismo segundo para el
  mismo usuario son idénticos.
- Mezclar `TestClient` (corre la app en su propio *event loop*) con un fixture de base de datos que
  usa el motor global de la aplicación falla en contenedor (asyncpg rechaza conexiones de otro loop).
  El fixture compartido en `backend/tests/conftest.py` crea su propio motor por test; usarlo siempre.
- Las pruebas que ejercitan rate limiting necesitan una IP/host único por test — los contadores viven
  15 minutos en Redis y se filtran entre pruebas si comparten dirección.
- `compose.test.yaml` corre PostgreSQL sin volumen persistente a propósito. Reconstruir sólo
  `backend` y volver a hacer `up` sin repetir `run --rm migrate` primero deja una base sin tablas
  (falla con `relation "..." does not exist` en casi todas las pruebas, no sólo las nuevas). `migrate`
  corre aparte, siempre, antes de cada `up` — nunca asumir que la corrida anterior lo dejó aplicado.
- Un archivo Kotlin nuevo no está verificado hasta que `./gradlew compileDebugKotlin` (o más)
  corre sobre él al menos una vez. Un import que falta (p. ej. `Modifier.width` sin
  `androidx.compose.foundation.layout.width`) no lo marca ningún editor por sí solo; sólo el
  compilador real. No declarar un archivo Android terminado sin haberlo compilado.
- La regla de ESLint `react-hooks/set-state-in-effect` (la trae Next 16) rechaza que un `useEffect`
  invoque, directa o indirectamente, cualquier función que llame a `setState` — incluso una función
  `async` donde el `setState` ocurre después de un `await`. La forma que sí acepta: encadenar
  `.then()/.catch()` directamente en el cuerpo del efecto (con una bandera `cancelled` si hace falta
  cancelar), de modo que cada `setState` quede dentro de un callback de promesa ya resuelta, nunca de
  forma síncrona ni delegado a un helper. Ver `frontend/src/app/page.tsx` (patrón ya existente desde
  el Sprint 3) o `frontend/src/components/DevicesPanel.tsx` (Sprint 6) como referencia.
- `README.md` (raíz del repo) tiene su propia sección `## Alcance del Sprint N` por cada sprint —
  un changelog aparte de `docs/sprint-NN.md`, no mencionado en "Dónde está cada cosa" arriba. Se
  quedó sin actualizar en el Sprint 13 y sólo se notó al cerrar el Sprint 14 (el dueño del proyecto
  lo vio en GitHub, parado en el Sprint 12). Actualizarlo en cada cierre de sprint, junto con
  `sprint-NN.md`/`sprint-NN-evidence.md`.
- `compose.yaml` (el stack de desarrollo persistente) tiene `backend`, `web` y `migrate` como
  servicios con imágenes independientes aunque `backend` y `migrate` compartan el mismo
  `Dockerfile` de `backend/`. Reconstruir `backend`/`web` con `docker compose build` no reconstruye
  `migrate`. Si además se corrió `alembic upgrade head` directo desde el entorno local contra esta
  base (para verificar una migración nueva), la base queda en una revisión que el contenedor
  `migrate` desactualizado no reconoce, y el siguiente `up` falla con
  `Can't locate revision identified by '<revision>'`. Reconstruir los tres servicios juntos
  (`docker compose -f compose.yaml build backend web migrate`) cuando cualquiera de los dos cambie.

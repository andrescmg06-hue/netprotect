# Historial de estado — NetProtect

Recuento sprint por sprint, movido tal cual desde la sección "Estado actual" que tenía el
`CLAUDE.md` original (antes de la reestructuración a `.claude/rules/`). Es el archivo completo;
para el detalle real de cada sprint, seguir las citas hasta `docs/sprint-NN.md`. Algunas de las
notas de aquí abajo también viven, en el mismo texto, dentro de la regla de `.claude/rules/` de su
área (para que carguen solas al tocar esa ruta) — este archivo es la copia íntegra de referencia.

## Estado actual (13/09/2026)

Los 27 sprints del roadmap están completos. Sprint 27 (documentación y presentación): nueve
documentos en `docs/manuals/` que sintetizan por audiencia lo ya construido y verificado en los
sprints 1-26, sin agregar código ni alcance nuevo — incluida la constatación honesta de que el
control de navegación web por `VpnService`/DNS del plan original (Paso 8) nunca se construyó (se
evaluó y se pospuso explícitamente en el Sprint 9, y no se retomó). Ver `docs/sprint-27.md` y
`docs/sprint-27-evidence.md`.

Sprint 26 (despliegue): infraestructura como código verificada
localmente sin dominio ni cuenta cloud reales (ver la Nota del Sprint 26 más abajo); CI en los 8
jobs existentes verificado en verde tras sus cambios, y un nuevo workflow `cd.yml` que construye y
publica imágenes en GHCR — ver `docs/sprint-26-evidence.md` para la corrida real.

Sprints 1 a 25 completos y verificados en CI (8 jobs — `backend`, `frontend`, `android`,
`integration`, `android-instrumented`, `api-collection`, `e2e`, `performance` — en verde, runner
limpio; ver `docs/sprint-25-evidence.md`, sección "CI en GitHub Actions", corrida
[34738208116](https://github.com/andrescmg06-hue/netprotect/actions/runs/34738208116)). Sprint 23:
supervisión remota por WebRTC; pendiente sólo la verificación manual que exige una persona (una
persona real aceptando el diálogo de captura de Android) — ver `docs/sprint-23-evidence.md`.
Sprint 25 (pruebas integrales): barrido de autorización sobre el router real del backend (encontró
y dejó documentadas dos excepciones legítimas — `GET /` y `POST /auth/logout` — que nunca se
habían escrito como decisión); pruebas instrumentadas de Android reales en emulador (8/8, Sprint 19
offline) para `RulesCacheStore`/`PendingRuleEventStore`, sin ninguna cobertura hasta ahora; una
colección de API con Newman (13 peticiones, 23 aserciones, 0 fallos) contra el backend real; E2E
web con Playwright contra un build de producción real del panel (Sprint 24) y el backend real; y
una prueba de rendimiento con k6 (referencia repetible, no de estrés) — ver `docs/sprint-25.md` y
`docs/sprint-25-evidence.md` para el detalle, las decisiones de diseño y los hallazgos reales
encontrados en el camino. Los 4 jobs nuevos de CI que este sprint agrega
(`android-instrumented`, `api-collection`, `e2e`, `performance`) corrieron en verde en su primer
intento sobre un runner de GitHub Actions, incluido el emulador Android vía
`reactivecircus/android-emulator-runner@v2`.
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
(`POST /devices/{id}/tamper-events`). Ninguna tabla nueva. Y endurecimiento de seguridad
(Sprint 21): rate limiting global por IP en toda la API (más límites propios en `/auth/google` y
`/auth/refresh`, que antes no tenían ninguno), validación de longitud en los schemas de auth,
cabeceras ampliadas (`Cache-Control: no-store` y `Cross-Origin-Resource-Policy` siempre; HSTS y
CSP sólo en producción), rechazo de tráfico no-HTTPS en producción, `Settings` que se niega a
arrancar en producción con un secreto de desarrollo puesto, un `exception_handler` genérico que
impide que un error inesperado filtre su mensaje, `network_security_config.xml` en Android, y
`pip-audit`/`npm audit` en CI — todo verificado con un escaneo real de OWASP ZAP contra el backend
(dos hallazgos encontrados y corregidos) y de MobSF contra el APK debug y release. Ninguna tabla
ni migración nueva.

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

**Nota del Sprint 21, válida para cualquier sprint que toque rate limiting, cabeceras o el
cliente de Redis**: hay **dos** criterios de rate limiting a propósito, y confundirlos rompe uno
de los dos. `app/core/rate_limit.py` (`enforce_rate_limit`, usado por `/auth/google`,
`/auth/refresh` y `/pairing/*`) falla **cerrado** — 503 si Redis no responde —, porque una
protección anti-fuerza-bruta que desaparece en silencio no es protección. El limitador **global**
de `enforcement_middleware` (`app/main.py`, todas las rutas salvo `/api/v1/health*`) falla
**abierto**, porque tumbar el 100% de la API por una caída de Redis es una regresión de
disponibilidad desproporcionada para una capa genérica anti-abuso. `/api/v1/health*` queda
excluido del conteo por dos razones, ambas reales: un chequeo de salud no debe depender del mismo
Redis que vigila, y `test_health.py` corre en el job `backend` de CI **sin infraestructura**
(cualquier cosa que haga tocar Redis a `/health` rompe ese job). Consecuencia descubierta al
correr la suite completa: como ahora *cada* petición toca Redis, la carrera de *event loop*
cruzado que el docstring de `close_redis()` ya advertía dejó de ser teórica — `hit_rate_limit()`
traduce por eso `(RedisError, OSError, RuntimeError)` a `RateLimitBackendError`, y el
`RuntimeError` de ese trío **no es decorativo**: una conexión con el transporte roto no lanza
`RedisError`, y sin esa traducción reventaba la petición con un 500 en vez del *fail-open*
diseñado. `compose.test.yaml` sube `RATE_LIMIT_GLOBAL_MAX_PER_IP` a 100000 porque la suite entera
sale de una sola IP simulada; los tests que ejercitan el limitador fijan su propio valor por test.
TLS: lo que existe es *enforcement* (en producción, `request.url.scheme != "https"` → 400), no
*terminación* — el certificado y el proxy inverso siguen siendo del Paso 25, que exige un dominio
real. HSTS y CSP sólo se envían en producción: en dev/test romperían los assets de Swagger UI, que
ahí sigue habilitado. Ver `docs/sprint-21.md` y `docs/sprint-21-evidence.md` (incluye los
hallazgos reales de ZAP ya corregidos y los de MobSF revisados uno por uno).

**Nota del Sprint 22, válida para cualquier sprint futuro que toque auditoría**: `AuditLog`
(`app/models/audit_log.py`, tabla `audit_logs` desde el Sprint 2, escrita desde el Sprint 3) no
tiene columna `device_id` — sólo modela quién (`actor_user_id`) hizo qué (`action`) sobre qué
(`resource_type`/`resource_id`, string libre cuyo significado cambia según el tipo). Por eso
`GET /users/me/audit`/`GET /users/me/audit/export` (nuevos) están **scopeados a "mis propias
acciones"** (`actor_user_id == current_user.id`), no a "todo lo que pasó en mis dispositivos" —
esto último exigiría inventar una regla de reconstrucción por tipo de recurso que el modelo no
soporta directamente. Consecuencia aceptada: `DEVICE_LINKED` se audita con el **supervisado**
como actor (quien redime el código), así que no aparece en la auditoría del tutor aunque el
dispositivo sea suyo — no es pérdida real, el tutor ya ve el estado de vinculación en la lista de
dispositivos desde el Sprint 6. Sin `require_role`: cualquier usuario autenticado consulta sólo su
propio registro, sin que el rol conceda ni restrinja nada (el filtro por `actor_user_id` ya hace
imposible ver información ajena). Paginación real (`limit`/`offset` + `total`), divergencia
consciente del tope fijo `MAX_X` de historial/alertas (Sprint 15/17): esas tablas purgan a los 90
días y acotan su volumen real; `AuditLog` no tiene retención — el plan la llama explícitamente
"registro inmutable" — así que un tope fijo sin paginación ocultaría permanentemente lo más
antiguo. La exportación (`StreamingResponse`, CSV) reutiliza el mismo filtro sin paginar, con un
tope de seguridad fijo (`MAX_AUDIT_EXPORT_ROWS = 10 000`) en su lugar. Ni el listado ni la
exportación auditan su propia lectura (mismo criterio ya usado por `list_alerts`/
`get_device_history`: el rastro registra acciones a revisar después, no la revisión en sí). Sólo
panel web (filtros + paginación + export); Android es sólo lectura, sin filtros, y es la primera
sección de `TutorScreen` que es de cuenta en vez de por dispositivo. Ver `docs/sprint-22.md`.

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

**Hallazgo del Sprint 23 encontrado al cerrar el Sprint 24, ya corregido**: `/security-review`
detectó que `ConnectionManager.begin_screen_share` (`backend/app/services/realtime.py`)
sobrescribía `_screen_share_peers[device_id]` sin comprobar si ya había una sesión anclada a otro
tutor conectado — el mismo tipo de fallo que las dos correcciones del párrafo anterior, pero en el
flujo de *re*-solicitud: cualquier tutor vinculado podía reenviar `screen_share_request` en
cualquier momento (incluso a mitad de una sesión ya en curso) y redirigir en silencio a quién
llegaban el consentimiento y el video, sin que la persona supervisada viera de quién era la
solicitud. Corregido: `begin_screen_share` ahora devuelve `False` (sin tocar la sesión existente)
si el slot ya está ocupado por *otra* conexión, y el endpoint responde al tutor rechazado con un
frame `{"event": "screen_share_busy"}` en vez de robar la sesión en silencio o descartar el intento
sin avisar; `RemoteViewPanel.tsx` lo muestra como mensaje al tutor. Cubierto por
`test_a_second_tutor_cannot_hijack_a_screen_share_session_already_in_progress` — ver
`docs/sprint-24-evidence.md` para la salida real de la suite completa en Docker.

**Nota del Sprint 24, válida para cualquier sprint futuro que toque el panel web**: las "16
secciones del dashboard" del Paso 23 son, igual que las categorías del Sprint 10, una decisión
propia — el enunciado que originalmente las nombraba no está en este repo. Trece ya eran un
componente propio desde su sprint de origen; `DeviceRulesPanel` se dividió en `AppRulesPanel`
(reglas por app) y `DevicePolicyPanel` (política por defecto + horario escolar) porque eran dos
tareas distintas de un tutor con ciclos de vida distintos, no para completar la cuenta — ver
`docs/sprint-24.md` antes de reorganizar la navegación o añadir una sección nueva. El estado del
dispositivo activo (`DashboardShell`) se calcula en cada render a partir de la selección explícita
del usuario, no se sincroniza con un `useEffect`: la regla nueva de ESLint
(`react-hooks/set-state-in-effect`) rechaza cualquier `setState` síncrono en el cuerpo de un
efecto, incluso sin llamada de red de por medio — "derived state", no un efecto, es el patrón a
seguir. La navegación usa el hash de la URL (`window.location.hash` + un listener de
`hashchange`), no `useSearchParams`/rutas dinámicas del App Router, porque este panel entero vive
tras un *gate* de autenticación 100% cliente y `useSearchParams` exigiría un límite `<Suspense>`
sin aportar nada aquí.

**Nota del Sprint 25, válida para cualquier sprint futuro que toque el router de FastAPI, Newman,
Playwright o k6**: la versión de FastAPI resuelta en este proyecto (0.141) ya no aplana
`include_router()` en `app.routes` — una ruta incluida aparece como un objeto interno con
`effective_route_contexts()`, no como una `APIRoute` directa. `test_route_authorization_sweep.py`
(nuevo, `backend/tests/`) recorre ese método por *duck-typing* (comprobando el atributo, no
importando la clase interna) para no quedar atado a un nombre privado que puede cambiar en otra
versión. Esa misma prueba dejó documentadas dos excepciones reales que nadie había escrito antes:
`GET /` (el banner estático) y `POST /auth/logout` (se autentica por posesión del propio
`refresh_token` que revoca, no por un *access token* bearer — el punto entero de logout es
funcionar incluso con el *access token* ya expirado). Newman/Playwright/k6 no pueden mockear la
verificación de Google como sí hacen los tests de pytest (`unittest.mock.patch` sólo funciona
dentro del mismo proceso) — los tres usan `backend/scripts/seed_test_session.py` (nuevo,
copiado sólo en el stage `test` del `Dockerfile`, nunca en `runtime`) para mintar una sesión real
con las funciones **propias y no mockeadas** del proyecto, mismo mecanismo que ya usó a mano
`docs/sprint-24-evidence.md`; no es una puerta trasera de autenticación, ningún archivo de
`backend/app` cambia. `compose.test.yaml` ganó un servicio `api_server` (misma imagen de test que
`backend`/`migrate`, sólo con `command: uvicorn ...` y puerto publicado) porque `backend` ahí corre
pytest y termina — Newman/Playwright/k6 necesitan un servidor HTTP real que se quede arriba.
Descubierto de la manera difícil: el *refresh token* de este proyecto es rotativo y de un solo uso
(Sprint 3) — una suite E2E que inicia sesión más de una vez con el mismo valor sembrado falla la
segunda vez con `invalid_refresh_token`; `frontend/e2e/dashboard.spec.ts` por eso es **un** test
continuo (con `test.step` para el reporte) en vez de varios separados. Playwright corre contra
`node .next/standalone/server.js` (con `public/`/`.next/static` copiados a mano, como ya hace
`frontend/Dockerfile`), no contra `next start`/`next dev`: `next.config.ts` fija
`output: "standalone"` desde el Sprint 24, y `next start` se niega a arrancar con esa
configuración. Las pruebas instrumentadas de Android (`RulesCacheStoreTest`,
`PendingRuleEventStoreTest`, nuevas en `mobile/app/src/androidTest/`) cubren el offline/
sincronización del Sprint 19, sin ninguna cobertura hasta este sprint porque Room exige un runtime
Android real (o Robolectric, no instalado aquí); `PendingRuleEventStoreTest` apunta su
`RuleEnforcementClient` a un puerto sin nada escuchando para forzar un fallo de red **genuino**, no
simulado — este proyecto no tiene librería de *mocking*. Un emulador con imagen
`google_apis_playstore`/reciente puede quedar en `unauthorized` para `adb` incluso sin ser un
dispositivo físico; se resuelve con `adb kill-server && adb start-server`. La prueba de k6
(`perf/load-test.js`) es a propósito un perfil moderado y repetible, no de estrés: sin la
infraestructura real del Sprint 26, cualquier número de quiebre sólo describiría el contenedor de
desarrollo de quien la ejecute. Ver `docs/sprint-25.md` y `docs/sprint-25-evidence.md`.

**Nota del Sprint 26, válida para cualquier sprint futuro que toque el reverse proxy, los secretos
de producción o el pipeline de CD**: no había cuenta cloud ni dominio real al empezar este sprint
(confirmado con el dueño del proyecto antes de escribir código) — la decisión tomada fue construir
toda la infraestructura como código y verificar todo lo que sí se puede probar sin dominio público,
dejando lo que exige cuenta/dominio real documentado como pendiente, mismo patrón ya usado para
Firebase/Maps. Caddy (`infra/caddy/Caddyfile`) termina TLS real: Let's Encrypt automático contra un
dominio público (`WEB_DOMAIN`/`API_DOMAIN`, ya implícitos en `CORS_ORIGINS`/`ALLOWED_HOSTS`/
`NEXT_PUBLIC_API_BASE_URL` desde el Sprint 21), o su propia CA interna contra `*.localhost` sin
dominio propio — verificado con una cadena de certificado validada de extremo a extremo, nunca
`curl -k`. Tres hallazgos reales, los tres encontrados corriendo el stack **completo**, no en
aislamiento: `uvicorn --proxy-headers` sólo confía en `X-Forwarded-Proto` desde `127.0.0.1`, no
desde la IP de contenedor de Caddy, así que todo el tráfico llegaba rechazado con
`https_required` hasta sumar `--forwarded-allow-ips=*` al `CMD` del `Dockerfile` (seguro
específicamente porque backend no tiene otro punto de entrada en esta topología: sin puerto
publicado al host); un `reverse_proxy` de Caddy sin restricción de ruta reenviaba también
`/metrics` al dominio público, cerrado con un matcher explícito (`@metrics path /metrics` +
`respond 404`) antes del `reverse_proxy`; y Prometheus, que scrapea `backend:8000/metrics` en HTTP
simple dentro de la red `private` sin pasar nunca por Caddy, chocaba con el mismo
`https_required` — la alerta `BackendDown` llegó a **dispararse de verdad** por esta causa la
primera vez que se levantó el stack completo, y a resolverse de verdad tras el fix
(`_OPERATIONAL_PATH_PREFIXES` en `app/main.py`, que ya eximía a `/health*` desde antes — luego
unificada con la excepción, antes separada, del limitador de tasa global, que `/code-review`
encontró que seguía sin cubrir `/metrics`). Los ocho
secretos de producción (`JWT_SECRET`, `DATABASE_URL`, etc.) pasan de variables de entorno en texto
plano a archivos (`secrets/README.md`), con el mismo patrón `_FILE` que ya usan las imágenes
oficiales de PostgreSQL/Redis (`backend/docker-entrypoint.sh` los exporta antes de `exec`) — elegido
sobre el `secrets_dir` propio de `pydantic-settings` porque este último no ayuda con valores
ensamblados como `DATABASE_URL`/`REDIS_URL`. Métricas (`GET /metrics`) son código propio sobre
`prometheus_client`, no `prometheus-fastapi-instrumentator`: esa librería rompe **cada** request
con `AttributeError: '_IncludedRouter' object has no attribute 'path'`, el mismo cambio interno de
FastAPI 0.141 que `test_route_authorization_sweep.py` (Sprint 25) ya tuvo que sortear con
*duck-typing* — dos librerías rotas por el mismo cambio en un sprint no es coincidencia.
Prometheus + Grafana + Loki + Promtail corren enteramente en Docker (sin cuenta cloud) en la red
`private`; sólo Grafana publica un puerto, y sólo a `127.0.0.1` del host (túnel SSH, no dominio
público). Backups de PostgreSQL (`infra/backup/backup.sh`/`restore.sh`) verificados con un ciclo
real de pérdida de datos: insertar una fila marcador, respaldar, destruir la tabla, restaurar,
confirmar que vuelve. `.github/workflows/cd.yml` construye y publica imágenes en GHCR encadenado
con `workflow_run` a la finalización real (no en paralelo) del workflow `ci`; `deploy-production`
queda detrás de un *environment* de GitHub llamado `production` y se salta (nunca falla) mientras
`vars.PROD_HOST` no exista, mismo patrón que `fcm_project_id` vacío en `app/services/push.py`. Ese
*environment* **no se pudo configurar con revisor obligatorio** en este sprint: crear o modificar
un *environment* de GitHub exige permisos de administrador sobre el repositorio, y el token usado
sólo tenía permisos de colaborador — pendiente de que el dueño del repositorio
(`andrescmg06-hue`) lo configure desde Settings → Environments → production. Ver
`docs/sprint-26.md` y `docs/sprint-26-evidence.md`.

**Nota del Sprint 27, válida para cualquier trabajo futuro sobre `docs/manuals/`**: son documentos
de síntesis, no una segunda fuente de verdad — cada uno cita el `docs/sprint-NN.md` o archivo de
código concreto donde vive el detalle real. Si un sprint futuro cambia algo que un manual describe,
actualizar el manual en el mismo commit que el cambio, igual que ya se hace con `README.md`. La
funcionalidad de control de navegación web (`VpnService`/filtrado DNS) del plan original sigue sin
construirse — documentada así a propósito en `docs/manuals/analisis-riesgos.md` y
`docs/manuals/documento-tecnico.md` (§3.1), no como un "V2" menor sino como un paso completo del
plan que quedó sin implementar. Ver `docs/sprint-27.md` y `docs/sprint-27-evidence.md`.

**Los 27 sprints del roadmap original están completos.** Después se abrió un plan propio de tres
sprints, `docs/planning/plan-turn.md` (Sprints 28–30): un servidor TURN para que la vista remota
del Sprint 23 transmita video entre redes sin camino directo. Una prueba en vivo del 24/09/2026
mostró que la señalización funcionaba pero el video no llegaba (ICE `failed` entre el emulador y
el navegador del host).

**Nota del Sprint 28, válida para cualquier trabajo futuro sobre coturn o las credenciales TURN**:
coturn (`infra/coturn/`) corre en los tres compose **solo en su propia red y con IP fija**, porque
`denied-peer-ip` (obligatorio, para que el relay no sirva para llegar a PostgreSQL/Redis) también
corta el relay entre dos clientes del mismo coturn, y la única salida verificada es
`allowed-peer-ip` apuntando a esa IP exacta (spike R1, `docs/sprint-28-evidence.md`). No bajar
`user-quota` de 16 sin medir: con 4 una sesión legítima ya fallaba con `486`, porque una conexión
WebRTC abre una asignación por interfaz × URL × transporte. El backend emite credenciales efímeras
(`app/services/turn.py`, esquema `use-auth-secret` de coturn, 1 hora, nonce aleatorio y nunca el
`user_id`) en el campo nuevo `turn_servers` de `webrtc-config`; `ice_servers` no se toca por
compatibilidad con la APK instalada. `scripts/verify_turn.sh`, paso del job `integration` de CI, es
la prueba real contra coturn: pytest sólo puede verificar la forma de la credencial. Ver
`docs/sprint-28.md`.

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

Lo que resta fuera del plan TURN es exclusivamente lo que varias notas de sprint ya documentan
como pendiente de un humano con cuenta cloud, dominio o permisos de administrador sobre el
repositorio de GitHub (ver `docs/manuals/analisis-riesgos.md` para la lista consolidada), más lo
que el Sprint 29 dejó anotado como backlog propio en `docs/sprint-29.md` (dispositivos duplicados
en `GET /devices/me`, reconexión del canal en tiempo real de Android, y el mensaje claro cuando la
misma cuenta es tutor y supervisado, heredado del Sprint 28).

**Nota de los Sprints 31–38 (rediseño del panel web), válida para cualquier cambio futuro en
`frontend/`**: plan en `docs/planning/plan-frontend.md`, un `docs/sprint-NN.md` por sprint y
`docs/sprint-38-cierre.md`. La regla que lo gobierna: **el aspecto sigue a los mockups, la
funcionalidad sigue al backend** — si un mockup muestra algo que la API no tiene, se omite o se
reemplaza por el dato real equivalente, nunca se simula. Ninguna ruta de API ni lógica de negocio
cambió en estos ocho sprints. Cómo está armado ahora: tokens de diseño en
`src/app/globals.css` (`:root`; los colores `*-text` son los que pasan AA como texto — los de
relleno como `--color-primary` no), un CSS Module por componente (sin Tailwind), primitivas en
`src/components/ui/` (importar siempre desde el barrel `@/components/ui`), `lucide-react` como
única librería de iconos, y **ninguna librería de gráficos ni de mapas**: `DonutChart`,
`BarList`, `ProgressBar`, `Timeline` y el esquema de geocercas (`GeofenceMap`) son SVG/HTML
propios — el mapa de geocercas es un esquema a escala a propósito, para no mandar coordenadas de
un menor a un servidor de teselas externo. `DashboardShell` renderiza el `PageHeader` de cada
sección; los paneles **no** deben renderizar el suyo. `globals.css` ya no tiene estilos heredados
de los Sprints 1–24 ni el antiguo "look" de `<button>`: su `:where(button)` es un reset neutro, así
que un botón nativo nuevo necesita su propia clase. Lecciones concretas de estos sprints:
`react-hooks/refs` prohíbe leer `ref.current` durante el render (usar estado), y
`react-hooks/immutability` prohíbe reasignar una variable local dentro del `map()` del JSX
(precalcular con `reduce` antes del `return`, ver `DonutChart`); una barra que se anima debe usar
`transform: scaleX`, no `width` (el detector de Impeccable lo marca como *layout thrash*); el
`<video>` de `RemoteViewPanel` debe quedar **siempre montado** — el evento `track` de WebRTC
asigna `srcObject` al nodo que `videoRef` ya apunta, y montarlo sólo en el estado "streaming"
pierde el stream; y `next build` en Windows falla con `EBUSY ... .next/standalone` si queda vivo un
`node .next/standalone/server.js` de una verificación anterior (matarlo antes de reconstruir).
Para verificar una vista con datos reales: levantar `compose.test.yaml` (con `migrate` primero),
sembrar con `backend/scripts/seed_test_session.py` + llamadas HTTP reales, servir el build
standalone en `localhost:3000` (el `CORS_ORIGINS` de test es exactamente ese origen) y detener
antes `backend`/`web` de `compose.yaml`, que ocupan el mismo puerto 8000. El *refresh token* de
la sesión sembrada es de un solo uso: cada `page.goto` de Playwright que recarga la app lo rota.

# Matriz preliminar de capacidades Android

Fecha de revisión: 07/09/2026 (Sprint 14: geocercas — por qué este proyecto decide NO usar la
Geofencing API de Android/GMS. Sprint 13: geolocalización aproximada. Sprint 9: identificar apps
que nunca deben bloquearse en modo lista blanca. Sprint 8: mecanismo de bloqueo sin device owner.
Sprint 7: enumeración de apps y estadísticas de uso). Todas verificadas contra fuentes oficiales
actuales — ver secciones dedicadas más abajo. Revisión anterior: 05/09/2026. Esta matriz evita
asumir capacidades que una aplicación Android convencional no posee.

| Capacidad | API/mecanismo oficial | Requisito principal | Limitación relevante | Decisión |
|---|---|---|---|---|
| Acceso a Internet | `INTERNET` | Permiso normal en manifiesto | No autoriza datos sensibles por sí mismo | Sprint 1 |
| Enumerar apps instaladas | `PackageManager` | `QUERY_ALL_PACKAGES` (permiso especial, sin diálogo runtime, sujeto a aprobación de Play) | Android 11+ filtra por defecto (`<queries>`); no cubre nuestro caso porque no sabemos de antemano qué apps tiene el supervisado | Sprint 7 — ver detalle abajo |
| Estadísticas de uso | `UsageStatsManager` | `PACKAGE_USAGE_STATS` + concesión del usuario en Ajustes (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) para la mayoría de consultas | No equivale a control total de otras apps; posible degradación de puntualidad por App Standby Buckets (sin confirmar en fuente oficial) | Sprint 7 — ver detalle abajo |
| App de supervisión visible (política anti-stalkerware) | N/A — política de **Google Play**, no del sistema Android | Sólo aplica si la app se **publica** en la tienda | Este proyecto se instala por sideload (Android Studio / `adb`) para el trabajo de la universidad, no se publica — la política no aplica hoy | Documentado y pospuesto, no bloqueante mientras no haya publicación — ver aclaración abajo |
| Bloqueo de apps sin device owner | `UsageStatsManager.queryEvents()` sondeado periódicamente (eventos `MOVE_TO_FOREGROUND`/`ACTIVITY_RESUMED`) + pantalla de bloqueo propia (`Activity`/overlay) | `PACKAGE_USAGE_STATS` (mismo permiso ya concedido en Sprint 7); un foreground service para sondear de forma sostenida | No hay API de notificación push para "app pasó a primer plano": hay que sondear, así que existe una ventana entre que la app aparece y se detecta/bloquea; evadible revocando el permiso en Ajustes o deteniendo el servicio | Sprint 8 — ver detalle abajo |
| Identificar launcher, teléfono y Ajustes (para nunca bloquearlos) | `PackageManager.resolveActivity()` con `ACTION_MAIN`+`CATEGORY_HOME` y con `Settings.ACTION_SETTINGS`; `TelecomManager.getDefaultDialerPackage()` y `getSystemDialerPackage()` | Visibilidad de paquetes (ya cubierta por `QUERY_ALL_PACKAGES` del Sprint 7) | Todas pueden devolver `null`; si la resolución falla, esa app queda fuera de la lista protegida y podría bloquearse en modo lista blanca | Sprint 9 — ver detalle abajo |
| Filtrado de tráfico local | `VpnService` | Preparación/consentimiento del usuario | Sólo una app VPN puede estar preparada a la vez; el usuario puede revocar | Evaluar Sprint 10+ (pospuesto explícitamente en el Sprint 9: necesita su propia Fase C) |
| Geolocalización | `LocationManager` (`NETWORK_PROVIDER`) | `ACCESS_COARSE_LOCATION` (runtime) + foreground service `location` iniciado sólo desde una `Activity` en primer plano | Sin `ACCESS_BACKGROUND_LOCATION`: el reporte se detiene si el proceso muere y la app no se reabre | Sprint 13 — ver detalle abajo. Decisión: sólo aproximada, sin permiso de segundo plano |
| Geocercas | Ninguna API de Android nueva — evaluación server-side sobre `LocationReportingService` (Sprint 13) | Ninguno (reutiliza `ACCESS_COARSE_LOCATION` ya concedido) | Latencia de detección atada al intervalo de reporte (~15 min), no a los 2-6 min de la Geofencing API real | Sprint 14 — ver detalle abajo. Decisión: NO usar la Geofencing API de Android/GMS |
| Detectar intento de desinstalación | Registro como Device Administrator (`DeviceAdminReceiver.onDisableRequested()`) — no existe ninguna API para detectar la desinstalación en sí | Activación por el usuario vía `ACTION_ADD_DEVICE_ADMIN` (pantalla del sistema); `BIND_DEVICE_ADMIN` en el receiver | No impide desinstalar: sólo obliga a desactivar el administrador primero, y eso avisa. El usuario puede retirarlo cuando quiera; el *callback* no tiene veto | Sprint 20 — ver detalle abajo |
| Saber si un servicio propio sigue vivo desde otro proceso | Ninguna API soportada (`getRunningServices()` obsoleto y limitado al propio proceso) — marca de tiempo cooperativa (`EnforcementLiveness`) | Ninguno | Cooperativa por diseño: detecta paradas ordinarias, no a un adversario técnico decidido | Sprint 20 — ver detalle abajo |
| Notificaciones | `NotificationListenerService` | Acceso habilitado por el usuario | Debe minimizarse el contenido recolectado | Evaluar Sprint 17/23 |
| Captura de pantalla | `MediaProjection` | Consentimiento del usuario y foreground service `mediaProjection` | En Android moderno el consentimiento no puede reutilizarse indefinidamente; cada sesión debe respetar las reglas vigentes | Evaluar Sprint 23 |
| Cámara remota | Camera + foreground service cuando aplique | `CAMERA` y estado/flujo permitido | Permisos while-in-use y restricciones para iniciar desde background | V2/Futuro |
| Micrófono remoto | AudioRecord/MediaRecorder + FGS cuando aplique | `RECORD_AUDIO` | Restricciones while-in-use/background | V2/Futuro |
| Administración empresarial profunda | Device Policy APIs / DPC | Aprovisionamiento como device/profile owner cuando corresponda | No debe asumirse para una instalación parental convencional de Play Store | Fuera del MVP salvo caso justificado |

## Sprint 7 — Inventario de apps: verificación detallada (05/09/2026)

Procedimiento obligatorio de la Fase C aplicado antes de escribir código. Fuentes oficiales
consultadas directamente (no memoria de entrenamiento); se marca explícitamente lo que no se pudo
confirmar en una fuente actual en vez de asumirlo.

### Enumerar apps instaladas

Desde Android 11 (API 30), si la app apunta a API 30+, el sistema **filtra por defecto** el
resultado de `getInstalledApplications()`, `getInstalledPackages()`, `queryIntentActivities()`, etc.
Sólo se ven los paquetes declarados en el elemento `<queries>` del manifiesto (por nombre, por
intent o por autoridad de proveedor). El elemento `<queries>` no sirve para nuestro caso porque no
podemos saber de antemano qué apps instalará un dispositivo supervisado. La alternativa es el
permiso `QUERY_ALL_PACKAGES`: especial (se declara en el manifiesto, se concede al instalar sin
diálogo runtime), pero **su publicación en Play está sujeta a aprobación** mediante el
"Permissions Declaration Form" de Play Console.

Riesgo real encontrado: la página de política enumera como usos permitidos "device search, antivirus
apps, file managers, and browsers" — **no nombra explícitamente control parental**. No se debe asumir
aprobación automática; hay que declarar el caso de uso real (inventario para control parental) en el
formulario y verificar la respuesta de Google antes de depender de esta capacidad en producción.

Fuentes: <https://developer.android.com/training/package-visibility>,
<https://developer.android.com/training/package-visibility/declaring>,
<https://support.google.com/googleplay/android-developer/answer/10158779>.

### Estadísticas de uso por app

`UsageStatsManager` (paquete `android.app.usage`, disponible desde API 21) expone `queryUsageStats()`
y `queryEvents()` con el tiempo de uso agregado por día/semana/mes/año. Requiere el permiso especial
`PACKAGE_USAGE_STATS`: declararlo en el manifiesto no basta, el usuario debe concederlo aparte en
Ajustes (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) — igual que Accessibility o Notification Listener,
no es un permiso runtime normal. Desde Android R (API 30), si el usuario del dispositivo no está
"unlocked" (`UserManager#isUserUnlocked()`), estos métodos devuelven `null` en vez de datos: hay que
manejarlo defensivamente, no asumir que siempre hay respuesta.

No se encontró una página de política de Play dedicada a este permiso (a diferencia de
`QUERY_ALL_PACKAGES`, que sí la tiene). **Esto es ausencia de evidencia, no confirmación de que no
aplica ninguna política** — se trata como pendiente de verificar en el propio Play Console al
publicar, no como hecho asentado. Tampoco se encontró una fuente oficial que documente en qué medida
los App Standby Buckets afectan la puntualidad de `queryUsageStats()`; se anota como plausible pero
no confirmado, no como limitación documentada.

Fuentes: <https://developer.android.com/reference/android/app/usage/UsageStatsManager> (mirror del
javadoc AOSP en
<https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/usage/UsageStatsManager.java>),
<https://developer.android.com/topic/performance/appstandby>.

### Aclaración clave: nada de esto aplica sin publicar en Google Play

Verificado explícitamente porque cambia la conclusión práctica: **las políticas de Google Play (el
formulario de declaración de `QUERY_ALL_PACKAGES`, la política anti-stalkerware, la divulgación
destacada) se activan cuando la app se envía a revisión de Play, no por usar un permiso**. Este
proyecto se instala directo desde Android Studio o `adb` en el emulador/dispositivo de prueba — eso
es sideload, Google nunca la revisa. `QUERY_ALL_PACKAGES` y `PACKAGE_USAGE_STATS` funcionan
exactamente igual sideloaded que publicados (se conceden igual); lo único que cambia es que **nadie
en Google evalúa el caso de uso**.

También se verificó que Play Protect (la protección que corre en el propio teléfono, no la revisión
de la tienda) **bloquea automáticamente la instalación sideloaded** de apps que declaren alguno de
estos cuatro permisos: `RECEIVE_SMS`, `READ_SMS`, acceso a notificaciones (`BIND_NOTIFICATION_
LISTENER_SERVICE`) o Accessibility Service — activo en ~185 países. **Ninguno de los dos permisos de
este sprint está en esa lista**, así que tampoco bloquea la instalación de prueba. Ese bloqueo sólo
aplica a instalaciones "desde internet" (navegador, mensajería, gestor de archivos); instalar por
cable desde Android Studio es una vía distinta y no entra ahí. Importante para sprints futuros: el
acceso a notificaciones (`NotificationListenerService`, evaluado para Sprint 17/23) sí está en esa
lista — revisar de nuevo la vía de instalación cuando se llegue a esa capacidad.

La política anti-stalkerware (notificación persistente + ícono distintivo mientras se supervisa,
exigida por Play para apps de control parental) **queda documentada y pospuesta**: es información
real y correcta para si este proyecto se publicara alguna vez, pero no bloquea el desarrollo ni las
pruebas mientras la instalación sea por sideload. No se implementa en este sprint por esa razón —no
porque se haya decidido ignorar la política, sino porque hoy no aplica.

Fuentes: <https://developers.google.com/android/play-protect/phacategories>,
<https://support.google.com/googleplay/android-developer/answer/11150561>,
<https://developers.google.com/android/play-protect/client-protections>,
<https://blog.google/intl/en-in/products/launching-enhanced-fraud-protection-pilot-in-india/>.

### Pendiente de confirmar (no se asume, se declara pendiente — sólo relevante si algún día se publica)

1. Si Google acepta "control parental / gestión familiar de dispositivos" como justificación válida
   en el formulario de declaración de `QUERY_ALL_PACKAGES` — la página de política de Play no lo
   nombra explícitamente, aunque la guía técnica de Android sí incluye "apps de gestión de
   dispositivos" entre los casos de uso aceptables, que es un encaje más cercano.
2. Si `PACKAGE_USAGE_STATS` realmente no tiene ningún paso de declaración en Play Console, o si la
   página existe y no se encontró.
3. El mecanismo y magnitud exactos con que los App Standby Buckets afectarían la puntualidad de
   `queryUsageStats()`.
4. ~~El diseño de la notificación persistente / foreground service para modo Supervisado, si el
   proyecto llegara a publicarse — no diseñado todavía porque no es necesario hoy.~~ **Corregido en
   el Sprint 8**: esto no era correcto tal como estaba escrito. La notificación persistente de un
   foreground service es una obligación del propio sistema operativo Android para poder sondear en
   segundo plano (no sólo una exigencia de la política anti-stalkerware de Play, que sigue aplicando
   sólo si se publica) — se necesita ya, sideloaded o no, en cuanto el sondeo debe seguir corriendo
   sin que la pantalla del tutor esté abierta. Ver la sección "Sprint 8" más abajo.

## Sprint 8 — Bloqueo de apps sin device owner: verificación detallada (05/09/2026)

Procedimiento obligatorio de la Fase C aplicado antes de escribir código, según lo exige
`CLAUDE.md` para este sprint.

### Cómo detectar qué app está en primer plano sin ser device owner

`ActivityManager#getRunningTasks()` y `getRunningAppProcesses()` están deprecados/restringidos desde
Android 5.0 (API 21): desde entonces sólo devuelven procesos de la propia app, no de terceros — no
sirven para este caso. La única vía documentada y vigente para saber qué otra app tiene el usuario
abierta, sin privilegios de administrador de dispositivo, es `UsageStatsManager.queryEvents()`
filtrando eventos `UsageEvents.Event.MOVE_TO_FOREGROUND` / `MOVE_TO_BACKGROUND` (o los más granulares
`ACTIVITY_RESUMED`/`ACTIVITY_PAUSED`, a nivel de `Activity` en vez de proceso). Usa el mismo permiso
especial `PACKAGE_USAGE_STATS` que ya se obtuvo en el Sprint 7 (concedido por el usuario en Ajustes,
no runtime) — no se necesita pedir nada nuevo.

Verificado directamente en el código fuente con javadoc de AOSP (mirror del `UsageStatsManager.java`
real, no una página resumen): desde Android R, si el usuario del dispositivo no está "unlocked"
(`UserManager#isUserUnlocked()`), `queryEvents()` devuelve `null` — igual que `queryUsageStats()`, ya
documentado en el Sprint 7. También confirmado: el sistema sólo conserva los eventos "por unos pocos
días" (`Events are only kept by the system for a few days`), lo cual no es relevante para bloqueo en
vivo pero sí importaría si se quisiera reconstruir historial pasado desde este mismo mecanismo.

**No se encontró documentación oficial que fije una latencia o un intervalo de sondeo recomendado**
para `queryEvents()` — no existe una API de tipo callback/push que avise "esta app acaba de pasar a
primer plano"; hay que sondear (poll) el método periódicamente desde un servicio en ejecución. Esto
es ausencia de evidencia, no confirmación de que no exista alguna guía: se declara pendiente en vez
de inventar un número. Consecuencia práctica que sí se puede afirmar con certeza aunque no haya cifra
oficial: **existe una ventana de tiempo real entre que la app supervisada aparece en primer plano y
el momento en que nuestro mecanismo la detecta y muestra la pantalla de bloqueo** — no es bloqueo
instantáneo ni preventivo, es reactivo.

### Alternativa considerada y descartada para este sprint: `AccessibilityService`

Un `AccessibilityService` recibe eventos (`TYPE_WINDOW_STATE_CHANGED`) de forma reactiva ante cambios
de ventana en primer plano, en teoría con menor latencia que sondear `UsageStatsManager`. Se descarta
para este sprint por lo ya verificado en el Sprint 7 y registrado en este mismo documento: Play
Protect **bloquea automáticamente la instalación sideloaded** (vía "desde internet": navegador,
mensajería, gestor de archivos) de cualquier app que declare un servicio de Accessibility, en ~185
países — a diferencia de `PACKAGE_USAGE_STATS`, que no está en esa lista. Aunque hoy este proyecto se
instala por cable desde Android Studio (vía distinta, no bloqueada), adoptar Accessibility ahora
introduciría un riesgo real si en algún momento se prueba instalando un APK "desde internet" en un
dispositivo de prueba, y además carga con el estigma de ser el mecanismo típico de apps
stalkerware reales. Queda anotado como alternativa técnica válida, no como error, por si un sprint
futuro necesita reconsiderarla con esa restricción explícita en mente.

### Límites explícitos del mecanismo elegido (declarar siempre, no prometer bloqueo garantizado)

- **No es preventivo, es reactivo**: la app objetivo puede quedar visible brevemente antes de que el
  sondeo la detecte y se muestre la pantalla de bloqueo propia.
- **El usuario supervisado puede revocar el permiso** en Ajustes (`Settings.ACTION_USAGE_ACCESS_
  SETTINGS`) en cualquier momento y desactivar la detección sin que Android lo impida — es un permiso
  especial revocable, no un candado del sistema.
- **El servicio que sondea puede detenerse**: si el usuario fuerza el cierre de la app o del proceso
  en segundo plano (o el sistema lo mata bajo presión de memoria/Doze sin que exista un mecanismo de
  reinicio fuera de lo que el propio proyecto implemente), el sondeo se interrumpe y con él el
  bloqueo, hasta que algo lo reinicie.
- **Requiere que el usuario haya desbloqueado el dispositivo** al menos una vez tras el arranque
  (`isUserUnlocked()`) para que `queryEvents()` devuelva datos; antes de eso, la detección no
  funciona (afecta sobre todo al primer arranque tras reiniciar el dispositivo).
- **No sustituye a un control de administrador de dispositivo real**: un usuario con conocimientos
  técnicos puede desinstalar la app supervisada, revocar el permiso especial, o (si tiene acceso de
  desarrollador) usar herramientas de depuración para inspeccionar o interferir con el proceso. Nada
  de esto se puede impedir sin ser device owner, y el proyecto no lo es ni lo pretende para el MVP.

### Requisito real: foreground service con notificación (no sólo política de Play)

Corrige lo anotado como pendiente en el Sprint 7 (ver punto 4 más arriba, tachado): sondear
`queryEvents()` mientras la pantalla del tutor o del supervisado no está abierta exige un
**foreground service**, y Android **exige por sí mismo** que todo foreground service muestre una
notificación mientras corre (`startForeground()` con un objeto `Notification`, prioridad `LOW` o
mayor) — esto es un requisito del sistema operativo desde Android 8 (API 26), no la política
anti-stalkerware de Play (que sigue aplicando sólo si se publica, y sigue sin aplicar hoy).

Verificado además, específico de este proyecto (`compileSdk`/`targetSdk` 36, muy por encima del
umbral): desde Android 14 (API 34) hay que declarar un `foregroundServiceType` en el manifiesto.
Ningún tipo predefinido (`camera`, `location`, `mediaPlayback`, etc.) encaja en "vigilar qué app
tiene el usuario en primer plano" — la propia documentación de Android confirma que para ese caso
`specialUse` es la única opción, y exige declarar el permiso `FOREGROUND_SERVICE_SPECIAL_USE` además
de `FOREGROUND_SERVICE`, más un elemento `<property android:name="android.app.PROPERTY_SPECIAL_USE_
FGS_SUBTYPE" android:value="...">` con una justificación en texto — que Google sólo revisa si la app
se publica (no aplica hoy, igual que el resto de políticas de Play ya documentadas).

También verificado: `POST_NOTIFICATIONS` (permiso runtime de Android 13+) no es necesario para que el
foreground service arranque — si el usuario lo niega, el servicio corre igual, sólo que la
notificación no aparece en la bandeja (sigue visible en el "FGS Task Manager" del sistema). Se pide
igual en este proyecto porque la notificación es deliberadamente visible, no algo que ocultar (ver
decisión de no perseguir sigilo en `docs/sprint-08.md`), no porque sea obligatorio para que el
bloqueo funcione.

Fuentes: <https://developer.android.com/develop/background-work/services/fgs/launch>,
<https://developer.android.com/about/versions/14/changes/fgs-types-required>,
<https://developer.android.com/develop/background-work/services/fgs/service-types>,
<https://developer.android.com/develop/ui/compose/notifications/notification-permission>.

### Verificado en ejecución real (emulador Pixel_8, API 36, 05-06/09/2026)

No basta con que compile: se instaló el APK y se arrancó `RuleEnforcementService` de verdad contra
un emulador real. Dos hallazgos que sólo aparecen en ejecución, no en el código fuente:

1. **Arrancar el foreground service "en frío" (por ejemplo, `adb shell am start-service` sin que la
   app tenga antes una actividad visible) falla** con `Error: app is in background uid null` — la
   restricción de Android 12+ a iniciar foreground services desde segundo plano. Sólo funcionó tras
   traer `MainActivity` a primer plano primero. Esto **confirma que el diseño ya elegido es el
   correcto y no opcional**: `RuleEnforcementService.start()` debe llamarse desde una `Activity` en
   primer plano (como ya hace `SupervisedScreen` en su `DisposableEffect`), nunca desde un contexto
   que Android considere "background".
2. Con la app en primer plano, el servicio **sí entra en estado foreground real**, confirmado con
   `dumpsys activity services`: `isForeground=true`, `types=0x40000000` (el valor numérico de
   `specialUse`), notificación con `flags=ONGOING_EVENT|FOREGROUND_SERVICE` en el canal
   `rule_enforcement` — sin ninguna `SecurityException` ni
   `MissingForegroundServiceTypeException`. Sin excepciones en el proceso durante los ~40 segundos
   que corrió sondeando contra un backend inalcanzable (credenciales falsas a propósito), lo que
   también confirma que los fallos de red silenciosos (`runCatching`) no lo interrumpen.

### Pendiente de confirmar

1. Intervalo de sondeo óptimo (ni oficial ni de terceros confirmado) — se decidirá empíricamente al
   implementar, documentando el valor elegido y su justificación (batería vs. latencia de bloqueo) en
   `docs/sprint-08.md`, no en esta matriz.
2. Comportamiento exacto de Doze/App Standby sobre un foreground service que sondea
   `queryEvents()` de forma sostenida — mismo estado de "plausible pero no confirmado" ya anotado
   para `queryUsageStats()` en el Sprint 7.

Fuentes: <https://developer.android.com/reference/android/app/usage/UsageStatsManager>,
<https://developer.android.com/reference/android/app/usage/UsageEvents.Event>,
<https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/usage/UsageStatsManager.java>
(javadoc real de `queryEvents()`, confirma el comportamiento con `isUserUnlocked()` y la retención de
"a few days"), <https://developers.google.com/android/play-protect/phacategories> y
<https://developers.google.com/android/play-protect/client-protections> (bloqueo de sideload a apps
con Accessibility Service, ya citadas en la sección del Sprint 7 de este mismo documento).

## Sprint 9 — Apps que nunca deben bloquearse: verificación detallada (05/09/2026)

Procedimiento obligatorio de la Fase C, aplicado antes de escribir código. Este sprint introduce el
modo **lista blanca** (bloquear por defecto, permitir sólo lo aprobado). Sin una lista de apps
protegidas, ese modo bloquearía también el launcher, la app de teléfono y Ajustes — dejando el
dispositivo inutilizable y, peor, pudiendo estorbar una llamada de emergencia. Antes de implementar
había que verificar cómo se identifican esas apps de forma fiable, sin hardcodear nombres de paquete
que varían entre fabricantes.

### Launcher (app de inicio)

`PackageManager.resolveActivity()` con un `Intent(ACTION_MAIN)` + `CATEGORY_HOME` y la bandera
`MATCH_DEFAULT_ONLY` devuelve el `ResolveInfo` de la app de inicio actual; el paquete sale de
`resolveInfo.activityInfo.packageName`. `resolveActivity()` devuelve `null` si nada puede atender el
intent, así que hay que tratarlo defensivamente. `MATCH_DEFAULT_ONLY` filtra a actividades con
`CATEGORY_DEFAULT`, que es lo correcto para un intent implícito como éste.

Sobre visibilidad de paquetes: desde Android 11 esta resolución estaría filtrada y podría devolver
`null` aunque la app exista, salvo que se declare un elemento `<queries>` o se tenga
`QUERY_ALL_PACKAGES`. **Este proyecto ya tiene `QUERY_ALL_PACKAGES` desde el Sprint 7**, así que no
hace falta agregar `<queries>` — se anota explícitamente porque quien lea sólo esta sección podría
concluir lo contrario.

### Teléfono (dialer)

Verificado directamente en el código fuente con javadoc de AOSP (`TelecomManager.java`), no en una
página resumen:

- `getDefaultDialerPackage()` — "package name for the default dialer package or null if no package
  has been selected as the default dialer". Sin anotación `@RequiresPermission` en el método.
- `getSystemDialerPackage()` — "Determines the package name of the system-provided default phone
  app"; devuelve "package name for the system dialer package or null if no system dialer is
  preloaded".

Se usan **ambos**: el usuario puede haber elegido un dialer distinto del preinstalado, y en ese caso
las dos apps son candidatas legítimas a protegerse. Las dos pueden devolver `null`.

### Ajustes

Misma técnica que el launcher, resolviendo `Settings.ACTION_SETTINGS`. Es la vía por la que el
usuario supervisado puede revocar el acceso a uso; bloquearla sería, además de hostil, una forma de
atrapar al usuario en un estado del que no puede salir — justo lo contrario de lo que este proyecto
declara sobre no prometer bloqueos inevadibles.

### Límite honesto de este mecanismo

Si alguna de esas resoluciones devuelve `null` (fabricante atípico, ausencia de app de teléfono en
una tablet, etc.), esa app simplemente no entra en la lista protegida y **podría bloquearse** en modo
lista blanca. No se compensa con nombres de paquete hardcodeados (`com.android.settings` y
compañía), porque varían entre fabricantes y darían una falsa sensación de cobertura. Mitigación
real que sí existe: el bloqueo de este proyecto es reactivo y sólo superpone una pantalla — no
impide que la app siga corriendo por debajo ni bloquea la bandeja de notificaciones ni los ajustes
rápidos del sistema, así que no puede dejar a nadie sin salida de forma absoluta.

Fuentes: <https://developer.android.com/reference/android/content/pm/PackageManager>,
<https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/telecomm/java/android/telecom/TelecomManager.java>
(javadoc real de `getDefaultDialerPackage()` y `getSystemDialerPackage()`),
<https://developer.android.com/reference/android/provider/Settings#ACTION_SETTINGS>,
<https://developer.android.com/training/package-visibility>.

## Sprint 10 — Categorías: sin verificación nueva de Android (05/09/2026)

A diferencia de los Sprints 7-9, este sprint no toca ningún permiso ni mecanismo del sistema
operativo que no estuviera ya verificado. Categorías es dato nuevo (a qué categoría pertenece una
app, qué regla tiene esa categoría) que se evalúa con el mismo `RuleEvaluator` y el mismo
`RuleEnforcementService` ya verificados en el Sprint 8 — sólo cambia qué tabla consulta antes de
caer en la política por defecto del dispositivo (Sprint 9). No aplica el procedimiento de la Fase C
porque no hay ninguna capacidad de Android nueva que verificar.

## Sprint 13 — Geolocalización: verificación detallada (07/09/2026)

Procedimiento obligatorio de la Fase C, aplicado antes de escribir código Android. Primer sprint
que toca permisos de ubicación en este proyecto — nada de esto estaba verificado todavía. Fuentes
oficiales consultadas directamente (developer.android.com), no memoria de entrenamiento.

### Niveles de acceso: aproximada vs. precisa (Android 12 / API 31+)

Desde Android 12, el usuario elige entre dos niveles de precisión al conceder el permiso, y esa
elección determina qué exactitud recibe la app **independientemente de qué permiso declare**:

- `ACCESS_COARSE_LOCATION` (declarado sin `ACCESS_FINE_LOCATION`): sólo acceso **aproximado**
  ("accurate to within about 3 square kilometers"). No dispara el selector de Android 12+ (ese
  selector sólo aparece cuando la app pide ambos permisos a la vez).
- `ACCESS_FINE_LOCATION`: acceso **preciso** ("usually within about 50 meters"), salvo que el
  usuario elija explícitamente "aproximada" en el selector — en ese caso, "regardless of which
  location permissions your app declares", el resultado es aproximado igual.

**Decisión de este sprint**: declarar únicamente `ACCESS_COARSE_LOCATION`. La frecuencia elegida
(~15 minutos, ver `docs/sprint-13.md`) es para saber en qué zona está el dispositivo supervisado,
no para rastrear su posición exacta en tiempo real — pedir precisión que el caso de uso no
necesita viola minimización de datos (el mismo principio ya aplicado al resto del proyecto) y
evita además tener que programar el selector aproximada/aproximada-vs-precisa de Android 12+, que
sólo se activa cuando se piden ambos permisos juntos.

Fuente: <https://developer.android.com/training/location/permissions>.

### `ACCESS_BACKGROUND_LOCATION`: por qué este sprint decide NO pedirlo

Verificado en <https://developer.android.com/develop/sensors-and-location/location/permissions/background>
y <https://developer.android.com/about/versions/10/privacy/changes>:

- Desde Android 10 (API 29), acceder a la ubicación **mientras la app está en segundo plano**
  exige declarar `ACCESS_BACKGROUND_LOCATION` en el manifiesto y concederlo aparte en tiempo de
  ejecución.
- Desde Android 11 (API 30), el diálogo del sistema **ya no ofrece "Permitir todo el tiempo"**:
  pedirlo junto con el permiso en primer plano no funciona (el sistema ignora ese intento); hay
  que dirigir al usuario a Ajustes con `getBackgroundPermissionOptionLabel()` para obtener el
  texto localizado exacto del botón, con una UI educativa propia antes (mismo patrón ya usado en
  este proyecto para `PACKAGE_USAGE_STATS`, Sprint 7).
- **Hallazgo clave que cambia la decisión de diseño**: la propia documentación de Android define
  cuándo una app cuenta como "en segundo plano" a efectos de este permiso — *"An app is considered
  to be accessing location in the background unless ... the app is running a foreground service
  that has declared a foreground service type of `location`"*. Es decir: **un foreground service
  con `foregroundServiceType="location"` ya cuenta como "en primer plano" para el sistema de
  permisos de ubicación**, sin necesitar `ACCESS_BACKGROUND_LOCATION`, mientras ese servicio siga
  vivo.

Este proyecto ya tiene el patrón exacto que hace falta para explotar esa excepción:
`RuleEnforcementService` (Sprint 8) se arranca sólo desde una `Activity` en primer plano
(`SupervisedScreen`) y sigue corriendo con una notificación persistente mientras el usuario usa
otras apps. Un `LocationReportingService` construido igual — arrancado desde
`SupervisedScreen.DisposableEffect`, con `foregroundServiceType="location"` — puede seguir
reportando ubicación aunque el usuario abra otra app, sin pedir nunca el permiso de segundo plano.

**Consecuencia práctica de esta decisión, declarada explícitamente en vez de prometer más de lo
que se puede sostener**: si el usuario supervisado cierra la app de un swipe en Recientes o el
sistema mata el proceso, el foreground service muere con él (mismo límite ya documentado para
`RuleEnforcementService` en el Sprint 8) y el reporte de ubicación se detiene hasta que la app se
vuelva a abrir. No hay reinicio automático (`BOOT_COMPLETED`, `WorkManager` persistente) en este
sprint — evaluar si hace falta queda para un sprint futuro si el caso de uso lo exige.

Ventaja adicional de esta decisión, verificada más abajo: al no declarar
`ACCESS_BACKGROUND_LOCATION`, la política de Play sobre "Prominent Disclosure" de ubicación en
segundo plano (ver siguiente sección) **no aplica en absoluto** a este proyecto — ni siquiera bajo
el razonamiento habitual de "sólo aplica al publicar", porque el permiso que la dispara nunca se
declara.

### Foreground service de tipo `location`

Verificado en <https://developer.android.com/develop/background-work/services/fgs/service-types>:

- Requiere declarar `android:foregroundServiceType="location"` en el `<service>` del manifiesto
  (obligatorio desde Android 14/API 34 para todo foreground service, ya aplicado en este proyecto
  al `specialUse` de `RuleEnforcementService` — mismo requisito, Sprint 8).
- Requiere los permisos `FOREGROUND_SERVICE` (ya declarado) y `FOREGROUND_SERVICE_LOCATION`
  (nuevo), más al menos uno de `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION` concedido en tiempo
  de ejecución antes de llamar a `startForeground()`.
- **Misma restricción ya verificada en ejecución real en el Sprint 8**: *"you cannot create a
  `location` foreground service while your app is in the background, unless you've been granted
  the `ACCESS_BACKGROUND_LOCATION` runtime permission"*. Como este proyecto no pide ese permiso
  (ver arriba), `LocationReportingService.start()` debe llamarse siempre desde una `Activity` en
  primer plano — exactamente el mismo patrón que `RuleEnforcementService.start()` en
  `SupervisedScreen`, nunca desde un contexto de fondo. No se repite la verificación empírica en
  emulador hecha en el Sprint 8 para este servicio nuevo porque el mecanismo de arranque (mismo
  `DisposableEffect`, mismo `startForegroundService()`) es idéntico al ya probado; si Android
  rechazara el arranque se manifestaría igual que en el Sprint 8 (`Error: app is in background uid
  null`), y la compilación/ejecución real de este sprint (ver `docs/sprint-13-evidence.md`) es lo
  que confirma que no ocurrió.

Fuentes: <https://developer.android.com/develop/background-work/services/fgs/service-types>,
<https://developer.android.com/about/versions/14/changes/fgs-types-required> (ya citada en Sprint 8).

### Educación antes del diálogo del sistema

`shouldShowRequestPermissionRationale()` sigue siendo el mecanismo estándar (sin cambios respecto
a permisos runtime "normales" ya usados en Android desde hace años) para decidir cuándo mostrar
una explicación propia antes de disparar el diálogo del sistema. Este proyecto ya tiene el patrón
exacto en `SupervisedScreen` para `POST_NOTIFICATIONS` (Sprint 8) y una tarjeta explicativa previa
para `PACKAGE_USAGE_STATS` (Sprint 7); Sprint 13 reutiliza la misma forma (tarjeta con texto +
botón) en vez de introducir un componente nuevo.

Fuente: <https://developer.android.com/training/permissions/requesting>.

### Política de Google Play sobre ubicación en segundo plano — verificado, no aplica hoy

Mismo razonamiento ya aplicado a `QUERY_ALL_PACKAGES` y Accessibility (Sprints 7-8), verificado de
nuevo para este permiso en concreto porque el enunciado pedía no copiarlo sin más:

- Play exige **Prominent Disclosure**: una divulgación dentro de la propia app (no sólo en la
  política de privacidad), visible sin que el usuario tenga que navegar a un menú, explicando qué
  dato se accede y para qué, **específicamente para apps que declaran
  `ACCESS_BACKGROUND_LOCATION`**.
- Play también exige un formulario de declaración de permisos con un video de demostración para
  cualquier app que pida ubicación en segundo plano, evaluado sólo al publicar en la tienda —
  igual que el formulario de `QUERY_ALL_PACKAGES` del Sprint 7.
- **Este proyecto no declara `ACCESS_BACKGROUND_LOCATION`** (ver decisión de diseño arriba), así
  que esta política no aplica ni siquiera bajo el razonamiento de "sólo al publicar": el permiso
  que la activa no existe en el manifiesto. Se documenta de todas formas para que quede registrado
  qué se verificó y por qué no aplica, no para dejarlo asumido.

Fuentes: <https://support.google.com/googleplay/android-developer/answer/11150561> (mejores
prácticas de "prominent disclosure and consent"),
<https://support.google.com/googleplay/android-developer/answer/9799150> (entendiendo los permisos
de ubicación en segundo plano).

### Decisión de no usar el SDK nativo de Google Maps en Android

`GOOGLE_MAPS_ANDROID_API_KEY` no se añadió a las variables de entorno de este sprint (a pesar de
que el encargo lo contemplaba como opción) porque se decidió no integrar el Maps SDK for Android
en absoluto: la pantalla del tutor muestra coordenadas/hora en texto y un botón "Abrir en mapa" que
lanza un `Intent(ACTION_VIEW, Uri.parse("geo:lat,lng?q=lat,lng"))` — Android resuelve ese intent con
cualquier app de mapas ya instalada (Google Maps en el emulador/dispositivo de prueba), sin
necesitar clave de API, restricción por SHA-1/paquete, ni la dependencia adicional del SDK. El
panel web sí necesita su propia clave (Maps Embed API, restringida por referer HTTP) porque un
navegador no tiene una "app de mapas" a la que delegar — ver `docs/sprint-13.md`.

### Pendiente de confirmar

1. Comportamiento exacto de Doze/App Standby sobre un foreground service de tipo `location` que
   reporta cada ~15 minutos — mismo estado "plausible pero no confirmado" ya anotado para
   `UsageStatsManager` en los Sprints 7-8; no se encontró una página oficial que fije una cifra.
2. Verificación en dispositivo físico real (no sólo emulador) del comportamiento de
   `NETWORK_PROVIDER` sin Google Play services de ubicación (`FusedLocationProviderClient`) — este
   proyecto usa `LocationManager` puro (sin añadir la dependencia `play-services-location`, igual
   que el resto de la app evita dependencias grandes) y no se ha comparado su precisión/latencia
   real contra el SDK de Google en un dispositivo con Play Services. Ver `docs/sprint-13-evidence.md`
   para lo que sí se verificó (emulador).

Fuentes citadas en esta sección: <https://developer.android.com/training/location/permissions>,
<https://developer.android.com/develop/sensors-and-location/location/permissions/background>,
<https://developer.android.com/about/versions/10/privacy/changes>,
<https://developer.android.com/develop/background-work/services/fgs/service-types>,
<https://developer.android.com/training/permissions/requesting>,
<https://support.google.com/googleplay/android-developer/answer/11150561>,
<https://support.google.com/googleplay/android-developer/answer/9799150>.

## Sprint 14 — Geocercas: verificación detallada (07/09/2026)

Procedimiento obligatorio de la Fase C, aplicado antes de escribir código — con un matiz respecto a
sprints anteriores: aquí la verificación termina en la decisión de **no** adoptar la API que el
plan de desarrollo asumía, precisamente porque se verificó primero. Fuente oficial consultada
directamente: <https://developer.android.com/develop/sensors-and-location/location/geofencing>.

### Qué exige realmente la Geofencing API de Android (`GeofencingClient`)

Verificado explícitamente porque `CLAUDE.md` lo pedía ("revisar de nuevo... antes de asumir que no
hace falta una nueva Fase C"): la Geofencing API real de Android —la que crea el plan de
desarrollo (`docs/planning/plan-desarrollo.md`, Paso 13)— **no** es una extensión de
`LocationManager` puro (lo que este proyecto usa desde el Sprint 13). Es parte de
**Google Play Services** (`com.google.android.gms.location.GeofencingClient`,
`LocationServices.getGeofencingClient()`) y exige, sin excepción documentada:

1. **`ACCESS_FINE_LOCATION`** — siempre, no sólo `ACCESS_COARSE_LOCATION`.
2. **`ACCESS_BACKGROUND_LOCATION`** (apps con `targetSdk` ≥ 29, que es el caso de este proyecto)
   para que los eventos ENTER/EXIT lleguen mientras la app **no** está en primer plano — que es
   justamente el caso de uso real: detectar que el supervisado cruzó una zona sin tener NetProtect
   abierto en ese momento.
3. La dependencia **`play-services-location`**, que este proyecto evita a propósito desde el
   Sprint 13 (usa `LocationManager` puro, sin Google Play services de ubicación).

A cambio, ofrece: hasta 100 geocercas por app/usuario, arquitectura sin foreground service propio
(un `PendingIntent`/`BroadcastReceiver` que Play Services invoca), y latencia de detección de
"usualmente menos de 2 minutos" (hasta 2-3 minutos con los límites de ubicación en segundo plano de
Android 8+, hasta 6 minutos con el dispositivo estacionario) — gestionada íntegramente por el
sistema, sin que la app tenga que sondear nada.

### Por qué este sprint decide NO usarla

Adoptarla exigiría revertir, no extender, tres decisiones de diseño ya tomadas y documentadas en
el Sprint 13 (`docs/sprint-13.md`, sección "Frecuencia y precisión"):

- Pasar de `ACCESS_COARSE_LOCATION` a `ACCESS_FINE_LOCATION` — más precisión de la que el caso de
  uso ("¿en qué zona está?") necesita, violando minimización de datos sin un beneficio real para
  este proyecto.
- Pedir `ACCESS_BACKGROUND_LOCATION` — exactamente el permiso que el Sprint 13 evitó a propósito
  explotando que un foreground service `location`-typed ya cuenta como "en primer plano" para el
  sistema de permisos. La Geofencing API no participa de esa excepción: sus callbacks llegan desde
  un proceso de Play Services, no desde nuestro propio foreground service, así que necesita el
  permiso de segundo plano de verdad.
- Añadir `play-services-location` — la única dependencia grande que este proyecto ha evitado dos
  veces ya (Sprint 13: ubicación con `LocationManager` puro; también evitó el Maps SDK for
  Android).

Esta decisión se confirmó con el dueño del proyecto antes de escribir código (mismo patrón que las
categorías del Sprint 10): ver `docs/sprint-14.md` para las dos alternativas planteadas y la
elegida.

### La alternativa elegida no toca Android en absoluto

Al evaluar transiciones ENTER/EXIT en el backend comparando reportes de ubicación consecutivos
contra cada geocerca (ver `docs/sprint-14.md` y `backend/app/services/geofencing.py`), Android no
necesita ningún permiso, dependencia, servicio ni cambio de manifiesto nuevo — `LocationReportingService`
(Sprint 13) sigue exactamente igual. Consecuencia aceptada y documentada: la latencia de detección
queda atada al intervalo de reporte (~15 minutos) en vez de los 2-6 minutos que ofrecería la
Geofencing API real — un costo directo de mantener la superficie de permisos mínima ya establecida,
no un descuido.

### Límite de geocercas: decisión propia, no un límite de la plataforma

Como este proyecto no usa la Geofencing API de Android, el límite real de esa API (100 por
app/usuario) no aplica. `settings.max_geofences_per_device = 20` (`backend/app/core/config.py`) es
una decisión propia — una salvaguarda de rendimiento (cuántas geocercas evalúa cada reporte de
ubicación), no un límite impuesto por el sistema operativo.

Fuente: <https://developer.android.com/develop/sensors-and-location/location/geofencing>.

## Sprint 20 — Detección de manipulación: verificación detallada (08/09/2026)

Mismo procedimiento que los sprints anteriores: las fuentes oficiales se consultaron antes de
escribir código, y se marca explícitamente lo que no se pudo confirmar.

### Detectar un intento de desinstalación sin ser device owner

Punto de partida verificado: una app **no puede** enterarse de su propia desinstalación. Sus
`BroadcastReceiver` desaparecen junto con el paquete, y `ACTION_PACKAGE_REMOVED` nunca se entrega
a la app que se está eliminando. No existe ninguna API para "el usuario abrió el diálogo de
desinstalar esta app".

Lo que sí existe, y es lo que el enunciado del Paso 19 llama "la API oficial", es el registro como
**Device Administrator** (`android.app.admin.DeviceAdminReceiver`):

- Mientras un administrador está activo, **Android impide desinstalar la app**: el usuario debe
  desactivarlo primero ("To uninstall an existing device admin app, users need to first unregister
  the app as an administrator").
- Esa desactivación dispara `onDisableRequested(Context, Intent)`, que devuelve un `CharSequence`
  que el propio sistema muestra como advertencia en su diálogo de confirmación. Después, si el
  usuario confirma, se llama a `onDisabled()`.
- **No hay veto**: el *callback* no puede cancelar ni retrasar la desactivación. Sólo advierte y
  se entera. La documentación tampoco garantiza `onDisableRequested()` en *todos* los caminos
  posibles (p. ej. flujos administrativos que eliminan el perfil completo); para el flujo normal
  por Ajustes sí se llama.
- Se activa con `ACTION_ADD_DEVICE_ADMIN` + `EXTRA_DEVICE_ADMIN` (y `EXTRA_ADD_EXPLANATION`), que
  abre una pantalla de confirmación del sistema. **Esto no es device owner ni aprovisionamiento
  MDM**: es un permiso corriente que el usuario concede y puede retirar cuando quiera. La fila
  "Administración empresarial profunda" de la tabla de arriba (que este proyecto sigue descartando)
  se refiere a device/profile *owner*, que exige aprovisionamiento en la configuración inicial del
  dispositivo; el administrador básico no.
- Manifiesto obligatorio: `<receiver android:permission="android.permission.BIND_DEVICE_ADMIN">`
  (restringe la invocación al sistema), `<meta-data android:name="android.app.device_admin">`
  apuntando a un XML con `<uses-policies>`, e `<intent-filter>` con
  `android.app.action.DEVICE_ADMIN_ENABLED`.
- `<uses-policies>` declara qué políticas aplicará el administrador. Este proyecto lo deja
  **vacío**: no cambia contraseñas, no fuerza bloqueo, no borra datos, no desactiva la cámara. El
  registro existe únicamente por el *callback* de desactivación.
- `onDisableRequested()` corre en el hilo principal y debe devolver rápido: nada de red ahí. La
  guía oficial de trabajo en segundo plano señala WorkManager como la vía para pasar trabajo
  garantizado desde un *broadcast receiver*, que es lo que hace `TamperReportWorker`.

Consecuencia aceptada y documentada en `docs/sprint-20.md`: esto **no vuelve la app
indesinstalable**, y no se pretende. Añade un paso previo y, sobre todo, un punto de detección.

Fuentes: <https://developer.android.com/reference/android/app/admin/DeviceAdminReceiver> y
<https://developer.android.com/work/device-admin>.

### Saber si un servicio propio sigue vivo desde otro proceso

No hay forma soportada. `ActivityManager.getRunningServices()` está obsoleto desde Android 8 y,
desde Android 5.0, sólo devuelve información del proceso que pregunta — el mismo límite ya
documentado en el Sprint 8 para `getRunningTasks()`/`getRunningAppProcesses()`. De ahí que
`EnforcementLiveness` sea una marca de tiempo cooperativa en `SharedPreferences` (sellada por
`RuleEnforcementService`, leída por `SyncWorker` y por el bucle de *heartbeat*) y no una consulta
al sistema. Detecta paradas ordinarias (deslizar la app fuera de Recientes, forzar detención,
muerte por presión de memoria); no pretende resistir a un adversario técnico decidido, exactamente
igual que el resto del mecanismo de bloqueo desde el Sprint 8.

### VPN: sigue sin aplicar

El Paso 19 menciona "revocación de la VPN". `VpnService` sigue siendo una capacidad **evaluada y
no adoptada** (fila "Filtrado de tráfico local" de la tabla, pospuesta en el Sprint 9 y nunca
retomada). Sin componente VPN no hay revocación que detectar; ver `docs/sprint-20.md` para la
decisión completa.

## Referencias oficiales consultadas

- Android Developers — `UsageStatsManager`.
- Android Developers — `UsageEvents.Event`, Sprint 8.
- Android Developers — foreground services: arranque, tipos de servicio y requisito de tipo desde
  Android 14, permiso de notificaciones en tiempo de ejecución, Sprint 8.
- Android Developers — `VpnService`.
- Android Developers — Geofencing.
- Android Developers — `NotificationListenerService`.
- Android Developers — `MediaProjectionManager` y cambios de comportamiento de Android 14+.
- Android Developers — foreground service types y restricciones de background.
- Android Developers — Package visibility (`<queries>` y `QUERY_ALL_PACKAGES`), Sprint 7.
- Play Console Help — política de `QUERY_ALL_PACKAGES` y formulario de declaración de permisos, Sprint 7.
- Play Protect — categorías de Potentially Harmful Apps (Stalkerware/Commercial Spyware), Sprint 7 y 8.
- Play Console Help — Prominent disclosure and consent, Sprint 7.
- AOSP — código fuente con javadoc de `UsageStatsManager.java` (mirror en `android.googlesource.com`), Sprint 8.
- Android Developers — `PackageManager.resolveActivity()`/`MATCH_DEFAULT_ONLY` y `Settings.ACTION_SETTINGS`, Sprint 9.
- AOSP — código fuente con javadoc de `TelecomManager.java` (`getDefaultDialerPackage()`, `getSystemDialerPackage()`), Sprint 9.
- Android Developers — permisos de ubicación (`ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION`, aproximada vs. precisa Android 12+), Sprint 13.
- Android Developers — `ACCESS_BACKGROUND_LOCATION` y cambios de privacidad de Android 10, Sprint 13.
- Android Developers — foreground service type `location` y sus requisitos, Sprint 13.
- Android Developers — `shouldShowRequestPermissionRationale()` y UI educativa, Sprint 13.
- Play Console Help — "Prominent disclosure and consent" y permisos de ubicación en segundo plano, Sprint 13.
- Android Developers — Geofencing API (`GeofencingClient`, permisos, límite de 100 geocercas, latencia de detección), Sprint 14.
- Android Developers — `DeviceAdminReceiver` (`onDisableRequested()`/`onDisabled()`), Sprint 20.
- Android Developers — Device administration overview (`ACTION_ADD_DEVICE_ADMIN`, `BIND_DEVICE_ADMIN`, `<uses-policies>`, desinstalación bloqueada mientras el administrador está activo), Sprint 20.

La matriz debe revisarse nuevamente en el sprint que implemente cada capacidad porque las políticas y restricciones de Android pueden cambiar.

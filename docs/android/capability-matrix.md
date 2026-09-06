# Matriz preliminar de capacidades Android

Fecha de revisión: 05/09/2026 (Sprint 9: identificar apps que nunca deben bloquearse en modo lista blanca. Sprint 8: mecanismo de bloqueo sin device owner. Sprint 7: enumeración de apps y estadísticas de uso). Todas verificadas contra fuentes oficiales actuales — ver secciones dedicadas más abajo. Revisión anterior: 30/08/2026. Esta matriz evita asumir capacidades que una aplicación Android convencional no posee.

| Capacidad | API/mecanismo oficial | Requisito principal | Limitación relevante | Decisión |
|---|---|---|---|---|
| Acceso a Internet | `INTERNET` | Permiso normal en manifiesto | No autoriza datos sensibles por sí mismo | Sprint 1 |
| Enumerar apps instaladas | `PackageManager` | `QUERY_ALL_PACKAGES` (permiso especial, sin diálogo runtime, sujeto a aprobación de Play) | Android 11+ filtra por defecto (`<queries>`); no cubre nuestro caso porque no sabemos de antemano qué apps tiene el supervisado | Sprint 7 — ver detalle abajo |
| Estadísticas de uso | `UsageStatsManager` | `PACKAGE_USAGE_STATS` + concesión del usuario en Ajustes (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) para la mayoría de consultas | No equivale a control total de otras apps; posible degradación de puntualidad por App Standby Buckets (sin confirmar en fuente oficial) | Sprint 7 — ver detalle abajo |
| App de supervisión visible (política anti-stalkerware) | N/A — política de **Google Play**, no del sistema Android | Sólo aplica si la app se **publica** en la tienda | Este proyecto se instala por sideload (Android Studio / `adb`) para el trabajo de la universidad, no se publica — la política no aplica hoy | Documentado y pospuesto, no bloqueante mientras no haya publicación — ver aclaración abajo |
| Bloqueo de apps sin device owner | `UsageStatsManager.queryEvents()` sondeado periódicamente (eventos `MOVE_TO_FOREGROUND`/`ACTIVITY_RESUMED`) + pantalla de bloqueo propia (`Activity`/overlay) | `PACKAGE_USAGE_STATS` (mismo permiso ya concedido en Sprint 7); un foreground service para sondear de forma sostenida | No hay API de notificación push para "app pasó a primer plano": hay que sondear, así que existe una ventana entre que la app aparece y se detecta/bloquea; evadible revocando el permiso en Ajustes o deteniendo el servicio | Sprint 8 — ver detalle abajo |
| Identificar launcher, teléfono y Ajustes (para nunca bloquearlos) | `PackageManager.resolveActivity()` con `ACTION_MAIN`+`CATEGORY_HOME` y con `Settings.ACTION_SETTINGS`; `TelecomManager.getDefaultDialerPackage()` y `getSystemDialerPackage()` | Visibilidad de paquetes (ya cubierta por `QUERY_ALL_PACKAGES` del Sprint 7) | Todas pueden devolver `null`; si la resolución falla, esa app queda fuera de la lista protegida y podría bloquearse en modo lista blanca | Sprint 9 — ver detalle abajo |
| Filtrado de tráfico local | `VpnService` | Preparación/consentimiento del usuario | Sólo una app VPN puede estar preparada a la vez; el usuario puede revocar | Evaluar Sprint 10+ (pospuesto explícitamente en el Sprint 9: necesita su propia Fase C) |
| Geolocalización | Location Services | Permisos de ubicación según alcance | Restricciones de background y precisión | Sprint 13 |
| Geocercas | Geofencing API | `ACCESS_FINE_LOCATION`; background location al aplicar según target/uso | Límite de geocercas y latencia en background | Sprint 14 |
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

La matriz debe revisarse nuevamente en el sprint que implemente cada capacidad porque las políticas y restricciones de Android pueden cambiar.

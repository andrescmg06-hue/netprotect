# Matriz preliminar de capacidades Android

Fecha de revisión: 05/09/2026 (Sprint 7: enumeración de apps y estadísticas de uso, verificado contra fuentes oficiales actuales — ver sección dedicada más abajo). Revisión anterior: 30/08/2026. Esta matriz evita asumir capacidades que una aplicación Android convencional no posee.

| Capacidad | API/mecanismo oficial | Requisito principal | Limitación relevante | Decisión |
|---|---|---|---|---|
| Acceso a Internet | `INTERNET` | Permiso normal en manifiesto | No autoriza datos sensibles por sí mismo | Sprint 1 |
| Enumerar apps instaladas | `PackageManager` | `QUERY_ALL_PACKAGES` (permiso especial, sin diálogo runtime, sujeto a aprobación de Play) | Android 11+ filtra por defecto (`<queries>`); no cubre nuestro caso porque no sabemos de antemano qué apps tiene el supervisado | Sprint 7 — ver detalle abajo |
| Estadísticas de uso | `UsageStatsManager` | `PACKAGE_USAGE_STATS` + concesión del usuario en Ajustes (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) para la mayoría de consultas | No equivale a control total de otras apps; posible degradación de puntualidad por App Standby Buckets (sin confirmar en fuente oficial) | Sprint 7 — ver detalle abajo |
| App de supervisión visible (política anti-stalkerware) | N/A — política de **Google Play**, no del sistema Android | Sólo aplica si la app se **publica** en la tienda | Este proyecto se instala por sideload (Android Studio / `adb`) para el trabajo de la universidad, no se publica — la política no aplica hoy | Documentado y pospuesto, no bloqueante mientras no haya publicación — ver aclaración abajo |
| Filtrado de tráfico local | `VpnService` | Preparación/consentimiento del usuario | Sólo una app VPN puede estar preparada a la vez; el usuario puede revocar | Evaluar Sprint 8 |
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
4. El diseño de la notificación persistente / foreground service para modo Supervisado, si el
   proyecto llegara a publicarse — no diseñado todavía porque no es necesario hoy.

## Referencias oficiales consultadas

- Android Developers — `UsageStatsManager`.
- Android Developers — `VpnService`.
- Android Developers — Geofencing.
- Android Developers — `NotificationListenerService`.
- Android Developers — `MediaProjectionManager` y cambios de comportamiento de Android 14+.
- Android Developers — foreground service types y restricciones de background.
- Android Developers — Package visibility (`<queries>` y `QUERY_ALL_PACKAGES`), Sprint 7.
- Play Console Help — política de `QUERY_ALL_PACKAGES` y formulario de declaración de permisos, Sprint 7.
- Play Protect — categorías de Potentially Harmful Apps (Stalkerware/Commercial Spyware), Sprint 7.
- Play Console Help — Prominent disclosure and consent, Sprint 7.

La matriz debe revisarse nuevamente en el sprint que implemente cada capacidad porque las políticas y restricciones de Android pueden cambiar.

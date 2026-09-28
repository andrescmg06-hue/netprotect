# Inventario de comportamiento actual: app Android NetProtect

Lista de comprobación para el Sprint 51: verificar que el rediseño no perdió ninguna funcionalidad.
Describe lo que el código hace HOY (rama `sprint-31-design-system`), no lo que debería hacer.
Fecha del inventario: 2026-09-28.

Archivos fuente leídos completos (ruta base `mobile/app/src/main/java/com/netprotect/app/`):

| Archivo | Líneas |
|---|---|
| `feature/home/HomeScreen.kt` | 338 |
| `feature/tutor/TutorScreen.kt` | 963 |
| `feature/supervised/SupervisedScreen.kt` | 587 |
| `feature/supervised/BlockScreenActivity.kt` | 138 |
| `MainActivity.kt` | 20 |

Consultados solo para nombrar endpoint/comportamiento: `core/network/*Client.kt` (rutas HTTP),
`core/auth/AuthRepository.kt` (signIn/restoreSession/signOut), `core/network/HttpJsonClient.kt`
(formato de errores). Los servicios (`RuleEnforcementService`, `LocationReportingService`,
`ScreenShareService`, `SyncWorker`) NO se leyeron; solo se cita lo que estas pantallas les piden.

Convenciones: `H:` = HomeScreen.kt, `T:` = TutorScreen.kt, `S:` = SupervisedScreen.kt,
`B:` = BlockScreenActivity.kt, `M:` = MainActivity.kt. Formato de cita: `archivo:línea`.
Colores, tamaños y espaciados no se inventarían (no se listan: no son comportamiento).

Formato de errores de red (HttpJsonClient.kt:73-77): si el HTTP no es 2xx, el mensaje mostrado es
el campo `detail` del JSON de respuesta; si no hay `detail`, es `"HTTP <código>"`. Timeout de lectura
5 s (HttpJsonClient.kt:60). Por eso, los textos "No se pudo..." de abajo solo aparecen cuando la
excepción no trae mensaje (`exception.message ?: "..."`); normalmente se ve el `detail` del backend
o `HTTP nnn`.

---

## 0. Punto de entrada

- [ ] M-01 `MainActivity` (M:10-20): `enableEdgeToEdge()` (M:13), `setContent { MaterialTheme { HomeScreen() } }` (M:14-18). Única Activity de UI principal; no maneja rotación ni argumentos en este archivo.

---

## 1. Home (router de estados) — `HomeScreen.kt`

Estados: `Loading`, `SignedOut(error)`, `SelectingRole(user, error)`, `InTutorMode(user)`, `InSupervisedMode(user)` (H:45-51). Estado inicial `Loading` (H:79).

### 1.1 Cargando

- [ ] H-01 Estado visible: texto `Cargando…` (H:182). Se muestra desde el inicio hasta que `restoreSession()` termina (H:128-130). Sin botones.
- [ ] H-02 Disparo al abrir: `authRepository.restoreSession()` (H:129) -> lee refresh token guardado; si existe llama `POST /api/v1/auth/refresh` y luego `GET /api/v1/auth/me` (AuthRepository.kt:51-63; AuthClient.kt:31,45). Cualquier excepción se traga: borra token y devuelve null (AuthRepository.kt:58-61) -> cae a `SignedOut()` sin mensaje.
- [ ] H-03 Tras restaurar sesión: si `RolePreference.read(context)` es `TUTOR` va a `InTutorMode`, si `SUPERVISADO` a `InSupervisedMode`, si no a `SelectingRole` (H:82-87).

### 1.2 Sin sesión (SignedOut)

- [ ] H-04 Texto fijo `NETPROTECT` (H:193).
- [ ] H-05 Título `Control parental` (H:200).
- [ ] H-06 Texto `Inicia sesión con tu cuenta de Google para continuar como tutor o como dispositivo supervisado.` (H:207-208).
- [ ] H-07 Botón `Iniciar sesión con Google` (H:231). Acción: `signIn()` (H:95-105) -> `authRepository.signIn(context)` (Credential Manager de Google, `setFilterByAuthorizedAccounts(false)`, AuthRepository.kt:25-32) -> `POST /api/v1/auth/google` con `id_token` (AuthClient.kt:28) -> guarda refresh token -> `GET /api/v1/auth/me`. Sin indicador de carga ni botón deshabilitado mientras dura (el estado no cambia hasta terminar).
- [ ] H-08 Error visible (texto rojo sobre el botón): `exception.message ?: "No se pudo iniciar sesión"` (H:102, mostrado en H:220-223). Caso especial: `GetCredentialCancellationException` (usuario cierra el selector) -> vuelve a `SignedOut()` sin mensaje (H:99-100). Error `"Credencial inesperada del selector de cuentas de Google"` si la credencial no es de tipo Google ID token (AuthRepository.kt:41).
- [ ] H-09 Línea de estado del servidor (bajo la tarjeta, H:236), tres variantes:
  - Comprobando: `Comprobando conexión con el servidor…` (H:244).
  - Error: `Servidor no disponible: <mensaje>` donde mensaje = `exception.message ?: "No fue posible contactar la API"` (H:140, H:249).
  - Listo: `Servidor: <BACKEND> · BD: <DATABASE> · Redis: <REDIS>` en mayúsculas (H:254-256).
  - Llamada: `healthClient.check()` (H:138) -> `GET <baseUrl>/api/v1/health/ready` (InfrastructureHealthClient.kt:19). Se dispara cada vez que `state` pasa a `SignedOut` (H:135-143). No se reinicia a "Comprobando" en comprobaciones posteriores (solo el valor inicial es `Checking`, H:80).

### 1.3 Selección de rol (SelectingRole)

- [ ] H-10 Saludo `Hola, <displayName o email>` (H:276).
- [ ] H-11 Pregunta `¿Cómo vas a usar este dispositivo?` (H:283).
- [ ] H-12 Tarjeta `Soy tutor` / `Superviso otros dispositivos desde este teléfono o tablet.` (H:290-291) con botón `Elegir` (H:334). Acción: `selectRole(user, "TUTOR")` (H:156).
- [ ] H-13 Tarjeta `Este es el dispositivo supervisado` / `Este teléfono es el que un tutor va a supervisar.` (H:296-297) con botón `Elegir` (H:334). Acción: `selectRole(user, "SUPERVISADO")` (H:157).
- [ ] H-14 Llamada de `selectRole` (H:107-121): `roleClient.selectRole(token, code)` -> `POST /api/v1/users/me/roles` con `role_code` (RoleClient.kt:8-10); si OK guarda `RolePreference.write` y entra al modo. Sin indicador de carga.
- [ ] H-15 Error visible bajo las tarjetas: `exception.message ?: "No se pudo guardar el modo"` (H:118, mostrado H:301-304). Si `authRepository.accessToken` es null, vuelve a `SignedOut()` sin mensaje (H:108-111).
- [ ] H-16 Botón de texto `Cerrar sesión` (H:309) -> `signOut()` (H:89-93): `authRepository.signOut()` (borra token local y llama `POST /api/v1/auth/logout` con `runCatching`, AuthRepository.kt:65-72) + `RolePreference.clear` -> `SignedOut()`.

### 1.4 Entrada a modos

- [ ] H-17 `InTutorMode` -> `TutorScreen(baseUrl, accessToken = authRepository.accessToken.orEmpty(), onSignOut, onSwitchMode)` (H:160-165).
- [ ] H-18 `InSupervisedMode` -> `SupervisedScreen(...)` mismos parámetros (H:166-171).
- [ ] H-19 `switchMode` (H:123-126): `RolePreference.clear` y `SelectingRole(user)`. No llama al backend (no verificado que el backend se entere del cambio; en este archivo no hay llamada).

---

## 2. Modo Tutor — `TutorScreen.kt`

Carga inicial: `LaunchedEffect(Unit) { reloadDevices() }` (T:294). Cabecera: texto `Modo Tutor` (T:306) y botón `Cambiar de modo` (T:311, llama `onSwitchMode` sin confirmación).

### 2.1 Vinculación (tarjeta superior, T:315-369)

- [ ] T-01 Estado sin código: texto `Vincula un dispositivo nuevo generando un código temporal.` (T:323) y botón `Generar código de vinculación` (T:338). Llamada: `pairingClient.generateCode` (T:330) -> `POST /api/v1/pairing/codes` (PairingClient.kt:24).
- [ ] T-02 Error al generar: `exception.message ?: "No se pudo generar el código"` (T:332, mostrado T:364-367 en rojo).
- [ ] T-03 Estado con código: `Código de vinculación` (T:341), el código en tamaño grande (T:343), `Válido por <expiresInSeconds/60> minutos. Uso único.` (T:346). Sin cuenta regresiva ni limpieza automática al expirar (no hay temporizador en el archivo).
- [ ] T-04 Botón `Revocar` (T:357): `pairingClient.revokeCurrentCode` (T:354) -> `DELETE /api/v1/pairing/codes/current` (PairingClient.kt:32). Sin confirmación. Error silenciado con `runCatching` (T:354); la UI borra el código igual (T:355).
- [ ] T-05 Botón `¿Ya se vinculó? Actualizar lista` (T:360) -> `reloadDevices()`.

### 2.2 Mi actividad (auditoría) (T:376-384)

- [ ] T-06 Título `Mi actividad (auditoría)` (T:376) y botón alterno `Ver` / `Ocultar` (T:378). Al abrir: `auditClient.listMyAuditLog` (T:287) -> `GET /api/v1/users/me/audit?limit=50&offset=0` (AuditClient.kt:24). Se recarga cada vez que se abre.
- [ ] T-07 Estados: cargando `Cargando auditoría…` (T:871); error `exception.message ?: "No se pudo cargar la auditoría"` (T:289, mostrado T:872); vacío `Sin acciones registradas todavía.` (T:875); con datos: filas `<acción>[ · <tipo de recurso>]` + fecha/hora local (T:883-885). Solo lectura, sin filtros ni exportación.

### 2.3 Lista de dispositivos (T:391-457)

- [ ] T-08 Título `Dispositivos vinculados` (T:391) y botón `Actualizar` (T:392) -> `reloadDevices()`: `deviceClient.listDevices` (T:168) -> `GET /api/v1/devices` (DeviceClient.kt:27).
- [ ] T-09 Estados: cargando `Cargando…` (T:397); error `exception.message ?: "No se pudo cargar la lista"` (T:170, mostrado T:398 en rojo, sin botón de reintento propio salvo `Actualizar`); vacío `Todavía no hay dispositivos vinculados.` (T:401); con datos `LazyColumn` de `DeviceRow` (T:403-454).
- [ ] T-10 Fila de dispositivo (T:515-528): nombre en negrita (T:520); línea `<plataforma> · <zona horaria>` o solo `<plataforma>` si no hay zona (T:522); pastilla de estado con el código literal `ONLINE`/`OFFLINE`/`ALERT` u otro (T:955-962; color según valor).
- [ ] T-11 Botón `Renombrar` (T:531): abre edición en línea (T:410-413) con campo `Nombre` (T:506) precargado; botones `Guardar` (T:511) y `Cancelar` (T:512). Guardar: `deviceClient.renameDevice` (T:417) -> `PATCH /api/v1/devices/{id}` con `name` (DeviceClient.kt:59-61); error silenciado con `runCatching` (T:416-418); después cierra edición y recarga lista (T:419-420). Sin validación de nombre vacío en este archivo.
- [ ] T-12 Botón `Desvincular` (T:532, texto rojo): `pairingClient.unlinkDevice` (T:426) -> `DELETE /api/v1/devices/{id}/link` (PairingClient.kt:64). Sin confirmación; error silenciado (`runCatching`, T:426); recarga lista (T:427).
- [ ] T-13 Botón alterno `Ver apps` / `Ocultar apps` (T:534). Llamada `applicationsClient.getApplications` (T:183) -> `GET /api/v1/devices/{id}/applications` (ApplicationsClient.kt:65). Estados: `Cargando apps…` (T:903); error `exception.message ?: "No se pudo cargar la lista de apps"` (T:185); vacío `Todavía no se sincronizó ninguna app desde este dispositivo.` (T:908); datos: ordenadas por uso descendente (T:913), fila con nombre de app, `Desinstalada` si tiene `uninstalledAt` (T:933) y duración (`<h> h <m> min`, `<m> min`, `< 1 min`, T:944-951) o `Sin datos de uso` (T:937).
- [ ] T-14 Botón alterno `Ver ubicación` / `Ocultar ubicación` (T:537). Llamada `locationClient.getLatestLocation` (T:199) -> `GET /api/v1/devices/{id}/location/latest` (LocationClient.kt:41). Estados: `Cargando ubicación…` (T:591); error `exception.message ?: "No se pudo cargar la ubicación"` (T:201); vacío `Todavía no hay ubicación reciente de este dispositivo.` (T:597); datos: `Lat <5 decimales>, Lng <5 decimales> (±<n> m)` (T:604-605) y `Capturada: <fecha local>` (T:610).
- [ ] T-15 Botón `Abrir en mapa` (T:621): `startActivity(Intent.ACTION_VIEW, geo:lat,lng?q=lat,lng)` (T:617-618). Error (no hay app de mapas) silenciado con `runCatching` (T:618).
- [ ] T-16 Botón alterno `Ver geocercas` / `Ocultar geocercas` (T:540). Llamadas: `listGeofences` (T:216) -> `GET /api/v1/devices/{id}/geofences` (GeofenceClient.kt:33) y `listGeofenceEvents` (T:217) -> `GET /api/v1/devices/{id}/geofences/events` (GeofenceClient.kt:38). Estados: `Cargando geocercas…` (T:636); error `exception.message ?: "No se pudieron cargar las geocercas"` (T:220); vacío `Todavía no hay geocercas. Créalas desde el panel web.` (T:642); datos `<nombre> · radio <n> m` (T:649). Subsección `Historial de entradas/salidas` (T:656): vacío `Todavía no se detectó ninguna entrada o salida.` (T:659); filas `Entró a <geocerca>` / `Salió de <geocerca>` + fecha (T:677-679). Solo lectura.
- [ ] T-17 Botón alterno `Ver historial` / `Ocultar historial` (T:543). Llamada `historyClient.listHistory` (T:234) -> `GET /api/v1/devices/{id}/history` (HistoryClient.kt:26). Estados: `Cargando historial…` (T:690); error `exception.message ?: "No se pudo cargar el historial"` (T:236); vacío `Todavía no hay eventos registrados para este dispositivo.` (T:695); filas: `Bloqueo (<ruleTypeApplied>) de <packageName>` para `APP_RULE` o `Entró a`/`Salió de <geocerca>` (T:714-719) + fecha.
- [ ] T-18 Botón alterno `Ver estadísticas` / `Ocultar estadísticas` (T:546). Al abrir usa el periodo previo del dispositivo o `today` (T:259). Selector de periodo con botones `Hoy`, `7 días`, `30 días` (T:725); el seleccionado queda deshabilitado (T:748). Llamada `statisticsClient.getStatistics` (T:246) -> `GET /api/v1/devices/{id}/statistics?period=today|7d|30d` (StatisticsClient.kt:52). Estados: `Cargando estadísticas…` (T:755); error `exception.message ?: "No se pudieron cargar las estadísticas"` (T:248).
- [ ] T-19 Contenido de estadísticas (T:759-808): `Apps más usadas` (vacío `Sin datos de uso.`, filas nombre + duración); `Bloqueos` (vacío `Ninguno en este periodo.`, filas tipo de regla + conteo); `Cumplimiento de límites diarios` (vacío `Sin reglas de límite diario.`, filas `<paquete o categoría o ?>` + `<n>% (<cumplidos>/<evaluados> días)` o `sin datos (…)`) (T:761,776,791,795-803).
- [ ] T-20 Botón alterno `Ver alertas` / `Ocultar alertas` (T:549). Llamada `alertsClient.listAlerts` (T:271) -> `GET /api/v1/devices/{id}/alerts` (AlertsClient.kt:27). Estados: `Cargando alertas…` (T:836); error `exception.message ?: "No se pudieron cargar las alertas"` (T:273); vacío `Sin alertas para este dispositivo.` (T:840); filas `[<nivel>] <etiqueta>[ (x<n>)]` + fecha (T:848-854).
- [ ] T-21 Etiquetas de alerta literales (T:814-827): `APP_BLOCKED` "Se bloqueó <paquete>"; `APP_LIMIT_REACHED` "Se alcanzó el límite de tiempo de <paquete>"; `GEOFENCE_EXIT` "Salió de <geocerca>"; `GEOFENCE_ENTER` "Entró a <geocerca>"; `PERMISSION_REVOKED` "Sin permiso de acceso a uso: no puede aplicar reglas"; `SERVICE_INACTIVE` "El servicio de control de apps no está en ejecución"; `HEARTBEAT_SILENCE` "Dejó de reportarse durante un periodo anormalmente largo"; `CLOCK_TAMPERING` "La hora del dispositivo no coincide con la del servidor"; `UNINSTALL_ATTEMPT` "Se intentó desactivar la protección contra desinstalación"; otro tipo -> el código crudo.
- [ ] T-22 Cada sección expandible es independiente por dispositivo; solo una sección de cada tipo abierta a la vez (una variable `expanded*DeviceId` por tipo, T:150-163). Se carga al abrir; volver a cerrar y abrir vuelve a llamar (no hay caché).
- [ ] T-23 Botón `Cerrar sesión` (T:461) al pie: `scope.launch { onSignOut() }` -> mismo `signOut()` de Home (H:89-93). Sin confirmación.

---

## 3. Modo Supervisado — `SupervisedScreen.kt`

Encabezado fijo `Modo Supervisado` (S:299). Estados de vínculo: `CheckingLink`, `EnteringCode(error)`, `Linked(tutorLabel)` (S:61-65).

### 3.1 Comprobación de vínculo

- [ ] S-01 Estado `Comprobando vínculo…` (S:314). Al componer: `deviceClient.getMyDevice(token)` (S:157) -> `GET /api/v1/devices/me` (DeviceClient.kt:37); 404 devuelve null (DeviceClient.kt:53-55).
- [ ] S-02 Resultado: con dispositivo y tutor -> guarda `LinkedDeviceStore.write` y pasa a `Linked` (S:163-165); sin dispositivo o sin tutores -> `LinkedDeviceStore.clear()` y `EnteringCode()` (S:166-169); si falla la red/otra excepción -> usa el caché local si existe (`Linked`), si no `EnteringCode()` sin mensaje (S:170-174).

### 3.2 Ingreso de código (EnteringCode)

- [ ] S-03 Texto `Introduce el código de 6 dígitos que te dio tu tutor.` (S:319).
- [ ] S-04 Campo `Código` (S:326): acepta solo dígitos, máximo 6 (S:325).
- [ ] S-05 Botón `Vincular dispositivo` (S:359), habilitado solo con 6 dígitos (S:356). Llamada `pairingClient.redeem` (S:338) -> `POST /api/v1/pairing/redeem` con `code`, `device_instance_id`, `device_name` (`<fabricante> <modelo>`), `platform: ANDROID`, `os_version`, `app_version` (S:338-345; PairingClient.kt:44-51). Al éxito: guarda `LinkedDeviceStore` y pasa a `Linked` (S:346-348). Sin indicador de carga ni bloqueo durante la llamada.
- [ ] S-06 Error visible bajo el campo: `exception.message ?: "No se pudo vincular"` (S:351, mostrado S:329-332).

### 3.3 Tarjeta "Dispositivo vinculado" (Linked)

- [ ] S-07 `Dispositivo vinculado` (S:364), `Supervisado por <etiqueta del tutor>` (S:366; varios tutores se unen con ", ", DeviceClient.kt:42) y `Este dispositivo reporta su estado cada minuto mientras la app esté abierta.` (S:369). Sin botones.

### 3.4 Tarjeta de solicitud de pantalla (solo si Linked y `screenShareRequested`, S:382)

- [ ] S-08 Título `Tu tutor quiere ver esta pantalla` (S:391) y texto `Si aceptas, Android te pedirá confirmarlo otra vez y verás un aviso permanente mientras dure la transmisión. Puedes detenerla en cualquier momento desde ese aviso.` (S:397-399).
- [ ] S-09 Botón `Aceptar` (S:416): envía por el WebSocket `{"type":"screen_share_consent","granted":true}` (S:407-411) y lanza el diálogo del sistema `ScreenCapture.consentIntent` (S:412). Si el resultado es OK con datos y hay `deviceId`: `ScreenShareService.start(context, API_BASE_URL, accessToken, deviceId, data)` (S:103-104). Si se cancela: oculta la tarjeta y envía `{"type":"screen_share_stop","reason":"projection_cancelled"}` (S:100,108-110).
- [ ] S-10 Botón `Ahora no` (S:429): oculta la tarjeta y envía `{"type":"screen_share_consent","granted":false}` (S:421-425).
- [ ] S-11 La tarjeta aparece al recibir evento `screen_share_request` y desaparece con `screen_share_stop` (S:269-270). WebSocket: `RealtimeClient.connect(deviceId, accessToken)` -> `/api/v1/devices/{id}/ws` (RealtimeClient.kt:39). Abierto solo mientras la pantalla está compuesta y `Linked` (S:260-277).

### 3.5 Tarjetas de permisos (solo si Linked; cada una desaparece al concederse)

- [ ] S-12 "Acceso a uso de apps" (si `!hasUsageAccess`, S:435): título `Acceso a uso de apps` (S:444); texto `Para que tu tutor vea qué apps usas y cuánto tiempo, actívalo en Ajustes → Acceso a datos de uso. Android exige que este permiso se conceda ahí, no aquí.` (S:450-452); botón `Abrir Ajustes` (S:462) -> `UsageAccessPermission.openSettings` (S:459); botón `Ya lo activé, verificar de nuevo` (S:466) -> relee `UsageAccessPermission.isGranted` (S:465).
- [ ] S-13 "Ubicación aproximada" (si `!hasLocationPermission`, S:472): título `Ubicación aproximada` (S:481); texto `Para que tu tutor vea en qué zona está este dispositivo, permite el acceso a la ubicación aproximada. No se usa la ubicación precisa, y sólo se comparte mientras esta app siga activa en segundo plano.` (S:487-489); botón `Permitir ubicación aproximada` (S:501) -> pide `ACCESS_COARSE_LOCATION` (S:497). Resultado actualiza `hasLocationPermission` (S:135).
- [ ] S-14 "Mostrar sobre otras apps" (si `!hasOverlayPermission`, S:507): título `Mostrar sobre otras apps` (S:516); texto `Sin este permiso, la pantalla de bloqueo no puede cubrir una app mientras el teléfono está desbloqueado y en uso — Android sólo la muestra automáticamente cuando el teléfono está bloqueado. Con este permiso, el bloqueo aparece de inmediato en cualquier momento.` (S:522-525); botón `Permitir` (S:535) -> `OverlayPermission.requestIntent` (S:532); botón `Ya lo activé, verificar de nuevo` (S:539) -> relee `OverlayPermission.isGranted` (S:538).
- [ ] S-15 "Protección contra desinstalación" (si `!hasDeviceAdmin`, S:545): título `Protección contra desinstalación` (S:554); texto `Si lo activas, Android pedirá desactivar esta protección antes de desinstalar NetProtect, y tu tutor recibirá un aviso cuando eso ocurra. No permite borrar el dispositivo ni cambiar tu contraseña, y puedes quitarlo cuando quieras.` (S:560-563); botón `Activar protección` (S:575) -> `DeviceAdminPermission.requestIntent` (S:571); al volver relee `DeviceAdminPermission.isActive` (S:119).

### 3.6 Pie

- [ ] S-16 Botón `Cambiar de modo` (S:582) -> `onSwitchMode` (limpia `RolePreference`, H:123-126). Sin confirmación.
- [ ] S-17 Botón `Cerrar sesión` (S:584) -> `onSignOut()` (H:89-93). Sin confirmación. No borra `LinkedDeviceStore` (Home.signOut no lo llama, H:89-93).

---

## 4. Pantalla de bloqueo — `BlockScreenActivity.kt`

Lanzada por `RuleEnforcementService` con `FLAG_ACTIVITY_NEW_TASK` según el docstring (B:28-29); `BlockScreenContent` también se reutiliza en `BlockOverlayController` (B:77-78, no leído).

- [ ] B-01 Extras del Intent: `package_name` (obligatorio) y `reason` (B:40-41). Sin `package_name` la actividad hace `finish()` sin mostrar nada (B:46-49).
- [ ] B-02 Razón: se mapea desde el valor de red (`wireValue`); si es nulo o desconocido se usa `BlockReason.BLOCK` (B:50-52).
- [ ] B-03 Nombre de la app: `PackageManager.getApplicationLabel`; si falla muestra el nombre de paquete (B:70-74).
- [ ] B-04 Texto fijo `APP BLOQUEADA` (B:100).
- [ ] B-05 Nombre de la app bloqueada (B:107).
- [ ] B-06 Texto por razón (B:80-90):
  - `BLOCK`: `Tu tutor bloqueó esta app.`
  - `DAILY_LIMIT`: `Ya usaste el tiempo diario permitido para esta app.`
  - `WEEKLY_LIMIT`: `Ya usaste el tiempo semanal permitido para esta app.`
  - `SCHEDULE`: `Esta app está bloqueada en este horario.`
  - `CATEGORY`: `Tu tutor bloqueó la categoría a la que pertenece esta app.`
  - `SCHOOL_MODE`: `Es horario escolar y esta app no está aprobada para este momento.`
  - `DEFAULT_POLICY`: `Este dispositivo sólo permite las apps que tu tutor aprobó, y ésta no está aprobada.`
- [ ] B-07 Aviso fijo en tarjeta: `Este bloqueo cubre la pantalla, pero no puede impedir que la app siga abierta de fondo ni que alguien con conocimientos técnicos lo evada (por ejemplo, revocando el acceso a uso en Ajustes). NetProtect no tiene privilegios de administrador de dispositivo.` (B:118-121).
- [ ] B-08 Botón `Ir al inicio` (B:134): `startActivity(ACTION_MAIN + CATEGORY_HOME)` (B:61-63). No llama a `finish()` en este archivo.
- [ ] B-09 No hay manejo de botón Atrás (`BackHandler`) en este archivo. El docstring dice que Atrás/Recientes pueden revelar la app y el siguiente sondeo vuelve a mostrar la pantalla (B:31-35); esa detección vive en `RuleEnforcementService` (no verificado en este inventario).

---

## 5. Comportamientos no visibles pero existentes

Solo lo observado en los cinco archivos.

### 5.1 Servicios y trabajo en segundo plano que arranca SupervisedScreen

- [ ] N-01 Latido cada 60 s mientras `Linked` y la pantalla está compuesta (S:58,181-201): `deviceClient.sendHeartbeat` -> `POST /api/v1/devices/{id}/heartbeat` con versión de SO, versión de app, zona horaria, `usage_access_granted` (leído fresco), `service_active` (`EnforcementLiveness.isRecentlyActive`) y `device_time` (S:186-197; DeviceClient.kt:90). Primer envío inmediato.
- [ ] N-02 Sincronización de apps cada 5 min si `Linked` y `hasUsageAccess` (S:59,207-223): `applicationsClient.syncApplications` -> `POST /api/v1/devices/{id}/applications/sync` con apps instaladas y uso de hoy (S:213-219; ApplicationsClient.kt:55). Primer envío inmediato.
- [ ] N-03 `RuleEnforcementService.start(...)` cuando `Linked` + `hasUsageAccess` + hay `deviceId`; `RuleEnforcementService.stop` al salir de composición o cambiar dependencias (S:230-237). Endpoints que usa (no leído el servicio): `GET /api/v1/devices/{id}/rules/active`, `POST /api/v1/devices/{id}/rule-events` (RuleEnforcementClient.kt:34,106).
- [ ] N-04 `LocationReportingService.start(...)` cuando `Linked` + `hasLocationPermission` + `deviceId`; `stop` al salir (S:244-251). Endpoint del cliente: `POST /api/v1/devices/{id}/location` (LocationClient.kt:33); no verificado que el servicio sea quien lo llama.
- [ ] N-05 `SyncWorker.schedule(context, API_BASE_URL, deviceId)` cuando `Linked` + `deviceId`; `SyncWorker.cancel` al salir (S:284-291). Nota: `onDispose` cancela el trabajo también al cerrar sesión o cambiar de modo.
- [ ] N-06 WebSocket de solicitud de pantalla (S:260-277), ver S-11. `ScreenShareService.start` solo tras aceptar y aprobar el diálogo del sistema (S:104). Su `stop` no se llama desde estos archivos.
- [ ] N-07 `DeviceIdentity.getOrCreate(context)` se usa como `device_instance_id` (S:79,340).
- [ ] N-08 Tras cerrar sesión o cambiar de modo, la pantalla Supervisada sale de composición y los `onDispose` paran `RuleEnforcementService`, `LocationReportingService`, `SyncWorker` y cierran el WebSocket (S:236,250,273-276,290). Nada de esto ocurre al cambiar de modo desde Tutor (Tutor no arranca servicios).

### 5.2 Permisos que se piden

- [ ] N-09 `POST_NOTIFICATIONS` (Android 13+): se pide automáticamente cada vez que `state` pasa a `Linked` (S:144-148); el resultado se ignora (S:141-143).
- [ ] N-10 `ACCESS_COARSE_LOCATION`: solo tras pulsar `Permitir ubicación aproximada` (S:497).
- [ ] N-11 Uso de apps (`PACKAGE_USAGE_STATS`), Superposición (`SYSTEM_ALERT_WINDOW`), Administrador de dispositivo: se conceden en pantallas del sistema abiertas desde las tarjetas (S:459,532,571).
- [ ] N-12 Captura de pantalla (`MediaProjection`): diálogo del sistema en cada solicitud (S:97-112,412).

### 5.3 Ciclo de vida

- [ ] N-13 Al volver a primer plano: en estos archivos no hay observador de ciclo de vida (`ON_RESUME`). `hasUsageAccess`, `hasLocationPermission`, `hasDeviceAdmin`, `hasOverlayPermission` se leen una vez al componer (S:83-86) y solo se actualizan por el resultado de un launcher o por los botones `Ya lo activé, verificar de nuevo` (S:465,538). Uso de apps y superposición: el launcher de superposición sí relee al volver (S:127); el de uso de apps no tiene launcher.
- [ ] N-14 Rotación / recreación de Activity: todo el estado de UI usa `remember` (no `rememberSaveable`) en H, T y S, por lo que se pierde si la Activity se recrea. Si la Activity se recrea al rotar depende del manifiesto: no verificado. Si se recrea, los `onDispose` de S paran y reinician los servicios.
- [ ] N-15 `accessToken` se pasa como parámetro y se captura al componer (H:162,168; T:127-129; S:67-70). En estos archivos no hay renovación del token; el token que reciben `RuleEnforcementService`, `LocationReportingService` y `ScreenShareService` es ese mismo (S:104,234,248). Según las notas del proyecto, los servicios se renuevan con `BackgroundTokenRefresher` (no verificado leyéndolos aquí).
- [ ] N-16 Preferencia de rol: `RolePreference` se lee tras iniciar/restaurar sesión (H:83), se escribe al elegir rol (H:115) y se borra al cerrar sesión (H:91) y al cambiar de modo (H:124).
- [ ] N-17 Cierre de sesión (H:89-93): `authRepository.signOut()` borra el refresh token local y hace `POST /api/v1/auth/logout` (con `runCatching`), luego borra `RolePreference` y muestra `SignedOut`. No llama a `LinkedDeviceStore.clear()` ni desvincula en servidor.
- [ ] N-18 Diagnóstico del servidor (H:135-143) corre solo mientras el estado es `SignedOut`; no hay reintento periódico.
- [ ] N-19 Modo Tutor: no arranca servicios ni pide permisos (no hay ninguno en T). Los datos se cargan solo al abrir cada sección o pulsar `Actualizar`; no hay sondeo automático.
- [ ] N-20 Fechas del Tutor: `formatCapturedAt` convierte ISO-8601 a zona horaria del sistema en estilo `MEDIUM`; si falla, muestra el texto original (T:894-898).

---

## 6. Cosas que hoy NO tienen confirmación o manejo de error

Solo hechos observados en los archivos leídos; no incluye juicio ni propuesta.

### 6.1 Acciones sin confirmación

- [ ] C-01 `Desvincular` dispositivo se ejecuta al primer toque (T:532 -> T:424-429).
- [ ] C-02 `Revocar` código de vinculación al primer toque (T:357).
- [ ] C-03 `Cerrar sesión` sin confirmación en Selección de rol (H:309), Tutor (T:460) y Supervisado (S:583).
- [ ] C-04 `Cambiar de modo` sin confirmación en Tutor (T:311) y Supervisado (S:582).
- [ ] C-05 `Vincular dispositivo` (S:359) y elección de rol `Elegir` (H:334) sin paso de confirmación.
- [ ] C-06 `Renombrar` -> `Guardar` sin validación de texto vacío en este archivo (T:416-418).

### 6.2 Errores silenciados o sin mensaje al usuario

- [ ] C-07 `unlinkDevice` con `runCatching`, sin mensaje si falla (T:426).
- [ ] C-08 `renameDevice` con `runCatching`, sin mensaje si falla (T:416-418); la edición se cierra igual (T:419).
- [ ] C-09 `revokeCurrentCode` con `runCatching`; el código desaparece de la UI aunque falle (T:354-355).
- [ ] C-10 Abrir mapa (`startActivity` geo) con `runCatching`, sin mensaje si no hay app (T:618).
- [ ] C-11 Heartbeat con `runCatching` en bucle, sin mensaje ni contador de fallos (S:185-198).
- [ ] C-12 Sincronización de apps con `runCatching` en bucle, sin mensaje (S:212-220).
- [ ] C-13 `getMyDevice` fallida: cae en silencio al caché o a `EnteringCode()` sin mostrar el error (S:170-174).
- [ ] C-14 `restoreSession` fallida: borra sesión y muestra `SignedOut()` sin mensaje (H:129; AuthRepository.kt:58-61).
- [ ] C-15 Cancelar el selector de Google: vuelve a `SignedOut()` sin mensaje (H:99-100). `selectRole` sin token: `SignedOut()` sin mensaje (H:108-111).
- [ ] C-16 Logout en servidor con `runCatching` (AuthRepository.kt:70); local se borra igual.
- [ ] C-17 Resultado de `POST_NOTIFICATIONS` ignorado (S:141-143). Permiso de ubicación denegado: solo actualiza el booleano, sin mensaje (S:135). Diálogo de Administrador de dispositivo o Superposición rechazado: solo relee estado, sin mensaje (S:119,127).
- [ ] C-18 Si `screenShareSignaling` es null al pulsar `Aceptar`/`Ahora no`, el `send` se omite (`?.`) sin aviso (S:407,422,108).
- [ ] C-19 Si al terminar el diálogo de captura `deviceId` es null o `data` es null, se trata como cancelación y envía `screen_share_stop` (S:103-110).
- [ ] C-20 `resolveAppLabel` falla -> muestra el nombre de paquete sin aviso (B:70-74). Falta `package_name` -> la Activity se cierra sin mensaje (B:46-49).
- [ ] C-21 `formatCapturedAt` con `runCatching` -> muestra el texto crudo (T:894-898).

### 6.3 Acciones sin indicador de progreso ni bloqueo de doble toque

- [ ] C-22 `Iniciar sesión con Google` (H:231), `Elegir` (H:334), `Generar código de vinculación` (T:338), `Vincular dispositivo` (S:359), `Guardar` (T:511), `Desvincular` (T:532): el botón sigue activo mientras la llamada está en curso (sin estado "en curso" en el código; solo `Vincular dispositivo` se deshabilita, y por longitud del código).

### 6.4 Otros hechos

- [ ] C-23 El código de vinculación no expira en pantalla (sin temporizador): la UI conserva el código mostrado hasta `Revocar` o salir (T:321-346).
- [ ] C-24 Los errores de sección del Tutor (apps, ubicación, geocercas, historial, estadísticas, alertas, auditoría) no tienen botón de reintento; se reintenta cerrando y abriendo la sección (T:174-292, excepto estadísticas que también se reintenta cambiando de periodo, T:748).
- [ ] C-25 En el diagnóstico del servidor, tras la primera comprobación el texto `Comprobando conexión con el servidor…` no vuelve a aparecer en comprobaciones posteriores (H:80,135-143).

---

## 7. Marcado como "no verificado"

- Si la Activity se recrea al rotar (depende de `AndroidManifest.xml`, no leído): N-14.
- Que el backend se entere de `Cambiar de modo`: H-19 (no hay llamada en el archivo).
- Endpoints/servicios internos (`RuleEnforcementService`, `LocationReportingService`, `ScreenShareService`, `SyncWorker`, `BlockOverlayController`): no leídos; solo se cita lo que los archivos de pantalla les piden. N-03, N-04, N-06, N-15, B-09.
- Que `LocationReportingService` sea quien llama a `POST .../location`: N-04.
- Que la renovación de token en segundo plano cubra a los servicios arrancados desde estas pantallas: N-15.
- Comportamiento exacto de `UsageAccessPermission`, `OverlayPermission`, `DeviceAdminPermission`, `LocationPermission`, `LinkedDeviceStore`, `RolePreference`, `DeviceIdentity`: solo se usó su nombre y contrato visible.

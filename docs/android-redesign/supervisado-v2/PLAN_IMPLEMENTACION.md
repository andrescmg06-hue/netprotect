# Plan de implementación · rediseño v2 del modo supervisado

Estado: **aprobado por el dueño el 12/10/2026**, con las 7 decisiones de §7 aceptadas tal como se recomiendan. Primer
paso: cerrar S51 reducido; después, S61.

Fuentes: `README.md` y `01`–`05` de esta carpeta, las 16 láminas, `docs/android-redesign/CONTINUAR.md`,
`DECISIONES.md`, `docs/sprint-51.md` y el código de la rama `sprint-51-cierre` (worktree `C:/Users/andre/np-s51`,
HEAD `d0e3b2b`). Rutas de código relativas a `mobile/app/src/main/java/com/netprotect/app/` (`A/`) y `backend/app/`
(`B/`). Toda referencia `archivo:línea` se comprobó leyendo el código; nada se ejecutó.

---

## 0. Punto de partida real (corrige el paquete)

| El paquete dice | La realidad | Evidencia |
|---|---|---|
| S50 está pendiente | **S50 está hecho y fusionado** en `android-redesign` (PR #12): consentimiento con casilla, Estado de NetProtect, banner de vista remota y reconexión realtime (B-02) | `git log origin/android-redesign`: `cb8d691 Merge pull request #12` |
| S51 está pendiente | **S51 va avanzado** en `sprint-51-cierre` (25 commits por delante de `android-redesign`): T1–T9 hechos, T5 y H-02 cerrados en el Galaxy, T10 parcial y T11 sin empezar. `d0e3b2b` está en local, sin push | `docs/sprint-51.md` (rama `sprint-51-cierre`) |
| Sprints S52–S57 | **S52–S60 ya son del rediseño web** (`sprint-52-rediseno-web-fase-0` … `sprint-60-rediseno-web-cierre`). Este rediseño se numera desde **S61** | `git branch -a` |
| «Cambiar de modo» por validar | Ya existe, y es un **hueco de seguridad**: un menor pasa a modo tutor con dos toques y se detienen todos los servicios sin avisar al tutor (ver P13). Además, el dueño ya decidió **quitar el modo tutor de la app** | `A/feature/home/HomeScreen.kt:127-130` |
| Serif por definir (Playfair como aproximación) | El panel web ya cambió Playfair por **Newsreader**, porque sus trazos finos desaparecían a 1×. La decisión D1 del web sigue abierta | `frontend/src/app/layout.tsx:11-17` (rama `sprint-60-rediseno-web-cierre`) |
| Paleta nueva | La paleta v2 (crema `#F5F3EE`, tinta `#11243D`) **es la del panel web**. La app actual usa otra (cielo `#F5F9FF`, navy `#102B63`, tinta `#1B2B4B`) | `frontend/src/app/globals.css:18,30`; `A/ui/theme/Color.kt:13-41` |

**Base del código para v2:** `sprint-51-cierre`, o `android-redesign` una vez fusionado S51. La copia de OneDrive está
en `chore/entorno-gentle-ai` (`502b0e5`) y **no tiene S50**. Esta carpeta `supervisado-v2/` está sin versionar en esa
rama. Antes del primer sprint hay que llevarla a la rama correcta (con OK del dueño).

---

## 1. Las 16 preguntas técnicas, respondidas

| # | Respuesta | Evidencia | Qué cambia en las láminas |
|---|---|---|---|
| 1 | Hay 3 errores distinguibles, no 4. Sin Internet y servicio caído son el mismo `IOException`. Un 5xx real se ve como «servidor». El chequeo de salud reduce todo fallo a `Unavailable`. No se usa `ConnectivityManager` | `A/core/network/UiError.kt` (`toUiError`); `HomeScreen.kt:132-137` | Lámina 01: o se fusionan «sin Internet» y «servicio no disponible», o se añade `ConnectivityManager` (permiso normal `ACCESS_NETWORK_STATE`, sin diálogo). Ver decisión 5 |
| 2 | No hay inicio silencioso con Google. Se restaura con el refresh token del Keystore (`/auth/refresh` y luego `/auth/me`). Sin token, o con 401 en la renovación, se va al login. Con red caída o 5xx también va al login, siempre con el texto de «sin conexión». La sesión caducada sí existe | `HomeScreen.kt:132-149`; `TokenSession.kt:111-148` | El «Cargando» de la lámina sirve. Hay que corregir que un 5xx se muestre como «sin conexión» |
| 3 | La caducidad del código no llega al teléfono. El TTL es de 180 s y solo lo recibe el tutor. Al canjear: 401 `invalid_or_expired_code`, igual para inválido y vencido (a propósito), 429 `too_many_attempts` y 503 `rate_limit_unavailable` | `B/core/config.py:34`; `B/schemas/pairing.py:14-15,35`; `B/api/v1/endpoints/pairing.py:35-41` | Lámina 02 correcta: sin cuenta atrás y un solo mensaje para inválido o vencido. Falta el texto del 429 |
| 4 | Con el shell compuesto, un latido cada 60 s. En segundo plano, `SyncWorker` cada 15 min o más (WorkManager) | `A/feature/supervised/SupervisedShell.kt:68,237-266`; `SyncWorker.kt:93` | «Cada minuto mientras la app esté abierta» es cierto y cauto. Se mantiene |
| 5 | Ubicación aproximada (`NETWORK_PROVIDER`, solo COARSE) cada 15 min, en un servicio en primer plano. Se detiene si se cierra la app desde recientes o muere el proceso (`START_NOT_STICKY`, sin arranque al encender) | `LocationReportingService.kt:63`; `docs/android/capability-matrix.md` (~380-387) | «Cada unos 15 minutos» es correcto. No prometer que funciona con la app cerrada |
| 6 | Uso de apps: `AppOps`, abre la lista general `ACTION_USAGE_ACCESS_SETTINGS`. Ubicación: diálogo de Android, sin alternativa si se denegó para siempre. Superposición: `canDrawOverlays`, abre la página de la app. Administrador: `isAdminActive`, abre el diálogo de Android. Se vuelve a comprobar en `ON_RESUME` | `SupervisedShell.kt:189-201`; `core/permissions/*` | Lámina 11: los «3 pasos» no son iguales para los 4 permisos. Ubicación y administrador no abren Ajustes, abren un diálogo |
| 7 | Solo avisa: al pedir la desactivación del administrador se envía `UNINSTALL_ATTEMPT` y se crea una alerta CRÍTICA. No impide desinstalar | `NetProtectDeviceAdminReceiver`; `B/services/tamper.py:19-33`; `capability-matrix.md` (~590) | El texto «Avisa a tu tutor si alguien intenta desinstalar NetProtect» es correcto |
| 8 | Sondeo de `UsageStats` cada 3 s y una ventana `TYPE_APPLICATION_OVERLAY`. Sin el permiso, una notificación de pantalla completa (débil con el teléfono en uso). No cierra la app; «Ir al inicio» lleva a HOME. Los límites por fabricante no están documentados | `RuleEnforcementService.kt:70`; `capability-matrix.md:172-190` | «Cubre la pantalla de la app, pero no la cierra» es verdad. Se publica |
| 9 | Solo el panel web lo inicia (rol TUTOR). Los 2 minutos de caducidad existen **solo en el teléfono**. Ni el backend ni la web tienen tiempo límite: el tutor puede quedarse esperando. Sin socket abierto, la solicitud se pierde (la app no usa FCM) | `SupervisedShell.kt:73,361-369`; `B/api/v1/endpoints/realtime.py` (~50) | La cuenta atrás de la lámina 06 se puede mostrar (el plazo es real), contando desde que llega la solicitud |
| 10 | «Continuar» envía `granted=true` y abre el diálogo de captura de Android. «Ahora no» envía `granted=false`. Cancelar el diálogo de Android envía `screen_share_stop`. **«Atrás» y la caducidad no envían nada**: el tutor sigue esperando | `SupervisedShell.kt:135-150,370-376,493-499` | «Atrás no cuenta como aceptar» es verdad. Hueco B5: avisar al tutor (ver decisión 6) |
| 11 | La vista termina con: «Detener» en el teléfono (banner o notificación), «Detener» del tutor o salir del panel, fin de la proyección por el sistema (chip o bloqueo de pantalla) y fallo de ICE. Si solo cae el socket de señalización, la captura no se detiene. Si se mata la app, el tutor ve el fallo con retraso (inferido) | `ScreenShareService.kt:304,353-360` | Las láminas no necesitan cambios. El comportamiento en dispositivos reales queda pendiente de probar |
| 12 | **El repositorio respalda «no graba ni guarda»**. En Android no hay `ImageReader`, `MediaRecorder`, `MediaMuxer` ni escritura de archivos. El backend solo transmite señalización. La web no tiene `MediaRecorder`, `captureStream` ni `toDataURL`. Por TURN, el vídeo pasa cifrado (DTLS-SRTP) sin grabación configurada. No cubre código de WebRTC de terceros ni la infraestructura de producción | grep en `mobile/app/src/main` y `frontend/src`; `infra/coturn/turnserver.conf` | «NetProtect no graba ni guarda la pantalla» puede publicarse. **«ni controla el dispositivo» es falso**: NetProtect bloquea apps. Se quita |
| 13 | `switchMode` borra la preferencia local y vuelve a «Elegir modo», sin llamar al backend, sin confirmación y sin aviso. Se detienen todos los servicios. El tutor solo lo nota porque el dispositivo pasa a OFFLINE a los 300 s. `selectRole` concede cualquier rol | `HomeScreen.kt:90-95,115-130`; `B/api/v1/endpoints/roles.py` | Lámina 10.3 «Cambiar de modo» **se elimina** (decisión 1) |
| 14 | Cerrar sesión borra los tokens y la preferencia de rol, pero no `LinkedDeviceStore` ni desvincula. Si el tutor desvincula, el latido sigue respondiendo 200 y el backend pone `status=UNLINKED` (`B/api/v1/endpoints/devices.py:252`), pero `sendHeartbeat` devuelve `Unit` y no lo lee (`A/core/network/DeviceClient.kt:87`). Solo `checkLink()` lo detecta: al arrancar el shell o con «Comprobar vínculo». Hoy la app vuelve a «Vincular» sin explicar nada | `HomeScreen.kt:97-101`; `SupervisedShell.kt:207-231` | Lámina 10.4 necesita detección (hueco B1) y un estado nuevo, fuera de `Linked` |
| 15 | El teléfono no guarda cuándo envió la última ubicación: `reportLocation` se traga el resultado y el registro solo tiene `locationRunning` | `LocationReportingService.kt:117-126`; `ServiceStatusRegistry.kt` | «Sin confirmar» necesita un `lastLocationReportedAt` local nuevo (hueco B2) |
| 16 | «Conectado» es `!heartbeatFailing`, sin umbral de tiempo, y vale `true` antes del primer latido. Estado de NetProtect usa otro umbral: «Sin confirmar» tras 180 s. El backend marca OFFLINE a los 300 s. Los tutores y el `last_seen_at` del servidor solo se leen en `checkLink` | `LinkedDeviceScreen.kt:157`; `ServiceStatusRegistry.kt` (~61); `B/services/device_status.py`; `SupervisedViews.kt:34` | Regla propuesta en §2.3; umbral en decisión 3 |

Otros hallazgos: no hay gestión de optimización de batería (ni `PowerManager` ni `BOOT_COMPLETED`). De un permiso
quitado en segundo plano **solo el acceso al uso llega al tutor**, con el campo `usage_access_granted` del latido y
una alerta `PERMISSION_REVOKED`. La ubicación, la superposición y el administrador fallan en silencio.

---

## 2. Correcciones a las láminas (mandan sobre `02_PANTALLAS.md`)

### 2.1 Textos falsos o sin respaldo

| Lámina | Texto en la lámina | Problema | Texto propuesto |
|---|---|---|---|
| 05, 10 | «NetProtect no graba la pantalla ni controla el dispositivo.» | Sí controla: bloquea apps | «NetProtect no graba ni guarda lo que se ve en tu pantalla.» |
| 06.3 | «Android no permite quitarlos.» | Desde Android 14 la persona puede descartar la mayoría de avisos de servicios en primer plano; si se niega `POST_NOTIFICATIONS`, no aparecen. Probar en el Galaxy | «Cada aviso aparece mientras su función está activa.» |
| 06.3 | «NetProtect está activo · Reporte y control de apps funcionando…» | Ese aviso es del servicio de control de apps; el reporte no es un servicio | «Control de apps funcionando en este dispositivo.» |
| 13.4 | «Tu tutor puede ver que este permiso está pendiente.» | Solo es cierto para el acceso al uso | Mostrarlo solo para acceso al uso. En los demás: «Activa el permiso para que NetProtect vuelva a funcionar.» |
| 10.1 | «Cambiar de modo» | Se elimina (decisión 1) | — |
| 10.1 | «Solo él puede hacerlo.» | Supone el género del tutor | «Solo tu tutor puede hacerlo.» |
| 01 | Cuatro errores | Hoy solo se distinguen tres | Según la decisión 5: o un único «No pudimos conectar con NetProtect. Revisa tu conexión a Internet e inténtalo de nuevo.», o los cuatro con `ConnectivityManager` |
| 02 | — | Falta el 429 | «Hiciste demasiados intentos. Espera unos minutos y vuelve a intentarlo.» |
| 11 (Ubicación, Administrador) | «Toca “Abrir Ajustes”» | Abren un diálogo de Android, no Ajustes | Ubicación: «Permitir ubicación aproximada». Administrador: «Activar protección». Pasos propios para cada uno |
| 07 | Pistas «Vuelve a abrirla mañana / el lunes» | Ya existen y se verificaron en S49 | Mantener los textos de `BlockPresentation.kt`; el cambio es solo de estilo |

### 2.2 Lo que ya es verdad y se publica

«Cada minuto mientras la app esté abierta», «cada unos 15 minutos» (nunca «en tiempo real»), «cubre la pantalla de la
app, pero no la cierra», «avisa a tu tutor si alguien intenta desinstalar», «no graba ni guarda la pantalla», «Atrás
no cuenta como aceptar», «caduca en 2 min» (desde que llega la solicitud).

### 2.3 Regla del titular de Inicio (lámina 03)

Se calcula con una función pura testeada en JVM, sobre datos que ya existen:

1. Algún permiso pendiente → **«Falta un paso»**, con «Revisar permisos».
2. Si no, sin latido correcto en los últimos *U* segundos, o todavía ninguno → **«Sin reporte reciente»**. Hoy la app
   dice «Conectado» antes del primer latido; eso se corrige.
3. Si no → **«Todo funciona»**.

*U* debe ser **el mismo umbral** en Inicio y en Estado de NetProtect, que hoy usa 180 s. Recomendación: 300 s, lo mismo
que el backend usa para marcar OFFLINE, para que el menor y el tutor vean lo mismo (decisión 3).

---

## 3. Mapa pantalla → archivo → cambio

| Lámina | Archivo hoy | Qué existe | Cambio |
|---|---|---|---|
| 00 Flujo | `A/feature/home/HomeScreen.kt` (`HomeState` :51-57), `SupervisedShell.kt` (`SupervisedState` :75-79), `SupervisedViews.kt:41` (`SupervisedRoute`) | Un solo flujo con `route` en `rememberSaveable` | Sin «Elegir modo»; rutas nuevas `MoreSettings`, `Privacy`, `PermissionDetail(n)`; estados nuevos `Unlinked` y `Welcome` **fuera de** `Linked` |
| 01 Acceso | `home/LoadingScreen.kt:25`, `home/LoginScreen.kt:42` (+ `ServiceStatusLine` :135) | Carga, login, línea de servicio y un solo `error: String?` | Restilo. Error tipado (`UiError`) en vez de texto. 5xx ≠ sin conexión. «Vuelve a entrar» cuando la sesión caducó |
| 02 Vincular | `supervised/link/LinkDeviceScreen.kt:36`, `ui/components/OtpInput.kt:28` | Casi todo: 6 casillas, estados, errores | Restilo y texto del 429. «Cerrar sesión» con confirmación |
| 03 Inicio | `supervised/linked/LinkedDeviceScreen.kt:49` | Nombre, versión, tutores, última comunicación, permisos pendientes, accesos | Panel navy con titular y sello. Regla §2.3. Accesos en rejilla. Filas «Privacidad» y «Más ajustes». Fuera «Cambiar de modo» y «Cerrar sesión» |
| 04 Permisos | `supervised/permissions/PermissionsScreen.kt:28`, `ui/components/PermissionCard.kt:25` | 4 tarjetas con acción y «verificar de nuevo» | `NpRail`: recorrido vertical con el siguiente paso resaltado y barra de progreso 4/4. Pendientes primero |
| 05 Estado | `supervised/services/ServicesStatusScreen.kt:44` | Prácticamente todo (estados, visualización, notificaciones) | Restilo. Frase de §2.1. «Sin confirmar» de ubicación con dato real (B2) |
| 06 Consentimiento | `supervised/consent/ScreenShareConsentScreen.kt:42`, `ui/components/ActiveShareBanner.kt:25`, avisos en `RuleEnforcementService.kt:379`, `LocationReportingService.kt:192`, `ScreenShareService.kt:401` | Casilla, Continuar, Ahora no, banner, 3 avisos | Pasos 1-2-3, cuenta atrás visible (desde `CONSENT_TIMEOUT_MS`), textos de §2.1 y opcionalmente B5 |
| 07 Bloqueo | `supervised/block/BlockScreenContent.kt:40`, `BlockPresentation.kt:48`, `BlockScreenActivity.kt` | 7 variantes con lógica y textos | Solo estilo: panel navy, candado, ficha de la app |
| 08 200 % | Todas | `FlowRow` en Inicio (`615e84d`) y píldora (`9c7eb73`) | Criterio de terminado de cada sprint |
| 10 Ajustes | — (nuevo) | `ConfirmDialog` existe (`ui/components/ConfirmDialog.kt:35`), solo lo usa el tutor | `MoreSettingsScreen`, `PrivacyScreen` y `UnlinkedScreen`; diálogo «¿Cerrar sesión?» |
| 11 Detalle | — (nuevo) | Comprobación en `ON_RESUME` | `PermissionDetailScreen` por permiso. Sin espera artificial: «Comprobando…» solo hasta que termina la lectura |
| 13 Casos | — (nuevo) | Detección de permisos en `ON_RESUME` | Bienvenida de 2 pasos (bandera guardada, fuera de `Linked`), aviso de batería y «Se desactivó un permiso» |
| 14 Variante B | — | — | No se construye salvo que la prueba con usuarios lo pida |
| Tema | `A/ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Elevation.kt`; `res/values/styles.xml`; `res/font/` | Paleta y tipos v1, Inter empaquetada; sin espaciado ni movimiento; sin `forceDarkAllowed` | Paleta v2, serif empaquetada, tokens de espaciado y movimiento, «quitar animaciones», `forceDarkAllowed=false` |
| Galería | `src/debug/.../ScreensGallery.kt:83`, `DesignGallery.kt:57` | Frames de S48–S50 | Frames de cada sprint al principio; ningún diálogo |

**Componentes:** se adaptan antes de crear otros nuevos. `NpButton` (añadir la ficha de flecha y altura mínima de
60 dp), `StatusPill` (hace de `NpStatusChip`), `ListRow`/`IconTile` (hacen de `NpRow`), `OtpInput` (hace de
`NpCodeField`), `NpBottomBar` y `ConfirmDialog`. Nuevos: `NpHeroPanel`, `NpSeal` y `NpRail`. Iconos que faltan en
`NpIcons.kt`: **Battery**, **WifiOff** y **SlidersHorizontal** (Más ajustes), de Lucide como `VectorDrawable` (D-03).

---

## 4. Huecos de datos y de backend

Ninguno exige cambiar el backend para la línea base v2.

| Id | Hueco | Dónde | Propuesta |
|---|---|---|---|
| B1 | El teléfono no detecta que el tutor lo desvinculó | App | Consultar `/devices/me` en `ON_RESUME` y cada 5 min mientras está vinculado. Una lista de tutores vacía pasa al estado `Unlinked`. Leer el `status` del latido no basta, porque `ALERT` lo tapa |
| B2 | No hay hora de la última ubicación enviada | App (`LocationReportingService`, `ServiceStatusRegistry`) | `lastLocationReportedAt`, que solo se escribe cuando el envío tiene éxito |
| B3 | Sin Internet y servicio caído se confunden | App | `ConnectivityManager` con `ACCESS_NETWORK_STATE` (decisión 5) |
| B4 | Un 5xx al restaurar la sesión se muestra como «sin conexión» | App (`AuthRepository.restoreSession`) | Distinguir `Server` de `Unreachable` |
| B5 | «Atrás» o la caducidad no avisan al tutor, y la web espera para siempre | App + web, realtime | Enviar `granted=false` (motivo `expired`/`dismissed`) y poner un límite en la web. Necesita `security-reviewer` (decisión 6) |
| B6 | Superposición, ubicación o administrador quitados no llegan al tutor | Backend + app | **Fuera de alcance.** Los textos no lo prometen (§2.1) |
| B7 | Cerrar sesión detiene la protección sin avisar al tutor (solo OFFLINE a los 300 s) | Backend | **Fuera de alcance**; pasa a `docs/tasks.md`. Decisión del dueño: cerrar sesión se permite, con confirmación |
| B8 | `selectRole` concede cualquier rol (B-03) | Backend | Sigue igual. Quitar el modo tutor de la app reduce la superficie |

---

## 5. Riesgos

1. **G-19: reiniciar servicios sin querer.** `SupervisedState.Linked(tutorLabel)` es una `data class`, y los efectos
   de los servicios dependen de `state` (`SupervisedShell.kt:237-357`). Si B1 refresca los tutores y cambia
   `tutorLabel`, **se reinician todos los servicios**. Regla del plan: `Linked` lleva solo una identidad estable. Los
   tutores, `deviceInfo`, la bienvenida, el aviso de batería y la ruta van en estado de presentación aparte. Además,
   `LaunchedEffect(state)` de `POST_NOTIFICATIONS` (:178-182) se vuelve a lanzar con cada cambio de estado.
   Prueba obligatoria tras tocar el shell: navegar 3 minutos y comparar `dumpsys activity services
   com.netprotect.app` (mismos `ServiceRecord`) y el latido en el backend cada minuto.
2. **Quitar el modo tutor** toca el flujo de inicio de sesión y roles (`HomeScreen.kt`). Es sensible: lo hace Claude,
   con `security-reviewer`. Hay que decidir qué ve una cuenta que solo tiene rol TUTOR (decisión 1).
3. **El cambio de paleta afecta a todo lo que usa `ui/theme`.** Si el modo tutor se queda, sus pantallas cambian de
   color sin haberse diseñado para la nueva paleta. Por eso la decisión 1 va antes del sistema v2.
4. **Tests con textos exactos.** Los tests de Compose buscan textos literales (lección de `615e84d`), así que cada
   cambio de texto actualiza su test en el mismo commit. Además `ColorTokensTest` y `DesignGalleryTest` dependen de
   los tokens y de las secciones.
5. **El emulador local (API 36) no pasa los tests de Compose** (causa sin diagnosticar). Se validan en CI.
6. **La fuente serif suma peso al APK**: se empaqueta solo el peso 600.
7. **Batería según el fabricante:** los pasos de One UI solo se validan en el Galaxy.
8. **La rama de integración lleva mucho sin fusionarse** (`android-redesign` no está en `main`). Apilar seis sprints
   más sobre ella sube el riesgo de conflictos (ver decisión 2).

---

## 6. Sprints propuestos (S61–S67)

Cada sprint vive en `sprint-NN-<slug>` desde `android-redesign`, con su `docs/sprint-NN.md` y `-evidence.md`. Se
fusiona solo con los 8 jobs de CI en verde y con OK del dueño.

| Sprint | Contenido | Láminas | Riesgo | Claude | DeepSeek |
|---|---|---|---|---|---|
| **S61** Solo supervisado | Quitar `feature/tutor/**`, `RoleSelectionScreen`, `switchMode` y la galería del tutor. Rol SUPERVISADO automático tras iniciar sesión. Pantalla para una cuenta que solo es tutor. Actualizar `INVENTARIO.md`, `CLAUDE.md` del proyecto y README | 00, 10.3 | Medio (auth/roles) | Todo: flujo de roles y `security-reviewer` | Borrado mecánico de tests y docs del tutor, si conviene |
| **S62** Sistema v2 | Paleta, serif, tokens de espaciado y movimiento, `rememberReducedMotion()` (`ANIMATOR_DURATION_SCALE`), `forceDarkAllowed=false`, `NpHeroPanel`, `NpSeal`, `NpRail`, ajustes de `NpButton`/`StatusPill`, iconos que faltan, galería | 08, 09 | Medio | Tokens base y revisión visual | Componentes con firmas dadas y su galería |
| **S63** Acceso, Vincular y sesión | Errores tipados (B3, B4), «Vuelve a entrar», 429, restilo de Carga, Login y Vincular, `MoreSettingsScreen` con «¿Cerrar sesión?» | 01, 02, 10.1-2 | Bajo | Mapeo de errores en `core/network` | Cuerpos de pantalla y tests |
| **S64** Inicio y Permisos | Regla §2.3 (pura, en JVM), panel de Inicio, rail de Permisos, **B1 + `UnlinkedScreen`**, separar `Linked` del estado de presentación | 03, 04, 10.4 | **Alto (G-19)** → Opus | Shell, B1 y regla | Cuerpos de Inicio, Permisos y Desvinculado |
| **S65** Consentimiento y Estado | Consentimiento en 3 pasos con cuenta atrás, restilo de Estado y banner, textos de los avisos, B2, B5 si se aprueba | 05, 06 | **Muy alto (realtime)** → Opus | Todo lo de realtime y servicios; `security-reviewer` | Cuerpo de Estado |
| **S66** Bloqueo, Privacidad y Detalle | Restilo del bloqueo (7 variantes), `PrivacyScreen`, `PermissionDetailScreen` | 07, 10.5, 11 | Medio | Rutas, intents en el shell y revisión | Cuerpos y galería |
| **S67** Casos reales y cierre | Bienvenida de 2 pasos, aviso de batería (`isIgnoringBatteryOptimizations`, solo comprobar y abrir ajustes, sin `REQUEST_IGNORE_…`), «Se desactivó un permiso» al volver a la app. Pasada de 200 %, TalkBack, contraste, regresión y PR `android-redesign` → `main` | 13, 08 | Medio | Todo | Lista mecánica de cierre |
| **Prueba** | Ronda parcial tras S64 (tareas 1, 2 y 6) y final tras S67 (todas); decide A/B | 14, 15 | — | Preparación y lectura | — |

El aviso de Android «Se desactivó un permiso» **en segundo plano** (lámina 13.5) requiere comprobar los permisos desde
`RuleEnforcementService`. Queda **opcional en S67**: la línea base solo lo detecta al volver a la app.

### Encaje con S50 y S51

Tu recomendación era hacer S50 con las láminas v2 y dejar S51 para el final. Como **S50 ya está hecho**, la duda real
es qué hacer con S51. Mi recomendación es **cerrar S51 ahora, reducido**:

- Hacer T11: `security-reviewer`, documentación y PR `sprint-51-cierre` → `android-redesign` → `main`.
- Declarar pendiente lo que queda de T10 (TalkBack, letra grande de 16 pantallas, capturas), porque v2 rehace esas
  pantallas y S67 lo vuelve a probar todo.
- Esto da a `main` una versión estable antes de seis sprints más, y evita revisar con TalkBack pantallas del tutor que
  se van a borrar.
- Contra: `main` recibe una versión que se va a reestilizar. La alternativa es dejar S51 para el final (como S67) y
  acumular todo en `android-redesign`.

### Criterios de terminado (cada sprint)

1. Coincide con las láminas en estructura, jerarquía y textos de §2. No hace falta igualar el píxel.
2. Ningún dato inventado. Lo que no existe se omite o usa un texto cauto.
3. `./gradlew test assembleDebug lintDebug` en verde, y los 8 jobs de CI en verde, incluido `android-instrumented`.
4. Galería con los frames del sprint y capturas propias revisadas por Claude.
5. Letra al 200 % y «quitar animaciones» comprobadas en el emulador. TalkBack en el teléfono.
6. Si se tocó el shell: la prueba G-19 de §5.1, con salida en el `-evidence.md`.
7. `security-reviewer` cuando se toque auth, roles, realtime, ubicación o el shell (S61, S64, S65).

### Pruebas que necesitan una persona o el Galaxy S25 FE

- Inicio de sesión real con Google y una cuenta que solo es tutor (S61).
- TalkBack y Accessibility Scanner en todas las pantallas nuevas.
- Letra al 200 % en el teléfono, en todas las pantallas.
- Vista remota de extremo a extremo, caducidad, «Atrás» y «Detener» desde la notificación (S65).
- Si los avisos permanentes se pueden descartar en Android 16 (corrige el texto de §2.1).
- Que lleguen los reportes de ubicación y el «Sin confirmar»: el emulador no tiene proveedor de red.
- Desvincular desde el panel web y ver «Este dispositivo ya no está vinculado» (S64).
- Los pasos de batería en One UI (S67).
- Quitar un permiso en Ajustes y recuperarlo (S67).
- La prueba con 5 personas, con consentimiento del tutor y del menor para los menores.

---

## 7. Decisiones del dueño (con recomendación)

1. **Quitar el modo tutor de la app** (ya lo anunciaste; falta confirmar el alcance). *Recomendación:* sí, como S61.
   Cierra el hueco de P13. Una cuenta que solo tiene rol TUTOR ve «Esta app es para el teléfono del menor; usa el panel
   web» y el botón «Usar este teléfono como supervisado», en vez de que se le asigne el rol en silencio.
2. **S51:** cerrarlo ahora reducido (T11 + PR a `main`) o dejarlo para el final. *Recomendación:* cerrarlo ahora.
3. **Umbral de «Sin reporte reciente»** (el mismo en Inicio y en Estado). *Recomendación:* 300 s, como el backend, en
   lugar de los 180 s de hoy.
4. **Fuente serif.** *Recomendación:* Newsreader 600 (OFL), la misma del panel web, empaquetada en `res/font/`.
5. **Separar «sin Internet» de «servicio no disponible»** con `ACCESS_NETWORK_STATE`. *Recomendación:* sí. Es un
   permiso normal, sin diálogo, y el menor sabe si el problema es suyo.
6. **B5: avisar al tutor cuando el menor sale con «Atrás» o la solicitud caduca**, y poner un límite en la web.
   *Recomendación:* sí, dentro de S65, con `security-reviewer`.
7. **Detalle de permiso (lámina 11).** *Recomendación:* construirlo en S66, sencillo y con los pasos reales de cada
   permiso (§2.1).

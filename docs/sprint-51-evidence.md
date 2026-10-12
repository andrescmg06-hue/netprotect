# Sprint 51 — Evidencia

## T9 — Regresión contra el inventario de S40

Fuente: `docs/android-redesign/INVENTARIO.md` (los puntos de la app anterior al rediseño). Cada punto se buscó en el
código actual (rama `sprint-51-cierre`) y los dudosos se comprobaron en el emulador con la build `minified`.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

**Resultado: 0 puntos perdidos.** Todos están presentes o sustituidos a propósito.

### Presentes sin cambio de comportamiento

H-01, H-03, H-05..H-08, H-10..H-14, H-16, H-19, T-01, T-05, T-08, T-15, S-01, S-02, S-10, S-16, S-17, B-01..B-06,
B-08, B-09, N-01..N-12, N-15..N-17, N-19, C-02..C-05, C-12, C-13, C-15..C-20.

Siguen sin confirmación, como antes: Revocar código (C-02), Cerrar sesión (C-03), Cambiar de modo (C-04), Vincular
y Elegir rol (C-05). La sincronización de apps sigue fallando en silencio (C-12).

### Sustituidos a propósito

| Punto | Antes | Ahora | Sprint |
|---|---|---|---|
| M-01, H-04, H-09 | `MaterialTheme`; «Servidor: … · BD: … · Redis: …» | Tema propio; «Servicio listo / no disponible» con «Reintentar» | S42, S43 |
| H-02, C-14, N-18, C-25 | Sin red: sesión borrada en silencio | Sesión conservada; «Sin conexión» y se retoma sola | S41 |
| H-15, T-02, S-06 | `detail` crudo o «HTTP nnn» | Mensajes en español (`UiError`) | S41 |
| H-17, H-18 | `TutorScreen`/`SupervisedScreen` monolíticos | `TutorShell`/`SupervisedShell` con `TokenSession` | S44, S48 |
| T-03, C-23 | Código sin caducidad en pantalla | Cuenta atrás «Vence en m:ss» y «El código venció» | S44 (G-09) |
| T-04, C-09 | Error al revocar silenciado | Error visible | S44 |
| T-06, T-07 | Sección «Ver/Ocultar» con 50 filas | Pestaña «Actividad», agrupada por día, paginada | S47 (G-17) |
| T-09, T-10 | `ONLINE`/`OFFLINE` crudos | «En línea / Desconectado / Alerta» y «hace N min» | S42 (G-07), S44 |
| T-11, C-06, C-08 | Renombrar sin validar; error silenciado | Valida 1–255; error visible; la edición sigue abierta | S44 |
| T-12, C-01, C-07 | Desvincular al primer toque, error silenciado | Diálogo de confirmación y error visible | S44 (G-11) |
| T-13 | Lista simple | Búsqueda, iconos, orden instaladas → uso | S45 (G-12) |
| T-14 | `Lat, Lng (±n m)` | Sin coordenadas: «Última lectura», «Precisión aproximada», «Dentro de» | S45 (G-13, D-05) |
| C-10 | Sin app de mapas: silencio | «No hay una aplicación de mapas en este teléfono.» | S45 |
| T-16 | Lista de texto | Pantalla Geocercas con «Entró · / Salió ·» | S45 |
| T-17 | Lista de texto | Agrupado por día, «Bloqueo de app · X», «Motivo: …» | S46 (G-14) |
| T-18, T-19 | Periodo recordado por dispositivo | Periodo guardado al girar; cumplimiento «5 de 7 días» | S46 (G-15), S51 |
| T-20 | Solo lectura | Marcar leída, Silenciar (con confirmación), filtros | S46 (G-16) |
| T-21 | 9 etiquetas | Las 9 siguen; 3 reescritas (ver abajo) | S46 |
| T-22, C-24 | Secciones desplegables sin reintento | Rutas apiladas; `ErrorState` con «Reintentar» | S44 (D-01) |
| T-23 | Cerrar sesión al pie | En Inicio y en la pestaña Más | S44 |
| S-03..S-05, C-22 | Campo de texto; sin bloqueo durante la llamada | 6 casillas; botón deshabilitado mientras vincula | S48 (G-20) |
| S-07, C-11 | Tarjeta fija; latido fallido en silencio | Varios tutores, última comunicación, «Sin conexión» | S48 |
| S-08, S-09, S-11 | Tarjeta de solicitud | Pantalla de consentimiento; reconexión; cierre a los 2 min | S50 (D-12, B-02) |
| S-12..S-15 | Tarjetas que desaparecen al conceder | Tarjetas «Configurado / Pendiente» siempre visibles | S48 |
| N-13 | Sin relectura al volver de Ajustes | `ON_RESUME` relee los 4 permisos | S48 |
| N-14 | Todo con `remember` | Pila, selecciones y código a medio escribir sobreviven al giro | S42, S51 |
| N-20, C-21 | `formatCapturedAt` estilo `MEDIUM` | «Hoy, 3:42 p. m.», «hace N min» | S42 |
| B-07 | Aviso que explicaba cómo evadir el bloqueo | «…la app puede seguir abierta en segundo plano.» | S49 |
| C-22 (resto) | Sin progreso en Generar, Guardar, Desvincular | Con estado «en curso» y bloqueo de doble toque | S44 |

### Cambios de texto que no estaban documentados (para el dueño)

- **S-13 (permiso de ubicación):** se quitó «sólo se comparte mientras esta app siga activa en segundo plano». La
  frase sigue siendo cierta, y la pantalla 17 lo cubre con la notificación «NetProtect comparte la ubicación».
- **T-21:** `PERMISSION_REVOKED`, `SERVICE_INACTIVE` y `HEARTBEAT_SILENCE` tienen redacción nueva, más precisa y con
  el mismo sentido.
- **S-15:** «tu tutor recibirá un aviso» pasó a «Permite avisar al tutor si se intenta desinstalar». Estaba en el
  plan de S48.

### Pendiente en un dispositivo real (T10)

- N-09: que el diálogo de notificaciones no reaparezca en cada vuelta a «Vinculado».
- N-13, S-12..S-15: volver de Ajustes y ver la tarjeta cambiar a «Configurado».
- S-08..S-11: consentimiento y vista remota real (H-02), que cubre también WebRTC con R8.

### Capturas

En `%TEMP%\np-sprint51\`, con la build `minified` y un supervisado de prueba vinculado por API:

- Tutor: `final-03-tutor-inicio` … `final-11-mi-actividad` (mockups 3–11).
- Supervisado: `t5-01-launch` (13, vinculado) y `t5-02-services` (17, Estado de NetProtect).

Sin capturar: 01–02 (login y elegir modo: requieren cerrar sesión y volver a entrar con Google), 12, 14, 15 y 16.
Quedan para T10 en el Galaxy; las de 12, 14, 15 y 16 están en las evidencias de S48, S49 y S50.

## T10 — Prueba en el Galaxy S25 FE (parcial, 12/10/2026)

Horas en UTC. Dispositivo del teléfono: `1f78580e` («samsung SM-S731B», Android 16). No se leyeron coordenadas.

### Instalación (punto 1)

- `ANDROID_SERIAL=R5CY92MJ2PY NETPROTECT_API_BASE_URL=http://localhost:8000 ./gradlew installMinified` desde `mobile/`:
  `BUILD SUCCESSFUL in 1m 49s`, con `minifyMinifiedWithR8`; instalado `com.netprotect.app` 0.1.0-sprint1.
- `adb -s R5CY92MJ2PY reverse tcp:8000 tcp:8000` → `reverse --list` muestra `UsbFfs tcp:8000 tcp:8000`.
- Fallo en el camino: el primer inicio de sesión dio «failed to connect to localhost port 8000». La lista de
  `adb reverse` estaba vacía (el puente se perdió); el backend respondía 200 en la PC. Se restauró con el mismo comando.
  El puente no sobrevive a reconexiones del cable ni a reinicios de `adb`, y `adb reverse` solo redirige TCP.

### Vista remota y ubicación (punto 4)

`audit_logs` desde las 03:50 (consulta por `action`, `resource_type` y `resource_id`):

| Hora | Acción | Dispositivo |
|---|---|---|
| 03:56:41 | `PAIRING_CODE_GENERATED` (×2) | — |
| 03:57:03 | `DEVICE_LINKED` y `SCREEN_SHARE_CONSENT_GRANTED` | `1f78580e` |
| 03:57:21–03:59:13 | `SCREEN_SHARE_REQUESTED` y `SCREEN_SHARE_STOPPED` (×3, sin `STARTED`) | `da5e6b92` (emulador) |
| 04:01:29 | `SCREEN_SHARE_REQUESTED` | `1f78580e` |
| 04:01:40 | `SCREEN_SHARE_STARTED` | `1f78580e` |
| 04:02:25 | `SCREEN_SHARE_STOPPED` | `1f78580e` |
| 04:03:35–04:04:45 | `LOCATION_VIEWED` (×3) | `1f78580e` |

- **Error de uso, no del producto:** las tres primeras solicitudes fueron al «Pixel 8 EMULADOR» (`da5e6b92`), que
  seguía vinculado al tutor y no captura pantalla; por eso no hay `STARTED`. Con el dispositivo correcto la sesión
  empezó 11 s después de la solicitud y duró unos 45 s.
- `device_location_reports` del teléfono: 1 reporte (03:58:46), precisión 2000 m. Es lo esperado: la app solo tiene
  `ACCESS_COARSE_LOCATION` y reporta cada 15 min (`REPORT_INTERVAL_MS`, `LocationReportingService.kt`).
- **Confirmación visual (H-02):** el dueño confirmó que vio el vídeo del teléfono en el panel. Es declaración suya,
  sin captura ni grabación.
- **Sin comprobar:** la ruta WebRTC. Los TURN
  por defecto (`turn:localhost:3478`, `turn:10.0.2.2:3478`, UDP) no son alcanzables desde un teléfono real, así que
  la sesión pudo ir en directo por Wi-Fi; para forzar un relé hay que poner la IP de la PC en `TURN_URLS` y abrir
  UDP 3478.

### Fuente grande (punto 3, parcial)

Escala cambiada con `adb shell settings put system font_scale` (valor original del teléfono: 1.7, restaurado al
terminar) y capturas con `adb exec-out screencap`, en `%TEMP%\np-sprint51\` (`t10-fuente-*` antes, `t10-fix-fuente-*`
después). **Solo se revisó la pantalla 13, «Dispositivo vinculado»**, arriba y abajo.

- **Defecto encontrado:** en «Última comunicación», «Conectado» se partía por letras: «Conectad / o» a 1.3, «Co / nec /
  tad / o» a 1.7 y una letra por línea a 2.0, estirando la tarjeta. Causa: `Row` con tres `Text`
  (`LinkedDeviceScreen.kt`); el estado recibía solo el ancho sobrante. Mismo tipo de defecto que B-06.
- **Corrección** (`615e84d`): `FlowRow`, como `DeviceDetailScreen.kt`; el texto «Sin conexión» no cambia, para no
  romper `LinkedDeviceScreenTest.kt:104`. Compilada e instalada con `installMinified` (`BUILD SUCCESSFUL in 1m 9s`);
  `testDebugUnitTest`: 162 tests, 0 fallos.
- **Comprobación visual tras el arreglo:** a 1.3 y a 2.0 «Conectado» queda entero en su propia línea.
- **Regresión mía en `615e84d`, vista en el CI** (run 38203874517, `android-instrumented`): pegué « ·» al texto del
  tiempo y `LinkedDeviceScreenTest.noContactShowsTodaviaNo` falló (`performScrollTo() failed`), porque busca
  «Todavía no» exacto; los otros 142 tests pasaron. Antes solo había buscado en los tests «Conectado», «Sin conexión»
  y «Hace un momento». Corregido en un commit posterior: el tiempo vuelve a ser un `Text` con su texto original y el
  punto va a su lado en una `Row`, un único elemento del `FlowRow`. Comprobado a mano a 1.3 y 2.0.
- **El test no se pudo ejecutar en local:** en el emulador `Pixel_8` (Android 16) los 10 tests de
  `LinkedDeviceScreenTest` fallan antes de evaluar nada con `Unable to resolve activity ... ComponentActivity`, y
  `DevicesScreenTest` falla igual; es del entorno, no del cambio. Causa sin diagnosticar. Lo valida el CI.
- **No hecho:** no se añadió test de regresión para fuente grande, y no se ejecutaron los tests instrumentados (el
  CI los ejecuta en el emulador, no en el teléfono del dueño). No se revisaron a fuente grande las otras 16 pantallas,
  ni la escala 1.7 tras el arreglo.

### Pendiente en T10

TalkBack en las 17 pantallas (punto 2), fuente al 130 % y 200 % en las otras 16 pantallas (punto 3) y capturas 01,
02, 12, 14, 15 y 16 (punto 5): sin evidencia. El punto 4 (H-02) y, con él, T5 quedan cerrados.

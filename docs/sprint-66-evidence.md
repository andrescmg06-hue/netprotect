# Sprint 66 — evidencia

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-s66`, rama `sprint-66-bloqueo-privacidad`. Emulador `Pixel_8` (Android 16), build de debug, cuenta
supervisada del dueño ya vinculada. Capturas y registros en `docs/android-redesign/supervisado-v2/s66/`.

## Builds sin caché y pruebas por tarea

`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` y `connectedDebugAndroidTest`, ejecutados por un `verifier` distinto del escritor, con
`:app:testDebugUnitTest` ejecutada (no UP-TO-DATE):

| Tarea | Resultado |
|---|---|
| T2 (bloqueo restilado) | BUILD SUCCESSFUL en 1m 12s, 182 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` en `Pixel_8`: **113 tests, 0 fallos** |
| T3 (Privacidad, Detalle, rutas) | BUILD SUCCESSFUL en 1m 26s, 194 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest`: **129 tests, 0 fallos** (con `PrivacyScreenTest` y `PermissionDetailScreenTest`) |

## T3 — afirmaciones de privacidad comprobadas contra el código

El escritor entregó una tabla afirmación → fuente; el orquestador la contrastó con el código (no solo con la tabla):

- **Latido cada minuto con la app abierta y también en segundo plano con menos frecuencia:** `SupervisedShell.kt` (`HEARTBEAT_INTERVAL_MS = 60_000L`) y `SyncWorker` (`sendHeartbeat`, `syncApplications`, periodo mínimo de WorkManager de 15 min).
- **Qué lleva el latido:** `DeviceClient.sendHeartbeat` envía `os_version`, `app_version`, `timezone`, `device_time`, `usage_access_granted` y `service_active`.
- **Lista de apps y tiempo de uso cada 5 minutos, solo con el acceso al uso:** `APP_SYNC_INTERVAL_MS = 5 * 60_000L` y `SyncWorker` (`if (UsageAccessPermission.isGranted…)`).
- **Cada bloqueo se informa al tutor con la app y la hora:** `RuleEnforcementService` → `reportRuleEvent(package_name, rule_type_applied, occurred_at)`; si falla se encola.
- **Ubicación aproximada cada 15 min, sin la precisa:** `LocationReportingService.REPORT_INTERVAL_MS`; el manifiesto declara solo `ACCESS_COARSE_LOCATION` (sin `FINE` ni `BACKGROUND`).
- **El administrador solo avisa, no impide desinstalar:** `NetProtectDeviceAdminReceiver.onDisableRequested` no puede vetar; reporta `UNINSTALL_ATTEMPT`.
- **No se graba ni se guarda la pantalla:** sin `MediaRecorder`, `ImageReader` ni `MediaMuxer` en Android ni en la web (PLAN §1, pregunta 12).
- **No se afirma ninguna retención:** solo la de ubicación (7 días) está documentada; la del uso de apps no, y `docs/security-baseline.md` la declara control diferido. Un test JVM prohíbe
  «impide», «ni controla», «controla», «retención», «conserva», «días», «meses» y «pendiente de validar» en todos los textos de Privacidad y Detalle.

## T4 — emulador

### Revisión visual (galería `ScreensGalleryActivity`, letra 1.0 y 2.0)

- **Privacidad** (lámina 10.5), **Detalle** de los cuatro permisos (lámina 11), **Inicio con la fila «Privacidad»**, **Permisos con «Ver detalle»** y las **9 variantes del bloqueo** (lámina 07) coinciden en
  estructura con las láminas. A letra 2.0 nada se parte por letras y el pie fijo con botones sigue usable. Los recortes dentro de los marcos de la galería son de la galería (cajas de altura fija), no de la app.
- Diferencias aceptadas con las láminas: pasos del Detalle distintos por permiso en lugar de los tres iguales de la lámina 11; botón «Activar protección» (no «Activar en Ajustes») para el administrador;
  sin «Comprobando…» (la relectura es instantánea y sería una espera falsa); en el bloqueo, un solo texto por motivo (los de `BlockPresentation.kt`) en lugar de titular más pista de la lámina.
  Detalle menor: en el motivo «bloqueada» el titular («Tu tutor bloqueó esta app.») y el detalle («Tu tutor decidió bloquear esta app en este dispositivo.») son parecidos.

### Bloqueo real (ventana superpuesta), con los márgenes de las barras del sistema

Se abrió YouTube con la regla `BLOCK` que el dispositivo ya tenía (`com.google.android.youtube`, del 4/10), sin tocar la base de datos. El escritor había dejado un margen mínimo fijo por si la ventana
`TYPE_APPLICATION_OVERLAY` no recibía los márgenes de las barras del sistema; se comprobó en tres combinaciones:

| Navegación | Letra | Resultado |
|---|---|---|
| Gestos | 1.0 | Panel bajo la barra de estado; «Ir al inicio» por encima de la barra de gestos; nada se corta (`app-real-bloqueo-gestos-100.png`) |
| Tres botones | 1.0 | «Ir al inicio» por encima de los tres botones, sin solapamiento (`app-real-bloqueo-3botones-100.png`) |
| Tres botones | 2.0 | Titular y detalle en varias líneas; nota y botón por encima de la barra (`app-real-bloqueo-3botones-200.png`) |

Después de la prueba se restauró el emulador: navegación por gestos y letra 1.0.

### Prueba G-19 — navegar no reinicia servicios

Guion en `g19-serviceRecords.log`. Durante 3 min 15 s se repitió 5 veces: Permisos → Detalle del primer permiso → Atrás → Atrás → Estado → Atrás → Privacidad → Atrás → Más ajustes → Atrás → Inicio del sistema → volver a la app.

```
T0 18:10:34 UTC  ServiceRecord{b00d953 … RuleEnforcementService}    createTime=-25s70ms     lastStartId=1
                 ServiceRecord{830b7bc … LocationReportingService}  createTime=-25s60ms     lastStartId=1   pid 4855
T1 18:13:49 UTC  ServiceRecord{b00d953 … RuleEnforcementService}    createTime=-3m39s620ms  lastStartId=1
                 ServiceRecord{830b7bc … LocationReportingService}  createTime=-3m39s608ms  lastStartId=1   pid 4855
```

Mismos `ServiceRecord`, mismo proceso y `lastStartId=1`: ningún servicio se paró ni se relanzó. Backend (`backend-heartbeat-y-b1.log`): latidos a las **18:11:10, 18:12:10 y 18:13:11** (uno por minuto; los dobles de las 18:09–18:10
son del arranque de la app) y **consultas B1** `GET /devices/me` en cada vuelta, sin ningún 5xx ni `Traceback`.

## Pendiente (declarado)

- **No probado en el Galaxy S25 FE ni con One UI:** los pasos de cada permiso (la página del permiso de superponer, la lista de «Acceso especial» del uso, la pantalla del administrador) son del sistema y cambian por fabricante; S67 los repasa.
- **No probado:** las 7 variantes del bloqueo en la ventana real; solo se vio «bloqueada» (BLOCK) con YouTube. Las demás se ven en la galería; la técnica de `docs/sprint-49-evidence.md` §3 sirve para provocarlas.
- **No probado: qué se envía al cerrar NetProtect del todo** (Atrás desde Inicio): el revisor deduce por el código que el `onDispose` del shell cancela `SyncWorker` y para los servicios; Privacidad no afirma nada al respecto.
- **No probado:** TalkBack ni «quitar animaciones» con el candado animado (el código lo respeta; sin prueba en dispositivo).
- **El detalle de ubicación «Abrir Ajustes de la app» no se ejercitó en el emulador** (abre `ACTION_APPLICATION_DETAILS_SETTINGS`, con el código revisado).
- **`coturn` se reinicia en bucle desde que Docker arrancó tras el reinicio del equipo** (`Restarting (2)`); no afecta a S66 pero conviene mirarlo antes de probar la vista remota con un tutor real.
- El CI de los 8 jobs corre con el PR.

## T5 — revisión de seguridad (`security-reviewer`, Opus)

Solo lectura del diff `origin/android-redesign..HEAD -- mobile/`: **sin hallazgos ALTA**; dos, ambos arreglados tras medirlos:

1. **MEDIA — Privacidad subestimaba lo que se envía.** El texto decía «Cada minuto mientras la app esté abierta. Cuando no lo está, también puede enviarlo, con menos frecuencia» (y lo mismo para la lista de apps cada 5 min).
   Los bucles del latido y de la sincronización de apps son `LaunchedEffect` del shell y siguen mientras el shell exista, aunque haya otra app delante. **Medido en el emulador** con NetProtect en segundo plano sin cerrarla
   (`segundo-plano-latido.log`, `backend-segundo-plano.log`): latidos a las 18:21:15, 18:22:16 y 18:23:17 con el lanzador del sistema en primer plano; sincronizaciones de apps a las 18:15:10 y 18:20:10 (esta con NetProtect detrás de la
   pantalla de Ajustes). Ahora: «mientras NetProtect esté en marcha, aunque uses otras apps»; sin afirmar qué pasa al cerrarla del todo. La frase estaba igual en `ServicesStatusScreen` desde antes del sprint y se corrigió también.
   Un test prohíbe volver a ligar el envío a «esté abierta».
2. **BAJA — el paso de superponer era falso.** Decía «Se abre la página de este permiso para NetProtect». En Android 11 y posteriores el sistema ignora el paquete de `ACTION_MANAGE_OVERLAY_PERMISSION` y abre la lista general: se comprobó en el
   emulador API 36 (`app-real-superponer-lista-general.png`, «Display over other apps» con NetProtect «Allowed» entre otras). Ahora: «Android abre una lista de apps con este permiso, no la página de NetProtect» y «Busca NetProtect en la lista…».

Confirmado sin hallazgos (el revisor lo contrastó con el código): campos del latido (`DeviceClient.sendHeartbeat`), lo que envía la lista de apps (`package_name`, `app_label`, `is_system_app`, `foreground_seconds`), el aviso de cada bloqueo con su cola,
la ubicación aproximada (`NETWORK_PROVIDER`, solo `ACCESS_COARSE_LOCATION`), el aviso de desinstalación que no puede impedirla, y que no hay `MediaRecorder`/`ImageReader`/`MediaMuxer` en mobile, web ni backend; ninguna afirmación de retención; `onOpenAppSettings`
con paquete fijo (`context.packageName`); las pantallas no lanzan Intents ni piden permisos; las rutas `Privacy` y `PermissionDetail` solo se pintan dentro de `Linked`; ninguna clave de efecto de servicio cambió; el candado se anima una vez y respeta
`rememberReducedMotion`; el diff no toca `BlockOverlayController`, `BlockScreenActivity` ni el manifiesto. Omisiones menores que el revisor no reporta: al vincular se envía el nombre del dispositivo (una vez) y la última ubicación del panel web puede
mostrarse con Google Maps Embed (decisión del Sprint 13).

Verificación posterior: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` → BUILD SUCCESSFUL en 2m 39s, 194 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` en `Pixel_8` → **129 tests, 0 fallos**.

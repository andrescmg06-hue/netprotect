# Sprint 64 — evidencia

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-s64`, rama `sprint-64-inicio-permisos`. Emulador `Pixel_8`
(Android 16), build de debug, cuenta supervisada del dueño ya vinculada (`Pixel 8 EMULADOR`). Capturas y registros en
`docs/android-redesign/supervisado-v2/s64/`.

## T2, T3 y T4 — builds sin caché

`./gradlew testDebugUnitTest [assembleDebug assembleDebugAndroidTest] lintDebug --rerun-tasks`, ejecutados por un `verifier`
distinto del escritor, con `:app:testDebugUnitTest` ejecutada (no UP-TO-DATE):

| Tarea | Resultado |
|---|---|
| T2 (regla §2.3, umbral 300 s) | BUILD SUCCESSFUL, 128 tests, 0 fallos, lint 0 errores |
| T3 (shell, B1, `Unlinked`) | BUILD SUCCESSFUL en 1m 26s, 137 tests, 0 fallos, lint 0 errores |
| T4 (Inicio, Permisos, Desvinculado) | BUILD SUCCESSFUL en 1m 25s, 144 tests, 0 fallos, lint 0 errores |

## T5 — emulador

- `./gradlew connectedDebugAndroidTest` → BUILD SUCCESSFUL en 3m 2s, **99 tests, 0 fallos, 0 errores, 0 omitidos** (los 88 de S63
  más los nuevos de Inicio, Permisos y Desvinculado).
- **Revisión visual de la galería** (`ScreensGalleryActivity`) a letra 1.0 y 2.0 contra las láminas 03, 04 y 10.4: Inicio en
  «Todo funciona», «Falta un paso» (2 pendientes y 1 pendiente), «Sin reporte reciente» y «Vista remota en curso»; Permisos con
  dos pendientes y con todos configurados; Desvinculado. A 2.0 todo baja de línea sin cortarse ni partir palabras por letras.
  Diferencias aceptadas: con 360 dp «Falta un paso» baja a dos líneas; la fila «Última comunicación» apila «Sin actualización
  reciente» bajo la hora; «Todos configurados» ocupa dos líneas en la mosaica de Permisos.
- **App real** (no la galería): Inicio y Permisos con datos reales (`Pixel 8 EMULADOR`, `Andres Mosquera`, 4 de 4 permisos).

### Prueba G-19 — navegar no reinicia servicios

Guion en `g19-serviceRecords.log`. Durante 3 min 11 s se repitió 6 veces: Permisos → Atrás → Estado → Atrás → Más ajustes → Atrás →
Inicio del sistema → volver a la app (cada vuelta dispara el `ON_RESUME` de B1).

```
T0 23:56:30  ServiceRecord{83ea7f5 … RuleEnforcementService}  createTime=-34s259ms  lastStartId=1
             ServiceRecord{658598a … LocationReportingService} createTime=-34s255ms  lastStartId=1   pid 9929
T1 23:59:41  ServiceRecord{83ea7f5 … RuleEnforcementService}  createTime=-3m45s371ms lastStartId=1
             ServiceRecord{658598a … LocationReportingService} createTime=-3m45s367ms lastStartId=1   pid 9929
```

Mismos `ServiceRecord`, mismo proceso y `lastStartId=1`: ningún servicio se paró ni se relanzó (`createTime` solo se aleja 3m11s).
Backend (`backend-heartbeat-y-b1.log`, 04:56–04:59 UTC): **latidos a las 04:56:56, 04:57:56, 04:58:56 y 04:59:57** (uno por minuto) y
**6 consultas B1** `GET /devices/me` (04:56:58, 04:57:30, 04:58:01, 04:58:33, 04:59:05, 04:59:37), todas 200, sin ningún 5xx ni
`Traceback`.

### B1 real — desvincular de verdad

Se simuló el cierre del vínculo por el tutor con un cambio reversible en la base de desarrollo (el panel web exige iniciar sesión
con Google): `tutor_devices` fila `09d6bf3a-…`, única activa del dispositivo.

1. Antes: `unlinked_at IS NULL` (activa). `UPDATE tutor_devices SET unlinked_at = now() WHERE id = '09d6bf3a-…' AND unlinked_at IS NULL`
   → `UPDATE 1` a las 05:00:59 UTC.
2. Volver a la app (`ON_RESUME`): la app pasó **sola** a «Este dispositivo ya no está vinculado» (`app-real-b1-desvinculado.png`)
   y `dumpsys activity services` pasó de 2 `ServiceRecord` a **0**.
3. Backend: último latido a las 05:00:58 (antes del cambio) y **ninguno más** hasta las 05:02:35 (el siguiente habría sido ≈05:01:58);
   tampoco `rules/active`. La sincronización y la ubicación están paradas.
4. Restauración: `UPDATE tutor_devices SET unlinked_at = NULL WHERE id = '09d6bf3a-…'` → `UPDATE 1`; filas activas del dispositivo = 1;
   al relanzar la app vuelve a «Todo funciona» con 2 `ServiceRecord` (`app-real-b1-revinculado.png`). Los datos quedaron como estaban.

## Defecto encontrado en la app real y corregido

Con el panel navy de Inicio bajo la barra de estado, la hora y los iconos (oscuros, por el estilo fijo de `MainActivity`) casi no se
leían (`app-real-inicio-antes-del-arreglo-barra.png`). La galería no lo mostraba porque sus frames no llegan a la barra. Arreglo:
`LightStatusBarIcons()` en `LinkedDeviceScreen` pone iconos claros mientras Inicio está en pantalla y restaura el valor anterior al
salir; Permisos vuelve a tener iconos oscuros (`app-real-inicio-barra-clara.png`, `app-real-permisos.png`).
`testDebugUnitTest installDebug lintDebug` → BUILD SUCCESSFUL en 1m 4s, 144 tests, lint 0 errores.

## Pendiente (declarado)

- **No probado con un tutor real:** desvincular desde el panel web (se simuló con la base de datos, que es el mismo dato que lee
  `/devices/me`) ni en el Galaxy S25 FE.
- **No probado:** TalkBack, «quitar animaciones» y los pasos de batería (S67).
- **Riesgo de uso, resuelto por decisión del dueño:** los pasos «Pendiente» del rail no llevaban botón (como la lámina 04), así que si el
  menor denegaba para siempre el permiso «siguiente» (p. ej. ubicación) los demás no se alcanzaban desde esa pantalla. Ahora todos los
  pendientes llevan botón (el «siguiente» primario, los demás secundario; `galeria-permisos-pendientes-con-botones-100.png`).
  `testDebugUnitTest lintDebug connectedDebugAndroidTest` → BUILD SUCCESSFUL en 3m 37s, 147 tests unitarios, lint 0 errores,
  99 instrumentados con `everyPendingStepHasItsOwnButton` y `withAllPendingEachButtonCallsItsOwnCallback` ejecutados y en verde.
- **Fuera de alcance, visto:** el aviso `ActiveShareBanner` dice «Tu tutor está viendo esta pantalla» y la lámina 03, «…tu pantalla»
  (lo rehace S65); `heartbeatFailing` del shell ya no se lee (aviso del compilador); el titular de Inicio usa el último contacto
  más reciente entre el latido local y el `last_seen_at` del servidor, y Estado solo el local, así que pueden diferir unos segundos
  en un arranque en frío.
- El camino del 404 «no vinculado» (la fila del dispositivo borrada) solo tiene pruebas JVM (`NotLinkedAnswerTest`); la prueba real de arriba
  usó el otro camino (200 con la lista de tutores vacía). El CI de los 8 jobs corre con el PR.

## T6 — revisión de seguridad (`security-reviewer`, Opus)

Solo lectura del diff `origin/android-redesign..HEAD -- mobile/`: **sin hallazgos ALTA**. Dos BAJA, ambos arreglados:

1. `getMyDevice` convertía cualquier 404 en `null` y `classifyLinkCheck` lo leía como desvinculación. Con la consulta repetida cada
   5 min y en cada `ON_RESUME`, un 404 que no viniera del backend (Caddy en un despliegue, una ruta mal enrutada) habría parado la
   ubicación, las reglas, el socket y el `SyncWorker` de todos los teléfonos que consultaran. Ahora solo el 404 con `detail =
   not_linked` (lo que manda `backend/app/api/v1/endpoints/devices.py:142`) es una desvinculación; `NotLinkedAnswerTest`, 3 tests.
2. El descarte `if (state != linked) return` comparaba por igualdad de la data class: tras `Unlinked` y un vínculo nuevo con el mismo
   `deviceId`, una respuesta tardía podía parar el vínculo nuevo. Ahora `!==`. Fallaba hacia el lado seguro y la ventana era muy
   estrecha.

Confirmado sin hallazgos: un fallo de red, timeout, 5xx, cuerpo ilegible o 401 es `Inconclusive` (no cambia estado ni borra la
caché); al entrar en `Unlinked` se paran latido, sync, reglas, ubicación, socket, `SyncWorker` y la vista remota; `UnlinkedScreen` no
muestra nombre de tutor ni `deviceId`; no hay `Log`, ni `rememberSaveable` nuevos; 300 s solo cambia lo que ve el menor, no el OFFLINE
que ve el tutor. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` → BUILD SUCCESSFUL en 1m 28s,
147 tests, 0 fallos, lint con 0 errores.

Observación fuera de alcance, ya existía: `PendingRuleEventStore` guarda por `deviceId` eventos con nombres de apps; si se vuelve a
vincular la misma fila con otro tutor, se le pueden enviar eventos del vínculo anterior. B1 hará más frecuente volver a vincular.

Nota de la galería (solo depuración): al haber varias copias de Inicio a la vez en `ScreensGalleryActivity`, la barra de estado se queda
con iconos claros sobre fondo crema y la hora se ve apagada en las capturas de la galería; en la app real, con una sola Inicio, el efecto
pone y restaura el valor como debe (`app-real-inicio-barra-clara.png`, `app-real-permisos.png`).

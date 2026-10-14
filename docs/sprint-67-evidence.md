# Sprint 67 — evidencia

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-s67`, rama `sprint-67-casos-reales`. Emulador `Pixel_8` (Android 16), build de debug, cuenta supervisada del dueño ya vinculada.
Capturas y registros en `docs/android-redesign/supervisado-v2/s67/`.

## Builds sin caché y pruebas por tarea

`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` y los tests de UI (`connectedDebugAndroidTest`), ejecutados por un `verifier` distinto del escritor, con
`:app:testDebugUnitTest` ejecutada (no UP-TO-DATE):

| Tarea | Resultado |
|---|---|
| T2 (bienvenida) | 202 tests unitarios, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` completo: **137 tests, 0 fallos** (con los 8 de `WelcomeScreenTest`) |
| T3 (aviso de batería) | BUILD SUCCESSFUL, 208 tests unitarios, 0 fallos, lint 0 errores; tests de UI del paquete `ui` en dos tandas: **132 tests, 0 fallos** |
| T4 («Se desactivó un permiso») | BUILD SUCCESSFUL en 2m 04s, 220 tests unitarios, 0 fallos, lint 0 errores; tests de UI en dos tandas: **137 tests, 0 fallos** (con los 5 de `PermissionRevokedScreenTest`) |

Los 13 tests instrumentados de `core` (`JsonExtTest`, `RulesCacheStoreTest`, `PendingRuleEventStoreTest`, sin Compose) no se repitieron por tandas; pasaron en la corrida completa de T2 y los cubre el CI.

## Incidentes de entorno (no del código), y qué se aprendió

1. **«System has crashed» en la primera corrida de UI de T2**: Android murió en el emulador con ~2 GB de RAM libres del equipo. Se repitió.
2. **Emulador bloqueado por su PIN tras un `adb reboot` mío** (`RUNNING_LOCKED`): con el usuario bloqueado el sistema no resolvía las actividades de la app y 124 tests Compose fallaron con «Unable to resolve
   activity». Se diagnosticó fuera de Gradle (`am instrument`, `pm dump`, `dumpsys user`) y se resolvió desbloqueando a mano. **Lección: no reiniciar el emulador con `adb reboot`; comprobar `RUNNING_UNLOCKED` antes de los tests de UI.**
3. **El emulador se cayó solo a media corrida de T3** (Windows registró un error de aplicación de `qemu-system-x86_64.exe`, módulo `qemu`; no fue la RAM, que entonces eran ~3 GB libres). Se reabrió con `-gpu swiftshader_indirect`
   (renderizado por software, solo una opción de arranque) y los tests se partieron en dos tandas. El único test que falló en esa caída, `MoreSettingsScreenTest.cancellingTheSignOutChangesNothing`, pasa al repetirlo.
4. **La primera prueba G-19 de S67 parecía fallar y era mi guion.** Los `ServiceRecord` cambiaron y los latidos se duplicaban (`g19-intento1-guion-erroneo.log`). Se investigó con el registro de eventos del sistema: cada `am start -n
   MainActivity` crea una **instancia nueva** de la actividad (`wm_create_activity`), con su shell y sus servicios; un usuario real vuelve por el lanzador o Recientes, que reutiliza la instancia. Experimento de control: con el intento del
   lanzador (`monkey … -c android.intent.category.LAUNCHER`) la actividad y los dos servicios son idénticos tras tres idas a Inicio del sistema. Se corrigió el guion y se repitió. **Lección: los guiones G-19 deben volver por el intento del lanzador; con `am start -n`
   el resultado depende de qué intento tenga la tarea como raíz, y las pruebas G-19 de S64 a S66 (que usaban `am start -n` y sí mostraron servicios estables) eran más frágiles de lo que parecían, aunque sus resultados eran correctos.**

## T5 — emulador

### «Se desactivó un permiso» con el permiso quitado de verdad

Con `adb shell appops set com.netprotect.app SYSTEM_ALERT_WINDOW deny` y volviendo a la app (`app-real-permiso-desactivado.png`): aparece «Se desactivó un permiso» con «Mostrar sobre otras apps», el chip «Pendiente» y el texto «se quitó en Ajustes de Android. Mientras tanto, es posible
que NetProtect no pueda mostrar la pantalla de bloqueo.», **sin mencionar al tutor** (la ubicación, superponer y el administrador no viajan como «revocado»; solo el acceso al uso). «Activar de nuevo» abre el Detalle de ese permiso, pendiente
(`app-real-activar-de-nuevo-detalle.png`); al volver a la app el aviso **no se repite**; y al restaurar el permiso (`appops … allow`) el Detalle pasa solo a «Configurado» (`app-real-permiso-recuperado.png`). Permiso restaurado al terminar.

### Revisión visual a letra 2.0 (galería)

Bienvenida (pasos 1 y 2), el aviso de batería en Inicio y la pantalla de batería se leen sin cortar y sin partir palabras por letras (`galeria-bienvenida-200.png`, `galeria-inicio-bateria-200.png`). En Inicio real el emulador sí optimiza la batería de la app,
así que la fila «La batería puede detener NetProtect» aparece de verdad (`app-real-inicio-con-aviso-bateria.png`).

### Prueba G-19 — navegar por las rutas nuevas no reinicia servicios

Guion en `g19-serviceRecords.log`. Durante 3 min 13 s se repitió 5 veces: Batería → Atrás → Permisos → Detalle → Atrás → Atrás → Estado → Atrás → Privacidad → Atrás → Inicio del sistema → volver por el lanzador.

```
T0 12:18:10 UTC  ServiceRecord{4514b5a … RuleEnforcementService}    createTime=-50s865ms  lastStartId=1
                 ServiceRecord{d2eb18b … LocationReportingService}  createTime=-50s860ms  lastStartId=1   pid 8643
T1 12:21:23 UTC  ServiceRecord{4514b5a … RuleEnforcementService}    createTime=-4m4s69ms  lastStartId=1
                 ServiceRecord{d2eb18b … LocationReportingService}  createTime=-4m4s64ms  lastStartId=1   pid 8643
```

Mismos `ServiceRecord`, mismo proceso y `lastStartId=1`. Backend (`backend-heartbeat-y-b1.log`): latidos a las **12:18:20, 12:19:20, 12:20:20 y 12:21:21** (uno por minuto) y una consulta B1 `GET /devices/me` por vuelta, sin ningún 5xx ni `Traceback`.

## Pendiente (declarado)

- **Galaxy S25 FE (One UI), lo que solo puede hacer una persona:** TalkBack y Accessibility Scanner en todas las pantallas nuevas; letra al 200 % en el teléfono real; «quitar animaciones»; los pasos de batería (el aviso puede dar falsos positivos o negativos porque
  «Sin restricciones» puede no coincidir con la lista blanca estándar); quitar cada permiso en Ajustes y recuperarlo (aquí solo se probó superponer, con `appops`; la ubicación mata el proceso en Android y se cubre por el `checkLink`, sin probar).
- **No probado:** la bienvenida con un vínculo nuevo real (necesita un código de vinculación del panel web), y el respaldo `appDetailsIntent` de batería.
- **Límites declarados:** cerrar sesión sin «Desvincular» no limpia la bandera de la bienvenida; si el permiso se quita mientras se ve la bienvenida o hay un consentimiento pendiente, el aviso se pierde; si se quitan varios a la vez se avisa del primero.
- **Prueba con cinco personas** (`05_PRUEBA_USUARIOS.md`): fila aparte «tras S67».
- **Tutor real desde el panel web:** vista remota de punta a punta, «Atrás» y la caducidad (S65), desvincular (S64).

## T6 — revisión de seguridad (`security-reviewer`, Opus) y barrido global

**S67** (`origin/android-redesign..HEAD`): sin hallazgos ALTA ni MEDIA; dos BAJA, arregladas:

1. El aviso de «permiso desactivado» del administrador de dispositivo no decía que el tutor se entera, a diferencia del acceso al uso. `NetProtectDeviceAdminReceiver.onDisableRequested` llama a `TamperReportWorker.reportUninstallAttempt` y el diálogo de Android ya avisa
   («tu tutor recibirá una alerta»). Nota nueva: «Al desactivarla, NetProtect avisa a tu tutor. Actívala de nuevo para volver a estar protegido.». Ubicación y superponer siguen sin mencionar al tutor (nada los reporta como quitados); un test lo fija.
2. El `catch` de `ActivityNotFoundException` de `onOpenBatterySettings` solo cubría el primer `startActivity`; el respaldo (ajustes de la propia app) queda envuelto en `runCatching`.

Confirmado por el revisor: el paso 2 de la bienvenida sale de `privacySections`; solo `usage_access_granted` genera `PERMISSION_REVOKED` (`backend/app/services/tamper.py:54-55`); `WelcomeStore` guarda solo el deviceId y `PermissionSnapshotStore` el deviceId y nombres de enum, ambos `MODE_PRIVATE`,
borrados en `enterUnlinked()` y **fuera de las copias de seguridad** (`allowBackup=false` y `data_extraction_rules.xml`); cerrar sesión no los borra pero otra cuenta recibe otro `device_id` (el backend lo asigna por `supervised_user_id` y `device_instance_id`, `pairing.py:205-222`), así que `read()` devuelve null y la bienvenida se muestra;
las pantallas nuevas no lanzan Intents; el aviso solo se calcula en `Linked`, no tapa `Unlinked` ni el consentimiento; ninguna clave de efecto cambia.

**Barrido global** (`origin/main...HEAD`, 384 archivos): el único permiso nuevo del manifiesto es `ACCESS_NETWORK_STATE`; ningún componente exportado nuevo; `allowBackup=false`; sin `Bearer `, claves ni URLs embebidas; sin `Log`, `println` ni `Timber` añadidos; **ningún cambio en `backend/app`** (solo `backend/tests/test_realtime_integration.py` y
`frontend/src/components/RemoteViewPanel.tsx`), así que la autorización de las rutas queda intacta; `ReconnectingRealtimeChannel` y `TokenStore` sin cambios; el `PendingIntent` de `ScreenShareService` es inmutable y explícito; los textos de privacidad son coherentes entre Privacidad, bienvenida, Estado y notificaciones (latido cada minuto, apps cada 5 min,
ubicación aproximadamente cada 15 min, sin retención ni «no controla»).

Verificación posterior: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` → BUILD SUCCESSFUL en 1m 54s, 220 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` de `PermissionRevokedScreenTest`, `WelcomeScreenTest` y `BatteryScreenTest` → 18 tests, 0 fallos.

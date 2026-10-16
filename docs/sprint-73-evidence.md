# Sprint 73 — Evidencia

Rama `sprint-73-bloqueo-segundo-plano`. Comandos ejecutados de verdad, con su salida.

## T1, T1b y T2 — compilación y tests JVM

Ejecutado en Windows (Git Bash) desde `mobile/`, sobre `ea133a7` (T1 `1392705`, T2 `b2f3e82`, T1b `ea133a7`), con
`--rerun-tasks` para que nada salga de la caché:

```
$ ./gradlew compileDebugKotlin testDebugUnitTest --console=plain --rerun-tasks
...
> Task :app:compileDebugKotlin
...
> Task :app:testDebugUnitTest
...
BUILD SUCCESSFUL in 35s
28 actionable tasks: 28 executed
$ ./gradlew --stop
Stopping Daemon(s)
1 Daemon stopped
```

Sin avisos del compilador (`grep -c "^w:"` sobre la salida: `0`).

Resultados leídos de `app/build/test-results/testDebugUnitTest/`:

```
<testsuite name="com.netprotect.app.core.rules.VisitRecheckTest" tests="6" skipped="0" failures="0" errors="0" ...>
<testsuite name="com.netprotect.app.core.rules.VisitTrackerTest" tests="11" skipped="0" failures="0" errors="0" ...>
suites 36 tests 281 skipped 0 failures 0 errors 0
```

Antes de T1b el total era 275 tests en 35 suites (tras T1 y tras T2, ambas en verde); los 6 de `VisitRecheckTest`
llevan a 281. `VisitTrackerTest` incluye la secuencia real del Redmi (Facebook → inicio → Facebook en una misma
ventana, con y sin los `PAUSED`), la navegación entre actividades de la misma app, el propio paquete y el lote vacío.

## Fuentes oficiales consultadas (T2/T3)

- <https://developer.android.com/about/versions/15/behavior-changes-15>: desde `BOOT_COMPLETED` no se pueden lanzar FGS
  `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` ni `microphone`
  (`ForegroundServiceStartNotAllowedException`); `specialUse` y `location` no están en la lista.
- <https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>: excepciones a la
  restricción de arranque en segundo plano (arranque, `MY_PACKAGE_REPLACED`, batería sin optimizar, `SYSTEM_ALERT_WINDOW`
  con ventana visible en Android 15) y la restricción *while-in-use*, cuyas excepciones no incluyen el arranque.

## T5 — endurecimiento: tests reales

### Android (JVM)

Ejecutado en Windows (Git Bash) desde `mobile/`, sobre `1ff8458`, sin caché:

```
$ ./gradlew compileDebugKotlin testDebugUnitTest --console=plain --rerun-tasks
...
BUILD SUCCESSFUL in 31s
28 actionable tasks: 28 executed
$ ./gradlew --stop
```

Sin avisos del compilador (`grep -c "^w:"`: `0`). Resultados leídos de `app/build/test-results/testDebugUnitTest/`:

```
<testsuite name="com.netprotect.app.core.auth.TokenSessionTest" tests="15" skipped="0" failures="0" errors="0"
<testsuite name="com.netprotect.app.core.network.DeviceGoneTest" tests="4" skipped="0" failures="0" errors="0"
<testsuite name="com.netprotect.app.core.rules.VisitRecheckTest" tests="6" skipped="0" failures="0" errors="0"
<testsuite name="com.netprotect.app.core.rules.VisitSeedAndEventWindowTest" tests="8" skipped="0" failures="0" errors="0"
<testsuite name="com.netprotect.app.core.rules.VisitTrackerTest" tests="11" skipped="0" failures="0" errors="0"
suites 38 tests 295 skipped 0 failures 0 errors 0
```

281 → 295: 2 de `TokenSessionTest` (un fallo pasajero del Keystore no termina la sesión y se recupera tras la espera;
cerrar sesión funciona con el Keystore ilegible), 4 de `DeviceGoneTest` (solo el 404 `device_not_found` es fin de
vínculo; qué fallo de la cola se descarta, se reintenta o termina el vínculo) y 8 de `VisitSeedAndEventWindowTest`
(siembra al arrancar; lecturas solapadas sin duplicar un `RESUMED` ni aplicar fuera de orden un evento tardío).

### Backend (integración en contenedor)

Proyecto de compose aislado y sin puertos (`-p np-s73-test`, servicios `db redis backend`), sin tocar
`netprotect-dev`:

```
$ docker compose -p np-s73-test -f compose.test.yaml build backend migrate
$ docker compose -p np-s73-test -f compose.test.yaml run --rm migrate
$ docker compose -p np-s73-test -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
backend-1  | 358 passed, 4 warnings in 40.84s
$ docker compose -p np-s73-test -f compose.test.yaml run --rm --no-deps backend ruff check --no-cache app tests alembic
All checks passed!
```

La primera pasada dio `1 failed, 357 passed`: `test_a_geofence_of_an_unlinked_tutor_is_not_evaluated_until_they_link_again`
esperaba un `ENTER` después de revincular, pero el informe «fuera» enviado sin vínculo ahora se rechaza, así que la
última posición aceptada seguía dentro; la prueba ahora comprueba el `EXIT` al salir tras revincular (misma
intención: la geovalla se evalúa de nuevo). `ruff` encontró una línea de 104 caracteres en la prueba nueva,
corregida antes del commit (solo cambió un docstring).

Las pruebas nuevas y las ajustadas, en detalle:

```
$ docker compose -p np-s73-test -f compose.test.yaml run --rm backend pytest -p no:cacheprovider \
    tests/test_devices_integration.py tests/test_location_integration.py \
    tests/test_geofence_ownership_integration.py -v -k "unlink or tutor_leaving or relinks or link_again"
tests/test_devices_integration.py::test_an_unlinked_device_drops_off_the_list PASSED
tests/test_devices_integration.py::test_an_unlinked_supervised_account_gets_404_from_devices_me PASSED
tests/test_devices_integration.py::test_devices_me_drops_a_tutor_after_they_unlink PASSED
tests/test_devices_integration.py::test_after_an_unlink_every_device_write_is_the_uniform_404 PASSED
tests/test_devices_integration.py::test_after_an_unlink_the_device_can_still_learn_it_and_link_again PASSED
tests/test_devices_integration.py::test_one_tutor_leaving_does_not_cut_off_a_device_another_tutor_still_supervises PASSED
tests/test_location_integration.py::test_a_tutor_who_relinks_does_not_see_what_arrived_in_between PASSED
tests/test_geofence_ownership_integration.py::test_a_geofence_of_an_unlinked_tutor_is_not_evaluated_until_they_link_again PASSED
================= 8 passed, 46 deselected, 1 warning in 2.48s ==================
```

Control negativo: con el `deps.py` anterior (`git show fc63c1e:backend/app/api/deps.py`, imagen reconstruida) la
prueba nueva falla como debe, y se restauró el archivo:

```
>           assert response.status_code == 404, (path, response.text)
E           assert 200 == 404
1 failed, 25 deselected, 1 warning in 1.00s
```

### No comprobado en T5

Nada del comportamiento en ejecución en un teléfono: la siembra al reiniciar, el fin del vínculo con la app cerrada
(SyncWorker, ubicación, bucle), el vigilante sin red, la reconexión del canal, el redibujo tras quitar el sistema la
superposición y el `forgetDevice` al cerrar sesión. Tampoco `assembleDebug`, `lintDebug` ni `security-reviewer`.
Queda para T4.

## T4 — verificación y prueba en emulador (16/10/2026)

Entorno: emulador Pixel 8, Android 16 (API 36), APK `assembleDebug`, backend y base de desarrollo en contenedor, dispositivo
supervisado vinculado por una persona con su cuenta de Google. Las reglas de prueba se insertaron en `app_rules` por `psql`
y se borraron después; no se usó el panel del tutor (la extensión de Chrome no estaba conectada), así que el aviso
instantáneo `rules_changed` **no** se probó. Cada medida se tomó con `dumpsys window | grep mCurrentFocus` cada 5 s.

### Compilación, tests, lint y revisión (verifier y security-reviewer)

```
$ cd mobile && ./gradlew test assembleDebug lintDebug --rerun-tasks      # 114 de 114 tareas ejecutadas
BUILD SUCCESSFUL   testDebug/testRelease/testMinified: 322 tests, 0 fallos, 0 errores, 0 omitidos
lint: 0 errores, 30 avisos (15 UseKtx, 9 GradleDependency, 3 NewerVersionAvailable, 1 DataExtractionRules,
      1 AGP, 1 InlinedApi en LocationPermission.kt:48: constante de texto, sin efecto en ejecución)
```

`security-reviewer` sobre T5 (`main...HEAD`): sin hallazgos. Sobre T6/T7: 3 hallazgos, corregidos en `81a6de9` y
reverificados (mismo comando, 322 tests, sin avisos de lint nuevos): (1) el envío de segundo plano solo capturaba
`Exception` y un `Error` habría cancelado el scope del servicio y apagado el bloqueo; (2) el texto previo a «Permitir
todo el tiempo» daba a entender que la ubicación era lo único compartido y hablaba de un solo tutor; (3) el diálogo
sobrevivía a una desvinculación. El primero se comprobó leyendo el código (`scope` es un `Job` normal); **no hay test
automático** que fuerce un `Error` en el envío.

### Resultados en ejecución

| Caso | Medida |
|---|---|
| Reentrada a YouTube bloqueada tras «Ir al inicio» a 1 s, con tecla Inicio al instante y a 8 s | foco en la ventana de bloqueo en los tres; un evento `BLOCK` por visita en `app_rule_events` |
| Regla `BLOCK` insertada con Chrome ya abierto | bloqueada a los 5 s; al borrarla, desbloqueada a los 46 s |
| Regla `SCHEDULE` 00:00–23:59 con Chrome abierto | bloqueada a los 52 s |
| Regla `DAILY_LIMIT` de 1 min con Chrome abierto (más de 1 min de uso hoy) | bloqueada a los 62 s (ciclo de refresco + muestreo de 5 s) |
| NetProtect cerrada con Atrás y deslizada en Recientes | sin `MainActivity` ni tarea; mismo pid y los dos servicios vivos; YouTube se bloquea |
| `kill -9` del proceso (APK anterior a T6/T7) | `RuleEnforcementService` renace en ~2 s; **la ubicación no renace** (hueco A) |
| Reinicio del teléfono, tras desbloquear el PIN (APK anterior a T6/T7) | `BootReceiver` arranca el proceso y el servicio de reglas con 0 actividades; YouTube se bloquea. La ubicación no arranca |
| Latido con la app cerrada (APK anterior a T7) | último latido parado más de 6 min (hueco B) |
| APK con T6/T7: `adb install -r` sin abrir la app | el proceso y `RuleEnforcementService` se relanzan solos (`MY_PACKAGE_REPLACED`) |
| Latido con la app cerrada (APK con T7) | el backend ve latidos a las 15:59:05, 16:00:22, 16:01:38 y 16:02:54 (~76 s) |
| Flujo real de T6: Permisos → «Permitir todo el tiempo» → explicación → Continuar → Ajustes → «Allow all the time» | la pantalla de Inicio pasó de «3 de 4» a completa; al volver arranca `LocationReportingService` |
| T6: app cerrada + `kill -9` con el permiso concedido | renacen **los dos** servicios (`isForeground=true`, tipos `specialUse` y `location`) sin abrir ninguna pantalla; una ubicación llega al backend 6 s después (16:04:52); YouTube se bloquea |
| Reinicio del teléfono con el permiso de fondo concedido, tras desbloquear el PIN | `BootReceiver` arranca el proceso (16:12:11) y ambos servicios están en primer plano con 0 actividades; el backend recibe un latido (16:12:14) y una ubicación (16:12:18); YouTube se bloquea. Android registra `Foreground service started from background can not have location… access` para los dos servicios, y aun así la ubicación llega: la concede el permiso de fondo |

### No comprobado

- **HyperOS/MIUI** (Redmi Note 13): el emulador no reproduce cómo mata el sistema los procesos al deslizar en Recientes
  ni la exigencia de «Inicio automático» y batería «Sin restricciones». Es el único sitio donde el límite documentado
  puede confirmarse.
- El aviso instantáneo `rules_changed` del panel (la medida de 5 s con `BLOCK` se explica por el refresco periódico, no
  por ese aviso).
- Que el panel del tutor muestre «Conectado» con la app cerrada (solo se midió lo que recibe el backend).
- La sincronización de apps y uso con la app cerrada (cada ~5 min 15 s): no se midió.
- Un `Error` en el envío de segundo plano (sin test automático, ver arriba) y los tests `androidTest` de Compose.
- Con el PIN puesto, tras un reinicio Android no entrega `BOOT_COMPLETED` hasta el primer desbloqueo: la protección
  vuelve entonces, no antes.

## Cierre del sprint — verificación completa y revisión de seguridad (16/10/2026)

Hecho por el `verifier` (modo completo; backend en un proyecto de compose aislado `np-s73-test`, sin tocar el stack
`netprotect-dev` que usaba el emulador) sobre `HEAD` limpio:

```
$ docker compose -p np-s73-test -f compose.test.yaml build backend migrate        OK
$ docker compose -p np-s73-test -f compose.test.yaml run --rm migrate             exit 0 (hasta f2b6c9d1e4a7)
$ docker compose -p np-s73-test -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
358 passed, 0 failed, 0 skipped    (4 avisos de pytest, ajenos a la app: 2 StarletteDeprecationWarning, 1 PytestCacheWarning)
$ docker compose -p np-s73-test -f compose.test.yaml run --rm --no-deps backend ruff check --no-cache app tests alembic
All checks passed!
$ cd mobile && ./gradlew test assembleDebug lintDebug --rerun-tasks --console=plain   # 114 de 114 tareas ejecutadas
BUILD SUCCESSFUL   testDebug/testMinified/testRelease: 322 tests, 0 fallos, 0 errores, 0 omitidos
lint: 0 errores y 30 avisos, ninguno nuevo (15 UseKtx, 9 GradleDependency, 3 NewerVersionAvailable,
      1 DataExtractionRules, 1 AGP, 1 InlinedApi en LocationPermission.kt:48)
```

No se corrió el frontend (no se tocó) ni `connectedAndroidTest`.

`security-reviewer` sobre `main...HEAD` (lectura del diff; no ejecutó pruebas): **0 ALTA, 0 MEDIA, 4 BAJA**; nada bloquea el
cierre. Las cuatro van al backlog (`docs/tasks.md`):

- **B-18** — el diálogo previo a «Permitir todo el tiempo» puede abrirse tras una desvinculación si esta ocurre mientras
  Android muestra el cuadro de ubicación (`SupervisedShell.kt:283` y `:674`); `81a6de9` solo cerró el diálogo ya abierto.
  Sin fuga de datos.
- **B-19** — el `launch` del bucle principal de `RuleEnforcementService` y su ciclo capturan solo `Exception`. Corrección
  propia: durante el sprint se dijo que un `Error` «apagaría el bloqueo en silencio». El revisor lo matiza y es lo más
  probable: sin `SupervisorJob`, el `Error` cancela el scope y llega al manejador de excepciones de Android, que cierra el
  proceso, y `START_STICKY` lo reinicia. Es una interrupción de unos segundos, no un apagado silencioso. Ese
  comportamiento no se midió. La lección de `.claude/rules/android.md` se reescribió con esa precisión.
- **B-20** — `TokenStore.isPermanentReadFailure` solo reconoce cuatro tipos de excepción del Keystore; otro tipo
  (según el OEM) se trataría como transitorio para siempre y la persona solo saldría cerrando sesión. No se borra nada de
  forma indebida.
- **B-21** — tras una desvinculación el dueño aún puede abrir el WebSocket y pedir `GET /webrtc-config` (credenciales TURN
  efímeras, sin fuga); `docs/security-baseline.md` y `.claude/rules/backend.md` dicen que «todo lo que el teléfono envía o
  lee» recibe el 404, lo cual es impreciso (relacionado con B-17).

El revisor confirmó que las tres correcciones de `81a6de9` son correctas, y dejó dicho que no pudo comprobar: el
comportamiento de Android que depende de la plataforma (excepciones exactas del Keystore por OEM, si
`MY_PACKAGE_REPLACED` está exento de la restricción de arrancar servicios en primer plano) ni HyperOS.

La revisión por recibos de la rama (46 archivos, 3243 líneas) no cupo en el presupuesto de contexto de la herramienta
(`lens_context_budget_exceeded`, sin autoridad de revisión creada) y el dueño decidió omitirla (`declined_this_candidate`).

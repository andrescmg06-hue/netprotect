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

## Pendiente (T4)

No ejecutado en este paso: `assembleDebug`, `lintDebug`, `security-reviewer` y la prueba en el Redmi Note 13
(re-entrada rápida a Facebook, regla creada con la app abierta, app deslizada en Recientes con y sin «Inicio
automático», reinicio del teléfono). Ningún comportamiento en ejecución de este sprint está comprobado todavía.

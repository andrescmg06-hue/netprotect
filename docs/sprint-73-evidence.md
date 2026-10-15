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

## Pendiente (T4)

No ejecutado en este paso: `assembleDebug`, `lintDebug`, `security-reviewer` y la prueba en el Redmi Note 13
(re-entrada rápida a Facebook, regla creada con la app abierta, app deslizada en Recientes con y sin «Inicio
automático», reinicio del teléfono). Ningún comportamiento en ejecución de este sprint está comprobado todavía.

# Sprint 19 — Evidencia

Sprint puramente Android: sin cambios de backend ni de panel web, así que no aplica la suite de
Docker de otros sprints — la verificación es la compilación y las pruebas de Android, con el mismo
comando que corre el job `android` de CI.

## Primer intento: kapt falla contra Kotlin 2.3.21

```
$ ./gradlew.bat compileDebugKotlin --console=plain
...
> Task :app:kaptDebugKotlin FAILED
...
Caused by: java.lang.IllegalArgumentException: Provided Metadata instance has version 2.3.0,
while maximum supported version is 2.2.0. To support newer versions, update the
kotlin-metadata-jvm library.
	at androidx.room.compiler.processing.javac.kotlin.KmClassContainer$Companion.createFor(...)
	at androidx.room.processor.TableEntityProcessor...
```

Diagnóstico: `room-compiler:2.7.1` vía `kapt` usa su propio `kotlin-metadata-jvm` fijo, que no
entiende el formato de metadatos que emite Kotlin 2.3.21. Se cambió el procesador de anotaciones de
Room de `kapt` a KSP (`com.google.devtools.ksp:2.3.11` — ver `docs/sprint-19.md`, sección "Room vía
KSP, no kapt").

## Segundo intento: compila con KSP

```
$ ./gradlew.bat compileDebugKotlin --console=plain
...
> Task :app:kspDebugKotlin
> Task :app:compileDebugKotlin
...
BUILD SUCCESSFUL in 2m 28s
18 actionable tasks: 18 executed
```

## Comando real de CI: `./gradlew test assembleDebug`

```
$ ./gradlew.bat test assembleDebug --console=plain
...
> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
> Task :app:kspReleaseKotlin
> Task :app:compileReleaseKotlin
> Task :app:kspReleaseUnitTestKotlin
> Task :app:compileReleaseUnitTestKotlin
> Task :app:testReleaseUnitTest
> Task :app:test
> Task :app:assembleDebug
...
BUILD SUCCESSFUL in 2m 51s
76 actionable tasks: 58 executed, 18 up-to-date
```

Room (vía KSP) y WorkManager compilan y generan código correctamente para las variantes debug y
release, sus pruebas unitarias corren (sin pruebas nuevas propias en este sprint — nada de lo
añadido tiene lógica pura aislable de Android/Room/WorkManager que valga la pena probar con JVM
puro sin un entorno instrumentado), y el APK debug se empaqueta completo.

## No se marca como verificado

- Login real de Google (límite recurrente de todos los sprints).
- Comportamiento real en un dispositivo/emulador: caché de Room sobreviviendo un arranque en frío
  sin red, la cola de `pending_rule_events` vaciándose al recuperar conexión, `SyncWorker`
  ejecutando con la app cerrada, y la renovación de token en segundo plano evitando 401 pasados los
  15 minutos — todo se diseñó y compiló contra las APIs reales de Room/WorkManager/`AuthClient`,
  pero nada de esto se ejerció en tiempo de ejecución en esta sesión.

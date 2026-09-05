# Evidencia de verificación — Sprint 7

Fecha: 05/09/2026. Equipo: `andre`, Windows 11 Pro.

## Investigación previa (procedimiento obligatorio de la Fase C)

Antes de escribir código se investigaron, contra fuentes oficiales actuales (no memoria de
entrenamiento), los permisos `QUERY_ALL_PACKAGES` y `PACKAGE_USAGE_STATS`, y la política
anti-stalkerware de Google Play. El detalle completo con citas está en
`docs/android/capability-matrix.md`. Hallazgo que cambió el plan original: esas políticas de Play
sólo aplican si la app se **publica**; como este proyecto se instala por sideload (Android
Studio/`adb`), no bloquean nada hoy. Se verificó explícitamente que Play Protect (la protección
del propio teléfono, activa en ~185 países) no bloquea la instalación sideloaded por ninguno de
los dos permisos de este sprint — sólo por otros cuatro no relacionados.

## Migración

```text
alembic upgrade head    → 7dbb7e44e8b7 -> fedd07a82d4e, applications: device application catalog and daily usage
alembic downgrade -1    → revierte limpiamente
alembic upgrade head    → reaplicada
```

Verificado contra el PostgreSQL de desarrollo real (`compose.yaml`), no sólo el de pruebas.

## Suite completa en contenedor

```text
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 76 passed, 3 warnings in 7.99s   (primera corrida)
→ backend-1 | 76 passed, 3 warnings in 8.10s   (segunda corrida, base y Redis recreados)
→ backend-1 exited with code 0
```

14 pruebas nuevas en `tests/test_applications_integration.py`, contra PostgreSQL real. Cubren:
sincronización exitosa con conteo correcto; el tutor no puede sincronizar (404); otra cuenta
supervisada no puede sincronizar el dispositivo de otra persona (404); segundos de uso negativos
rechazados (422); listado con el uso más reciente; un extraño no puede listar (404); el propio
dispositivo supervisado no puede listarse a sí mismo con el endpoint del tutor (404); una app sin
uso reportado aparece con `latest_usage: null`; re-sincronizar el mismo paquete actualiza en vez de
duplicar; una app ausente en una sincronización posterior queda marcada como desinstalada; una app
reinstalada dentro del mismo dispositivo deja de aparecer como desinstalada; re-sincronizar el
mismo día sobrescribe el uso en vez de acumularlo; el día más reciente sincronizado es el que se
muestra como actual; y que sincronizar actualiza `device_status.last_sync_at` (dando uso real por
primera vez a una columna que existía desde el Sprint 2 sin ningún endpoint que la tocara).

## Backend: `ruff check`

```text
ruff check app tests alembic
All checks passed!
```

## Panel web

```text
npm run lint    → sin errores ni warnings (--max-warnings=0)
npm run build   → compila TypeScript, genera build de producción, 0 errores
```

## Android

```text
./gradlew compileDebugKotlin                  → BUILD SUCCESSFUL
./gradlew test assembleDebug assembleRelease  → BUILD SUCCESSFUL (103 tareas, 31 ejecutadas)
```

## Entorno de desarrollo real

Se reconstruyeron `backend`, `web` y `migrate` de `compose.yaml` (el stack persistente) y se
confirmó en caliente:

```text
GET http://localhost:8000/api/v1/health/ready → {"status":"ready", ...}
GET http://localhost:8000/openapi.json → incluye /api/v1/devices/{device_id}/applications/sync
                                           y /api/v1/devices/{device_id}/applications
GET http://localhost:3000/ → sirve el build nuevo
```

## Dos problemas reales encontrados al verificar

**1. Deprecación real en `AppOpsManager.unsafeCheckOpNoThrow`, no una falsa alarma.** El código
inicial usaba ese método (3 argumentos) para comprobar si el usuario concedió el acceso a
estadísticas de uso. Compiló, pero con advertencia real de deprecación. Se verificó en el bytecode
del SDK (`javap` sobre `android.jar` de la API 36, usando el JDK que trae Android Studio) que existe
la familia de métodos con `attributionTag` (`checkOpNoThrow` de 4 argumentos), y se migró a esa
variante pasando `null` como `attributionTag`. Recompiló sin advertencias.

**2. Reconstruir `backend` y `web` en `compose.yaml` sin reconstruir `migrate` deja el contenedor
de migración desactualizado.** Los tres servicios comparten el mismo `Dockerfile` de `backend/`
pero Docker Compose les asigna nombres de imagen independientes por servicio; reconstruir uno no
reconstruye los otros. Al usar `alembic upgrade head` directamente desde el entorno virtual local
(para verificar el ciclo upgrade/downgrade/upgrade de la migración nueva) la base de datos de
desarrollo quedó en la revisión `fedd07a82d4e`, pero el contenedor `migrate` seguía construido con
el código anterior a esa migración — sin el archivo que la define. El resultado fue
`Can't locate revision identified by 'fedd07a82d4e'` al arrancar el stack de desarrollo.
Corregido reconstruyendo también `migrate` explícitamente. Ninguna otra corrida de este sprint usó
esta secuencia incorrecta (`compose.test.yaml` sí reconstruye los tres servicios en un solo
comando, por diseño desde el Sprint 5).

## CI en GitHub Actions

Run [`33972875905`](https://github.com/andrescmg06-hue/netprotect/actions/runs/33972875905), commit
`1090dd8`, los 4 jobs en verde: `backend` (22s), `frontend` (31s), `integration` (48s), `android`
(1m39s).

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con salida real, incluidos los dos hallazgos y
sus correcciones. Limitación honesta, la misma de siempre: el flujo completo en vivo (el
dispositivo supervisado activando el permiso real en Ajustes, viendo su propia app en la lista, el
tutor viéndola aparecer en el panel web o en su app) no se probó haciendo clic real en un emulador
ni en un teléfono, porque requiere una sesión de Google iniciada por una persona — la misma
limitación documentada desde el Sprint 3. Lo que sí se verificó en su lugar: la API que ambos
consumen tiene prueba automatizada positiva y negativa para cada caso descrito arriba, el
inventario y el permiso de uso se implementaron siguiendo exactamente los mecanismos oficiales
verificados, y el entorno de desarrollo real quedó corriendo con el código nuevo para que el tutor
lo compruebe con su propia cuenta y su propio emulador.

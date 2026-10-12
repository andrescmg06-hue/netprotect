# Sprint 41 — Evidencia

Todo lo de abajo se ejecutó de verdad; los fallos del camino están incluidos.

## 1. Android — `cd mobile && ./gradlew test assembleDebug lintDebug`

```text
BUILD SUCCESSFUL in 32s
TokenSessionTest      13 tests, 0 fallos
RuleEvaluatorTest     34 tests, 0 fallos
ExampleUnitTest        1 test,  0 fallos
lint: 0 errores, 26 avisos (línea base S40: 27; desapareció CredentialManagerMisuse, ninguno nuevo)
```

Tests de `TokenSessionTest`: 20 llamantes concurrentes → 1 renovación · sin red hasta estar cerca de
caducar · token de 1 min renovado a los 30 s · reintento único tras 401 · sin bucle si el renovado
también se rechaza · un 401 de vinculación no renueva · refresh rechazado → sesión expirada · error de
red conserva la sesión · cerrar sesión durante una renovación no la revive · cancelar al llamante no
pierde el token rotado · espera tras 429 (10 llamadas → 1 intento) · el token descartado se revoca ·
mapeo a `UiError`.

`git diff` sobre `mobile/` con `Log.|println|printStackTrace|toString()`: 0 coincidencias.

## 2. Revisión de seguridad (rol `security-reviewer`)

Primera pasada: 4 hallazgos (2 MEDIA, 2 BAJA), todos corregidos — renovación cancelable que perdía el
token rotado; reintentos sin espera que agotaban el límite del backend; carrera `signOut`/renovación;
sockets conectando con token vacío o caducado. Sin hallazgos ALTA.

## 3. Backend — suite completa en contenedor (`compose.test.yaml`, `RUN_INTEGRATION_TESTS=1`)

```text
backend-1  | 276 passed, 4 warnings in 34.59s
exit=0
```

Incluye los dos tests nuevos de `test_alerts_integration.py` (5 reportes simultáneos → 1 alerta con
5 ocurrencias; duplicados heredados no rompen un reporte nuevo). `ruff check app tests`: All checks passed!

## 4. Prueba real en teléfono (Galaxy S25 FE, Android 16) con tokens de 1 minuto

Backend de desarrollo con `ACCESS_TOKEN_TTL_MINUTES=1` (override temporal, comprobado dentro del
contenedor: `TTL minutes = 1`). APK instalado por `adb`, túnel `adb reverse tcp:8000 tcp:8000`.
Login real con Google hecho por el dueño.

**Tutor** — 3 min sin tocar la app (el token caducó 3 veces), luego "Ver" en *Mi actividad*:

```text
POST /api/v1/auth/refresh                        200 OK
GET  /api/v1/users/me/audit?limit=50&offset=0    200 OK
```

La auditoría en pantalla mostró `TOKEN_REFRESH · 28/09/2026, 9:52:25 p. m.`. Ningún 401 de la app.

**Supervisado**, primer intento (21:56–21:59):

```text
4 POST /auth/refresh                     200 OK
4 POST /devices/2219e3a5…/heartbeat      500 Internal Server Error   ← MultipleResultsFound (alerts.py)
```

La renovación funcionaba; el 500 era el bug de alertas (ver `docs/sprint-41.md`).

Segundo intento tras el arreglo (22:04–22:07): **no llegó nada** — la pantalla se apagó
(`mWakefulness=Dozing`) y Samsung congeló la app (sin servicio en primer plano porque no había
*Acceso a uso de apps*). Prueba inválida, repetida con `svc power stayon usb`.

Tercer intento (22:09–22:12), pantalla encendida:

```text
3 POST /auth/refresh                     200 OK
4 POST /devices/2219e3a5…/heartbeat      200 OK
tracebacks: 0
```

Las `GET /devices/2219e3a5…/alerts → 401` que aparecen en las tres ventanas no son del teléfono (el
supervisado no consulta alertas): es el panel web del tutor con un token caducado.

Al terminar: backend de vuelta a 15 min (`TTL minutes = 15`), `stayon` desactivado, túnel quitado.

## 5. No verificado

- `RuleEnforcementService` en real (sin permiso de acceso a uso).
- Tests instrumentados de Android (no los hay todavía; D-13 los añade en S42).

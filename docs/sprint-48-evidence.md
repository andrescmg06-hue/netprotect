# Sprint 48 — Evidencia

## 1. Backend — `make test` (contenedor)

```text
backend-1  | 285 passed, 4 warnings in 52.25s
ruff check --no-cache app tests alembic → All checks passed!   (primero 3 × E501 en los tests nuevos, corregidos)
```

Tests nuevos (`backend/tests/test_devices_integration.py`):
- `test_devices_me_with_two_installs_answers_for_the_one_asking`: reproduce B-01 con una instalación vieja
  desvinculada y otra nueva vinculada. Cada id de instalación obtiene su dispositivo.
- `test_devices_me_without_an_install_id_prefers_the_linked_device`.
- `test_devices_me_for_an_unknown_install_is_404`.
- `test_devices_me_never_answers_with_another_accounts_install`.

**Caso real:** la cuenta andrescmg06 tenía dos filas (`f911d720…`, de 2026-09-05, sin tutor; `da5e6b92…`, de
2026-09-24, vinculada). Tras el arreglo, con el backend de desarrollo reconstruido:

```text
GET /api/v1/devices/me?device_instance_id=5d8c17b5-4f2b-4106-be3e-eb793b2bb9ed HTTP/1.1" 200 OK
```

## 2. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
Parte de Claude (7a1bbd9): BUILD SUCCESSFUL in 5m 9s · JVM 149, 0 fallos (5 nuevos) · instrumentados 96, 0 fallos · lint 0 errores, 24 avisos
Tras DeepSeek + d97b8c1:   BUILD SUCCESSFUL in 5m 30s · instrumentados 116, 0 fallos (20 nuevos) · lint 0 errores, 24 avisos
```

## 3. Prueba de servicios: 3 minutos, 17 idas y vueltas Vinculado ↔ Permisos (emulador supervisado, cuenta real)

Servicios antes y después (`adb shell dumpsys activity services com.netprotect.app`):

```text
antes:   ServiceRecord{9b1af29 … RuleEnforcementService}   createTime=-1m9s7ms   lastStartId=1
         ServiceRecord{6bc42ae … LocationReportingService} createTime=-1m9s1ms   lastStartId=1
después: ServiceRecord{9b1af29 … RuleEnforcementService}   createTime=-4m37s748ms lastStartId=1
         ServiceRecord{6bc42ae … LocationReportingService} createTime=-4m37s741ms lastStartId=1
```

Son los mismos objetos (mismo id), con el tiempo de vida acumulado y sin un segundo arranque: ninguno se detuvo.

Backend durante la prueba:

```text
15:37:36 POST …/heartbeat 200   15:37:37 GET …/rules/active 200
15:38:36 POST …/heartbeat 200   15:38:37 GET …/rules/active 200
15:39:37 POST …/heartbeat 200   15:39:37 GET …/rules/active 200
15:40:37 POST …/heartbeat 200   15:40:38 GET …/rules/active 200
WebSocket …/ws [accepted]: solo al arrancar (15:36:36), ninguna reconexión durante la navegación
```

El latido y el sondeo de reglas del servicio de bloqueo siguieron cada minuto, y el socket de vista remota no se
reabrió.

**Ubicación:** `device_location_reports` no tiene filas de este dispositivo, ni de antes ni de ahora.
`dumpsys location` muestra que el emulador solo tiene una posición GPS, y la app usa exclusivamente
`NETWORK_PROVIDER` (ubicación aproximada, Sprint 13). El servicio sigue vivo (la misma instancia, arriba), pero no
tiene nada que reportar. Pendiente en el teléfono real.

## 4. Permisos retirados y restaurados desde fuera de la app (`appops`, sin matar el proceso)

```text
antes:          RuleEnforcementService + LocationReportingService
retirar SYSTEM_ALERT_WINDOW y GET_USAGE_STATS → HOME → volver
                "2 permisos pendientes"; solo queda LocationReportingService
restaurar ambos → HOME → volver
                "Todos los permisos están configurados"; RuleEnforcementService otra vez en marcha
```

`connectedDebugAndroidTest` reinstala la app y detiene su proceso (servicios en 0 al terminar). Al reabrir la app,
ambos servicios arrancan de nuevo, comprobado.

## 5. Revisión del trabajo de DeepSeek

- `git diff --stat -- …/SupervisedShell.kt` vacío: el shell sigue intacto.
- `git status` muestra solo archivos del encargo.
- El grep de `Build.`, `LocalContext`, `Intent`, `.launch`, `Desvincular`, `precisa` y colores literales solo
  encuentra «No se usa la ubicación precisa.», que estaba permitida.
- **Capturas** (`%TEMP%\sprint48-gallery\`, con nombres de archivo desalineados respecto a su contenido) y **pantalla
  real «Dispositivo vinculado»** en el emulador supervisado, comparadas con los mockups 12–14.
- **Hallazgo de DeepSeek:** `ListRow` no permite título en rojo. Se aceptó.
- **Corrección de Claude:** botones de ancho completo en `PermissionCard`.

## 6. Revisión de seguridad (hecha por Claude)

- **Código de vinculación:** no va a logs ni a disco. El shell lo borra al vincular y el límite de intentos sigue
  siendo del backend.
- **Pantallas:** no lanzan `Intent`s ni permisos, ni tocan servicios; todo pasa por el shell (comprobado por grep).
- **B-01:** la búsqueda sigue filtrada por la cuenta autenticada, y un test comprueba que el id de instalación de otra
  cuenta da 404.
- **Ningún servicio se arranca desde segundo plano:** siguen arrancando desde el efecto del shell, que solo existe con
  la actividad visible, como antes.
- **Agente:** el `security-reviewer` no estaba disponible como agente en la sesión.

## 7. No verificado

- Reportes de ubicación (necesitan el teléfono real).
- Vincular desde cero con un código de una segunda cuenta de tutor.
- Aspecto en el Galaxy S25 FE.

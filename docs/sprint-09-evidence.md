# Evidencia de verificación — Sprint 9

Fecha: 05-06/09/2026. Máquina: Windows 11 Home Single Language, Docker Desktop, emulador Android
Pixel_8 (API 36, imagen `google_apis_playstore`).

## Investigación previa (procedimiento obligatorio de la Fase C)

Antes de escribir código se verificó cómo identificar, en tiempo de ejecución, las apps que el modo
lista blanca **nunca** debe bloquear: launcher, teléfono y Ajustes. Sin eso, activar ese modo dejaría
el dispositivo sin pantalla de inicio, sin forma de llamar a emergencias y sin acceso al ajuste que
desactiva la supervisión. Detalle completo con fuentes en `docs/android/capability-matrix.md`
(sección "Sprint 9").

**Corrección de una suposición propia, encontrada al verificar**: se escribió primero el código
asumiendo que `TelecomManager.getSystemDialerPackage()` no era API pública (que existía sólo como
system API en AOSP), y se implementó un rodeo resolviendo `ACTION_DIAL`. Al comprobarlo de verdad
contra el SDK instalado, la suposición resultó **falsa**:

```text
javap -classpath $ANDROID_SDK/platforms/android-36/android.jar android.telecom.TelecomManager
  public java.lang.String getDefaultDialerPackage();
  public java.lang.String getSystemDialerPackage();
```

Ambos métodos son públicos en API 36. Se corrigió el código (y el comentario que afirmaba lo
contrario) para usar los dos, conservando además la resolución de `ACTION_DIAL` como último recurso:
este conjunto es lo que separa el modo lista blanca de un teléfono que no puede marcar a emergencias,
así que aquí la redundancia vale más que el minimalismo. Es exactamente el motivo por el que este
proyecto exige verificar en vez de asumir.

## Backend

### Migración

```text
alembic upgrade head    → 02c8782f69e6 -> bd93f41d0fa3, device default app policy and DEFAULT_POLICY rule event type
alembic downgrade -1    → revierte limpiamente (se comprobó que la columna default_app_policy desaparece de `devices`)
alembic upgrade head    → reaplicada
```

Escrita a mano, no autogenerada: Alembic no detecta cambios en `CheckConstraint`, y esta revisión
tiene que reescribir uno (`app_rule_events` acepta ahora `DEFAULT_POLICY`).

Verificado con `psql` sobre datos reales insertados (una fila vacía no ejercita una restricción, así
que se creó un usuario y un dispositivo de prueba):

```text
=== política por defecto de una fila recién creada ===
 ALLOW

=== política inválida (debe fallar) ===
ERROR:  new row for relation "devices" violates check constraint "ck_devices_default_policy_valid"

=== evento con tipo inválido (debe fallar) ===
ERROR:  new row for relation "app_rule_events" violates check constraint "ck_app_rule_events_type_valid"

=== evento DEFAULT_POLICY (debe pasar) ===
INSERT 0 1
```

Definiciones reales leídas de `pg_constraint`, no del código fuente:

```text
ck_devices_default_policy_valid => CHECK (default_app_policy = ANY (ARRAY['ALLOW','BLOCK']))
ck_app_rule_events_type_valid   => CHECK (rule_type_applied = ANY (ARRAY['ALLOW','BLOCK','DAILY_LIMIT','SCHEDULE','DEFAULT_POLICY']))
```

El `server_default='ALLOW'` es lo que garantiza el criterio 1: los dispositivos que ya existían
siguen comportándose exactamente igual que antes del sprint.

### Suite completa en contenedor

```text
docker compose -f compose.test.yaml build backend migrate
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 109 passed, 3 warnings in 79.15s
→ backend-1 exited with code 0
```

13 pruebas nuevas en `tests/test_device_policy_integration.py` (96 previas + 13 = 109): valor por
defecto `ALLOW`, la política aparece en el detalle del dispositivo, cambio a `BLOCK` y vuelta a
`ALLOW`, que cambiar de modo no toca las reglas existentes, rechazo de un valor de política
desconocido (422), las tres formas de acceso no autorizado (tutor ajeno, dispositivo supervisado,
lectura ajena — todas 404), que el dispositivo recibe política y reglas juntas en
`/rules/active`, que se puede reportar un bloqueo `DEFAULT_POLICY` y que un tipo inventado sigue
siendo rechazado, y la auditoría de `DEVICE_POLICY_CHANGED`.

**Tropiezo real, ya documentado en `CLAUDE.md` y repetido aquí porque volvió a pasar**: reconstruir
sólo `backend` no reconstruye `migrate` (son imágenes independientes). La primera corrida aplicó
migraciones hasta `02c8782f69e6` y la nueva no apareció, sin ningún error visible — sólo la ausencia
de la línea esperada. Corregido con `docker compose -f compose.test.yaml build migrate`.

### `ruff check`

```text
ruff check app tests alembic
All checks passed!
```

## Android

```text
./gradlew compileDebugKotlin testDebugUnitTest    → BUILD SUCCESSFUL, sin warnings
./gradlew test assembleDebug assembleRelease      → BUILD SUCCESSFUL en 20m 49s
```

`RuleEvaluatorTest`: **17 pruebas** (10 previas + 7 nuevas), 0 fallos. Las nuevas cubren el modo
lista blanca: una app sin regla se bloquea con motivo `DEFAULT_POLICY`; una regla `ALLOW` la aprueba;
una regla `BLOCK` sigue reportándose como `BLOCK` y no como política por defecto; `DAILY_LIMIT` y
`SCHEDULE` cuentan como aprobación mientras no se cumpla su condición (quien puso "una hora al día"
aprobó esa app durante esa hora); y una regla de otro paquete no aprueba a éste.

## Panel web

```text
npm run lint   → eslint . --max-warnings=0 → limpio
npm run build  → Compiled successfully; TypeScript sin errores; 3 páginas estáticas
```

## `/security-review`

Corrido sobre el diff completo del sprint. **Encontró un fallo real que se introdujo en este sprint
y se corrigió antes de commitear**, más dos límites que quedan documentados:

**1. Corregido — las apps protegidas se saltaban toda la evaluación, no sólo la política por
defecto.** La primera versión hacía `if (changedPackage in protectedPackages) { ... }` *antes* de
evaluar, así que una app protegida quedaba exenta de **cualquier** regla, incluido un `BLOCK`
explícito del tutor. Y el conjunto protegido no es una lista fija del sistema: incluye el marcador y
el launcher **elegidos por el usuario**. Explotación real, sin root ni conocimientos técnicos: el
supervisado instala un marcador de terceros con navegador/feed incorporado, lo pone como "app de
teléfono por defecto" en Ajustes (tres toques) y esa app queda exenta de todas las reglas, en
silencio — el panel del tutor sigue mostrando la regla como activa y no se emite ningún evento.
Corregido: las apps protegidas ahora se evalúan normalmente y sólo quedan exentas de
`DEFAULT_POLICY`; una regla escrita a propósito por el tutor sí les aplica. La propia app de
NetProtect sigue totalmente exenta (bloquearse a sí misma haría un bucle con su pantalla de bloqueo).

**2. Documentado, no corregido — el modo lista blanca falla en abierto si la primera consulta de
reglas falla.** `defaultPolicy` arranca en `ALLOW` y sólo se actualiza si la petición tiene éxito; el
servicio es `START_NOT_STICKY` y no persiste nada. Si el dispositivo arranca sin red (modo avión), se
queda con "todo permitido" mientras la notificación sigue diciendo que NetProtect está activo. El
mecanismo es heredado del Sprint 8, pero la consecuencia se **invierte**: en modo lista negra fallar
en abierto significaba "las pocas apps bloqueadas funcionan"; en modo lista blanca significa "todo
funciona", que es justo lo contrario de lo que promete este sprint. Mitigación parcial existente: el
heartbeat también se detiene, así que el panel del tutor termina mostrando el dispositivo OFFLINE
(indistinguible de un teléfono apagado). La solución real —persistir la última política y reglas
conocidas localmente— es trabajo del Sprint 19 (Offline), que ya existe en el roadmap para esto.

**3. Preexistente, no introducido aquí, pero relevante para este sprint — una cuenta supervisada
puede volverse tutora de su propio dispositivo.** `select_role` concede roles de forma acumulativa y
`redeem_pairing_code` no rechaza que quien canjea sea el mismo tutor que generó el código, así que la
cadena `POST /users/me/roles {"TUTOR"}` → generar código → canjearlo desde el propio dispositivo →
`PUT /devices/{id}/policy {"ALLOW"}` deja al supervisado salir del modo lista blanca. Ya permitía
borrar reglas desde el Sprint 8, pero en modo lista blanca borrar reglas hace el dispositivo *más*
restrictivo — cambiar la política es la palanca específica para escapar, y este sprint la pone al
alcance de esa cadena. Queda auditado en `audit_logs` (detectable a posteriori), pero no impedido. La
corrección de una línea sería que `require_tutor_of_device` rechace también cuando
`device.supervised_user_id == current_user.id`; no se aplica en este sprint porque toca el flujo de
vinculación y merece su propia verificación, no un parche apurado al cierre. Anotado para el
Sprint 21 (Seguridad integral).

## No se marca como verificado

Lo anterior son comandos ejecutados realmente con su salida real, incluida la corrección de una
suposición equivocada propia. Limitación honesta, la misma de todos los sprints anteriores: el flujo
end-to-end con login real de Google (el tutor activa el modo lista blanca desde el panel, el
dispositivo supervisado lo recibe y bloquea una app no aprobada mostrando la pantalla nueva) no se
probó, porque exige que una persona elija su cuenta en el selector del sistema. Lo que sí se
verificó en su lugar: cada criterio de aceptación del backend tiene prueba automatizada positiva y
negativa contra PostgreSQL real, y la lógica de decisión (`RuleEvaluator`) tiene prueba unitaria real
para los dos modos y sus casos límite.

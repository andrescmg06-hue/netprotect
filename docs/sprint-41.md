# Sprint 41 — La sesión sobrevive a los 15 minutos y los errores se dicen en español

Segundo sprint del rediseño Android (`docs/android-redesign/sprints/S41-sesion-y-errores.md`). Solo
Claude Code: es código de tokens. Sin cambios visuales.

## Qué se encontró al explorar (y difiere del plan)

- **G-01 era real**: el access token se fijaba al iniciar sesión y se pasaba como `String` a
  `TutorScreen`/`SupervisedScreen`; nadie lo renovaba y a los 15 min todo daba 401.
- **G-02 no era "cierre de sesión global"**: `auth.py` `refresh_tokens` no revoca la familia al
  reutilizar un refresh token, solo responde 401. El daño real era otro: `restoreSession()` borraba el
  token guardado **ante cualquier error** — perder la carrera contra un servicio que acababa de
  renovar, o simplemente abrir la app sin red, dejaba el teléfono sin sesión.
- **Hallazgo nuevo**: `LocationReportingService` recibía el token por el Intent y **nunca lo
  renovaba**; con reportes cada 15 min, desde el segundo todos fallaban en silencio (el tutor dejaba
  de recibir la ubicación). Igual el latido y la sincronización de apps de `SupervisedScreen`.
- Todos los componentes corren en un solo proceso (sin `android:process`): un `Mutex` basta.

## Qué se hizo

- **`core/auth/TokenSession`** (nuevo, sin Android, testeable) y **`TokenProvider`** (instancia de
  proceso): único dueño de los tokens.
  - *Single-flight*: N llamantes con el mismo token caducado → **una** llamada a `/auth/refresh`.
  - Renueva antes de caducar (60 s antes, o a mitad de vida si el token dura menos de 2 min).
  - Solo un 401 **del refresh** termina la sesión (`expired` → la UI vuelve al login). Un error de red
    no borra nada.
  - La renovación corre en `NonCancellable`: si se cancela quien la pidió, el token ya rotado no se
    pierde.
  - Espera creciente tras un fallo que no sea 401 (5 s → 5 min; ≥ 1 min ante 429): protege el límite
    del backend (30 renovaciones / 15 min por IP, que cuenta también los fallos).
  - `install`/`signOut`/`expire` y la escritura del resultado van bajo un lock corto: cerrar sesión
    durante una renovación no la revive, y el token descartado se revoca en el backend.
- **`authorized { token -> … }`**: ante 401 **del token** (`invalid_access_token`,
  `not_authenticated`) renueva una vez y reintenta una vez. Otros 401 (p. ej. vincular con
  `invalid_or_expired_code`) no disparan renovación.
- **`core/network/UiError`** (nuevo): Sin conexión · Sesión expirada · No existe o no tienes acceso ·
  Demasiados intentos · Error del servidor · textos propios para los códigos de vinculación.
- `AuthRepository`, `BackgroundTokenRefresher`, los servicios y los workers pasan por la sesión
  compartida (cambios de una línea). `LocationReportingService` y el primer `connect` del canal en
  tiempo real ahora usan un token válido.
- `HomeScreen`: sesión expirada → login con "Tu sesión expiró. Vuelve a iniciar sesión."; sin red al
  abrir, la sesión se conserva y se retoma sola cuando el backend responde.
- `TutorScreen`/`SupervisedScreen`: todas las llamadas por `authorized`; Renombrar, Desvincular y
  Revocar muestran el error en vez de tragarlo. **Desvincular sigue sin confirmación** (S44).
- **L-01**: sin cuenta de Google en el teléfono, mensaje en español.

## Bug del backend encontrado en la prueba real (commit aparte)

Con el supervisado en marcha, **cada latido daba 500**: `MultipleResultsFound` en
`services/alerts.py` `_record_alert`. El latido de la pantalla y el de `SyncWorker` reportaban la
misma señal a la vez, ambos veían "no hay alerta abierta" y ambos insertaban una; desde entonces la
consulta de "la alerta abierta" encontraba dos. Corregido con un `pg_advisory_xact_lock` por
(dispositivo, `dedup_key`) que vuelve atómico el comprobar-e-insertar, y la consulta ahora toma la más
antigua para que los duplicados ya existentes no sigan rompiendo. Dos tests de integración nuevos.

## Decisiones

- **La prueba real no usó `.env`**: `compose.yaml` solo pasa al backend una lista fija de variables y
  `ACCESS_TOKEN_TTL_MINUTES` no está; se usó un override temporal fuera del repo (sin secretos).
- **El teléfono llegó al backend por `adb reverse`** (APK con `NETPROTECT_API_BASE_URL=http://localhost:8000`),
  sin depender de la Wi-Fi ni de `local.properties`.
- Sin dependencias nuevas; sin tocar `TokenStore` ni la duración de los tokens.

## Pendiente / anotado

- `RuleEnforcementService` no se probó en real (el teléfono no tenía *Acceso a uso de apps*); su
  camino lo cubren los tests JVM.
- Los tokens siguen viajando en extras de Intent a servicios propios no exportados (preexistente).
- El **panel web** consulta alertas con un token caducado (401 cada minuto en el log): bug del
  frontend, anotado en `docs/tasks.md`.
- Con letra grande del sistema, los botones "Ver"/"Actualizar" de la pantalla actual se aplastan en
  vertical: para el rediseño (S42/S51).
- Login real de Google: lo hizo el dueño en su teléfono (H-01 cubierto para Android en este sprint).

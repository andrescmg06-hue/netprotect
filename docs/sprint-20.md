# Sprint 20 — Detección de manipulación

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 19) pide: "Sólo señales legítimas: pérdida de permisos,
desactivación del servicio, revocación de la VPN, silencio anómalo del heartbeat, cambio de hora e
intento de desinstalación detectable por la API oficial. Sin rootkits, sin evasión y sin
ocultamiento; se registra el evento y se alerta al tutor."

Dos piezas llevaban reservadas desde antes de que existiera un generador para ellas, y este sprint
es el que las llena, tal como anticipaban sus propios comentarios en el código:

- `DeviceStatus.ALERT` (`app/models/device.py`), reservado desde el Sprint 6. El docstring de
  `compute_effective_status` ya decía que `ALERT` sería "un hecho explícito puesto por su propio
  disparador (detección de manipulación…)", no algo derivado de la antigüedad del *heartbeat*.
- Los niveles `HIGH`/`CRITICAL` de alertas (`app/models/alert.py`), reservados en el Sprint 17 con
  su `CHECK` constraint ya puesto "para que ese sprint sólo necesite un `alert_type` nuevo, no una
  migración de nivel". Eso es exactamente lo que pasó: la migración de este sprint toca un único
  `CHECK`, el de `alert_type`.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-075 | Como tutor, quiero enterarme si el dispositivo supervisado pierde el permiso que necesita para aplicar reglas, en vez de suponer que siguen aplicándose. |
| HU-076 | Como tutor, quiero enterarme si el servicio de control de apps deja de ejecutarse en el dispositivo. |
| HU-077 | Como tutor, quiero enterarme si el dispositivo deja de reportarse durante un periodo anormalmente largo, distinto de una caída de red normal. |
| HU-078 | Como tutor, quiero enterarme si se cambia la hora del dispositivo, porque los horarios y el modo escolar se evalúan con la hora local. |
| HU-079 | Como tutor, quiero enterarme si alguien intenta desinstalar la app del dispositivo supervisado. |
| HU-080 | Como dispositivo supervisado, quiero que todo esto ocurra a la vista: sin ocultarme, sin impedir que el usuario haga lo que decida hacer. |

## Criterios de aceptación

1. Un *heartbeat* que reporta `usage_access_granted: false` genera una alerta
   `HIGH`/`PERMISSION_REVOKED` y deja el dispositivo en estado `ALERT`.
2. Un *heartbeat* que reporta `service_active: false` genera `HIGH`/`SERVICE_INACTIVE` y `ALERT`.
3. Un *heartbeat* cuyo `device_time` se aparta de la hora del servidor más de
   `device_clock_skew_alert_seconds` genera `HIGH`/`CLOCK_TAMPERING` y `ALERT`; dentro de la
   tolerancia no genera nada.
4. Un *heartbeat* que llega tras un silencio mayor que `device_heartbeat_silence_alert_seconds`
   genera `HIGH`/`HEARTBEAT_SILENCE` y `ALERT`; un silencio de los que ya producían `OFFLINE`
   (mayor que `device_offline_threshold_seconds`, mucho menor que el umbral anterior) no genera
   nada.
5. `POST /devices/{id}/tamper-events` con `event_type: "UNINSTALL_ATTEMPT"` genera
   `CRITICAL`/`UNINSTALL_ATTEMPT` y `ALERT`; sólo el dispositivo supervisado dueño puede llamarlo
   (tutor ajeno, tutor del propio dispositivo y otro supervisado reciben 404), y cualquier otro
   `event_type` es 422.
6. Un *heartbeat* que no envía ninguno de los tres campos (una APK anterior a este sprint) no
   genera ninguna alerta ni cambia el estado: "sin información" nunca se lee como "manipulado".
7. Un *heartbeat* sano posterior devuelve el dispositivo a `ONLINE`; la alerta ya generada
   permanece en la bandeja.
8. Repeticiones de la misma señal se pliegan en la alerta abierta (`occurrence_count`), y
   silenciarla la detiene — la maquinaria del Sprint 17 aplica igual a estas alertas.

## Decisiones de diseño y su motivo

### La VPN no existe en este proyecto: esa señal queda fuera, explícitamente

El enunciado menciona "revocación de la VPN". Este proyecto **no tiene ningún componente VPN**: el
bloqueo se hace con `UsageStatsManager` + pantalla propia (Sprint 8), y `VpnService` aparece en
`docs/android/capability-matrix.md` como "Evaluar Sprint 10+", pospuesto explícitamente en el
Sprint 9 y nunca adoptado desde entonces. No hay nada que revocar, así que no se implementa una
señal para ello — y no se inventa un `VpnService` sólo para poder detectar su revocación, que
habría sido construir la vulnerabilidad para poder venderle la alarma. Queda anotado aquí para
que quien retome el proyecto no lo lea como un olvido. Si algún sprint futuro adopta filtrado de
tráfico, la señal correspondiente encaja sin cambios de esquema: un `alert_type` nuevo en el mismo
`CHECK`, igual que estos cinco.

### Dónde vive cada señal: cuatro son condiciones del *heartbeat*, una es un evento

Cuatro de las cinco señales implementadas son **condiciones continuas**: o el permiso está o no
está, o el servicio corre o no corre, o el reloj cuadra o no cuadra, o el dispositivo lleva
callado demasiado o no. Todas se resuelven mirando el *heartbeat* que ya existía desde el Sprint 6
— tres campos opcionales nuevos (`usage_access_granted`, `service_active`, `device_time`) y una
comparación contra el `last_seen_at` anterior. Ningún endpoint nuevo, ninguna tabla nueva, ningún
*scheduler*: el mismo patrón "evaluar al escribir" del resto del proyecto.

El intento de desinstalación es distinto: es un **evento discreto** que ocurre una vez, en un
momento concreto, disparado por el sistema operativo y no por nuestro propio bucle. Por eso tiene
su propio endpoint (`POST /devices/{id}/tamper-events`), con la misma forma que
`POST /rule-events` y `POST /location` — y por eso es el único con nivel `CRITICAL`: las otras
cuatro son estados de los que el dispositivo puede volver por sí solo en el siguiente latido;
ésta significa que alguien está quitando la protección entera.

### El silencio anómalo se detecta cuando el dispositivo vuelve, no con un barrido

Detectar una **ausencia** es lo único que "evaluar al escribir" no resuelve solo: si nadie escribe,
nada se dispara. Las dos opciones eran un *scheduler* (que este proyecto no tiene, y que su propio
código señala como ausente a propósito: "rather than waiting on a background sweep this project
doesn't have yet") o una escritura provocada por una lectura del tutor (un `GET` que muta y hace
`commit`, un patrón que no existe en ninguna parte de este backend).

Se eligió una tercera, que no inventa ninguna de las dos: el hueco se mide **contra el
`last_seen_at` anterior en el momento en que el dispositivo vuelve a reportarse** — exactamente
como el Sprint 14 detecta entradas/salidas de geocerca comparando cada reporte de ubicación nuevo
contra el anterior, sin API de geofencing ni proceso de fondo. Costo aceptado y documentado: un
dispositivo que se calla **para siempre** (desinstalado del todo, apagado y nunca reencendido) no
produce esta alerta, porque nunca hay un latido posterior que cierre la comparación. Lo que el
tutor sí ve en ese caso es lo que ya veía desde el Sprint 6: el dispositivo en `OFFLINE` con su
`last_seen_at` real. La señal cubre el caso al que apunta el enunciado —un silencio anómalo del
que el dispositivo *vuelve*—, no un dispositivo que dejó de existir.

### `ALERT` se recalcula en cada latido, no se queda pegado

`status_row.status` pasa a `ALERT` cuando ese latido trae alguna condición de manipulación, y
vuelve a `ONLINE`/`UNLINKED` cuando no trae ninguna. No es un estado que haya que "limpiar" a
mano, y por eso este sprint **no** añade un endpoint de reconocimiento para el tutor: habría sido
inventar maquinaria que nadie pidió.

Esto es coherente con el docstring de `compute_effective_status`, no una contradicción: lo que ese
comentario prohíbe es que la *antigüedad* de un latido degrade un `ALERT` a `OFFLINE`
(y sigue prohibido — esa función no se tocó). Un latido nuevo, con información fresca de las
cuatro condiciones, es precisamente "su propio disparador" reevaluando el hecho. El registro
duradero de lo que pasó no es el semáforo del dispositivo, es la alerta en la bandeja, con su
`first_occurred_at`, su `occurrence_count` y su `read_at`.

### `null` nunca significa "manipulado"

Los tres campos del *heartbeat* son opcionales, y su ausencia se trata como "no hay información",
nunca como una condición negativa. Importa en dos casos reales: una APK anterior a este sprint
(que no envía ninguno) y un `SyncWorker` que corre antes de que `RuleEnforcementService` haya
sellado nunca su marca de vida (`service_active` = `null`, no `false`). Marcar un dispositivo
como manipulado por no tener noticias suyas sería justo el tipo de falso positivo que vuelve
inútil una bandeja de alertas.

### Cómo se sabe si el servicio sigue vivo: una marca cooperativa, y se dice que lo es

Desde Android 5.0 una app no puede preguntarle al sistema si uno de sus propios servicios sigue
corriendo desde otro proceso (`ActivityManager.getRunningServices()` está obsoleto y sólo ve el
proceso que pregunta). `EnforcementLiveness` (nuevo) es una marca de tiempo en
`SharedPreferences` que `RuleEnforcementService` sella en cada ciclo de sondeo (~3 s); quien
mande el siguiente *heartbeat* la lee y reporta `service_active`.

Es cooperativa, y eso es exactamente lo que el enunciado pide ("sin rootkits, sin evasión"):
detecta los casos ordinarios —el usuario desliza la app fuera de Recientes, la fuerza a detenerse,
o el sistema mata el proceso por memoria— y no pretende resistir a alguien decidido con acceso de
desarrollador. La matriz de capacidades ya lo decía desde el Sprint 8 y sigue siendo cierto: nada
de esto se puede *impedir* sin ser device owner; este sprint no lo impide, lo **reporta**.

Dos detalles que evitan falsos positivos, ambos verificados al escribirlos:

- La marca se sella también en `RuleEnforcementService.start()`, de forma síncrona, no sólo dentro
  del bucle. `startForegroundService()` es asíncrono y el bucle de *heartbeat* de
  `SupervisedScreen` arranca en la misma composición: sin esto, el primer latido de cada sesión
  podía leer una marca que el servicio todavía no había puesto y reportar un `SERVICE_INACTIVE`
  que nunca ocurrió.
- `stop()` la borra. Una parada deliberada (pantalla desmontada, sesión cerrada, permiso de uso
  retirado) no es manipulación, así que el siguiente latido reporta `null`, no `false`. Que maten
  el proceso nunca ejecuta `stop()` — que es justo el caso que esta señal busca.

### Intento de desinstalación: Device Administrator, no device owner

Verificado contra la documentación oficial antes de escribir código (ver
`docs/android/capability-matrix.md`, sección Sprint 20): una app **no** puede enterarse de su
propia desinstalación —sus receivers se van con el paquete— y no existe ningún *broadcast* para
ello. Lo que sí existe, y es la "API oficial" a la que apunta el enunciado, es el registro como
**Device Administrator** (`DeviceAdminReceiver`): mientras está activo, Android se niega a
desinstalar la app hasta que el usuario lo desactive primero, y esa desactivación dispara
`onDisableRequested()`, que devuelve el texto de advertencia que el propio sistema muestra en su
diálogo.

Esto **no** es device owner ni aprovisionamiento MDM: es un permiso que el usuario concede desde
una pantalla del sistema (`ACTION_ADD_DEVICE_ADMIN`) y puede retirar cuando quiera, y la matriz
de capacidades ya distinguía ambas cosas. Decisiones concretas:

- `res/xml/device_admin.xml` declara `<uses-policies>` **vacío**: esta app no cambia contraseñas,
  no bloquea la pantalla, no borra el dispositivo, no desactiva la cámara. El registro existe sólo
  por el *callback*; pedir políticas que nunca se usan sería pedir más poder del necesario.
- `onDisableRequested()` no bloquea nada (no puede: el sistema no le da veto) y no intenta
  disuadir con trucos. Encola un `TamperReportWorker` (WorkManager, la vía recomendada
  oficialmente para pasar trabajo desde un *broadcast receiver*, ya dependencia del proyecto desde
  el Sprint 19) y devuelve una advertencia en texto claro. La llamada de red no puede ir en el
  *callback*: corre en el hilo principal y debe devolver rápido, y el proceso puede desaparecer
  segundos después.
- Es **opcional**. Nada en la app depende de ello; si el usuario no lo activa, todo lo demás sigue
  funcionando igual. La tarjeta que lo ofrece en `SupervisedScreen` sigue el mismo patrón
  educativo que las de acceso a uso y ubicación, y dice sin adornos lo que hace y lo que no.

### Sin tabla de eventos propia

`AppRuleEvent`/`GeofenceEvent` existían antes que las alertas y el Sprint 17 los reutilizó. Aquí
no hay ninguna señal previa que reutilizar, y la `Alert` generada ya guarda cuándo empezó, cuántas
veces se repitió y si el tutor la leyó — suficiente historial dentro de `alert_retention_days`. Una
tabla `tamper_events` paralela habría duplicado eso sin ningún consumidor que la pidiera.

## Fuera de alcance

- **Revocación de VPN**: no hay VPN en este proyecto (ver arriba).
- **Impedir** cualquiera de estas acciones: no se puede sin ser device owner, y el enunciado pide
  registrar y alertar, no bloquear. Un usuario con conocimientos técnicos puede desactivar el
  administrador, quitar el permiso de uso y desinstalar la app; todo eso queda reportado, nada
  queda impedido.
- **Endpoint para que el tutor "limpie" el estado `ALERT`**: innecesario, porque el estado se
  recalcula solo en cada latido (ver arriba).
- **Detección de root, emulador, depuración USB o reempaquetado**: no están en el enunciado, y
  cada una es una carrera armamentista con falsos positivos propios. `HIGH` sigue teniendo sitio
  para ellas si algún sprint futuro las justifica.
- **Notificación push de la alerta al tutor**: la bandeja se revisa al abrir el panel/la app, igual
  que en el Sprint 17. El canal en tiempo real del Sprint 18 difunde cambios de reglas hacia el
  dispositivo, no alertas hacia el tutor.

## Backend

- `app/services/tamper.py` (nuevo): `evaluate_heartbeat_tamper_signals()` (las cuatro condiciones,
  evaluadas contra el `last_seen_at` anterior antes de sobrescribirlo) y `record_tamper_event()`
  (el evento de desinstalación), con el catálogo de niveles por señal.
- `app/services/alerts.py`: `record_alert_for_tamper_signal()`, que reutiliza tal cual
  `_record_alert()` — misma deduplicación, mismo silenciado, misma purga por retención.
- `app/models/alert.py`: cinco `alert_type` nuevos en `ALERT_TYPES`; `ALERT_LEVELS` intacto.
- `app/schemas/device.py`: `HeartbeatRequest` gana tres campos opcionales;
  `ReportTamperEventRequest` (nuevo, `Literal["UNINSTALL_ATTEMPT"]`).
- `app/api/v1/endpoints/devices.py`: el *heartbeat* evalúa señales y decide el estado;
  `POST /devices/{id}/tamper-events` (nuevo, `require_supervised_owner_of_device`, 204, sin
  entrada de auditoría — es telemetría del dispositivo, mismo criterio que `/rule-events`).
- `app/core/config.py`: `device_heartbeat_silence_alert_seconds` (6 h),
  `device_clock_skew_alert_seconds` (5 min).
- Migración `c8a1e5b90d34`: un solo `CHECK` (`ck_alerts_type_valid`). Ciclo
  upgrade → downgrade → upgrade verificado en Docker.

## Android

- `core/rules/EnforcementLiveness.kt` (nuevo): la marca de vida del servicio de reglas.
- `core/tamper/NetProtectDeviceAdminReceiver.kt` y `core/tamper/TamperReportWorker.kt` (nuevos).
- `core/permissions/DeviceAdminPermission.kt` (nuevo): estado y *intent* de solicitud.
- `res/xml/device_admin.xml` (nuevo, `<uses-policies>` vacío), `AndroidManifest.xml` (receiver con
  `BIND_DEVICE_ADMIN`), `res/values/strings.xml` (textos que muestra el sistema).
- `core/network/DeviceClient.kt`: `sendHeartbeat()` con las tres señales, `reportTamperEvent()`.
- `core/rules/RuleEnforcementService.kt`: sella la marca en cada ciclo, al arrancar, y la borra al
  parar deliberadamente.
- `core/sync/SyncWorker.kt` y `feature/supervised/SupervisedScreen.kt`: mandan las señales en cada
  *heartbeat*; la pantalla añade la tarjeta de protección contra desinstalación.
- `feature/tutor/TutorScreen.kt`: etiquetas en español para los cinco tipos nuevos.

## Web

- `apiClient.ts`: `AlertType` con los cinco tipos nuevos.
- `AlertsPanel.tsx`: sus etiquetas (sin `package_name` ni geocerca: describen el dispositivo).
- `globals.css`: `.statusPill.alert` con fondo propio — "no se reporta" y "se detectó
  manipulación" no deben leerse igual de un vistazo.

## Verificación

Ver `docs/sprint-20-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- **El flujo real de Device Administrator en un dispositivo/emulador**: activar el administrador
  desde la pantalla del sistema, intentar desinstalar la app y comprobar que Android lo impide, y
  ver `onDisableRequested()` dispararse de verdad. Se verificó el contrato contra la documentación
  oficial y el código compila, pero nadie ejecutó ese recorrido a mano en esta sesión. Es lo
  primero que debería probar quien tenga el emulador delante.
- El resto de las señales se probó de extremo a extremo contra el backend real (PostgreSQL/Redis
  en Docker) por el mismo endpoint que usa Android, pero no se recorrió la UI de Android a mano.

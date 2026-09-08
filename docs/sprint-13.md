# Sprint 13 — Ubicación

## Objetivo

El dispositivo supervisado reporta su ubicación aproximada al backend periódicamente; el tutor ve
la última ubicación conocida desde el panel web y desde la app Android. Primer sprint de
geolocalización del proyecto — exige su propia Fase C (permisos de ubicación en Android varían
fuerte entre versiones, y no había nada verificado todavía, ver
`docs/android/capability-matrix.md`).

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-051 | Como dispositivo supervisado, quiero reportar mi ubicación aproximada periódicamente, para que mi tutor pueda ver en qué zona estoy. |
| HU-052 | Como tutor, quiero ver la última ubicación conocida de un dispositivo, en el panel web y en la app. |
| HU-053 | Como dueño del proyecto, quiero que la ubicación se guarde cifrada y se elimine automáticamente tras un periodo corto, porque es el dato más sensible que este proyecto recolecta sobre un menor. |

## Criterios de aceptación

1. El dispositivo supervisado puede reportar `{latitude, longitude, accuracy_meters,
   captured_at}`; sólo el dueño del dispositivo puede reportar por él (404 si no es suyo o no
   existe, mismo patrón `require_supervised_owner_of_device` ya usado para el heartbeat).
2. El tutor dueño del dispositivo puede leer la última ubicación conocida y el historial dentro de
   la ventana de retención; un tutor ajeno recibe 404 (mismo patrón `require_tutor_of_device`).
3. `latest` devuelve `null` (no un error) cuando el dispositivo nunca reportó, o cuando su único
   reporte ya venció — ambos casos son indistinguibles para un tutor y se muestran igual.
4. Las coordenadas se guardan cifradas en la base de datos (columna, no disco); `accuracy_meters`
   se guarda en claro por no ser sensible sin las coordenadas que acompaña.
5. Cada reporte nuevo purga los reportes de ese mismo dispositivo más viejos que la ventana de
   retención, antes de insertarse — nunca una fila más vieja que la ventana sobrevive a un reporte
   posterior.
6. La app Android pide justificación en pantalla antes de disparar el diálogo de permiso de
   ubicación, y explica qué se comparte y cuándo.
7. El panel web y la pantalla del tutor en Android muestran la última ubicación con hora de
   captura; si hay un mapa configurado (ver clave pendiente más abajo), se ve un mapa real, si no,
   coordenadas en texto y un enlace/intent para abrir en un mapa externo.

## Decisiones de diseño y su motivo

### Retención: 7 días, purga inline al reportar (sin scheduler)

La ubicación es el dato más sensible que este proyecto recolecta sobre un menor — a diferencia del
inventario de apps o el tiempo de uso, permite inferir dónde vive, estudia o pasa el tiempo una
persona real. Una ventana corta acota tanto la exposición de privacidad como el tamaño de la tabla.
7 días es suficiente para que un tutor entienda un patrón reciente ("¿llegó al colegio hoy?") sin
acumular un historial de largo plazo que nadie pidió y que aumenta el daño de una fuga.

La purga corre **dentro del propio endpoint de reporte** (`POST .../location`), no en un job
programado — este proyecto no tiene scheduler todavía (mismo patrón ya usado para
`device_offline_threshold_seconds`, que calcula el estado online/offline al vuelo en vez de barrer
la tabla periódicamente). Cada reporte primero borra las filas de **ese mismo dispositivo** más
viejas que `location_retention_days`, luego inserta la nueva — nunca un barrido global, para que un
dispositivo reportando su propia ubicación no pueda disparar trabajo de limpieza sobre filas de
otro dispositivo.

Consecuencia aceptada: si un dispositivo deja de reportar (se desinstala la app, se revoca el
permiso), sus filas viejas no se purgan solas — nadie más va a insertar un reporte nuevo para ese
`device_id` que dispare la purga. No es un problema de privacidad práctico (7 días es corto y esas
filas simplemente dejan de crecer), pero se documenta en vez de asumirse.

### Cifrado a nivel de columna (Fernet), no a nivel de disco

`latitude`/`longitude` se cifran con Fernet (`app/core/crypto.py`) antes de guardarse;
`accuracy_meters` se guarda en claro. El cifrado de PostgreSQL a nivel de disco (si estuviera
activado) protegería contra el robo del disco físico, pero no contra un `pg_dump` plano, un backup
mal configurado, o una credencial de sólo lectura filtrada — todos esos casos seguirían mostrando
las coordenadas en claro sin este cifrado adicional. `accuracy_meters` no se cifra porque un radio
sin el centro que acompaña no identifica nada por sí solo.

La clave (`LOCATION_ENCRYPTION_KEY`) vive en su propia variable de entorno, nunca reutiliza
`JWT_SECRET` ni `PAIRING_CODE_PEPPER` — filtrar un secreto no debe descifrar ubicaciones también.
Es una clave Fernet real (32 bytes base64 url-safe), no un secreto de formato libre como los otros:
un valor inválido rompe sólo los endpoints de ubicación al usarse, no el arranque del backend (ver
docstring de `_fernet()` en `app/core/crypto.py`).

### Frecuencia y precisión: ~15 minutos, sólo aproximada, sin permiso de segundo plano

Decisión tomada junto con la Fase C de Android (`docs/android/capability-matrix.md`, sección
Sprint 13): reportar cada ~15 minutos con `ACCESS_COARSE_LOCATION` únicamente, sin pedir nunca
`ACCESS_BACKGROUND_LOCATION`. Tres motivos, verificados contra la documentación oficial de Android:

1. El caso de uso ("¿en qué zona está?") no necesita precisión de metros ni actualización en
   tiempo real — pedir `ACCESS_FINE_LOCATION` violaría minimización de datos sin beneficio real.
2. Un foreground service con `foregroundServiceType="location"` **ya cuenta como "en primer
   plano" para el sistema de permisos de ubicación de Android** mientras corre — así que este
   proyecto puede seguir reportando aunque el usuario cambie a otra app, sin necesitar
   `ACCESS_BACKGROUND_LOCATION` en absoluto (ver la cita exacta de la documentación en la matriz).
3. Sin ese permiso de segundo plano, la política de Play sobre "Prominent Disclosure" para
   ubicación en background no aplica — ni siquiera bajo el razonamiento habitual de "sólo aplica
   al publicar": el permiso que la activa nunca se declara.

`MAX_HISTORY_REPORTS = 1000` en `backend/app/schemas/location.py` está dimensionado contra esta
frecuencia (4/hora máximo) — si la frecuencia cambiara, ese comentario y ese número deben
revisarse juntos.

### Sin SDK nativo de Google Maps en Android — un intent `geo:` basta

La pantalla del tutor en Android **no** integra el Maps SDK for Android. En su lugar, muestra las
coordenadas y hora en texto más un botón "Abrir en mapa" que lanza
`Intent(ACTION_VIEW, Uri.parse("geo:lat,lng?q=lat,lng"))`: Android delega la resolución a
cualquier app de mapas ya instalada (Google Maps en el emulador de pruebas), sin necesitar clave de
API, sin restricción por SHA-1/paquete, y sin añadir la dependencia del SDK a un proyecto que
evita dependencias grandes a propósito (ver convención de Android en `CLAUDE.md`). Por eso
`GOOGLE_MAPS_ANDROID_API_KEY` **no** se añadió a las variables de entorno de este sprint — no hace
falta.

El panel web sí necesita una clave (Maps Embed API, vía `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`) porque
un navegador no tiene una "app de mapas" externa a la que delegar; se degrada con un mensaje claro
y un enlace a Google Maps (sin clave) cuando la variable no está configurada.

### Pendiente de un humano: `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`

Igual que `GOOGLE_WEB_CLIENT_ID` (`docs/sprint-03.md`), esta clave no la puede generar un agente:
requiere una cuenta de Google Cloud, habilitar Maps Embed API (o Maps JavaScript API), y
restringirla por referer HTTP al dominio del panel web — sin esa restricción, cualquiera podría
usar la clave desde otro sitio y agotar la cuota o generar cargos. Documentado como pendiente
explícito de un humano, no asumido ni simulado. Mientras no exista, el panel web muestra
coordenadas en texto y un enlace a Google Maps — funciona igual, sólo sin mapa embebido.

## Fuera de alcance

- Historial de ubicación en el panel (ruta/trail) — sólo se implementa "última ubicación conocida"
  en las pantallas; el endpoint `GET .../location/history` existe en el backend (y está probado)
  pero ningún cliente lo consume todavía. Queda disponible para un sprint futuro que lo necesite.
- Geocercas (alertas de entrada/salida de una zona) — es el Sprint 14, explícitamente el siguiente.
- Reinicio automático del reporte tras matar el proceso (`BOOT_COMPLETED`, `WorkManager`
  persistente) — mismo límite ya aceptado para `RuleEnforcementService` en el Sprint 8; si el
  usuario cierra la app de un swipe o el sistema mata el proceso, el reporte se detiene hasta que
  la app se vuelva a abrir.

## Backend

- `DeviceLocationReport` (insert-only, `backend/app/models/location.py`): `device_id`,
  `latitude_ciphertext`/`longitude_ciphertext` (Fernet), `accuracy_meters` (claro), `captured_at`
  (reloj del dispositivo) / `received_at` (reloj del servidor) — mismo split que
  `AppRuleEvent.occurred_at`/`received_at` del Sprint 8.
- `POST /devices/{id}/location` (`require_supervised_owner_of_device`): purga inline + inserta.
  `GET /devices/{id}/location/latest` y `GET /devices/{id}/location/history`
  (`require_tutor_of_device`).
- `app/core/crypto.py`: `encrypt_coordinate`/`decrypt_coordinate` sobre Fernet, clave en
  `settings.location_encryption_key`, construida perezosamente (`@lru_cache`) para que una clave
  placeholder inválida sólo rompa el uso real, no el arranque del proceso.
- Migración `ba8b839ae0eb` (nueva tabla `device_location_reports`, índice compuesto
  `(device_id, captured_at)` además del índice simple por `device_id`, `CheckConstraint
  accuracy_meters >= 0`). Ciclo upgrade → downgrade → upgrade verificado en Docker (ver evidencia).

## Android

- `LocationPermission` (nuevo, `core/permissions`): sólo `ACCESS_COARSE_LOCATION`, permiso runtime
  normal (a diferencia de `PACKAGE_USAGE_STATS`, no exige ir a Ajustes).
- `LocationReportingService` (nuevo, `core/location`): foreground service
  `foregroundServiceType="location"`, mismo patrón de arranque que `RuleEnforcementService`
  (sólo desde una `Activity` en primer plano, vía `DisposableEffect` de `SupervisedScreen`). Usa
  `LocationManager.NETWORK_PROVIDER` puro (sin dependencia `play-services-location`), pide una
  actualización con timeout de 30s y cae a `getLastKnownLocation()` si no llega a tiempo o el
  proveedor está desactivado. Reporta cada 15 minutos.
- `LocationClient` (nuevo, `core/network`): `reportLocation`/`getLatestLocation`.
- `SupervisedScreen`: tarjeta de justificación antes de pedir el permiso (mismo patrón visual que
  la tarjeta de acceso a uso del Sprint 7), `DisposableEffect` independiente para arrancar/parar
  `LocationReportingService` (gateado por su propio permiso, no por `hasUsageAccess`).
- `TutorScreen`: sección "Ver ubicación" por dispositivo — coordenadas, precisión y hora en texto,
  botón "Abrir en mapa" (intent `geo:`, ver decisión de diseño arriba).
- Manifiesto: `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, servicio con
  `foregroundServiceType="location"`.

## Web

- `apiClient.ts`: tipo `LocationReport`, `getLatestLocation`.
- `DeviceLocationPanel` (nuevo componente): última ubicación en texto; mapa embebido (Maps Embed
  API, iframe) si `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` está configurada, enlace a Google Maps si no.
  Integrado en `DevicesPanel` junto a apps/reglas/categorías.

## Verificación

Ver `docs/sprint-13-evidence.md` para comandos y salida real. Resumen: 150 pruebas de backend en
verde en Docker (12 nuevas en `test_location_integration.py`), `ruff check` limpio, migración
verificada upgrade→downgrade→upgrade; `./gradlew compileDebugKotlin` y `assembleDebug` en verde
para los archivos Kotlin nuevos; `npm run lint`/`npm run build` en verde en el panel web.

## No se marca como verificado

- **Verificación en dispositivo/emulador real del reporte de ubicación en ejecución** (arrancar el
  servicio, conceder el permiso, confirmar un reporte llegando al backend desde un emulador vivo):
  no se hizo en esta sesión — se verificó compilación real (`compileDebugKotlin`, `assembleDebug`)
  y el mecanismo de arranque reutiliza exactamente el patrón ya probado en ejecución real para
  `RuleEnforcementService` en el Sprint 8 (mismo `DisposableEffect`, mismo `startForegroundService`
  desde una Activity en primer plano). Queda pendiente una corrida real en emulador/dispositivo
  como parte del cierre de un sprint futuro o de una verificación manual, no se da por hecha aquí.
- **La clave `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`**: pendiente de que un humano con cuenta de Google
  Cloud la genere y la restrinja por referer — ver decisión de diseño arriba. Sin ella, el mapa
  embebido del panel web no se puede ver en vivo, sólo el fallback de texto (sí verificado).
- Login real de Google, mismo límite recurrente en todos los sprints anteriores (exige elegir una
  cuenta en un selector real).

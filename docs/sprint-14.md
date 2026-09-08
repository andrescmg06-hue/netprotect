# Sprint 14 — Geocercas

## Objetivo

El tutor define zonas circulares ("Casa", "Colegio") sobre un dispositivo; el backend detecta
cuándo el dispositivo entra o sale de cada zona y guarda el evento para que el tutor lo consulte.
Segundo sprint de geolocalización — reutiliza por completo el reporte periódico de ubicación del
Sprint 13 (`LocationReportingService`, `ACCESS_COARSE_LOCATION`) en vez de adoptar la Geofencing
API real de Android, tras verificar que esa API exige revertir tres decisiones de diseño ya
tomadas en ese sprint (ver más abajo y `docs/android/capability-matrix.md`).

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-054 | Como tutor, quiero crear, editar y eliminar geocercas (nombre, centro, radio) sobre un dispositivo. |
| HU-055 | Como tutor, quiero que se registre automáticamente cuándo el dispositivo entra o sale de una geocerca, sin depender de que el dispositivo esté reportando en ese instante exacto. |
| HU-056 | Como tutor, quiero ver el historial de entradas y salidas de cada geocerca. |
| HU-057 | Como dueño del proyecto, quiero que esta función no exija pedirle al supervisado más permisos de ubicación de los que ya se piden desde el Sprint 13. |

## Criterios de aceptación

1. El tutor dueño del dispositivo puede crear una geocerca (`name`, `latitude`, `longitude`,
   `radius_meters`), listarlas, editarlas y eliminarlas; un tutor ajeno recibe 404 (mismo patrón
   `require_tutor_of_device` de siempre), igual que el supervisado del propio dispositivo (no es
   tutor de ningún dispositivo, así que tampoco tiene una fila en `tutor_devices`).
2. Cada dispositivo tiene un límite máximo de geocercas (`settings.max_geofences_per_device = 20`,
   decisión propia — ver más abajo); superarlo devuelve 422.
3. Cuando el dispositivo supervisado reporta una nueva ubicación (`POST .../location`, sin cambios
   en su contrato ni en Android), el backend compara ese punto contra el anterior conocido del
   mismo dispositivo, para cada geocerca activa: si el estado (dentro/fuera) cambió, se registra un
   evento `ENTER` o `EXIT`.
4. El primer reporte del dispositivo contra una geocerca (no hay punto anterior con qué comparar)
   no genera ningún evento — establece la base silenciosamente, no un `ENTER` falso.
5. Permanecer dentro (o fuera) de una geocerca entre dos reportes consecutivos no genera un evento
   duplicado — sólo un cambio real de estado lo dispara.
6. El tutor dueño del dispositivo puede leer el historial de eventos; un tutor ajeno recibe 404.
7. Eliminar una geocerca no borra su historial: los eventos ya registrados conservan el nombre que
   tenía la geocerca en el momento de detectarse (snapshot), igual que `AppRuleEvent` conserva
   `package_name` sin depender de que la regla siga existiendo.
8. Las coordenadas del centro de cada geocerca se guardan cifradas (Fernet, misma clave y mecanismo
   que `DeviceLocationReport` desde el Sprint 13) — un centro etiquetado por el tutor ("Casa") es
   si acaso más sensible que un punto de paso sin etiquetar.
9. Ningún permiso, dependencia ni servicio nuevo se añade a Android: `LocationReportingService`
   sigue exactamente igual que en el Sprint 13.

## Decisiones de diseño y su motivo

### La decisión central: NO usar la Geofencing API de Android/GMS

El plan de desarrollo (`docs/planning/plan-desarrollo.md`, Paso 13) asumía "Geofencing API de
Android". Antes de escribir código se verificó esa API contra la documentación oficial
(`docs/android/capability-matrix.md`, sección Sprint 14) y resultó exigir, sin excepción:

- `ACCESS_FINE_LOCATION` (no sólo `ACCESS_COARSE_LOCATION`, lo único que este proyecto pide desde
  el Sprint 13).
- `ACCESS_BACKGROUND_LOCATION` para que los eventos ENTER/EXIT lleguen con la app en segundo
  plano — que es el caso de uso real (el supervisado no va a tener NetProtect abierto cuando cruce
  una zona). El truco que el Sprint 13 usa para evitar este permiso (un foreground service
  `location`-typed cuenta como "en primer plano") **no aplica aquí**: los callbacks de geofencing
  llegan desde un proceso de Google Play Services, no desde nuestro propio foreground service.
- La dependencia `play-services-location`, que este proyecto evita a propósito desde el Sprint 13
  (usa `LocationManager` puro).

Adoptarla habría revertido tres decisiones de minimización de datos y de superficie de permisos ya
tomadas y documentadas, a cambio de mejor latencia (2-6 min en vez de ~15 min) y gestión de energía
por el sistema en vez de por nuestro propio servicio. Se presentó esta disyuntiva al dueño del
proyecto (mismo patrón que las categorías del Sprint 10) y se optó por mantener la superficie de
permisos mínima ya establecida, aceptando la peor latencia como costo explícito — no un descuido.

### Evaluación server-side sobre el reporte periódico existente, sin scheduler

Detectar ENTER/EXIT no vive en Android en absoluto: `POST /devices/{id}/location`
(`backend/app/api/v1/endpoints/location.py`) ya recibe cada punto que el dispositivo reporta cada
~15 minutos (Sprint 13); antes de insertar el nuevo punto, el endpoint ahora también obtiene el
punto anterior de ese mismo dispositivo (si existe) y llama a
`evaluate_geofence_transitions()` (`backend/app/services/geofencing.py`), que para cada geocerca
del dispositivo calcula la distancia (fórmula de Haversine, `backend/app/core/geo.py`) del punto
anterior y del nuevo contra el centro de la geocerca, y compara si el estado dentro/fuera cambió.
Mismo patrón "evaluar en el momento de escribir, sin scheduler" ya usado para la purga de
retención de ubicación (Sprint 13) y el estado online/offline por heartbeat (Sprint 6) — este
proyecto no tiene scheduler todavía.

Consecuencia aceptada: la latencia de detección queda atada al intervalo de reporte (~15 minutos)
en vez de los 2-6 minutos que ofrecería la Geofencing API real — el costo directo de la decisión
anterior, ya explicado.

### El primer reporte contra una geocerca no dispara un evento

Si no hay un punto anterior con el que comparar (el dispositivo nunca reportó, o su único reporte
anterior ya venció por la ventana de retención de 7 días del Sprint 13),
`evaluate_geofence_transitions()` no genera ningún evento — sólo establece la base. La alternativa
(disparar un `ENTER` inmediato si el dispositivo ya está dentro al crear la geocerca) generaría
ruido, no señal: un tutor que crea la geocerca "Casa" mientras el dispositivo ya está en casa no
necesita que se le avise de algo que ya sabía en el instante mismo en que lo configuró. Esto es
deliberado, no una limitación: sólo se reportan transiciones reales entre dos puntos conocidos.

### Sin foreign key entre `GeofenceEvent` y `Geofence` — snapshot del nombre

`GeofenceEvent.geofence_id` no tiene restricción de clave foránea, y la fila guarda su propio
`geofence_name` en el momento del evento — mismo patrón que `AppRuleEvent.package_name` (Sprint 8):
una geocerca puede editarse o eliminarse después, pero el hecho de que el dispositivo entró o salió
de "Casa" en un momento dado debe sobrevivir a eso, igual que el historial de bloqueos sobrevive a
que se borre la regla que lo causó.

### Cifrado del centro de la geocerca, mismo mecanismo que el Sprint 13

`latitude_ciphertext`/`longitude_ciphertext` en `geofences` usan exactamente `encrypt_coordinate`/
`decrypt_coordinate` (`backend/app/core/crypto.py`, Fernet, `LOCATION_ENCRYPTION_KEY`) — no una
clave nueva. Un centro de geocerca etiquetado por el tutor ("Casa", "Colegio") es si acaso más
sensible que un punto de paso sin etiquetar del Sprint 13, así que merece exactamente el mismo
nivel de protección, no menos.

### Límite de 20 geocercas por dispositivo: decisión propia, no de la plataforma

Como este proyecto no usa la Geofencing API de Android (que sí impone un límite real de 100 por
app/usuario), `settings.max_geofences_per_device = 20` es una salvaguarda de rendimiento propia:
cada reporte de ubicación evalúa todas las geocercas activas del dispositivo, así que un límite
acota ese costo. 20 cubre con margen los lugares reales de un menor (casa, colegio, un par de
familiares) — ver `backend/app/core/config.py`.

### Sin upsert por clave natural: crear siempre es una fila nueva

A diferencia de `AppRule` (una por `(device_id, package_name)`, upsert automático), una geocerca no
tiene una clave natural evidente — un tutor podría razonablemente querer dos zonas con el mismo
nombre algún día. `POST` siempre crea una fila nueva; editar es un `PUT /{geofence_id}` explícito
por id, igual que `AppRule` lo sería si tuviera un endpoint de edición separado.

### Ingreso manual de coordenadas, no un mapa clicable

Ni el panel web ni Android permiten "hacer clic en el mapa" para fijar el centro de una geocerca:
el panel web usa Maps Embed API (Sprint 13), que no expone eventos de clic al sitio anfitrión (a
diferencia de Maps JavaScript API, una dependencia distinta que este sprint no introduce), y
Android sigue sin SDK de Maps por la misma decisión del Sprint 13. En su lugar, el formulario web
pide latitud/longitud/radio en texto, con un botón "Usar última ubicación conocida" que autocompleta
desde `GET .../location/latest` (útil, por ejemplo, para crear "Colegio" mientras el supervisado
está ahí). Igual que las reglas y categorías (Sprints 8-10), la gestión (crear/editar/eliminar) es
sólo del panel web; la pantalla del tutor en Android es de sólo lectura (lista de geocercas +
historial), sin `GeofenceClient` de escritura — mismo patrón exacto de esos sprints, no una
limitación nueva de este.

## Fuera de alcance

- Alertas push/tiempo real al tutor cuando ocurre un ENTER/EXIT — el evento se guarda y el tutor lo
  consulta activamente; notificaciones son el Sprint 17 (Alertas) y Sprint 18 (Tiempo real/FCM).
- Geocercas poligonales o de forma libre — sólo círculos (centro + radio), como toda implementación
  común de geofencing, incluida la propia API de Android que este sprint decidió no usar.
- Mejorar la precisión de detección más allá de lo que ya limita `ACCESS_COARSE_LOCATION` (Sprint
  13, "accurate to within about 3 square kilometers") — una geocerca de radio pequeño hereda esa
  misma imprecisión; no se impone un radio mínimo porque no es este proyecto quien debe decidir ese
  trade-off por el tutor, pero se documenta aquí explícitamente.

## Backend

- `Geofence` (`backend/app/models/geofence.py`): `device_id`, `name`, `latitude_ciphertext`/
  `longitude_ciphertext` (Fernet), `radius_meters` (`CheckConstraint > 0`), `created_at`/
  `updated_at`.
- `GeofenceEvent` (insert-only, sin FK a `Geofence`): `device_id`, `geofence_id` (suelto),
  `geofence_name` (snapshot), `event_type` (`ENTER`/`EXIT`), `occurred_at`/`received_at` — mismo
  split que `AppRuleEvent`.
- `app/core/geo.py`: `haversine_distance_meters()`, función pura.
- `app/services/geofencing.py`: `evaluate_geofence_transitions()`, llamada desde
  `POST /devices/{id}/location` justo antes del commit — compara el reporte anterior y el nuevo
  contra cada geocerca del dispositivo y crea los eventos que correspondan.
- `POST/GET/PUT/DELETE /devices/{id}/geofences` y `GET /devices/{id}/geofences/events`
  (`app/api/v1/endpoints/geofences.py`), todos con `require_tutor_of_device`. Crear/editar/eliminar
  auditan `GEOFENCE_CREATED`/`GEOFENCE_UPDATED`/`GEOFENCE_DELETED` (`app/services/audit.py`); los
  eventos ENTER/EXIT no se auditan (evidencia detectada por el sistema, no una acción del tutor —
  mismo criterio que `AppRuleEvent`).
- Migración `e60012acd532` (tablas `geofences` y `geofence_events`). Ciclo upgrade → downgrade →
  upgrade verificado en Docker (ver evidencia).
- `settings.max_geofences_per_device = 20` (`app/core/config.py`).

## Android

- `GeofenceClient` (nuevo, `core/network`, sólo lectura): `listGeofences`/`listGeofenceEvents`.
  Sin `createGeofence`/`updateGeofence`/`deleteGeofence` — gestión es sólo web, mismo patrón que
  reglas/categorías desde los Sprints 8-10.
- `TutorScreen`: botón "Ver geocercas" por dispositivo (junto a "Ver ubicación") que muestra la
  lista de geocercas y el historial de entradas/salidas.
- **Sin cambios en el manifiesto, sin permisos nuevos, sin dependencias nuevas.**
  `LocationReportingService` (Sprint 13) no se toca — sigue reportando exactamente igual.

## Web

- `apiClient.ts`: tipos `Geofence`/`GeofenceEvent` y funciones `listGeofences`/`createGeofence`/
  `updateGeofence`/`deleteGeofence`/`listGeofenceEvents`.
- `GeofencePanel` (nuevo componente): formulario crear/editar (nombre, latitud, longitud, radio,
  con botón "Usar última ubicación conocida"), lista con editar/eliminar, e historial de
  entradas/salidas — mismo patrón que `DeviceRulesPanel`. Integrado en `DevicesPanel` junto a
  apps/reglas/categorías/ubicación.

## Verificación

Ver `docs/sprint-14-evidence.md` para comandos y salida real.

## No se marca como verificado

- **Verificación en dispositivo/emulador real de que un ENTER/EXIT detectado en el backend
  refleja correctamente un cruce físico de geocerca**: no se hizo en esta sesión — se verificó la
  lógica de detección con pruebas de integración que simulan reportes consecutivos dentro/fuera
  (Docker, PostgreSQL real), no un recorrido físico con GPS real. `LocationReportingService` en sí
  ya fue compilado y su mecanismo de arranque es el mismo probado en ejecución real en el Sprint 8;
  este sprint no le agrega código.
- Login real de Google, mismo límite recurrente en todos los sprints anteriores.

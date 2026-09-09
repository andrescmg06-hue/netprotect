# Sprint 22 — Auditoría

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 21) pide: "Registro inmutable de acciones sensibles con
actor, acción, recurso, fecha y origen. Consulta de auditoría para el tutor y exportación."
`app/services/audit.py` (`record_audit_event`) y la tabla `audit_logs` existen desde el esquema
inicial (Sprint 2) y se escriben desde el Sprint 3 (login/refresh/logout) — 23 llamadas en 9
endpoints distintos hoy — pero nunca se exponían: `docs/security-baseline.md` lo listaba
explícitamente como control diferido ("hoy se escribe pero no se expone"). Este sprint es
enteramente de lectura: ningún modelo, migración ni call site de `record_audit_event` nuevo — sólo
los endpoints que consultan y exportan lo que ya se guardaba.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-068 | Como tutor, quiero consultar el registro de mis propias acciones administrativas (qué hice, cuándo y desde qué IP) para poder revisar mi propia actividad en la cuenta. |
| HU-069 | Como tutor, quiero filtrar ese registro por tipo de acción, tipo de recurso y rango de fechas para encontrar un cambio concreto sin desplazarme por todo el historial. |
| HU-070 | Como tutor, quiero exportar mi registro de auditoría a un archivo que pueda guardar o compartir fuera de la plataforma. |

## Criterios de aceptación

1. `GET /users/me/audit` devuelve, para cualquier usuario autenticado, sólo las filas de
   `audit_logs` cuyo `actor_user_id` sea el suyo — nunca las de otro usuario, sin importar su rol.
2. Acepta filtros opcionales `action`, `resource_type`, `from_date`, `to_date`, combinables entre
   sí (AND).
3. Pagina con `limit`/`offset` reales (no un tope fijo de tamaño de respuesta) y devuelve `total`
   además de la página pedida.
4. `GET /users/me/audit/export` devuelve un CSV con las mismas columnas y los mismos filtros que
   el listado, sin paginar (hasta un tope de seguridad fijo).
5. Sin token, ambos endpoints responden 401.
6. Ni el listado ni la exportación generan una fila nueva en `audit_logs` (leer el propio registro
   no es, en sí, una acción a auditar — mismo criterio que ya regía `GET /devices/{id}/alerts` y
   `GET /devices/{id}/history`).
7. Disponible en el panel web (con filtros, paginación y botón de exportar) y, en modo sólo
   lectura y sin filtros, en la app del tutor en Android.

## Decisiones de diseño y su motivo

### Alcance: "mis propias acciones", no "todo lo que pasó en mis dispositivos"

`AuditLog` (`app/models/audit_log.py`) no tiene columna `device_id` — a diferencia de
`AppRuleEvent`/`GeofenceEvent`/`Alert`/`DeviceLocationReport`. Lo único que modela directamente es
quién (`actor_user_id`) hizo qué (`action`) sobre qué (`resource_type`/`resource_id`, un string
libre cuyo significado cambia según `resource_type`: un UUID de dispositivo para `"device"`, un
código de rol para `"role"`, nada para `"pairing_code"`). Filtrar por `actor_user_id ==
current_user.id` es lo único que se puede hacer sin inventar una regla de reconstrucción por
recurso (¿qué se hace con `LOGIN`/`ROLE_GRANTED`/`PAIRING_CODE_GENERATED`, que no nombran un
dispositivo en absoluto?) — y es, además, el mismo patrón de "registro de actividad de cuenta" que
ya usan otros productos (historial de acciones de GitHub, actividad de la cuenta de Google): un
usuario ve lo que él mismo hizo, nunca lo que hizo otro, sin que su rol se lo tenga que conceder.

El costo aceptado: `DEVICE_LINKED` se audita con el **supervisado** como actor
(`app/api/v1/endpoints/pairing.py`, porque es quien redime el código), así que no aparece en la
auditoría del tutor aunque el dispositivo sea suyo. No es una pérdida real de información — el
tutor ya ve el estado de vinculación directamente en la lista de dispositivos desde el Sprint 6 —
sólo no aparece en esta línea de tiempo concreta. Verificado explícitamente con una prueba
(`test_device_linked_is_recorded_under_the_supervised_actor_not_the_tutor`).

Consecuencia adicional, también deliberada: el endpoint no exige rol `TUTOR`
(`require_role("TUTOR")`) ni ningún otro — cualquier usuario autenticado consulta su propio
registro. No hace falta restringirlo por rol porque el filtro por `actor_user_id` ya hace
imposible ver información ajena sea cual sea el rol del llamante; y un usuario `SUPERVISADO`
también genera acciones propias (`DEVICE_LINKED`, `LOGIN`) que tiene sentido que pueda revisar.

### Paginación real, no un tope fijo de tamaño de respuesta

`history.py` (Sprint 15) y `alerts.py` (Sprint 17) usan un `MAX_X` fijo sin `limit`/`offset`,
justificado ahí porque ambas tablas fuente ya purgan a los 90 días — el volumen real está acotado.
`AuditLog` no tiene ninguna retención (ver más abajo), así que un tope fijo sin paginación real
ocultaría permanentemente cualquier fila más antigua una vez que una cuenta lo superase. Por eso
`GET /users/me/audit` implementa `limit` (por defecto 50, máximo 200) y `offset` reales, con
`total` devuelto aparte para que el panel pueda construir una paginación de verdad — la primera
vez que este proyecto pagina así, divergencia consciente del patrón anterior y documentada aquí
por esa razón.

### Sin retención ni purga

El propio plan llama a esto un **"registro inmutable"** — a diferencia de ubicación (7 días),
`AppRuleEvent`/`GeofenceEvent`/`Alert` (90 días), aquí no se agrega ningún
`audit_log_retention_days` ni se llama a `purge_expired_rows()` (que, de hecho, exige un
`device_id` que esta tabla no tiene, así que tampoco es reutilizable tal cual). Es la única tabla
de eventos del proyecto que crece sin límite. Aceptado porque: (a) el propio enunciado pide
inmutabilidad para este rastro concreto, a diferencia de la telemetría operativa que sí se purga;
y (b) lo que guarda (`action`, `resource_type`, un `resource_id` que es un UUID o un código de rol,
una IP) es mucho menos sensible que la ubicación cruda de un menor, que es lo que motivó la
retención corta del Sprint 13.

### Exportación: CSV con tope de seguridad, no auditada

`GET /users/me/audit/export` reutiliza el mismo filtro que el listado pero sin `limit`/`offset` —
no hay UI de paginación en una descarga — así que en su lugar tiene un tope duro
(`MAX_AUDIT_EXPORT_ROWS = 10 000`) para que una cuenta con años de actividad y sin filtros no
pueda forzar un CSV sin límite. `StreamingResponse` con `Content-Disposition: attachment`, mismo
mecanismo estándar de FastAPI, sin biblioteca nueva.

Ni el listado ni la exportación llaman a `record_audit_event`: leer el propio registro de
auditoría es, otra vez, el mismo tipo de acción que `list_alerts`/`get_device_history` ya trataban
como no auditable — el rastro registra acciones que vale la pena revisar después, no la revisión
en sí misma.

### Android: sólo lectura, sin filtros ni exportación

Mismo criterio ya establecido para historial/alertas/geocercas: Android sólo muestra lo que el
backend ya expone, sin escritura ni construcción de UI nueva compleja (filtros de fecha,
descarga de archivo) que sólo tiene sentido en el panel web. A diferencia de los paneles
anteriores, esta sección no es por dispositivo — es de cuenta — así que en `TutorScreen` vive como
una sección propia ("Mi actividad"), no dentro de la fila de cada dispositivo.

## Fuera de alcance

- **Nueva tabla o migración**: `audit_logs` ya existe desde el Sprint 2; este sprint es
  exclusivamente de lectura sobre datos que ya se escribían.
- **Auditar la propia lectura/exportación de auditoría**: ver la sección de exportación arriba.
- **Vista "todo lo que pasó en mis dispositivos"** (cruzando `resource_type`/`resource_id` con los
  dispositivos del tutor): ver la sección de alcance arriba — el modelo de datos no lo soporta sin
  inventar una regla de reconstrucción por tipo de recurso.
- **Exportación desde Android**: sólo panel web, mismo split que crear/editar reglas y geocercas.

## Backend

- `app/schemas/audit.py` (nuevo): `AuditLogResponse`, `AuditLogListResponse`,
  `MAX_AUDIT_PAGE_SIZE = 200`, `DEFAULT_AUDIT_PAGE_SIZE = 50`, `MAX_AUDIT_EXPORT_ROWS = 10_000`.
- `app/api/v1/endpoints/audit.py` (nuevo): `GET /users/me/audit` (filtros + paginación real +
  `total`) y `GET /users/me/audit/export` (CSV, mismos filtros, sin paginar). Ambos protegidos sólo
  con `get_current_user` — sin `require_role` ni `require_tutor_of_device`, ver "Alcance" arriba.
- `app/api/v1/router.py`: registra `audit_router`.
- Ningún cambio a `app/models/audit_log.py`, `app/services/audit.py` ni a ninguna migración.

## Web

- `apiClient.ts`: tipo `AuditLogEntry`, `listMyAuditLog` (filtros + paginación) y
  `exportMyAuditLog` (descarga vía `Blob`/`URL.createObjectURL`, porque el endpoint exige
  cabecera `Authorization` y un `<a href>` plano no puede fijarla).
- `AuditPanel` (nuevo componente): filtros de acción/tipo de recurso, paginación anterior/
  siguiente, botón "Exportar CSV". A diferencia de los demás paneles, se monta una sola vez en
  `page.tsx` junto a `DevicesPanel`, no por dispositivo — es de cuenta, no por dispositivo.

## Android

- `AuditClient` (nuevo, `core/network`, sólo lectura, sin filtros).
- `TutorScreen`: sección de cuenta "Mi actividad (auditoría)", con su propio estado y `toggle`,
  fuera del bucle de dispositivos (primera sección de la pantalla que no es por dispositivo).

## Verificación

Ver `docs/sprint-22-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Ejecución de la pantalla de Android en un emulador/dispositivo físico visualizando la sección de
  auditoría — se compiló (`compileDebugKotlin`) y se verificó la lógica de consumo de la API contra
  el backend real por el mismo endpoint que usa el panel web (probado con la suite de integración),
  pero no se recorrió la UI de Android a mano en esta sesión.

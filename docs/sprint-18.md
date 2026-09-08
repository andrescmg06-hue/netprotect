# Sprint 18 — Tiempo real

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 17) pide: "WebSockets autenticados para tutor y
dispositivo, con canal por dispositivo. Firebase Cloud Messaging para despertar al dispositivo...
Propagación de un cambio de regla verificable de extremo a extremo." Hasta este sprint, un cambio
de regla (Sprints 8-12) sólo llegaba al dispositivo supervisado en su siguiente sondeo periódico
(hasta `RULES_REFRESH_INTERVAL_MS` = 60s, `RuleEnforcementService.kt`) y al panel del tutor en su
siguiente recarga manual. `docs/security-baseline.md` ya listaba "FCM/WebSockets" entre los
controles diferidos conscientemente desde el Sprint 3 — este sprint los recoge.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-068 | Como tutor, quiero que un cambio de regla que hago en el panel llegue al dispositivo supervisado sin esperar a su próximo sondeo. |
| HU-069 | Como tutor con el panel abierto, quiero ver reflejado de inmediato un cambio de regla hecho desde otra sesión (otro tutor, u otra pestaña). |
| HU-070 | Como dueño del proyecto, quiero que el canal en tiempo real esté autenticado por dispositivo, no abierto a cualquiera que adivine un ID. |
| HU-071 | Como dispositivo supervisado que no tiene el socket abierto (proceso en segundo plano, sin app abierta), quiero poder recibir igual una señal de que hay una regla nueva que aplicar. |

## Criterios de aceptación

1. `WS /devices/{device_id}/ws` exige un primer frame `{"token": "<access token>"}`; sin él, con
   un token inválido, o si quien lo presenta no es ni el tutor activo ni el supervisado dueño del
   dispositivo, el servidor cierra la conexión (4401 sin identidad válida, 4404 con identidad
   válida pero sin acceso a este dispositivo — mismo criterio 404-para-ambos-casos que el resto
   de la API, ver `docs/security-baseline.md`).
2. Un tutor activo del dispositivo y el supervisado dueño del dispositivo pueden conectarse al
   mismo canal (`/devices/{id}/ws`); un tutor ajeno o un supervisado de otro dispositivo, no.
3. Al crear/editar/eliminar una `AppRule`, una `CategoryRule`, una asignación de categoría, la
   política por defecto o el horario escolar de un dispositivo, cualquier conexión abierta en el
   canal de ese dispositivo recibe `{"event": "rules_changed", "device_id": ..., "changed_at":
   ...}` — probado de extremo a extremo: el dispositivo supervisado conectado recibe el mensaje
   tras un cambio hecho por el tutor vía REST, y un tutor conectado lo recibe tras un cambio de
   política.
4. `POST /devices/{device_id}/push-token` permite al dispositivo supervisado (y sólo a él)
   registrar su token FCM.
5. Si el dispositivo supervisado no tiene el canal abierto en el momento del cambio, y tiene un
   token FCM registrado, el backend intenta una notificación push de "despertar" (probado con
   `unittest.mock.patch` sobre la función que de verdad llamaría a FCM, mismo patrón que la
   verificación de Google — ver más abajo). Si el dispositivo sí tiene el canal abierto, no se
   intenta el push (ya recibió el aviso por WebSocket).

## Decisiones de diseño y su motivo

### Autenticación por primer mensaje, no por cabecera ni por query string

Un navegador no puede fijar cabeceras personalizadas en el *handshake* de un WebSocket, así que
`Authorization: Bearer ...` (el patrón que usa el resto de esta API) no es viable aquí. La
alternativa más común, pasar el token como parámetro de la URL (`?token=...`), lo deja expuesto en
logs de acceso, historiales de navegador y proxies intermedios — justo el tipo de fuga que este
proyecto evita deliberadamente en otros lados (p. ej. el pepper de los códigos de vinculación,
Sprint 5). En su lugar, el servidor acepta la conexión y espera un primer frame de texto
`{"token": "<access token>"}`; sin él (o si es inválido) en `ws_auth_timeout_seconds` (10s por
defecto), cierra el socket. Mismo protocolo para el panel web, Android y las pruebas.

### Un canal por dispositivo, no por usuario

El tutor y el supervisado comparten el mismo canal (`/devices/{id}/ws`) porque ambos necesitan
enterarse de lo mismo — un cambio de regla — y porque ya existe la relación de autorización
correcta para decidir quién puede escuchar (`require_tutor_of_device` /
`require_supervised_owner_of_device`, reimplementadas a mano dentro del *handshake* de WebSocket
porque son dependencias HTTP que esperan cabeceras, no un primer frame).

### El mensaje no lleva el cambio, sólo avisa que hubo uno

`{"event": "rules_changed", ...}` no incluye qué regla cambió ni su nuevo valor. El dispositivo ya
tiene un endpoint que devuelve su set completo de reglas en una sola llamada
(`GET /devices/{id}/rules/active`, Sprint 8), y el panel web ya sabe recargar lo que muestra. Mantener
sincronizados dos formatos — el de la respuesta REST y el de un parche por WebSocket — para ahorrar
una llamada HTTP no vale la complejidad ni el riesgo de que diverjan; "avisar y recargar" es el
mismo patrón que ya usa el sondeo de Android desde el Sprint 8, sólo que activado por evento en vez
de por temporizador.

### Registro de conexiones en memoria del proceso, no en Redis

El backend corre como un único *worker* de uvicorn (`backend/Dockerfile`, sin `--workers`), así que
un diccionario en memoria del proceso es un bus de difusión correcto hoy, no un atajo: sólo existe
un proceso que pueda tener cualquier socket dado. Escalar el backend a más de un *worker*/réplica
necesitaría un bus compartido (Redis pub/sub, que el proyecto ya usa para *rate limiting*) — se
deja para cuando ese escalamiento sea una necesidad real, mismo criterio que ya rige la ausencia de
un *scheduler* en el resto del proyecto. Ver `app/services/realtime.py`.

### FCM: estructura real, credenciales pendientes de un humano

`docs/planning/plan-desarrollo.md` ya anticipaba este punto (fila "Paso 17" de la tabla de
herramientas): un proyecto Firebase y su `google-services.json` se crean en
console.firebase.google.com, con una cuenta de Google — el mismo tipo de paso que ya requirió
`GOOGLE_WEB_CLIENT_ID` (Sprint 3) y `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` (Sprint 13). Este repo no
tiene ese proyecto ni esa credencial, y no se puede crear uno de mentira que compile: el plugin de
Gradle `com.google.gms.google-services` falla la compilación completa si `google-services.json` no
existe — así que **no se agregó el SDK de Firebase Messaging a Android en este sprint**, para no
dejar `compileDebugKotlin` roto. Lo que sí se construyó, y funciona hoy sin esa credencial:

- `devices.fcm_token`/`fcm_token_updated_at` (migración `d3f6a9c2b5e7`) y
  `POST /devices/{id}/push-token` para que el dispositivo registre su token cuando exista uno real.
- `app/services/push.py`: obtiene un token OAuth2 desde una cuenta de servicio
  (`fcm_service_account_file`) y llama la API HTTP v1 de FCM
  (`https://fcm.googleapis.com/v1/projects/{project}/messages:send`). Con `fcm_project_id` vacío
  (el valor por defecto en desarrollo/pruebas/CI), `is_push_configured()` es `False` y el envío se
  omite con un log, sin fallar la petición que lo disparó — un cambio de regla del tutor debe
  tener éxito exista o no un proyecto Firebase detrás.
- La llamada real a Google (`_post_fcm_message`) está aislada en su propia función para poder
  simularla con `unittest.mock.patch`, el mismo patrón que ya usa este proyecto para la
  verificación de ID tokens de Google (`docs/sprint-03.md`) — se simula la función que llama a
  Google, nunca la lógica propia (`notify_rules_changed`, que decide *si* llamar).

### `RuleEnforcementService` en Android: WebSocket como acelerador, no como reemplazo del sondeo

El *foreground service* del Sprint 8 sigue sondeando cada `POLL_INTERVAL_MS` (3s) y refrescando su
copia de las reglas cada `RULES_REFRESH_INTERVAL_MS` (60s) — ese sondeo es lo único que sigue
funcionando si el proceso pierde el socket (Wi-Fi inestable, el propio backend reiniciando). El
WebSocket sólo adelanta ese refresco: al recibir `rules_changed`, marca un `AtomicBoolean` que la
próxima vuelta del bucle de sondeo lee y fuerza el refresco antes de que se cumplan los 60s. Ningún
camino de bloqueo depende de que el socket esté vivo.

### OkHttp en Android: excepción puntual, no un cambio de rumbo

`HttpJsonClient.kt` documenta desde el Sprint 3 por qué el proyecto usa `java.net.HttpURLConnection`
en vez de Retrofit/OkHttp para las llamadas REST: la superficie de API es pequeña y `java.net` ya la
cubre. Pero `java.net.HttpURLConnection` no tiene cliente de WebSocket en absoluto, y
`java.net.http.WebSocket` (el de la JDK 11+) sólo llegó a Android en la API 34 — muy por encima del
`minSdk 26` de este proyecto. Se agregó `com.squareup.okhttp3:okhttp` (`4.12.0`) sólo para
`RealtimeClient.kt`; el resto de los clientes de red siguen en `java.net`.

## Fuera de alcance

- **El SDK de Firebase Messaging en Android** y el propio proyecto Firebase — pendientes de que un
  humano con cuenta de Google Cloud/Firebase los cree (ver arriba). El endpoint de registro y el
  servicio de envío ya existen y están probados en su propia lógica; falta la credencial real.
- **Reactividad en vivo del historial/estadísticas/alertas ante `rules_changed`**: sólo
  `DeviceRulesPanel` (web) y `RuleEnforcementService` (Android) escuchan el canal en este sprint —
  son las dos superficies que el criterio de aceptación 3 pide cubrir. Los demás paneles siguen
  recargando bajo demanda, como en sprints anteriores.
- **Reconexión automática tras una caída del socket**: ni el cliente web ni `RealtimeClient.kt`
  reintentan la conexión si se cae; ambos vuelven a intentarlo si el usuario reabre el panel de
  reglas o si el servicio Android se reinicia. El sondeo de 60s en Android cubre el vacío mientras
  tanto; el panel web no tiene un equivalente y se queda sin avisos en vivo hasta recargar.
- **Escalar el backend a más de un `worker`**: el registro de conexiones vive en memoria del
  proceso (ver arriba); necesitaría Redis pub/sub si el backend alguna vez corre en más de una
  réplica.

## Backend

- `app/models/device.py`: columnas `fcm_token`/`fcm_token_updated_at`.
- Migración `d3f6a9c2b5e7` (arriba de `b7c3e0d4f1a8`). Ciclo upgrade → downgrade → upgrade
  verificado en Docker.
- `app/services/realtime.py` (nuevo): `ConnectionManager` en memoria (por dispositivo, con rol
  `DEVICE`/`TUTOR`) y `notify_rules_changed()`.
- `app/services/push.py` (nuevo): `is_push_configured()`, `send_rule_change_wake()`, llamada real a
  FCM aislada en `_post_fcm_message()`.
- `app/schemas/realtime.py` (nuevo): `RegisterPushTokenRequest`/`Response`.
- `app/api/v1/endpoints/realtime.py` (nuevo): `WS /devices/{id}/ws`,
  `POST /devices/{id}/push-token`.
- `app/api/v1/endpoints/rules.py`/`categories.py`: cada mutación de regla llama a
  `notify_rules_changed()` tras su propio `commit()`.
- `app/core/config.py`: `ws_auth_timeout_seconds`, `fcm_project_id`, `fcm_service_account_file`.

## Web

- `apiClient.ts`: `deviceRealtimeWebSocketUrl()`.
- `DeviceRulesPanel.tsx`: se conecta al canal del dispositivo mientras el panel está montado;
  al recibir `rules_changed`, recarga las reglas (`loadRules()`) y notifica al padre
  (`onPolicyChanged()`) para refrescar política/horario escolar también.

## Android

- `RealtimeClient.kt` (nuevo, `core/network`, sobre OkHttp — ver decisión arriba).
- `RuleEnforcementService.kt`: abre el canal al iniciar el sondeo; un `rules_changed` fuerza el
  refresco de reglas en la siguiente vuelta del bucle en vez de esperar los 60s habituales.

## Verificación

Ver `docs/sprint-18-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Un proyecto Firebase real y el envío efectivo de una notificación FCM: no existe la credencial
  (ver "FCM: estructura real, credenciales pendientes de un humano" arriba); lo que se verificó es
  que `notify_rules_changed()` decide correctamente *cuándo* intentarlo (con la llamada a Google
  simulada) y que un cambio de regla no falla si el envío no está configurado.
- Recorrido manual en un navegador o en un emulador/dispositivo Android viendo la actualización en
  vivo — se verificó `npm run build`/`npm run lint` (web) y `compileDebugKotlin` (Android), y la
  propagación de extremo a extremo real está probada en `test_realtime_integration.py` contra el
  backend real en Docker, pero nadie abrió la app ni el panel a mirarlo ocurrir en pantalla en esta
  sesión.

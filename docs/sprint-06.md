# Sprint 6 — Gestión de dispositivos

## Objetivo

El tutor ve su parque de dispositivos con estado real, en la app Android y en el panel web:
listado, detalle, renombrado y desvinculación, sobre el estado de conexión que ya calcula el
heartbeat introducido en este sprint.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-018 | Como tutor, quiero ver la lista de mis dispositivos vinculados con su estado actual. |
| HU-019 | Como tutor, quiero ver el detalle de un dispositivo (plataforma, versión, última conexión). |
| HU-020 | Como tutor, quiero renombrar un dispositivo para identificarlo fácilmente. |
| HU-021 | Como tutor, quiero desvincular un dispositivo cuando ya no debo supervisarlo. |
| HU-022 | Como dispositivo supervisado, quiero reportar que sigo activo (heartbeat) para que mi tutor vea mi estado real. |
| HU-023 | Como dispositivo supervisado, quiero poder confirmar quién me supervisa aunque cambie la cuenta que inició sesión en este teléfono. |

## Criterios de aceptación

1. El heartbeat del dispositivo supervisado actualiza `last_seen_at`; sin un heartbeat dentro del
   umbral configurado (`device_offline_threshold_seconds`, 300s por defecto) el estado efectivo pasa
   a `OFFLINE` sin que exista ningún job en segundo plano — se calcula al leer, no al escribir.
2. Listado y detalle sólo muestran dispositivos vinculados al tutor autenticado; un tutor ajeno
   recibe 404 (mismo patrón anti-IDOR que el resto del proyecto: "no existe" y "no es tuyo" son
   indistinguibles desde fuera).
3. Renombrar y desvincular exigen ser el tutor vinculado a ese dispositivo.
4. Sólo el propio dispositivo supervisado envía su heartbeat; ni su tutor ni otra cuenta supervisada
   pueden hacerlo en su nombre.
5. El dispositivo supervisado puede preguntar por su propio estado y quién lo supervisa
   (`GET /devices/me`) sin que la respuesta exponga nada de ningún otro dispositivo.
6. Listado, detalle, renombrado y desvinculación están disponibles en la app del tutor (Android) y
   en el panel web.

## Decisiones de diseño relevantes

- **Estado calculado al leer, no escrito por un job.** `compute_effective_status()` compara
  `last_seen_at` contra el umbral en el momento de la consulta. El dato crudo (cuándo se vio de
  verdad por última vez) nunca se sobrescribe; sólo se deriva de él una interpretación distinta al
  mostrarlo. Evita introducir un scheduler que este proyecto todavía no tiene, sin perder la
  garantía de que el tutor nunca ve "ONLINE" a un dispositivo apagado hace rato.
- **`GET /devices/me` — el supervisado consulta su propia verdad, no confía sólo en su caché
  local.** La app Android recuerda su `device_id` tras vincularse (`LinkedDeviceStore`) para no
  pedir el código en cada arranque. Esa caché es sólo un atajo de interfaz: si una cuenta de Google
  distinta inicia sesión en el mismo teléfono, o el tutor desvincula el dispositivo de forma remota,
  nada la invalidaría por sí sola, y un heartbeat contra un `device_id` que ya no pertenece a esa
  sesión fallaría con 404 sin explicación. Este endpoint le da a la app una forma de preguntarle al
  servidor "¿qué sabes tú de mi cuenta?" en cada arranque; si la respuesta es "no vinculado", se
  limpia la caché y se vuelve a pedir el código. Si la petición falla por red (no por 404), se
  confía en la caché en lugar de forzar una re-vinculación por un problema pasajero de conectividad.
- **El panel web es sólo de tutor; Android puede ser cualquiera de los dos roles.** La misma app
  Android se instala tanto en el teléfono del tutor como en el del supervisado, así que necesita
  preguntar qué rol quiere esta instalación (`HomeScreen`, pantalla nueva de este sprint). El panel
  web sólo tiene sentido para un tutor, así que en vez de mostrarle una elección sin objeto,
  asegura el rol TUTOR en silencio (`POST /users/me/roles`, ya idempotente desde el Sprint 4) antes
  de listar dispositivos.
- **`HomeScreen.kt` reemplaza a `SprintOneScreen.kt` como punto de entrada.** La pantalla de
  diagnóstico del Sprint 1 (login + estado de infraestructura) ya cumplió su propósito: ahora existe
  un flujo real (`Loading → SignedOut → SelectingRole → TutorMode/SupervisedMode`). Su
  comprobación de infraestructura no se descartó — se movió a la pantalla de sesión cerrada, que es
  precisamente donde "no se pudo iniciar sesión" es ambiguo entre una cuenta rechazada y un backend
  inalcanzable.

## Ejecución

```bash
docker compose up --build -d

# tutor: listar, renombrar, desvincular
curl http://localhost:8000/api/v1/devices -H "Authorization: Bearer <token_tutor>"
curl -X PATCH http://localhost:8000/api/v1/devices/<id> -H "Authorization: Bearer <token_tutor>" \
  -H "Content-Type: application/json" -d '{"name":"Tablet de la sala"}'
curl -X DELETE http://localhost:8000/api/v1/devices/<id>/link -H "Authorization: Bearer <token_tutor>"

# supervisado: heartbeat y autoconsulta
curl -X POST http://localhost:8000/api/v1/devices/<id>/heartbeat \
  -H "Authorization: Bearer <token_supervisado>" -H "Content-Type: application/json" -d '{}'
curl http://localhost:8000/api/v1/devices/me -H "Authorization: Bearer <token_supervisado>"
```

Panel web: `http://localhost:3000` → iniciar sesión con Google → "Dispositivos vinculados".

## Verificación

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

cd frontend && npm run lint && npm run build

cd mobile && ./gradlew test assembleDebug assembleRelease
```

## Seguridad del sprint

- Ningún endpoint nuevo se sale del patrón ya establecido: dependencias de autorización por recurso
  (`require_tutor_of_device`, `require_supervised_owner_of_device`) en cada ruta, 404 uniforme para
  "no existe" y "no es tuyo".
- El heartbeat no genera entradas de auditoría (se repite cada minuto mientras la app está abierta;
  `last_seen_at` ya es su propio historial). Renombrar, desvincular y los cambios de rol sí quedan
  auditados, como en sprints anteriores.
- `GET /devices/me` sólo expone tutores con vínculo activo (`unlinked_at IS NULL`); un tutor que se
  desvinculó desaparece de la respuesta del supervisado igual que desaparece de la del tutor.

## Fuera de alcance

Los estados `SYNCING`, `ALERT` y `RESTRICTED` de la máquina de estados del plan de desarrollo: hoy
sólo existen `ONLINE`, `OFFLINE` y `UNLINKED`, que son los que el heartbeat y la vinculación pueden
producir honestamente. Los demás necesitan las reglas de control (Fase C) y la detección de
manipulación (Fase E) que todavía no existen; añadirlos ahora sería un campo que nada actualiza.
También queda fuera un scheduler en segundo plano para heartbeats en Android (WorkManager, Sprint 19)
— el de este sprint sólo corre mientras la pantalla del supervisado está en primer plano.

## Definition of Done del Sprint 6

Los 6 criterios de aceptación tienen prueba automatizada contra PostgreSQL y Redis reales: 21 pruebas
de dispositivos (17 de integración + 4 unitarias de `compute_effective_status`), verdes dos veces
seguidas en contenedor limpio, sobre una suite total de 62. Android compila, pasa sus pruebas y
empaqueta debug y release. El panel web pasa lint estricto (`--max-warnings=0`) y build de
producción. Ver `docs/sprint-06-evidence.md`.

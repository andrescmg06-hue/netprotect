# Sprint 18 — Evidencia

Todos los comandos se ejecutaron realmente, en Windows, contra contenedores Docker reales para el
backend (no mocks de base de datos), siguiendo la nota de rendimiento de `CLAUDE.md`.

## Migración

```
$ docker compose -f compose.test.yaml run --rm migrate
...
INFO  [alembic.runtime.migration] Running upgrade b7c3e0d4f1a8 -> d3f6a9c2b5e7,
  devices.fcm_token: FCM registration token for the real-time wake-up nudge (Sprint 18)
```

## Lint (ruff)

```
$ docker compose -f compose.test.yaml run --rm --no-deps backend sh -c "ruff check --no-cache app tests alembic"
All checks passed!
```

(La primera corrida encontró un `E501` en `app/core/config.py` y cuatro `B017` — "no afirmar una
excepción genérica" — en `test_realtime_integration.py`, por usar `pytest.raises(Exception)` para
verificar el cierre del WebSocket. Se corrigieron: la línea larga se partió, y las cuatro
aserciones pasaron a `pytest.raises(WebSocketDisconnect)`, la excepción concreta que
`fastapi.testclient` lanza cuando el servidor cierra la conexión durante el *handshake*.)

## Suite de pruebas del backend (Docker, PostgreSQL/Redis reales)

```
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | ........................................................................ [ 35%]
backend-1  | ........................................................................ [ 70%]
backend-1  | .............................................................            [100%]
...
backend-1  | 205 passed, 4 warnings in 79.10s (0:01:19)
backend-1 exited with code 0
```

205 pruebas en verde — las 190 heredadas de los Sprints 1-17 más 15 nuevas de
`test_realtime_integration.py`: rechazo del *handshake* sin token / con token inválido / de un
tutor no vinculado / de un `device_id` inexistente (los cuatro casos comparten el mismo cierre
4401/4404, sin distinguir "no existe" de "no es tuyo" para quien prueba IDs a ciegas); aceptación
del tutor dueño y del supervisado dueño en el mismo canal; propagación en vivo de un cambio de
regla (`POST /rules`) hasta el WebSocket del dispositivo supervisado conectado; propagación de un
cambio de política (`PUT /policy`) hasta el WebSocket de un tutor conectado; registro del token de
push únicamente por el propio dispositivo supervisado (404 si lo intenta el tutor); y el aviso de
despertar por FCM, simulado con `unittest.mock.patch` sobre
`app.services.realtime.send_rule_change_wake` — invocado cuando el dispositivo con token
registrado no tiene el canal abierto, y **no** invocado cuando sí lo tiene.

Se corrió dos veces (antes y después de reconstruir la imagen tras las correcciones de `ruff`):
mismo resultado, 205 pruebas, ambas veces.

## Web: lint y build

```
$ npm run lint
> netprotect-web@0.1.0-sprint1 lint
> eslint . --max-warnings=0
[exited with code 0]

$ npm run build
> netprotect-web@0.1.0-sprint1 build
> next build
...
✓ Compiled successfully in 95s
  Running TypeScript ...
  Finished TypeScript in 36.4s ...
✓ Generating static pages using 4 workers (3/3) in 12.4s
[exited with code 0]
```

`eslint` con `--max-warnings=0` confirma que el nuevo `useEffect` de `DeviceRulesPanel.tsx` (el que
abre el WebSocket) no dispara la regla `react-hooks/set-state-in-effect` documentada en
`CLAUDE.md`: el `setState` sólo ocurre dentro del callback `message` del WebSocket, un evento
asíncrono real, no en el cuerpo síncrono del efecto.

## Android: compilación

```
$ ./gradlew.bat compileDebugKotlin --console=plain
...
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 4m 20s
17 actionable tasks: 17 executed
```

Compila con la nueva dependencia `com.squareup.okhttp3:okhttp:4.12.0` (agregada sólo para
`RealtimeClient.kt`, ver `docs/sprint-18.md`) y con `RuleEnforcementService.kt` usándola.

## No se marca como verificado

- Login real de Google (límite recurrente de todos los sprints).
- Envío efectivo de una notificación FCM a un dispositivo real: no existe un proyecto Firebase en
  este entorno (ver `docs/sprint-18.md`, sección "FCM: estructura real, credenciales pendientes de
  un humano"). Lo que se verificó es la lógica que decide *cuándo* intentarlo
  (`test_a_rule_change_wakes_a_disconnected_device_with_a_registered_token`/
  `test_a_rule_change_does_not_wake_a_device_that_is_already_connected`), con la llamada de red a
  Google simulada.
- Recorrido manual en un navegador viendo `DeviceRulesPanel` recargar en vivo, o en un
  emulador/dispositivo Android viendo `RuleEnforcementService` reaccionar a un `rules_changed` —
  no se abrió ninguna de las dos interfaces en esta sesión; lo que las respalda es
  `test_realtime_integration.py` contra el backend real más la compilación limpia de ambos
  clientes.

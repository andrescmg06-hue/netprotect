# Evidencia de verificación — Sprint 6

Fecha: 05/09/2026. Equipo: `andre`, Windows 11 Pro.

## Suite completa en contenedor

```text
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 62 passed, 3 warnings in 5.47s   (primera corrida)
→ backend-1 | 62 passed, 3 warnings in 5.47s   (segunda corrida, base y Redis recreados)
→ backend-1 exited with code 0
```

21 pruebas nuevas sobre la base del Sprint 5: 17 en `tests/test_devices_integration.py` (listado,
detalle, renombrado, heartbeat y `GET /devices/me`, contra PostgreSQL y Redis reales) y 4 unitarias
en `tests/test_device_status.py` para `compute_effective_status` (sin marcador `integration`, corren
en el job rápido de CI).

Cubren específicamente de `/devices/me`: cuenta supervisada sin vincular recibe 404 `not_linked`; un
TUTOR no puede llamarlo (403); un dispositivo vinculado ve su propio estado y a su tutor; el mismo
teléfono (mismo `device_instance_id`) vinculado a dos tutores los ve a ambos; un tutor que se
desvincula desaparece de la lista sin que el dispositivo deje de existir para el supervisado.

## Backend: `ruff check`

```text
ruff check app tests alembic
All checks passed!
```

Verificado tras cada cambio (esquemas, endpoint, pruebas), no sólo al final.

## Panel web

```text
npm run lint    → sin errores ni warnings (--max-warnings=0)
npm run build   → compila TypeScript, genera build de producción, 0 errores
```

## Android

```text
./gradlew compileDebugKotlin       → BUILD SUCCESSFUL
./gradlew test assembleDebug assembleRelease → BUILD SUCCESSFUL (103 tareas, 26 ejecutadas)
```

`test assembleDebug` es exactamente el comando que corre el job `android` de CI; se ejecutó también
`assembleRelease` como comprobación adicional de este sprint.

## Entorno de desarrollo real, no sólo contenedores de prueba

Se reconstruyeron las imágenes `backend` y `web` de `compose.yaml` (el stack persistente que ya
tenía 13-27 horas corriendo) y se confirmó en caliente:

```text
GET http://localhost:8000/api/v1/health/ready → {"status":"ready","backend":"connected","database":"connected","redis":"connected"}
GET http://localhost:8000/openapi.json → incluye /api/v1/devices/me antes que /api/v1/devices/{device_id}
GET http://localhost:3000/ → contiene "Panel del tutor" (el build nuevo, no el cacheado)
```

## Cuatro problemas reales encontrados al verificar

**1. Bug en mi propia prueba, no en el producto.** `test_devices_me_lists_every_active_tutor` usaba
un `device_instance_id` distinto para el segundo canje, así que el backend — correctamente — creaba
un segundo dispositivo en vez de sumar un segundo tutor al mismo. El comportamiento real está bien:
la identidad del teléfono se resuelve por `device_instance_id`. Corregido reutilizando el mismo
identificador en la prueba, simulando el mismo teléfono canjeando el código de un segundo tutor.

**2. Migración olvidada entre corridas, no un defecto de código.** `compose.test.yaml` corre
PostgreSQL sin volumen persistente a propósito (aislamiento entre corridas). Tras la primera
verificación se reconstruyó sólo el servicio `backend` y se relanzó `up` sin repetir `run --rm
migrate`: 48 de 62 pruebas fallaron con `relation "users" does not exist` porque la base nueva no
tenía ninguna tabla. Ya estaba documentado en `CLAUDE.md` que `migrate` debe correr aparte antes de
cada `up`; este fallo confirma por qué esa nota existe.

**3. Bug real preexistente en Android, atrapado por no haber compilado antes.** `TutorScreen.kt` (
escrito en este mismo sprint, antes de este punto de la sesión) usaba
`Spacer(modifier = Modifier.width(8.dp))` sin importar `androidx.compose.foundation.layout.width`.
Nadie lo notó porque el proyecto Android no se había compilado desde que ese archivo se escribió.
`./gradlew compileDebugKotlin` lo encontró de inmediato. Corregido añadiendo el import. Es la razón
por la que este proyecto no marca nada como terminado sin haber ejecutado el build real.

**4. La regla de ESLint `react-hooks/set-state-in-effect` rechazó el patrón `async/await` natural.**
`DevicesPanel.tsx` llamaba, desde un `useEffect`, a una función `useCallback` que internamente hacía
`await` y luego `setState`. La regla (nueva en la versión de `eslint-plugin-react-hooks` que trae
Next 16) lo marca como error aunque el `setState` ocurra después de esperar una promesa, porque
rastrea la función completa, no sólo lo que pasa antes del primer `await`. Corregido reescribiendo el
efecto como una cadena `.then()/.catch()` con una bandera `cancelled`, igual al patrón que ya usaba
`page.tsx` desde el Sprint 3 — cada `setState` queda dentro de un callback de promesa resuelta, nunca
de forma síncrona en el cuerpo del efecto.

## CI en GitHub Actions

Run [`33941379603`](https://github.com/andrescmg06-hue/netprotect/actions/runs/33941379603), commit
`d86ba7e`, los 4 jobs en verde: `backend` (19s), `frontend` (31s), `integration` (50s), `android`
(1m36s).

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con salida real, incluidos los cuatro defectos
encontrados y corregidos durante la propia verificación. Una limitación honesta: el flujo completo
del panel web (iniciar sesión real con Google, ver un dispositivo vinculado en vivo) no se probó
haciendo clic en el navegador desde este entorno, porque el inicio de sesión de Google exige
interacción humana que este entorno no puede simular. Lo que sí se verificó en su lugar: la misma
API que el panel consume tiene prueba automatizada positiva y negativa para cada acción
(listar, renombrar, desvincular, autoconsulta), el build de producción del panel compila sin
errores de tipos, y el servidor de desarrollo real quedó corriendo en `localhost:3000` con el build
nuevo para que el tutor lo compruebe con su propia cuenta.

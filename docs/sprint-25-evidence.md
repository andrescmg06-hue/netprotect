# Sprint 25 — Evidencia

Todo lo que sigue se ejecutó realmente en esta máquina (Windows, Docker Desktop, emulador Android
Pixel_8 API 30 con Google APIs) durante el cierre de este sprint. Comandos y salidas reales,
recortadas donde son repetitivas.

## 1. Barrido de autorización de rutas (backend)

```
docker compose -f compose.test.yaml run --rm backend pytest -q -m "not integration" \
  tests/test_route_authorization_sweep.py -v
```

Primer intento: falló con `assert 1 > 30` — FastAPI 0.141 (versión resuelta en este proyecto) dejó
de aplanar `include_router()` en `app.routes`; una ruta incluida aparece como un objeto interno
con `effective_route_contexts()`, no como una `APIRoute` directa. Corregido recorriendo ese método
en vez de asumir que `app.routes` ya viene aplanado — ver el docstring de `_iter_routes` en
`backend/tests/test_route_authorization_sweep.py`.

Segundo intento, con el recorrido corregido: encontró dos rutas reales sin `Depends(get_current_user)`
que nadie había documentado como excepción deliberada: `GET /` (el banner estático de
`app/main.py`) y `POST /auth/logout` (se autentica por posesión del propio `refresh_token` que
revoca, no por un access token bearer — ver el comentario en `_PUBLIC_ROUTES`). Añadidas a la lista
de excepciones explícitas con su motivo documentado in situ; ninguna de las dos era un fallo de
seguridad real, pero tampoco eran una decisión escrita en ningún lado antes de esta prueba.

Resultado final:

```
tests/test_route_authorization_sweep.py .                                [100%]
1 passed, 1 warning in 0.67s
```

## 2. Pruebas instrumentadas de Android (offline/sincronización, Sprint 19)

Emulador real, no un doble: Pixel_8 (AVD), Android API 36, Google APIs.

```
.\gradlew.bat connectedDebugAndroidTest
```

Primer intento: `com.android.builder.testing.api.DeviceException: No connected devices!` — el
emulador había arrancado pero `adb` seguía reportándolo `unauthorized` (variantes `google_apis
playstore`/imágenes recientes simulan el diálogo de autorización de depuración USB incluso en el
propio emulador). Resuelto con `adb kill-server && adb start-server`, que fuerza un nuevo
*handshake* y lo deja en `device`.

Segundo intento, con el emulador autorizado:

```
Starting 8 tests on Pixel_8(AVD) - 16
Finished 8 tests on Pixel_8(AVD) - 16
BUILD SUCCESSFUL in 48s
```

`app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_8(AVD) - 16-_app-.xml`:
`tests="8" failures="0" time="2.03"`. Las 8 pruebas, reales sobre Room con SQLite real
(`Room.inMemoryDatabaseBuilder`, `room-testing`):

- `RulesCacheStoreTest`: `a_device_that_never_synced_has_no_cache`,
  `what_was_cached_is_what_comes_back`, `a_second_fetch_replaces_the_first_wholesale_not_merged`,
  `caches_for_two_devices_do_not_leak_into_each_other`.
- `PendingRuleEventStoreTest`: `an_enqueued_block_survives_until_flushed`,
  `a_block_that_cannot_be_reported_stays_queued_after_flush`,
  `an_unrecognized_reason_is_dropped_without_ever_touching_the_network`,
  `flush_stops_at_the_first_failure_instead_of_retrying_every_row`.

Las cuatro últimas ejercitan `PendingRuleEventStore.flush()` contra un `RuleEnforcementClient` real
apuntado a un puerto que nadie escucha (`http://127.0.0.1:59999`) — el fallo de red es genuino
(`ConnectException` real), no simulado, porque este proyecto no tiene librería de *mocking*.

`.\gradlew.bat test assembleDebug` (el job `android` ya existente) sigue en verde tras estos
cambios: `BUILD SUCCESSFUL in 1m 18s`.

## 3. Colección de API con Newman

```
docker compose -f compose.test.yaml up -d db redis api_server
seed=$(docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py)
node api-tests/build_environment.js "$seed" > api-tests/environment.json
npx newman run api-tests/netprotect.postman_collection.json -e api-tests/environment.json
```

Salida real:

```
┌─────────────────────────┬───────────────────┬───────────────────┐
│                         │          executed │            failed │
├─────────────────────────┼───────────────────┼───────────────────┤
│              iterations │                 1 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│                requests │                13 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│            test-scripts │                13 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│              assertions │                23 │                 0 │
├─────────────────────────┴───────────────────┴───────────────────┤
│ total run duration: 2s                                          │
└─────────────────────────────────────────────────────────────────┘
```

13 peticiones reales contra el backend real: roles, generación y canje de un código de
vinculación de verdad, creación/listado de una regla, lectura de reglas activas por el propio
dispositivo, *heartbeat* real (el dispositivo pasa a `ONLINE`), el caso anti-IDOR (un tutor sin
relación con el dispositivo recibe 404, no 403 ni 200) y limpieza (borrar la regla, desvincular el
dispositivo). 0 fallos.

## 4. E2E web con Playwright

Contra un build de producción real, no `next dev`:

```
npm run build && node -e "fs.cpSync(...)" && node .next/standalone/server.js
```

Primer intento: `"next start" does not work with "output: standalone" configuration` —
`next.config.ts` fija `output: "standalone"` desde el Sprint 24 (imagen Docker liviana), que
requiere `node .next/standalone/server.js` y copiar a mano `public/` y `.next/static` junto al
`server.js` (limitación documentada de Next.js, no un error de configuración). Corregido en
`playwright.config.ts`.

Segundo intento: los 3 tests (separados) fallaron los últimos dos con `invalid_refresh_token`. La
causa real: el *refresh token* de este proyecto es rotativo y de un solo uso (Sprint 3) — el
primer test lo consumía al iniciar sesión, y los siguientes intentaban reusar el mismo valor ya
invalidado. Corregido rediseñando el spec como **un solo test continuo** (`test.step` para
reportar cada fase por separado) que inicia sesión una vez y navega dentro de la misma página —
ver el comentario en `frontend/e2e/dashboard.spec.ts` explicando por qué esto no es una preferencia
de estilo sino una consecuencia del propio ciclo de vida del token.

Resultado final:

```
Running 1 test using 1 worker
  ok 1 e2e\dashboard.spec.ts:27:5 › a returning tutor can navigate the whole dashboard and see real backend data (1.1s)
1 passed (20.9s)
```

Verifica, contra el backend real y un dispositivo/regla creados de verdad por `global-setup.ts`:
login por *refresh* real (no la pantalla de "Inicia sesión con Google"), navegación real por la
barra lateral entre "Resumen" → "Dispositivos" → "Vinculación" → "Reglas por aplicación", que el
dispositivo recién vinculado aparece en la lista, y que la regla recién creada aparece en su panel.

## 5. Rendimiento con k6

Perfil moderado y repetible (10 VUs sostenidos 30s, rampas de 10s/5s), no una prueba de estrés —
ver `docs/sprint-25.md` para el porqué. Contra el backend real en Docker:

```
K6_BIN=k6 bash perf/run.sh
```

Salida real (segunda corrida, para descartar variación anómala de la primera):

```
  █ THRESHOLDS
    http_req_duration
    ✓ 'p(95)<500' p(95)=17.12ms
    http_req_failed
    ✓ 'rate<0.01' rate=0.00%

  █ TOTAL RESULTS
    checks_total.......: 1110    24.361597/s
    checks_succeeded...: 100.00% 1110 out of 1110
    checks_failed......: 0.00%   0 out of 1110

    HTTP
    http_req_duration..............: avg=11.34ms min=6.41ms med=10.64ms max=28.12ms p(90)=14.63ms p(95)=17.12ms
    http_req_failed................: 0.00%  0 out of 1110
    http_reqs......................: 1110   24.361597/s

    EXECUTION
    iterations.....................: 370    8.120532/s
    vus_max........................: 10     min=10        max=10
```

Primera corrida (para contexto, misma máquina): p95=34.6ms, 1089 checks, 100% éxito — la variación
entre corridas (17ms vs. 35ms de p95) es exactamente la clase de ruido que se espera del contenedor
de desarrollo de una máquina de escritorio bajo carga variable (Docker Desktop, el emulador Android
y Gradle corrían simultáneamente durante la primera corrida), y es precisamente el motivo por el
que `docs/sprint-25.md` documenta esta prueba como una referencia repetible y no como un veredicto
de capacidad: los números absolutos de **esta** máquina no dicen nada sobre el despliegue real del
Sprint 26, pero el propio script sí sirve para comparar antes/después una vez que ese despliegue
exista.

## 6. Suite completa de backend (regresión)

Verificado que ningún cambio de este sprint (Dockerfile, `compose.test.yaml`, el barrido de
autorización) rompió nada de lo ya existente:

```
docker compose -f compose.test.yaml down -v
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm backend ruff check --no-cache app tests alembic
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

```
All checks passed!                     # ruff
262 passed, 4 warnings in 46.76s        # pytest — 261 previas + la nueva del barrido de rutas
```

## 7. Frontend (regresión, por los archivos nuevos de Playwright/`package.json`)

```
npm run lint    # eslint . --max-warnings=0 — limpio
npm run build   # Next 16 + TypeScript estricto — "Compiled successfully"
```

## Hallazgos reales encontrados en el camino (resumen)

1. FastAPI 0.141 ya no aplana `include_router()` en `app.routes` — el barrido de rutas necesitó
   recorrer `effective_route_contexts()`.
2. `GET /` y `POST /auth/logout` no tenían `Depends(get_current_user)` y nunca se habían
   documentado como excepciones deliberadas — ninguna era un fallo real, pero tampoco eran una
   decisión escrita antes de esta prueba.
3. Imágenes de sistema Android `google_apis_playstore`/recientes dejan el emulador en
   `unauthorized` para `adb` hasta un `kill-server`/`start-server` — no es exclusivo de dispositivos
   físicos.
4. `python scripts/seed_test_session.py` fallaba con `ModuleNotFoundError: No module named 'app'`
   sin `PYTHONPATH=/app` — un script invocado por ruta no hereda el directorio de trabajo en
   `sys.path` como sí lo hacen uvicorn/alembic/pytest. Corregido en el `Dockerfile`.
5. `next start` rechaza correr con `output: "standalone"` (Sprint 24) — Playwright necesita
   `node .next/standalone/server.js` y copiar `public/`/`.next/static` a mano, exactamente como ya
   hace `frontend/Dockerfile`.
6. El *refresh token* rotativo de un solo uso (Sprint 3) rompe cualquier suite E2E que intente
   iniciar sesión más de una vez con el mismo valor sembrado — cada corrida de Playwright/Newman/k6
   necesita su propia siembra fresca, y dentro de una misma corrida, un solo login por sesión de
   navegador.

## No ejecutado en esta sesión

- Cualquier prueba que dependa de un login real de Google, como en cada sprint anterior de
  autenticación.

## CI en GitHub Actions

Commit `9b06971` ("feat: add sprint 25 integration testing (auth sweep, offline Room tests,
API/E2E/perf)"), corrida
[34738208116](https://github.com/andrescmg06-hue/netprotect/actions/runs/34738208116):

```
✓ backend                32s     (04:34:32 → 04:35:08)
✓ frontend                30s     (04:34:32 → 04:35:02)
✓ integration             1m19s   (04:34:32 → 04:35:51)
✓ android                 3m3s    (04:34:31 → 04:37:34)
✓ api-collection          58s     (04:34:32 → 04:35:30)
✓ e2e                     1m29s   (04:34:32 → 04:36:01)
✓ performance             1m39s   (04:34:31 → 04:36:10)
✓ android-instrumented    5m26s   (04:34:32 → 04:39:58)
```

Los 8 jobs en verde en un runner limpio de GitHub Actions, incluidos los 4 que este sprint agrega.
`android-instrumented` es, como se esperaba, el más lento (arranca un emulador real vía
`reactivecircus/android-emulator-runner@v2` sobre KVM) pero corrió sin ningún ajuste adicional
sobre lo ya validado localmente — ni el arranque del emulador en runner ni el paso de habilitar KVM
necesitaron corrección. Sprint 25 cerrado en firme.

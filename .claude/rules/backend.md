---
paths:
  - "backend/**"
---

# Backend (FastAPI + SQLAlchemy 2.0 + Alembic)

- `ruff check app tests alembic` debe pasar limpio (config en `backend/pyproject.toml`); `B008`
  está ignorado a propósito — es el patrón `Depends(...)` de FastAPI, no el bug que esa regla busca.
- Modelos con `Mapped`/`mapped_column` (SQLAlchemy 2.0), nunca el estilo `Column` antiguo.
- Secretos: cada uno con su propia variable y su propio valor — nunca reutilizar `JWT_SECRET` para
  otra cosa "porque ya existe". Uno nuevo se genera con `secrets.token_urlsafe(48)` y se documenta
  en los tres `.env.*.example`.
- Toda ruta con un id de dispositivo pasa por `api/deps.py` (`require_tutor_of_device`,
  `require_supervised_owner_of_device`, `require_device_participant`), nunca comprobada a mano.
- "No existe" y "no es tuyo" devuelven el mismo 404 uniforme — un 403 que revela existencia es un
  hallazgo de seguridad.
- Verificar rápido con `ruff check app tests alembic` + `pytest -q -m "not integration"`; completo
  con `make test` (siempre en contenedor). Ver skill `verificar`.

---

**Nota del Sprint 18, válida para cualquier sprint futuro que toque el canal en tiempo real o
FCM**: `google-services.json`/el SDK de Firebase Messaging **no** se agregaron a Android — el
plugin de Gradle `com.google.gms.google-services` rompe la compilación completa si ese archivo no
existe, y no hay proyecto Firebase real en este repo (pendiente de un humano con cuenta de Google
Cloud, igual que `GOOGLE_WEB_CLIENT_ID` en su momento — ver `docs/planning/plan-desarrollo.md`,
fila "Paso 17"). Lo que sí existe y funciona sin esa credencial: `devices.fcm_token`, el endpoint
para registrarlo, y `app/services/push.py`, que con `FCM_PROJECT_ID` vacío (el valor por defecto)
omite el envío con un log en vez de fallar el cambio de regla que lo disparó — la llamada real a
Google está aislada en una función (`_post_fcm_message`) para poder simularla con
`unittest.mock.patch`, mismo patrón que la verificación de ID tokens de Google desde el Sprint 3.
El registro de conexiones WebSocket vive en memoria del proceso backend, no en Redis: correcto hoy
porque el backend corre como un único *worker* de uvicorn (`backend/Dockerfile`, sin `--workers`);
necesitaría Redis pub/sub el día que corra en más de una réplica. En Android se agregó OkHttp
(`RealtimeClient.kt`) sólo para el WebSocket — el resto de los clientes de red sigue en
`java.net.HttpURLConnection` (ver `HttpJsonClient.kt`), porque `java.net` no tiene cliente de
WebSocket en absoluto y `java.net.http.WebSocket` sólo llegó a Android en la API 34, por encima del
`minSdk 26` de este proyecto. Ver `docs/sprint-18.md`.

**Nota del Sprint 21, válida para cualquier sprint que toque rate limiting, cabeceras o el
cliente de Redis**: hay **dos** criterios de rate limiting a propósito, y confundirlos rompe uno
de los dos. `app/core/rate_limit.py` (`enforce_rate_limit`, usado por `/auth/google`,
`/auth/refresh` y `/pairing/*`) falla **cerrado** — 503 si Redis no responde —, porque una
protección anti-fuerza-bruta que desaparece en silencio no es protección. El limitador **global**
de `enforcement_middleware` (`app/main.py`, todas las rutas salvo `/api/v1/health*`) falla
**abierto**, porque tumbar el 100% de la API por una caída de Redis es una regresión de
disponibilidad desproporcionada para una capa genérica anti-abuso. `/api/v1/health*` queda
excluido del conteo por dos razones, ambas reales: un chequeo de salud no debe depender del mismo
Redis que vigila, y `test_health.py` corre en el job `backend` de CI **sin infraestructura**
(cualquier cosa que haga tocar Redis a `/health` rompe ese job). Consecuencia descubierta al
correr la suite completa: como ahora *cada* petición toca Redis, la carrera de *event loop*
cruzado que el docstring de `close_redis()` ya advertía dejó de ser teórica — `hit_rate_limit()`
traduce por eso `(RedisError, OSError, RuntimeError)` a `RateLimitBackendError`, y el
`RuntimeError` de ese trío **no es decorativo**: una conexión con el transporte roto no lanza
`RedisError`, y sin esa traducción reventaba la petición con un 500 en vez del *fail-open*
diseñado. `compose.test.yaml` sube `RATE_LIMIT_GLOBAL_MAX_PER_IP` a 100000 porque la suite entera
sale de una sola IP simulada; los tests que ejercitan el limitador fijan su propio valor por test.
TLS: lo que existe es *enforcement* (en producción, `request.url.scheme != "https"` → 400), no
*terminación* — el certificado y el proxy inverso siguen siendo del Paso 25, que exige un dominio
real. HSTS y CSP sólo se envían en producción: en dev/test romperían los assets de Swagger UI, que
ahí sigue habilitado. Ver `docs/sprint-21.md` y `docs/sprint-21-evidence.md` (incluye los
hallazgos reales de ZAP ya corregidos y los de MobSF revisados uno por uno).

**Nota del Sprint 22, válida para cualquier sprint futuro que toque auditoría**: `AuditLog`
(`app/models/audit_log.py`, tabla `audit_logs` desde el Sprint 2, escrita desde el Sprint 3) no
tiene columna `device_id` — sólo modela quién (`actor_user_id`) hizo qué (`action`) sobre qué
(`resource_type`/`resource_id`, string libre cuyo significado cambia según el tipo). Por eso
`GET /users/me/audit`/`GET /users/me/audit/export` (nuevos) están **scopeados a "mis propias
acciones"** (`actor_user_id == current_user.id`), no a "todo lo que pasó en mis dispositivos" —
esto último exigiría inventar una regla de reconstrucción por tipo de recurso que el modelo no
soporta directamente. Consecuencia aceptada: `DEVICE_LINKED` se audita con el **supervisado**
como actor (quien redime el código), así que no aparece en la auditoría del tutor aunque el
dispositivo sea suyo — no es pérdida real, el tutor ya ve el estado de vinculación en la lista de
dispositivos desde el Sprint 6. Sin `require_role`: cualquier usuario autenticado consulta sólo su
propio registro, sin que el rol conceda ni restrinja nada (el filtro por `actor_user_id` ya hace
imposible ver información ajena). Paginación real (`limit`/`offset` + `total`), divergencia
consciente del tope fijo `MAX_X` de historial/alertas (Sprint 15/17): esas tablas purgan a los 90
días y acotan su volumen real; `AuditLog` no tiene retención — el plan la llama explícitamente
"registro inmutable" — así que un tope fijo sin paginación ocultaría permanentemente lo más
antiguo. La exportación (`StreamingResponse`, CSV) reutiliza el mismo filtro sin paginar, con un
tope de seguridad fijo (`MAX_AUDIT_EXPORT_ROWS = 10 000`) en su lugar. Ni el listado ni la
exportación auditan su propia lectura (mismo criterio ya usado por `list_alerts`/
`get_device_history`: el rastro registra acciones a revisar después, no la revisión en sí). Sólo
panel web (filtros + paginación + export); Android es sólo lectura, sin filtros, y es la primera
sección de `TutorScreen` que es de cuenta en vez de por dispositivo. Ver `docs/sprint-22.md`.

**Nota del Sprint 25, válida para cualquier sprint futuro que toque el router de FastAPI, Newman,
Playwright o k6**: la versión de FastAPI resuelta en este proyecto (0.141) ya no aplana
`include_router()` en `app.routes` — una ruta incluida aparece como un objeto interno con
`effective_route_contexts()`, no como una `APIRoute` directa. `test_route_authorization_sweep.py`
(nuevo, `backend/tests/`) recorre ese método por *duck-typing* (comprobando el atributo, no
importando la clase interna) para no quedar atado a un nombre privado que puede cambiar en otra
versión. Esa misma prueba dejó documentadas dos excepciones reales que nadie había escrito antes:
`GET /` (el banner estático) y `POST /auth/logout` (se autentica por posesión del propio
`refresh_token` que revoca, no por un *access token* bearer — el punto entero de logout es
funcionar incluso con el *access token* ya expirado). Newman/Playwright/k6 no pueden mockear la
verificación de Google como sí hacen los tests de pytest (`unittest.mock.patch` sólo funciona
dentro del mismo proceso) — los tres usan `backend/scripts/seed_test_session.py` (nuevo,
copiado sólo en el stage `test` del `Dockerfile`, nunca en `runtime`) para mintar una sesión real
con las funciones **propias y no mockeadas** del proyecto, mismo mecanismo que ya usó a mano
`docs/sprint-24-evidence.md`; no es una puerta trasera de autenticación, ningún archivo de
`backend/app` cambia. `compose.test.yaml` ganó un servicio `api_server` (misma imagen de test que
`backend`/`migrate`, sólo con `command: uvicorn ...` y puerto publicado) porque `backend` ahí corre
pytest y termina — Newman/Playwright/k6 necesitan un servidor HTTP real que se quede arriba.
Descubierto de la manera difícil: el *refresh token* de este proyecto es rotativo y de un solo uso
(Sprint 3) — una suite E2E que inicia sesión más de una vez con el mismo valor sembrado falla la
segunda vez con `invalid_refresh_token`; `frontend/e2e/dashboard.spec.ts` por eso es **un** test
continuo (con `test.step` para el reporte) en vez de varios separados. Playwright corre contra
`node .next/standalone/server.js` (con `public/`/`.next/static` copiados a mano, como ya hace
`frontend/Dockerfile`), no contra `next start`/`next dev`: `next.config.ts` fija
`output: "standalone"` desde el Sprint 24, y `next start` se niega a arrancar con esa
configuración. Las pruebas instrumentadas de Android (`RulesCacheStoreTest`,
`PendingRuleEventStoreTest`, nuevas en `mobile/app/src/androidTest/`) cubren el offline/
sincronización del Sprint 19, sin ninguna cobertura hasta este sprint porque Room exige un runtime
Android real (o Robolectric, no instalado aquí); `PendingRuleEventStoreTest` apunta su
`RuleEnforcementClient` a un puerto sin nada escuchando para forzar un fallo de red **genuino**, no
simulado — este proyecto no tiene librería de *mocking*. Un emulador con imagen
`google_apis_playstore`/reciente puede quedar en `unauthorized` para `adb` incluso sin ser un
dispositivo físico; se resuelve con `adb kill-server && adb start-server`. La prueba de k6
(`perf/load-test.js`) es a propósito un perfil moderado y repetible, no de estrés: sin la
infraestructura real del Sprint 26, cualquier número de quiebre sólo describiría el contenedor de
desarrollo de quien la ejecute. Ver `docs/sprint-25.md` y `docs/sprint-25-evidence.md`.

- Un JWT firmado con HS256 puede coincidir carácter por carácter con otro si sólo cambia el último
  byte de la firma (relleno de Base64). Para pruebas que "alteran" un token, tocar un carácter del
  medio, no el último.
- Los tokens de acceso necesitan un `jti` aleatorio: sin él, dos emitidos en el mismo segundo para el
  mismo usuario son idénticos.
- Mezclar `TestClient` (corre la app en su propio *event loop*) con un fixture de base de datos que
  usa el motor global de la aplicación falla en contenedor (asyncpg rechaza conexiones de otro loop).
  El fixture compartido en `backend/tests/conftest.py` crea su propio motor por test; usarlo siempre.
- Las pruebas que ejercitan rate limiting necesitan una IP/host único por test — los contadores viven
  15 minutos en Redis y se filtran entre pruebas si comparten dirección.
- Una excepción dentro de la tarea de un **WebSocket** no se ve: no aparece traza en la salida de
  pytest ni se cierra el socket. Se manifiesta como una prueba **colgada para siempre**, no como
  una prueba en rojo (en el Sprint 23 dejó un contenedor ocho horas en `[ 55%]`). Si una prueba de
  WebSocket se cuelga, sospechar de una excepción silenciosa en el handler antes que de la lógica
  del test — se localiza instrumentando con `print(..., flush=True)`. Caso concreto ya sufrido:
  tras `db.expire_all()`, leer cualquier atributo de un objeto ORM cargado antes (incluido su
  `id`) dispara una recarga perezosa que muere así; por eso la conexión guarda sólo el `user_id`.

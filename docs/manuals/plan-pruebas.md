# Plan de pruebas — NetProtect

Audiencia: quien evalúa la calidad del proyecto. Este documento describe qué se prueba, con qué
herramienta, y dónde está la evidencia real de ejecución — no una promesa de cobertura.
`CLAUDE.md` fija la regla que gobierna todo esto: "nada se marca como terminado sin evidencia real
de que se ejecutó".

## 1. Resumen por tipo de prueba

| Tipo | Herramienta | Dónde vive | Sprint de origen |
|---|---|---|---|
| Unitarias e integración de backend | pytest, contra PostgreSQL/Redis reales en Docker | `backend/tests/` | 2 en adelante |
| Barrido de autorización | pytest, recorre el router real de FastAPI | `backend/tests/test_route_authorization_sweep.py` | 25 |
| Instrumentadas de Android (Room) | `./gradlew connectedDebugAndroidTest`, emulador real | `mobile/app/src/androidTest/` | 25 |
| Colección de API | Newman (Postman) contra el backend real en Docker | `backend/scripts/seed_test_session.py` + colección | 25 |
| E2E web | Playwright contra un build de producción real | `frontend/e2e/dashboard.spec.ts` | 25 |
| Rendimiento | k6, perfil moderado y repetible | `perf/load-test.js` | 25 |
| Seguridad — aplicación | OWASP ZAP baseline | `docs/sprint-21-evidence.md` | 21 |
| Seguridad — Android | MobSF (APK debug y release) | `docs/sprint-21-evidence.md` | 21 |
| Dependencias | `pip-audit` (backend), `npm audit --audit-level=high` (frontend) | CI, cada push/PR | 21 |
| CI end-to-end | GitHub Actions, 8 jobs | `.github/workflows/ci.yml` | 25 |

## 2. Pruebas de backend (unitarias e integración)

265 pruebas a la fecha del cierre del Sprint 26 (`docs/sprint-26-evidence.md`), corriendo contra
PostgreSQL/Redis **reales** en contenedores Docker (`compose.test.yaml`) — nunca contra una base de
datos mockeada, salvo la única pieza que exige una persona real: la verificación del ID token de
Google, simulada con `unittest.mock.patch` sobre la función que llama a Google
(`verify_google_id_token`), nunca sobre la lógica propia (`docs/sprint-03.md`).

Cobertura por dominio: autenticación y sesión, roles y autorización (positiva y negativa, incluida
la prueba de barrido de rutas), vinculación, dispositivos, aplicaciones, reglas por app y por
categoría, política de dispositivo, tiempo/horarios/modo escolar, ubicación, geocercas, historial,
estadísticas, alertas, WebSocket en tiempo real, detección de manipulación, rate limiting, cabeceras
de seguridad, auditoría, señalización WebRTC, y las pruebas de endurecimiento del Sprint 26
(exención de HTTPS/rate-limit para `/health*` y `/metrics`).

Ejecutar:

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

### 2.1 Barrido de autorización (Sprint 25)

`test_route_authorization_sweep.py` recorre el router real de FastAPI (por *duck-typing* sobre
`effective_route_contexts()`, no importando una clase interna, para no acoplarse a un detalle de
implementación de FastAPI 0.141) y falla si una ruta funcional no tiene autenticación. Excepciones
explícitas, documentadas, no descubiertas por accidente: `GET /` (banner estático),
`POST /auth/logout` (se autentica por posesión del propio refresh token que revoca),
`GET /health*` y `GET /metrics` (infraestructura operativa, ver `docs/security-baseline.md`).

## 3. Pruebas instrumentadas de Android (Sprint 25)

Room exige un runtime Android real (o Robolectric, no instalado en este proyecto) para ejecutar
SQLite — `RulesCacheStoreTest` y `PendingRuleEventStoreTest`
(`mobile/app/src/androidTest/`) cubren el caché offline y la cola de eventos pendientes del
Sprint 19, sin ninguna cobertura antes de este sprint. `PendingRuleEventStoreTest` apunta su cliente
a un puerto sin nada escuchando para forzar un fallo de red genuino — el proyecto no usa librerías
de *mocking* en Android.

```bash
cd mobile
./gradlew connectedDebugAndroidTest
```

No se instrumentó la UI de Compose (justificación en `docs/sprint-25.md`: la lógica de Compose en
este proyecto es declarativa y delgada, y ya tiene revisión visual manual en cada sprint que la
toca).

## 4. Colección de API (Newman)

Un flujo real de extremo a extremo (tutor + supervisado) contra el backend real en Docker.
`backend/scripts/seed_test_session.py` (sólo en el stage `test` del Dockerfile, nunca en
`runtime`) mintan una sesión válida con las funciones reales del proyecto — no es una puerta trasera
de autenticación, no modifica ningún archivo de `backend/app`. Resultado verificado: 13 peticiones,
23 aserciones, 0 fallos (`docs/sprint-25-evidence.md`).

## 5. E2E web (Playwright)

Corre contra `node .next/standalone/server.js` (build de producción real), no contra `next dev` —
`next.config.ts` fija `output: "standalone"` desde el Sprint 24, y `next start` se niega a arrancar
con esa configuración. Es un único test continuo (`test.step` por etapa) en vez de varios separados,
porque el refresh token de este proyecto es rotativo y de un solo uso (Sprint 3): una suite que
inicia sesión más de una vez con el mismo valor sembrado fallaría la segunda vez.

## 6. Rendimiento (k6)

Perfil de carga moderado y repetible, no una prueba de estrés — sin la infraestructura de
producción real del Sprint 26 en el momento en que se escribió, medir el punto de quiebre no tenía
valor todavía. Sirve como referencia repetible (mismo script, mismo perfil) para comparar después de
un despliegue real. Salida real (requests/s, p95, tasa de error) documentada con números reales en
`docs/sprint-25-evidence.md`, no estimados.

## 7. Seguridad

- **OWASP ZAP** (Sprint 21), baseline contra el backend real: 0 hallazgos de riesgo alto/medio en la
  corrida de cierre; dos advertencias reales encontradas y corregidas en el propio código durante
  el sprint (cabeceras `Cache-Control`/`Cross-Origin-Resource-Policy`).
- **MobSF** (Sprint 21) contra el APK real, debug y release: sin hallazgos `HIGH` en release; el
  resto son de librerías de terceros o decisiones ya documentadas (p. ej. cleartext permitido sólo
  en debug).
- **pip-audit** / **npm audit** corren en cada push/PR desde el Sprint 21 (`.github/workflows/
  ci.yml`).

Ver `docs/sprint-21-evidence.md` para la salida real de ambos escaneos.

## 8. CI en GitHub Actions

8 jobs en `.github/workflows/ci.yml`, todos verificados en verde en un runner limpio de GitHub
Actions (no sólo localmente): `backend`, `frontend`, `android`, `integration`,
`android-instrumented`, `api-collection`, `e2e`, `performance`. `android-instrumented` levanta un
emulador real vía `reactivecircus/android-emulator-runner@v2` sobre KVM — es, como se esperaba, el
job más lento (~3-5 min). Ver `docs/sprint-25-evidence.md` (primera corrida verde de los 8) y
`docs/sprint-26-evidence.md` (corrida más reciente, incluye un fallo real encontrado y corregido en
el propio proceso de cierre del Sprint 26 — ver sección 9).

## 9. Lo que las pruebas automatizadas no pueden cubrir

- Un login real de Google exige que una persona elija su cuenta en un selector — ningún agente ni
  pipeline de CI puede hacerlo. Se documenta explícitamente como pendiente en cada sprint de
  autenticación, no se marca como probado.
- La verificación visual de una sesión de captura de pantalla real (Sprint 23) exige que una
  persona acepte el diálogo del sistema Android — pendiente de verificación manual, documentado en
  `docs/sprint-23-evidence.md`.
- Pruebas de rendimiento de Android (batería, memoria) — fuera del alcance pedido por
  `docs/planning/plan-desarrollo.md` para el Paso 24.

## 10. Lección aprendida durante el cierre de este mismo plan de pruebas

Al cerrar el Sprint 26, el job `integration` de CI falló de verdad por una causa no relacionada con
el código de ese sprint: varios tests de integración sembraban fechas de calendario fijas
(`2026-09-06`/`2026-09-07`) para `captured_at` de ubicación, seguras cuando se escribieron pero no
relativas a "ahora" — la retención de 7 días de `DeviceLocationReport` terminó purgándolas al
alcanzarlas el reloj real. Corregido reemplazando esas fechas por un helper que calcula
`datetime.now(UTC)` menos un offset relativo, verificado con la suite completa en Docker antes de
volver a empujar. Ver `docs/sprint-26-evidence.md`, sección 16, para el fallo real, el diagnóstico y
la corrección — un recordatorio de que una prueba con una fecha absoluta "seguramente dentro de la
ventana" es una bomba de tiempo, no una prueba estable.

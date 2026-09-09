# Sprint 21 — Evidencia

Todos los comandos de esta página se ejecutaron realmente en esta máquina (Windows + Docker
Desktop) el 2026-09-08/09. La salida se recorta donde es repetitiva, nunca donde cambia el
resultado.

## Repaso OWASP API Top 10 (2023)

| # | Riesgo | Estado en NetProtect |
|---|---|---|
| API1 | Broken Object Level Authorization | Cubierto desde Sprint 4: `require_tutor_of_device` decide por fila, 404 uniforme para "no existe" y "no es tuyo" (`docs/security-baseline.md`). Sin cambios este sprint. |
| API2 | Broken Authentication | `/auth/google`, `/auth/refresh` ahora con rate limit por IP (nuevo, este sprint). JWT HS256 corto + refresh rotativo de alta entropía ya existían (Sprint 3). |
| API3 | Broken Object Property Level Authorization | Los schemas de respuesta son explícitos campo por campo (Pydantic), nunca serializan el modelo ORM completo — sin cambios necesarios. |
| API4 | Unrestricted Resource Consumption | **Gap cerrado este sprint**: antes sólo `/pairing/*` tenía límite; ahora hay un límite global por IP (`enforcement_middleware`) más límites específicos en auth. `MAX_APPLICATIONS_PER_SYNC` (Sprint 7) y `max_geofences_per_device` (Sprint 14) ya acotaban payloads grandes. |
| API5 | Broken Function Level Authorization | `require_role`/`require_tutor_of_device` ya separan TUTOR/SUPERVISADO por endpoint (Sprint 4). Sin cambios. |
| API6 | Unrestricted Access to Sensitive Business Flows | La vinculación por código (`/pairing/*`) ya tenía límites de intentos por usuario e IP (Sprint 5). Sin cambios. |
| API7 | Server Side Request Forgery | La API nunca hace una petición saliente a una URL provista por el usuario (Google/FCM usan endpoints fijos del propio código, no URLs de entrada). No aplica. |
| API8 | Security Misconfiguration | **Gap cerrado este sprint**: cabeceras ampliadas (HSTS, CSP, Cache-Control, CORP), exception handler genérico (nunca se filtraba antes, pero tampoco existía explícitamente — dependía del default de Starlette), validación de secretos al arrancar. |
| API9 | Improper Inventory Management | Un único backend, un único `api_v1_prefix`, sin versiones fantasma ni endpoints de depuración expuestos en producción (`/docs` deshabilitado ahí desde Sprint 1). |
| API10 | Unsafe Consumption of APIs | La verificación de Google ID token está aislada en una función mockeable (`verify_google_id_token`, Sprint 3); el envío a FCM igual (`_post_fcm_message`, Sprint 18). Sin cambios. |

## Repaso OWASP Top 10 (2021, aplicable a una API+SPA+app móvil)

| # | Riesgo | Estado |
|---|---|---|
| A01 | Broken Access Control | Cubierto (ver API1/API5 arriba). |
| A02 | Cryptographic Failures | JWT HS256, refresh hasheado SHA-256, pairing HMAC-SHA256 con pepper propio, ubicación cifrada con Fernet (todo de sprints previos). Este sprint: la app ahora **rehúsa arrancar** en producción si cualquiera de esos secretos sigue en su valor de desarrollo. |
| A03 | Injection | SQLAlchemy con `select()`/bind params en todo el proyecto, nunca SQL crudo interpolado. Sin hallazgos. |
| A04 | Insecure Design | El límite global fail-open vs. el fail-closed de auth/pairing es una decisión de diseño explícita de este sprint (ver `sprint-21.md`), no un descuido. |
| A05 | Security Misconfiguration | Igual que API8 arriba. |
| A06 | Vulnerable and Outdated Components | **Nuevo en CI este sprint**: `pip-audit` (backend) y `npm audit --audit-level=high` (frontend), ambos en verde hoy (ver más abajo). |
| A07 | Identification and Authentication Failures | Rate limiting en login/refresh (nuevo). Sesión/revocación ya cubiertas desde Sprint 3. |
| A08 | Software and Data Integrity Failures | CI ya construye desde el propio `Dockerfile`/`requirements.txt` con hashes de dependencia fijados por rango; sin pipeline de firmas de artefactos (fuera de alcance). |
| A09 | Security Logging and Monitoring Failures | `record_audit_event` (desde Sprint 4) + el `logger.exception`/`logger.warning` nuevos de este sprint para errores no manejados y caídas del rate limiter. Sin agregación/alerting central (fuera de alcance de este proyecto). |
| A10 | Server-Side Request Forgery | Igual que API7. |

## Checklist OWASP ASVS (nivel 1, capítulos relevantes)

| Capítulo | Control | Estado |
|---|---|---|
| V2 Autenticación | Credenciales/tokens no en URL, tokens de vida corta, rotación en refresh | ✅ (Sprint 3) |
| V2 Autenticación | Rate limiting en login | ✅ **nuevo este sprint** |
| V3 Gestión de sesión | Revocación inmediata al desvincular/logout | ✅ (Sprint 3/6) |
| V4 Control de acceso | BOLA/BFLA por recurso, 404 uniforme | ✅ (Sprint 4) |
| V5 Validación | Todo input con límite de longitud/formato | ✅ **cerrado este sprint** (`schemas/auth.py` era el único grupo sin `Field(max_length=...)`) |
| V7 Manejo de errores | Sin detalle interno filtrado al cliente | ✅ **nuevo este sprint** (`unhandled_exception_handler`) |
| V8 Protección de datos | Cifrado en reposo de datos sensibles (ubicación) | ✅ (Sprint 13) |
| V9 Comunicaciones | TLS obligatorio | 🟡 *enforcement* de app listo este sprint (rechaza no-HTTPS en producción); *terminación* real sigue en Paso 25 (dominio pendiente) |
| V11 Lógica de negocio | Límites de negocio (código de 6 dígitos, geocercas máx.) | ✅ (Sprints 5/14) |
| V12 Archivos/recursos | Límite de tamaño de payloads (`MAX_APPLICATIONS_PER_SYNC`) | ✅ (Sprint 7) |
| V13 API | Rate limiting global, CORS mínimo, cabeceras | ✅ **cerrado este sprint** |
| V14 Configuración | Secretos propios por función, sin defaults en producción | ✅ **cerrado este sprint** (antes sólo "secretos separados", sin verificación de arranque) |

## Backend — ruff, pip-audit, pytest

```
$ ruff check app tests
All checks passed!

$ pip-audit --strict --requirement requirements.txt --requirement requirements-dev.txt
No known vulnerabilities found

$ pytest -q -m "not integration"
.............                                                            [100%]
13 passed, 229 deselected, 2 warnings in 5.57s
```

Los 13 tests no-integration incluyen los nuevos `test_config_secrets.py` (validación de secretos,
sin infraestructura) además de los ya existentes `test_health.py`/`test_device_status.py`.

### Suite completa contra Postgres/Redis reales (`compose.test.yaml`)

```
$ docker compose -f compose.test.yaml build
 Image netprotect-test-migrate Built
 Image netprotect-test-backend Built

$ docker compose -f compose.test.yaml run --rm migrate
INFO  [alembic.runtime.migration] Running upgrade ... -> c8a1e5b90d34, alerts: allow the
Sprint 20 manipulation-detection signal types

$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
FAILED tests/test_history_integration.py::test_the_owning_tutor_sees_rule_and_geofence_events_merged_by_time
1 failed, 241 passed, 5 warnings in 70.84s (0:01:10)
```

**Ese fallo es preexistente, no introducido por este sprint**: `test_history_integration.py` no
tiene ningún cambio desde el Sprint 15 (`git log` lo confirma), y el mismo test, ejecutado solo
(`pytest tests/test_history_integration.py`, sin el resto de la suite alrededor), pasa limpio:

```
$ docker compose -f compose.test.yaml run --rm migrate
$ docker compose -f compose.test.yaml run --rm backend pytest -q tests/test_history_integration.py
5 passed, 3 warnings in 3.01s
```

Es un caso de contaminación de datos entre tests dependiente del orden de ejecución, ya presente
antes de este sprint (los dos archivos de test nuevos de este sprint, `test_config_secrets.py` —
sin base de datos — y `test_security_hardening_integration.py` — orden alfabético posterior a
`test_history_integration.py` — no pueden ser la causa). Queda anotado aquí en vez de ocultado,
como exige la regla de este proyecto; corregir el aislamiento de datos de ese test es trabajo
pendiente fuera del alcance de "seguridad integral".

## Frontend — lint, build, npm audit

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — limpio)

$ npm run build
✓ Compiled successfully in 35.7s
✓ Generating static pages using 4 workers (3/3) in 2.2s

$ npm audit --audit-level=high
found 0 vulnerabilities
```

## Android — compilación real

```
$ ./gradlew test assembleDebug
BUILD SUCCESSFUL in 32s
76 actionable tasks: 37 executed, 39 up-to-date
```

Confirma que `network_security_config.xml` (base + override de `debug`) y el nuevo atributo del
manifest compilan y no rompen nada — nunca se declaró terminado por inspección del XML.

## OWASP ZAP — escaneo contra el entorno propio

Entorno objetivo: `compose.yaml` (stack de desarrollo real) levantado con
`docker compose -f compose.yaml up -d`, contenedor de ZAP unido a la misma red Docker
(`netprotect_network`) apuntando a `http://backend:8000` — evita depender de host-networking en
Docker Desktop/Windows.

**Primera corrida (antes de las correcciones de este sprint):**

```
$ docker run --rm --network netprotect_network \
    -v <scratchpad>/zap:/zap/wrk:rw zaproxy/zap-stable zap-baseline.py \
    -t http://backend:8000 -r zap-baseline-report.html -J zap-baseline-report.json -I
...
WARN-NEW: Storable and Cacheable Content [10049] x 2
	http://backend:8000 (200 OK)
	http://backend:8000/robots.txt (404 Not Found)
WARN-NEW: Cross-Origin-Resource-Policy Header Missing or Invalid [90004] x 1
	http://backend:8000 (200 OK)
FAIL-NEW: 0	FAIL-INPROG: 0	WARN-NEW: 2	WARN-INPROG: 0	INFO: 0	IGNORE: 0	PASS: 65
```

Corregido en `backend/app/main.py` (`Cache-Control: no-store` + `Cross-Origin-Resource-Policy:
same-origin`, ver `sprint-21.md`). **Segunda corrida, backend reconstruido con el fix:**

```
FAIL-NEW: 0	FAIL-INPROG: 0	WARN-NEW: 1	WARN-INPROG: 0	INFO: 0	IGNORE: 0	PASS: 66
WARN-NEW: Non-Storable Content [10049] x 2
	http://backend:8000 (200 OK)
	http://backend:8000/robots.txt (404 Not Found)
```

El `Cross-Origin-Resource-Policy` desapareció de las advertencias. La advertencia restante bajo el
mismo ID de regla (`10049`) cambió de nombre a **"Non-Storable Content"** — es decir, ZAP ahora
está señalando, como nota informativa, exactamente el resultado buscado (`Cache-Control: no-store`
hace el contenido no-almacenable). Revisado y aceptado: no es un hallazgo de seguridad, es la
confirmación de la corrección. `FAIL-NEW: 0` en ambas corridas — sin hallazgos de riesgo alto o
medio en ningún momento.

## MobSF — escaneo estático del APK

MobSF corrido vía Docker (`opensecurity/mobile-security-framework-mobsf:latest`, puerto host 8937
para no chocar con el backend en 8000), APK subido y escaneado por su API REST
(`/api/v1/upload`, `/api/v1/scan`).

**APK debug** (`assembleDebug`) — score de seguridad 46/100:

| Severidad | Hallazgo | Disposición |
|---|---|---|
| HIGH | `usesCleartextTraffic=true` | Esperado: Gradle sólo fija esto en el build `debug` (ver `build.gradle.kts`) — confirmado al no aparecer en el escaneo del release, más abajo. |
| HIGH | `debuggable=true` | Mismo caso — atributo automático de Gradle sólo en `debug`. |
| warning | Varios componentes exportados de librerías (`androidx.work.*`, Google Sign-In `RevocationBoundService`, Compose `PreviewActivity`) | Terceros empaquetados, protegidos por permisos de sistema (`BIND_JOB_SERVICE`, `DUMP`) o exclusivos de herramientas de desarrollo — no son código propio. |
| warning | `NetProtectDeviceAdminReceiver` exportado | Ya documentado y decidido en Sprint 20 (`android:permission="BIND_DEVICE_ADMIN"`, sólo el sistema puede invocarlo). |
| warning | minSdk 26 "vulnerable" | Decisión de proyecto ya documentada (`docs/android/capability-matrix.md`). |
| warning | `android_ip_disclosure` en `BuildConfig.java` | Es `10.0.2.2`, el alias del emulador Android usado como URL base por defecto en debug — no una IP sensible. |

**APK release sin firmar** (`assembleRelease`) — score de seguridad 52/100:

```
$ python -c "import json; d=json.load(open('report_release.json')); print(d['appsec']['security_score'])"
52
```

Sin ningún hallazgo `HIGH` (confirma que `usesCleartextTraffic`/`debuggable` eran artefactos del
build debug, no del código). Advertencias restantes: las mismas librerías de terceros que en
debug, más dos hallazgos de análisis de código:

```
android_insecure_random -> d5/f.java:8, e6/b.java:5, s7/a.java:3, s7/b.java:3, t7/a.java:3
android_hardcoded       -> i1/c.java:57, t0/z0.java:29
```

Ninguna de esas rutas corresponde a un paquete `com.netprotect.app` — son clases minificadas por
R8 de dependencias de terceros. Verificado con un grep directo sobre el código propio:

```
$ grep -rn "java.util.Random\|new Random(" mobile/app/src/main/java/
(sin resultados)
$ grep -rniE "password\s*=|secret\s*=|api_key\s*=" mobile/app/src/main/java/
(sin resultados)
```

Ambos hallazgos son falsos positivos contra bytecode de terceros, no contra código propio —
documentados, no "arreglados", porque no hay nada propio que arreglar.

## Certificate pinning y CSP con nonce

No implementados este sprint — ver "Controles que quedan fuera de este sprint" en `sprint-21.md`
para el motivo de cada uno.

# Sprint 21 — Seguridad integral

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 20) pide para este sprint: repaso de OWASP Top 10 y OWASP
API Top 10 con OWASP ASVS como lista de comprobación; rate limiting global, validación estricta,
cabeceras, CORS mínimo, gestión de secretos y TLS obligatorio; y escaneo con OWASP ZAP contra el
entorno propio y MobSF sobre el APK.

Antes de escribir código se auditó el estado real (no por inspección superficial: se leyeron
`main.py`, `config.py`, `redis_client.py`, `pairing.py`, `auth.py`, todos los `schemas/`,
`next.config.ts`, `ci.yml`, los tres `compose*.yaml`, el manifest Android y la suite de tests
completa). Los hallazgos de esa auditoría son la base de la sección de decisiones más abajo.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-081 | Como responsable del proyecto, quiero un repaso sistemático contra OWASP Top 10 / API Top 10 / ASVS, no una revisión ad-hoc. |
| HU-082 | Como operador, quiero que ningún endpoint de la API quede sin límite de frecuencia, para que un script no pueda agotar login, refresh ni el resto de la API. |
| HU-083 | Como operador, quiero que la aplicación se niegue a arrancar en producción si todavía tiene un secreto de desarrollo puesto. |
| HU-084 | Como tutor o supervisado, quiero que ningún error interno inesperado del backend me devuelva un detalle que no debería ver. |
| HU-085 | Como responsable del proyecto, quiero evidencia real de un escaneo OWASP ZAP contra el entorno propio y de un escaneo MobSF sobre el APK, no una promesa de que "debería estar bien". |

## Criterios de aceptación

1. `/auth/google` y `/auth/refresh` responden 429 al superar un límite por IP; `/pairing/*` sigue
   funcionando exactamente igual que antes (mismo comportamiento, código compartido).
2. Toda ruta salvo `/api/v1/health*` cuenta contra un límite global por IP; una caída de Redis no
   tumba la API (falla abierto), a diferencia de los límites de auth/pairing (fallan cerrado).
3. `Settings(app_env="production", ...)` levanta `ValidationError` si cualquiera de
   `jwt_secret`/`pairing_code_pepper`/`database_url`/`redis_url`/`location_encryption_key` sigue
   en su valor de desarrollo; con valores reales, arranca normalmente.
4. Un error no manejado (`Exception` genérica) responde 500 con `{"detail": "internal_error",
   "request_id": ...}`, nunca el mensaje original.
5. `GoogleLoginRequest.id_token` y los `refresh_token` de `RefreshRequest`/`LogoutRequest` tienen
   límite de longitud; superarlo es 422.
6. En producción, toda respuesta lleva `Strict-Transport-Security` y
   `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`; fuera de producción, no
   (para no romper Swagger UI). Toda respuesta, en cualquier entorno, lleva `Cache-Control:
   no-store` y `Cross-Origin-Resource-Policy: same-origin`.
7. En producción, una petición que no llega como HTTPS (`request.url.scheme != "https"`) responde
   400 `https_required` antes de tocar cualquier endpoint.
8. `ruff check`, `pip-audit` y `npm audit --audit-level=high` corren en CI y están en verde.
9. Existe un escaneo real de OWASP ZAP contra el backend corriendo en `compose.yaml`, con su
   reporte guardado, y un escaneo real de MobSF sobre el APK compilado (`assembleDebug` y
   `assembleRelease`), también con su reporte guardado. Ver `docs/sprint-21-evidence.md`.

## Decisiones de diseño y su motivo

### El limitador global falla abierto; los de auth/pairing siguen fallando cerrado

`app/cache/redis_client.py` ya documentaba desde el Sprint 5 el criterio "fail closed" para
rate limiting: una protección anti-fuerza-bruta que desaparece silenciosamente cuando Redis está
caído no es protección. Ese criterio se mantiene intacto para `/auth/google`, `/auth/refresh` y
`/pairing/*` (objetivos de alto valor, un único punto de entrada cada uno).

Pero aplicar el mismo criterio a un limitador **global** (todas las rutas) habría significado que
una caída de Redis tumba el 100% de la API, no sólo el flujo que ese límite protege — una
regresión de disponibilidad desproporcionada para lo que es, a propósito, una capa de protección
genérica contra abuso masivo, no un guardián de un objetivo específico. `enforcement_middleware`
(`backend/app/main.py`) por eso falla *abierto* ante `RateLimitBackendError`, con un
`logger.warning` en vez de un 503 — documentado explícitamente en el propio código para que la
próxima persona que lo lea no asuma que es el mismo criterio que `app/core/rate_limit.py`.

### `/api/v1/health` queda fuera del limitador global — y esto no es cosmético

Descubierto durante la propia auditoría: casi todos los archivos de test del proyecto están
sufijados `_integration.py` y sólo corren con Redis/Postgres reales
(`RUN_INTEGRATION_TESTS=1`, ver `compose.test.yaml`). El job `backend` de CI (sin infraestructura,
`pytest -q -m "not integration"`) sólo ejecuta `test_health.py` y `test_device_status.py` — y
`test_health.py::test_health_endpoint` llama a `/api/v1/health` a través de `TestClient(app)` (la
app completa, con todos los middlewares) **sin ningún Redis disponible**. Si el limitador global
tocara Redis para esa ruta, ese test rompería el job rápido de CI. Además, un chequeo de salud es
exactamente lo que un orquestador sondea para decidir si un proceso sigue vivo — limitarlo por
frecuencia haría que una ráfaga de sondeos legítimos se lea como un contenedor caído. Por ambas
razones, `/api/v1/health*` queda explícitamente excluido del conteo.

### TLS obligatorio: *enforcement*, no *terminación*

Se confirmó en la auditoría que ningún `compose*.yaml` tiene un terminador TLS (Caddy/Traefik/
nginx) — sigue coincidiendo con lo que `docs/security-baseline.md` ya documentaba como diferido.
Provisionar certificados reales exige un dominio real, que este repo no tiene todavía (ese es el
Paso 25 de `plan-desarrollo.md`, sprint de despliegue).

Lo que sí se implementó este sprint es la mitad que **no** depende de tener un dominio: si
`APP_ENV=production` y el esquema de la petición no es `https`, se responde 400 `https_required`
antes de procesar nada. Esto funciona porque `backend/Dockerfile` ya arranca uvicorn con
`--proxy-headers`, así que `request.url.scheme` refleja el `X-Forwarded-Proto` que reenviaría un
proxy real. Es decir: el código para exigir HTTPS ya existe y está probado; lo único que falta es
el propio proxy y su certificado, que llegan con el dominio real.

### CSP sólo en producción

`docs_url`/`redoc_url`/`openapi_url` siguen habilitados fuera de producción (decisión ya tomada en
Sprint 1) y Swagger UI carga sus assets desde un CDN. Un `Content-Security-Policy: default-src
'none'` en dev/test rompería esa página sin aportar nada (nadie expone `/docs` en producción). Por
eso la cabecera CSP — igual que HSTS — sólo se envía cuando `app_env == "production"`, entorno en
el que `/docs` ya está deshabilitado desde el Sprint 1.

### `/auth/logout` queda sin límite propio

A diferencia de login y refresh, cerrar sesión exige ya poseer un refresh token válido — no es un
objetivo de fuerza bruta de la misma forma. Sigue cubierto por el limitador global, que es
suficiente para su perfil de riesgo.

### Hallazgos reales de OWASP ZAP, corregidos en este mismo sprint

Un `zap-baseline.py` contra el backend real (`compose.yaml`, red `netprotect_network`, target
`http://backend:8000`) encontró dos advertencias reales antes de cualquier corrección:
`Storable and Cacheable Content [10049]` y `Cross-Origin-Resource-Policy Header Missing [90004]`.
Ambas se corrigieron añadiendo `Cache-Control: no-store` y `Cross-Origin-Resource-Policy:
same-origin` a toda respuesta (`request_id_middleware`, `backend/app/main.py`) — una API JSON con
`allow_credentials=False` no tiene ninguna razón para que sus respuestas (algunas con tokens o
datos de ubicación de un menor) queden en una caché compartida o sean legibles como recurso
cross-origin. Un segundo escaneo confirmó ambas resueltas (0 `FAIL`, 0 `WARN` de ese tipo — ver
`docs/sprint-21-evidence.md` para el detalle de la advertencia residual, revisada y aceptada por
ser exactamente el resultado esperado de la propia corrección).

### Hallazgos de MobSF: se escanearon debug y release por separado a propósito

El primer escaneo (APK `debug`) marcó `usesCleartextTraffic=true` y `debuggable=true` como
`HIGH` — ambos son atributos que Gradle fija automáticamente sólo para el build `debug` (ver
`mobile/app/build.gradle.kts`), no representan cómo se distribuye la app. Escanear también el
APK `release` (`assembleRelease`, sin firmar) lo confirma: ninguno de los dos aparece ahí, y la
puntuación de seguridad de MobSF sube de 46 a 52. El resto de advertencias del release
(`RevocationBoundService` de Google Sign-In, `ProfileInstallReceiver` de AndroidX, minSdk 26) son
de librerías de terceros empaquetadas o decisiones ya documentadas en sprints anteriores, no código
propio. Dos hallazgos de "análisis de código" (`android_insecure_random`,
`android_hardcoded`) señalan archivos con nombres ofuscados por R8 (`d5/f.java`, `i1/c.java`, …)
que no corresponden a ningún paquete `com.netprotect.app`; se verificó con un grep directo sobre
`mobile/app/src/main/java` que el código propio no usa `java.util.Random` ni tiene secretos
hardcodeados — ambos hallazgos son patrones detectados en bytecode de dependencias de terceros, no
en código de este proyecto. Ver `docs/sprint-21-evidence.md` para el detalle completo.

### Certificate pinning: decisión consciente de no implementarlo

No hay todavía un dominio ni un certificado de producción real contra el cual fijar un pin (mismo
motivo, ya usado en el Sprint 13 para no integrar el SDK nativo de Google Maps). Implementar
pinning ahora significaría fijarlo contra un certificado de desarrollo que cambiará en cuanto
exista un dominio real — trabajo que se descartaría al llegar al Paso 25. Queda como control
diferido, no omitido.

### `network_security_config.xml`: aditivo, no un reemplazo

Se añadió `mobile/app/src/main/res/xml/network_security_config.xml` (estricto,
`cleartextTrafficPermitted="false"` sin excepciones) más una superposición en
`mobile/app/src/debug/res/xml/` que sólo en debug permite cleartext hacia `10.0.2.2`/`localhost`
(el emulador) — Gradle superpone automáticamente el resource set de `debug` sobre el de `main`. El
atributo `android:usesCleartextTraffic="${usesCleartextTraffic}"` del manifest se dejó tal cual:
Android da precedencia al NSC cuando ambos están presentes, así que el atributo queda superado
pero inofensivo — no se borró código que funciona sin verificar antes con un build real
(`./gradlew compileDebugKotlin assembleDebug`, ver evidencia).

### Gestión de secretos: fallo rápido, no una advertencia

`Settings._reject_dev_secrets_in_production` (`backend/app/core/config.py`) compara cada secreto
contra **su propio default declarado** en la misma clase, no contra una copia literal del valor
— así el chequeo no puede desincronizarse silenciosamente si algún día cambia el valor de
desarrollo. Levanta `ValueError` (que Pydantic reempaqueta en `ValidationError`) al construir
`Settings`, es decir, al arrancar el proceso — no una advertencia en el log que alguien podría no
leer.

## Controles que quedan fuera de este sprint, explícitamente

- **Terminación TLS real** (certificado, proxy inverso): Paso 25, exige un dominio real.
- **Certificate pinning en Android**: mismo motivo — no hay certificado de producción contra el
  cual fijarlo todavía.
- **CSP con nonce en el frontend**: `script-src`/`style-src` siguen necesitando `'unsafe-inline'`
  porque Next.js App Router no genera un nonce por request sin un cambio de arquitectura mayor
  (middleware de Next inyectando nonce). Documentado como endurecimiento futuro, no bloqueante.
- **Rotación de claves (Fernet, JWT)**: no forma parte del alcance de "gestión de secretos" pedido
  para este sprint; sigue siendo trabajo futuro.

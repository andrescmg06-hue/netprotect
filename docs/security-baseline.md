# Línea base de seguridad

## Matriz de permisos (Sprint 4)

| Acción | TUTOR | SUPERVISADO | Anónimo |
|---|---|---|---|
| `POST /auth/google`, `/auth/refresh` | Sí | Sí | Sí (son el punto de entrada) |
| `GET /auth/me` | Sí (de sí mismo) | Sí (de sí mismo) | No — 401 |
| `GET/POST /users/me/roles` | Sí | Sí | No — 401 |
| Leer/administrar un dispositivo (a partir del Sprint 6) | Sólo si existe un `tutor_devices` activo entre ese tutor y ese dispositivo | No | No |
| Ser el operador supervisado de un dispositivo | N/A | Sólo el dispositivo donde `devices.supervised_user_id` es su propio id | No |
| Reportar telemetría del dispositivo (`/heartbeat`, `/rule-events`, `/location`, y desde el Sprint 20 `/tamper-events`) | No — 404 | Sólo el suyo (`require_supervised_owner_of_device`) | No |
| Leer alertas, incluidas las de manipulación (Sprint 20) | Sólo del dispositivo que supervisa | No | No |

Reglas de diseño:

- **Poseer un rol no concede acceso a ningún recurso por sí solo.** `require_role` sólo protege
  acciones que no apuntan a un recurso concreto. El acceso real a un dispositivo se decide por fila
  (`require_tutor_of_device`), nunca por el rol declarado en el token.
- **Un dispositivo inexistente y uno que no es tuyo responden igual: 404.** Nunca 403, para que
  quien intente enumerar IDs de dispositivo no pueda distinguir "no existe" de "no es tuyo"
  (anti-IDOR/BOLA).
- **Desvincular revoca el acceso de inmediato.** `require_tutor_of_device` sólo considera vínculos
  con `unlinked_at IS NULL`; verificado con una prueba que crea el vínculo, confirma acceso, lo
  desvincula, y confirma que el acceso desaparece.
- **Un usuario puede sostener ambos roles a la vez** (por ejemplo, ser tutor de un dispositivo y
  supervisado en otro). Nada en el esquema ni en `require_role` lo impide.

## Controles aplicados

1. **Secretos excluidos de Git.** `.env` reales y `local.properties` están ignorados.
2. **Separación de ambientes.** Desarrollo, pruebas y producción tienen configuración independiente.
3. **PostgreSQL y Redis sin exposición pública en producción.** Ambos permanecen en la red interna `private`.
4. **Autenticación de Redis en Compose.** Desarrollo, pruebas y producción requieren contraseña.
5. **CORS explícito.** No se habilita `*`.
6. **Trusted Hosts.** FastAPI restringe hosts aceptados.
7. **Errores sanitizados.** Los fallos de PostgreSQL/Redis se traducen a respuestas genéricas de disponibilidad.
8. **Request ID.** Cada respuesta incorpora un identificador para trazabilidad futura.
9. **Cabeceras defensivas iniciales.** `nosniff`, anti-frame y `Referrer-Policy`.
10. **OpenAPI/Swagger deshabilitado en producción.** Se mantiene únicamente fuera de producción.
11. **Contenedores Web/Backend sin root.** Los procesos de aplicación usan usuarios sin privilegios.
12. **Android sin backup.** `android:allowBackup="false"`.
13. **Mínimo privilegio Android.** Sprint 1 sólo solicita `INTERNET`.
14. **Cleartext limitado a debug.** El build Android `release` define `usesCleartextTraffic=false` y debe usar HTTPS.
15. **Redis no es fuente de verdad.** Los datos persistentes de negocio se mantendrán en PostgreSQL.
16. **Google OAuth/OpenID Connect (Sprint 3).** Verificación de ID token contra las claves públicas de
    Google; ninguna contraseña de Google, ni el secreto del cliente OAuth, se almacena jamás.
17. **Sesiones, tokens y revocación (Sprint 3).** Access JWT corto con `jti` único, refresh token
    rotativo de alta entropía, hash SHA-256 en base de datos, revocación inmediata al usarse o cerrar
    sesión.
18. **RBAC por identidad y por recurso (Sprint 4).** Ver la matriz de permisos arriba:
    `require_role` para acciones sin recurso, `require_tutor_of_device` (404 anti-enumeración) para
    todo lo que apunte a un dispositivo concreto.
19. **Cifrado del refresh token en Android (Sprint 3).** AES-256-GCM con una clave del Android
    Keystore, no `EncryptedSharedPreferences` (deprecado).
20. **Canal WebSocket autenticado por primer mensaje, no por query string (Sprint 18).** Un
    token en la URL del *handshake* quedaría expuesto en logs de acceso y proxies intermedios;
    en su lugar, el servidor exige `{"token": ...}` como primer frame, con el mismo criterio
    404-para-ambos-casos que el resto de la API para quien no tiene acceso a ese dispositivo. Ver
    `docs/sprint-18.md`.
21. **Renovación de token en segundo plano sin nuevo almacenamiento de secretos (Sprint 19).**
    `RuleEnforcementService`/`SyncWorker` renuevan su propio *access token* reutilizando el mismo
    `refresh_token` ya cifrado en el Android Keystore (`TokenStore`, Sprint 3) — ningún componente
    nuevo guarda un secreto por su cuenta. Ver `docs/sprint-19.md`.

22. **Detección de manipulación sin ocultamiento ni evasión (Sprint 20).** Cinco señales
    legítimas (permiso de uso revocado, servicio de reglas detenido, reloj desfasado, silencio
    anómalo del *heartbeat*, intento de desinstalación) que **registran y alertan, nunca impiden**.
    El registro como Device Administrator es opcional, con `<uses-policies>` vacío (ninguna
    política aplicada: ni borrado, ni contraseña, ni cámara), revocable por el usuario, y su
    pantalla de solicitud dice explícitamente qué hace y qué no. Ningún dato nuevo se almacena:
    los tres campos del *heartbeat* se evalúan y se descartan; sólo persiste la alerta resultante.
    Ver `docs/sprint-20.md`.

23. **Rate limiting global y en autenticación (Sprint 21).** Toda ruta salvo `/api/v1/health*`
    cuenta contra un límite por IP (`enforcement_middleware`, `backend/app/main.py`), que falla
    *abierto* ante una caída de Redis — a diferencia de los límites de `/auth/google`,
    `/auth/refresh` y `/pairing/*` (helper compartido `app/core/rate_limit.py`), que siguen
    fallando *cerrado* por ser objetivos de alto valor. Ver `docs/sprint-21.md`.
24. **Cabeceras ampliadas (Sprint 21).** `Cache-Control: no-store` y
    `Cross-Origin-Resource-Policy: same-origin` en toda respuesta (hallazgos reales de un escaneo
    OWASP ZAP, corregidos el mismo sprint); `Strict-Transport-Security` y
    `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` sólo en producción (para
    no romper Swagger UI en dev/test). Frontend: mismas cabeceras base desde Sprint 1 más
    `Strict-Transport-Security`/`Content-Security-Policy` en `next.config.ts`.
25. **TLS obligatorio a nivel de aplicación (Sprint 21).** En producción, una petición que no
    llega como HTTPS (`request.url.scheme`, reflejando `X-Forwarded-Proto` vía
    `uvicorn --proxy-headers`) responde 400 antes de procesar nada. La terminación TLS real
    (certificado, proxy inverso) sigue perteneciendo al Paso 25 de `plan-desarrollo.md` — exige un
    dominio real que este repo no tiene todavía.
26. **Validación de secretos al arrancar (Sprint 21).** `Settings` rechaza construirse con
    `app_env=production` si `jwt_secret`/`pairing_code_pepper`/`database_url`/`redis_url`/
    `location_encryption_key` siguen en su valor de desarrollo — falla el arranque, no una
    advertencia en el log.
27. **Validación estricta de autenticación (Sprint 21).** `GoogleLoginRequest.id_token` y los
    `refresh_token` de `RefreshRequest`/`LogoutRequest` ahora tienen límite de longitud — eran el
    único grupo de schemas del backend sin uno.
28. **Errores sin fuga de detalle interno (Sprint 21).** Un `@app.exception_handler(Exception)`
    genérico devuelve `{"detail": "internal_error", "request_id": ...}` y registra el traceback
    real sólo del lado del servidor.
29. **Dependencias auditadas en CI (Sprint 21).** `pip-audit` (backend) y
    `npm audit --audit-level=high` (frontend) corren en cada push/PR.
30. **`network_security_config.xml` en Android (Sprint 21).** Base estricta
    (`cleartextTrafficPermitted="false"`) para todas las variantes, con una excepción sólo en
    `debug` para el emulador (`10.0.2.2`/`localhost`) — control más fino que el atributo
    `usesCleartextTraffic` del manifest, que queda superado pero sin retirar.
31. **Escaneo con OWASP ZAP y MobSF (Sprint 21).** ZAP baseline contra el backend real
    (`compose.yaml`): 0 hallazgos de riesgo alto/medio, dos advertencias reales corregidas en el
    propio código (ítem 24). MobSF contra el APK real (debug y release): sin hallazgos `HIGH` en
    release; las advertencias restantes son de librerías de terceros o decisiones ya documentadas.
    Ver `docs/sprint-21-evidence.md`.

## Controles diferidos conscientemente

Se implementarán en los sprints correspondientes:

- RBAC por dispositivo aplicado a endpoints reales de gestión de dispositivos (Sprint 6; la
  dependencia `require_tutor_of_device` ya existe y está probada, falta el CRUD que la use).
- Auditoría persistente con consulta y exportación para el tutor (Sprint 22; hoy se escribe pero no
  se expone).
- Cifrado de campos sensibles adicionales (ubicación, contenido de eventos).
- FCM: la estructura (registro de token, envío vía API HTTP v1) existe desde el Sprint 18, pero
  sin proyecto Firebase real todavía — pendiente de un humano con cuenta de Google Cloud, ver
  `docs/sprint-18.md`. WebSockets ya no está diferido (Sprint 18).
- Políticas de retención y minimización por tipo de dato.
- Gestor de secretos cloud.
- **Terminación TLS real** (certificado, proxy inverso) — el *enforcement* a nivel de aplicación ya
  existe desde el Sprint 21 (ítem 25); falta el dominio real del Paso 25.
- **Certificate pinning en Android** — no hay todavía un certificado de producción real contra el
  cual fijarlo (Sprint 21, mismo motivo que la Nota del Sprint 13 sobre el SDK de Maps).
- **CSP con nonce en el frontend** — `next.config.ts` usa `'unsafe-inline'` en
  `script-src`/`style-src` porque Next.js App Router no genera un nonce por request sin cambiar de
  arquitectura (Sprint 21).
- Rotación de claves (Fernet, JWT).

Ya no están diferidos, desde el Sprint 21: rate limiting por identidad/IP/operación (ahora también
global y en auth, no sólo en pairing) y SAST/DAST/análisis móvil (ZAP + MobSF corridos contra el
entorno propio, ver `docs/sprint-21-evidence.md`).

Diferirlos no significa omitirlos: el diseño de cada sprint evita decisiones que impidan agregarlos
correctamente más adelante.

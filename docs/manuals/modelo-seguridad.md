# Modelo de seguridad — NetProtect

Audiencia: responsables de seguridad y evaluadores. Da contexto formal (activos, amenazas, mapeo a
marcos reconocidos) a los 36 controles ya listados y evidenciados en `docs/security-baseline.md` —
no los repite íntegros, los organiza. Para el detalle de cada control (motivo, sprint, evidencia),
ver ese archivo directamente.

## 1. Activos a proteger

| Activo | Por qué importa |
|---|---|
| Identidad de los usuarios (cuenta de Google, tokens propios) | Compromiso = suplantación de tutor o supervisado. |
| Ubicación de la persona supervisada | Dato sensible de un menor; cifrado en reposo desde el Sprint 13. |
| Inventario y uso de aplicaciones | Revela hábitos y rutinas de un menor. |
| Reglas y política de un dispositivo | Su manipulación anula el propósito entero del producto. |
| Sesión WebSocket en tiempo real | Canal de control activo; secuestrarlo permitiría espiar o inyectar comandos. |
| Sesión de supervisión remota (WebRTC) | Video en vivo del dispositivo del menor. |
| Secretos de infraestructura (JWT, claves de cifrado, credenciales de BD) | Su compromiso rompe todos los demás controles. |
| Registro de auditoría | Única fuente para reconstruir qué pasó ante un incidente. |

## 2. Actores y modelo de confianza

- **Tutor**: confiable dentro del alcance de sus propios dispositivos vinculados; nunca fuera de
  ese alcance (autorización por fila, no por rol — `docs/security-baseline.md`).
- **Supervisado**: confiable como reportero de su propia telemetría; nunca puede leer ni escribir
  datos de otro dispositivo.
- **Dispositivo supervisado (proceso Android)**: semi-confiable — puede ser manipulado por quien
  tiene el teléfono en mano; de ahí la detección de manipulación (Sprint 20) en vez de asumir que el
  cliente siempre se comporta.
- **Atacante externo**: sin cuenta, intentando enumerar recursos, fuerza bruta o explotar la API
  directamente.
- **Infraestructura (Docker, PostgreSQL, Redis, Caddy, Prometheus)**: confiable dentro de la red
  `private`/`edge`; segmentada para que un compromiso de un contenedor no exponga directamente los
  demás.

## 3. Amenazas por categoría STRIDE y su control

| STRIDE | Amenaza concreta | Control aplicado | Sprint |
|---|---|---|---|
| **S**poofing | Suplantar a otro usuario | Verificación de ID token de Google contra JWKS reales; nunca contraseña propia almacenada | 3 |
| **S**poofing | Reutilizar un token robado indefinidamente | Access JWT corto con `jti` único, refresh rotativo de un solo uso, revocación inmediata | 3 |
| **T**ampering | Modificar un token para escalar privilegios | Firma HS256 verificada en cada request; alterar un carácter del medio del token (no el último, por el relleno de Base64) lo invalida | 3, 21 |
| **T**ampering | Manipular el dispositivo supervisado (desinstalar, revocar permisos, cambiar hora) | 5 señales que registran y alertan, nunca ocultan ni evaden — sin rootkits, sin device owner | 20 |
| **R**epudiation | Negar haber hecho una acción sensible | `audit_logs` inmutable, actor + acción + recurso + fecha, consultable y exportable por su propio dueño | 2, 3, 22 |
| **I**nformation disclosure | Enumerar IDs de dispositivo ajenos | 404 uniforme para "no existe" y "no es tuyo" en toda ruta con recurso (anti-IDOR/BOLA) | 4 |
| **I**nformation disclosure | Leer ubicación cifrada desde un volcado de base de datos | Cifrado Fernet de latitud/longitud, clave fuera del repositorio | 13 |
| **I**nformation disclosure | Un error inesperado filtra un traceback o mensaje de driver | `exception_handler` genérico → `{"detail": "internal_error", "request_id": ...}`, traceback sólo en log del servidor | 21 |
| **I**nformation disclosure | Un token en la URL de un WebSocket queda en logs de acceso/proxies | Autenticación por primer frame (`{"token": ...}`), nunca por query string | 18 |
| **D**enial of service | Fuerza bruta sobre el código de vinculación de 6 dígitos | Hash del código (no el código), comparación en tiempo constante, rate limiting con fallo cerrado | 5, 21 |
| **D**enial of service | Saturar la API entera | Rate limiting global por IP, con fallo abierto (disponibilidad de la API entera pesa más que esta capa genérica) | 21 |
| **D**enial of service | Explotar un secreto de desarrollo dejado en producción | `Settings` se niega a arrancar en producción con un secreto de desarrollo puesto | 21 |
| **E**levation of privilege | Que tener un rol declarado en el token conceda acceso a un recurso concreto | `require_role` sólo protege acciones sin recurso; todo acceso a un dispositivo verifica la fila real (`require_tutor_of_device`) | 4 |
| **E**levation of privilege | Un tutor de otro dispositivo lea/escriba reglas de este | Autorización por fila, nunca por posesión de rol | 4, 6 |
| **E**levation of privilege | Un segundo tutor secuestra una sesión de supervisión remota ya en curso | Sesión anclada a la conexión del tutor que la pidió; una segunda solicitud recibe `screen_share_busy`, no roba la sesión | 23, 24 |

## 4. Mapeo a OWASP Top 10 (aplicación web) y OWASP API Security Top 10

| OWASP | Control relevante | Evidencia |
|---|---|---|
| A01 Broken Access Control / API1 BOLA | Autorización por fila + 404 uniforme | `docs/security-baseline.md`, matriz de permisos |
| A02 Cryptographic Failures | Fernet para ubicación, AES-256-GCM (Android Keystore) para refresh token, hash SHA-256 de refresh token en BD | Sprints 3, 13 |
| A03 Injection | SQLAlchemy 2.0 con `Mapped`/consultas parametrizadas; sin SQL crudo interpolado | Todo el backend |
| A04 Insecure Design | Autorización decidida siempre en backend, nunca confiando en el cliente Android/web | `docs/architecture.md`, principios |
| A05 Security Misconfiguration | CORS explícito, Trusted Hosts, Swagger deshabilitado en producción, cabeceras defensivas, escaneo ZAP real | Sprints 1, 21 |
| A06 Vulnerable and Outdated Components | `pip-audit`/`npm audit` en cada push/PR | Sprint 21 |
| A07 Identification and Authentication Failures | OAuth/OIDC real con Google, sin contraseñas propias, sesión revocable | Sprint 3 |
| A08 Software and Data Integrity Failures | Migraciones versionadas con Alembic, CI que exige pruebas verdes antes de publicar imagen | Sprints 2, 26 |
| A09 Security Logging and Monitoring Failures | `audit_logs`, Prometheus/Grafana/Loki, alertas de manipulación | Sprints 2, 20, 26 |
| A10 Server-Side Request Forgery | No aplica directamente — el backend no realiza peticiones salientes a URLs suministradas por el usuario | — |
| API4 Unrestricted Resource Consumption | Rate limiting global y por operación sensible | Sprint 21 |
| API8 Security Misconfiguration | TLS obligatorio a nivel de aplicación y ahora real con Caddy | Sprints 21, 26 |

## 5. Controles diferidos conscientemente y por qué no son una omisión

`docs/security-baseline.md`, sección "Controles diferidos conscientemente", lista: cifrado de
campos sensibles adicionales, políticas de retención por tipo de dato más finas, certificate
pinning en Android, CSP con nonce en el frontend, rotación automática de claves, y el gestor de
secretos/dominio de producción concretos. Cada uno tiene una razón técnica documentada de por qué
no se implementó todavía (normalmente: depende de un recurso que exige una cuenta o dominio real
que este repositorio no tiene) — no una omisión sin explicación. Ver
`docs/manuals/analisis-riesgos.md` para el detalle de riesgo asociado a cada uno.

## 6. Verificación real, no sólo diseño

Este modelo no es sólo teórico: `docs/sprint-21-evidence.md` documenta una corrida real de OWASP
ZAP (0 hallazgos de riesgo alto/medio en el cierre, dos advertencias reales encontradas y
corregidas) y de MobSF contra el APK real. `docs/security-baseline.md` mismo nació, en parte, de
hallazgos reales de esos escaneos, no de una lista redactada de antemano.

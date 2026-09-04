# Línea base de seguridad

## Matriz de permisos (Sprint 4)

| Acción | TUTOR | SUPERVISADO | Anónimo |
|---|---|---|---|
| `POST /auth/google`, `/auth/refresh` | Sí | Sí | Sí (son el punto de entrada) |
| `GET /auth/me` | Sí (de sí mismo) | Sí (de sí mismo) | No — 401 |
| `GET/POST /users/me/roles` | Sí | Sí | No — 401 |
| Leer/administrar un dispositivo (a partir del Sprint 6) | Sólo si existe un `tutor_devices` activo entre ese tutor y ese dispositivo | No | No |
| Ser el operador supervisado de un dispositivo | N/A | Sólo el dispositivo donde `devices.supervised_user_id` es su propio id | No |

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

## Controles diferidos conscientemente

Se implementarán en los sprints correspondientes:

- RBAC por dispositivo aplicado a endpoints reales de gestión de dispositivos (Sprint 6; la
  dependencia `require_tutor_of_device` ya existe y está probada, falta el CRUD que la use).
- Rate limiting por identidad/IP/operación, especialmente en la vinculación por código (Sprint 5).
- Auditoría persistente con consulta y exportación para el tutor (Sprint 22; hoy se escribe pero no
  se expone).
- Cifrado de campos sensibles adicionales (ubicación, contenido de eventos).
- FCM/WebSockets.
- Protección criptográfica y anti-fuerza-bruta del código de vinculación.
- Políticas de retención y minimización por tipo de dato.
- SAST/DAST y análisis móvil completos.
- Gestor de secretos cloud.
- TLS/HTTPS de producción en edge.

Diferirlos no significa omitirlos: el diseño de cada sprint evita decisiones que impidan agregarlos
correctamente más adelante.

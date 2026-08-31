# Línea base de seguridad — Sprint 1

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

## Controles diferidos conscientemente

Se implementarán en los sprints correspondientes:

- Google OAuth/OpenID Connect.
- Sesiones, access/refresh tokens, expiración y revocación.
- RBAC por identidad, tutor y dispositivo.
- Rate limiting por identidad/IP/operación.
- Auditoría persistente.
- Cifrado de campos sensibles.
- FCM/WebSockets.
- Protección criptográfica y anti-fuerza-bruta del código de vinculación.
- Políticas de retención y minimización por tipo de dato.
- SAST/DAST y análisis móvil completos.
- Gestor de secretos cloud.
- TLS/HTTPS de producción en edge.

Diferirlos no significa omitirlos: el diseño del Sprint 1 evita decisiones que impidan agregarlos correctamente.

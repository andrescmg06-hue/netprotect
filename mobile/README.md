# NetProtect Android — Sprint 1

Aplicación Android única en Kotlin + Jetpack Compose. En sprints posteriores soportará Tutor y Supervisado sin crear dos APK independientes.

## Funcionalidad actual

- Solicita únicamente el permiso normal `INTERNET`.
- Consume `GET /api/v1/health/ready`.
- Muestra el estado de Backend, PostgreSQL y Redis.
- Permite volver a ejecutar la comprobación.

## Desarrollo local

En emulador Android Studio, la URL por defecto es `http://10.0.2.2:8000`.

Si necesita otra URL, agregue a `local.properties`:

```properties
NETPROTECT_API_BASE_URL=http://192.168.1.50:8000
```

No versionar `local.properties`.

## Seguridad

El build `debug` permite cleartext para conexión al host local. `release` define `usesCleartextTraffic=false`; la URL release debe entregarse mediante `NETPROTECT_RELEASE_API_BASE_URL` y usar HTTPS.

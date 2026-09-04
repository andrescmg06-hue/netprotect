# Evidencia de verificación — Sprint 3

Fecha: 03-04/09/2026. Equipo: `andre`, Windows 11 Pro.

## Google Cloud

Proyecto `netprotect-507600`. Pantalla de consentimiento OAuth en modo Externo/Prueba, con el correo
del equipo agregado como usuario de prueba. Dos credenciales OAuth creadas:

- `NetProtect Web` (tipo Aplicación web), origen autorizado `http://localhost:3000`.
- `NetProtect Android` (tipo Android), paquete `com.netprotect.app`, huella SHA-1 del keystore de
  depuración de este equipo.

Confirmado por inspección del JSON descargado de la credencial Android: no contiene `client_secret`
(los clientes tipo Android nunca lo generan). El flujo implementado (verificación de ID token) tampoco
usa el secreto de la credencial Web en ningún punto.

## Backend

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q
→ 14 passed, 2 warnings in 33.86s

ruff check app tests alembic
→ All checks passed!
```

Las 5 pruebas nuevas (`tests/test_auth_integration.py`), contra PostgreSQL real, con la verificación
de Google simulada (`unittest.mock.patch` sobre `verify_google_id_token`, ya que un ID token real
sólo puede emitirlo Google tras una interacción humana):

- `test_google_login_creates_user_and_issues_tokens` — crea el usuario, devuelve el par de tokens,
  y confirma un registro en `audit_logs` con `action=LOGIN`.
- `test_me_requires_a_valid_access_token` — 401 sin cabecera `Authorization`, 200 con el access
  token real emitido en el login.
- `test_tampered_and_expired_access_tokens_are_rejected` — 401 con un token alterado un carácter y
  con un JWT válido pero con `exp` en el pasado.
- `test_refresh_rotates_the_token_and_invalidates_the_old_one` — el refresh devuelve un par nuevo,
  reusar el token viejo da 401, y el nuevo sigue funcionando.
- `test_logout_revokes_the_refresh_token` — tras logout, ese refresh token ya no sirve para renovar.

### Verificación manual contra el backend real en Docker

```bash
curl -o /dev/null -w "%{http_code}" http://localhost:8000/api/v1/auth/me
→ 401

curl -H "Authorization: Bearer basura.invalida.aqui" http://localhost:8000/api/v1/auth/me
→ 401

curl -X POST -d '{"id_token":"no-es-un-token-real"}' http://localhost:8000/api/v1/auth/google
→ 401 {"detail":"invalid_google_token"}
```

### Bug real encontrado y corregido: colisión de access tokens

La primera corrida de `compose.test.yaml` con las pruebas de auth falló en
`test_refresh_rotates_the_token_and_invalidates_the_old_one`: el access token "nuevo" resultó ser
idéntico, carácter por carácter, al anterior. Causa: el JWT no llevaba ningún identificador único —
con el mismo `sub`, `type`, `iat` y `exp` (todos con precisión de segundo), dos tokens emitidos para
el mismo usuario dentro del mismo segundo son el mismo token firmado. En este equipo, con más
latencia entre llamadas, nunca se veía; en un contenedor de CI sin esa latencia, login y refresh caen
rutinariamente en el mismo segundo. Se agregó un `jti` aleatorio (`uuid4().hex`) a cada token; se
verificó con dos corridas limpias consecutivas de la suite completa en Docker tras el fix.

## Web

```bash
cd frontend
npm run lint   → sin errores
npm run build  → compilación exitosa
```

`curl http://localhost:3000` → HTTP 200. Confirmado que la página incluye la nueva sección de sesión.
No se completó un login real en navegador: exige elegir una cuenta de Google en el selector de
cuentas, algo que sólo una persona puede hacer.

## Android

```bash
cd mobile
./gradlew.bat test assembleDebug    → BUILD SUCCESSFUL
./gradlew.bat assembleRelease       → BUILD SUCCESSFUL (incluye R8/minify sin reglas proguard extra)
```

Manifiesto de `release` confirmado con `usesCleartextTraffic="false"` (sigue igual que en el Sprint 1).

No se completó un login real en el emulador en esta sesión: el emulador estaba cerrado y, aunque se
reabra, seleccionar una cuenta de Google en el selector de Credential Manager exige interacción
humana con la UI del sistema, que una sesión de agente no puede ejecutar.

## Pendiente — requiere interacción humana

1. Abrir `http://localhost:3000`, hacer clic en "Iniciar sesión con Google", elegir la cuenta de
   prueba agregada en Google Cloud, y confirmar que la tarjeta de sesión muestra el nombre y correo.
2. Abrir el emulador, ejecutar la app, tocar "Iniciar sesión con Google", elegir la cuenta, y
   confirmar lo mismo en la pantalla de la app.
3. Repetir el paso 1 o 2 y luego cerrar sesión, confirmando que vuelve al estado "sin sesión".

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con salida real, incluido el bug del `jti`
encontrado y corregido en el camino. El clic real en el selector de cuentas de Google —lo único que
falta para cerrar el sprint por completo— no se marca como probado porque no se ejecutó.

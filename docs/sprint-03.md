# Sprint 3 — Autenticación con Google

## Objetivo

Login real con Google, sin contraseñas propias. El backend recibe el *ID token* que emite Google,
lo valida contra las claves públicas de Google (nunca contra una contraseña), crea o recupera al
usuario y emite sus propios tokens: un access token corto (JWT, 15 min) y un refresh token rotativo
de alta entropía (30 días), cuyo hash vive en la tabla `sessions` del Sprint 2.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-009 | Como tutor, quiero iniciar sesión con Google desde la web para acceder de forma segura. |
| HU-010 | Como supervisado, quiero iniciar sesión con Google desde la app para más adelante vincular mi dispositivo. |
| HU-011 | Como usuario, quiero que mi sesión se pueda renovar y cerrar de forma segura. |

## Criterios de aceptación

1. Un endpoint protegido (`GET /auth/me`) devuelve 401 sin token, 200 con token válido, 401 con
   token manipulado o expirado.
2. `POST /auth/google` valida el ID token contra Google (firma, emisor, audiencia, `email_verified`)
   y nunca almacena una contraseña.
3. `POST /auth/refresh` rota el refresh token: el anterior queda inválido de inmediato.
4. `POST /auth/logout` revoca el refresh token indicado.
5. Login, refresh y logout quedan en `audit_logs`.
6. La app Android inicia sesión con Credential Manager / Google ID y guarda el refresh token cifrado.
7. La web inicia sesión con Google Identity Services.

## Tareas técnicas y estado

| Tarea | Estado | Evidencia |
|---|---|---|
| Verificación de ID token de Google (`google-auth`) | Implementada | `backend/app/services/google_auth.py` |
| Emisión de tokens propios (JWT + refresh rotativo) | Implementada | `backend/app/core/security.py` |
| Endpoints `/auth/google`, `/refresh`, `/logout`, `/me` | Implementados | `backend/app/api/v1/endpoints/auth.py` |
| Dependencia `get_current_user` | Implementada | `backend/app/api/deps.py` |
| Auditoría de login/refresh/logout | Implementada | `backend/app/services/audit.py` |
| Pruebas de integración (401/200/refresh/logout) | Implementadas, 5 pruebas | `backend/tests/test_auth_integration.py` |
| Login con Google Identity Services | Implementado | `frontend/src/components/GoogleSignInButton.tsx` |
| Contexto de sesión en la web | Implementado | `frontend/src/contexts/AuthContext.tsx` |
| Login con Credential Manager en Android | Implementado | `mobile/app/.../core/auth/AuthRepository.kt` |
| Cifrado del refresh token en Android | Implementado | `mobile/app/.../core/auth/TokenStore.kt` |
| Credenciales OAuth (Web + Android) en Google Cloud | Configuradas | `docs/sprint-03-evidence.md` |

## Decisiones de diseño relevantes

- **Verificación por ID token, no por código de autorización**: tanto la web (Google Identity
  Services) como Android (Credential Manager) obtienen un ID token directamente y se lo entregan al
  backend para verificar. Esto significa que **no existe ningún flujo que necesite el secreto del
  cliente OAuth de Google** — sólo el ID de cliente Web, usado como audiencia esperada. El secreto
  nunca se generó ni se guardó en ningún lado.
- **Una sola audiencia para los dos clientes**: Android usa Credential Manager con
  `setServerClientId(WEB_CLIENT_ID)`, así que el ID token que emite para el celular tiene la misma
  audiencia que el de la web. El backend sólo necesita validar contra `GOOGLE_WEB_CLIENT_ID`, sin
  importar la plataforma de origen.
- **`jti` en el access token**: sin un identificador único, dos tokens emitidos para el mismo usuario
  dentro del mismo segundo son bit a bit idénticos (mismo `sub`, `type`, `iat`, `exp`). Esto pasó
  inadvertido en local (donde hay suficiente latencia entre llamadas) y se detectó en CI, con un
  runner rápido donde login y refresh caían en el mismo segundo. Se agregó un `jti` aleatorio a cada
  token.
- **Rotación de refresh token sin cascada de revocación**: cada `/auth/refresh` revoca el token usado
  y emite uno nuevo. Reutilizar uno ya revocado da 401. No se implementó la revocación en cascada de
  todas las sesiones del usuario ante un intento de reuso (señal típica de robo de token); queda
  como mejora candidata para el Sprint 21 (seguridad integral).
- **Sesión web: en memoria + `sessionStorage`, no cookie `HttpOnly`**: la arquitectura original
  contemplaba una cookie `HttpOnly` para el refresh token, que exige un proxy en el propio Next.js
  (todas las llamadas protegidas pasarían por rutas de servidor de Next.js que adjuntan el token,
  porque JavaScript no puede leer una cookie `HttpOnly`). Se decidió no construir ese proxy en este
  sprint por alcance: hoy no hay ninguna otra pantalla protegida que consumir desde la web más allá
  del login mismo. El access token vive sólo en memoria (nunca en `localStorage`) y el refresh token
  en `sessionStorage` (se borra al cerrar la pestaña). Es una postura intermedia, no la versión
  endurecida final; anotado aquí para no perder de vista la diferencia con lo planeado.
- **`EncryptedSharedPreferences` no se usó**: está deprecado desde `androidx.security:security-crypto`
  1.1.0. `TokenStore` cifra con AES-256-GCM usando una clave del Android Keystore directamente
  (`android.security.keystore`), sin añadir DataStore + Tink, que es la migración completa que Google
  documenta pero resulta desproporcionada para guardar un solo string.

## Ejecución

```bash
docker compose up --build -d
```

Backend: `GET /api/v1/auth/me` sin token → 401. Web: `http://localhost:3000`, botón "Iniciar sesión
con Google". Android: abrir `mobile/` en Android Studio, ejecutar en un emulador con Google APIs.

## Verificación

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q tests/test_auth_integration.py
ruff check app tests alembic
```

```bash
cd frontend
npm run lint && npm run build
```

```bash
cd mobile
./gradlew.bat test assembleDebug assembleRelease
```

## Seguridad del sprint

- Ninguna contraseña de Google se almacena, ni se ve, en ningún punto del sistema.
- El secreto del cliente OAuth de Google nunca se generó (el flujo de ID token no lo necesita).
- `JWT_SECRET` y `GOOGLE_WEB_CLIENT_ID` viven en `.env`, nunca versionados; `compose.yaml` y
  `compose.prod.yaml` los exigen con `${VAR:?...}`.
- Los refresh tokens se guardan como hash SHA-256 en la base de datos, nunca en texto plano.
- El refresh token en Android se cifra con una clave del Android Keystore antes de escribirse a disco.
- Login, refresh y logout quedan en `audit_logs` con IP de origen.

## Fuera de alcance

RBAC funcional (Sprint 4: elegir Tutor no concede privilegios automáticamente, eso lo decide el
backend), vinculación por código (Sprint 5), y el proxy de cookie `HttpOnly` para la web mencionado
arriba, que puede abordarse cuando haya más superficie protegida que justifique construirlo.

## Definition of Done del Sprint 3

Todos los criterios de aceptación tienen evidencia ejecutada realmente contra PostgreSQL real (no
mockeada, salvo la respuesta de Google, que si se puede simular de forma legítima porque requiere
una cuenta real). Backend, web y Android compilan y pasan sus pruebas. El flujo de clic real —
seleccionar una cuenta de Google en un navegador y en el emulador — no se pudo ejecutar en esta sesión
porque exige interacción humana con el selector de cuentas de Google; queda pendiente de confirmación
visual por el equipo, ver `docs/sprint-03-evidence.md`.

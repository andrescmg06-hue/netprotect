# Plan de desarrollo NetProtect — paso a paso

Fecha: 03/09/2026.

Este documento es la guía operativa del proyecto. Define en qué orden se construye NetProtect, qué hay que instalar o crear en el computador antes de cada paso, qué se entrega y cómo se verifica. Complementa a `docs/planning/roadmap.md` (qué sprint) explicando el cómo y el con qué.

Regla de trabajo: **ningún paso se marca como terminado sin evidencia ejecutada realmente** (comando ejecutado, captura o salida de prueba). Lo que no se ejecutó se documenta como pendiente.

---

## 1. Estado actual verificado

Revisión hecha sobre el árbol de trabajo el 03/09/2026.

### Lo que ya existe

| Área | Estado | Evidencia |
|---|---|---|
| Monorepo y estructura | Completo | `backend/ frontend/ mobile/ database/ docs/ infra/ tests/ scripts/ .github/` |
| Documentación previa (arquitectura, 5 diagramas, matriz Android, backlog, historias, DoD, roadmap) | Completo | `docs/` |
| Backend FastAPI con health/db/redis/ready | Implementado | `backend/app/` |
| Middleware de seguridad (TrustedHost, CORS, X-Request-ID, cabeceras) | Implementado | `backend/app/main.py` |
| Web Next.js que consume `/health/ready` | Implementado | `frontend/src/app/page.tsx` |
| App Android Kotlin/Compose | Implementada, **regresionada** | ver bloqueos |
| Docker Compose dev/test/prod | Implementado | `compose*.yaml` |
| Ambientes y variables separados | Implementado | `.env.*.example`, `docs/environments.md` |
| CI GitHub Actions (backend, frontend, android, integración) | Implementado | `.github/workflows/ci.yml` |
| Pruebas backend unitarias | 4 pasan | `docs/sprint-01-evidence.md` |

### Bloqueos y deuda detectados

| # | Problema | Impacto | Se resuelve en |
|---|---|---|---|
| B1 | `mobile/local.properties` apunta a `C:\Users\crist\...\Android\Sdk`, ruta que no existe en este equipo (el usuario es `andre`) | Gradle no compila | Paso 0 |
| B2 | Docker Desktop está instalado pero el demonio no está corriendo | No hay backend, ni BD, ni Redis, ni integración | Paso 0 |
| B3 | `MainActivity.kt` fue reemplazado por un `Text("NETPROTECT FUNCIONA")` y ya no usa `SprintOneScreen` | Rompe el criterio de aceptación 8 del Sprint 1 | Paso 0 |
| B4 | El SDK tiene `platforms/android-37.0` y `build-tools 36.0.0`, pero no la plataforma que pide `compileSdk = 36`; no hay `cmdline-tools`, ni `system-images`, ni AVD | No se puede compilar ni ejecutar en emulador | Paso 0 |
| B5 | Gradle wrapper 9.3.0 recién añadido, sin verificar contra AGP 8.13.2 | El build puede fallar por incompatibilidad | Paso 0 |
| B6 | `compose.yaml` modificado con contraseñas por defecto reales embebidas (`NetProtectDev2026`) y sin publicar los puertos `5432/6379` | Riesgo de secreto versionado; no se puede inspeccionar la BD con un cliente externo | Paso 0 |
| B7 | Cambios sin commitear en 6 archivos y wrapper sin versionar | Trabajo no versionado | Paso 0 |
| B8 | No hay modelo de datos real, ni migraciones, ni ORM, ni autenticación | Es justamente el trabajo de los sprints 2 y 3 | Pasos 1 y 2 |

### Herramientas presentes en el equipo

| Herramienta | Versión detectada | Suficiente |
|---|---|---|
| Git | 2.55.0 | Sí |
| Docker Desktop | 29.5.2 + Compose v5.1.4 | Sí (falta arrancarlo) |
| Python | 3.12.10 | Sí (CI usa 3.13; se documenta la diferencia) |
| Node.js / npm | 24.20.0 / 11.19.0 | Sí |
| Java | 21.0.2 LTS | Sí |
| Android Studio | Instalado | Sí |
| Gradle CLI | No instalado | No hace falta: se usa el wrapper |
| Android SDK | build-tools 36.0.0, platform android-37.0 | Incompleto (ver B4) |

---

## 2. Descargas y cuentas, y cuándo hacen falta

No hay que instalar todo hoy. Esta tabla dice **cuándo** se necesita cada cosa.

| Cuándo | Qué | Dónde |
|---|---|---|
| Paso 0 | Android SDK Platform 36 + Command-line Tools + system image (API 36, Google APIs, x86_64) + un AVD | Android Studio → SDK Manager / Device Manager |
| Paso 0 | Arrancar Docker Desktop (WSL2 habilitado) | Ya instalado |
| Paso 1 | `alembic` (pip, entra en `requirements.txt`) | pip |
| Paso 1 (opcional) | DBeaver o pgAdmin para inspeccionar PostgreSQL | dbeaver.io |
| Paso 2 | Proyecto en Google Cloud Console, pantalla de consentimiento OAuth y Client ID Web + Android (con SHA-1 del keystore de debug) | console.cloud.google.com |
| Paso 2 | `keytool` para obtener el SHA-1 (viene con el JDK ya instalado) | Ya instalado |
| Pasos 3-5 | Postman o Bruno para la colección de API | postman.com |
| Paso 12 | Clave de Google Maps Platform (Maps SDK for Android + Maps JavaScript API) con restricciones | console.cloud.google.com |
| Paso 17 | Proyecto Firebase + `google-services.json` para FCM | console.firebase.google.com |
| Paso 20 | OWASP ZAP; MobSF vía Docker; Burp Suite Community sólo contra nuestro propio entorno | zaproxy.org / hub.docker.com |
| Paso 24 | Playwright (E2E web) y k6 (rendimiento) | npm / k6.io |
| Paso 25 | Cuenta cloud (VPS o proveedor gestionado), dominio y certificados vía Let's Encrypt (Caddy o Traefik) | proveedor elegido |
| Todo el proyecto | Repositorio remoto en GitHub con Actions habilitado | github.com |

Nada de esto exige licencias de pago salvo el dominio y el hosting del Paso 25.

---

## 3. Fases

| Fase | Pasos | Sprints | Resultado |
|---|---|---|---|
| A. Cimientos verificados | 0 | cierre del 1 | Entorno reproducible y demostrable |
| B. Núcleo de la plataforma | 1-5 | 2-6 | Identidad, roles, vinculación y dispositivos reales |
| C. Motor de reglas | 6-11 | 7-12 | Control de apps, web, listas, categorías, tiempo y modo escolar |
| D. Contexto y visibilidad | 12-16 | 13-17 | Ubicación, geocercas, historial, estadísticas y alertas |
| E. Sincronización y resiliencia | 17-19 | 18-20 | Tiempo real, offline y detección de manipulación |
| F. Endurecimiento | 20-22 | 21-23 | Seguridad integral, auditoría y supervisión viable |
| G. Producto | 23-26 | 24-27 | Panel web completo, pruebas, despliegue y documentación |

---

## FASE A — Cimientos verificados

### Paso 0 — Cerrar el Sprint 1 con evidencia real

Objetivo: que los tres carriles (backend, web, Android) arranquen en **este** computador y quede registrada la evidencia que hoy falta en `docs/sprint-01-evidence.md`.

Instalar/descargar:

1. Android Studio → SDK Manager → SDK Platforms: instalar **Android API 36**.
2. SDK Manager → SDK Tools: instalar **Android SDK Command-line Tools (latest)** y confirmar **Platform-Tools** y **Build-Tools 36**.
3. Device Manager: crear un AVD (Pixel, imagen **API 36, Google APIs, x86_64**); esto descarga la system image.
4. Arrancar Docker Desktop y esperar a que el demonio responda.

Trabajo técnico:

- Corregir `mobile/local.properties` → `sdk.dir=C\:\\Users\\andre\\AppData\\Local\\Android\\Sdk` (B1).
- Restaurar `MainActivity.kt` para que vuelva a renderizar `SprintOneScreen` (B3).
- Ejecutar `.\gradlew.bat :app:assembleDebug` y resolver la compatibilidad AGP 8.13.2 / Gradle 9.3.0 / Kotlin 2.3.21 / Compose BOM que haga falta (B5); si el wrapper 9.3 no es compatible, se fija la versión que sí lo sea.
- Versionar el wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`): es lo que hace reproducible el build (B7).
- Sacar las contraseñas por defecto de `compose.yaml`, dejando la variable obligatoria o un placeholder inequívoco, y republicar `127.0.0.1:5432` y `127.0.0.1:6379` sólo en el compose de desarrollo (B6).
- Rotar las contraseñas del `.env` local, ya que las anteriores quedaron escritas en el árbol de trabajo.

Verificación (esto es la evidencia del sprint):

```powershell
docker compose up --build -d
curl http://localhost:8000/api/v1/health
curl http://localhost:8000/api/v1/health/db
curl http://localhost:8000/api/v1/health/redis
curl http://localhost:8000/api/v1/health/ready
```

```powershell
cd frontend; npm install; npm run lint; npm run build
cd ..\mobile; .\gradlew.bat test assembleDebug
```

- Web en `http://localhost:3000` mostrando el estado conectado.
- App en el emulador mostrando `Backend`, `PostgreSQL` y `Redis` en `CONNECTED`.
- Pipeline verde en GitHub Actions.
- Actualizar `docs/sprint-01-evidence.md` con las salidas reales y hacer commit.

---

## FASE B — Núcleo de la plataforma

### Paso 1 — Sprint 2: Base de datos real

Objetivo: pasar del esquema conceptual a un modelo físico versionado y migrable.

Instalar: `alembic` (se agrega a `backend/requirements.txt`); opcionalmente DBeaver.

Trabajo:

- Modelos SQLAlchemy 2.0 (`Mapped`/`mapped_column`) para el núcleo: `users`, `roles`, `user_roles`, `devices`, `tutor_devices`, `pairing_codes`, `sessions`, `audit_logs`, `device_status`. El resto de las 25 tablas se introduce en el sprint que las usa, no antes.
- Claves primarias UUID, `created_at`/`updated_at` con zona horaria, borrado lógico donde aplique.
- Integridad referencial explícita, `ON DELETE` razonado, índices para las consultas previstas (por tutor, por dispositivo, por fecha) y restricciones únicas (un código de vinculación activo por sesión).
- Alembic configurado con migración inicial; el backend **no** crea tablas con `create_all`.
- Comando `make migrate` o contenedor de migración para aplicar antes de arrancar la API.
- Semillas mínimas: roles `TUTOR` y `SUPERVISADO`.
- ER real generado a partir del modelo, en `docs/diagrams/`.

Verificación: `alembic upgrade head` y `alembic downgrade -1` funcionan sobre el PostgreSQL del compose; pruebas de integración que insertan y consultan; `\dt` muestra las tablas esperadas.

### Paso 2 — Sprint 3: Autenticación con Google

Objetivo: identidad real, sin contraseñas propias.

Crear antes de programar:

- Proyecto en Google Cloud Console.
- Pantalla de consentimiento OAuth (externa, en modo prueba, con el correo del equipo como tester).
- **Client ID tipo Web**: lo usan el backend para validar y Next.js para el login web.
- **Client ID tipo Android** con el package `com.netprotect.app` y el SHA-1 del keystore de debug:

```powershell
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Trabajo:

- Backend: endpoint que recibe el **ID token** de Google y valida firma contra los JWKS de Google, `aud`, `iss`, expiración y `email_verified`; crea o recupera el usuario; emite **tokens propios** (access corto + refresh rotativo, persistido y revocable). Nunca se almacena contraseña de Google.
- Dependencia `get_current_user` que protege endpoints; `/auth/refresh`, `/auth/logout` y revocación de sesión.
- Web: login con Google Identity Services y sesión en cookie `HttpOnly`/`SameSite`.
- Android: Credential Manager con Google ID; el token propio se guarda cifrado (`EncryptedSharedPreferences` respaldado por Android Keystore), nunca en claro.
- Auditoría de `login`, `logout` y `refresh`.

Verificación: login real desde web y desde el emulador; un endpoint protegido devuelve 401 sin token, 200 con token válido y 401 con token manipulado o expirado; el refresh rotativo invalida el anterior.

### Paso 3 — Sprint 4: Roles y RBAC

Objetivo: que "elegir Tutor" no conceda privilegios; el backend decide.

Trabajo:

- Endpoint de selección de rol que **solicita** un rol y el backend lo concede según reglas (por ejemplo: un usuario sólo es SUPERVISADO del dispositivo que él mismo vinculó).
- Dependencias `require_role("TUTOR")` y comprobación de propiedad del recurso (anti IDOR/BOLA): todo acceso a un dispositivo verifica la relación tutor→dispositivo, no sólo el rol.
- Matriz de permisos documentada en `docs/security-baseline.md`.
- Pruebas negativas: el tutor A no puede leer el dispositivo del tutor B; el supervisado no puede escribir reglas.

Verificación: suite de autorización con casos positivos y negativos, y una prueba que recorre el router comprobando que ninguna ruta funcional queda sin dependencia de autorización.

### Paso 4 — Sprint 5: Vinculación tutor ↔ dispositivo

Objetivo: el flujo del código de 6 dígitos, seguro.

Trabajo:

- Generación con `secrets` (no `random`), 6 dígitos, TTL de 3 minutos en Redis, un solo uso.
- Se almacena el **hash** del código, no el código; comparación en tiempo constante.
- Rate limiting por cuenta, por dispositivo y por IP, con bloqueo temporal tras N intentos.
- Invalidación inmediata al usarse, al expirar y al generar uno nuevo para la misma sesión.
- Endpoints: generar, canjear, listar vinculaciones, desvincular y revocar.
- Android: pantalla de código para el supervisado y pantalla de generación para el tutor.
- Auditoría de vinculación, desvinculación y revocación.

Verificación: el canje correcto vincula; el segundo canje falla; el canje tras 3 minutos falla; la fuerza bruta sobre 6 dígitos queda bloqueada por rate limiting, demostrado con una prueba automatizada.

### Paso 5 — Sprint 6: Gestión de dispositivos

Objetivo: el tutor ve su parque de dispositivos con estado real.

Trabajo:

- Registro de dispositivo con nombre, modelo, versión de Android, versión de app e identificador propio.
- Heartbeat periódico → `last_seen`; máquina de estados `ONLINE / OFFLINE / SYNCING / ALERT / RESTRICTED / UNLINKED`.
- Listado y detalle en web y en la app del tutor; renombrar y desvincular.

Verificación: apagar el emulador y ver la transición a `OFFLINE` dentro del umbral definido.

---

## FASE C — Motor de reglas

Antes de cada paso de esta fase se repite el procedimiento obligatorio: verificar API oficial, versión mínima, permisos, restricciones, políticas de Play, viabilidad y alternativa; y actualizar `docs/android/capability-matrix.md`. No se implementa nada que Android no permita a una app normal.

### Paso 6 — Sprint 7: Inventario de aplicaciones

- Android: enumerar apps con `PackageManager` y uso con `UsageStatsManager`, previa concesión de `PACKAGE_USAGE_STATS` desde Ajustes (no es un permiso runtime normal).
- Backend: catálogo `applications` por dispositivo y sincronización incremental.
- Web y app del tutor: lista de apps con su tiempo de uso.

### Paso 7 — Sprint 8: Reglas de aplicaciones y bloqueo

- Decisión técnica documentada del mecanismo de bloqueo viable sin device owner (detección de la app en primer plano + pantalla de bloqueo propia), con sus límites explícitos.
- Motor de reglas en el backend (permitir / bloquear / límite / horario) y evaluación local en el dispositivo.
- Eventos `usage_events` reportados al backend.

### Paso 8 — Sprint 8/9: Control de navegación web

- Evaluación e implementación con `VpnService` local, sin servidor VPN externo: filtrado por dominio a partir de las consultas DNS, tratando aparte los resolutores DoH conocidos.
- Límites documentados honestamente: DNS cifrado, apps que ignoran el sistema, revocación por el usuario.
- Registro de `web_events` y de bloqueos.

### Paso 9 — Sprint 9: Listas blanca y negra

- Modelo de listas por dispositivo y por tutor, con **prioridad de reglas** definida y probada: lista negra > lista blanca > categoría > regla por defecto, o el orden que se justifique.

### Paso 10 — Sprint 10: Categorías

- Catálogo de las 11 categorías del enunciado, clasificación de dominios y apps, y reglas por categoría.

### Paso 11 — Sprints 11 y 12: Tiempo, horarios y modo escolar

- Límite diario, semanal, por app y por categoría; ventanas horarias; zona horaria del dispositivo.
- Modo escolar como perfil de reglas con vigencia horaria (07:00-14:00 por defecto, configurable).
- Aplicación local sin conexión permanente al backend.

---

## FASE D — Contexto y visibilidad

### Paso 12 — Sprint 13: Ubicación

- Clave de Google Maps Platform con restricción por package/SHA-1 y por referente web.
- Permisos de ubicación con justificación en pantalla; precisión y frecuencia mínimas necesarias.
- Almacenamiento con retención limitada y cifrado en tránsito y en reposo.

### Paso 13 — Sprint 14: Geocercas

- Geofencing API de Android; alta, baja y edición desde el tutor; eventos de ENTRADA y SALIDA.
- Límite de geocercas y latencia en background documentados.

### Paso 14 — Sprint 15: Historial

- Persistencia de eventos de apps, web, bloqueos, reglas, ubicación, geocercas y alertas.
- Política de retención por tipo de dato, con purga automatizada.

### Paso 15 — Sprint 16: Estadísticas

- Agregaciones por hoy / 7 días / 30 días; apps más usadas, categorías, bloqueos y cumplimiento.
- Consultas indexadas o tablas de agregación si el volumen lo exige.

### Paso 16 — Sprint 17: Alertas

- Tipos y niveles `INFO / WARNING / HIGH / CRITICAL`; reglas de generación; bandeja para el tutor; deduplicación y silenciado.

---

## FASE E — Sincronización y resiliencia

### Paso 17 — Sprint 18: Tiempo real

- WebSockets autenticados para tutor y dispositivo, con canal por dispositivo.
- Firebase Cloud Messaging para despertar al dispositivo: proyecto Firebase, `google-services.json` y credenciales de servidor en el gestor de secretos, nunca en el repositorio.
- Propagación de un cambio de regla verificable de extremo a extremo.

### Paso 18 — Sprint 19: Funcionamiento offline

- Room como fuente local de reglas, horarios, límites, listas y cola de eventos pendientes.
- Estrategia de conflicto y versión de política; las reglas siguen aplicándose sin Internet.

### Paso 19 — Sprint 20: Detección de manipulación

- Sólo señales legítimas: pérdida de permisos, desactivación del servicio, revocación de la VPN, silencio anómalo del heartbeat, cambio de hora e intento de desinstalación detectable por la API oficial.
- Sin rootkits, sin evasión y sin ocultamiento; se registra el evento y se alerta al tutor.

---

## FASE F — Endurecimiento

### Paso 20 — Sprint 21: Seguridad integral

- Repaso de OWASP Top 10 y OWASP API Top 10 sobre lo construido, con OWASP ASVS como lista de comprobación.
- Rate limiting global, validación estricta, cabeceras, CORS mínimo, gestión de secretos y TLS obligatorio.
- Escaneo con OWASP ZAP contra el entorno propio y MobSF sobre el APK.

### Paso 21 — Sprint 22: Auditoría

- Registro inmutable de acciones sensibles con actor, acción, recurso, fecha y origen.
- Consulta de auditoría para el tutor y exportación.

### Paso 22 — Sprint 23: Supervisión remota viable

- Evaluación formal de `MediaProjection` (consentimiento por sesión, foreground service de tipo `mediaProjection`) y de WebRTC como transporte.
- Se implementa únicamente lo que las APIs y las políticas permiten, con consentimiento visible y auditado.
- Cámara, micrófono y notificaciones se deciden con el mismo criterio y pueden quedar como V2.

---

## FASE G — Producto

### Paso 23 — Sprint 24: Panel web completo

- Las 16 secciones del dashboard sobre la misma API y la misma cuenta del tutor.

### Paso 24 — Sprint 25: Pruebas integrales

- Unitarias, de integración, de API (colección Postman/Newman en CI), E2E web con Playwright, instrumentadas de Android, offline, sincronización, permisos, autorización y rendimiento con k6.

### Paso 25 — Sprint 26: Despliegue

- Cuenta cloud, dominio, HTTPS con Let's Encrypt, reverse proxy, secretos gestionados, backups de PostgreSQL, monitorización y logs, y separación real de dev / test / prod.
- CD desde GitHub Actions con aprobación manual para producción.

### Paso 26 — Sprint 27: Documentación y presentación

- Documento técnico, manuales de instalación, usuario y administrador, plan de pruebas, análisis de riesgos, modelo de seguridad, política de privacidad y manual de despliegue.

---

## 4. Ciclo de trabajo dentro de cada paso

1. Explicar el objetivo. 2. Historias de usuario. 3. Criterios de aceptación. 4. Tareas técnicas. 5. Arquitectura afectada. 6. Base de datos. 7. Backend. 8. Android. 9. Web. 10. Seguridad. 11. Pruebas. 12. Documentación. 13. Definition of Done. 14. Cómo ejecutar. 15. Cómo comprobar que funciona.

Cierre de cada paso: pruebas en verde, CI en verde, documento del sprint actualizado, evidencia registrada y commit con mensaje descriptivo.

## 5. Riesgos abiertos

| Riesgo | Mitigación |
|---|---|
| El bloqueo de apps sin device owner es limitado y está sujeto a políticas de Play | Decidir y documentar el mecanismo en el Paso 7 antes de codificar |
| El DNS cifrado (DoH/DoT) evade el filtrado por dominio | Documentar el límite, bloquear resolutores DoH conocidos y no prometer filtrado absoluto |
| `MediaProjection` no permite consentimiento permanente | Sesiones explícitas y auditadas; alcance recortado si es necesario |
| Versiones muy recientes de AGP, Gradle y Compose | Fijar versiones compatibles verificadas en el Paso 0 y no moverlas sin motivo |
| Datos sensibles de menores | Minimización, retención corta, cifrado, control de acceso y auditoría desde el primer sprint que los toque |

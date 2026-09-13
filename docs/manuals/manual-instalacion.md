# Manual de instalación — NetProtect

Audiencia: quien clona el repositorio por primera vez en una máquina de desarrollo y necesita los
tres carriles (backend, web, Android) corriendo localmente. Todos los comandos de este manual ya
fueron ejecutados y verificados en `docs/sprint-01-evidence.md`; este manual no introduce ninguno
nuevo, sólo los organiza para quien empieza desde cero.

## 1. Requisitos previos

| Herramienta | Uso | Notas |
|---|---|---|
| Git | clonar el repositorio | — |
| Docker Desktop (con WSL2 en Windows) | backend, PostgreSQL, Redis, web en contenedores | el demonio debe estar corriendo antes de `docker compose up` |
| Node.js / npm | desarrollo del panel web fuera de contenedor | opcional si sólo se usa Docker |
| Python 3.12+ | desarrollo del backend fuera de contenedor | opcional; CI usa 3.13 |
| Java (JDK 21 LTS) | Android Gradle | ya lo trae Android Studio embebido |
| Android Studio | SDK, emulador, compilación de la app | ver §4 |

Ver `docs/sprint-01-paso-0.md` si hace falta replicar la puesta a punto exacta de Android
Studio/Docker en una máquina Windows nueva.

## 2. Clonar y configurar variables de entorno

```bash
git clone <url-del-repositorio>
cd netprotect
cp .env.development.example .env
```

Completar en `.env` cualquier valor marcado `change_me_*`/`your-*`. El `.env` real **nunca** se
versiona (`.gitignore`). Ver `docs/environments.md` para el detalle de cada ambiente
(desarrollo/pruebas/producción).

## 3. Levantar backend, PostgreSQL, Redis y web (Docker)

```bash
docker compose up --build -d
```

Verificación (los mismos cuatro *health checks* del Sprint 1):

```bash
curl http://localhost:8000/api/v1/health
curl http://localhost:8000/api/v1/health/db
curl http://localhost:8000/api/v1/health/redis
curl http://localhost:8000/api/v1/health/ready
```

`ready` sólo responde 200 si el backend alcanza PostgreSQL y Redis de verdad. El panel web queda en
`http://localhost:3000`.

Si se necesita aplicar una migración de Alembic nueva sin reconstruir todo el stack, `compose.yaml`
trae un servicio `migrate` independiente — ver la advertencia de `CLAUDE.md` ("Errores que ya se
cometieron una vez") sobre reconstruir `backend`/`web`/`migrate` juntos cuando cualquiera cambie.

## 4. Preparar Android Studio y el emulador

1. **SDK Manager → SDK Platforms**: instalar Android API 36.
2. **SDK Manager → SDK Tools**: Android SDK Command-line Tools (latest), Platform-Tools,
   Build-Tools 36.
3. **Device Manager**: crear un AVD (imagen **API 36, Google APIs, x86_64**).
4. Verificar `mobile/local.properties` → `sdk.dir` apunta a la ruta real del SDK en esta máquina
   (no una ruta de otro usuario/equipo — causa real de bloqueo documentada en
   `docs/planning/plan-desarrollo.md`, problema B1).

Compilar y correr pruebas unitarias:

```powershell
cd mobile
.\gradlew.bat test assembleDebug
```

Un archivo Kotlin nuevo no está verificado hasta que este comando (o `compileDebugKotlin`) corre
sobre él al menos una vez — un import faltante no lo marca ningún editor por sí solo
(`CLAUDE.md`, "Errores que ya se cometieron una vez").

## 5. Credenciales de Google (login)

El login con Google (Sprint 3) exige un proyecto real en Google Cloud Console:

1. Proyecto en Google Cloud Console + pantalla de consentimiento OAuth en modo "Prueba".
2. **Client ID tipo Web** (`GOOGLE_WEB_CLIENT_ID`) — lo usan el backend para validar y Next.js para
   el botón de login.
3. **Client ID tipo Android**, con el package `com.netprotect.app` y el SHA-1 del keystore de
   debug:

```powershell
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

4. Agregar como *tester* cualquier cuenta de Google que vaya a iniciar sesión — la pantalla de
   consentimiento sigue en modo "Prueba" (ver `CLAUDE.md`, sección "Entorno de trabajo").

Estas credenciales ya existen en la cuenta de Google Cloud del dueño original del proyecto;
pedirlas directamente o crear un proyecto propio siguiendo `docs/sprint-03.md`.

## 6. Verificar el panel web fuera de Docker (opcional, para iterar rápido)

```bash
cd frontend
npm install
npm run lint
npm run build
npm run dev
```

## 7. Correr la suite de pruebas del backend

Ver la nota de rendimiento de `CLAUDE.md`: en Windows, correr contra los puertos publicados de
Docker Desktop añade ~1.4s por petición; para verificar de verdad, usar siempre los contenedores
directamente:

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

`migrate` corre aparte, con `run --rm`, siempre antes del `up` — nunca como dependencia dentro del
mismo `up` (`--abort-on-container-exit` mataría el stack en cuanto `migrate` termine, antes de que
`backend` llegue a correr sus pruebas; ver `docs/sprint-02-evidence.md`).

## 8. Verificación final de que la instalación quedó completa

- `GET /api/v1/health/ready` responde 200.
- El panel web en `http://localhost:3000` permite iniciar sesión con Google.
- La app Android compila (`assembleDebug`) y, en el emulador, `HomeScreen` decide Tutor o
  Supervisado tras el login.
- `docker compose -f compose.test.yaml up ...` termina con la suite completa en verde (265 pruebas
  de backend a la fecha del Sprint 26, ver `docs/sprint-26-evidence.md`).

Si algo de esto falla, `docs/sprint-01.md`/`docs/sprint-01-evidence.md` documentan los bloqueos
reales ya encontrados y resueltos la primera vez que se instaló este proyecto.

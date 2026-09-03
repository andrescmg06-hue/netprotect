# PASO 0 — Cierre verificado del Sprint 1

Documento de trabajo. Sirve como instrucción para el equipo y como prompt para una sesión de agente.
Fecha: 03/09/2026.

---

## 1. Objetivo

El Sprint 1 está construido pero **no demostrado**: `docs/sprint-01-evidence.md` declara que el build de
Android, `docker compose up` y `npm install` nunca se ejecutaron. Además, cambios sin commitear dejaron
el árbol en un estado que no compila.

El Paso 0 termina cuando los tres carriles del incremento —`Android → Backend → PostgreSQL`,
`Web → Backend → PostgreSQL` y `Backend → Redis`— arrancan en este computador, cada criterio de
aceptación del Sprint 1 tiene una salida real que lo respalda, y el trabajo está commiteado.

## 2. Alcance

**Dentro:** reparar los 8 bloqueos, compilar Android, levantar la pila con Docker, ejecutar las
verificaciones, registrar evidencia y commitear.

**Fuera:** cualquier cosa del Sprint 2 en adelante. No se crean tablas de negocio, ni modelos ORM, ni
migraciones, ni autenticación, ni endpoints nuevos. Si algo de eso hace falta, se anota y se hace en su paso.

## 3. Bloqueos que se resuelven

| # | Bloqueo | Síntoma |
|---|---|---|
| B1 | `mobile/local.properties` apunta a `C:\Users\crist\...\Android\Sdk` | Gradle no encuentra el SDK |
| B2 | Docker Desktop instalado pero sin arrancar | No hay API, ni PostgreSQL, ni Redis |
| B3 | `MainActivity` con `Text("NETPROTECT FUNCIONA")` en vez de `SprintOneScreen` | Se pierde el criterio 8 |
| B4 | Falta la plataforma Android 36, las command-line tools, la system image y el AVD | No compila ni se ejecuta |
| B5 | Wrapper Gradle 9.3.0 sin validar contra AGP 8.13.2 y Kotlin 2.3.21 | Build puede fallar |
| B6 | `compose.yaml` con contraseñas reales por defecto y sin publicar 5432/6379 | Secreto en el repo, BD no inspeccionable |
| B7 | 6 archivos modificados y wrapper sin versionar | Build no reproducible |
| B8 | `ALLOWED_HOSTS` no incluye `10.0.2.2` | **La app Android recibe HTTP 400 aunque todo esté arriba** |
| B9 | `compose.test.yaml` y `compose.prod.yaml` montan `/var/lib/postgresql/data` | `postgres:18` rechaza ese punto de montaje y el contenedor no arranca |

Detalle de B9 (encontrado al ejecutar la integración, no en la revisión estática inicial): desde la
versión 18, la imagen oficial de PostgreSQL guarda los datos en una ruta con el número de versión
(`/var/lib/postgresql/18/docker`) y **rechaza explícitamente** el montaje directo en
`/var/lib/postgresql/data` con el mensaje "this is usually the result of upgrading the Docker image
without upgrading the underlying database". `compose.yaml` ya montaba el directorio padre
`/var/lib/postgresql` (forma correcta para 18+), pero `compose.test.yaml` (como `tmpfs`) y
`compose.prod.yaml` (como volumen nombrado) seguían con la ruta vieja. Se corrigen ambos de la misma
forma. Sin este cambio, `docker compose -f compose.test.yaml up` falla siempre con
`db-1 exited with code 1`, y el job `integration` de CI nunca pasaría.

Detalle de B8: `TrustedHostMiddleware` compara el `Host` de la petición contra `ALLOWED_HOSTS`. El
emulador llama a `http://10.0.2.2:8000/api/v1/health/ready`, así que el `Host` es `10.0.2.2`, que no está
en la lista `localhost,127.0.0.1,backend`. Starlette responde 400 antes de llegar al endpoint y
`InfrastructureHealthClient` muestra «La API respondió HTTP 400». Sin esto, el criterio 8 no puede cumplirse.

---

## 4. PARTE A — Instalaciones en el computador

Esta parte la hace una persona en la interfaz gráfica. No la puede hacer el agente.

### A1. Arrancar Docker Desktop

1. Abrir Docker Desktop desde el menú Inicio.
2. Esperar a que el indicador inferior izquierdo diga **Engine running**.
3. Comprobar en la terminal:

```powershell
docker info --format "{{.ServerVersion}}"
docker compose version
```

Ambos deben responder sin error. Si `docker info` falla con `dockerDesktopLinuxEngine`, el motor todavía
no terminó de arrancar o falta habilitar WSL2 en Settings → General.

### A2. Instalar la plataforma Android 36

1. Android Studio → **Settings** → *Languages & Frameworks* → **Android SDK**.
2. Pestaña **SDK Platforms** → marcar **Android API 36**.
3. **Apply** y aceptar la licencia.

Se instala la 36 porque `mobile/app/build.gradle.kts` declara `compileSdk = 36` y `targetSdk = 36`. En el
equipo sólo está `android-37.0`, que no sirve para ese `compileSdk`.

### A3. Instalar las herramientas del SDK

1. Misma pantalla, pestaña **SDK Tools**.
2. Marcar **Android SDK Command-line Tools (latest)**.
3. Confirmar que están **Android SDK Platform-Tools** y **Android SDK Build-Tools 36**.
4. **Apply**.

### A4. Crear el emulador

1. Android Studio → **Device Manager** → **Create Device**.
2. Dispositivo: **Pixel 8** (o cualquier teléfono reciente).
3. System image: **API 36**, variante **Google APIs**, ABI **x86_64**. Descargar si aparece la flecha.
4. Nombre del AVD: `NetProtect_API36`. **Finish**.

Se elige *Google APIs* y no *Google Play* porque incluye los servicios de Google que harán falta en los
pasos de Maps y FCM, y deja el dispositivo con permisos de depuración más cómodos.

### A5. Comprobar la instalación

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" version
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed
```

En la lista deben aparecer `platforms;android-36`, `build-tools;36.x.x`, `platform-tools` y la system image.

---

## 5. PARTE B — Cambios de código

Formato: ARCHIVO, CAMBIO, MOTIVO. Ejecutar en este orden.

### B1. `mobile/local.properties`

**Cambio:** `sdk.dir=C\:\\Users\\andre\\AppData\\Local\\Android\\Sdk`
**Motivo:** la ruta actual pertenece a otro usuario de Windows y no existe aquí. El archivo está en
`.gitignore`, así que es una corrección local y no se versiona.

### B2. `mobile/app/src/main/java/com/netprotect/app/MainActivity.kt`

**Cambio:** volver a importar `com.netprotect.app.feature.sprint1.SprintOneScreen` y montarlo dentro de
`MaterialTheme`; eliminar los imports de `Text` y `Color` que dejó la prueba manual.
**Motivo:** `SprintOneScreen` es lo único que consulta `/api/v1/health/ready` y pinta las tres filas
`Android → Backend`, `Backend → PostgreSQL` y `Backend → Redis`. Sin ella el criterio 8 no existe.

### B3. `.env`, `.env.development.example`, `compose.yaml`

**Cambio:** `ALLOWED_HOSTS=localhost,127.0.0.1,backend,10.0.2.2`
**Motivo:** resuelve B8. Si además se va a probar en un teléfono físico, se añade la IP LAN del PC.

### B4. `compose.yaml`

**Cambios:**

1. Quitar los valores reales como fallback. Las credenciales pasan a ser obligatorias:
   `POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?falta POSTGRES_PASSWORD en .env}` y lo mismo para
   `REDIS_PASSWORD`, `DATABASE_URL` y `REDIS_URL`.
2. Volver a publicar `127.0.0.1:5432:5432` en `db` y `127.0.0.1:6379:6379` en `redis`.
3. Conservar las mejoras que sí aportan: `restart: unless-stopped`, `start_period` en los healthchecks y
   los nombres explícitos de volúmenes y red.
4. Añadir salto de línea final al archivo.

**Motivo:** (1) el repositorio no puede contener contraseñas, ni siquiera de desarrollo, y fallar con un
mensaje claro es mejor que arrancar con una credencial conocida; (2) sin puertos publicados no se puede
inspeccionar la base con psql ni DBeaver, y ese acceso hace falta desde el Paso 1; (3) son cambios buenos
del trabajo pendiente y se mantienen.

### B5. `.env`

**Cambio:** generar contraseñas nuevas para `POSTGRES_PASSWORD` y `REDIS_PASSWORD`, y propagarlas de forma
coherente a `DATABASE_URL` y `REDIS_URL`. Usar sólo caracteres alfanuméricos.
**Motivo:** las anteriores quedaron escritas en el árbol de trabajo, así que se consideran comprometidas.
Se evitan símbolos porque `DATABASE_URL` y `REDIS_URL` son URLs y `@`, `:`, `/` o `#` obligarían a
codificarlas en porcentaje. Si se cambian las contraseñas hay que borrar los volúmenes
(`docker compose down -v`), porque PostgreSQL sólo fija la contraseña en la primera inicialización.

### B6. Versionar el wrapper de Gradle

**Cambios:** quitar `gradlew`, `gradlew.bat` y `gradle/wrapper/` de la exclusión efectiva y añadirlos al
control de versiones; crear `.gitattributes` en la raíz con al menos:

```text
* text=auto eol=lf
*.bat text eol=crlf
gradlew text eol=lf
*.jar binary
```

**Motivo:** el wrapper es lo que garantiza que todos compilen con la misma versión de Gradle. `gradlew` es
un script de shell que se rompe si git lo convierte a CRLF, y `gradlew.bat` se rompe si lo convierte a LF.

### B7. `.github/workflows/ci.yml`

**Cambio:** en el job `android`, sustituir `gradle-version: "8.13"` y `gradle test assembleDebug` por el
wrapper: `./gradlew test assembleDebug`.
**Motivo:** si el wrapper es la fuente de verdad del build local, CI debe usarlo; de lo contrario CI y el
equipo compilan con versiones distintas.

### B8. Compilar y fijar versiones compatibles

**Cambio:** ejecutar el build y, si falla por incompatibilidad, ajustar en este orden:
`gradle/wrapper/gradle-wrapper.properties` (versión de Gradle), luego `mobile/gradle/libs.versions.toml`
(AGP, Kotlin, Compose BOM).
**Motivo:** hoy conviven Gradle 9.3.0, AGP 8.13.2, Kotlin 2.3.21 y dos valores distintos de Compose BOM
(`2026.08.00` versionado, `2026.06.00` en el árbol de trabajo). No se supone cuál es compatible: se compila
y se fija la combinación que funcione, dejando anotado en `docs/sprint-01.md` cuál quedó.

### B9. Revisar el reformateo pendiente

**Cambio:** en `mobile/app/build.gradle.kts` conservar el cambio funcional —`kotlinOptions` sustituido por
el bloque `kotlin { compilerOptions { jvmTarget } }`— y devolver a su forma anterior el reformateo que no
aporta nada (dependencias partidas en varias líneas).
**Motivo:** un diff pequeño y explicable es revisable; uno lleno de ruido esconde los cambios reales.

---

## 6. PARTE C — Verificación

Cada comprobación se mapea a un criterio de aceptación de `docs/sprint-01.md`. Se ejecutan todas y se
guarda la salida real.

### C1. Infraestructura (criterios 2 a 6)

```powershell
docker compose up --build -d
docker compose ps
```

Los cuatro servicios deben aparecer `running`, y `db`, `redis` y `backend` como `healthy`.

```powershell
curl.exe -s http://localhost:8000/api/v1/health
curl.exe -s http://localhost:8000/api/v1/health/db
curl.exe -s http://localhost:8000/api/v1/health/redis
curl.exe -s http://localhost:8000/api/v1/health/ready
```

Salidas esperadas: `status: ok` en las tres primeras y, en la cuarta,
`{"status":"ready","backend":"connected","database":"connected","redis":"connected"}`.

### C2. Readiness negativo (criterio 6)

```powershell
docker compose stop redis
curl.exe -s -o NUL -w "%{http_code}`n" http://localhost:8000/api/v1/health/ready
docker compose start redis
```

Debe devolver **503**. Si devuelve 200 con Redis caído, el endpoint no está comprobando nada y hay un fallo real.

### C3. Persistencia de PostgreSQL

```powershell
docker compose exec db psql -U netprotect -d netprotect -c "create table paso0_check(id int); insert into paso0_check values (1);"
docker compose down
docker compose up -d
docker compose exec db psql -U netprotect -d netprotect -c "select count(*) from paso0_check;"
docker compose exec db psql -U netprotect -d netprotect -c "drop table paso0_check;"
```

Debe devolver `1`. Esto confirma que el punto de montaje del volumen es el correcto para
`postgres:18`, que cambió la ubicación del directorio de datos respecto a versiones anteriores.

### C4. Web (criterio 7)

```powershell
cd frontend
npm install
npm run lint
npm run build
```

Después, abrir `http://localhost:3000` y comprobar que la página muestra la infraestructura conectada.

### C5. Backend en local (calidad)

```powershell
cd backend
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt -r requirements-dev.txt
pytest -q -m "not integration"
ruff check app tests
```

Esperado: `4 passed, 3 deselected` y ruff sin hallazgos.

### C6. Android compila (criterios 8 y 9)

```powershell
cd mobile
.\gradlew.bat --version
.\gradlew.bat test assembleDebug
```

Esperado: `BUILD SUCCESSFUL` y el APK en `mobile/app/build/outputs/apk/debug/`.

### C7. Android ejecuta (criterio 8)

1. Arrancar el AVD `NetProtect_API36` desde el Device Manager.
2. Ejecutar la configuración `app` en variante **debug**.
3. La pantalla debe mostrar «NETPROTECT · SPRINT 1» y las tres filas en **CONNECTED**.
4. Pulsar «Volver a comprobar» y confirmar que sigue en verde.
5. Prueba negativa: `docker compose stop backend`, pulsar «Volver a comprobar» y confirmar que aparece
   «Infraestructura no disponible»; después `docker compose start backend`.

Si aparece HTTP 400, B3 no se aplicó o el contenedor no se recreó tras cambiar `.env`
(`docker compose up -d --force-recreate backend`).

### C8. Release deshabilita cleartext (criterio 9)

```powershell
cd mobile
.\gradlew.bat assembleRelease
```

Verificar en el manifiesto fusionado de release
(`app/build/intermediates/merged_manifests/release/AndroidManifest.xml`) que
`android:usesCleartextTraffic="false"`.

### C9. Integración en contenedores (criterio 11)

```powershell
docker compose -f compose.test.yaml config
docker compose -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend
docker compose -f compose.test.yaml down -v
```

### C10. Sin secretos versionados (criterio 12)

```powershell
git status --short
git grep -nE "NetProtectDev2026|NetProtectRedis2026" -- . ; if ($LASTEXITCODE -eq 0) { "REVISAR" } else { "limpio" }
```

`.env` no debe aparecer como archivo seguido, y la búsqueda no debe encontrar credenciales.

### C11. CI (criterio 11)

Hacer push y comprobar que los cuatro jobs —`backend`, `frontend`, `android`, `integration`— quedan en verde.

---

## 7. PARTE D — Evidencia y cierre

1. Reescribir `docs/sprint-01-evidence.md` con las salidas **reales** de C1 a C11, con fecha y equipo.
   Lo que no se ejecutó se marca como no ejecutado; no se da por bueno nada por inspección visual.
2. Anotar en `docs/sprint-01.md` la combinación de versiones que quedó fijada (Gradle, AGP, Kotlin, Compose BOM).
3. Actualizar en `README.md` los requisitos si alguna versión cambió.
4. Commitear en dos partes, para que el diff sea legible:

```powershell
git add .gitattributes mobile/gradlew mobile/gradlew.bat mobile/gradle/wrapper
git commit -m "build: version gradle wrapper for reproducible android builds"

git add -A
git commit -m "fix: restore sprint one android screen and harden dev compose"
```

Ambos mensajes terminan con la línea de coautoría acordada para el proyecto.

## 8. Definition of Done del Paso 0

- [x] Los cuatro endpoints de salud responden y `/ready` devuelve 503 con Redis caído.
- [x] Los datos de PostgreSQL sobreviven a `down` + `up`.
- [x] `npm run lint` y `npm run build` pasan. HTTP 200 confirmado; la transición visual a "online" en
      un navegador real queda pendiente (ver `docs/sprint-01-evidence.md`).
- [x] `pytest -m "not integration"` y `ruff` pasan en local.
- [x] `gradlew test assembleDebug` y `assembleRelease` terminan en `BUILD SUCCESSFUL`.
- [x] La app en el emulador muestra las tres filas en CONNECTED (captura de pantalla real). **Parcial**:
      falta la prueba negativa (reacción al fallo del backend) — bloqueada por contención de recursos
      entre el emulador y el CLI de Docker en esta sesión, ver `docs/sprint-01-evidence.md`.
- [x] El manifiesto de release tiene `usesCleartextTraffic="false"`.
- [x] `compose.test.yaml` termina con código 0 (`7 passed, 3 warnings`; corregido B9 para lograrlo).
- [x] No hay credenciales versionadas y `.env` sigue ignorado.
- [ ] CI en verde en los cuatro jobs. **Pendiente de push**.
- [x] `docs/sprint-01-evidence.md` contiene salidas reales.
- [x] Todo commiteado y sin cambios sueltos en `git status`.

## 9. Fallos previstos

| Síntoma | Causa probable | Acción |
|---|---|---|
| `SDK location not found` | B1 no aplicado o ruta mal escapada | Revisar `sdk.dir` con dobles barras invertidas |
| `Failed to install the following SDK components: platforms;android-36` | Licencia sin aceptar | `sdkmanager.bat --licenses` y aceptar todo |
| Gradle falla por versión incompatible | B5 | Bajar la versión del wrapper o subir AGP hasta que compile; dejarlo anotado |
| `Unsupported class file major version` | JDK que usa Gradle | Fijar el JDK de Android Studio en Settings → Build Tools → Gradle |
| App muestra HTTP 400 | B8 o contenedor sin recrear | Añadir `10.0.2.2` a `ALLOWED_HOSTS` y `--force-recreate backend` |
| App muestra timeout o `Failed to connect` | El emulador no alcanza el host | Confirmar que el backend publica `127.0.0.1:8000` y que la URL es `10.0.2.2` |
| `password authentication failed` tras rotar credenciales | Volumen viejo con la contraseña anterior | `docker compose down -v` y volver a levantar |
| Healthcheck de Redis en `unhealthy` | `REDIS_PASSWORD` distinto del que va en `REDIS_URL` | Igualar ambos en `.env` |

## 10. Reglas del paso

- No se marca como verificado nada que no se haya ejecutado.
- No se toca nada de sprints posteriores: sin tablas, sin ORM, sin autenticación, sin endpoints nuevos.
- Si aparece un problema fuera de alcance, se anota en `docs/sprint-01.md` y se lleva a su sprint.
- Todo cambio de versión queda justificado por escrito.

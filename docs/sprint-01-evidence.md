# Evidencia de verificación — Sprint 1

Fecha: 03/09/2026. Equipo: `andre`, Windows 11 Pro. Docker Desktop 29.5.2 (Compose v5.1.4).

Esta revisión corresponde al cierre del **Paso 0** (`docs/sprint-01-paso-0.md`). A diferencia de la
evidencia previa, todo lo listado aquí como "ejecutado" se ejecutó realmente en este equipo durante esta
sesión de trabajo.

## Bloqueos encontrados y resueltos

Además de los 8 bloqueos identificados en `docs/sprint-01-paso-0.md` (B1-B8), al ejecutar la integración
en contenedores efímeros apareció un noveno:

- **B9 (nuevo):** `compose.test.yaml` y `compose.prod.yaml` seguían montando el volumen de PostgreSQL en
  `/var/lib/postgresql/data`. La imagen `postgres:18` rechaza ese punto de montaje explícitamente
  ("this is usually the result of upgrading the Docker image without upgrading the underlying database")
  porque desde la versión 18 los datos se guardan en una ruta con el número de versión
  (`/var/lib/postgresql/18/docker`). Se corrigió montando el directorio padre `/var/lib/postgresql`,
  igual que ya se había hecho en `compose.yaml`. Sin este cambio, `docker compose -f compose.test.yaml up`
  fallaba con `db-1 exited with code 1` y el job `integration` de CI habría fallado siempre.

## Infraestructura — `docker compose up --build -d` (compose.yaml)

```text
Container netprotect-dev-db-1       Up (healthy)   127.0.0.1:5432->5432/tcp
Container netprotect-dev-redis-1    Up (healthy)   127.0.0.1:6379->6379/tcp
Container netprotect-dev-backend-1  Up (healthy)   127.0.0.1:8000->8000/tcp
Container netprotect-dev-web-1      Up             127.0.0.1:3000->3000/tcp
```

Los cuatro servicios se construyeron y arrancaron sin intervención manual.

### Endpoints de salud

```text
GET /api/v1/health        → {"status":"ok","service":"NetProtect API","environment":"development","version":"0.1.0-sprint1"}
GET /api/v1/health/db     → {"status":"ok","database":"connected"}
GET /api/v1/health/redis  → {"status":"ok","redis":"connected"}
GET /api/v1/health/ready  → {"status":"ready","backend":"connected","database":"connected","redis":"connected"}
```

### Readiness negativo (criterio 6)

Se detuvo el contenedor `redis` con la pila corriendo y se repitió la petición:

```text
docker compose stop redis
GET /api/v1/health/ready → HTTP 503 {"detail":"redis_unavailable"}
docker compose start redis
```

Confirma que `/health/ready` depende realmente de la disponibilidad de Redis y no es un valor fijo.

### Persistencia de PostgreSQL

```text
docker compose exec db psql ... "create table paso0_check(id int); insert into paso0_check values (1);"
docker compose down            # sin -v: conserva los volúmenes
docker compose up -d
docker compose exec db psql ... "select count(*) from paso0_check;"  → 1
docker compose exec db psql ... "drop table paso0_check;"
```

Los datos sobrevivieron a `down` + `up`, confirmando que el punto de montaje `postgres_dev_data:/var/lib/postgresql`
es el correcto para la imagen `postgres:18.6-alpine`.

## Web — Next.js

```bash
cd frontend
npm install          # 344 paquetes, sin errores
npm run lint          # sin errores tras corregir un hallazgo (ver "Hallazgos" abajo)
npm run build          # compilación exitosa, 2 rutas estáticas generadas
```

`curl -s -o /dev/null -w "%{http_code}" http://localhost:3000` → `200`.
El HTML servido por el contenedor muestra el estado inicial `checking` (correcto: es un componente
cliente, la comprobación real contra `/api/v1/health/ready` ocurre en el navegador tras la hidratación;
esta sesión no abrió un navegador real, así que la transición visual a "online" no fue observada
directamente y queda pendiente de una comprobación manual en pantalla).

## Backend — pruebas y calidad

```bash
cd backend
py -m venv .venv
pip install -r requirements.txt -r requirements-dev.txt
pytest -q -m "not integration"   → 4 passed, 3 deselected
ruff check app tests             → All checks passed (tras corregir 2 hallazgos, ver abajo)
```

## Integración en contenedores efímeros — `compose.test.yaml`

```bash
docker compose -f compose.test.yaml config                     → válido
docker compose -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend
  → backend-1: 7 passed, 3 warnings
  → backend-1 exited with code 0
docker compose -f compose.test.yaml down -v
```

Las 7 pruebas incluyen las de integración (marcadas `integration`) que en el entorno local se
deseleccionan por no tener PostgreSQL/Redis reales; aquí sí se ejecutaron contra instancias reales.

## Hallazgos corregidos durante la verificación (no estaban en el diagnóstico original)

Al ejecutar por primera vez `ruff` y `npm run lint` contra el código ya versionado (nunca se habían
corrido con éxito antes; la evidencia previa lo admitía explícitamente), aparecieron 2 problemas
preexistentes sin relación con los cambios de este Paso 0:

1. **`backend/app/cache/redis_client.py`**: `except (OSError, asyncio.TimeoutError)` — regla `UP041` de
   ruff 0.13 (no presente en ruff 0.12, la versión con la que probablemente se verificó por última vez).
   Corregido a `TimeoutError` nativo.
2. **`backend/app/db/session.py`**: import sin ordenar y línea de 101 caracteres. Corregido con
   `ruff --fix`.
3. **`frontend/src/app/page.tsx`**: la regla `react-hooks/set-state-in-effect` (ESLint React Hooks 7.1.1)
   detectó que `checkInfrastructure` actualizaba estado de forma síncrona dentro de un efecto. Se separó
   la comprobación (como cadena de promesas `.then()/.catch()` dentro del efecto) del reinicio manual de
   estado (sólo en el manejador del botón), con un contador `attempt` para forzar el reintento.
4. **`frontend/next.config.ts`**: Next.js avisaba de un `package-lock.json` ajeno fuera del repositorio
   (en `C:\Users\andre`) y quedaba ambigua la raíz del workspace de Turbopack. Se fijó explícitamente con
   `turbopack.root`.

Ninguno de estos afecta el comportamiento en producción; los cuatro se consideran parte de dejar el
Sprint 1 realmente verificado, no del alcance de sprints posteriores.

## Pendiente — requiere Android Studio (fuera del alcance de una sesión de agente)

No se pudo completar por falta de la plataforma Android 36, las command-line tools y un AVD en este
equipo (ver Parte A de `docs/sprint-01-paso-0.md`, pasos A2-A5, que requieren la interfaz gráfica de
Android Studio):

1. `./gradlew test assembleDebug` — build de Android.
2. Fijar la combinación definitiva de versiones (Gradle 9.3.0 / AGP 8.13.2 / Kotlin 2.3.21 / Compose BOM)
   una vez que compile.
3. Ejecución en el emulador mostrando `Backend`, `PostgreSQL` y `Redis` en `CONNECTED`, y la prueba
   negativa de detener el backend y ver el mensaje de error.
4. `./gradlew assembleRelease` y verificación de `usesCleartextTraffic="false"` en el manifiesto fusionado.
5. Pipeline de CI en GitHub (no se ha hecho push todavía).
6. Confirmación visual en navegador de la transición de la web a "online" (sólo se confirmó el HTTP 200
   y el HTML inicial en estado "checking"; ver nota en la sección Web).

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente en este equipo, con su salida real. Lo que aparece en
la sección "Pendiente" no se da por bueno: no hay build de Android, ni ejecución en emulador, ni
confirmación visual en navegador, ni pipeline de CI ejecutado.

# Sprint 71 — Preparar la salida a producción (lo que se resuelve en el repo)

Rama `sprint-71-salida-produccion` (desde `main` tras B-09). Estado: cerrado.

El dueño pidió «solucionar todos los problemas y dejar todo listo para ponerla en uso». Un mapeo de solo lectura (`docs/progress.md`, `compose.prod.yaml`, `infra/`, `cd.yml`, Gradle, validador de configuración) separó lo que se arregla en el repo de lo que exige una persona o una cuenta externa. Este sprint hace lo primero y deja escrito lo segundo en una lista de salida ejecutable.

## Hallazgos que se corrigen (verificados en el código)

**ALTA: producción arrancaría o se compilaría con valores falsos sin avisar**
- `backend/app/core/config.py:193-212`: el validador de producción solo compara seis secretos con su valor de desarrollo. No rechaza `GOOGLE_WEB_CLIENT_ID` vacío o de ejemplo, `CORS_ORIGINS`/`ALLOWED_HOSTS` con `localhost`, ni valores de ejemplo `REPLACE_WITH_…` (p. ej. FCM: `config.py:115-122` solo trata `""` como push desactivado, así que un placeholder intentaría enviar a un proyecto falso).
- `mobile/app/build.gradle.kts:32-37`: el release sale con la API `https://api.netprotect.invalid` y el client ID de Google vacío si faltan las variables, sin fallar.
- `mobile/app/build.gradle.kts:54-62`: el tipo `release` no tiene `signingConfig`; no se puede firmar sin editar el archivo.
- `.github/workflows/cd.yml:91-92`: la imagen web de producción se construye con `https://api.example.com` y client ID vacío si faltan las variables del repositorio, y se publica igual.

**MEDIA: operación**
- `compose.prod.yaml`: solo `db` y `redis` tienen healthcheck; `web` y `caddy` no esperan a que el backend esté sano; ningún servicio tiene límite de memoria; `FCM_SERVICE_ACCOUNT_FILE` se pasa como ruta pero el archivo no se monta.
- `cd.yml:116-122`: el despliegue hace `git pull` y `up -d` sin comprobar `/api/v1/health/ready` después.
- `infra/caddy/Caddyfile`: sin límite de tamaño de cuerpo ni cabeceras de seguridad propias (las ponen Next y el backend; esto es defensa en profundidad).
- `mobile/app/build.gradle.kts:47-48`: `versionCode = 1` fijo; un APK nuevo no puede actualizar uno instalado.
- `infra/monitoring/alert_rules.yml`: solo `BackendDown` y `HighErrorRate`; sin alerta de disco ni de backup fallido.

**BAJA: documentación**
- `README.md` desactualizado (habla del Sprint 1, FCM «futuro»), sin sección de producción.
- No hay una lista de salida única: está repartida entre `docs/environments.md`, `docs/security-baseline.md`, `docs/manuals/analisis-riesgos.md` y `docs/progress.md`.

## Decisiones

- **Fallar pronto y en voz alta**: producción no arranca y el release no compila con valores de ejemplo o vacíos. Mejor un error al desplegar que una app que apunta a un dominio inválido con datos de menores.
- **El validador rechaza placeholders** en lugar de editar `.env.production.example` (una regla de permisos impide leer los `.env*` en esta sesión; el ejemplo queda como está y se anota).
- **Firma por variables de entorno** (`NETPROTECT_RELEASE_KEYSTORE_*`), nunca un keystore en el repo. Sin variables, `assembleRelease` falla con un mensaje claro; `minified` (debug-signed, solo local) no cambia.
- `versionCode`/`versionName` desde variables con el valor actual por defecto (sin cambio para debug).
- Sin cambios de rutas, de lógica de negocio ni del cliente Android de FCM (no existe; el servicio ya refresca por sondeo; queda anotado).

## Fuera de alcance (exige una persona o cuenta; va a la lista de salida)

Dominio y DNS, VPS y sus secretos, variables y secretos de GitHub, entorno `production` con revisor (H-04), cliente OAuth de producción, keystore real y su huella SHA-1, Firebase, TURN con IP pública y firewall, destino de alertas y copia de backups fuera del servidor, certificate pinning, pruebas en dispositivo y con personas, política de privacidad, D-01 (filtrado web por VPN).

## Tareas

- [x] T1 — Mapear y abrir el sprint. Inline. (este commit)
- [x] T2 — Validador de producción (tests primero). Delegado (**Opus**; configuración y secretos). `2ac69af`
- [x] T3 — Gradle: release exige URL y client ID, firma por variables, versión por variables. Delegado (mismo escritor, unidad aparte). `fe990ea`
- [x] T4 — `compose.prod.yaml` (healthchecks, `depends_on` sanos, límites de memoria, montaje del archivo FCM), Caddy (límite de cuerpo), `cd.yml` (fallar sin variables, comprobar salud tras desplegar), reglas de alerta de disco y backup. Delegado (mismo escritor, unidad aparte). `91c88c7`
- [x] T5 — `docs/go-live-checklist.md` (orden, comandos, quién) y README. Delegado (mismo escritor, unidad aparte). `76f95fb`
- [x] T6 — Verificación (pytest en contenedor, `docker compose config` de prod, Gradle `assembleDebug` y comprobación de que `assembleRelease` falla sin variables), `security-reviewer` y PR. Inline.

## Criterios de aceptación

1. `APP_ENV=production` con cualquier valor de ejemplo, vacío obligatorio o `localhost` en CORS/hosts → el backend no arranca y el error dice qué variable.
2. `assembleRelease` sin URL, client ID o keystore → falla con un mensaje que nombra la variable; `assembleDebug` y `minified` siguen igual.
3. `docker compose -f compose.prod.yaml config` válido; `web`/`caddy` esperan a un backend sano.
4. El despliegue falla si el backend no responde sano tras `up -d`.
5. Una persona puede seguir `docs/go-live-checklist.md` de principio a fin sin buscar en otros documentos.

## Revisión de seguridad (T6) y su estado

Sin hallazgos ALTA. Lo que encontró `security-reviewer` y qué se hizo:

- **MEDIA — la restauración de `go-live-checklist.md` §11 borraría producción:** el comando usaba
  `.env.production` y `compose.prod.yaml` con la indicación vaga «en un entorno que no sea
  producción». **Corregido:** el paso dice ahora que nunca se ejecuta en el servidor real y por qué.
- **MEDIA — `FCM_SERVICE_ACCOUNT_SECRET_FILE` no figura en `.env.production.example`:** está
  documentada en la lista de salida (§5 y §6) y en `compose.prod.yaml`. **Pendiente del dueño:** una
  regla de permisos impide a Claude leer los `.env*`; hay que añadir la línea a mano.
- **BAJA, pendientes para el sprint de despliegue:** logs públicos del job de `cd.yml`, `::1` en el
  validador de hosts, dimensionado de swap/RAM del VPS y frases desfasadas en la documentación.


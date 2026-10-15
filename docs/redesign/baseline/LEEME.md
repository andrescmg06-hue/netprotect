# Línea base visual del panel web (Sprint 53, T1)

Capturas del panel **antes** de cualquier cambio del rediseño (tokens, tipografía, primitivas), para poder detectar
regresiones visuales en los sprints S53–S59. No son una prueba automática: se comparan a ojo (o con una herramienta de
diff de imágenes) contra capturas nuevas hechas con el mismo procedimiento.

- Fecha: 04/10/2026
- Rama: `sprint-53-rediseno-web-base-visual`, commit `bcc069a` (árbol sin cambios en `frontend/src`, `frontend/public`,
  `backend/` ni `mobile/`; el build es el del código de ese commit).
- Total: **34 PNG** (login + 16 vistas, cada una a 1440 y a 390), unos 2,8 MB en conjunto.

## Archivos y viewports

| Archivo | Viewport | Qué muestra |
|---|---|---|
| `login-1440.png`, `login-390.png` | 1440x900 y 390x844 | Pantalla de login, sin sesión |
| `<clave>-1440.png` | 1440x900 | La vista con sesión de tutor |
| `<clave>-390.png` | 390x844 | La misma vista, mismo navegador, sin recargar |

Claves: `account`, `overview`, `devices`, `pairing`, `apps`, `rules`, `policy`, `categories`, `location`, `geofences`,
`history`, `statistics`, `alerts`, `silenced`, `remote`, `audit` (las de `frontend/src/lib/dashboardSections.ts`).

Todas son capturas de **página completa** (`fullPage`) con animaciones desactivadas, en Chromium de Playwright 1.63. Por
eso el alto varía: a 1440 casi todas miden 900 px, y las más largas (categorías, geocercas, estadísticas) 1049-1052 px;
a 390 miden entre 844 y 2127 px.

## Datos de la sesión sembrada

Sesión real creada con el script del propio proyecto, sin mocks de autenticación (el mismo mecanismo que usa el job `e2e`
de CI): `backend/scripts/seed_test_session.py` dentro de `compose.test.yaml`, y luego las mismas llamadas HTTP reales de
`frontend/e2e/global-setup.ts`.

- Un tutor (`Sprint 25 seed (tutor)`, correo `tutor-<hex>@example.com`, zona horaria `America/Bogota`).
- Un dispositivo vinculado y "En línea": `Baseline S53 2026-10-04`, Android 16.
- Una regla por app: `com.instagram.android`, Bloquear.
- Nada más: sin apps sincronizadas, sin ubicaciones, sin geocercas, sin eventos, sin alertas, sin categorías asignadas.
  No se insertó ningún dato a mano.

## Estado de cada vista (lo que la UI actual muestra)

Ninguna vista muestra error ni quedó cargando: en la corrida final no hubo errores de consola, errores de página ni
respuestas HTTP >= 400 (ver "Cómo se hizo").

- Con datos: `overview` (1/1 en línea, 0 alertas, 0 bloqueos, 0 min), `devices`, `rules` (1 regla activa), `audit`
  (5 filas de actividad real), `account`, `policy` (modo por defecto + horario escolar apagado), `pairing`
  (instrucciones; el código no se genera hasta pulsar el botón).
- Estado vacío (esperado, el seed no crea esos datos): `apps` ("Todavía no se sincronizó ninguna app"), `location`
  ("Todavía no hay ubicación reciente"), `geofences` (0 geocercas), `history` ("Todavía no hay eventos"),
  `statistics` (todo en 0 y "sin datos"), `alerts` ("Sin alertas para este filtro"), `silenced` ("No hay ninguna alerta
  silenciada"), `categories` (11 categorías "Sin regla", ninguna app asignada).
- `remote`: solo el estado inactivo ("Todavía no has pedido ver la pantalla"). No se pidió ninguna sesión: requiere que
  una persona acepte el diálogo de captura en un teléfono real.
- `login`: muestra "Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID." en lugar del botón de Google, porque el build de
  esta captura (igual que el de CI) no define esa variable. Es el estado real de ese build, no un error de la captura.
  El login con Google real no se pudo capturar (lo hace una persona).

## Particularidades de las imágenes

- A 1440x900 el sidebar mide `100vh` y tiene scroll interno: las entradas de la parte baja de la navegación
  (Estadísticas en adelante) quedan fuera de la imagen. Es el comportamiento real de la UI actual, no un recorte de la
  captura.
- En las vistas más largas a 1440 (`categories`, `geofences`, `statistics`) el sidebar aparece solo en los primeros 900
  px y debajo se ve el fondo liso: así lo pinta una captura de página completa con un sidebar de alto fijo.
- La captura se hizo con el navegador en 1440x900 y se pasó a 390x844 con `setViewportSize` en la misma página, sin
  recargar; por eso el login a 390 sí es una carga nueva, pero las vistas a 390 vienen de la misma sesión.
- Las fechas, el id del dispositivo y el correo del tutor cambian en cada corrida; al comparar, ignorar esas zonas.

## Cómo se hizo (comandos exactos)

Docker desde Git Bash con `export MSYS_NO_PATHCONV=1`. Primero se detuvo (sin `down`) el `web` y el `backend` del stack
de desarrollo, porque usan los mismos puertos 3000 y 8000.

```bash
docker compose -f compose.yaml -f compose.web.yaml stop web backend
docker compose -f compose.test.yaml build backend migrate api_server
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up -d db redis api_server
timeout 60 sh -c 'until curl -sf http://localhost:8000/api/v1/health/ready; do sleep 1; done'
docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py   # JSON a un archivo temporal fuera del repo

cd frontend
npm ci --no-audit --no-fund
npm run build
node -e "const fs=require('fs');fs.cpSync('public','.next/standalone/public',{recursive:true});fs.cpSync('.next/static','.next/standalone/.next/static',{recursive:true})"
PORT=3000 HOSTNAME=0.0.0.0 node .next/standalone/server.js        # en segundo plano
npx playwright install chromium                                    # ya estaba instalado
node <script-temporal-fuera-del-repo>.cjs                          # ejecutado desde frontend/
```

El script (fuera del repo, descartable) hace: login en un contexto limpio a ambos tamaños; luego **un solo contexto**
con `sessionStorage["netprotect.refresh_token"]` sembrado (el refresh token es de un solo uso), una única carga de
`/`, y para cada vista `window.location.hash = '#section=<clave>&device=<id>'` sin recargar. Antes de capturar espera,
con tope de 12 s, a que el único `h1` muestre el nombre de la sección y a que desaparezcan los textos "Cargando…" y los
marcadores "…" de las tarjetas de métrica (una primera pasada capturó `overview` con "…" todavía en pantalla; se
detectó al revisar la imagen, se endureció la espera y se repitió toda la captura con una sesión nueva).

Al terminar se detuvo el servidor standalone, se hizo `docker compose -f compose.test.yaml down` (sin `-v` sobre
`compose.yaml`) y se levantó de nuevo el stack de desarrollo con
`docker compose -f compose.yaml -f compose.web.yaml up -d web backend`.

## Entorno y límites

- Node v24.14.1 / npm 11.11.0 en esta máquina; CI usa Node 22. Next resolvió 16.3.8 tras `npm ci` (el `node_modules`
  previo era viejo: Next 16.3.3 y sin `lucide-react`). Pequeñas diferencias de render de fuentes entre máquinas son
  posibles; para comparar, repetir las capturas en este mismo entorno.
- La imagen del backend de prueba se construyó en el momento desde el árbol del commit indicado.
- No se capturaron: el login con Google real, la vista remota con una sesión activa, ni estados con datos reales de
  uso, ubicación, geocercas o alertas (el seed no los crea y no se insertaron datos a mano).
- `next build` reescribe `frontend/next-env.d.ts` (rutas `.next/dev` -> `.next`); se revirtió con `git checkout`.

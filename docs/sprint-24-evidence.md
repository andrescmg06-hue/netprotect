# Sprint 24 — Evidencia

## Frontend: lint

Primera corrida, sobre el código antes de corregir — 8 errores reales, todos arreglados en el
código final:

```
$ npm run lint
...
DashboardShell.tsx:71:31  error  Expected the first argument to be an inline function expression   react-hooks/use-memo
DashboardShell.tsx:115:7  error  Calling setState synchronously within an effect can trigger cascading renders   react-hooks/set-state-in-effect
DashboardShell.tsx:194:69 / 194:81  error  `"` can be escaped with `&quot;`...   react/no-unescaped-entities
DevicesPanel.tsx:86:85 / 86:97  error  `"` can be escaped with `&quot;`...   react/no-unescaped-entities
OverviewPanel.tsx:32:66 / 32:78  error  `"` can be escaped with `&quot;`...   react/no-unescaped-entities
✖ 8 problems (8 errors, 0 warnings)
```

Corregidos: `useMemo(() => readHash(), [])`; `activeDeviceId` pasó de estado sincronizado por
efecto a valor derivado en el render (ver `docs/sprint-24.md`); comillas rectas → `&ldquo;`/
`&rdquo;`.

Corrida final, limpia:

```
$ npm run lint
> netprotect-web@0.1.0-sprint1 lint
> eslint . --max-warnings=0
```

Sale sin salida — cero errores, cero warnings.

## Frontend: build

```
$ npm run build
▲ Next.js 16.3.3 (Turbopack)
✓ Compiled successfully in 2.3s
  Running TypeScript ...
  Finished TypeScript in 3.2s ...
✓ Generating static pages using 4 workers (3/3) in 1312ms

Route (app)
┌ ○ /
└ ○ /_not-found

○  (Static)  prerendered as static content
```

TypeScript estricto (`tsc` vía `next build`) y el build de producción pasan limpios tras cada
cambio, incluida la corrección de hidratación de `AuthContext.tsx`.

## Verificación visual end-to-end contra el backend real

Sin login real de Google (exige elegir una cuenta en un selector — ningún agente puede hacerlo),
se verificó el dashboard funcionando de punta a punta así:

1. `docker compose up -d --build db redis migrate backend` — stack de desarrollo real.
   `curl http://localhost:8000/api/v1/health/ready` → `{"status":"ready","backend":"connected","database":"connected","redis":"connected"}`.
2. `npm run dev` (Next.js, Turbopack) sirviendo en `http://localhost:3000`.
3. Un script Python ejecutado dentro del contenedor `backend` (mismo mecanismo que los *fixtures*
   `conftest.py` de las pruebas de integración: usa directamente
   `app.core.security.create_access_token`/`generate_refresh_token` y el modelo `UserSession`, no
   un *bypass* del backend) creó un usuario tutor de prueba y una sesión real (fila en `sessions`
   con el hash correcto del refresh token) — no un login de Google.
4. El resto del flujo es 100 % real contra la API HTTP: `POST /users/me/roles` (TUTOR y
   SUPERVISADO), `POST /pairing/codes` → código de 6 dígitos, `POST /pairing/redeem` → vincula un
   dispositivo de prueba, `POST /devices/{id}/rules` → una regla `BLOCK` sobre
   `com.instagram.android`. Mismos endpoints que usaría la app Android.
5. Playwright (Chromium, instalado sólo en un directorio temporal fuera del repo — no se añadió
   como dependencia del proyecto) navegó a `http://localhost:3000` con el refresh token inyectado
   en `sessionStorage` antes de que cargara cualquier script (`page.addInitScript`), reproduciendo
   exactamente el flujo real de `AuthContext` (`refreshTokens` → `fetchCurrentUser` →
   `"authenticated"`), y capturó cada sección.

Resultado: el dashboard renderiza las 16 secciones agrupadas en el sidebar; el selector de
dispositivo en el encabezado aparece sólo en secciones `perDevice` y persiste el dispositivo
elegido al cambiar de sección; "Reglas por aplicación" muestra la regla de Instagram creada por
API; "Política y horario escolar" muestra sus dos controles ya separados del componente original;
"Vinculación" genera un código real con cuenta regresiva; "Alertas" refleja correctamente el
estado vacío. Capturas guardadas localmente durante la sesión (no versionadas en el repo):
`overview`, `devices`, `pairing`, `rules`, `policy`, `alerts`, y la landing pre-login.

### Hallazgo real encontrado y corregido: mismatch de hidratación

Antes del fix, la consola del navegador (capturada con `page.on("pageerror")`/`page.on("console")`
en el mismo script de Playwright) mostraba:

```
[authed] console/page errors: 2
  - Uncaught Error: Hydration failed because the server rendered text didn't match the client...
  - eval() is not supported in this environment...
```

(el segundo es una limitación del propio entorno headless para el modo debug de React, no del
código del proyecto — persiste después del fix). Después de mover el estado inicial de
`AuthContext` a una constante y trasladar la decisión completa al efecto existente
(`docs/sprint-24.md`), la misma comprobación automatizada:

```
[authed] console/page errors: 1
  - eval() is not supported in this environment...
[landing] console/page errors: 1
  - eval() is not supported in this environment...
```

El overlay de errores de `next dev` (visible en las capturas) pasó de "2 Issues" a "1 Issue" en
ambas páginas, confirmando que el error de hidratación desapareció y no era exclusivo de la página
autenticada.

## Navegación por hash: verificado bidireccional

La primera corrida de la captura (cambiando `window.location.hash` entre secciones) mostró el
mismo contenido de "Resumen" repetido en todas las capturas — el hash cambiaba pero la sección no
reaccionaba, porque no existía un listener de `hashchange`. Corregido añadiéndolo en
`DashboardShell` (ver `docs/sprint-24.md`); la segunda corrida mostró cada sección
(`Dispositivos`, `Vinculación`, `Reglas por aplicación`, `Política y horario escolar`, `Alertas`)
con su propio contenido distinto tras el cambio de hash.

## Limpieza

Los datos de prueba (dos usuarios, un dispositivo, su regla, su sesión) se borraron de la base de
datos de desarrollo al terminar (`DELETE` explícito sobre `Device` primero —
`devices.supervised_user_id` es `RESTRICT`, no `CASCADE`— y luego sobre `User`, que si cascadea).
`docker compose down` (sin `-v`: el volumen `postgres_dev_data` no se tocó) y el servidor `next
dev` se detuvieron al finalizar. Playwright se instaló únicamente en un directorio temporal fuera
del repositorio (no aparece en `package.json`).

## `/security-review`

Corrido sobre el diff completo de la rama (incluye, además de este sprint, el commit ya hecho del
Sprint 23 aún sin *push*, que es el único con código de backend). Sobre el frontend de este sprint:
sin hallazgos. Un hallazgo real, pero en `backend/app/services/realtime.py`/
`backend/app/api/v1/endpoints/realtime.py` (Sprint 23, cero archivos tocados por el Sprint 24):
`ConnectionManager.begin_screen_share` (línea 53-54) sobrescribe `_screen_share_peers[device_id]`
sin comprobar si ya hay una sesión de vista remota anclada a otra conexión de tutor — cualquier
tutor vinculado y autorizado puede reenviar `screen_share_request` en cualquier momento y
redirigir en silencio a quién llegan el consentimiento/oferta/candidatos ICE, incluso a mitad de
una sesión ya en curso, sin que la persona supervisada vea de quién es la solicitud
(`screenShareRequested` en `SupervisedScreen.kt` es un booleano simple, sin identidad del tutor).
Es el mismo tipo de fallo que el propio Sprint 23 ya corrigió una vez (difusión a todos los
tutores conectados en vez de anclar la sesión a quien la pidió, ver `docs/sprint-23-evidence.md`),
pero en el flujo de *re*-solicitud. No se corrige en esta sesión — es alcance del Sprint 23, no
del 24, y requeriría re-verificar la suite de integración de backend en Docker completa. Queda
anotado aquí para corregirse antes de dar el Sprint 23 por cerrado en firme (o al abrir el
siguiente sprint que toque `realtime.py`).

## No ejecutado en esta sesión

- CI en GitHub Actions (job `frontend`) contra este diff, en un runner limpio.
- `ruff check` / pruebas de backend: no aplica, cero archivos de `backend/` tocados en este
  sprint.

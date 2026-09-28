# Sprint 39 — Evidencia

Salidas reales de los comandos ejecutados. Los fallos del camino están marcados y explicados; nada
de lo que aquí se da por bueno es una salida inventada.

## 1. `npm run lint`

```text
> netprotect-web@0.1.0-sprint1 lint
> eslint . --max-warnings=0

(exit 0 — 0 errores, 0 warnings)
```

## 2. `npm run build`

```text
> netprotect-web@0.1.0-sprint1 build
> next build

▲ Next.js 16.3.3 (Turbopack)
- Environments: .env.local
✓ Running next.config.ts took 25ms

  Creating an optimized production build ...
✓ Compiled successfully in 1156ms
  Running TypeScript ...
  Finished TypeScript in 3.8s ...
  Collecting page data using 6 workers ...
  Generating static pages using 6 workers (5/5) ...
✓ Generating static pages using 6 workers (5/5) in 542ms
  Finalizing page optimization ...

Route (app)
┌ ○ /
├ ○ /_not-found
├ ○ /design-system
└ ○ /icon.png

○  (Static)  prerendered as static content
```

`/login-mockup` ya no aparece en el listado de rutas.

## 3. Comparación visual (pixelmatch, `threshold: 0.1`)

Línea base capturada en `npm run dev` contra `/login-mockup` (antes de tocar nada) y comparada contra
`/` después del cambio, mismos 7 viewports y condiciones (`networkidle` + `document.fonts.ready`,
botón de Google habilitado en ambos). PNG guardados fuera del repo, en `%TEMP%\sprint39-baseline\`
y `%TEMP%\sprint39-compare\`.

### 3a. Tal como queda (Nunito, D6)

```text
1672x941:   21047/1573352 = 1.3377%
1440x900:   17067/1296000 = 1.3169%
1920x1080:  24123/2073600 = 1.1633%
1280x720:   14617/921600  = 1.5860%
1024x768:   11930/786432  = 1.5170%
768x1024:   11697/786432  = 1.4874%
390x844:    11436/329160  = 3.4743%
```

Supera el umbral. **Causa única: la fuente.** El mockup renderiza Inter (su Nunito está roto, ver
`sprint-39.md` §"Conflicto D6"), y la portación aplica Nunito (D6). No es un defecto de layout.

### 3b. Con la fuente normalizada a Inter (mismas condiciones que el mockup)

Se re-capturó `/` inyectando `font-family: Inter …` para aislar cualquier diferencia ajena a la
fuente:

```text
1672x941:   271/1573352 = 0.0172%
1440x900:   271/1296000 = 0.0209%
1920x1080:  272/2073600 = 0.0131%
1280x720:   273/921600  = 0.0296%
1024x768:   270/786432  = 0.0343%
768x1024:   274/786432  = 0.0348%
390x844:    270/329160  = 0.0820%
```

Todos muy por debajo del umbral (`≤0.005` en 1672/1440/1920; `≤0.01` en el resto). **La portación es
pixel a pixel idéntica al mockup salvo por la fuente (D6 vs Inter).**

Los ~270 píxeles restantes (~0.02–0.08%) son diferencias sub-píxel/antialiasing, no un defecto
apreciable.

### Comparación de `getComputedStyle` (Paso 0)

Posiciones/tamaños/colores de `h1`, `h2`, botón y tarjeta coinciden con la línea base en los 7
viewports, **salvo** `font-family` (Inter→Nunito) y los anchos/posiciones del texto que cambian con
la métrica de la fuente (p. ej. `h1` "Inicia sesión" centrado, más estrecho en Nunito). En el pase
normalizado a Inter, hasta `width`/`left` vuelven a coincidir. `scrollWidth == innerWidth` en los 7
viewports (sin desborde horizontal; a 390×844 ambos son 390).

## 4. Sin desborde horizontal a 390×844

```text
1672x941: scrollWidth=1672 innerWidth=1672 (overflow=false)
1440x900: scrollWidth=1440 innerWidth=1440 (overflow=false)
1920x1080: scrollWidth=1920 innerWidth=1920 (overflow=false)
1280x720: scrollWidth=1280 innerWidth=1280 (overflow=false)
1024x768: scrollWidth=1024 innerWidth=1024 (overflow=false)
768x1024: scrollWidth=768 innerWidth=768 (overflow=false)
390x844: scrollWidth=390 innerWidth=390 (overflow=false)
```

## 5. Estados de la tarjeta (§6.6)

- **"Comprobando sesión…" (loading)**: capturado sembrando un `refresh_token` en `sessionStorage` y
  colgando `**/api/v1/auth/refresh` para mantener `authStatus === "loading"`. El `Spinner` con
  `aria-live="polite"` se muestra en la tarjeta. (`%TEMP%\sprint39-states\loading.png`)
- **"Conectando…"**: capturado haciendo clic en el botón y capturando en pleno vuelo; el DOM
  contenía "Conectando…". (`%TEMP%\sprint39-states\connecting.png`)
- **"Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID"**: **no capturado**. La rama
  (`if (!GOOGLE_CLIENT_ID) return <p role="alert">…</p>`) está implementada y compila, pero en este
  entorno `NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID` está definido en `.env.local` (que no puedo leer ni
  editar), y en Windows/Node una variable de entorno puesta a cadena vacía se ve como *undefined*,
  así que no pude forzar la rama. Queda documentado como **no verificado en captura**.

## 6. Script `gsi/client` carga y el botón se habilita (§6.7)

Sobre `/` con el client id presente:

```json
{
  "windowGoogleLoaded": true,
  "buttonText": "Continuar con Google",
  "buttonDisabled": false,
  "buttonOpacity": "1"
}
```

`window.google.accounts.id` está disponible y el botón queda habilitado.

## 7. `npm uninstall tailwindcss @tailwindcss/postcss react-icons`

```text
npm warn cleanup Failed to remove some directories [
npm warn cleanup   [ '…\node_modules\@tailwindcss\.oxide-win32-x64-msvc-…\tailwindcss-oxide.win32-x64-msvc.node',
npm warn cleanup     [Error: EPERM: operation not permitted, unlink …] ] ]
…
removed 14 packages, and audited 349 packages in 1s
found 0 vulnerabilities
```

Los `EPERM` son el `next dev` aún en marcha reteniendo los `.node` nativos de Tailwind/lightningcss;
los paquetes sí se eliminaron de `package.json`/`package-lock.json`. `package.json` final solo
depende de `lucide-react`, `next`, `react`, `react-dom` (+ devDeps de lint/tipos/Playwright).

`grep -rniE "tailwind|login-mockup|FcGoogle" frontend --include=* --exclude-dir=node_modules --exclude-dir=.next`
no devuelve nada fuera de `docs/` (verificado; el único resto son menciones en este sprint y en
`docs/sprint-38-cierre.md`/`docs/progress.md` históricos).

## 8. Fallos del camino (reales)

1. **`pixelmatch` ESM**: la primera ejecución de `compare.js` falló con
   `TypeError: pixelmatch is not a function` (v7 exporta `default`). Corregido importando `.default`.
2. **Doble `next dev`**: intenté un segundo `next dev --port 3001` para capturar el estado sin
   client id; Next 16 lo rechazó ("Another next dev server is already running"). Abandonado.
3. **Override de env a vacío**: `$env:NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID=""` no produce una variable
   vacía en el proceso Node (se ve `undefined`), así que `.env.local` siguió ganando y no pude
   capturar el estado "Falta configurar" (ver §5).
4. **Layout below-lg (ya corregido)**: la primera comparación daba 3–5% en 768×1024 y 390×844
   incluso normalizando a Inter. La causa era el subtítulo del hero a 16px (la guía decía
   "text-base = 16px"), cuando el mockup lo renderiza a **14px** (`--text-base` pisado por
   `globals.css`). Corregido a `14px`; tras eso la diferencia normalizada bajó a ≤0,082%.

## 9. E2E con sesión sembrada (§6.5) — PENDIENTE

**No ejecutado.** Requiere levantar `compose.test.yaml` (backend de pruebas publicando el puerto
8000, que hoy ocupa el backend de desarrollo en marcha), correr `migrate`, sembrar con
`backend/scripts/seed_test_session.py`, servir el build standalone y ejecutar Playwright. No levanté
ese stack, así que el E2E queda **pendiente**. Nota: el E2E (`frontend/e2e/dashboard.spec.ts`) prueba
el **dashboard autenticado** (la rama `if (authStatus === "authenticated")` de `page.tsx`, que no
tocó este sprint), no la pantalla de login nueva.

## 10. Login real con Google — PENDIENTE (por naturaleza)

Exige una persona y un cliente OAuth real. Verificado solo que el script `gsi/client` carga y el
botón se habilita (§6).

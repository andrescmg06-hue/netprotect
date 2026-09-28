# Sprint 39 — El login del mockup reemplaza al login actual de `/`

> **Para: DeepSeek (OpenCode).** Trabajo delegado por Claude. Léelo entero antes de tocar nada.
> Al empezar, mueve este archivo a `docs/delegated/active/`; al terminar, a `docs/delegated/done/`.

## 1. Objetivo

La pantalla de inicio de sesión que se ve en `/` cuando no hay sesión debe quedar **visualmente
idéntica** a la que hoy existe aislada en `/login-mockup`. Cuando termines:

- `/` (sin sesión) muestra la pantalla del mockup: fondo fotográfico, título "Panel del tutor",
  imagen de laptop + teléfono, tarjeta blanca "Inicia sesión" con el botón "Continuar con Google" y
  el pie con Ayuda / Privacidad / Términos.
- `/` (con sesión) sigue mostrando `DashboardShell`, sin cambios.
- La ruta `/login-mockup` **ya no existe**.
- Es fiel al mockup: **el criterio de aceptación es la comparación visual del §6**, no tu impresión.

Es una tarea de UI. **No cambia** rutas de API, contratos de `apiClient.ts`, `AuthContext`, backend
ni móvil.

## 2. Decisiones ya tomadas (no las reabras)

| # | Decisión | Motivo |
|---|---|---|
| D1 | **Se porta a CSS Modules y se elimina Tailwind del proyecto.** | Todo el resto del panel usa CSS Modules sobre los tokens de `globals.css` (`.claude/rules/frontend.md`: "Sin Tailwind"). Tailwind solo existe por este mockup. Todos los valores del mockup son explícitos (`vw`, `vh`, `clamp()`, px), así que la traducción es mecánica. |
| D2 | **Se quita del login la tarjeta de infraestructura** (5 servicios, "Volver a comprobar") y con ella el `fetch` a `/api/v1/health/ready` de `app/page.tsx`. | El mockup no tiene dónde ponerla, y dejar el estado sin usar sería código muerto. El endpoint del backend no se toca. Se documenta en `sprint-39.md` como decisión, no como olvido. |
| D3 | **Se conservan** la imagen del showcase con sus datos de ejemplo ("Dispositivo de Sofía"…) y los enlaces `href="#"` del pie. | Decisión explícita del dueño del proyecto para esta pantalla. La imagen es decorativa (`aria-hidden`, `alt=""`); no se presenta como datos reales. Documentarlo. |
| D4 | **Se conserva el estado "Comprobando sesión…"** (Spinner dentro de la tarjeta mientras `authStatus === "loading"`), que hoy hace `LoginPanel`. | El mockup lo ignora, pero quitarlo haría parpadear el botón antes de saber si hay sesión. Es comportamiento, no aspecto. |
| D5 | **Un solo botón de Google**: el del mockup (`GoogleButton`, con `google.accounts.id.prompt()` y *fallback* al botón oficial). Reemplaza a `GoogleSignInButton`. | Dos componentes para lo mismo, con declaraciones de `Window.google` que ya tuvieron que mantenerse separadas para no chocar. |
| D6 | **Nunito solo para esta pantalla**; el resto sigue en Inter. | Es lo que hace el mockup hoy. |
| D7 | Logo del login = **el SVG en línea del mockup**, no `ui/Logo`. | Es lo que se ve en el mockup. `ui/Logo` (PNG) sigue usándose en el resto del panel; no lo toques. |

## 3. Lo que NO debes tocar

`backend/`, `mobile/`, `infra/`, `secrets/`, `.env*`, `.claude/`, `frontend/AGENTS.md`,
`src/contexts/AuthContext.tsx`, `src/lib/apiClient.ts`, `src/components/DashboardShell*`,
`src/components/ui/*`, los demás paneles, `e2e/*` (el E2E actual solo prueba el dashboard con sesión;
no cambia). **Sin `git commit` ni `git push`**: deja todo en el árbol de trabajo, Claude lo revisa.
Sin dependencias nuevas.

Reglas del proyecto que aplican (`CLAUDE.md` y `.claude/rules/frontend.md`):
- Lee `frontend/AGENTS.md` y consulta `node_modules/next/dist/docs/` antes de usar una API de Next.
- ESLint de Next 16: `react-hooks/set-state-in-effect`, `react-hooks/refs`, `react-hooks/immutability`
  están activas (ver la nota en `.claude/rules/frontend.md`). No leas `ref.current` durante el render.
- Git siempre con `GIT_OPTIONAL_LOCKS=0`. En Windows, `export MSYS_NO_PATHCONV=1` antes de `docker`.
- Nunca leas ni edites `.env` ni `secrets/`.
- **Nada se da por terminado sin evidencia real de que se ejecutó.** Lo que no puedas probar lo
  declaras pendiente. No inventes salidas de comandos.

## 4. Punto de partida (léelo antes de escribir)

Mockup (lo que hay que replicar): `frontend/src/app/login-mockup/`
- `page.tsx` (composición y posiciones a `lg:`), `layout.tsx` (Nunito), `tailwind.css` (tokens)
- `components/`: `BackgroundScene`, `HeroShowcase`, `LoginCard`, `GoogleButton`, `Logo`, `Footer`
- Imágenes: `frontend/public/login-mockup/{background-desk-clean.png, devices-showcase.png}`
  (`background-desk.png` es la fuente sin procesar y no se usa).

Login actual (lo que se reemplaza): `frontend/src/app/page.tsx`, `page.module.css`,
`src/components/{LoginHero,LoginPanel,GoogleSignInButton}.tsx` y sus `.module.css`.

`globals.css` pinta un degradado opaco en `<body>`; por eso `BackgroundScene` va primero en el DOM y
es `absolute` (no `fixed`, sin z-index). Respeta ese orden de apilado.

## 5. Pasos

### Paso 0 — Línea base visual (ANTES de cambiar nada)

1. Levanta el frontend (`cd frontend && npm run dev`, puerto 3000; si hay que compilar, ver la
   trampa de `EBUSY .next/standalone` en `.claude/rules/frontend.md`).
2. Con Playwright (ya instalado; `npx playwright install chromium` si falta el navegador) captura
   `http://localhost:3000/login-mockup` **sin sesión** en estos viewports, `fullPage: false`,
   esperando `networkidle` y la fuente (`document.fonts.ready`):
   `1672×941` (medida original), `1440×900`, `1920×1080`, `1280×720`, `1024×768`, `768×1024`, `390×844`.
3. Guarda los PNG en la carpeta temporal del sistema (p. ej. `%TEMP%\sprint39-baseline\`), **no en
   el repo**. Guarda también, por viewport, `getComputedStyle` (posición/tamaño/`font-size`/`color`)
   del `h1`, del `h2`, del botón de Google y del contenedor de la tarjeta: sirve para diagnosticar
   diferencias que el ojo no ve.
4. El botón de Google puede verse deshabilitado hasta que cargue `accounts.google.com/gsi/client`;
   haz las capturas de línea base y de comparación en las mismas condiciones de red.

### Paso 1 — Estructura nueva

Crea `frontend/src/components/login/` (todo `"use client"` solo donde haga falta):

| Archivo | Contenido |
|---|---|
| `LoginScreen.tsx` + `LoginScreen.module.css` | Reemplaza a `login-mockup/page.tsx`: `<main>` con la composición y las posiciones `lg:`. Recibe `authStatus: "loading" \| "unauthenticated"` (D4). Aquí se aplica Nunito (D6). **Sin** el `useEffect`/`router.replace` de `login-mockup/page.tsx`: `app/page.tsx` ya renderiza `DashboardShell` cuando hay sesión. |
| `LoginCard.tsx` + `.module.css` | La tarjeta. Si `authStatus === "loading"`, en lugar del botón va `<Spinner label="Comprobando sesión…" />` (de `@/components/ui`) dentro de un contenedor `aria-live="polite"`, con la misma altura que el botón para que no salte el layout. |
| `GoogleButton.tsx` + `.module.css` | Port de `login-mockup/components/GoogleButton.tsx`. Sustituye `FcGoogle` de `react-icons` por el SVG oficial de 4 colores de la "G" de Google, en línea. Mantén `role="alert"` en los mensajes de error y la lógica de `prompt()` + *fallback* tal cual. Este archivo es ahora el **único** que declara/usa `window.google`. |
| `LoginLogo.tsx` + `.module.css` | Port de `login-mockup/components/Logo.tsx` (SVG en línea con el `linearGradient`; `id` único). |
| `HeroShowcase.tsx`, `BackgroundScene.tsx`, `LoginFooter.tsx` (+ módulos CSS) | Ports directos de los componentes homónimos. |
| `loginFont.ts` | `Nunito` con `next/font/google` (`subsets: ["latin"]`, pesos 400/600/700/800, `variable: "--font-nunito"`, `display: "swap"`). |

Y en `app/page.tsx`: quita `fetchReadiness`, `ReadyPayload`, `ApiState`, los `useState`/`useEffect`
de la comprobación (D2) y devuelve `<LoginScreen authStatus={...} />` cuando no hay sesión.
Deja intacta la rama autenticada. Borra `page.module.css` si queda sin uso.

Imágenes: `git mv frontend/public/login-mockup/background-desk-clean.png` y `devices-showcase.png` a
`frontend/public/login/`; actualiza las rutas (`/login/...`). Borra `background-desk.png` (sigue en el
historial de git). Comprueba que `next/image` sirve el showcase en el build `standalone` (mira la
petición de red; si el optimizador falla en standalone, usa `unoptimized` como hace `ui/Logo`).

### Paso 2 — Reglas de la traducción Tailwind → CSS Modules

- **Fidelidad antes que pureza**: conserva exactamente las unidades del mockup (`vw`, `vh`,
  `clamp(...)`, `min(...)`). No las "redondees" a tokens.
- Breakpoints de Tailwind v4: `sm` = 640px, `lg` = 1024px, mobile-first (`@media (min-width: 1024px)`).
  Escala por defecto: 1 unidad = 0.25rem (`px-4` = 1rem, `py-10` = 2.5rem, `mt-3` = 0.75rem, `gap-2` = 0.5rem);
  `rounded-xl` = 12px; `font-extrabold` = 800, `font-semibold` = 600; `leading-tight` = 1.25,
  `leading-relaxed` = 1.625; `text-base` = 16px, `text-sm` = 14px (interlineado 1.25rem).
- **Cuidado con los colores de paleta de Tailwind v4** (p. ej. `text-red-600` es `oklch`, no el
  hex clásico): para cualquier color que no sea un hex literal en el código, lee el valor calculado
  en el navegador sobre `/login-mockup` (`getComputedStyle`) y usa ese.
- Los colores que ya son tokens del proyecto (`--color-navy`, `--color-primary`, …) usan el token; el
  resto de colores literales del mockup (`#0B1F5C`, `#5B6B8C`, `#D8DEE9`, `#1B2F63`, `#6B80B0`…) se
  declaran **como variables locales en `.screen { --login-...: ... }`** dentro de
  `LoginScreen.module.css`, no en `globals.css`. Nada de hex sueltos repetidos por los módulos.
- Sin `preflight` en el mockup: `<h1>`, `<h2>`, `<p>` mantenían el margen del navegador salvo `mt-0/mb-0`
  explícitos. En el módulo, fija cada margen a mano según el mockup; **no confíes solo en leer las
  clases: mide** (Paso 0, `getComputedStyle`).
- Estados que deben sobrevivir: `hover`, `focus-visible` (outline 2px, offset 2px, color primario),
  `disabled` (opacidad .6, cursor `not-allowed`), `hover:underline` en los enlaces del pie.
- `prefers-reduced-motion` ya lo anula `globals.css`; no añadas animaciones nuevas.

### Paso 3 — Retirar Tailwind y lo viejo

1. Borra `frontend/src/app/login-mockup/` entera.
2. Borra `LoginHero.tsx/.module.css`, `LoginPanel.tsx/.module.css`, `GoogleSignInButton.tsx/.module.css`
   (antes: `grep -rn` para confirmar que nada más los importa; el `error` de `GoogleSignInButton.module.css`
   ya no tiene consumidores).
3. Borra `frontend/postcss.config.mjs`.
4. `cd frontend && npm uninstall tailwindcss @tailwindcss/postcss`; y `react-icons` **solo si**
   `grep -rn "react-icons" src` ya no devuelve nada. Deja `package-lock.json` consistente.
5. `grep -rniE "tailwind|login-mockup|FcGoogle" frontend --include=* --exclude-dir=node_modules --exclude-dir=.next`
   no debe devolver nada fuera de `docs/`.

### Paso 4 — Documentación (en el mismo cambio)

- `docs/sprint-39.md`: qué se hizo y **por qué**, incluidas D1–D7 y qué del login anterior se eliminó (D2).
- `docs/sprint-39-evidence.md`: comandos reales con su salida real, **incluidos los fallos del camino**.
- `docs/progress.md`: Sprint actual 39; quita "Problemas conocidos → `/login-mockup`" y el "Siguiente paso"
  ya resuelto.
- `README.md`: sección `## Alcance del Sprint 39`.
- `.claude/rules/frontend.md` **no lo edites** (`.claude/` es de Claude); anota en `sprint-39.md` la línea
  "Sin Tailwind (salvo `/login-mockup`)" como pendiente de actualizar por Claude.

## 6. Verificación (criterio de "terminado")

Ejecuta y pega la salida real en `docs/sprint-39-evidence.md`:

1. `cd frontend && npm run lint` → 0 errores, 0 warnings (`--max-warnings=0`).
2. `cd frontend && npm run build` → verde (mata antes cualquier `node .next/standalone/server.js` vivo).
3. **Comparación visual**: mismas capturas del Paso 0 sobre `http://localhost:3000/` sin sesión, mismos
   viewports y condiciones. Calcula la diferencia de píxeles contra la línea base (p. ej. `pixelmatch`/
   `pngjs` instalados **fuera** del proyecto o `expect(page).toHaveScreenshot` con un `maxDiffPixelRatio`
   ≤ 0.005 en `1672×941`, `1440×900`, `1920×1080`; en los demás, revisión visual con margen ≤ 0.01).
   Reporta el porcentaje real por viewport. Si algún viewport supera el umbral, **corrige y repite**;
   no lo maquilles subiendo el umbral. Compara también los `getComputedStyle` del Paso 0.
4. Sin desborde horizontal a `390×844` (`document.documentElement.scrollWidth <= innerWidth`).
5. Con un `refresh_token` sembrado (ver `e2e/`) `/` sigue llevando al dashboard: ejecuta el E2E real
   (`verificar` skill → área frontend/integración, `compose.test.yaml`). Si no puedes levantar el stack,
   declara el E2E **pendiente** en el informe; no lo des por bueno.
6. Estados: `Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID` (sin la variable), "Comprobando sesión…"
   (`loading`) y "Conectando…". Captura cada uno.
7. **Pendiente por naturaleza, decláralo así**: el inicio de sesión real con Google (requiere una persona
   y un cliente OAuth real). Verifica al menos que el script `gsi/client` carga y que el botón se habilita.

## 7. Informe final (en tu último mensaje)

1. Lista de archivos creados / movidos / borrados.
2. Salidas reales de lint, build, E2E y del porcentaje de diferencia visual por viewport.
3. Qué quedó pendiente o no pudiste probar, sin adornos.
4. Cualquier decisión que tuviste que tomar fuera de D1–D7 y por qué.

Claude hará después la revisión con `verifier` y `security-reviewer` (se tocó la pantalla de auth);
no las simules tú.

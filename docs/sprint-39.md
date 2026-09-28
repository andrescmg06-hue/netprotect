# Sprint 39 — El login del mockup reemplaza al login actual de `/`

La pantalla de inicio de sesión que se ve en `/` sin sesión pasa a ser una réplica **pixel a pixel**
de la que estaba aislada en `/login-mockup`. La ruta `/login-mockup` desaparece y, con ella, todo
Tailwind y las dependencias que solo existían por ella.

## Qué se hizo

- **Nueva estructura `frontend/src/components/login/`** (CSS Modules, sin Tailwind):
  - `LoginScreen.tsx` + `.module.css` — `<main>` con la composición y posiciones `lg:` del mockup.
    Recibe `authStatus: "loading" | "unauthenticated"` (D4). Aquí se aplica Nunito (D6).
  - `LoginCard.tsx` + `.module.css` — la tarjeta "Inicia sesión"; en `loading` muestra
    `<Spinner label="Comprobando sesión…" />` dentro de un contenedor `aria-live="polite"` con la
    misma altura que el botón (D4).
  - `GoogleButton.tsx` + `.module.css` — port del botón del mockup; sustituye `FcGoogle` (react-icons)
    por el SVG oficial de 4 colores de la "G" en línea (D5). Es ahora el **único** archivo que
    declara/usa `window.google`.
  - `LoginLogo.tsx`, `HeroShowcase.tsx`, `BackgroundScene.tsx`, `LoginFooter.tsx` (+ módulos) —
    ports directos de los componentes homónimos del mockup (D3, D7).
  - `loginFont.ts` — `Nunito` con `next/font/google` (`subsets: ["latin"]`, pesos 400/600/700/800,
    `variable: "--font-nunito"`, `display: "swap"`).
- **`app/page.tsx`** vuelve a ser solo el *auth gate*: sin `fetchReadiness`/`ReadyPayload`/`ApiState`
  ni los `useState`/`useEffect` de la comprobación de infraestructura (D2). La rama autenticada
  (`DashboardShell`) queda **intacta**; la no autenticada devuelve `<LoginScreen authStatus={…} />`.
- **Imágenes** movidas con `git mv` a `frontend/public/login/` (`background-desk-clean.png`,
  `devices-showcase.png`); `background-desk.png` (fuente sin procesar, sin uso) se borra.
- **Retirada de Tailwind y de lo viejo**: se borra `login-mockup/` entero, `LoginHero`,
  `LoginPanel`, `GoogleSignInButton` (con sus `.module.css`), `app/page.module.css` y
  `postcss.config.mjs`; se desinstalan `tailwindcss`, `@tailwindcss/postcss` y `react-icons`
  (`grep -rn "react-icons" src` ya no devolvía nada).

## Decisiones (D1–D7) y qué se eliminó del login anterior

| # | Decisión | Cómo se reflejó |
|---|---|---|
| D1 | CSS Modules, fuera Tailwind | Todo el login es ahora CSS Modules; Tailwind y PostCSS eliminados. |
| D2 | Fuera la tarjeta de infraestructura y su `fetch` a `/api/v1/health/ready` | Eliminados `LoginHero` (5 servicios, "Volver a comprobar") y el `fetch`/estado de `page.tsx`. El endpoint del backend **no se toca**. |
| D3 | Se conservan la imagen del showcase con sus datos de ejemplo y los `href="#"` del pie | `HeroShowcase` y `LoginFooter` son ports directos; la imagen es decorativa (`aria-hidden`, `alt=""`). |
| D4 | Se conserva "Comprobando sesión…" | `LoginCard` muestra el `Spinner` mientras `authStatus === "loading"`. |
| D5 | Un solo botón de Google (el del mockup, con `prompt()` + *fallback*) | `GoogleButton` reemplaza a `GoogleSignInButton`; `window.google` solo se declara aquí. |
| D6 | Nunito solo para esta pantalla | `loginFont.ts` + `font-family: var(--font-nunito)` en `.screen`. |
| D7 | Logo = SVG en línea del mockup | `LoginLogo` (SVG con `linearGradient`, `id` único `login-shield`); `ui/Logo` (PNG) sigue intacto. |

## Hallazgos de fidelidad (mide, no leas las clases)

El plan pedía **medir** con `getComputedStyle` (Paso 0) y no fiarse solo de las clases. La medición
reveló que el mockup renderizaba **distinto de lo que dicen sus clases**, porque `globals.css` es
CSS **sin capa** (unlayered) y pisa las utilidades de Tailwind (que sí van en `@layer utilities`).
La portación reproduce el **resultado medido**, no la intención de las clases:

- **h1/h2**: `globals.css` (`h1,h2,h3,h4 { color: var(--color-navy); line-height: 1.2 }`) pisa el
  `text-[#0B1F5C]` y el `leading-tight`/`leading-[1.05]` del mockup → los títulos salen **navy
  (#102b63)** y con `line-height: 1.2`, no `#0B1F5C`.
- **Botón de Google**: el reset `:where(button)` (unlayered) pisa `border`, `bg-white`,
  `text-[clamp(…)]` y `font-semibold` → el botón renderiza **sin borde, fondo transparente** y con
  el texto heredado del `body` (14px / 400 / `--color-text` #1B2B4B). Se conservan `height`,
  `border-radius`, `box-shadow`, `gap` y el layout flex.
- **Enlaces del pie**: `globals.css` (`a { color: var(--color-primary-text) }`) pisa el
  `text-[#5B6B8C]` → los enlaces "Ayuda / Privacidad / Términos" salen **azules (#1f5fe6)**.
- **`text-base` / `text-sm`**: las utilidades de Tailwind v4 son `font-size: var(--text-*)`, y
  `globals.css` re-declara `--text-base: 14px` y `--text-sm: 13px` (unlayered). Por eso el subtítulo
  del hero (`text-base`) mide **14px** (no 16px) y los mensajes del botón (`text-sm`) **13px** (no
  14px). La guía del plan ("text-base = 16px") describía la intención de Tailwind, no el render real.

## Conflicto detectado entre D6 y la realidad (no resuelto por mí)

**D6** dice "Nunito solo para esta pantalla … es lo que hace el mockup hoy". Medido, **el mockup
renderiza Inter, no Nunito**: su `tailwind.css` define `--font-display: var(--font-nunito), …` en
`@theme` (que se emite en `:root`), pero `--font-nunito` lo define `next/font` en el *wrapper* de la
ruta, no en `:root`. Al resolverse `var(--font-nunito)` en `:root` queda inválido, `--font-display`
queda *guaranteed-invalid*, y `font-family: var(--font-display)` cae al valor heredado (Inter). La
portación aplica Nunito **correctamente** (D6), así que renderiza Nunito donde el mockup renderizaba
Inter. Consecuencia: la comparación de píxeles difiere **solo en la fuente** (ver evidencia en
`sprint-39-evidence.md`); normalizando la fuente a Inter la diferencia cae a ≤0,082% en todos los
viewports. **No reabrí D6**: lo dejo documentado para que Claude decida si fija la fuente del mockup
(Nunito) o si el login debe seguir al render real (Inter).

## Pendiente de Claude (no lo toco: `.claude/` es de Claude)

`.claude/rules/frontend.md` línea 12 dice aún **"Sin Tailwind (salvo `/login-mockup`)"**. Tras este
sprint ya no existe la excepción: la regla correcta es "Sin Tailwind". Anotado aquí por no poder
editar `.claude/`.

## Verificación

Detalle y salidas reales (incluidos los fallos del camino) en `docs/sprint-39-evidence.md`.
Resumen: `npm run lint` (0 errores, 0 warnings), `npm run build` verde, comparación de píxeles
normalizada a Inter ≤0,082% en los 7 viewports, sin desborde horizontal a 390×844, y el script
`gsi/client` carga y habilita el botón. **Pendiente por naturaleza**: el inicio de sesión real con
Google (exige una persona y un cliente OAuth real) y el E2E con sesión sembrada (no levanté
`compose.test.yaml`; ver evidencia).

## Resolución de Claude (revisión del Sprint 39)

- **D6 revisada: el login usa Inter, no Nunito.** El dueño pidió que quede *como el mockup*, y el mockup
  que aprobó visualmente renderizaba Inter (su Nunito nunca se aplicó). Se borró `loginFont.ts` y el
  `font-family` propio de `.screen`; el login hereda la fuente del `body`, igual que el resto del panel
  (una fuente menos que descargar). Con eso la diferencia contra la línea base es la medida en la
  evidencia §3b (≤0,082 %), no la de §3a.
- `.claude/rules/frontend.md`: "Sin Tailwind (salvo `/login-mockup`)" pasa a "Sin Tailwind".
- Verificación de Claude: `tsc --noEmit` y `npm run lint` limpios tras el cambio. No se repitió la
  comparación de píxeles ni el E2E.

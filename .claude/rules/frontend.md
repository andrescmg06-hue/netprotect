---
paths:
  - "frontend/**"
---

# Frontend (Next.js 16)

- **El aspecto sigue a los mockups; la funcionalidad sigue al backend.** Si un mockup muestra algo
  que la API no tiene, se omite o se usa el dato real equivalente. Nunca se simula.
- Tareas de UI no cambian rutas de API ni lógica de negocio.
- Estilos: CSS Module por componente sobre los tokens de `src/app/globals.css`; primitivas desde
  `@/components/ui`. Nada de colores, radios ni espaciados a mano. Sin Tailwind.
- Sin librerías de gráficos ni de mapas: `DonutChart`, `BarList`, `ProgressBar`, `Timeline` y
  `GeofenceMap` son propios. El mapa es un esquema **a propósito** (no enviar coordenadas de un menor
  a servidores de teselas).
- `DashboardShell` pinta el `PageHeader`; los paneles no.
- Antes de usar una API de Next, consultar `node_modules/next/dist/docs/` (ver `frontend/AGENTS.md`).
- Verificar con `npm run lint && npm run build` (vía `verifier`).

---

- Frontend: Next.js App Router, componentes cliente (`"use client"`). Ver `src/contexts/AuthContext.tsx`
  para el patrón de sesión actual (en memoria + `sessionStorage`, no cookie `HttpOnly` — decisión
  documentada y deliberadamente pospuesta en `docs/sprint-03.md`). Estilos: CSS Modules sobre los
  tokens de `globals.css` y las primitivas de `src/components/ui/` (ver la nota de los Sprints
  31–38); nada de colores, radios ni espaciados escritos a mano fuera de los tokens.

**Nota de los Sprints 31–38 (rediseño del panel web), válida para cualquier cambio futuro en
`frontend/`**: plan en `docs/planning/plan-frontend.md`, un `docs/sprint-NN.md` por sprint y
`docs/sprint-38-cierre.md`. La regla que lo gobierna: **el aspecto sigue a los mockups, la
funcionalidad sigue al backend** — si un mockup muestra algo que la API no tiene, se omite o se
reemplaza por el dato real equivalente, nunca se simula. Ninguna ruta de API ni lógica de negocio
cambió en estos ocho sprints. Cómo está armado ahora: tokens de diseño en
`src/app/globals.css` (`:root`; los colores `*-text` son los que pasan AA como texto — los de
relleno como `--color-primary` no), un CSS Module por componente (sin Tailwind), primitivas en
`src/components/ui/` (importar siempre desde el barrel `@/components/ui`), `lucide-react` como
única librería de iconos, y **ninguna librería de gráficos ni de mapas**: `DonutChart`,
`BarList`, `ProgressBar`, `Timeline` y el esquema de geocercas (`GeofenceMap`) son SVG/HTML
propios — el mapa de geocercas es un esquema a escala a propósito, para no mandar coordenadas de
un menor a un servidor de teselas externo. `DashboardShell` renderiza el `PageHeader` de cada
sección; los paneles **no** deben renderizar el suyo. `globals.css` ya no tiene estilos heredados
de los Sprints 1–24 ni el antiguo "look" de `<button>`: su `:where(button)` es un reset neutro, así
que un botón nativo nuevo necesita su propia clase. Lecciones concretas de estos sprints:
`react-hooks/refs` prohíbe leer `ref.current` durante el render (usar estado), y
`react-hooks/immutability` prohíbe reasignar una variable local dentro del `map()` del JSX
(precalcular con `reduce` antes del `return`, ver `DonutChart`); una barra que se anima debe usar
`transform: scaleX`, no `width` (el detector de Impeccable lo marca como *layout thrash*); el
`<video>` de `RemoteViewPanel` debe quedar **siempre montado** — el evento `track` de WebRTC
asigna `srcObject` al nodo que `videoRef` ya apunta, y montarlo sólo en el estado "streaming"
pierde el stream; y `next build` en Windows falla con `EBUSY ... .next/standalone` si queda vivo un
`node .next/standalone/server.js` de una verificación anterior (matarlo antes de reconstruir).
Para verificar una vista con datos reales: levantar `compose.test.yaml` (con `migrate` primero),
sembrar con `backend/scripts/seed_test_session.py` + llamadas HTTP reales, servir el build
standalone en `localhost:3000` (el `CORS_ORIGINS` de test es exactamente ese origen) y detener
antes `backend`/`web` de `compose.yaml`, que ocupan el mismo puerto 8000. El *refresh token* de
la sesión sembrada es de un solo uso: cada `page.goto` de Playwright que recarga la app lo rota.

- La regla de ESLint `react-hooks/set-state-in-effect` (la trae Next 16) rechaza que un `useEffect`
  invoque, directa o indirectamente, cualquier función que llame a `setState` — incluso una función
  `async` donde el `setState` ocurre después de un `await`. La forma que sí acepta: encadenar
  `.then()/.catch()` directamente en el cuerpo del efecto (con una bandera `cancelled` si hace falta
  cancelar), de modo que cada `setState` quede dentro de un callback de promesa ya resuelta, nunca de
  forma síncrona ni delegado a un helper. Ver `frontend/src/app/page.tsx` (patrón ya existente desde
  el Sprint 3) o `frontend/src/components/DevicesPanel.tsx` (Sprint 6) como referencia.

**Lecciones del Sprint 60, válidas para cualquier cambio futuro en `frontend/`** (evidencia en
`docs/sprint-60-evidence.md`):

- **Anillo de foco.** El `:focus-visible` global (especificidad 0,1,0) lo pierde cualquier componente que
  declare su propio `box-shadow` con una clase (0,1,0), porque las hojas de módulo cargan después: pasaba con
  `Button.primary` y con la fila seleccionada de Alertas. Un componente con sombra propia necesita su
  `.x:focus-visible` (0,2,0) que la repita junto al anillo. En modo de alto contraste de Windows el
  `box-shadow` desaparece: `globals.css` tiene un `outline` dentro de `@media (forced-colors: active)`.
- **Probar el foco en e2e.** Llegar con `Tab` (no `locator.focus()`, que no garantiza `:focus-visible`) y leer
  `getComputedStyle(el).boxShadow` con `expect.poll`: `Button` anima `box-shadow` 0,15 s y una lectura inmediata
  devuelve el inicio de la transición. La sombra de reposo se serializa como `oklab(…)`/`color(srgb …)`, no `rgba`.
  Sirve de test de regresión solo si falla con el CSS anterior: se comprobó con un worktree en `d42997c`.
- **`networkidle` no sirve en esta SPA:** se resuelve en 2–8 ms al cambiar de sección, así que no espera a las
  peticiones del panel. Usar una espera acotada o esperar a un elemento concreto. El build de CI no lleva
  `NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID`: el login sale sin botón de Google (aviso «Falta configurar…»); un e2e del login no
  puede afirmarlo.
- **Imágenes.** Next 16 solo admite `quality` 75 por defecto y redondea cualquier otro valor en silencio: si un
  `<Image quality={90}>` ha de surtir efecto, declararlo en `images.qualities` de `next.config.ts`. `/_next/image` es el
  mismo origen (`img-src 'self'` ya lo cubre) y sin `remotePatterns` no puede usarse de proxy; no hace falta tocar la CSP
  para pasar un asset local por el optimizador.
- **Texto sobre cristal translúcido** (login): el contraste se mide con la foto real, desenfocada y aclarada, en el peor
  píxel detrás de cada texto, no se estima. Un velo fino exige texto en `--color-navy` pleno; `--color-text-muted` sobre
  el cristal daba 3,35:1. Sobre fondos oscuros (aquí, los árboles de `study.jpg`) un panel solo más transparente sale
  gris: se aclara el fondo con `brightness()` dentro del `backdrop-filter`.

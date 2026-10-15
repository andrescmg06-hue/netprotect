# Sprint 53 — Rediseño editorial del panel web, base visual (sin migrar vistas)

Segundo sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Rama `sprint-53-rediseno-web-base-visual`, creada desde
`sprint-52-rediseno-web-fase-0` para conservar el informe aprobado; el PR de este sprint va a `web-redesign` cuando el
del S52 ya esté dentro. Parte del informe `docs/redesign/fase-0-informe.md` (aprobado el 04/10/2026).

## Objetivo

Cambiar el **sistema** (tokens, tipografía, primitivas) sin tocar el JSX de ninguna de las 16 vistas. Si la base está
bien, las vistas ya se ven en el lenguaje nuevo (crema, navy, serif, líneas) y los sprints S54–S59 solo recomponen.

## Por qué en este orden

Cambiar un token mueve las 16 vistas a la vez (riesgo alto del plan). Por eso primero la **línea base** de capturas
(T1, pendiente del S52), después los tokens y solo al final las primitivas, siempre manteniendo sus props.

## Reglas del sprint

- No se toca JSX de vistas (`*Panel.tsx`, `DashboardShell.tsx`, `login/*`); sí `globals.css`, `layout.tsx`, `ui/*`,
  `app/design-system/*`, `public/` y `DESIGN.md`.
- Mismos nombres de token (alias de los antiguos). Sin Tailwind, sin librerías de gráficos ni de mapas.
- D1 (serif) y D5 (logo) siguen abiertas: Playfair Display por defecto en **una sola variable** (`--font-serif`), y los
  logos actuales de `public/brand/` no se sustituyen.
- Un commit local por unidad de trabajo. Sin push ni PR.

## Tareas

Ruta: **inline** = quien lidera; **delegado** = worker acotado, con el motivo.

- [x] T1 — Línea base: capturas de login + 16 vistas a 1440 y 390 px en `docs/redesign/baseline/`, con sesión sembrada
  (`compose.test.yaml`). Delegado (Docker + Playwright, trabajo largo y mecánico). **Antes de tocar tokens.**
  Hecha: 34 capturas verificadas, ver `docs/redesign/baseline/LEEME.md`. El login sale sin botón de Google porque el
  build no lleva ID de cliente; ocho vistas están en estado vacío porque la sesión sembrada no crea datos para ellas.
- [x] T2 — Tokens de `:root` en `globals.css`: valores nuevos (informe §6), nuevos tokens, corrección de contraste
  (`--color-text-subtle` → `#6f6b60`, separado de `--color-border-strong`), `--focus-ring` para fondo oscuro, tokens de
  layout (cortes y anchos), y literales sueltos a tokens. Inline (decisión de diseño).
- [x] T3 — Serif en `app/layout.tsx` (`--font-playfair`), h1–h4 en serif, `body` con fondo plano, cifras tabulares,
  utilidades `eyebrow` y `quote`. Inline.
- [x] T4 — Assets: copiar a `public/` las fotos del paquete (≤ 250 KB, para `next/image` en S54). **No** se retiran
  `devices-showcase.png` ni `background-desk-clean.png`: `HeroShowcase` y `BackgroundScene` aún los usan; salen con el
  login nuevo (S54). Inline.
- [x] T5 — Primitivas (mismas props): `Card` como panel abierto + ranura de cabecera, `Button`, `StatusBadge`,
  `EmptyState` con ilustración opcional y `PageHeader` con banda. Los componentes nuevos del informe (`LoadState`,
  `ReadOnlyField`, `lib/format.ts`) **no** se crean aquí: quedarían sin uso hasta migrar una vista, así que nacen con
  la primera vista que los necesite (S55+). Delegado (CSS/JSX mecánico, una vez fijados los tokens).
- [x] T6 — Reescribir `DESIGN.md` (raíz) y actualizar la galería `/design-system` a los tokens nuevos. Inline.
- [x] T7 — Verificación: `npm run lint && npm run build` (agente `verifier`), galería a 1440/390 y comparación contra la
  línea base de T1. Delegado.
- [ ] T8 — Cierre: `docs/sprint-53-evidence.md`, README, `docs/progress.md`; `/cerrar-sprint 53` (lo lanza el dueño).

## Criterio de aceptación

Lint y build en verde; la galería muestra los tokens nuevos a 1440 y 390 px; las 16 vistas y el login siguen
funcionando y sin regresión visual grave frente a la línea base (las diferencias esperadas son de color, radio, sombra y
tipografía, no de estructura); ningún archivo de vista cambió; el e2e de Playwright sigue en verde.

## Estado (04/10/2026)

- T1–T7 hechas; evidencia en `docs/sprint-53-evidence.md` (lint, tipos y build en verde; comparación visual sin regresiones de estructura; dos defectos reales encontrados por la comparación y corregidos: contraste de la banda y cifras de Playfair).
- T8: README, `docs/progress.md` y evidencia hechos. **Falta `/cerrar-sprint 53`**, que solo lo lanza el dueño, y que CI pase los 8 jobs tras el push.
- Criterios: lint y build en verde (cumplido); galería con los tokens nuevos a 1440 y 390 px (cumplido, `docs/redesign/s53/`); ninguna vista cambió de estructura (cumplido); **e2e de Playwright en verde: pendiente, solo lo corre CI**.

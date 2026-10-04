# Sprint 52 — Rediseño editorial del panel web, Fase 0 (inspección y propuesta)

Primer sprint del rediseño del panel web (`docs/redesign/PLAN_SPRINTS.md`, S52–S60). **No se escribe código del
producto**: se entrega un informe y se espera aprobación antes del S53. Rama: `sprint-52-rediseno-web-fase-0`, desde
`web-redesign`.

## Por qué existe este sprint

El paquete de diseño se preparó contra una copia desactualizada del repo en GitHub (le faltaban sprints hasta el 49 y el
panel ya había tenido un primer rediseño, S31–S39). Antes de cambiar tokens que mueven las 16 vistas a la vez (S53), hay
que medir el estado real: qué hay, qué se puede reutilizar y qué muestra cada mockup que el backend no tiene.

## Tareas

Ruta: **inline** = quien lidera directo; **delegado** = un explorador/escritor acotado, con el motivo.

- [ ] T1 — Arquitectura del frontend: rutas, `DashboardShell`, estado compartido, `AuthContext`, dependencias.
  Delegado (mapeo de 4+ archivos).
- [ ] T2 — Sistema de estilos actual: `globals.css`, `DESIGN.md`, CSS Modules, `components/ui/*` y qué variables usa
  cada componente. Delegado (mismo mapeo que T1).
- [ ] T3 — Componentes repetidos e inconsistencias entre los 16 paneles + login. Delegado.
- [ ] T4 — Assets: `public/brand/` y `public/login/` frente a `docs/redesign/assets/` (peso, formato, qué falta).
  Inline.
- [ ] T5 — Matriz por vista: datos reales del backend (`lib/apiClient.ts`, `docs/planning/plan-frontend.md`) frente a
  lo que muestra el mockup → lista de omisiones y sustituciones. Delegado; el resultado se revisa contra
  `03_VISTAS.md`.
- [ ] T6 — Propuesta de design system: qué variables de `:root` cambian de valor (sin renombrar), cuáles son nuevas y qué
  componentes pasan a ser globales (`PageHeader` con banda, `Timeline`, `ScheduleBar`, `StatusBadge`, `EmptyState` con
  ilustración, `Field`, `DataTable`). Inline (decisión de diseño).
- [ ] T7 — Riesgos: contraste, CSP de fuentes (`font-src 'self'`), rendimiento de imágenes, e2e de Playwright existente.
  Inline.
- [ ] T8 — Capturas base de las 16 vistas + login a 1440 y 390 px en `docs/redesign/baseline/`. Necesita backend y una
  sesión de tutor (`docs/redesign/CONTINUAR.md` §3); si no se puede, se declara pendiente y el S53 las toma antes de
  cambiar tokens.
- [ ] T9 — Redactar `docs/redesign/fase-0-informe.md` (T1–T7) con las decisiones abiertas: tipografía serif definitiva,
  assets faltantes, orden de vistas. **Detenerse y esperar aprobación del dueño.**
- [ ] T10 — Cierre: `docs/sprint-52-evidence.md`, `docs/progress.md`, `/cerrar-sprint 52`.

## Criterio de aceptación

El informe cubre los 7 puntos con evidencia (rutas y líneas, no volcados), la matriz de datos reales por vista está
completa y el dueño lo aprueba por escrito. Ninguna línea de `frontend/src/` cambió.

## Decisiones abiertas (se cierran con el informe)

- Fuente serif definitiva (hoy Playfair Display, una aproximación a los mockups).
- Assets faltantes: isotipo blanco monocromo, SVG limpio, ilustraciones de Reglas, Auditoría y Silenciadas.
- Si el S51 se fusiona en `main` antes de cerrar el S52: actualizar `web-redesign` desde `main`.

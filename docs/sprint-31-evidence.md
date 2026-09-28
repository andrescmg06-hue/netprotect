# Sprint 31 — Evidencia

## Verificaciones

| Paso | Resultado |
|---|---|
| `npx eslint .` | limpio |
| `npx tsc --noEmit` | limpio |
| `npx next build` | compila; rutas `/`, `/design-system`, `/icon.png` |
| Standalone de producción | `/design-system` → 404, `/` → 200, `/brand/logo-full.png` → 200, sin referencias a `fonts.googleapis`/`fonts.gstatic` |
| E2E Playwright contra `compose.test.yaml` | 1 passed |
| Imagen Docker `web` reconstruida | compila dentro del contenedor (Inter descargado en build), `localhost:3000` → 200 |

## Revisión visual (Playwright, capturas reales)

- Galería a 1440 px: métricas, botones, badges, formulario, días, switch, búsqueda, periodo y
  estado vacío con el lenguaje visual de los mockups.
- Diálogo de confirmación: fondo difuminado, foco atrapado por `<dialog>`, se cierra con Escape.
- Login heredado a 1440 px: ya en tema claro sin cambios en su JSX.
- Móvil a 390 px: primera captura con desbordamiento horizontal (`minmax(420px, …)` en la
  galería y en `MetricGrid`) y el selector de días partido en dos filas. Corregido con
  `minmax(min(…, 100%), 1fr)` y botones de día de 36 px; segunda medición:
  `document.documentElement.scrollWidth === 390`.

## Aviso conocido (no es de este sprint)

En `next dev` el overlay marca "1 Issue": React intenta `eval()` en modo desarrollo y la CSP del
Sprint 21 lo bloquea. React no usa `eval()` en producción; no requiere cambios.

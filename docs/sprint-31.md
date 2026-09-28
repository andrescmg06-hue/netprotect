# Sprint 31 — Fundaciones del sistema de diseño

Primer sprint de `docs/planning/plan-frontend.md`. Deja listo lo que todas las vistas van a
reutilizar; ninguna vista se rediseña todavía.

## Qué se hizo

- **Tokens** (`src/app/globals.css`): colores semánticos, radios, sombras, espaciado, tipografía
  y movimiento. Tema claro; se retira el oscuro.
- **Transición sin romper nada:** las variables anteriores (`--surface`, `--line`, `--accent`…)
  son ahora alias de los tokens, así los 16 paneles actuales pasan a tema claro sin tocar su
  JSX. Los selectores globales que contaminarían componentes nuevos se acotaron: `button` →
  `:where(button)` (especificidad cero), `article` → `.grid article`, `h1` → `.hero h1`.
- **Inter** con `next/font/google`: se descarga en el build y se sirve desde el propio origen
  (verificado: el HTML no referencia hosts de Google), así que la CSP `font-src 'self'` no cambia.
- **`lucide-react`** como único set de iconos.
- **Logo** (`public/brand/`): recorte con fondo transparente del PNG entregado (1 MB → 104 KB
  completo, 54 KB solo escudo) mediante relleno desde los bordes, para no borrar el blanco de las
  figuras. El escudo es también el favicon (`app/icon.png`).
- **Componentes** (`src/components/ui/`, CSS Modules): `Button`, `Card`/`CardHeader`,
  `MetricCard`/`MetricGrid`, `StatusBadge`, `PageHeader`, `EmptyState`, `Field`/`Input`/`Select`,
  `DayPicker` (misma máscara de bits que el backend, bit 0 = lunes), `SegmentedControl`, `Switch`,
  `ConfirmDialog` (`<dialog>` nativo), `Spinner`, `Logo`.
- **Galería** `/design-system`, solo en desarrollo (404 en producción, verificado), para revisar
  cada componente en un navegador real.

## Desviación del plan

No se creó `TimeInput`: `<Input type="time">` usa el selector nativo con el mismo estilo.

## Encontrado y anotado para el Sprint 32

La CSP (`img-src`) no incluye `lh3.googleusercontent.com`, así que el avatar de Google del tutor
no se ve hoy en ningún entorno. El header del Sprint 32 lo muestra: hay que agregarlo allí.

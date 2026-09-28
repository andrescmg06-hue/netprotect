# Sprint 32 — Evidencia

## Verificaciones

| Paso | Resultado |
|---|---|
| `npx eslint .` / `npx tsc --noEmit` | limpios (antes y después de cada lote de correcciones) |
| `impeccable detect --json` sobre shell/, ui/, DashboardShell y globals.css | `[]` |
| E2E Playwright (`dashboard.spec.ts`, selectores por rol) contra `compose.test.yaml` | 1 passed |
| Imagen Docker `web` reconstruida | `localhost:3000` → 200 |

## Capturas (build de producción + backend real, sesión sembrada)

Script temporal de Playwright (no versionado) con una sola página que cambia de tamaño, porque
el refresh token es de un solo uso: 1440, 1024, 390 y 1920 px, más cajón abierto, menú de cuenta
y buscador con resultados.

- **Tropiezo:** la primera corrida se colgó 90 s buscando un botón con un nombre que no existe en
  los datos de prueba; era del script, no de la app. Corregido y repetido con sesión nueva.
- **Primera inspección:** en vistas de dispositivo la ruta de navegación y la tarjeta se apilaban
  y empujaban el contenido; en móvil la tarjeta no ocupaba el ancho, el texto del buscador se
  cortaba y "PANEL DEL TUTOR" del logo era ilegible. Corregidos en un lote.
- `document.documentElement.scrollWidth` a 390 px: **390** en todas las corridas.

## Revisión final independiente (Impeccable)

El agente revisor del skill no estaba registrado en la sesión (arrancó en la carpeta padre); se
sustituyó por un agente nuevo, sin historial, que adoptó su definición
(`.claude/agents/impeccable-finish-reviewer.md`).

**Ronda 1 — `fix`, 7 puntos:** foco del cajón móvil (WCAG 2.4.3), contraste de la etiqueta de
resultados (≈2,6:1) y del texto guía del buscador (≈4,43:1), `role="menu"` sin navegación con
flechas, "Cerrar sesión" en el riel de tablet más fácil de tocar que cualquier sección, móvil sin
marca, desalineación a más de ~1710 px. Tipo, material y fondo: *match* con los mockups.

**Ronda 2 (verificación):** 6 resueltos, 1 parcial. Medido en la corrida:

```
after menu Escape, focus on: Cuenta de Sprint 25 seed (tutor)
drawer open, focus on: Cerrar menú
drawer closed, focus on: Abrir menú
```

El parcial: la etiqueta "Sección" sobre la fila resaltada (#f3f7ff) daba ≈4,43:1. Se aplicó el
valor que calculó el propio revisor (#5b6b82, ≈5:1). No se abrió una tercera ronda por ser un
único valor mecánico ya medido; queda anotado aquí.

## Aviso del skill

`npx impeccable update` (v4.2.0 → v4.4.0) falló con HTTP 404 del proveedor; no se instaló nada y
se siguió con v4.2.0.

## DESIGN.md y corrección de contraste

El documentador del skill (agente nuevo con la definición de `.claude/agents/impeccable-documenter.md`)
escribió `DESIGN.md` y `.impeccable/design.json` desde el código construido, y midió cinco pares
de color por debajo de 4,5:1: texto de badge verde (4,01), naranja (4,26), gris (4,23), azul sobre
el fondo de hover (4,24) y gris/azul sobre el fondo de página (4,50/4,31). Corregidos en los tokens:

| Token | Antes | Ahora | Peor caso medido |
|---|---|---|---|
| `--color-text-muted` | #64748b | #5b6b82 | 4,83:1 (sobre #eef2f7) |
| `--color-success-text` | #0e8a4b | #0b7a42 | 4,92:1 |
| `--color-warning-text` | #b25e02 | #9a5000 | 5,43:1 |
| `--color-primary-text` (nuevo) | — | #1f5fe6 | 4,96:1 (sobre #eef4ff) |

`#246bfe` queda solo para rellenos (texto blanco encima: 4,56:1); todo texto o icono azul usa
`--color-primary-text`. `DESIGN.md` se actualizó con la regla "Fill-vs-Text Blue" y los valores
medidos. Los nombres de la paleta ("The Calm Blue Frame", Signal Blue, Shield Navy…) son propuesta
del documentador y quedan a confirmar por el dueño del proyecto.

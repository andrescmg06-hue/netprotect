# Sprint 53 — Evidencia

Fecha: 04/10/2026. Rama `sprint-53-rediseno-web-base-visual` (sale de `sprint-52-rediseno-web-fase-0`). Node local 24.14.1
(CI usa 22: solo CI lo confirma).

## 1. Línea base antes de tocar tokens (T1)

34 capturas (login + 16 vistas, a 1440 y 390 px) en `docs/redesign/baseline/`, tomadas con el stack de `compose.test.yaml`,
la sesión sembrada por el script propio del proyecto y el build de producción en `localhost:3000`. Verificado por quien
lidera: 34 PNG, ninguno vacío (<5 KB), 2.7 MB en total, un render real abierto (`overview-1440.png`). Commit `4356204`.

Tropiezos del camino (del reporte del worker): el primer pase de `overview` salió con «…» en las métricas y se repitió
esperando a que desaparecieran; `next build` modifica el archivo versionado `frontend/next-env.d.ts` y hay que
revertirlo; el login de la línea base sale sin botón de Google porque el build no lleva ID de cliente (igual que CI).

## 2. Verificación del código (T7, agente `verifier`)

```
$ cd frontend && npm run lint        # eslint . --max-warnings=0
PASS, 0 errores, 0 warnings (20 s)
$ npx tsc --noEmit
PASS, sin salida (15 s)
$ npm run build                      # Next.js 16.3.8, Turbopack
PASS, exit 0 (48 s). Compiled successfully en 14.8 s; 5 páginas estáticas; rutas: /, /_not-found, /design-system, /icon.png
```

La descarga de Playfair Display por `next/font/google` funcionó (el log no contiene errores de fuente). Con el build
restaurado `frontend/next-env.d.ts`; el árbol quedó solo con los cambios del sprint.

Ninguna vista cambió de estructura: `git diff --name-only` no incluye ningún `*Panel.tsx`, `DashboardShell.tsx`, `login/*`
y las nuevas props (`PageHeader band/quote`, `EmptyState illustration`) son opcionales y apagadas por defecto.

## 3. Contraste

Los valores del paquete que fallaban se corrigieron en `globals.css` (cálculo en `docs/sprint-52-evidence.md` §4):
`--color-text-subtle` pasa de `#8a8678` (3.29:1 sobre crema, falla como texto) a `#6f6b60` (4.80:1 sobre crema, 5.32:1 sobre
blanco, 4.71:1 sobre `#f3f1ec`); `#8a8678` queda solo como borde de control (3.29:1 y 3.64:1, mínimo 3:1 para no texto).

## 4. Comparación visual contra la línea base

Un worker repitió el procedimiento de la línea base sobre el árbol con los cambios (build de producción, sesión
sembrada, 34 capturas) y las comparó vista por vista; yo abrí además la galería, Inicio y Política y horario. Capturas
elegidas en `docs/redesign/s53/` (9 PNG y `LEEME.md`); el juego completo de 34 está fuera del repo.

**Sin regresiones de estructura**: ni desbordes, ni textos cortados, ni estados seleccionados invisibles, ni iconos
ausentes; 0 errores de consola, 0 errores de página, 0 respuestas HTTP ≥ 400 en las 34 capturas. Las diferencias son las
esperadas (colores, fondo crema, radios de 4–8 px, casi sin sombras, títulos en serif, métricas sin caja); las alturas de
documento bajan de 0 a 164 px por las métricas sin caja.

**Hallazgos y qué se hizo** (corregidos en el commit del cierre, verificados en un navegador real contra el servidor de
desarrollo, sin repetir el build completo):

| Hallazgo | Medición | Corrección | Resultado |
|---|---|---|---|
| Descripción de la banda por debajo de 4.5:1 | 4.14:1 en la cola de la línea 1 (x 626–660): el texto llegaba a 640 px y el velo sólido terminaba en 554 px | Velo sólido hasta el 58 % y columna de texto limitada al 55 % (`PageHeader.module.css`) | Título 14.09:1, descripción 5.17:1, frase 5.17:1 (mínimo por línea, píxeles reales de fondo con el texto oculto) |
| Cifras «de texto» de Playfair: el «1» de «Dispositivos vinculados (1)» parece una «ı» | Visible en `devices-1440` y `devices-390` | `font-variant-numeric: lining-nums` en h1–h4 (`globals.css`) | Estilo computado `lining-nums`; render a 2× con «(1) · 2026 · 0123456789» legible |
| Foto de la banda cargada en diferido | Aviso de Next «LCP … loading="eager"» en el log de desarrollo | `loading="eager"` (commit `ebaba4d`) | `tsc` y `eslint` en verde |

Notas honestas:
- La insignia roja «1 Issue» de la galería en `next dev`: el worker la atribuye a la CSP (`next.config.ts` sin
  `unsafe-eval`, solo en desarrollo); yo vi el aviso de imagen en el log, pero **no verifiqué** cuál de los dos la causa.
  Solo aparece con `next dev`, no en el build de producción.
- **Sin comprobar en pantalla real**: en la galería la barra de la «e» de «editorial» desaparece en Chromium sin
  pantalla a 1×; las `h1` de las vistas se ven bien. Hay que mirarlo en un monitor real. No hay galería de línea base,
  así que la galería no tiene comparación antes/después.
- A 390 px la foto de la banda queda casi cubierta por el velo y el selector («En línea») se estira a ancho completo:
  detalle de diseño para S54, no una regresión.
- Preexistentes y no contados: la marca gris sobre azul de la tarjeta seleccionada de Política, el sidebar recortado en
  vistas altas y la etiqueta «8:00» sobre la barra azul a 390 px.

## 5. Pendiente

- `/cerrar-sprint 53`: solo lo lanza el dueño; verifica CI (8 jobs) en GitHub Actions tras el push de su amigo.
- E2E de Playwright (`frontend/e2e/dashboard.spec.ts`) con la rama nueva: solo lo corre CI.
- D1 (serif definitiva) y D5 (logo oficial): dependen del diseñador del paquete.
- Dos restos señalados por el writer, para S54/S55: el velo del cajón del `Sidebar` conserva el navy antiguo
  (`rgba(16,43,99,.3)`) y los discos redondos de `MetricCard` y `EmptyState` contradicen la regla «píldora solo para
  interruptores y puntos» hasta tener el icono de tono compartido.

# Sprint 54 — Evidencia

Fecha: 04/10/2026. Código en `35af89d` (Cristian, 04:22); verificación con datos reales hecha después, en la tanda de
S59. Este archivo se escribió en el S60 (T1) **a partir de lo que dejaron las fuentes**; no repite ni inventa
resultados. La evidencia visual de S54–S59 está **concentrada** en `docs/redesign/s59/LEEME.md` (un solo juego de
capturas del 04/10 con el panel completo ya recompuesto), no repartida por sprint.

## 1. Qué consta

| Qué | Fuente | Estado |
|---|---|---|
| `tsc` y `eslint` en verde | Mensaje de `35af89d` («tsc and eslint pass») y `docs/sprint-54.md` T1 | Registrado solo en texto; **no hay salida guardada** |
| La imagen `web` reconstruida sirve el login nuevo (200) | `docs/sprint-54.md` T5 | Registrado en texto |
| Ningún `*Panel.tsx` cambió | `docs/sprint-54.md` T5; `git show --stat 35af89d` lo respalda (solo `DashboardShell`, login, shell y `ui/`) | Comprobado en este archivo con el historial |
| Render del login a 1440 y 390 px | `docs/sprint-54.md` T1 | Dicho; **sin captura en el repo** (ver §3) |
| Causa de la insignia «1 Issue» de `next dev` | `docs/sprint-54.md` (Decisiones): CSP sin `unsafe-eval`, solo en desarrollo | Registrado |
| Los tres layouts del marco (sidebar completo a 1440, riel a 1000, cajón a 390) se ven correctos | `docs/redesign/s59/LEEME.md`, «Defectos encontrados» | Observación a ojo, con sesión sembrada por la API |
| Sin errores de consola, de página ni respuestas HTTP >= 400; sin desbordamiento horizontal | `docs/redesign/s59/LEEME.md`, «Qué se comprobó» | Registrado para las 34 capturas del conjunto (login + 16 vistas, 1440 y 390) |

## 2. Capturas que sí existen en el repo

Todas en `docs/redesign/s59/` (verificado con `ls`). Se tomaron con HEAD `8be5abd`, **antes** de `eb2db05` (que cambió la
serif a Newsreader): muestran el marco con Playfair.

- `login-1440.png` — login nuevo. Muestra «Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.»: el build de prueba no
  define esa variable, así que **no hay botón real de Google** en la captura.
- `overview-1440.png` — Inicio dentro del marco (sidebar, header, banda) a 1440 px.
- `overview-390.png` — Inicio dentro del marco a 390 px.

(No se abrieron los PNG al escribir este archivo: lo que se dice de ellos viene del LEEME.)

## 3. Evidencia que no existe

- `login-390.png` (el LEEME dice que existe en el juego de 34 generado **fuera del repo**; no se puede citar).
- `overview-1000` (riel) y `overview-390-drawer` (cajón abierto): el LEEME los menciona, pero **están fuera del repo**.
- Capturas «tras cada pieza» (login → sidebar → header → banda), como pedía el plan (§4, S54): no existen por separado.
- El login real con Google y la revisión visual del shell con una **sesión real de Google**: pendiente, la hace el dueño
  (T5 de `docs/sprint-54.md`, sin marcar).
- Que el cajón de 390 px desplace su lista hasta «Auditoría» y «Cerrar sesión»: el LEEME declara que **no se comprobó**
  (en la imagen del cajón la lista se corta en «Vista remota»).
- Salida de `eslint`, `tsc` o `npm run build` guardada para este commit.
- Recaptura posterior a `eb2db05` (cambio de serif a Newsreader).
- Medición de contraste con herramienta (el LEEME dice «solo a ojo»).

## 4. Observaciones del marco en la verificación del S59

Del LEEME: ningún hallazgo de sidebar, encabezado o banda rotos, ni de estados seleccionado o hover invisibles. Menores:
el sidebar mide 100vh en las capturas `fullPage` y deja ver solo hasta «Auditoría» (comportamiento esperado de la captura,
con scroll interno).

## 5. Pendiente

- T5 de `docs/sprint-54.md`: revisión visual con sesión real de Google (dueño).
- Serif Newsreader sigue «abierta para el diseñador».
- Restos del S53 para S54/S55 (velo del cajón del `Sidebar` con el navy antiguo; discos redondos de `MetricCard` y
  `EmptyState`): no consta que se hayan resuelto.

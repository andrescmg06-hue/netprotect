# Sprint 58 — Evidencia

Fecha: 04/10/2026. Código en `7b6b117` (Cristian, 10:00); correcciones en `eb2db05`. Escrito en el S60 (T1) a partir de lo
que dejaron las fuentes. La evidencia visual de S54–S59 está **concentrada** en `docs/redesign/s59/LEEME.md` (un solo
juego del 04/10, con HEAD `8be5abd`, antes de `eb2db05`); no se reparte artificialmente.

## 1. Qué consta

| Qué | Fuente | Estado |
|---|---|---|
| Datos reales de estadísticas y alertas | LEEME: uso de hoy y 5 días previos, 5 bloqueos (BLOCK x2, DAILY_LIMIT, SCHOOL_MODE, CATEGORY), 6 alertas generadas, una silenciada 7 días (TikTok) y una marcada como leída | Sembrado por la API |
| E2E en verde sobre el conjunto | LEEME: «1 passed, 15,7 s» | Registrado |
| Sin errores de consola/página ni HTTP >= 400; sin desbordamiento horizontal | LEEME, «Qué se comprobó» | Registrado para el conjunto |
| `tsc` / `eslint` | Ni `7b6b117` ni `eb2db05` los mencionan | **Sin registro** |

## 2. Capturas que sí existen en el repo

En `docs/redesign/s59/` (verificado con `ls`), tomadas antes de `eb2db05`:

- `statistics-1440.png` — Estadísticas.
- `alerts-1440.png` — Alertas.

## 3. Evidencia que no existe

- **Ninguna captura de Silenciadas** (`silenced-1440`/`silenced-390`), ni observación de esa vista en el LEEME.
- `statistics-390.png` y `alerts-390.png`: en el juego de 34 fuera del repo; no se pueden citar (el LEEME describe
  defectos de ambas, pero las imágenes no están aquí).
- Recaptura tras `eb2db05`.
- Salida guardada de `eslint`, `tsc` o `npm run build`.
- Medición de contraste con herramienta (el LEEME dice «solo a ojo»). Relevante aquí: D2 (rojo solo en crítica) se
  juzgó a ojo.

## 4. Defectos del LEEME en estas vistas y qué dice `eb2db05`

| Defecto observado (LEEME, con HEAD `8be5abd`) | Corrección declarada en `eb2db05` | Verificada |
|---|---|---|
| `statistics` (1440 y 390): en la dona «Por categoría» el azul se repite (Redes sociales y Sin categoría) | Colores distintos y un neutro para apps sin categoría | No |
| `statistics`: Streaming y Comunicación (verde y verde-azulado) casi iguales | No mencionada | No consta |
| `statistics` a 390: «Redes sociales» truncado («Redes socia…») | No mencionada | No consta |
| `alerts-390`: la tira de contadores (Todas/Info/Advertencia/Alta/Crítica) queda cortada a la derecha | La tira de niveles ya no se corta a 390 px | No |
| Alertas pedía otra vez la lista de dispositivos | Reutiliza la del shell | No es un defecto visual del LEEME; es una mejora declarada en el commit |

## 5. Pendiente

- Captura de Silenciadas (1440 y 390) y los 390 px que faltan.
- Recaptura de Estadísticas y Alertas tras `eb2db05`.
- Ver `docs/sprint-58.md`, «Pendiente conocido».

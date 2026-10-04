# Sprint 56 — Evidencia

Fecha: 04/10/2026. Código en `bdfa6e8` (Cristian, 10:03). Escrito en el S60 (T1) a partir de lo que dejaron las fuentes.
La evidencia visual de S54–S59 está **concentrada** en `docs/redesign/s59/LEEME.md` (un solo juego del 04/10, con HEAD
`8be5abd`, antes de `eb2db05`); no se reparte artificialmente.

## 1. Qué consta

| Qué | Fuente | Estado |
|---|---|---|
| Datos reales de las cuatro vistas | LEEME: 11 apps; reglas por app de los 5 tipos (Instagram BLOCK, Duolingo ALLOW, YouTube DAILY_LIMIT 90, Netflix SCHEDULE 20:00-22:00 L-V, Clash of Clans WEEKLY_LIMIT 420); 9 apps asignadas a categorías y 3 reglas de categoría; modo escolar 07:00-14:00 L-V activado; política ALLOW | Sembrado por la API, no desde la interfaz |
| E2E en verde sobre el conjunto | LEEME: «1 passed, 15,7 s»; incluye que `com.instagram.android` aparece una sola vez | Registrado |
| Sin errores de consola/página ni HTTP >= 400; sin desbordamiento horizontal | LEEME, «Qué se comprobó» | Registrado para el conjunto |
| `tsc` / `eslint` | El mensaje de `bdfa6e8` **no** los menciona | **Sin registro** |

## 2. Capturas que sí existen en el repo

En `docs/redesign/s59/` (verificado con `ls`), tomadas antes de `eb2db05`:

- `rules-1440.png` — Reglas.
- `policy-1440.png`, `policy-390.png` — Política y horario.
- `categories-1440.png` — Categorías.

## 3. Evidencia que no existe

- **Ninguna captura de Apps** (`apps-1440`/`apps-390`) en el repo, ni observación de esa vista en el LEEME.
- `rules-390.png` y `categories-390.png`: en el juego de 34 fuera del repo; no se pueden citar.
- Arrastre real de la `ScheduleBar`, o guardar el modo escolar desde la interfaz: no registrado.
- Salida guardada de `eslint`, `tsc` o `npm run build`.
- Recaptura posterior a `eb2db05`.
- Medición de contraste con herramienta (el LEEME dice «solo a ojo»).

## 4. Defectos del LEEME que tocan estas vistas

El LEEME **no lista defectos específicos** de Apps, Reglas, Política ni Categorías. Solo el general de los títulos `h1`
en Playfair (ver `docs/sprint-55-evidence.md` §4), que `eb2db05` atiende cambiando a Newsreader sin recaptura.

## 5. Pendiente

- Captura (1440 y 390) de Apps con datos, y los 390 px que faltan.
- Ver `docs/sprint-56.md`, «Pendiente conocido».

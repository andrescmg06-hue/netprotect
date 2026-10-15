# Sprint 57 — Evidencia

Fecha: 04/10/2026. Código en `8be5abd` (Cristian, 10:11); correcciones en `eb2db05`. Escrito en el S60 (T1) a partir de lo
que dejaron las fuentes. La evidencia visual de S54–S59 está **concentrada** en `docs/redesign/s59/LEEME.md` (un solo
juego del 04/10, con HEAD `8be5abd`, antes de `eb2db05`); no se reparte artificialmente.

## 1. Qué consta

| Qué | Fuente | Estado |
|---|---|---|
| Datos reales de ubicación | LEEME: 2 geocercas (Colegio San Rafael 300 m, Casa 150 m); 4 ubicaciones (fuera, dentro, dentro, fuera: genera ENTRADA y SALIDA); 5 bloqueos | Sembrado por la API, no por un teléfono |
| E2E en verde sobre el conjunto | LEEME: «1 passed, 15,7 s» | Registrado |
| Sin errores de consola/página ni HTTP >= 400; sin desbordamiento horizontal | LEEME, «Qué se comprobó» | Registrado para el conjunto |
| `tsc` / `eslint` | Ni `8be5abd` ni `eb2db05` los mencionan | **Sin registro** |
| `security-reviewer` (el plan lo exige: toca ubicación) | — | **Sin registro** |

## 2. Capturas que sí existen en el repo

En `docs/redesign/s59/` (verificado con `ls`), tomadas antes de `eb2db05`:

- `geofences-1440.png`, `geofences-390.png` — Geocercas.
- `location-1440.png` — Ubicación.
- `history-1440.png` — Historial.

## 3. Evidencia que no existe

- `location-390.png` e `history-390.png`: en el juego de 34 fuera del repo; no se pueden citar.
- **Recaptura tras `eb2db05`**: las capturas muestran los defectos de abajo, no sus correcciones.
- Salida guardada de `eslint`, `tsc` o `npm run build`.
- Informe del `security-reviewer`.
- Una ubicación real de un teléfono; la auditoría `LOCATION_VIEWED` por cada lectura (solo afirmada en el commit).
- Medición de contraste con herramienta (el LEEME dice «solo a ojo»).

## 4. Defectos del LEEME en estas vistas y qué dice `eb2db05`

| Defecto observado (LEEME, con HEAD `8be5abd`) | Corrección declarada en `eb2db05` | Verificada |
|---|---|---|
| `geofences-390`: etiquetas de «Colegio San Rafael» y «Última ubicación» superpuestas («C Última ubicación el»); «el defecto más claro» | Etiquetas del mapa ya no colisionan en móvil | No |
| `history` (1440 y 390): la hora se parte en dos líneas («10:30 a. / m.») | La hora queda en una línea | No |
| «Cifras con pie» en «04 OCT 2026» de Historial: el 6 se lee como 8 | El commit cambia la serif (Playfair → Newsreader); no cita este caso | No (el LEEME ya decía «no verificado con zoom») |
| Coordenadas con espacio tras el signo («- 74.07210») | Signo menos verdadero en coordenadas | No |

## 5. Pendiente

- Revisión de seguridad por `security-reviewer` (ubicación).
- Recaptura de las tres vistas tras `eb2db05`, y los 390 px de Ubicación e Historial.
- Ver `docs/sprint-57.md`, «Pendiente conocido».

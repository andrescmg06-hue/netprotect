# Sprint 69 — B-12: cada tutor ve solo las geovallas que creó

Cierra **B-12** de `docs/tasks.md` (invariante 6, privacidad de un menor), que dejó abierto el Sprint 68 (B-09). Un tutor nuevo que revincula la misma instalación ve datos **derivados de la ubicación** de antes de su vínculo: las coordenadas descifradas del centro de las geovallas del tutor
anterior (`geofences.py`), sus eventos de entrada y salida, la línea de tiempo (`history.py`) y las alertas `GEOFENCE_ENTER/EXIT` (`alerts.py`). Una geovalla con nombre («Casa», «Colegio») es más sensible que una ubicación suelta: el tutor ha etiquetado qué significa ese punto.

**Decisión del dueño (14/10/2026): cada tutor ve solo las geovallas que él creó.**

Rama `sprint-69-b12-geovallas`, desde `main` (`f72fbc3`), en el worktree `C:/Users/andre/np-b12`. Backend de seguridad con **migración**: no se delega a DeepSeek (regla del proyecto); lo escribe un subagente Claude (Opus) con esta especificación y lo revisa Claude.

## Qué implica (una geovalla no guarda quién la creó)

`geofences` solo tiene `device_id`; `geofence_events` y las alertas de geovalla tampoco saben de quién son, y los eventos sobreviven a que se borre su geovalla (por diseño: llevan un `geofence_name` de instantánea). Un filtro no basta: hace falta un dueño en los tres sitios.

## Decisiones

- **Dueño de la geovalla:** nueva columna `geofences.created_by_user_id` (FK a `users`). Crear, listar, editar y borrar una geovalla solo afecta a las del propio tutor; las de otro tutor responden el mismo **404 uniforme** que una inexistente (anti-IDOR).
- **Dueño de lo derivado, con instantánea:** `geofence_events.tutor_user_id` y la columna equivalente en las alertas de geovalla (`alerts`), rellenadas al insertar con el dueño de la geovalla en ese momento. Así un tutor **sigue viendo los eventos de una geovalla suya que borró**, y los de otro nunca.
- **Lecturas filtradas por dueño:** `GET /devices/{id}/geofences`, `.../geofences/events`, `GET /devices/{id}/history` (la parte de geovallas) y `GET /devices/{id}/alerts` (las `GEOFENCE_*`). El resto de la línea de tiempo y de las alertas no cambia. Las formas de las respuestas no cambian (solo hay menos filas).
- **Migración de lo existente:** las geovallas, eventos y alertas ya guardados se asignan al **único tutor con vínculo activo** del dispositivo si hay exactamente uno; si hay ninguno o varios, quedan con dueño `NULL` y **nadie los ve** (no se borra nada: la retención los purgará). Se prefiere ocultar a adivinar el dueño de un dato sensible. Documentar cuántas filas quedan huérfanas.
- **Una geovalla de un tutor que ya no está vinculado deja de evaluarse:** `evaluate_geofence_transitions` solo considera geovallas cuyo creador tiene un vínculo activo con el dispositivo (si no, generaría eventos y alertas que nadie puede ver). Si el tutor vuelve, la geovalla vuelve a evaluarse. No se borra al desvincular.
- Sin cambio de rutas. Android ya no tiene modo tutor (S61): el único cliente es el panel web, cuyas respuestas no cambian.

## Tareas

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [ ] T2 — Migración, modelos, endpoints, servicio de evaluación y tests de integración, **tests primero**. Delegado (**Opus**; backend de seguridad con migración).
- [ ] T3 — `ruff`, suite de integración completa en contenedor, prueba de la migración sobre datos existentes (con una base de pruebas con filas antes de migrar), `security-reviewer` (autorización, 404 uniforme, migración) y PR. Inline.

## Criterios de aceptación

- Un tutor nuevo tras revincular la misma instalación **no** ve geovallas, eventos, línea de tiempo ni alertas de geovalla del tutor anterior, y no puede leer, editar ni borrar sus geovallas (404 uniforme).
- Un tutor ve y conserva lo suyo, incluidos los eventos de una geovalla suya que borró.
- La migración asigna dueño a lo existente solo cuando es inequívoco y no borra nada; es reversible (`downgrade`).
- `ruff` limpio, suite de integración completa y los 8 jobs de CI en verde.

# Sprint 68 — B-09: un tutor nuevo no ve la ubicación anterior a su vínculo

Cierra **B-09** de `docs/tasks.md` (invariante 6, privacidad de un menor): al revincular la misma instalación con otro tutor se reutiliza la fila del dispositivo (`pairing.py:204-233`) y las lecturas de ubicación filtran solo por
`device_id`, así que un tutor nuevo veía ubicaciones guardadas **antes** de su vínculo (dentro de la retención de 7 días). Preexistente; el revisor del Sprint 67 lo volvió a mencionar.

Rama `sprint-68-b09-historial-ubicacion`, desde `main` (`f72fbc3`), en el worktree `C:/Users/andre/np-b09`. Es **backend** de seguridad: lo escribe Claude directamente, no se delega (regla del proyecto).

## Decisiones

- **Regla:** un tutor solo lee informes de ubicación **recibidos desde el inicio de su vínculo activo** (`TutorDevice.linked_at` de la fila con `unlinked_at IS NULL`). Aplica a `GET /devices/{id}/location` (la última) y a
  `GET /devices/{id}/location/history`; el estado que ve un tutor antiguo, vinculado desde hace tiempo, no cambia.
- **Se compara con `received_at`**, la hora del servidor, no con `captured_at`: este es el reloj del dispositivo y puede estar desajustado, y es lo que la persona supervisada controla.
- Si el mismo tutor se desvincula y vuelve a vincular, la fila nueva tiene un `linked_at` nuevo: **no ve lo recibido en el periodo desvinculado**. Es deliberado: la regla es «solo lo recibido mientras el vínculo está activo».
- **No se borra nada** de la tabla: los informes siguen existiendo hasta que los purgue la retención; solo cambia qué puede leer cada tutor.
- Sin cambio de esquema ni de migración, ni de rutas, ni de respuestas (solo hay menos filas). El alcance es la **ubicación** (lo que dice B-09); otros datos del dispositivo (eventos de reglas, alertas, uso) quedan fuera y se anotan aparte.

## Tareas

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [ ] T2 — Tests de integración primero (fallan antes del arreglo) y el arreglo en `location.py`. Inline.
- [ ] T3 — `make test` en contenedor, `ruff`, `security-reviewer` (toca ubicación y autorización) y PR.

## Criterios de aceptación

- Un tutor nuevo, tras revincular la misma instalación, **no** ve informes recibidos antes de su vínculo ni en la última ubicación ni en el historial; sí ve los posteriores.
- El tutor que ya estaba vinculado ve lo mismo que antes.
- `make test` en contenedor en verde, `ruff check app tests alembic` limpio y los 8 jobs de CI.

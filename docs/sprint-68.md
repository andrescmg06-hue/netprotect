# Sprint 68 — B-09: un tutor nuevo no ve la ubicación anterior a su vínculo

Cierra **B-09** de `docs/tasks.md` (invariante 6, privacidad de un menor): al revincular la misma instalación con otro tutor se reutiliza la fila del dispositivo (`pairing.py:204-233`) y las lecturas de ubicación filtran solo por
`device_id`, así que un tutor nuevo veía ubicaciones guardadas **antes** de su vínculo (dentro de la retención de 7 días). Preexistente; el revisor del Sprint 67 lo volvió a mencionar.

Rama `sprint-68-b09-historial-ubicacion`, desde `main` (`f72fbc3`), en el worktree `C:/Users/andre/np-b09`. Es **backend** de seguridad: lo escribe Claude directamente, no se delega (regla del proyecto).

## Decisiones

- **Regla:** un tutor solo lee informes de ubicación **recibidos desde el inicio de su vínculo activo** (`TutorDevice.linked_at` de la fila con `unlinked_at IS NULL`). Aplica a `GET /devices/{id}/location/latest` (la última) y a
  `GET /devices/{id}/location/history`; el estado que ve un tutor antiguo, vinculado desde hace tiempo, no cambia.
- **Se compara con `received_at`**, la hora del servidor, no con `captured_at`: este es el reloj del dispositivo y puede estar desajustado, y es lo que la persona supervisada controla.
- Si el mismo tutor se desvincula y vuelve a vincular, la fila nueva tiene un `linked_at` nuevo: **no ve lo recibido en el periodo desvinculado**. Es deliberado: la regla es «solo lo recibido mientras el vínculo está activo».
- **No se borra nada** de la tabla: los informes siguen existiendo hasta que los purgue la retención; solo cambia qué puede leer cada tutor.
- Sin cambio de esquema ni de migración, ni de rutas, ni de respuestas (solo hay menos filas). El alcance es la **ubicación** (lo que dice B-09): la última y el historial.
- **Fuera de alcance, y por qué:** el `security-reviewer` encontró (MEDIA, preexistente) que un tutor nuevo también ve **datos derivados de la ubicación** anteriores a su vínculo: las coordenadas descifradas del centro de las
  geovallas que creó el tutor anterior (`GET /devices/{id}/geofences`, `geofences.py:90-102`), sus eventos de entrada y salida (`geofences.py:180-193`), la línea de tiempo (`history.py:41-48`) y las alertas `GEOFENCE_ENTER/EXIT` (`alerts.py:50-54`).
  No se amplía este PR porque hay una decisión de producto que no es técnica: **qué pasa con las geovallas de un tutor cuando se va y llega otro** (¿se borran al desvincular?, ¿las ve el nuevo?, ¿empiezan sin ellas?). Queda registrado como **B-12** en `docs/tasks.md`.

## Tareas

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [x] T2 — `280bc50`, `63297a2`. Tests de integración primero, y **fallaron por la razón correcta** (la primera vez fallaron por un error mío en la URL de la última ubicación, `/location/latest`, y se corrigió antes de escribir el arreglo). `_tutor_link_started_at` en `location.py`;
  la última ubicación y el historial solo devuelven informes con `received_at >= TutorDevice.linked_at` del vínculo activo del tutor que consulta. `received_at` sale de `server_default=func.now()` y no lo controla el cliente.
- [x] T3 — `security-reviewer` (Opus): **sin hallazgos ALTA**; el corte es correcto y no se evade desde el dispositivo. Dos hallazgos: **MEDIA** (geovallas, línea de tiempo y alertas sin corte: **B-12**, ver Decisiones) y **BAJA** (`scalar_one()` podía dar un 500 en vez del 404 uniforme si
  una desvinculación se confirmaba entre las dos sentencias): arreglada con `scalar_one_or_none()` y el mismo `404 device_not_found`. Tests reforzados (la última ubicación coincide con el primer elemento del historial en los dos tutores) y uno nuevo: volver a emparejar con el mismo tutor sin desvincular **conserva** su historial.
  `ruff check app tests alembic` limpio y suite de integración completa en contenedor: ver `docs/sprint-68-evidence.md`. Falta el CI de los 8 jobs, que corre con el PR.

## Criterios de aceptación

- Un tutor nuevo, tras revincular la misma instalación, **no** ve informes recibidos antes de su vínculo ni en la última ubicación ni en el historial; sí ve los posteriores.
- El tutor que ya estaba vinculado ve lo mismo que antes.
- `make test` en contenedor en verde, `ruff check app tests alembic` limpio y los 8 jobs de CI.

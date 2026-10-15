# Sprint 69 — evidencia (B-12)

Árbol: `C:/Users/andre/np-b12`, rama `sprint-69-b12-geovallas`. Todo en el contenedor de pruebas (`compose.test.yaml`); no se tocó el stack de desarrollo.

## Los tests fallan sin el cambio

Con el código de `backend/app` anterior a B-12 (y la migración aplicada), `tests/test_geofence_ownership_integration.py` da **6 failed, 1 passed**. El que pasa es `test_a_tutor_keeps_the_events_and_alerts_of_a_geofence_they_deleted`: ese comportamiento ya existía y queda como guarda contra regresiones.
El test de silencios se escribió antes de su arreglo y falló por la razón correcta: el tutor B recibía `GEOFENCE_EXIT:<id de la geovalla de A>` en `GET /alert-silences`.

## Después del cambio

- `ruff check app tests alembic` → `All checks passed!`.
- Suite de integración completa (`build`, `run --rm migrate`, `up --abort-on-container-exit --exit-code-from backend`) → exit 0, **294 passed, 0 failed, 0 skipped** (287 de `main` + 7 nuevos).

## Migración sobre datos previos

Esquema anterior (`c8a1e5b90d34`), filas sembradas con SQL, `upgrade head`. Dueño resultante (alias, no ids):

| Caso | Geovalla / evento / alerta |
|---|---|
| Un único tutor en toda la historia y sigue vinculado (A) | A |
| Evento y alerta de una geovalla ya borrada, mismo caso | A |
| Dos tutores activos | sin dueño |
| Sin tutor activo (vínculo cerrado) | sin dueño |
| Revinculado de E a F (un solo activo hoy) | sin dueño, **no F** |
| Alerta que no es de geovalla | sin dueño (sigue visible para todos los tutores) |

Recuentos de filas idénticos antes y después (no se borró nada), `downgrade` quita las 3 columnas y sus índices, y volver a `upgrade head` repite el resultado. Log del backfill: solo recuentos.
Límite: no se pudo sembrar «dibujada por E» (la columna no existe antes de migrar); lo probado es la regla (vínculo cerrado + otro activo → sin dueño).

## Revisión de seguridad (`security-reviewer`, Opus)

Sin hallazgos ALTA. Dos MEDIA, ambos corregidos:

- **Backfill:** daba al único tutor activo las geovallas de un tutor anterior en dispositivos ya revinculados. Ahora exige un único tutor en toda la historia (`docs/sprint-69.md`).
- **`alert-silences`:** listaba y dejaba borrar los silencios de las alertas de geovalla de otro tutor. Ahora siguen al dueño de la alerta; lo ajeno da el mismo 404.

BAJA, sin cambio: el tope `max_geofences_per_device` ahora es por tutor y dispositivo (el nombre ya no describe la semántica); las geovallas de un tutor desvinculado se conservan sin límite (decisión del sprint).

## Pendiente / límites declarados

- El CI de los 8 jobs corre con el PR.
- No probado con un tutor real ni contra el stack de desarrollo.
- Las geovallas existentes que quedan sin dueño (dispositivos con dos tutores, sin tutor activo o revinculados) **desaparecen para los tutores**: es la decisión conservadora acordada; quien las quiera tiene que volver a crearlas.

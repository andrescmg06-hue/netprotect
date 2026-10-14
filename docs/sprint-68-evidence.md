# Sprint 68 — evidencia (B-09)

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-b09`, rama `sprint-68-b09-historial-ubicacion`. Backend, en el contenedor de pruebas (`compose.test.yaml`); sin tocar el stack de desarrollo.

## Los tests fallaron primero, y por la razón correcta

Los tres tests de integración se escribieron **antes** del arreglo y se ejecutaron en el contenedor de pruebas
(`docker compose -f compose.test.yaml run --rm --no-deps backend pytest -q -rfE -k "new_tutor_does_not_see or each_tutor_of_the_same_device or unlinks_and_relinks" tests/test_location_integration.py`).

1. **Primera pasada, falló por un error mío, no por el bug:** `3 failed`, con `assert 405 == 200` en el ayudante `_read`: usé `GET /devices/{id}/location` para la última ubicación y esa ruta no existe (es `/location/latest`; `/location` solo acepta POST).
   Se corrigió el test y el documento del sprint antes de escribir el arreglo. Si se hubiera escrito el arreglo primero, estos tests habrían fallado igual y habrían parecido un arreglo que no funciona.
2. **Segunda pasada, el fallo correcto** (`3 failed, 17 deselected`), que reproduce el bug con datos reales:
   - tutor nuevo tras revincular la misma instalación: `assert latest["report"] is None` → veía un informe recibido antes de su vínculo;
   - dos tutores del mismo dispositivo: `assert len(second_history) == 1` → `2 == 1`;
   - el mismo tutor que se desvincula y vuelve: `assert len(history) == 1` → `3 == 1`.

## Después del arreglo

`ruff check app tests alembic` (en un contenedor efímero de Python, sin instalar nada en el equipo) → `All checks passed!`.
Suite de integración completa en contenedor (`build`, `run --rm migrate`, `up --abort-on-container-exit --exit-code-from backend db redis backend`) → exit 0, **291 passed, 0 failed, 0 skipped** (287 previos + 4 nuevos).
Los cuatro tests de B-09, ejecutados con nombre (`-rA`), `PASSED`:

- `test_a_new_tutor_does_not_see_locations_received_before_their_link`
- `test_each_tutor_of_the_same_device_sees_from_their_own_link`
- `test_a_tutor_who_relinks_does_not_see_what_arrived_in_between`
- `test_pairing_again_with_the_same_tutor_keeps_their_history`

## Revisión de seguridad (`security-reviewer`, Opus)

Sin hallazgos ALTA. Confirmado: el corte de `latest` y `history` es correcto; `received_at` sale de `server_default=func.now()` y no lo controla el cliente; `linked_at` y `received_at` son del mismo reloj de Postgres (`timestamptz`); dos vínculos activos del mismo tutor son imposibles por
el índice parcial `uq_tutor_devices_active_link`; la autorización (404 uniforme), la auditoría (una fila por lectura, sin coordenadas), la retención y los logs no cambian; el tutor que ya estaba vinculado conserva todo su historial.

- **BAJA, arreglada:** `.scalar_one()` en el ayudante podía lanzar `NoResultFound` (un 500 en vez del 404 uniforme) si el mismo tutor desvinculaba entre las dos sentencias, que van en READ COMMITTED. Ahora `scalar_one_or_none()` y el mismo `404 device_not_found`. No se filtraba nada.
- **MEDIA, no arreglada en este PR → B-12:** un tutor nuevo sigue viendo datos derivados de la ubicación anteriores a su vínculo: las coordenadas descifradas del centro de las geovallas del tutor anterior (`geofences.py:90-102`), sus eventos (`geofences.py:180-193`), la línea de tiempo (`history.py:41-48`) y las alertas
  `GEOFENCE_ENTER/EXIT` (`alerts.py:50-54`), y las geovallas no se borran al desvincular (`pairing.py:276-326`). Hay una decisión de producto de por medio (qué pasa con las geovallas de un tutor cuando se va y llega otro), así que queda registrado en `docs/tasks.md` con los detalles exactos.

Huecos de test que el revisor señaló y no se cubrieron (BAJA): que una lectura vacía del tutor nuevo siga dejando una fila `LOCATION_VIEWED` o `LOCATION_HISTORY_VIEWED`, y la cobertura de geovallas y eventos (que es B-12).

## Pendiente / límites declarados

- **El CI de los 8 jobs** corre con el PR.
- **No probado con un tutor real** ni contra el stack de desarrollo: solo en el contenedor de pruebas.
- **Efecto deliberado:** un tutor que se desvincula y vuelve a vincular no ve lo recibido en el hueco (regla «solo lo recibido mientras el vínculo está activo»). El que vuelve a emparejar sin desvincularse conserva todo.
- Un informe cuya transacción empezó antes del canje del código pero se confirmó después queda con `received_at < linked_at` y el tutor nuevo no lo ve: milisegundos de un dato capturado antes del vínculo; aceptable.

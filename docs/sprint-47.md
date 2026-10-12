# Sprint 47 — Tutor: Mi actividad y auditoría de las consultas de ubicación

Octavo sprint del rediseño Android (`docs/android-redesign/sprints/S47-tutor-mi-actividad-y-auditoria.md`). Mockup 11.
Decisión aplicada: D-10 (a), registrar la consulta de ubicación con solo la acción y el id del dispositivo.

## Qué se hizo

- **Claude — backend** (`f16d857`): cada lectura de la ubicación de un dispositivo por parte de su tutor queda auditada.
  - `GET /devices/{id}/location/latest` registra `LOCATION_VIEWED`.
  - `GET /devices/{id}/location/history` registra `LOCATION_HISTORY_VIEWED`. Hoy ningún cliente lo usa, pero expone
    más lecturas que la última.
  - El registro se escribe **después** de `require_tutor_of_device`: un 404 no deja fila.
  - Solo guarda la acción y el id del dispositivo, nunca coordenadas, precisión ni hora de la lectura: la auditoría no
    debe ser una segunda copia, sin cifrar, de la ubicación del menor.
  - Una consulta es un registro, sin deduplicar.
  - No hizo falta migración: `audit_logs.action` es texto libre de 64 caracteres.
  - 5 tests de integración.
- **Claude — web** (`4427b27`): Mi actividad del panel muestra las acciones en español (antes, los códigos en crudo).
  El código queda en el tooltip y la exportación CSV no cambia.
- **Claude — Android** (`7d578d1`, `6e1a202`):
  - A `AuditLabels` le faltaban 6 de las 33 acciones que el backend registra: las que se pasan en una variable
    (crear y actualizar reglas de app y de categoría, asignar y reasignar la categoría de una app). Ahora están todas.
  - Cada acción tiene una familia (icono y color). Las lecturas sensibles, ubicación y vista remota, van en rojo para
    que destaquen al revisar.
  - El recurso se nombra solo con lo que guarda la auditoría: el nombre del dispositivo si está en la lista del tutor
    y, si no, «Dispositivo», «Alerta», «Geocerca»…
  - Paginación real con «Cargar más»:
    - las entradas desplazadas por acciones nuevas no se repiten;
    - una página sin nada nuevo retira el botón;
    - un fallo conserva lo ya mostrado.
  - Agrupación por el día del tutor.
  - `LegacySections.kt` se borró: la auditoría era lo último que quedaba en él.
  - 7 tests JVM.
- **DeepSeek** (`docs/delegated/done/sprint-47-mi-actividad.md`, `effd1c7`): la pantalla, 10 tests de UI y 5 estados en
  la galería. Además encontró un aviso de lint en el código de Claude (`mutableIntStateOf`), ya corregido.

## Cambios visibles para el tutor

- **Pestaña Actividad:**
  - línea de tiempo por día;
  - hora, icono y color por tipo de acción;
  - etiqueta en español y el dispositivo afectado;
  - «Cargar más».
- **Consultar la ubicación** de un dispositivo, desde el móvil o desde el web, aparece como «Ubicación consultada».
- **Panel web:** Mi actividad en español.

## Decisiones (aprobadas por el dueño)

- Auditar también `/location/history` (`LOCATION_HISTORY_VIEWED`).
- Etiquetas en español también en el web, con los mismos textos que el móvil.
- Texto «Ubicación consultada», en el estilo del resto de las etiquetas.
- `TOKEN_REFRESH` («Renovación de sesión») se muestra: una auditoría no se filtra, y ocultarla rompería la paginación.

## Diferencias con el mockup aceptadas

| Mockup 11 | Qué se hizo | Por qué |
|---|---|---|
| «Límite diario de 2 h para YouTube», horarios, ciudad | Solo la etiqueta y el recurso | La auditoría no guarda detalles |
| «Aplicación bloqueada», «Configuración de alertas» | Solo acciones reales del tutor | No son acciones auditadas |
| «…solo en la app» | «(app y panel web)» | Registra ambas |
| Flecha de volver | Sin flecha | Es una pestaña |

## Pendiente

- La Ubicación del web y las Geocercas del web (que piden la última ubicación para su mapa) también quedan registradas;
  se verificó el endpoint, no esas pantallas en concreto.
- Aspecto en el Galaxy S25 FE.

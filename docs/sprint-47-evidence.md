# Sprint 47 — Evidencia

## 1. Backend — `make test` (contenedor)

```text
backend-1  | 281 passed, 4 warnings in 35.08s
ruff check --no-cache app tests alembic → All checks passed!
pytest -q -m "not integration" → 21 passed, 260 deselected
```

Tests nuevos en `backend/tests/test_location_integration.py`:
- `test_reading_the_latest_location_is_audited_once_without_location_data`: exactamente un `LOCATION_VIEWED`, con
  `resource_type` `device` y el id del dispositivo, sin «4.15», «-73.63», «1500», «latitude», «longitude» ni
  «accuracy».
- `test_every_read_is_one_row_even_when_nothing_was_reported`: dos lecturas, dos filas.
- `test_a_rejected_read_leaves_no_audit_row`: un tutor ajeno recibe 404 en `latest` y `history`, igual que con un
  dispositivo inexistente, y no queda ninguna fila.
- `test_reading_the_location_history_is_audited_separately`.
- `test_the_device_reporting_its_location_is_not_audited`.

El primer intento con `ruff` falló por el permiso de su caché dentro del contenedor (`Failed to create temporary
file … .ruff_cache`), no por el código. Se repitió con `--no-cache`.

## 2. Web — `cd frontend && npm run lint && npm run build`

```text
eslint . --max-warnings=0 → sin salida (limpio)
next build → ○ (Static) prerendered as static content
```

Ningún test E2E depende de los códigos de acción en crudo. `next build` regeneró `frontend/next-env.d.ts` y ese
cambio se descartó.

## 3. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
Parte de Claude (7d578d1): BUILD SUCCESSFUL in 3m 29s · JVM 144, 0 fallos (7 nuevos) · instrumentados 86, 0 fallos · lint 0 errores, 24 avisos
Tras DeepSeek (informe): instrumentados 96, 0 fallos (10 nuevos) · lint 25 avisos (AutoboxingStateCreation en MyActivityState.kt:75, código de Claude)
Tras 6e1a202: BUILD SUCCESSFUL in 6m 28s · instrumentados 96, 0 fallos · lint 0 errores, 24 avisos
```

## 4. Revisión del trabajo de DeepSeek

- **Archivos tocados:** solo los del encargo.
- **Patrones prohibidos:** el grep de `MyActivityScreen.kt` (`Color(0x`, `Instant.now`, `resourceId`,
  `auditActionLabel`, `Clock.format`…) sale sin coincidencias.
- **Capturas** (`%TEMP%\sprint47-gallery\`):
  - dos días;
  - cargando más;
  - error al cargar más;
  - vacío;
  - error.

  Comparadas con el mockup 11: sin detalles inventados, con «(app y panel web)», sin flecha y sin
  exportar, borrar ni editar.

## 5. Prueba real (backend de desarrollo reconstruido con `f16d857`, emulador con la cuenta del tutor)

**Abrir Ubicación del samsung SM-S731B:**

```text
LOCATION_VIEWED | device | 2219e3a5-ec69-44ff-84e5-3c9ce95d5ee4 | extra NULL | 2026-09-30 01:06:40
```

Una sola fila, sin datos de ubicación. Mi actividad la muestra como «Ubicación consultada — samsung SM-S731B».
Abrir Ubicación del emulado también muestra «Ubicación consultada · Google sdk_gphone64_x86_64», arriba del todo.

**«Cargar más» con las 84 acciones reales de la cuenta:**

```text
GET /api/v1/users/me/audit?limit=50&offset=0 HTTP/1.1 200 OK
GET /api/v1/users/me/audit?limit=50&offset=50 HTTP/1.1 200 OK
```

Se ve hasta la primera acción (4 de septiembre de 2026) y el botón desaparece (0 nodos «Cargar más»).

## 6. Revisión de seguridad (hecha por Claude)

- **Registro sin datos de ubicación:** solo acción y dispositivo; lo comprueba un test y se vio en la base real.
- **Orden de autorización:** la escritura va después de la dependencia de autorización. La fila y la lectura se
  confirman en la misma transacción que ya usa `record_audit_event`.
- **Alcance de Mi actividad:** sigue siendo `actor_user_id == usuario actual`; el móvil no añade filtros ni
  exportación.
- **Agente:** el `security-reviewer` no estaba disponible como agente en la sesión.

## 7. No verificado

- Que abrir Ubicación o Geocercas **en el panel web** cree la fila: se verificó el endpoint, que es el mismo.
- Aspecto en el Galaxy S25 FE.

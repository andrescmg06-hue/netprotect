# Sprint 47 — Tutor: Mi actividad (pantalla 11) y auditoría de consultas de ubicación

**Prioridad: MEDIUM** para la pantalla; **HIGH** para la parte de backend si D-10 = a (trazabilidad
sobre datos de menores). **Dueño:** Claude (backend) + DeepSeek (pantalla). **Rama:**
`sprint-47-mi-actividad`. **Mockups:** `11-tutor-mi-actividad.jpg`. **Decisiones:** D-10.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

La pestaña Actividad muestra el registro de auditoría del propio tutor, de solo lectura, agrupado por
día y con etiquetas en español. Si D-10 = a, además cada consulta de la última ubicación de un
dispositivo queda registrada como `LOCATION_VIEWED`.

## Problema que resuelve

G-17 y G-18. Hoy la auditoría se ve como una lista cruda de códigos (`DEVICE_RENAMED …`), y consultar
la ubicación de un menor no deja rastro.

## Dependencias

S44 (ruta Mi actividad). D-10 resuelta. S45 hecho (la pantalla de Ubicación es la que dispara el
registro).

## Alcance

**Claude (backend, solo si D-10 = a), commit propio y antes del encargo:**
1. Registrar `LOCATION_VIEWED` con `record_audit_event` en `GET /devices/{id}/location/latest`,
   después de autorizar, con `resource_type="device"` y el id del dispositivo. **Nunca** coordenadas,
   precisión ni hora de la lectura en el registro.
2. La columna `action` de `backend/app/models/audit_log.py` es `String(64)` sin enum (visto el
   28/09): no debería hacer falta migración. Confirmarlo (y que no hay CHECK en las migraciones); si
   la hubiera, migración nueva con `make revision` (nunca editar una versionada).
3. Tests de integración: la consulta autorizada crea exactamente un registro; la no autorizada (404)
   no crea ninguno; el registro no contiene datos de ubicación.
4. Etiqueta en el web (`AuditPanel.tsx`) y en `ui/format` del móvil: "Consultó la ubicación".
   (El cambio web es de una línea; va en su propio commit.)

**Encargo para DeepSeek — pantalla:**
- "Mi actividad", "Registro de las acciones que has realizado en NetProtect (app y panel web). Esta
  información es de solo lectura." (corrige el mockup, que dice "solo en la app").
- `GET /users/me/audit?limit=50&offset=N`: línea de tiempo por día (`DayHeader`), hora a la izquierda,
  icono y color por familia de acción (vinculación, dispositivo, reglas, geocercas, alertas, sesión),
  título = etiqueta de la acción, subtítulo = recurso (p. ej. el nombre del dispositivo si está en la
  lista cargada; si no, "Dispositivo"). **Sin** detalles que la auditoría no guarda ("2 h para
  YouTube"). "Cargar más" con paginación. Vacío: "Sin acciones registradas todavía." Error.
- `InfoBanner`: "Este registro no se puede editar ni borrar. No incluye la actividad del dispositivo
  supervisado."
- Quitar la sección de auditoría de `LegacySections.kt` (si ya no queda nada, borrar el archivo).

## Fuera de alcance

Exportar (existe en el web, no en el móvil), borrar o editar registros, auditar otras lecturas
(dispositivos, apps: sería ruido).

## Archivos/módulos afectados

Backend (si D-10 = a): `backend/app/api/v1/endpoints/location.py`, tests de ubicación/auditoría,
quizá una migración nueva. Web: `frontend/src/components/AuditPanel.tsx` (etiqueta). Android: nuevo
`feature/tutor/activity/MyActivityScreen.kt`; `ui/format/AuditLabels.kt`, `TutorShell.kt`,
`LegacySections.kt`; `core/network/AuditClient.kt` si hace falta el `offset`.

## Seguridad

La auditoría no debe convertirse en un segundo almacén de ubicación (invariante 7). El endpoint de
lista ya limita a `actor_user_id == usuario actual`. `security-reviewer` obligatorio si hay backend.

## Testing

Backend: los 3 tests del alcance, `make test`. Android JVM: agrupación y familia de acción. UI (si
D-13 = a): lista / vacío / error. Manual: abrir Ubicación en el móvil → aparece "Consultó la
ubicación" en Mi actividad y en el panel web.

## Criterios de aceptación

- [ ] (D-10 = a) `LOCATION_VIEWED` registrado solo en consultas autorizadas, sin datos de ubicación.
- [ ] Pantalla de solo lectura, sin botones de borrar/editar/exportar.
- [ ] Todas las acciones conocidas con etiqueta en español; paginación funciona.
- [ ] Captura revisada contra el mockup 11; `make test` (si backend) y `test assembleDebug lintDebug` verdes.

## Definition of Done

La común del HANDOFF.

## Riesgos

Registrar también las recargas automáticas de la pantalla de ubicación (volumen): solo se registra la
petición real, y la pantalla no debe recargar sola en bucle.

## Decisiones técnicas

Una consulta = un registro, sin deduplicar (la auditoría no se resume).

---

## PROMPT A — Claude Code

```
Sprint 47 del rediseño Android de NetProtect: Mi actividad y, si D-10 = a, auditar la consulta de
ubicación. Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S47-tutor-mi-actividad-y-auditoria.md), mockups/README.md §11, el
mockup 11, D-10. Código: backend/app/api/v1/endpoints/{location,audit}.py, backend/app/services/audit.py,
el modelo de auditoría, AuditClient.kt, LegacySections.kt, ui/format/AuditLabels.kt.

Fases 1–3; espera mi OK. Si D-10 = a, implementa el backend con sus tests (make test vía verifier),
security-reviewer, y commit propio feat(backend) con mi OK; luego la etiqueta web en otro commit.
Después escribe docs/delegated/pending/sprint-47-mi-actividad.md (formato del Sprint 39) con la
pantalla. Commit docs(sprint-47).
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-47-mi-actividad.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-47-mi-actividad.md. Lee su informe; commit de lo suyo tal
cual con mi OK. Fases 6–10 del HANDOFF: diff contra el encargo (sin exportar/borrar, sin detalles
inventados), verifier (test assembleDebug lintDebug), prueba real (abrir Ubicación → aparece en Mi
actividad), captura contra el mockup 11 en %TEMP%\np-sprint47\, docs, /cerrar-sprint 47, informe del
§9. Commits separados con mi OK.
```

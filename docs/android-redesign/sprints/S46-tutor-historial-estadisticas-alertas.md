# Sprint 46 — Tutor: Historial, Estadísticas y Alertas (pantallas 8, 9 y 10)

**Prioridad: HIGH** — alertas es donde el tutor actúa (marcar leída, silenciar) y el mockup describe
mal el cumplimiento. **Dueño:** DeepSeek (pantallas y 2 métodos de cliente) + Claude (revisión).
**Rama:** `sprint-46-historial-alertas`. **Mockups:** `08`, `09`, `10`. **Decisiones:** D-09.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Historial en línea de tiempo por día, Estadísticas con la semántica real y Alertas con filtros,
"Marcar como leída" y "Silenciar" funcionando contra los endpoints existentes.

## Problema que resuelve

G-14, G-15, G-16. El backend ya tiene `POST /devices/{id}/alerts/{alert_id}/read` y
`POST /devices/{id}/alerts/{alert_id}/silence` (autorizados y auditados: `ALERT_READ`,
`ALERT_SILENCED`), pero `AlertsClient` de Android no los llama.

## Dependencias

S44. D-09 resuelta.

## Alcance

- **Historial** (mockup 8, `GET .../history`): agrupado con `DayHeader` ("Hoy 27 de septiembre de
  2026", "Ayer …", fecha). Eventos `APP_RULE`: "Bloqueo de app · {nombre}" (nombre cruzando
  `package_name` con la lista de apps; si no aparece, el paquete) y el motivo con la etiqueta de
  `rule_type_applied`. Eventos `GEOFENCE`: "Entró · {geocerca}" / "Salió · {geocerca}". Hora a la
  derecha. `InfoBanner`: "El historial puede tener retrasos porque los eventos se registran cuando el
  dispositivo envía su siguiente reporte." Vacío: "Sin eventos en el historial."
- **Estadísticas** (mockup 9, `GET .../statistics?period=today|7d|30d`): `SegmentedControl`
  Hoy / 7 días / 30 días. "Apps más usadas" (`top_apps`, barras, duración). "Bloqueos": total por
  motivo real con conteo > 0 (hasta 7 tarjetas pequeñas, etiqueta e icono del motivo).
  "Cumplimiento de límites diarios": **texto correcto** "Días dentro del límite en este periodo." y por
  regla: nombre de app o categoría, "Límite: {n} min/día", barra = `compliance_rate`, "{días
  cumplidos} de {días evaluados} días". Vacíos exactos que ya usa la app: "Sin datos de uso.",
  "Ninguno en este periodo.", "Sin reglas de límite diario.". `InfoBanner` de retraso.
- **Alertas** (mockup 10, `GET .../alerts`): filtros locales "Todas", "No leídas", "Críticas"
  (`CRITICAL` y `HIGH`), "Advertencias" (`WARNING`). Tarjeta: `SeverityBadge`, título con la etiqueta
  del tipo (+ app o geocerca si viene), "Hoy, 3:42 p. m.", "Repetido {n} veces" si `occurrence_count`
  > 1, marca "No leída". Acciones: "Marcar como leída" (oculta si ya lo está) y "Silenciar" con
  `ConfirmDialog` (según D-09: "No volverás a recibir esta alerta de este dispositivo. Puedes quitar el
  silencio desde el panel web."). Tras cada acción, recargar; si falla, mensaje. Vacío: "Sin alertas
  para este dispositivo." `InfoBanner`: "Las alertas se generan cuando el dispositivo envía su
  siguiente reporte y pueden tener un retraso de algunos minutos."
- `AlertsClient`: `markRead(accessToken, deviceId, alertId)` y
  `silence(accessToken, deviceId, alertId, days: Int?)`, copiando el patrón de los métodos
  existentes y usando `authorized` de S41.
- Quitar las tres secciones de `LegacySections.kt`.

## Fuera de alcance

Gestión de silencios en el móvil (D-09 a); nuevos tipos de alerta; exportar; notificaciones push.

## Archivos/módulos afectados

Nuevos `feature/tutor/history/HistoryScreen.kt`, `feature/tutor/statistics/StatisticsScreen.kt`,
`feature/tutor/alerts/AlertsScreen.kt`; modificados `core/network/AlertsClient.kt`,
`feature/tutor/TutorShell.kt`, `feature/tutor/legacy/LegacySections.kt`.

## Trabajo por capa

Backend, web, BD: ninguno (Claude confirma en `backend/app/schemas/alert.py` el cuerpo de
`silence`: `{"days": null}`).

## Seguridad

Acciones de escritura: la autorización es del backend (404 uniforme si la alerta no es del tutor);
la UI no la simula. Sin nombres de apps en logs. `security-reviewer` ligero sobre el cliente.

## Testing

JVM: agrupación por día (cambio de día en la zona del tutor), filtros de alertas, texto de
cumplimiento. UI (si D-13 = a): alertas con datos / vacío / error, diálogo de silenciar. Manual:
marcar una alerta como leída y verlo en el panel web; silenciarla y ver el silencio en el web;
cambiar de periodo en estadísticas.

## Criterios de aceptación

- [ ] "Marcar como leída" y "Silenciar" funcionan y se reflejan en el panel web.
- [ ] El texto de cumplimiento describe días dentro del límite, no porcentaje de tiempo.
- [ ] Solo aparecen tipos y motivos reales, con las etiquetas del web.
- [ ] Nivel de alerta con icono + texto + color.
- [ ] Capturas revisadas contra 08, 09 y 10; `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF.

## Riesgos

Recargar la lista tras cada acción cambia el orden bajo el dedo (mantener la posición). Doble toque
en "Silenciar" → dos peticiones (deshabilitar mientras envía).

## Decisiones técnicas

"Críticas" agrupa `CRITICAL` y `HIGH` (lo que requiere atención); si el dueño prefiere solo
`CRITICAL`, se decide en la fase A.

---

## PROMPT A — Claude Code

```
Sprint 46 del rediseño Android de NetProtect: Historial, Estadísticas y Alertas del tutor.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S46-tutor-historial-estadisticas-alertas.md), mockups/README.md §8–10,
los mockups 08, 09 y 10, D-09, docs/sprint-45.md (errores de DeepSeek a evitar). Código:
LegacySections.kt, clientes History/Statistics/Alerts, TutorShell.kt, ui/format/*Labels.kt,
backend/app/schemas/{history,statistics,alert}.py y backend/app/api/v1/endpoints/alerts.py.

Fases 1–3; espera mi OK (incluida la duda "Críticas = CRITICAL+HIGH"). Escribe
docs/delegated/pending/sprint-46-historial-alertas.md (formato del Sprint 39): tres pantallas, los dos
métodos de AlertsClient con su cuerpo JSON exacto, textos literales (incluido el de cumplimiento
correcto), lista cerrada de archivos, pasos y verificación. Commit docs(sprint-46) con mi OK.
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-46-historial-alertas.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-46-historial-alertas.md. Lee su informe; commit de lo
suyo tal cual con mi OK. Fases 6–10 del HANDOFF:
- Diff: AlertsClient (URL, método, cuerpo, uso de authorized), textos contra mockups/README.md §8–10
  (cumplimiento, tipos de alerta inexistentes), archivos fuera de lista.
- verifier: test assembleDebug lintDebug (+ UI tests).
- Prueba real: marcar leída y silenciar desde el móvil y comprobarlo en el panel web; capturas en
  %TEMP%\np-sprint46\ de las tres pantallas (datos, vacío, error) contra 08, 09 y 10.
- security-reviewer ligero. docs, /cerrar-sprint 46, informe del §9. Commits separados con mi OK.
```

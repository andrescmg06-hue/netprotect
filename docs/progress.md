# Progreso — NetProtect
_Actualizado: 2026-09-28 · Sprint actual: 38_

## En curso
- Exploración de una nueva pantalla de login en una ruta aislada (`frontend/src/app/login-mockup/`),
  con el flujo real de Google Sign-In pero sin reemplazar aún el login real de `/`.
- Reestructuración de este mismo `CLAUDE.md` en `.claude/rules/` + `docs/historial-estado.md` (este
  trabajo) para que el contexto por área cargue solo según la ruta tocada.

## Siguiente paso
- Decidir e implementar cómo `/login-mockup` reemplaza (o no) el login real de la app.

## Terminado recientemente
- Sprint 38 (cierre): consistencia y accesibilidad del rediseño del panel web — `docs/sprint-38-cierre.md`
- Sprint 38: redesign Vista remota y Auditoría — `docs/sprint-38.md`
- Sprint 37: redesign Estadísticas, Alertas y Silenciadas — `docs/sprint-37.md`

## Problemas conocidos
- `/login-mockup` usa Tailwind CSS v4 (scoped solo a esa ruta) y datos de ejemplo fabricados
  ("Dispositivo de Sofía", apps instaladas) — contradice a propósito la regla de "no simular" y el
  "sin Tailwind" del resto del panel, por decisión explícita del dueño del proyecto para esa
  exploración puntual. No usar como referencia para otras pantallas.

## Decisiones vigentes que se suelen olvidar
- Filtrado web por VPN: evaluado y pospuesto (Sprint 9), no construido.
- Sesión web en `sessionStorage`, no cookie HttpOnly (Sprint 3).
- Rate limiting falla abierto si Redis cae (Sprint 21).
- Mapa de geocercas: esquema SVG, sin servidor de teselas (Sprints 31–38).

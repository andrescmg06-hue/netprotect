# Progreso — NetProtect
_Actualizado: 2026-09-28 · Sprint actual: 40_

## En curso
- Reestructuración de este mismo `CLAUDE.md` en `.claude/rules/` + `docs/historial-estado.md` (este
  trabajo) para que el contexto por área cargue solo según la ruta tocada.

## Siguiente paso
- S41 (sesión y errores, solo Claude Code): token de la UI que caduca a los 15 min y modelo de errores.
  Conviene hacerlo con Opus (posible carrera entre refresh concurrentes). Luego S42, primer encargo
  para DeepSeek. Plan: `docs/android-redesign/SPRINT_PLAN.md`.

## Terminado recientemente
- Sprint 40: línea base, 14 decisiones del rediseño Android resueltas, inventario y OpenCode
  configurado — `docs/sprint-40.md` (+ `docs/sprint-40-evidence.md`)
- Sprint 39: el login del mockup reemplaza al login real de `/`; fuera Tailwind y la ruta
  `/login-mockup` — `docs/sprint-39.md` (+ `docs/sprint-39-evidence.md`)
- Sprint 38 (cierre): consistencia y accesibilidad del rediseño del panel web — `docs/sprint-38-cierre.md`
- Sprint 38: redesign Vista remota y Auditoría — `docs/sprint-38.md`
- Sprint 37: redesign Estadísticas, Alertas y Silenciadas — `docs/sprint-37.md`

## Decisiones vigentes que se suelen olvidar
- Filtrado web por VPN: evaluado y pospuesto (Sprint 9), no construido.
- Sesión web en `sessionStorage`, no cookie HttpOnly (Sprint 3).
- Rate limiting falla abierto si Redis cae (Sprint 21).
- Mapa de geocercas: esquema SVG, sin servidor de teselas (Sprints 31–38).

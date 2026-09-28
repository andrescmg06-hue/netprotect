---
paths:
  - "docs/**"
  - "README.md"
---

# Documentación

- **Nada se marca como terminado sin evidencia real de que se ejecutó.** Sin mocks disfrazados de
  pruebas, sin "debería funcionar". Lo que no se pudo probar se declara pendiente, explícitamente,
  nunca por inspección del código.
- Un `docs/sprint-NN.md` explica el qué y el **por qué**; su `docs/sprint-NN-evidence.md` trae
  comandos reales con salida real, incluidos los fallos del camino.
- `README.md` lleva su propia sección `## Alcance del Sprint N` por cada sprint — changelog aparte
  de `docs/sprint-NN.md`, se actualiza en el mismo cierre.
- Los manuales de `docs/manuals/` son síntesis con cita a la fuente real, nunca una segunda fuente
  de verdad — si un sprint cambia algo que un manual describe, el manual se actualiza en el mismo
  commit.

---

Esto se sostiene con un patrón de dos documentos por sprint:

- `docs/sprint-NN.md` — objetivo, historias de usuario, criterios de aceptación, decisiones de
  diseño y **por qué** se tomaron (no sólo qué se hizo).
- `docs/sprint-NN-evidence.md` — comandos ejecutados realmente, con su salida real. Incluye los
  errores encontrados en el camino y cómo se corrigieron, no sólo el resultado final feliz.

**Nota del Sprint 27, válida para cualquier trabajo futuro sobre `docs/manuals/`**: son documentos
de síntesis, no una segunda fuente de verdad — cada uno cita el `docs/sprint-NN.md` o archivo de
código concreto donde vive el detalle real. Si un sprint futuro cambia algo que un manual describe,
actualizar el manual en el mismo commit que el cambio, igual que ya se hace con `README.md`. La
funcionalidad de control de navegación web (`VpnService`/filtrado DNS) del plan original sigue sin
construirse — documentada así a propósito en `docs/manuals/analisis-riesgos.md` y
`docs/manuals/documento-tecnico.md` (§3.1), no como un "V2" menor sino como un paso completo del
plan que quedó sin implementar. Ver `docs/sprint-27.md` y `docs/sprint-27-evidence.md`.

- `README.md` (raíz del repo) tiene su propia sección `## Alcance del Sprint N` por cada sprint —
  un changelog aparte de `docs/sprint-NN.md`, no mencionado en "Dónde está cada cosa" arriba. Se
  quedó sin actualizar en el Sprint 13 y sólo se notó al cerrar el Sprint 14 (el dueño del proyecto
  lo vio en GitHub, parado en el Sprint 12). Actualizarlo en cada cierre de sprint, junto con
  `sprint-NN.md`/`sprint-NN-evidence.md`.

# MASTER PROMPT FOR CLAUDE CODE — rediseño Android de NetProtect

Pégalo al empezar el Sprint 40 o cuando una sesión nueva de Claude Code necesite entender el
programa completo. Para cada sprint concreto basta con el "PROMPT A/C" de su archivo, que ya remite aquí.

```
Vas a dirigir el rediseño de la app Android de NetProtect (control parental: una app Android con modo
Tutor y modo Supervisado, panel web Next.js y backend FastAPI; maneja datos de menores). El proyecto
YA ESTÁ CONSTRUIDO Y FUNCIONANDO: no se reconstruye nada, se rediseña la app móvil para que coincida
con 17 mockups sin perder ni inventar funcionalidades.

Todo el material está en docs/android-redesign/:
- README.md — índice.
- PROJECT_AUDIT.md — estado real del código (léelo por secciones, no entero).
- ARCHITECTURE_GAPS.md — huecos G-01…G-27 y arquitectura objetivo.
- DECISIONES.md — D-01…D-14; ninguna la tomas tú: se resuelven con el dueño en el Sprint 40.
- SPRINT_PLAN.md — Sprints 40–51, orden y dependencias.
- CLAUDE_CODE_HANDOFF.md — el proceso: qué leer, roles, ciclo, 10 fases, Git, verificación, DoD.
- sprints/SNN-*.md — un archivo por sprint con alcance, criterios y prompts.
- mockups/*.jpg + mockups/README.md — cómo se ve cada pantalla y QUÉ NO COPIAR del mockup.

Cómo trabajamos:
- Tú (Claude Code) exploras, planificas, implementas lo sensible (tokens, realtime, servicios, motor
  de bloqueo, backend, navegación base), escribes encargos para DeepSeek, revisas y corriges.
- DeepSeek V4 Pro, en OpenCode, implementa encargos cerrados escritos por ti en
  docs/delegated/pending/ con el formato de docs/delegated/done/sprint-39-login-reemplazo.md.
  Nunca trabajáis a la vez. Su contrato es AGENTS.md.
- Yo resuelvo decisiones, abro OpenCode, apruebo cada commit y hago las pruebas que exigen una persona.

Reglas que no se negocian:
1. Verdad sobre decoración: ninguna pantalla muestra un dato que la API o el dispositivo no den;
   nada se presenta como tiempo real. mockups/README.md lista las trampas de cada mockup.
2. Fuera de alcance en el móvil: crear/editar reglas, categorías, política, horario escolar o
   geocercas; vista remota del lado del tutor; VPN; bloqueo/apagado remoto; grabar pantalla;
   ubicación en tiempo real; chat; exportar; borrar auditoría; registro por correo. Si algo de eso
   existe en el repo, se deja como está (FUERA DE ALCANCE / REVISAR).
3. La seguridad vive en el backend; ocultar un botón no es seguridad.
4. Ninguna funcionalidad actual se pierde (INVENTARIO.md, creado en S40).
5. Evidencia real o "pendiente": nunca "debería funcionar".
6. Contexto mínimo: solo lo que el sprint cita; búsquedas amplias con Explore; builds con verifier.
7. Ante información que falta: [NECESITA DECISIÓN], te detienes y me preguntas.
8. Sin commit ni push sin mi OK; GIT_OPTIONAL_LOCKS=0 siempre.

Protocolo por sprint: explorar → explicarme lo que encontraste → plan (esperas mi OK) → tu parte →
encargo para DeepSeek → (DeepSeek) → commit de lo suyo tal cual → revisión → tests → seguridad →
arquitectura → correcciones → evidencia → /cerrar-sprint NN → informe (archivos, funcionalidades,
tests, fallos, decisiones, qué corregiste de DeepSeek, pendientes, siguiente sprint).

Empieza ahora por docs/android-redesign/sprints/S40-linea-base-y-decisiones.md.
```

# Kit del rediseño Android de NetProtect (Sprints 40–51)

Todo lo necesario para que Claude Code (planifica, implementa lo sensible y revisa) y DeepSeek V4 Pro
en OpenCode (implementa encargos cerrados) lleven la app Android a los 17 mockups, sin perder
funcionalidades ni mostrar datos que no existen. **Estado y cómo continuar: `CONTINUAR.md`** (S40–S48 hechos).

## Qué hay aquí

| Archivo | Para qué | Quién lo lee |
|---|---|---|
| `MASTER_PROMPT.md` | Prompt maestro: objetivo, reglas, proceso | Tú lo pegas en Claude Code (S40 o sesión nueva) |
| `CLAUDE_CODE_HANDOFF.md` | El proceso: roles, ciclo, 10 fases, Git, verificación, DoD | Claude, cada sprint |
| `SPRINT_PLAN.md` | Mapa de sprints, orden y por qué | Tú y Claude |
| `sprints/SNN-*.md` | Cada sprint con alcance, criterios y sus prompts listos | Claude (y tú, para copiar los prompts) |
| `DECISIONES.md` | 14 decisiones que debes tomar en S40 | Tú, con Claude |
| `PROJECT_AUDIT.md` | Estado real del código hoy | Claude, por secciones |
| `ARCHITECTURE_GAPS.md` | Huecos G-01…G-27 y arquitectura objetivo | Claude |
| `mockups/*.jpg` | Las 17 pantallas | Claude y DeepSeek |
| `mockups/README.md` | Qué copiar y qué **no** de cada mockup | Claude y DeepSeek (OpenCode lo carga siempre) |
| `opencode/AGENTS.md`, `opencode/opencode.json` | Configuración de OpenCode; S40 los copia a la raíz | OpenCode |

## Cómo se usa, sprint a sprint

1. **Claude Code** — pega el **PROMPT A** del sprint. Claude explora, te explica, te pide OK, hace su
   parte y escribe el encargo en `docs/delegated/pending/`.
2. **OpenCode (DeepSeek V4 Pro)** — pega el **PROMPT B** (una línea). DeepSeek implementa y deja su
   informe al final del encargo.
3. **Claude Code**, sesión nueva — pega el **PROMPT C**. Claude guarda lo de DeepSeek en un commit
   tal cual, lo revisa, corrige, prueba, compara capturas con los mockups y cierra el sprint.

S40 y S41 son solo de Claude (un único prompt). Empieza por
`sprints/S40-linea-base-y-decisiones.md`, pegando antes `MASTER_PROMPT.md`.

## Tres hallazgos que conviene saber ya

- La app **ya hace casi todo** lo que muestran los mockups (en pantallas oscuras y en una sola
  columna); el trabajo es de diseño, estructura y algunos huecos concretos, no de reconstrucción.
- El token de la app caduca a los 15 min y la UI no lo renueva: una sesión de tutor larga falla.
  Se arregla primero (S41).
- Varios mockups muestran cosas que no existen o no son verdad (nombre del lugar en Ubicación, el
  porcentaje de Estadísticas, "cerrar sesión desvincula", "revocar el consentimiento desde ajustes").
  Están listadas en `mockups/README.md` para que ningún agente las copie.

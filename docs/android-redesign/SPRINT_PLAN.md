# SPRINT_PLAN — rediseño de la app Android (Sprints 40–51)

La numeración continúa la del repo (el último cerrado es el 39), para que `/cerrar-sprint NN`,
`docs/sprint-NN.md` y el README sigan funcionando igual.

## Mapa

| Sprint | Nombre | Pantallas | Prioridad y motivo | Depende de | Claude | DeepSeek |
|---|---|---|---|---|---|---|
| S40 | Línea base, decisiones y OpenCode | — | CRITICAL: sin decisiones ni línea base no se puede empezar | — | Todo | — |
| S41 | Sesión y errores | — | CRITICAL: seguridad de sesión; bloquea todo uso > 15 min | S40 | Todo | — |
| S42 | Sistema de diseño y navegación | — | CRITICAL: base de las 17 | S41 | Navegación, deps de test, revisión | Tema, recursos, componentes, formateadores (2 encargos) |
| S43 | Login y Elegir modo | 1, 2 | HIGH: puerta de la app; riesgo bajo | S42 | B-03, revisión | Pantallas |
| S44 | Tutor: estructura, Inicio, Detalle | 3, 4 | HIGH: desbloquea S45–47; acción destructiva sin confirmar | S42 | Shell, legado, cliente | Pantallas |
| S45 | Apps, Ubicación, Geocercas | 5, 6, 7 | HIGH: datos de un menor | S44 | Revisión + seguridad | Pantallas |
| S46 | Historial, Estadísticas, Alertas | 8, 9, 10 | HIGH: acciones del tutor; semántica corregida | S44 | Revisión | Pantallas + 2 métodos de cliente |
| S47 | Mi actividad (+ auditar ubicación) | 11 | MEDIUM (pantalla) / HIGH (backend, si D-10) | S44, S45 | Backend | Pantalla |
| S48 | Supervisado: estructura, Vincular, Vinculado, Permisos | 12, 13, 14 | HIGH (partirlo mal detiene servicios) | S42 | Shell con servicios, B-01 | Pantallas |
| S49 | App bloqueada | 16 | HIGH: la ve el menor a diario | S42 | Datos al overlay, tema | Diseño de variantes |
| S50 | Consentimiento, Servicios, reconexión | 15, 17 | HIGH: consentimiento y transparencia; riesgo técnico alto | S48 | Realtime, registro de servicios, enrutado | Maquetación |
| S51 | Endurecimiento e integración | todas | HIGH: condición para llegar a `main` | todos | Todo salvo arreglos mecánicos | Arreglos de accesibilidad |

**Orden de ejecución:** 40 → 41 → 42 → 43 → 44 → 45 → 46 → 47 → 48 → 49 → 50 → 51. S48–S50 no
dependen de S43–S47 (solo de S42); si hace falta priorizar el supervisado, pueden adelantarse sin
romper nada.

## Por qué este orden y no "una pantalla por sprint"

1. **Lo transversal primero.** La sesión que caduca (S41) y el sistema de diseño (S42) afectan a las
   17 pantallas; hacerlos después obligaría a rehacerlas.
2. **La estructura antes que las hojas.** S44 y S48 crean los contenedores (navegación del tutor,
   shell del supervisado con los servicios) y mueven lo existente sin perder nada; las pantallas de
   datos se enchufan luego.
3. **Agrupación por fuente de datos y riesgo.** 5–7 son datos de ubicación y uso (misma revisión de
   privacidad); 8–10 son eventos y acciones; 11 lleva backend; 15 y 17 comparten realtime y servicios.
4. **Lo sensible nunca se delega.** Tokens, realtime, servicios, reglas de bloqueo y backend son de
   Claude; DeepSeek recibe presentación y código que copia un patrón ya escrito.

## Qué se descartó de la propuesta inicial

- **Sprints para funciones fuera de alcance**: ninguno (editor de reglas, geocercas, VPN, etc.).
- **Agentes `explorer`, `android`, `frontend`, `backend`, `database`, `qa`, `reviewer`**: no se crean.
  El harness ya tiene `Explore` (integrado), `verifier` y `security-reviewer`; el "implementador
  barato" es DeepSeek, y el "arquitecto/revisor" es la sesión principal de Claude Code.
- **Sprint de backend propio**: no hace falta; los dos cambios de backend (B-01 y, si se aprueba,
  `LOCATION_VIEWED`) van en el sprint de la pantalla que los necesita, en su propio commit.

## Git

Ver `CLAUDE_CODE_HANDOFF.md` §6. Resumen: `main` → `android-redesign` → `sprint-NN-<slug>`; por
sprint, commits `docs` (plan y encargo) → `feat` de Claude si lo hay → `feat` de DeepSeek tal cual →
`fix` de la revisión → `docs` de cierre; backend siempre en commit aparte; PR con CI verde.

## Estimación honesta

Entre 1 y 3 sesiones de Claude Code por sprint más una de DeepSeek. Las pruebas que requieren una
persona (login real de Google, aceptar la captura de pantalla) quedan declaradas como pendientes
(H-01, H-02) y no bloquean el cierre técnico.

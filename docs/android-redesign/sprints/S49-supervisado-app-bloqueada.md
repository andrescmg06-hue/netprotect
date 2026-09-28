# Sprint 49 — Supervisado: pantalla "App bloqueada" en sus 7 variantes (pantalla 16)

**Prioridad: HIGH** — es la pantalla que el menor ve a diario; tono y verdad importan. Riesgo técnico
medio: el contenido se dibuja dentro de una ventana superpuesta. **Dueño:** Claude (datos que llegan
a la pantalla, tema en el overlay) + DeepSeek (diseño de las variantes). **Rama:**
`sprint-49-app-bloqueada`. **Mockups:** `16-supervisado-app-bloqueada.jpg`. **Decisiones:** D-11.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

`BlockScreenContent` (usado por `BlockOverlayController` y por `BlockScreenActivity`) muestra el diseño
del mockup para los 7 motivos reales de `BlockReason`, con icono y nombre de la app leídos del propio
dispositivo, la categoría real si la app tiene una asignada y, según D-11, una frase de reinicio solo
si es verdad. "Ir al inicio" hace lo mismo que hoy.

## Problema que resuelve

G-23. Hoy es una pantalla oscura con el nombre y el motivo; el overlay no usa ningún tema.

## Dependencias

S42 (tema y componentes). D-11 resuelta. Independiente de S48.

## Alcance

**Claude (antes de delegar):**
1. Llevar hasta `BlockScreenContent` la categoría asignada de la app (ya la conoce el motor por
   `CategoryAssignment`), añadiendo un parámetro opcional en `BlockOverlayController.show` y en el
   `Intent` de `BlockScreenActivity`. Sin cambiar cuándo ni por qué se bloquea.
2. Verificar en `RuleEvaluator`/`AppInventoryCollector` cómo se reinician los contadores: día natural
   local para el diario, semana de lunes a domingo para el semanal. Solo si se confirma, el encargo
   incluye "Podrás volver a usarla mañana." y "… a partir del lunes.".
3. Envolver la composición del overlay y de la actividad en `NetProtectTheme`.

**Encargo para DeepSeek:**
- `feature/supervised/block/BlockPresentation.kt`: función pura `BlockReason → (icono, color de
  acento, título "APP BLOQUEADA", mensaje, pista opcional)`. Mensajes: **los textos actuales de
  `reasonText`** (no cambian: ya son respetuosos y verdaderos). Iconos: `lock` con distintivo —
  BLOCK `user`, DAILY_LIMIT `clock`, WEEKLY_LIMIT `calendar`, SCHEDULE `moon`, CATEGORY `layout-grid`,
  SCHOOL_MODE `graduation-cap`, DEFAULT_POLICY `shield-check`.
- `BlockScreenContent` rediseñado (mockup 16): marca pequeña, ilustración del candado con el
  distintivo, "APP BLOQUEADA", mensaje, tarjeta de la app (icono local con `PackageManager`, nombre,
  categoría si existe con las etiquetas de `CategoryLabels`), tarjeta de pista con el color del motivo
  (texto de la pista o, si no hay, una frase neutra por motivo que fije Claude en el encargo), botón
  "Ir al inicio". Mantener la nota honesta actual ("Este bloqueo cubre la pantalla, pero no puede
  impedir…") en letra pequeña.
- Test JVM: los 7 motivos tienen presentación completa; ninguno queda sin mapear si se añade uno al enum.

## Fuera de alcance

Cambiar reglas, prioridades o apps protegidas (`ProtectedPackages`: nunca se bloquean Teléfono,
Ajustes, el launcher ni NetProtect); cambiar el mecanismo de overlay/notificación de pantalla
completa; mostrar franjas horarias concretas (D-11 b).

## Archivos/módulos afectados

Claude: `core/rules/BlockOverlayController.kt`, `core/rules/RuleEnforcementService.kt` (solo pasar la
categoría), `feature/supervised/BlockScreenActivity.kt` (extra del `Intent` y tema). DeepSeek: nuevo
`feature/supervised/block/BlockPresentation.kt`, `BlockScreenContent` (moverlo a
`feature/supervised/block/BlockScreenContent.kt` y dejar la actividad llamándolo), test en
`src/test/.../block/`.

## Trabajo por capa

Backend, web, BD: ninguno.

## Seguridad

Sin nombres de apps en logs nuevos. El `Intent` a `BlockScreenActivity` sigue sin exportar. Ningún
botón que "desbloquee" o "pida tiempo" (no existe esa función). `security-reviewer` ligero.

## Testing

JVM: presentación de los 7 motivos; `RuleEvaluatorTest` intacto y verde. Manual en el teléfono:
crear desde el panel web una regla de cada tipo (bloqueo, límite diario bajo, semanal, horario,
categoría, horario escolar, política "solo aprobadas") y capturar las 7 variantes; comprobar que
Teléfono y Ajustes nunca se bloquean y que "Ir al inicio" funciona desde el overlay.

## Criterios de aceptación

- [ ] 7 capturas reales, una por motivo, revisadas contra el mockup 16.
- [ ] Los mensajes son los actuales; ninguna pista falsa (solo las verificadas).
- [ ] Categoría con el nombre real o ausente; nunca inventada.
- [ ] Overlay y actividad con el tema; "Ir al inicio" igual que antes.
- [ ] `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF.

## Riesgos

Composición en overlay sin `LifecycleOwner`/tema (ya resuelto el primero en
`BlockOverlayController`; el segundo lo hace Claude). Iconos de apps enormes o sin fondo (tamaño fijo
y recorte).

## Decisiones técnicas

La presentación es una función pura para que los 7 casos se prueben sin dispositivo.

---

## PROMPT A — Claude Code

```
Sprint 49 del rediseño Android de NetProtect: pantalla "App bloqueada" en 7 variantes.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S49-supervisado-app-bloqueada.md), mockups/README.md §16, el mockup 16,
D-11. Código: core/rules/{RuleEvaluator,RuleEnforcementService,BlockOverlayController,AppRule,ProtectedPackages}.kt,
core/inventory/AppInventoryCollector.kt, feature/supervised/BlockScreenActivity.kt, RuleEvaluatorTest.

Fases 1–3; espera mi OK. Implementa tu parte (categoría hasta la pantalla, verificación de los
reinicios diario/semanal, tema en overlay y actividad). verifier; RuleEvaluatorTest verde. Commit
propio con mi OK. Escribe docs/delegated/pending/sprint-49-app-bloqueada.md (formato del Sprint 39),
con los textos exactos (los de reasonText + pistas verificadas + frases neutras) y la lista cerrada de
archivos (DeepSeek no toca core/rules). Commit docs(sprint-49).
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-49-app-bloqueada.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-49-app-bloqueada.md. Lee su informe; commit de lo suyo tal
cual con mi OK. Fases 6–10 del HANDOFF: diff (¿tocó core/rules?, ¿cambió mensajes?, ¿algún botón de
desbloqueo?), verifier, y guíame para crear desde el panel web las 7 reglas y capturar cada variante
en el teléfono (%TEMP%\np-sprint49\); compáralas con el mockup 16. Comprueba Teléfono y Ajustes.
security-reviewer ligero. docs, /cerrar-sprint 49, informe del §9. Commits separados con mi OK.
```

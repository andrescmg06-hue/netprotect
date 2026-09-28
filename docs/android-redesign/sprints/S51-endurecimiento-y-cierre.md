# Sprint 51 — Endurecimiento, accesibilidad, regresión e integración a `main`

**Prioridad: HIGH** — sin este sprint el rediseño no se integra: es donde se demuestra que nada se
perdió. **Dueño:** Claude (auditoría y decisiones) + DeepSeek (arreglos mecánicos de accesibilidad
que Claude liste). **Rama:** `sprint-51-cierre`. **Mockups:** los 17. **Decisiones:** D-02 (revisión),
D-14. `{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

La app rediseñada completa pasa: inventario de S40 sin pérdidas, accesibilidad básica, rotación,
modo sin conexión, build de release, CI; y se integra `android-redesign` en `main` con un PR verde.

## Problema que resuelve

G-27 y el riesgo acumulado de 10 sprints de cambios en dos agentes.

## Dependencias

S40–S50 cerrados.

## Alcance

**Claude:**
1. Código muerto: `LegacySections.kt`, restos de `TutorScreen.kt`/`SupervisedScreen.kt`, colores
   literales (`grep -rn "Color(0x" {pkg} --include=*.kt` fuera de `ui/theme` = 0).
2. Regresión: recorrer `docs/android-redesign/INVENTARIO.md` punto por punto en el teléfono y el
   emulador; cada punto con resultado.
3. Accesibilidad: TalkBack en las 17 pantallas (orden y etiquetas), escala de fuente 130 % y 200 %
   sin cortes, objetivos ≥ 48 dp, contraste AA de badges (textos `*-text` de `DESIGN.md`). Lista de
   arreglos → encargo para DeepSeek si son mecánicos.
4. Rotación y muerte del proceso ("No conservar actividades" en opciones de desarrollador): se
   conserva la pantalla; D-02 se revisa con esa evidencia.
5. Sin conexión: modo avión en cada pantalla de datos → `ErrorState` "Sin conexión" + "Reintentar".
6. Release: `./gradlew assembleRelease` (R8 activo) e instalar; la galería de depuración no está.
7. D-14: `lintDebug` en el job `android` de CI si se aprobó (commit propio, `.github/workflows/ci.yml`).
8. Capturas finales de las 17 pantallas en `%TEMP%\np-sprint51\` y comparación con los mockups;
   las diferencias aceptadas (por verdad sobre decoración) se listan en `docs/sprint-51.md`.
9. Docs: `docs/android/capability-matrix.md` solo si hubo decisión de plataforma nueva; `README.md`
   (alcance); `docs/progress.md`; cerrar G-01…G-27 en `tasks.md`; actualizar `.claude/rules/android.md`
   con las lecciones "válidas para el futuro" del rediseño (tema, navegación, efectos en el shell).
10. PR `android-redesign` → `main` con los 8 jobs de CI verdes (merge lo hace el dueño).

**Encargo para DeepSeek:** solo la lista cerrada de arreglos mecánicos de accesibilidad y textos que
Claude encuentre en el punto 3 (etiquetas, tamaños, contrastes), archivo por archivo.

## Fuera de alcance

Funcionalidades nuevas; refactors que no sean retirar código muerto; publicar en Google Play.

## Archivos/módulos afectados

Los de `feature/`, `ui/` según hallazgos; `.github/workflows/ci.yml` (D-14); docs listados.

## Trabajo por capa

Backend: `make test` final. Web: `npm run lint && npm run build` (por la etiqueta de S47).

## Seguridad

`security-reviewer` sobre el diff completo `main...android-redesign`. Comprobar: sin tokens ni datos de
menores en logs (`grep -rn "Log\." {pkg}`), galería ausente en release, ningún componente exportado
nuevo salvo la galería de debug.

## Testing

Todo: `./gradlew test assembleDebug lintDebug connectedDebugAndroidTest assembleRelease`,
`make test`, frontend lint/build, CI completo.

## Criterios de aceptación

- [ ] Inventario de S40: 100 % de puntos presentes o sustituidos a propósito (y documentado).
- [ ] 0 colores literales fuera de `ui/theme`.
- [ ] TalkBack y fuente 200 % sin bloqueos en las 17 pantallas.
- [ ] Rotación y muerte de proceso sin perder la pantalla.
- [ ] Release instala y funciona.
- [ ] `security-reviewer` sin hallazgos ALTA sobre todo el rediseño.
- [ ] PR a `main` con CI verde.

## Definition of Done

La común del HANDOFF y el PR listo para que el dueño haga merge.

## Riesgos

R8 elimina algo usado por reflexión (probar release en el teléfono). Descubrir una regresión grande
tarde: se abre un sprint de corrección, no se tapa.

## Decisiones técnicas

Lo que no pase aquí no se integra: el PR espera.

---

## PROMPT A — Claude Code

```
Sprint 51 del rediseño Android de NetProtect: endurecimiento, accesibilidad, regresión e integración.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S51-endurecimiento-y-cierre.md), docs/android-redesign/INVENTARIO.md,
D-02 y D-14, y los docs/sprint-4[0-9].md y sprint-50.md solo en su sección de pendientes.

Fases 1–3; espera mi OK. Haz los puntos 1–7 del alcance (guíame en lo que requiere el teléfono);
genera la lista de arreglos de accesibilidad y, si son mecánicos, escribe
docs/delegated/pending/sprint-51-accesibilidad.md (formato del Sprint 39) con la lista cerrada.
Commits separados con mi OK.
```

## PROMPT B — OpenCode con DeepSeek V4 Pro (solo si hay encargo)

```
Ejecuta el encargo docs/delegated/pending/sprint-51-accesibilidad.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
Cierre del Sprint 51 y del rediseño Android. Si DeepSeek hizo el encargo de accesibilidad, commit de
lo suyo tal cual con mi OK y revísalo. Luego: puntos 8–10 del alcance, verifier con todo
(test assembleDebug lintDebug connectedDebugAndroidTest assembleRelease + make test + frontend),
security-reviewer sobre main...android-redesign, docs, /cerrar-sprint 51, informe del §9 con un
resumen de todo el rediseño (qué se hizo, qué corrigió Claude de DeepSeek por sprint, pendientes
humanos). Prepara el PR android-redesign → main; el merge lo hago yo.
```

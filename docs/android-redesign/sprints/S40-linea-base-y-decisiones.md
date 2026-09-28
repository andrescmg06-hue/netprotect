# Sprint 40 — Línea base, decisiones y preparación de OpenCode

**Prioridad: CRITICAL** — nada del rediseño puede empezar con decisiones abiertas ni sin saber qué
funciona hoy. **Dueño:** solo Claude Code (no se delega). **Rama:** según D-06. **Mockups:** ninguno.

## Objetivo

Al terminar: las 14 decisiones de `DECISIONES.md` resueltas por el dueño y registradas; la línea base
de tests medida; un inventario escrito de todo lo que la app hace hoy (para comprobar en S51 que no se
perdió nada); OpenCode configurado (`AGENTS.md` + `opencode.json` en la raíz); ramas preparadas.

## Problema que resuelve

Sin decisiones, cada sprint las tomaría a medias y distinto. Sin línea base, un fallo previo se
confundiría con una regresión. Sin `AGENTS.md`, DeepSeek hoy lee `CLAUDE.md` (que le habla de
agentes que en OpenCode no existen) y ninguna regla de Android.

## Dependencias

Ninguna. Árbol limpio (hoy solo `docs/planning/prompt-mockups-android.md` está sin versionar).

## Alcance

1. Versionar el kit: `docs/android-redesign/` y `docs/planning/prompt-mockups-android.md`.
2. D-06: proponer y, si se aprueba, abrir el PR `sprint-31-design-system` → `main`; tras CI verde,
   crear `android-redesign` desde `main`.
3. Línea base vía `verifier`: backend (`ruff`, `pytest -m "not integration"`, `make test`) y
   Android (`./gradlew test assembleDebug lintDebug`). Fallos previos → `docs/tasks.md` con P0–P3.
4. **Inventario de comportamiento actual**: leer `HomeScreen.kt`, `TutorScreen.kt`,
   `SupervisedScreen.kt`, `BlockScreenActivity.kt` y escribir `docs/android-redesign/INVENTARIO.md`:
   cada acción, estado y mensaje visible, por pantalla actual (lista de comprobación, no prosa).
5. Resolver `DECISIONES.md` con el dueño, una a una (pregunta con opciones y la recomendación).
6. Instalar `docs/android-redesign/opencode/AGENTS.md` → `AGENTS.md` y
   `docs/android-redesign/opencode/opencode.json` → `opencode.json` (raíz). Pedir al dueño que abra
   OpenCode y confirme que el modelo es DeepSeek V4 Pro y que carga las instrucciones.
7. `docs/tasks.md`: sección "Rediseño Android (S40–S51)" con G-01…G-27 y los hallazgos nuevos
   (desvincular sin confirmación, errores silenciados, token de 15 min, posible carrera de refresh).
8. `docs/progress.md`: sprint actual 40, siguiente paso S41.

## Fuera de alcance

Cualquier cambio en `mobile/`, `backend/`, `frontend/`, `infra/`. Arreglar la deuda que aparezca.

## Archivos/módulos afectados

`AGENTS.md` (nuevo), `opencode.json` (nuevo), `docs/android-redesign/DECISIONES.md`,
`docs/android-redesign/INVENTARIO.md` (nuevo), `docs/tasks.md`, `docs/progress.md`,
`docs/sprint-40.md` y `-evidence.md` (nuevos), `README.md` (`## Alcance del Sprint 40`).

## Trabajo por capa

- **Backend / Web / Base de datos:** ninguno.
- **Android:** solo lectura (inventario).
- **Configuración de agentes:** `AGENTS.md`, `opencode.json`.

## Seguridad

`opencode.json` debe negar lectura de `.env*`, `secrets/`, `local.properties`,
`google-services.json`, y edición de `.claude/`, migraciones, `CLAUDE.md`/`AGENTS.md`; negar
`git commit/push/reset`. Comprobar que la sintaxis de patrones (`*` incluye `/`) funciona
pidiéndole a OpenCode que lea `.env`: debe negarse.

## Testing

Solo la línea base (no se escribe código).

## Criterios de aceptación

- [ ] `DECISIONES.md`: 14 de 14 en `Resuelta`, con opción y fecha.
- [ ] Línea base registrada con salidas reales en `docs/sprint-40-evidence.md`.
- [ ] `INVENTARIO.md` cubre las 4 pantallas actuales y la pantalla de bloqueo.
- [ ] `AGENTS.md` y `opencode.json` en la raíz; prueba de lectura de `.env` negada, con evidencia.
- [ ] Ramas según D-06.
- [ ] `tasks.md` y `progress.md` actualizados.

## Definition of Done

Criterios cumplidos, commits separados (kit / configuración de agentes / docs del sprint), nada de
código de la app modificado (`git diff --stat -- mobile backend frontend` vacío).

## Riesgos

CI rojo al integrar en `main` (no mezclar arreglos aquí: se anota y decide el dueño). OneDrive
bloqueando git (`GIT_OPTIONAL_LOCKS=0`).

## Decisiones técnicas

Ninguna propia: este sprint existe para que el dueño decida.

---

## PROMPT PARA CLAUDE CODE (único; este sprint no se delega)

```
Sprint 40 del rediseño Android de NetProtect. Trabajo SOLO de documentación y configuración:
no modifiques nada en mobile/, backend/, frontend/ ni infra/.

Lee, en este orden y nada más: docs/android-redesign/MASTER_PROMPT.md,
docs/android-redesign/CLAUDE_CODE_HANDOFF.md, docs/android-redesign/sprints/S40-linea-base-y-decisiones.md,
docs/android-redesign/DECISIONES.md.

Sigue el protocolo de 10 fases del HANDOFF. Pasos:
1. GIT_OPTIONAL_LOCKS=0 git status. Si hay algo más que docs/android-redesign/ y
   docs/planning/prompt-mockups-android.md sin versionar, detente y pregúntame.
   Propón el commit "docs: kit de rediseño Android (mockups, auditoría, plan)" y espera mi OK.
2. Explícame D-06 y pregúntame. Si elijo (a): prepara el PR de sprint-31-design-system → main
   (gh pr create), espera CI verde, y crea android-redesign desde main. No hagas merge sin mí.
3. Delega a verifier la línea base completa (backend rápido + make test + Android test assembleDebug
   lintDebug). Registra salidas reales. Fallos previos → docs/tasks.md con P0–P3, sin arreglarlos.
4. Lee HomeScreen.kt, TutorScreen.kt, SupervisedScreen.kt y BlockScreenActivity.kt y escribe
   docs/android-redesign/INVENTARIO.md: por pantalla actual, lista de comprobación de cada acción,
   estado (cargando/vacío/error), mensaje visible y llamada a la API.
5. Recorre DECISIONES.md de D-01 a D-14 conmigo: para cada una, 3 líneas de contexto, opciones y tu
   recomendación, y pregúntame. Registra mi respuesta (Estado: Resuelta — opción — fecha).
6. Copia docs/android-redesign/opencode/AGENTS.md → AGENTS.md y opencode.json → opencode.json en la
   raíz. Pídeme que abra OpenCode con DeepSeek V4 Pro y que le pida leer .env: debe negarse. Anota el
   resultado que te diga.
7. Actualiza docs/tasks.md (sección "Rediseño Android (S40–S51)" con G-01…G-27 de
   ARCHITECTURE_GAPS.md y los hallazgos nuevos), docs/progress.md, docs/sprint-40.md,
   docs/sprint-40-evidence.md y README (## Alcance del Sprint 40).
8. Informe final según el §9 del HANDOFF y propuesta de commits separados. No hagas commit sin mi OK.
Detente y pregúntame ante cualquier cosa que no esté en estos documentos.
```

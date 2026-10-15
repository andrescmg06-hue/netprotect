# Sprint 72 — Orden del repositorio (sin cambios funcionales)

Rama `sprint-72-orden` (desde `main` en `efbfec5`, tras fusionar #32–#35). Estado: en curso.

El dueño pidió que no vuelva a subirse un diseño equivocado de la web o de la app y que el código quede
ordenado, **quitando solo lo que no hace falta, sin afectar el funcionamiento ni la seguridad**.

## Causa del «diseño equivocado» y lo que ya se resolvió (antes de este sprint, sin código)

- El panel rediseñado no estaba en `main`: vivía en #14, #32 y `web-redesign` a la vez. `main` local iba
  244 commits atrás y el stack de desarrollo se construía desde un worktree aparte (`np-dev`, detached).
- Hecho el 14/10/2026: #32, #33, #34 y #35 fusionados con CI 8/8; borradas 38 ramas remotas y 35 locales
  (todas comprobadas con `git cherry`: ningún parche fuera de `main`); quitados los 9 worktrees; el
  checkout principal está en `main` y el stack `netprotect-dev` se construye desde él (comprobado con la
  etiqueta `com.docker.compose.project.working_dir`). Los archivos sueltos de `docs/` (paquete original del
  rediseño web, 147 archivos, 116 idénticos a `main`) se respaldaron fuera del repo y se quitaron.

## Inventario (dos mapeos de solo lectura, verificados con grep en `src`, tests y manifiesto)

Código limpio en general: sin TODO/FIXME/console.log/print de depuración, sin pantallas v1 ni componentes de
un diseño anterior montados, todas las dependencias en uso. Sobra:

- **Android:** 26 constantes de `NpIcons` sin uso y sus 25 drawables + `logo_full.png` (restos del modo
  tutor); `Motion.SLOW_MS`, `StaggerMs`, `EnterOffset`; `WelcomeStore.isSeen()`;
  `LocationPermission.isPreciseGranted()`; comentario obsoleto en `RuleEnforcementClient.kt:28`;
  `mobile/README.md` dice que la app «soportará Tutor y Supervisado».
- **Web:** `ui/ChartCard` (+ CSS y línea del barrel) sin uso; `public/brand/card-twilight.jpg` sin uso (W-04).
- **Repo:** `scripts/check.sh` y `check.ps1` (comprobaciones del Sprint 1, sin referencias);
  `docs/android-redesign/opencode/` duplica `AGENTS.md` y `opencode.json` de la raíz.
- **Documentos que contradicen el estado:** `docs/android-redesign/README.md` («S40–S48 hechos»),
  `docs/redesign/CONTINUAR.md` (estado al 04/10, W-06); kits de diseño ya ejecutados sin aviso de que son
  históricos (mockups v1 del modo tutor Android incluidos).

## Decisiones

- **Se queda** (no es código muerto): la galería `/design-system` y `MetricCard` (vitrina del sistema de
  diseño), las funciones `@Preview`, `seed_test_session.py`, `third_party_licenses`, `compose.web.yaml`.
- **No se tocan:** tipos exportados de `apiClient.ts` (contrato con la API), avisos UseKtx en `core/auth` y
  `EnforcementLiveness` (zona sensible, beneficio nulo), migraciones, `frontend/AGENTS.md`.
- **Los kits de diseño históricos no se borran** (los citan los `sprint-NN.md`): se marcan como históricos y
  se deja escrito en un solo sitio cuál es el diseño vigente: el código de `main`, con `DESIGN.md` para la
  web y `docs/android-redesign/supervisado-v2/` para la app.

## Tareas

- [x] T1 — Android: quitar iconos, drawables y símbolos sin uso; comentario y README. Delegado (writer). Commit `77764d1`.
- [x] T2 — Web: quitar `ChartCard` y `card-twilight.jpg` (cierra W-04). Delegado (mismo writer). Commit `cbcbee8`.
- [x] T3 — Repo: quitar `scripts/check.*` y `docs/android-redesign/opencode/`. Delegado (mismo writer). Commit `f31e8fd`.
- [x] T4 — Diseño vigente en un solo sitio: avisos de histórico en los kits, `CONTINUAR.md` web al día
  (W-06), README de `android-redesign`, y la regla en `.claude/rules/`. Delegado (mismo writer). Commit `13a7a99`.
- [ ] T5 — Verificación (`verifier`: frontend lint+build, Android test+assembleDebug+lintDebug),
  `security-reviewer` (toca `core/auth/WelcomeStore.kt`), `progress.md` y PR. Inline.

## Criterios de aceptación

1. Ningún comportamiento cambia: mismos tests en verde, `assembleDebug`, `lintDebug` y `next build` sin errores nuevos.
2. Cada borrado tiene su grep de cero usos registrado en la evidencia.
3. Un solo documento dice cuál es el diseño vigente de la web y de la app, y los kits viejos lo remiten a él.

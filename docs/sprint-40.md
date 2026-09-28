# Sprint 40 — Línea base, decisiones y preparación de OpenCode

Primer sprint del rediseño Android (`docs/android-redesign/`). Solo documentación y configuración:
`git diff --stat -- mobile backend frontend infra` está vacío.

## Qué se hizo

- **Kit versionado** (`f913713`): `docs/android-redesign/` y `docs/planning/prompt-mockups-android.md`.
- **14 decisiones resueltas** por el dueño el 2026-09-28, todas con la opción recomendada:
  navegación con rutas propias (D-01), sin ViewModel (D-02), iconos Lucide como VectorDrawable
  (D-03), Inter empaquetada (D-04), Ubicación sin nombre de lugar ni mapa (D-05), PR a `main` y rama
  `android-redesign` (D-06), icono local o monograma (D-07), cuatro pestañas con "Más" (D-08),
  silenciar indefinido (D-09), auditar `LOCATION_VIEWED` (D-10), bloqueo sin franja horaria (D-11),
  consentimiento con casilla (D-12), tests de UI de Compose (D-13), `lintDebug` en CI (D-14).
- **Línea base** medida (ver evidencia): Android verde (35 tests, lint 0 errores/27 avisos); la
  integración y el resto los cubre el CI del PR #2.
- **Inventario** de comportamiento actual: `docs/android-redesign/INVENTARIO.md` (268 líneas).
- **OpenCode**: `AGENTS.md` y `opencode.json` en la raíz.
- **`docs/tasks.md`**: sección "Rediseño Android (S40–S51)" con G-01…G-27 y L-01…L-06 (hallazgos de
  la línea base).

## Decisiones

- **`adb` no está en el PATH.** Se documentó la ruta completa del SDK en `AGENTS.md` y se añadió
  `*platform-tools/adb.exe *` a los permisos de `opencode.json`, en lugar de tocar el PATH de Windows.
- **La línea base de backend y de integración no se midió en local**: `backend/.venv` no tiene
  `prometheus_client` (L-05) y `make test` habría parado el backend de desarrollo con poca memoria
  libre. Lo cubre el CI del PR #2 (8 jobs).
- **El PR ya existía** (#2, borrador). Se reutilizó en vez de abrir otro. El PR #1 (`sprint-28-turn`)
  queda redundante al fusionar #2: sus commits ya viajan en la misma rama.

## Pendiente

- Que el dueño abra OpenCode con DeepSeek V4 Pro, le pida leer `.env` y confirme que se niega
  (prueba de `opencode.json`); sin eso, la sintaxis de patrones de permisos no está verificada.
- Merge del PR #2 a `main` (lo decide el dueño) y creación de `android-redesign` desde `main`.

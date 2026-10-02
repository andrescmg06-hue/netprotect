# Sprint 51 — Endurecimiento, accesibilidad, regresión e integración a `main`

Duodécimo y último sprint del rediseño Android (`docs/android-redesign/sprints/S51-endurecimiento-y-cierre.md`).
Decisiones: D-02 (revisión con evidencia), D-14 (a): `lintDebug` en el job `android` de CI. Sin funcionalidades
nuevas: aquí se demuestra que nada se perdió.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = un escritor (subagente) o DeepSeek, con el motivo.

- [ ] T1 — D-14: `lintDebug` en el job `android` de CI. Inline (una línea en `ci.yml`).
- [ ] T2 — Último color literal fuera de `ui/theme` (`feature/home/HomeScreen.kt:171`) → token del tema.
- [ ] T3 — L-02: `dataExtractionRules` en el manifest; no se respalda nada de la app (datos de menores).
- [ ] T4 — Texto a 200 %: duración en `StatisticsScreen.kt:215` (sin elipsis, ancho fijo) y nombre de la app en
  `BlockScreenContent.kt:119` (una sola línea).
- [ ] T5 — Release con R8: `./gradlew assembleRelease`, reglas `keep` solo si algo falla; instalar y probar.
- [ ] T6 — Accesibilidad: decidir qué iconos con `contentDescription = null` llevan significado → encargo
  `docs/delegated/pending/sprint-51-accesibilidad.md` para DeepSeek (incluye `AppIcon.kt:38`).
- [ ] T7 — Sin conexión: modo avión en Inicio del tutor y pantallas del supervisado (las otras 9 ya usan `ErrorState`).
- [ ] T8 — Rotación y muerte del proceso; D-02 se revisa con esa evidencia.
- [ ] T9 — Regresión contra `docs/android-redesign/INVENTARIO.md` y capturas de las 17 pantallas.
- [ ] T10 — Pruebas humanas: release en el Galaxy S25 FE, TalkBack, fuente 130/200 %, H-02.
- [ ] T11 — `security-reviewer` sobre `main...android-redesign`, docs, cierre G-01…G-27 y PR `android-redesign` → `main`.

## Hallazgos de la auditoría (fase 1)

- El código legado (`LegacySections.kt`, `TutorScreen.kt`, `SupervisedScreen.kt`) ya no existe.
- `proguard-rules.pro` no tiene reglas y R8 está activo en release: riesgo con Room, WebRTC y receivers.
- Solo hay dos `Log.w` (errores SDP en `ScreenShareService`); no registran tokens ni datos del menor.
- No hay ViewModel (D-02 a): los datos se recargan al girar; la pila de navegación sí se conserva.

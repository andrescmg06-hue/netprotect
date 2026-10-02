# Progreso — NetProtect
_Actualizado: 2026-10-02 · Sprint actual: 51_

## En curso
- **S51 (endurecimiento y cierre del rediseño Android)**, rama `sprint-51-cierre` (worktree `C:/Users/andre/np-s51`,
  con push). Hecho: T1–T9 y la revisión de seguridad (0 ALTA). Detalle y commits: `docs/sprint-51.md`.

## Siguiente paso
1. **Dueño:** fusionar PR #12 (S50) y PR #13 (entorno Gentle AI + D-05) en `android-redesign`; comprobar que las
   tres `.env.*.example` tienen `TURN_SHARED_SECRET`, `TURN_URLS` y `TURN_CREDENTIAL_TTL_SECONDS`.
2. **T10 en el Galaxy S25 FE** (con el dueño): la build `minified` (release con R8), TalkBack, fuente 130/200 %, H-02 (vista
   remota real, cierra WebRTC con R8) y capturas 01, 02, 12, 14, 15, 16.
3. **Resto de T11:** PR `sprint-51-cierre` → `android-redesign`; cerrar G-01…G-27 en `tasks.md`; lecciones en
   `.claude/rules/android.md`; `/cerrar-sprint 51`; PR `android-redesign` → `main` (merge del dueño).
   **Para retomar: `docs/android-redesign/CONTINUAR.md`.**

## Terminado recientemente
- Sprint 51 (en curso): bug «todas las apps Desinstalada» (`optString` y JSON null) corregido; selecciones del
  tutor sobreviven al giro (D-02 revisada); textos al 200 %; `lintDebug` en CI; build `minified` para probar R8;
  regresión del inventario sin pérdidas — `docs/sprint-51.md`
- Sprint 50: consentimiento de vista remota como pantalla propia, «Estado de NetProtect» y reconexión del
  canal realtime (B-02) — `docs/sprint-50.md`
- Sprint 49: pantalla «App bloqueada» en sus 7 variantes, con la categoría real y solo textos verdaderos;
  verificada con bloqueos reales de YouTube en el emulador — `docs/sprint-49.md`
- Sprint 48: modo supervisado con shell que conserva los servicios al navegar; Vincular, Vinculado y
  Permisos rediseñadas; B-01 (`/devices/me` con varios dispositivos) corregido — `docs/sprint-48.md`
- Sprint 47: Mi actividad rediseñada con paginación; cada consulta de ubicación queda auditada
  (`LOCATION_VIEWED`, sin coordenadas); etiquetas de auditoría en español también en el web — `docs/sprint-47.md`
- Sprint 46: Historial, Estadísticas y Alertas rediseñadas; «Marcar leída» y «Silenciar» desde el móvil,
  verificados contra el panel web — `docs/sprint-46.md`
- Sprint 45: Apps, Ubicación y Geocercas rediseñadas sin coordenadas ni lugar; "Dentro de" solo con certeza —
  `docs/sprint-45.md`
- Sprint 44: modo tutor con barra inferior, Inicio, Dispositivos, Detalle (desvincular con confirmación) y Más —
  `docs/sprint-44.md`
- Sprint 43: pantallas de carga, inicio de sesión y elegir modo rediseñadas (mockups 1–2) — `docs/sprint-43.md`
- Sprint 42: sistema de diseño Android (tema, Inter, iconos Lucide, formateadores, 23 componentes,
  galería de depuración) y esqueleto de navegación — `docs/sprint-42.md`
- Sprint 41: la sesión Android sobrevive a los 15 min (renovación única por proceso), errores en
  español, y arreglo de latidos con 500 por alertas duplicadas — `docs/sprint-41.md`
- Sprint 40: línea base, 14 decisiones del rediseño Android resueltas, inventario y OpenCode
  configurado — `docs/sprint-40.md` (+ `docs/sprint-40-evidence.md`)
- Sprint 39: el login del mockup reemplaza al login real de `/`; fuera Tailwind y la ruta
  `/login-mockup` — `docs/sprint-39.md` (+ `docs/sprint-39-evidence.md`)
- Sprint 38 (cierre): consistencia y accesibilidad del rediseño del panel web — `docs/sprint-38-cierre.md`
- Sprint 38: redesign Vista remota y Auditoría — `docs/sprint-38.md`
- Sprint 37: redesign Estadísticas, Alertas y Silenciadas — `docs/sprint-37.md`

## Decisiones vigentes que se suelen olvidar
- Filtrado web por VPN: evaluado y pospuesto (Sprint 9), no construido.
- Sesión web en `sessionStorage`, no cookie HttpOnly (Sprint 3).
- Rate limiting falla abierto si Redis cae (Sprint 21).
- Mapa de geocercas: esquema SVG, sin servidor de teselas (Sprints 31–38).

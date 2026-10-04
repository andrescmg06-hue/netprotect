# Progreso — NetProtect
_Actualizado: 2026-10-04 · Sprint actual: 60 (rediseño web, cierre)_

## En curso
- **S60 (rediseño del panel web, cierre)**, rama `sprint-60-rediseno-web-cierre`, creada desde `fix/s54-auth-log-sin-pii`
  (`e9e4f69`). Cadena de ramas real: `sprint-52…` → `sprint-53…` → `sprint-54…` → `fix/s54-auth-log-sin-pii` → `sprint-60…`.
  T1 (documentación de S54–S59) escrita, pendiente de revisión y commit. Falta accesibilidad (contraste, foco, favicon), imágenes, estados vacíos,
  e2e y verificación (T2–T8). Detalle: `docs/sprint-60.md`.
- **S54–S59 (rediseño del panel web, marco y las 16 vistas)**: código hecho por Cristian el 04/10/2026 (`35af89d`,
  `c76ceea`, `bdfa6e8`, `8be5abd`, `7b6b117`, `7ec38c7`/`ccde5cf`, y `eb2db05` con las correcciones y el cambio de serif a
  Newsreader). Documentación al día desde el S60: `docs/sprint-54.md` … `docs/sprint-59.md` y sus `-evidence.md`; la
  evidencia visual está concentrada en `docs/redesign/s59/LEEME.md` (e2e «1 passed», 18 capturas en el repo, tomadas antes
  de `eb2db05`). **Pendiente del dueño**: revisión visual con sesión real de Google (T5 del S54), Vista remota con un
  teléfono real, revisión de seguridad de S57 y S59 (`security-reviewer`, sin registro), recaptura tras `eb2db05` y
  `lint`/`tsc` de S56–S59 sin salida guardada.
- **S53 (rediseño del panel web, base visual)**, rama `sprint-53-rediseno-web-base-visual` (sale de la del S52). Hecho:
  línea base de 34 capturas, tokens nuevos, serif, primitivas con las mismas props, fotos, `DESIGN.md` reescrito y
  galería; lint, tipos y build en verde. Detalle: `docs/sprint-53.md` y `docs/sprint-53-evidence.md`. Pendiente:
  `/cerrar-sprint 53` (lo lanza el dueño), D1 (serif, hoy Newsreader, abierta para el diseñador) y D5 (logo).
- **S52 (rediseño del panel web, Fase 0, sin código)**, rama `sprint-52-rediseno-web-fase-0`. Informe
  `docs/redesign/fase-0-informe.md` **aprobado por el dueño el 04/10/2026**; evidencia en `docs/sprint-52-evidence.md`.
  Pendiente: T8 (capturas base, necesitan sesión de tutor), tipografía serif y logo oficial (decisiones D1 y D5 del
  informe) y `/cerrar-sprint 52`. Siguiente: S53 (base visual: tokens, serif, primitivas, `DESIGN.md`).
- **S51 (endurecimiento y cierre del rediseño Android)**, rama `sprint-51-cierre` (worktree `C:/Users/andre/np-s51`,
  con push). Hecho: T1–T9 y la revisión de seguridad (0 ALTA). Detalle y commits: `docs/sprint-51.md`.

## Siguiente paso
**Rediseño editorial del panel web (S52–S60):** plan en `docs/redesign/PLAN_SPRINTS.md`; para retomar,
`docs/redesign/CONTINUAR.md`. S52–S59 hechos (S54–S59 con código y documentación al día); **S60 en curso**
(`docs/sprint-60.md`). `web-redesign` solo trae los documentos del S52; el pase a `main` lo decide el dueño. Es
independiente del cierre del S51 de abajo.

Hecho el 02/10/2026: PR #12 (S50) y PR #13 (entorno + D-05) fusionados en `android-redesign` (`d0724b0`); esa rama ya
está fusionada dentro de `sprint-51-cierre`; plantillas `.env.*.example` comprobadas (sin hueco, ver `docs/sprint-51.md`).

1. **T10 con el Galaxy S25 FE real** (con el dueño), checklist completa en `docs/sprint-51.md`: build `minified`,
   TalkBack, fuente 130/200 %, **H-02** (vista remota real con R8; cierra T5) y capturas 01, 02, 12, 14, 15, 16.
   Opus. Aplazado a propósito hasta tener el teléfono.
2. **Resto de T11:** cerrar G-01…G-27 en `tasks.md` y lecciones en `.claude/rules/android.md` (apto para DeepSeek);
   `/cerrar-sprint 51`; PR `sprint-51-cierre` → `android-redesign`; PR `android-redesign` → `main` (merge del dueño).
3. **Después del S51:** el proyecto solo corre en demo local (backend en la PC). Para presentarlo sin la PC falta el
   Paso 25 (dominio, hosting, certificados, Firebase/FCM, ID de cliente de Google, TURN real) y firma de release.

**Para retomar: `docs/android-redesign/CONTINUAR.md`.**

## Terminado recientemente
- Sprints 54–59 (código, 04/10/2026): marco (login, sidebar navy, header, banda) y recomposición editorial de las 16
  vistas del panel web, con solo datos reales; serif Newsreader; e2e en verde sobre el conjunto —
  `docs/sprint-54.md` … `docs/sprint-59.md`
- Sprint 53: base visual del rediseño web (tokens, serif, primitivas, `DESIGN.md`, galería) —
  `docs/sprint-53.md` (+ `docs/sprint-53-evidence.md`)
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

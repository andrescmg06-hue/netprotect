# Progreso — NetProtect
_Actualizado: 2026-10-14 · Sprint actual: 67 (cierre del rediseño v2 del supervisado, a falta de PR y CI)_

## En curso
- **S67 (casos reales y cierre del rediseño v2)**, rama `sprint-67-casos-reales` (worktree `C:/Users/andre/np-s67`). Hecho T1–T6: bienvenida de 2 pasos, aviso de batería,
  «Se desactivó un permiso», pasada de letra 2.0, prueba G-19, revisión de seguridad global (0 ALTA; 2 BAJA arregladas). Pendiente: PR de S67 y los dos últimos pasos de abajo.
  Detalle, commits y evidencia: `docs/sprint-67.md` y `docs/sprint-67-evidence.md`.

## Siguiente paso
1. **PR `sprint-67-casos-reales` → `android-redesign`** con los 8 jobs de CI en verde.
2. **Fusionar `main` en `android-redesign`** (`main` tiene el merge del PR #18 que `android-redesign` no tiene: 1 commit frente a 77) y **PR `android-redesign` → `main`**
   (S61–S67); el merge a `main` lo hace el dueño. Después, `/cerrar-sprint 67`.
3. **Pruebas que solo puede hacer una persona** (declaradas, no hechas): Galaxy S25 FE con One UI (TalkBack y Accessibility Scanner, letra al 200 % en el teléfono, «quitar animaciones», pasos de batería,
   quitar cada permiso en Ajustes y recuperarlo); tutor real desde el panel web (vista remota de punta a punta, «Atrás» y caducidad de S65, desvincular de S64); la prueba con cinco personas
   (`docs/android-redesign/supervisado-v2/05_PRUEBA_USUARIOS.md`).
4. **Para presentarlo sin la PC:** el proyecto solo corre en demo local (backend en la PC). Falta el Paso 25 (dominio, hosting, certificados, Firebase/FCM, ID de cliente de Google, TURN real)
   y firma de release; la release real de producción no se ha instalado.

**Para retomar: `docs/android-redesign/CONTINUAR.md`.**

## Terminado recientemente
- Sprint 67 (a falta de PR): bienvenida de 2 pasos con el contenido de Privacidad, aviso de batería sin pedir la exención, aviso «Se desactivó un permiso» (con la línea base guardada en el teléfono);
  el permiso quitado de verdad en el emulador, G-19, y la revisión de seguridad global — `docs/sprint-67.md`
- Sprint 66 (PR #24): bloqueo en v2, Privacidad (solo lo que el código respalda) y Detalle de permiso con pasos reales; dos hallazgos medidos en el emulador antes de corregir el texto — `docs/sprint-66.md`
- Sprint 65 (PR #23): consentimiento en 3 pasos con cuenta atrás, Estado, B2 y B5 (el tutor se entera de «Atrás» y de la caducidad); 1 MEDIA y 4 BAJA arregladas — `docs/sprint-65.md`
- Sprint 64 (PR #22): Inicio y Permisos, B1 (el teléfono detecta la desvinculación) y G-19 — `docs/sprint-64.md`
- Sprint 63 (PR #21): Acceso, Vincular y «Más ajustes» con cierre de sesión confirmado — `docs/sprint-63.md`
- Sprint 62 (PR #20): sistema visual v2 — `docs/sprint-62.md`
- Sprint 61 (PR #19): la app es solo del supervisado — `docs/sprint-61.md`
- Sprints 40–51 del rediseño Android: fusionados en `main` (PR #18)
- Sprint 51 (fusionado en `main` con el PR #18): bug «todas las apps Desinstalada» (`optString` y JSON null)
  corregido; selecciones del tutor sobreviven al giro (D-02 revisada); textos al 200 %; `lintDebug` en CI; build
  `minified` para probar R8; vista remota real en el Galaxy con R8 (H-02); regresión del inventario sin pérdidas;
  seguridad sin ALTA — `docs/sprint-51.md`
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

# Progreso — NetProtect
_Actualizado: 2026-10-16 · En `main`: Android S61–S67 + B-14, límite y ubicación precisa; web S52–S60 + B-05; backend B-09, B-12, B-13; preparación de producción S71; orden del repositorio S72 (PR #36). S73 está terminado en local en su rama, sin PR. En GitHub solo existe la rama `main`._

## En curso
- **S73 (el bloqueo no se salta y la protección sigue con la app cerrada)**, rama `sprint-73-bloqueo-segundo-plano`
  (desde `main` en `e5b0a81`): terminado en local el 16/10/2026, **sin PR todavía**. Probado en el emulador Pixel 8
  (API 36), no en el Redmi. **T4 sigue abierta solo por** la prueba en el Redmi con HyperOS y el aviso instantáneo
  `rules_changed` desde el panel. Detalle: `docs/sprint-73.md` y `docs/sprint-73-evidence.md`.

## Cómo se trabaja desde ahora (para no volver a usar una versión equivocada)
- **Una sola fuente:** el checkout principal en `main` actualizado. El stack de desarrollo `netprotect-dev` se
  construye desde ese checkout (`docker compose -p netprotect-dev --env-file .env -f compose.yaml up -d --build`).
- Cada trabajo nuevo sale de `main` en su rama, entra por PR con CI en verde y la rama se borra al fusionar.
- Diseño: lo que cuenta es el código de `main`; ver `docs/diseno-vigente.md`.

## Siguiente paso
1. **Cerrar T4 del S73 en el Redmi Note 13 (HyperOS 2.0):** conceder «Inicio automático» y batería «Sin restricciones»,
   y comprobar re-entrada rápida a una app bloqueada, app deslizada en Recientes, reinicio y «Permitir todo el tiempo».
   Después, desde el panel del tutor: el aviso instantáneo `rules_changed` (regla nueva con la app ya abierta) y que
   el dispositivo siga «Conectado» con la app cerrada (H-07, H-08).
2. **Decidir el push y el PR de la rama `sprint-73-bloqueo-segundo-plano`** (no se hace sin que lo pida el dueño).
3. **Prueba en un teléfono real** (sin desplegar): APK `minified` con `NETPROTECT_API_BASE_URL=http://localhost:8000`,
   `adb install -r` y `adb reverse tcp:8000 tcp:8000`. Comprobar la ubicación «Precisa», el límite diario que bloquea
   estando dentro de la app y el panel rediseñado con una sesión real de Google (H-05, H-06).
4. **Despliegue:** seguir `docs/go-live-checklist.md` (S71). Antes: añadir `FCM_SERVICE_ACCOUNT_SECRET_FILE` a
   `.env.production.example` (a mano, por el dueño) y los cuatro hallazgos BAJA anotados en `docs/sprint-71.md`.
5. **Pruebas que solo puede hacer una persona** (declaradas, no hechas): TalkBack, letra al 200 % y permisos en un
   teléfono real; vista remota de punta a punta con un tutor real; la prueba con cinco personas
   (`docs/android-redesign/supervisado-v2/05_PRUEBA_USUARIOS.md`).
6. Abierto en el backlog (`docs/tasks.md`): D-01 (filtrado web, contradice RF-07 del informe), B-11, W-01, L-06, y lo
   que dejó el S73 (B-17, D-09 a D-11; B-14 ampliado).

## Terminado recientemente
- Sprint 73 (en local, 16/10/2026, sin PR): el bloqueo vuelve a bloquear tras «Ir al inicio», una regla nueva bloquea la
  app que ya está abierta, y bloqueo, latido y ubicación siguen y reviven con la app cerrada (servicios que sobreviven
  a la pantalla, `START_STICKY`, arranque al encender, «Permitir todo el tiempo» por decisión del dueño). Verificado en
  el emulador Pixel 8, 322 tests JVM; sin probar en el Redmi con HyperOS ni `rules_changed` desde el panel —
  `docs/sprint-73.md` (+ `docs/sprint-73-evidence.md`)
- Sprint 72 (PR #36, en `main`): orden del repositorio sin cambios funcionales; `docs/diseno-vigente.md` — `docs/sprint-72.md`
- 14/10/2026: consolidación en `main` (PR #32 rediseño web + B-05, #33 B-14 + límite + ubicación precisa, #34 B-13,
  #35 Sprint 71), CI 8/8 en cada uno; borradas todas las ramas salvo `main` tras comprobar que no tenían nada fuera de él
- Sprint 71 (PR #35): producción no arranca con valores de ejemplo, release firmado por variables, healthchecks y lista
  de salida única `docs/go-live-checklist.md` — `docs/sprint-71.md`
- B-13 (PR #34): el refresh token es de un solo uso también con canjes simultáneos
- Prueba en un teléfono real (OPPO, Android 16): B-14, límite diario dentro de la app y ubicación precisa (PR #33)
- Sprint 70 (B-05, en el PR #32): el panel renueva el token de acceso antes de que caduque — `docs/sprint-70.md`
- Sprints 68–69 (PR #29, #30): B-09 y B-12, cada tutor ve solo los datos de ubicación y geovallas de su vínculo
- Sprint 67 (a falta de PR): bienvenida de 2 pasos con el contenido de Privacidad, aviso de batería sin pedir la exención, aviso «Se desactivó un permiso» (con la línea base guardada en el teléfono);
  el permiso quitado de verdad en el emulador, G-19, y la revisión de seguridad global — `docs/sprint-67.md`
- Sprint 66 (PR #24): bloqueo en v2, Privacidad (solo lo que el código respalda) y Detalle de permiso con pasos reales; dos hallazgos medidos en el emulador antes de corregir el texto — `docs/sprint-66.md`
- Sprint 65 (PR #23): consentimiento en 3 pasos con cuenta atrás, Estado, B2 y B5 (el tutor se entera de «Atrás» y de la caducidad); 1 MEDIA y 4 BAJA arregladas — `docs/sprint-65.md`
- Sprint 64 (PR #22): Inicio y Permisos, B1 (el teléfono detecta la desvinculación) y G-19 — `docs/sprint-64.md`
- Sprint 63 (PR #21): Acceso, Vincular y «Más ajustes» con cierre de sesión confirmado — `docs/sprint-63.md`
- Sprint 62 (PR #20): sistema visual v2 — `docs/sprint-62.md`
- Sprint 61 (PR #19): la app es solo del supervisado — `docs/sprint-61.md`
- Sprint 60 (en local, 04/10/2026): accesibilidad (contraste AA y anillo de foco que ya no tapa la sombra del botón),
  favicon completo, logos por el optimizador de imágenes, login con foto a pantalla completa y panel de cristal, e2e
  ampliado (foco, 16 secciones, aviso de consentimiento en 390 px) y arreglo del log de rechazo de token de Google que
  copiaba texto del token — `docs/sprint-60.md` (+ `docs/sprint-60-evidence.md`)
- Sprints 54–59 (código, 04/10/2026): marco (login, sidebar navy, header, banda) y recomposición editorial de las 16
  vistas del panel web, con solo datos reales; serif Newsreader; e2e en verde sobre el conjunto —
  `docs/sprint-54.md` … `docs/sprint-59.md`
- Sprint 53: base visual del rediseño web (tokens, serif, primitivas, `DESIGN.md`, galería) —
  `docs/sprint-53.md` (+ `docs/sprint-53-evidence.md`)
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

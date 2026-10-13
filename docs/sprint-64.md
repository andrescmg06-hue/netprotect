# Sprint 64 — Inicio y Permisos, y detectar que el tutor desvinculó

Cuarto sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`, fila S64). Láminas 03 (Inicio),
04 (Permisos) y 10.4 (Dispositivo desvinculado por el tutor), con los textos de `02_PANTALLAS.md` §03, §04 y §10.
Cierra **B1** (el teléfono no se entera de que el tutor lo desvinculó) y separa `SupervisedState.Linked` de los datos que
cambian, que es el riesgo **G-19**.

Rama `sprint-64-inicio-permisos`, creada desde `android-redesign` (`527deac`), en el worktree `C:/Users/andre/np-s64`.

## Decisiones

- **Regla §2.3** (función pura, tests en JVM): (1) algún permiso pendiente → «Falta un paso»; (2) si no, ningún latido
  correcto en los últimos U segundos, o ninguno todavía → «Sin reporte reciente»; (3) si no → «Todo funciona». **U = 300 s**
  (decisión 3, aprobada), igual en Inicio y en Estado de NetProtect, que hoy usa 180 s (`ServiceStatusRegistry.kt`).
- **B1:** consultar `/devices/me` al volver a la app (`ON_RESUME`) y cada 5 min mientras el dispositivo está vinculado. Una
  lista de tutores vacía, o un 404, pasa a `Unlinked`. Leer el `status` del latido no basta: ALERT lo tapa. **Un fallo de
  red nunca cuenta como desvinculación**: se conserva el estado y la caché.
- **G-19:** `Linked` pasa a una identidad estable (`deviceId`); los tutores, el nombre del dispositivo y la ruta van en
  estado aparte, para que un refresco de B1 no pare ni relance ningún servicio. Solo la transición real `Linked` →
  `Unlinked` ejecuta los `onDispose` (deseado: para los servicios y el `SyncWorker`) y limpia `LinkedDeviceStore`.
- **Decisiones menores** (el dueño las vio al aprobar el plan): singular correcto («Falta 1 permiso»); con un permiso
  pendiente y sin reporte la titular sigue la regla («Falta un paso») y los arcos del sello se quedan pendientes; Inicio no
  muestra «Privacidad» hasta S66; la pantalla `Unlinked` no inventa quién ni cuándo desvinculó (no hay ese dato).
- No cambia el backend.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [x] T2 — `4262686`. `homeStatus(pendingPermissions, lastHeartbeatOk, now)` (`AllWorking`, `MissingStep(n)`, `NoRecentReport`) con
  los textos de §03 y el singular, y `isReportRecent` + `REPORT_STALE_AFTER_SECONDS = 300` compartidos por Inicio y Estado
  (antes 180 s). El límite exacto de 300 s cuenta como reciente. `testDebugUnitTest lintDebug --rerun-tasks` (sin caché) →
  BUILD SUCCESSFUL, 128 tests, 0 fallos, lint con 0 errores. Delegado (Opus).
- [x] T3 — `37c78e2`, `076166d`. `SupervisedState.Linked(deviceId)` lleva solo la identidad; la etiqueta de tutores pasó a estado de
  presentación. Los 11 efectos del shell usan ahora una sola clave, `linkedDeviceId` (más los permisos donde ya los usaban).
  B1: `/devices/me` en `ON_RESUME` y cada 5 min; `classifyLinkCheck` devuelve `Linked`, `Unlinked` o `Inconclusive`, y
  cualquier fallo es `Inconclusive` (no cuenta como desvinculación). Al entrar en `Unlinked` se limpia `LinkedDeviceStore`,
  se reinician `lastHeartbeatOk` y `heartbeatFailing`, y se detiene una vista remota en curso; los `onDispose` paran servicios,
  socket, latido y `SyncWorker`. `UnlinkedScreen` con firma final y cuerpo provisional. `testDebugUnitTest assembleDebug
  assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 1m 26s, 137 tests, 0 fallos, lint con
  0 errores. Delegado (Opus). Desviaciones del escritor, aceptadas y para `security-reviewer`: (1) al arrancar, con caché y un
  404 o sin tutores, se entra en `Unlinked` (otra cuenta de Google en el mismo teléfono vería «Tu tutor terminó el vínculo»,
  porque cerrar sesión no borra la caché); (2) al desvincularse se detiene la vista remota activa.
- [x] T4 — `94353c5`, `b00b732`, `75e3ca6`. Inicio con `NpHeroPanel` y `homeStatus` (4 estados más el de vista remota; sin fila «Privacidad»), Permisos
  con `NpRail` (`permissionSteps` decide el «siguiente»; solo ese lleva botón), `UnlinkedScreen` restilada, 8 frames de galería de S64
  al principio y tests. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD
  SUCCESSFUL en 1m 25s, 144 tests, 0 fallos, lint con 0 errores. Delegado (Opus). El shell solo perdió `reachable = !heartbeatFailing`
  (`heartbeatFailing` ya no se lee: aviso de compilador, no error). Decisiones del escritor a revisar en T5: (1) la regla recibe
  `lastContact` (el más reciente entre el latido local y el `last_seen_at` del servidor), no solo `lastHeartbeatOk`, para que el
  titular coincida con la fila «Última comunicación»; en un arranque en frío Inicio puede decir «Todo funciona» antes del primer
  latido local mientras Estado aún no lo confirma; (2) los pasos «Pendiente» del rail no llevan botón, así que si el menor rechaza
  el «siguiente» (p. ej. ubicación denegada para siempre) los demás no se alcanzan desde esa pantalla.
- [ ] T5 — Revisión visual en el emulador a letra 1.0 y 2.0 contra las láminas 03, 04 y 10.4, y la **prueba G-19**: 3 min
  navegando y `dumpsys activity services com.netprotect.app` con los mismos `ServiceRecord`, más el latido en el backend
  cada minuto. Inline.
- [ ] T6 — `verifier` (**Haiku**), `security-reviewer` (**Opus**: toca sesión y servicios) y CI.

## Criterios de aceptación

- La regla §2.3 tiene tests en JVM y Inicio y Estado usan el mismo umbral de 300 s.
- Un tutor que desvincula se refleja en el teléfono como «Este dispositivo ya no está vinculado»; un fallo de red no lo dispara.
- Refrescar los tutores no reinicia ningún servicio (prueba G-19 con salida real en `docs/sprint-64-evidence.md`).
- Las pantallas siguen las láminas 03, 04 y 10.4 en estructura y textos, y con letra al 200 % no se corta nada.
- `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde; `security-reviewer` sin ALTA.

## Pendiente que necesita una persona

- Desvincular desde el panel web y ver «Este dispositivo ya no está vinculado» en el teléfono real.

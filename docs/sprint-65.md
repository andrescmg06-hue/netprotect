# Sprint 65 — Consentimiento y Estado, B2 y B5

Quinto sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`, fila S65). Láminas 05 (Estado de
NetProtect) y 06 (Consentimiento y notificaciones permanentes), con los textos de `02_PANTALLAS.md` §05 y §06. Es el sprint de
más riesgo: toca el consentimiento de la vista remota, el tiempo real y los servicios, así que G-19 y `security-reviewer` son
obligatorios.

Rama `sprint-65-consentimiento-estado`, creada desde `android-redesign` (`e941996`), en el worktree `C:/Users/andre/np-s65`.

## Decisiones

- **Consentimiento en 3 pasos con cuenta atrás** (lámina 06): «1 · Leer, 2 · Autorizar, 3 · Android» (el 3 es el diálogo del
  sistema, sin pantalla propia). El plazo real es de **2 min desde que llega la solicitud, solo en el teléfono**. La hora de la
  solicitud se guarda como **estado de presentación**, nunca como clave de un efecto de servicio (G-19).
- **B2:** `ServiceStatusRegistry` guarda `lastReportedAt` (en memoria), escrito **solo si el envío de ubicación tiene éxito**;
  Estado muestra «Sin confirmar» con un dato real y no según el servicio esté o no corriendo.
- **B5 (decisión 6 del plan, aprobada): sin cambiar el esquema del backend.** Si el menor sale con «Atrás» o la solicitud caduca, el
  teléfono envía `screen_share_stop` con motivo `dismissed` o `expired` (el esquema ya admite `reason` de hasta 64 caracteres y es lo
  que ya hace `projection_cancelled`). Un `granted=false` no libera el hueco del tutor en el backend; `stop` sí. La web añade los
  dos motivos a `describeStopReason` y un límite de espera en el estado `requesting`. El envío es de mejor esfuerzo y sin cola
  (`ReconnectingRealtimeChannel.send`): B5 **no puede garantizar la entrega** con el socket caído, y queda declarado.
- **Textos que se corrigen por ser falsos o engañosos** (`PLAN` §2.1): «NetProtect no graba la pantalla ni controla el dispositivo»
  → «NetProtect no graba ni guarda lo que se ve en tu pantalla.» (sí se bloquean apps); «Android no permite quitarlos» → «Cada
  aviso aparece mientras su función está activa.» (los avisos permanentes se pueden descartar desde Android 14; sin probar en
  Android 16). «Cada unos 15 minutos» en ubicación solo si el código lo hace de verdad.
- **Notificación de la vista remota:** la lámina dice «Toca para abrir NetProtect y detener la vista» y hoy tocarla no abre nada; se
  añade un `PendingIntent` inmutable que abre la app.
- No cambia el esquema del backend ni sus rutas. Sí cambian la web (`RemoteViewPanel.tsx`) y un test de backend de `stop` con motivo.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [x] T2 — `4a4d000`, `90862d3`. B2: `ServiceStatus.locationLastReportedAt` (solo el instante, nunca la ubicación), escrito por `reportAndRecord` solo
  si el envío tuvo éxito y borrado al parar el servicio; `servicesView` da la ubicación «Sin confirmar» salvo con un envío dentro de
  `LOCATION_STALE_AFTER_SECONDS` = 2 intervalos de 15 min + 5 min de margen = 35 min (el latido sigue en 300 s). Ventana de
  consentimiento pura (`CONSENT_WINDOW_SECONDS = 120`, `consentRemainingSeconds`, `isConsentExpired`, `consentProgress`,
  `consentCountdownText`: «Esta solicitud caduca en 2 min / 1 min 48 s / 45 s»), con el instante inyectado.
  `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 2m 6s,
  162 tests, 0 fallos, lint con 0 errores. Delegado (Opus). Fuera de T2, a T3 y T5: derivar `CONSENT_TIMEOUT_MS` de la ventana;
  los 3 pasos de la lámina; mostrar la hora del último reporte de ubicación (`ServicesView` no tiene el campo).
- [ ] T3 — Shell: guardar la hora de la solicitud, cuenta atrás, `screen_share_stop` con motivo `dismissed` (Atrás) y `expired`
  (caducidad), sin tocar las claves de los efectos de servicio. Delegado (**Opus**: G-19, realtime y consentimiento).
- [ ] T4 — Web: motivos `expired` y `dismissed` en `describeStopReason`, límite de espera en `requesting`, y test de backend de
  `screen_share_stop` con motivo. Delegado (**Sonnet**).
- [ ] T5 — Pantallas: consentimiento en 3 pasos con cuenta atrás, Estado restilado (panel «En curso», «Detener ahora»), banner,
  textos de los avisos, intent de la notificación de vista remota, frames de galería y tests. Delegado (**Opus**: oficio visual).
- [ ] T6 — Revisión visual en el emulador a letra 1.0 y 2.0 contra las láminas 05 y 06, y la **prueba G-19** (3 min navegando,
  `dumpsys activity services com.netprotect.app`, latido en el backend). Inline.
- [ ] T7 — `verifier` (**Haiku**), `security-reviewer` (**Opus**: consentimiento, realtime y servicios), CI y PR.

## Criterios de aceptación

- La cuenta atrás se ve en el consentimiento y llega a cero cerrando la solicitud; «Atrás» y la caducidad envían `screen_share_stop`
  con su motivo y la web los muestra.
- Estado muestra «Sin confirmar» en ubicación solo con un dato real (B2).
- Ningún texto dice que NetProtect no controla el dispositivo.
- La prueba G-19 con salida real en `docs/sprint-65-evidence.md`; `security-reviewer` sin ALTA.
- `./gradlew test assembleDebug lintDebug`, `npm run lint && npm run build` y los 8 jobs de CI en verde.

## Pendiente que necesita una persona

- Vista remota de extremo a extremo con un tutor real en el panel web, caducidad, «Atrás» y «Detener» desde la notificación y el
  banner, y si los avisos permanentes se pueden descartar en Android 16 (Galaxy S25 FE).

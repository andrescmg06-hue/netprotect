# Sprint 73 — El bloqueo no se salta y la protección sigue con la app cerrada

Rama `sprint-73-bloqueo-segundo-plano` (desde `main` en `e5b0a81`). Estado: **cerrado en local el 16/10/2026, sin PR**.
Lo comprobado está en `docs/sprint-73-evidence.md`; lo que solo puede probar una persona (el Redmi con HyperOS y el aviso
instantáneo del panel) queda como H-07 y H-08 en `docs/tasks.md`, no como hecho.

Dos fallos que el dueño encontró probando en un teléfono real (Redmi Note 13, Android 15, HyperOS 2.0) el 15/10/2026.

## Fallo 1 — Facebook bloqueada se pudo usar tras volver al inicio

Eventos reales del teléfono (`adb shell dumpsys usagestats`):

```
10:03:30 ACTIVITY_RESUMED com.facebook.katana   ← se bloquea
10:03:33 ACTIVITY_RESUMED com.miui.home         ← «Ir al inicio»
10:03:34 ACTIVITY_RESUMED com.facebook.katana   ← vuelve a entrar 1 s después
10:04:23 ACTIVITY_PAUSED  com.facebook.katana   ← 49 s de uso sin bloqueo
```

**Causa raíz:** `ForegroundAppDetector.pollForegroundChange()` (`core/rules/ForegroundAppDetector.kt:23-43`) devuelve
solo el **último** paquete que pasó a primer plano en la ventana de 3 s. Salir al inicio y volver a Facebook dentro de la
misma ventana devuelve «facebook», igual que `lastHandledPackage`, y el bucle de `RuleEnforcementService`
(`RuleEnforcementService.kt:225-296`) lo trata como la misma visita, ya bloqueada (`blockedThisVisit`), así que no
vuelve a evaluar ni a dibujar la pantalla de bloqueo, que el botón «Ir al inicio» ya había quitado (`:362-367`).

**Arreglo:** la visita se decide con **toda** la secuencia de eventos de la ventana, no con el último; y en cada ciclo,
si la app bloqueada sigue en primer plano y la pantalla de bloqueo no está, se vuelve a dibujar (vigilancia periódica,
no solo en el cambio de app). Una excepción en un ciclo ya no termina el bucle.

## Fallo 2 — Con la app cerrada no funciona nada

**Causa raíz:** `SupervisedShell` arranca los servicios desde efectos de la pantalla y los **detiene a propósito** al
desmontarse (`SupervisedShell.kt:495`, `:506` y `:549`: `RuleEnforcementService.stop`, `LocationReportingService.stop`,
`SyncWorker.cancel`). Cerrar la app (Atrás o deslizarla en Recientes) destruye la Activity y con ella toda la
protección. Además nada la vuelve a arrancar: los servicios devuelven `START_NOT_STICKY`, no hay receptor de arranque y
`SyncWorker` solo se programa desde la pantalla. Era un límite aceptado desde los Sprints 8, 13 y 19; el dueño decide
ahora que la protección debe seguir con la app cerrada.

**Arreglo:** los servicios se paran solo cuando de verdad toca (cerrar sesión, desvinculación, permiso retirado), no al
cerrar la pantalla; se reinician solos si el sistema los mata (`START_STICKY`, reconstruyendo su configuración desde lo
guardado); se arrancan al encender el teléfono o actualizar la app; y `SyncWorker` hace de vigilante.

**Límite que no se arregla con código:** HyperOS/MIUI mata los procesos al deslizar la app en Recientes si no tiene
«Inicio automático» y el ahorro de batería en «Sin restricciones». Eso lo concede una persona en Ajustes; se comprueba
en este teléfono y se documenta.

## Fallo 3 — Una regla nueva no bloquea la app que ya se está usando

Lo encontró el dueño en la misma prueba: si el tutor crea una regla de bloqueo mientras el menor **ya** está dentro de
esa app, no se bloquea hasta que la cierra y la vuelve a abrir. Lo mismo con una regla de horario (`SCHEDULE`) o el modo
escolar que empiezan con la app abierta.

**Causa raíz:** el bucle de `RuleEnforcementService` sí refresca las reglas (cada 60 s, o al instante con el aviso
`rules_changed` del canal en tiempo real: `RuleEnforcementService.kt:205-223` en `e5b0a81`), pero la visita en curso solo
se evaluaba al empezar (`:225-262`). Lo único que se reevaluaba a mitad de visita era un límite diario o semanal (B-15,
`:262-296`, filtrado por `LimitRecheck.isGovernedByLimit`); un `BLOCK` o un `SCHEDULE` nuevos no entraban por ahí.

**Arreglo:** en cada «latido de reglas» (el refresco de 60 s o el inmediato por `rules_changed`, haya funcionado la
descarga o no, para que un horario que empieza sin conexión también bloquee con las reglas guardadas), la visita en curso
de otra app que sigue delante y no está bloqueada se vuelve a evaluar con las reglas vigentes: si ahora toca bloquearla, se
bloquea y se reporta el evento como en cualquier visita (uno por bloqueo, nunca repetido para una visita ya bloqueada). Si
la visita ya estaba bloqueada y las reglas nuevas ya no la bloquean (se quitó la regla, terminó el horario), se quita la
pantalla de bloqueo sin reportar nada. La decisión «¿qué hacer con la visita en curso?» es una función pura
(`VisitRecheck.onRulesBeat`) con tests. Por qué en el latido y no en cada ciclo de 3 s: los cambios del tutor llegan al
instante por `rules_changed`, y los horarios van por minutos, así que reevaluar una vez por minuto acota el retraso a
alrededor de un minuto sin leer las estadísticas de uso veinte veces más a menudo.

## Tareas

- [x] T1 — Fallo 1: visita por secuencia de eventos (lógica pura con tests), re-dibujo periódico de la pantalla de
  bloqueo y bucle resistente a excepciones. Delegado (writer). `1392705`.
- [x] T1b — Fallo 3: reevaluar la visita en curso en cada latido de reglas (`VisitRecheck`, lógica pura con tests);
  quitar la pantalla de bloqueo si la regla desaparece. Delegado (mismo writer), commit aparte porque T1 ya estaba
  cerrado. `ea133a7`.
- [x] T2 — Fallo 2: parada explícita (sesión, desvinculación, permiso), `START_STICKY` con configuración guardada,
  receptor de arranque, `SyncWorker` independiente de la pantalla y como vigilante. Delegado (mismo writer). `b2f3e82`.
- [x] T3 — Documentación: `docs/android/capability-matrix.md` y `.claude/rules/android.md` (los límites de S8/S13/S19
  cambian). Delegado (mismo writer). Commit `docs(sprint-73): documentar el reinicio de la protección y sus límites`.

Comprobado hasta aquí (ver `docs/sprint-73-evidence.md`): `compileDebugKotlin` y `testDebugUnitTest` en verde (281
tests, 17 nuevos). **Pendiente (T4):** `assembleDebug`, `lintDebug`, `security-reviewer` y toda la prueba en el
teléfono real; nada del comportamiento en ejecución (redibujo, reinicio, arranque, MIUI) está comprobado todavía.
- [x] T5 — Endurecimiento tras las revisiones de seguridad y de fiabilidad (15/10/2026), mismo tipo de fallo que los
  tres de arriba. Delegado (writer). Se corrige: (a) al arrancar o reiniciar el servicio se lee qué app está delante
  (hoy no se evalúa hasta que el menor sale y vuelve); (b) una desvinculación con la app cerrada se detecta: el backend
  rechaza con el 404 uniforme los envíos del supervisado sin vínculo activo y el teléfono lo trata como fin del
  vínculo; (c) cerrar sesión borra el vínculo guardado y las cachés, para que otra cuenta no herede el dispositivo;
  (d) la marca de vida solo se sella en ciclos que funcionan; (e) el vigilante no depende de la red; (f) una
  evaluación que falla se reintenta en el ciclo siguiente; (g) el canal en tiempo real del servicio se reconecta;
  (h) el bucle no puede morir en su arranque y el reinicio comprueba que sigue activo; (i) la pantalla de bloqueo
  quitada por el sistema se detecta; (j) un evento pendiente inválido no bloquea la cola; (k) la red ya no retrasa la
  evaluación; (l) ventanas de eventos con solape y sin duplicados; (m) un fallo pasajero del Keystore no borra la
  sesión. Al backlog, con nota: avisar al tutor de superposición o ubicación retiradas y de la batería optimizada
  (cambia el latido), apps en pantalla dividida o flotante, y abrir la app sin red.
  Commits: `9e3fd44` (backend, b), `72ef7c9` (m), `81cbc53` (b en Android, c, e, j), `1ff8458` (a, d, f, g, h, i, k,
  l) y el de documentación. Decisiones tomadas al implementar:
  - (b) No hizo falta dependencia nueva: `require_supervised_owner_of_device` exige ahora un vínculo activo, y
    todas las rutas que la usan son de un dispositivo vinculado (latido, ubicación, `applications/sync`,
    `rule-events`, `rules/active`, `tamper-events`, `push-token`). `GET /policy` es solo del tutor (el teléfono la
    recibe dentro de `rules/active`). `checkLink`/`refreshLink` usan `/devices/me`, que no pasa por ella; el canal
    WebSocket no se tocó (no envía datos del menor; sin tutor nadie pide la vista remota). El teléfono solo acepta
    como fin del vínculo el 404 con detalle `device_not_found` y solo si el `deviceId` es el del vínculo guardado
    (un trabajo viejo de un vínculo anterior no puede borrar uno nuevo). Tras ese fin se recuerda que lo terminó el
    servidor (`LinkedDeviceStore.markEndedByServer`), para que al abrir la app se vea «desvinculado» y no la
    pantalla del código. Dos pruebas del backend que enviaban ubicación tras desvincular se ajustaron al rechazo.
  - (c) `forgetDevice` (parar todo, olvidar el vínculo, borrar Room) en cerrar sesión, sesión caducada, abrir sin
    sesión y entrar con una cuenta distinta de la confirmada en `SupervisedRoleCache`. La cola de eventos pendientes
    **se borra**: solo esa sesión podría enviarla (otra cuenta recibe el 404) y no deben quedar datos del menor en un
    teléfono sin su sesión; el coste es que, si la misma persona vuelve a entrar, los bloqueos ocurridos sin red
    antes de cerrar sesión no llegan al tutor. **No** se guardó el id de usuario junto al vínculo: sin red no hay
    forma de saber el usuario de la sesión guardada, y todos los caminos por los que entra otra cuenta pasan antes
    por uno de los cuatro de arriba; la ventana que queda (entre instalar la sesión nueva y comprobar la cuenta) la
    cierra el 404 de (b).
  - (e) `EnforcementWatchWorker`, periódico sin restricción de red, único (`UPDATE`), se programa y cancela junto a
    `SyncWorker` (pantalla, `BootReceiver`, `stopAll`); en `SyncWorker` el vigilante pasa al principio y el latido
    sigue informando el estado de la marca **anterior** al reinicio.
  - (m) Solo cuenta como «sin sesión» un fallo de descifrado irrecuperable (`AEADBadTagException`,
    `KeyPermanentlyInvalidatedException`, `UnrecoverableKeyException`, entrada corrupta); el resto es
    `TokenStoreUnavailableException`, que espera como un fallo de red. Al abrir la app con el Keystore caído se ve el
    error de «servicio no disponible» y la sesión se conserva.
  - (a) La lectura hacia atrás es de 30 min: una app abierta antes y que sigue delante no se ve hasta su siguiente
    transición. (l) Además de no repetir eventos, se descarta un evento tardío más antiguo que otro ya entregado: si
    se aplicara fuera de orden pondría delante una app que ya no lo está.
- [x] T6 — Hallazgo de la prueba en el emulador (Pixel 8, API 36, 16/10/2026): con la app cerrada el bloqueo sigue
  (Atrás + deslizar en Recientes: sin `MainActivity`, proceso y servicios vivos), pero **la ubicación no revive** tras
  `kill -9` del proceso: en Android 11+ un servicio `location` no arranca desde segundo plano sin
  `ACCESS_BACKGROUND_LOCATION` (`LocationReportingService.canStartFromBackground`). El dueño ya decidió pedirlo
  («Permitir todo el tiempo», Engram `decision/android-background-location`, 15/10/2026). Se implementa: permiso en el
  manifiesto, flujo con explicación previa en el supervisado, `canStartFromBackground` según el permiso concedido, y
  la matriz de capacidades/regla de Android (cambia la decisión de los Sprints 13/14). Delegado (writer).
  **Hecho** (commit `2ebd3de`; `compileDebugKotlin` + `testDebugUnitTest` en verde, 309 tests al cerrar T6). Decisiones: no se
  añadió un quinto permiso (todo el texto y los contadores «N de 4» siguen igual): «Permitir todo el tiempo» es el
  **último paso del permiso «Ubicación»** (`PermissionsUi.locationBackgroundMissing`; `location` solo es «configurado»
  con precisa y con el de fondo donde Android lo exige). Con precisa concedida y el de fondo pendiente (API 30+), el
  botón abre antes la explicación para el menor (`BackgroundLocationExplanationDialog`, sobre `ConfirmDialog`) y
  luego `RequestPermission(ACCESS_BACKGROUND_LOCATION)`, que lleva a Ajustes; la explicación sale también sola al
  conceder la precisa. «Ahora no» no bloquea nada. `canStartFromBackground(context)` = API < 30 o permiso concedido
  (`LocationPermission.backgroundReady`, puro y con tests). Retirarlo en Ajustes deja «Ubicación» pendiente en Permisos
  e Inicio, **sin** aviso «se quitó» ni al tutor (el latido no lo lleva; no se cambió el backend: avisarle al tutor sería
  un campo nuevo, al backlog junto con el de T5). **Sin comprobar en ejecución:** el diálogo, el salto a Ajustes y el
  reinicio de la ubicación tras `kill -9` con el permiso concedido.
- [x] T7 — Hallazgo de la misma prueba: el latido de 1 min y la sincronización de apps/uso viven en `SupervisedShell`
  (`LaunchedEffect`, solo con la pantalla abierta). Con la app cerrada solo manda latidos `SyncWorker` (≥15 min) y el
  backend da por desconectado al dispositivo a los 5 min: el tutor lo vería «sin reporte» aunque el bloqueo funcione.
  Se corrige haciendo que el servicio de reglas (siempre vivo mientras hay vínculo) envíe el latido en su pulso de
  60 s y la sincronización de apps cada pocos minutos, reutilizando lo que ya hace `SyncWorker`. Delegado (mismo writer).
  **Hecho** (commit `139323b`; `compileDebugKotlin`, `testDebugUnitTest` (322 tests, 0 fallos) y `lintDebug` en verde).
  Decisiones: el envío vive en un solo lugar, `core/sync/DeviceReporter` (campos del latido y de la sincronización), usado por
  `SyncWorker`, por el servicio (`PeriodicReporter`, desde su bucle de 3 s) y por los `LaunchedEffect` del shell, que se
  quedan para la pantalla. La cadencia es pura (`ReportCadence`, con tests): el servicio es **respaldo** de la pantalla y
  envía cuando pasan el intervalo + 15 s desde el último intento o éxito de cualquiera (`ReportClock`, en memoria), así con
  la pantalla abierta no duplica tráfico; con la app cerrada el latido sale cada ~75 s (no «en el pulso de 60 s»: ese margen
  evita la carrera con el shell y sigue lejos de los 5 min del backend) y la sincronización cada ~5 min 15 s, solo con acceso
  al uso. Se envía desde corutinas hijas (nunca retrasa un ciclo de bloqueo) y un 404 `device_not_found` termina el vínculo
  como en `SyncWorker`. **Sin comprobar en ejecución:** que el panel del tutor mantenga «conectado» al dispositivo con la app
  cerrada (emulador o teléfono).
- [ ] T4 — Verificación (`verifier`: test, assembleDebug, lintDebug), `security-reviewer` (toca `core/rules`, sesión y
  arranque de servicios) y prueba real en el Redmi: re-entrada rápida a Facebook, app deslizada en Recientes, reinicio.
  Inline. **Hecho en el emulador (16/10/2026, `docs/sprint-73-evidence.md`):** tests, `assembleDebug` y `lintDebug` en
  verde, revisión de seguridad de T5 y de T6/T7 (3 hallazgos corregidos en `81a6de9`), y los tres fallos del dueño más
  app cerrada, proceso muerto, reinicio, latido y ubicación con la app cerrada. **Pendiente, por eso sigue abierta:** la
  prueba en el Redmi con HyperOS (el emulador no reproduce cómo mata el sistema los procesos), el aviso instantáneo
  `rules_changed` desde el panel. El reinicio con el permiso de fondo concedido se comprobó (ambos servicios,
  latido y ubicación llegan sin abrir la app). La revisión por recibos de la rama (46 archivos) no cabe en el
  presupuesto de contexto de la herramienta y el dueño decidió omitirla el 16/10/2026.

## Criterios de aceptación

Estado de cada uno al cierre (16/10/2026):

1. **Cumplido** (`docs/sprint-73-evidence.md`): test con la secuencia real de eventos y, en el emulador, reentrada a 1 s,
   instantánea y a 8 s: siempre bloqueada, un evento por visita.
2. **Pendiente de medir en ejecución.** Cubierto solo por tests JVM (redibujo periódico y detección de la pantalla quitada
   por el sistema); no se quitó la superposición en el emulador para medir el ≤ 3 s.
3. **Cumplido en el emulador, pendiente en HyperOS (H-07).** Atrás y deslizada en Recientes: sin `MainActivity` ni tarea,
   proceso y servicios vivos y bloqueo operativo; también tras `kill -9` y tras reiniciar. «Inicio automático» no existe
   en el emulador: la parte de MIUI/HyperOS solo la confirma el Redmi.
4. **Cumplido en pruebas automáticas, sin comprobar en un teléfono.** Los 358 tests de backend (rechazo uniforme de envíos
   tras desvincular) y los 322 de Android cubren la parada de servicios; no se cerró sesión ni se desvinculó el emulador
   para no deshacer el vínculo que usó la prueba.
5. **Cumplido por el refresco periódico, pendiente por el aviso instantáneo (H-08).** Regla `BLOCK` con la app abierta: 5 s;
   horario: 52 s; al quitar la regla, 46 s; límite diario: 62 s (≈ un refresco más el muestreo de 5 s). El aviso
   `rules_changed` (≤ 3 s) no se probó: la extensión de Chrome no estaba conectada y las reglas se pusieron por `psql`.
   Un evento por visita bloqueada en `app_rule_events`; tests de `VisitRecheck` para «nunca un segundo evento».

Texto original de los criterios:

1. Salir al inicio y volver a una app bloqueada, por rápido que sea, la vuelve a bloquear (test con la secuencia real).
2. Si la pantalla de bloqueo desaparece y la app bloqueada sigue delante, vuelve en el siguiente ciclo (≤ 3 s).
3. Con la app cerrada (Atrás y deslizada en Recientes, con «Inicio automático» concedido) el bloqueo sigue funcionando.
4. Cerrar sesión y la desvinculación siguen deteniendo todo, como antes.
5. Una regla de bloqueo creada mientras el menor ya está dentro de esa app la bloquea sin cerrarla: al instante si llega
   el aviso `rules_changed` (≤ 3 s), y como mucho en el siguiente refresco de reglas (≤ 60 s) si no llega. Un horario que
   empieza con la app abierta la bloquea en ≤ 1 min. Si la regla se quita, la pantalla de bloqueo desaparece en ese mismo
   plazo. Nunca se reporta un segundo evento de bloqueo para una visita ya bloqueada (tests de `VisitRecheck`).

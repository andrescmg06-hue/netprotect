# Sprint 73 — El bloqueo no se salta y la protección sigue con la app cerrada

Rama `sprint-73-bloqueo-segundo-plano` (desde `main` en `e5b0a81`). Estado: en curso.

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

## Tareas

- [ ] T1 — Fallo 1: visita por secuencia de eventos (lógica pura con tests), re-dibujo periódico de la pantalla de
  bloqueo y bucle resistente a excepciones. Delegado (writer).
- [ ] T2 — Fallo 2: parada explícita (sesión, desvinculación, permiso), `START_STICKY` con configuración guardada,
  receptor de arranque, `SyncWorker` independiente de la pantalla y como vigilante. Delegado (mismo writer).
- [ ] T3 — Documentación: `docs/android/capability-matrix.md` y `.claude/rules/android.md` (los límites de S8/S13/S19
  cambian). Delegado (mismo writer).
- [ ] T4 — Verificación (`verifier`: test, assembleDebug, lintDebug), `security-reviewer` (toca `core/rules`, sesión y
  arranque de servicios) y prueba real en el Redmi: re-entrada rápida a Facebook, app deslizada en Recientes, reinicio.
  Inline.

## Criterios de aceptación

1. Salir al inicio y volver a una app bloqueada, por rápido que sea, la vuelve a bloquear (test con la secuencia real).
2. Si la pantalla de bloqueo desaparece y la app bloqueada sigue delante, vuelve en el siguiente ciclo (≤ 3 s).
3. Con la app cerrada (Atrás y deslizada en Recientes, con «Inicio automático» concedido) el bloqueo sigue funcionando.
4. Cerrar sesión y la desvinculación siguen deteniendo todo, como antes.

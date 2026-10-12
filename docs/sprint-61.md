# Sprint 61 — Solo supervisado: el modo tutor sale de la app

Primer sprint del rediseño v2 del supervisado (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`). El
plan lo aprobó el dueño el 12/10/2026, decisión 1: la app Android es solo del menor y el tutor usa únicamente el panel
web. Este sprint también cierra B-07 (`docs/tasks.md`): con «Cambiar de modo», el menor apagaba la protección con dos
toques y sin avisar al tutor.

Rama `sprint-61-solo-supervisado`, creada desde `android-redesign` (`61a8d10`), en el worktree `C:/Users/andre/np-s61`.

## Decisiones

- **Rol al iniciar sesión.** `/auth/me` no devuelve roles, pero `GET /users/me/roles` sí (`backend/app/api/v1/endpoints/roles.py:23-35`).
  - Si la cuenta tiene SUPERVISADO, entra directamente.
  - Si no tiene ningún rol, la app concede SUPERVISADO sola con `POST /users/me/roles`. La llamada es idempotente y
    audita `ROLE_GRANTED` solo la primera vez.
  - Si la cuenta solo tiene TUTOR, se muestra «Esta app es para el teléfono del menor; usa el panel web» con dos
    botones, «Usar este teléfono como supervisado» y «Cerrar sesión». El rol nunca se concede en silencio a un tutor.
  - Hace falta SUPERVISADO porque el backend lo exige para canjear códigos y para `/devices/me`.
- **Fuera:** «Cambiar de modo», «Elegir modo» (`RoleSelectionScreen`), `RolePreference`, `feature/tutor/**`, y el
  código, los tests y los frames de galería que solo usaba el tutor.
- **Se conservan aunque queden sin uso:** `ConfirmDialog` y `ErrorState` (los usa S63) y todos los iconos de `NpIcons`
  (la limpieza va en S67). `QUERY_ALL_PACKAGES` se queda porque lo usa el supervisado: inventario de apps, pantalla de
  bloqueo y `ProtectedPackages`.
- No cambia el backend.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis) con el motivo.

- [x] T1 — Paquete v2 y plan aprobado en la rama. Inline (copiar y guardar). `7006eeb`.
- [x] T2 — Flujo de rol solo supervisado. `aa63cf1`. `decideRole` es una función pura con 4 tests JVM. Hay caché local del
  rol confirmado (`SupervisedRoleCache`) y `TutorAccountScreen` con 4 tests instrumentados. `./gradlew test assembleDebug
  assembleDebugAndroidTest lintDebug` → `BUILD SUCCESSFUL in 2m 5s`, 166 tests por variante, lint 0 errores. Los tests
  instrumentados quedan para el CI. Enunciado: `HomeScreen` decide con `GET /users/me/roles`: concede SUPERVISADO o muestra
  la pantalla de cuenta de tutor. Se quitan `SelectingRole`, `InTutorMode`, `switchMode`, `RolePreference`,
  `RoleSelectionScreen` y el «Cambiar de modo» de Vinculado y del shell, con sus tests y frames. Delegado
  (**Opus**: auth y roles, sensible; más de 2 archivos no triviales).
- [ ] T3 — Borrar `feature/tutor/**` y el código muerto: componentes, formateadores, `ui/navigation`, `ui/state`,
  clientes y métodos de red solo del tutor; también sus tests JVM e instrumentados, las galerías y `DesignGalleryTest`.
  Delegado (**Sonnet**: borrado amplio pero mecánico, inventario ya hecho).
- [ ] T4 — Docs que describen la app como «Tutor o Supervisado»: `CLAUDE.md`, `README.md`, `.claude/rules/android.md`,
  `INVENTARIO.md`, `capability-matrix.md` y el comentario de `QUERY_ALL_PACKAGES` en el manifest. Delegado (**Sonnet**).
- [ ] T5 — Verificación (`verifier`, **Haiku**: `test assembleDebug lintDebug`), `security-reviewer` (**Opus**, flujo de
  roles) y CI. Pendiente humano: login real con una cuenta supervisada y con una que solo es tutor.

## Criterios de aceptación

- No queda ninguna ruta a modo tutor ni «Cambiar de modo» (grep de `TutorShell`, `RoleSelection`, `switchMode`,
  `RolePreference` sin resultados en `mobile/`).
- Una cuenta nueva entra como supervisada sin elegir nada; una cuenta que solo es tutor ve la pantalla de aviso.
- Los servicios del supervisado siguen igual (sin tocar `SupervisedShell` salvo el parámetro `onSwitchMode`).
- `./gradlew test assembleDebug lintDebug` en verde y los 8 jobs de CI en verde.
- `security-reviewer` sin ALTA.

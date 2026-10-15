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
- [x] T3 — `0217236`: 79 archivos, −8502 líneas. `test assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` →
  `BUILD SUCCESSFUL in 1m 38s`, 84 tests unitarios y lint con 0 errores. La búsqueda de `feature.tutor`, `TutorShell`,
  `onSwitchMode`, `RoleSelection`, `RolePreference` y `NavStack` no encuentra nada. `SupervisedShell` y los servicios no
  cambiaron. También salió `LocationReport`, que solo usaba el tutor. Enunciado: Borrar `feature/tutor/**` y el código muerto: componentes, formateadores, `ui/navigation`, `ui/state`,
  clientes y métodos de red solo del tutor; también sus tests JVM e instrumentados, las galerías y `DesignGalleryTest`.
  Delegado (**Sonnet**: borrado amplio pero mecánico, inventario ya hecho).
- [x] T4 — Docs que describen la app como «Tutor o Supervisado»: `CLAUDE.md`, `README.md`, `.claude/rules/android.md`,
  `INVENTARIO.md`, `capability-matrix.md` y el comentario de `QUERY_ALL_PACKAGES` en el manifest. Delegado (**Sonnet**). Hecho (este commit): descripciones actuales ajustadas a «app solo del supervisado»; el historial de sprints anteriores no se tocó.
- [ ] T5 — Verificación (`verifier`, **Haiku**: `test assembleDebug lintDebug`), `security-reviewer` (**Opus**, flujo de
  roles) y CI. Pendiente humano: login real con una cuenta supervisada y con una que solo es tutor.
  **Avance (12/10/2026):**
  - `verifier` sobre `d7345ea`:
    - comando: `env -u CI ./gradlew clean test assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`;
    - resultado: `BUILD SUCCESSFUL in 2m 26s`, 84 tests unitarios sin fallos y lint con 0 errores (24 avisos);
    - las búsquedas de restos del tutor y de colores literales no encuentran nada.
  - `security-reviewer` sobre `origin/android-redesign...HEAD -- mobile`: **0 ALTA, 1 MEDIA, 1 BAJA.**
    - MEDIA: cerrar sesión sigue apagando la protección sin aviso. B-07 queda parcial en `tasks.md` (S63 y B-11).
    - BAJA: la guarda contra el doble toque en `useAsSupervised` leía el estado capturado. Corregido en `f622e54`
      (compila y los tests JVM pasan).
  - El rol nunca se concede en silencio a un tutor. La caché va por id de usuario, en `MODE_PRIVATE`, se borra al
    cerrar sesión y no entra en copias de seguridad. El backend sigue exigiendo SUPERVISADO.
  - CI del PR #19 (run `38321817276`): 8/8 en verde; `android-instrumented` «Finished 71 tests on emulator-5554», que son
    todos los `@Test` de `androidTest`, incluidos los 4 de `TutorAccountScreenTest`.
  - **Pendiente, humano:** login real con una cuenta supervisada y con una que solo es tutor.

## Criterios de aceptación

- No queda ninguna ruta a modo tutor ni «Cambiar de modo» (grep de `TutorShell`, `RoleSelection`, `switchMode`,
  `RolePreference` sin resultados en `mobile/`).
- Una cuenta nueva entra como supervisada sin elegir nada; una cuenta que solo es tutor ve la pantalla de aviso.
- Los servicios del supervisado siguen igual (sin tocar `SupervisedShell` salvo el parámetro `onSwitchMode`).
- `./gradlew test assembleDebug lintDebug` en verde y los 8 jobs de CI en verde.
- `security-reviewer` sin ALTA.

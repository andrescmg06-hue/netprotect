# Sprint 67 — Casos reales y cierre del rediseño v2

Último sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`, fila S67). Láminas 13.1 a 13.4 (bienvenida, aviso de batería, «Se desactivó un permiso») de
`02_PANTALLAS.md`, más la pasada de letra al 200 %, y el cierre (`android-redesign` → `main`). Riesgo medio-alto: toca el shell (G-19) y la primera impresión del menor.

Rama `sprint-67-casos-reales`, creada desde `android-redesign` (`182b4b7`, con S61 a S66), en el worktree `C:/Users/andre/np-s67`.

## Decisiones (por defecto con el plan; el dueño puede cambiarlas)

- **Bienvenida de 2 pasos** (láminas 13.1 y 13.2): se muestra la primera vez tras vincular. Estado de presentación fuera de `Linked` (nunca clave de un efecto, G-19) y una bandera en `SharedPreferences`
  (no hay DataStore en el proyecto). El paso 2 («Esto es lo que tu tutor puede ver») debe cuadrar con `PrivacyContent.kt` de S66, que es lo que el código respalda; si la lámina dice otra cosa, gana Privacidad.
  La bandera se borra al desvincular, para que un vínculo nuevo vuelva a mostrar la bienvenida.
- **Aviso de batería** (lámina 13.3): solo comprueba `PowerManager.isIgnoringBatteryOptimizations` y abre los ajustes; **sin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** (riesgo de política de Play). Un solo texto cauto,
  sin ramas por fabricante («pasos por validar en un Samsung real»: queda pendiente de persona). Se añade el icono Battery a `NpIcons`. Se reconoce el límite: en One UI la lista blanca estándar y «Sin restricciones»
  pueden no coincidir, así que el aviso puede dar falsos positivos o negativos; solo se sabrá en el Galaxy.
- **«Se desactivó un permiso»** (lámina 13.4): al volver a la app (`ON_RESUME`) se compara el estado anterior con el nuevo de los cuatro permisos y se avisa de los que **se quitaron** (distinto de «nunca se dio»),
  con «Activar de nuevo» (lleva al Detalle de S66) y «Volver al inicio». Texto falso a evitar (PLAN §2.1): «Tu tutor puede ver que este permiso está pendiente» solo vale para el acceso al uso (es lo único que viaja,
  `usage_access_granted` y la alerta `PERMISSION_REVOKED`); para los demás: «Activa el permiso para que NetProtect vuelva a funcionar.».
- **Fuera de alcance, diferido:** el aviso de Android en segundo plano (lámina 13.5) exige tocar `RuleEnforcementService`; es opcional en el plan y se difiere. Tampoco cambia el backend.
- **Cierre:** pasada de letra 200 % en las pantallas nuevas y las que nadie vio a 2.0 (`TutorAccountScreen`, `ActiveShareBanner` en la app real, el pie fijo del consentimiento y de Estado en pantalla pequeña), prueba G-19,
  revisión de seguridad **global** de `main...android-redesign` (como pidió la T11 de S51), `docs/progress.md` (desfasado en el Sprint 51) y `docs/tasks.md`, y los dos PR: `sprint-67` → `android-redesign` y
  `android-redesign` → `main`. `main` tiene el merge del PR #18 que `android-redesign` no tiene (1 commit frente a 77): se resuelve fusionando `main` en `android-redesign` antes del PR final. **El merge a `main` lo hace el dueño.**

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis). Un solo escritor a la vez: las tareas T2 a T4 tocan el shell.

- [x] T1 — Abrir el sprint y poner `CONTINUAR.md` al día. Inline. (este commit)
- [x] T2 — `4ddd96b`, `92a3ece`, `5ded891`. `WelcomeScreen` de 2 pasos; el paso 2 no tiene textos propios: pinta `privacySections` y `PRIVACY_NO_RECORDING` de Privacidad (un test comprueba que es exactamente esa lista), porque la lámina 13.2 decía cosas que el código no respalda
  tal cual («Si el dispositivo está activo», «Cada unos 15 minutos») y omitía la lista de apps, los bloqueos y el aviso de desinstalación. `WelcomeStore` guarda el deviceId para el que se completó (no un booleano) y `enterUnlinked()` lo borra. Shell: `showWelcome` y
  `welcomeStep` son estado de presentación (`rememberSaveable`), no clave de ningún efecto ni parte de `Linked`; solo se muestra tras un vínculo nuevo (no a quien ya estaba vinculado); «Atrás» en el paso 1 no la salta; el botón final («Entendido, configurar permisos») abre
  Permisos solo si queda algún permiso pendiente, y si no dice «Entendido» y vuelve a Inicio. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL, 202 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` → **137 tests, 0 fallos**
  (con los 8 de `WelcomeScreenTest`). Delegado (Opus). Límites: cerrar sesión (sin «Desvincular») no limpia la bandera; el diálogo del sistema de notificaciones sigue saliendo encima del paso 1.
  Incidente de entorno (no del código): la primera corrida de UI abortó con «System has crashed» por falta de RAM del equipo (~2 GB libres), y un `adb reboot` mío dejó el emulador bloqueado por su PIN (124 tests Compose fallaron con «Unable to resolve activity»);
  se desbloqueó a mano y la repetición pasó. Lección: no reiniciar el emulador con `adb reboot`, y comprobar `RUNNING_UNLOCKED` antes de los tests de UI.
- [x] T3 — `74895a1`, `3e0668a`, `8fbbc83`. `BatteryOptimization` (`isIgnoring` con `PowerManager`, `settingsIntent` = `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, respaldo a la ficha de la app), `BatteryScreen` y una fila de aviso en Inicio solo cuando Android optimiza la batería de NetProtect;
  no entra en «N de 4 permisos» ni cambia «Todo funciona». **El manifiesto no cambia** (sin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Textos cautos: «puede detenerse» (no «se detiene»: solo se sabe que hay optimización), el paso 1 avisa de que Android abre una lista de apps
  (la lámina decía que abría los ajustes de NetProtect) y la nota admite que los pasos aún se comprueban en teléfonos reales; un test prohíbe «nunca», «garantiza», «Play» y nombres de fabricante. Shell: `batteryIgnoring` es estado de presentación que se relee en el mismo ON_RESUME que los permisos, ruta `Battery`,
  ninguna clave de efecto cambia. Icono Battery nuevo. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL, 208 tests, 0 fallos, lint 0 errores; tests de UI del paquete `ui` en dos tandas → **132 tests, 0 fallos** (los 13 de `core`, sin Compose, pasaron en la corrida completa anterior).
  Delegado (Opus). Incidente: una corrida completa se cortó porque el emulador se cayó solo (error de `qemu` en Windows); el único test que falló en ese momento, `MoreSettingsScreenTest.cancellingTheSignOutChangesNothing`, pasa al repetirlo. El emulador se reabrió con `-gpu swiftshader_indirect`.
  Sin probar: los pasos en One UI (falsos positivos o negativos posibles porque «Sin restricciones» puede no coincidir con la lista blanca estándar) y el respaldo `appDetailsIntent` en un dispositivo.
- [x] T4 — `e1805e2`, `1870950`, `f0aff90`. `PermissionSnapshotStore` (SharedPreferences, ligado al deviceId; `enterUnlinked()` lo borra) guarda qué permisos estaban concedidos, porque al quitar uno en Ajustes Android puede matar el proceso; `revokedPermissions` (función pura) compara con la última
  lectura y no avisa sin línea base; `PermissionRevokedScreen` («Se desactivó un permiso», chip «Pendiente», «Activar de nuevo» → Detalle de S66, «Volver al inicio»). La comprobación va en el ON_RESUME que ya releía los permisos y también tras `checkLink` (por si Android mata la app al quitar la ubicación);
  no avisa durante la bienvenida ni con un consentimiento pendiente. **Qué ve el tutor, verificado en el backend:** solo el acceso al uso viaja (`usage_access_granted` → alerta `PERMISSION_REVOKED`, `tamper.py:54-55`), por eso solo ese permiso dice «Tu tutor puede ver que este permiso está desactivado»; los demás no mencionan al tutor y un test lo prohíbe.
  Ninguna línea de efecto del shell cambia. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL, 220 tests, 0 fallos, lint 0 errores; tests de UI en dos tandas → **137 tests, 0 fallos**. Delegado (Opus). Límites: si se quitan varios a la vez se avisa del primero; si el permiso se quita durante la bienvenida o un consentimiento, el aviso se pierde.
- [x] T5 — Emulador `Pixel_8`: **«Se desactivó un permiso» con el permiso quitado de verdad** (`appops … deny` y volver): aviso correcto sin mencionar al tutor, «Activar de nuevo» abre el Detalle pendiente, el aviso no se repite y al restaurar el permiso el Detalle pasa solo a «Configurado»; pasada de letra 2.0 de las pantallas nuevas;
  **prueba G-19** con las rutas nuevas (3 min 13 s, mismos `ServiceRecord` y proceso, un latido por minuto, B1 por vuelta, sin 5xx). El primer intento de G-19 «falló» por mi guion (`am start -n` crea instancias nuevas de la actividad): se diagnosticó con el registro de eventos y un experimento de control, y se repitió volviendo por el intento del lanzador; el intento erróneo queda guardado.
  Evidencia e incidentes de entorno (RAM, emulador bloqueado por su PIN tras un `adb reboot`, caída de `qemu`) en `docs/sprint-67-evidence.md`. Inline.
- [ ] T6 — `verifier` (**Haiku**) y `security-reviewer` (**Opus**: revisión global `main...android-redesign`), `docs/progress.md`, `docs/tasks.md`, README, CI y PR de S67.
- [ ] T7 — Fusionar `main` en `android-redesign`, CI y PR `android-redesign` → `main` (lo fusiona el dueño); `/cerrar-sprint 67`.

## Criterios de aceptación

- Bienvenida, aviso de batería y «Se desactivó un permiso» siguen la estructura de las láminas 13.1 a 13.4 y no afirman nada falso (cada afirmación con su fuente).
- Ninguna clave de efecto de servicio del shell cambia; prueba G-19 con salida real.
- Pasada de 200 % sin textos cortados ni botones aplastados, en emulador; el 200 % en el teléfono real queda declarado como pendiente.
- `security-reviewer` sin ALTA; `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde.

## Pendiente que necesita una persona (no se puede hacer desde aquí)

- **Galaxy S25 FE:** TalkBack y Accessibility Scanner en todas las pantallas nuevas, letra al 200 %, «quitar animaciones», los pasos de batería en One UI, y quitar un permiso en Ajustes y recuperarlo.
- **Prueba con cinco personas** (`05_PRUEBA_USUARIOS.md`): fila aparte «tras S67».
- **Tutor real** desde el panel web: la vista remota de punta a punta, «Atrás» y la caducidad (S65), y desvincular desde el panel (S64).

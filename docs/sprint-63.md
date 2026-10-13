# Sprint 63 — Acceso, Vincular y cerrar sesión con confirmación

Tercer sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`). Láminas 01 (Acceso), 02 (Vincular)
y 10 (Más ajustes y confirmación de cierre de sesión), más los textos de `02_PANTALLAS.md` §01, §02 y §10. Cierra la mitad de
B-07 que quedó abierta en S61: cerrar sesión apaga la protección, así que ahora pide confirmación.

Rama `sprint-63-acceso-vincular`, creada desde `android-redesign` (`93ced9e`), en el worktree `C:/Users/andre/np-s63`.

## Decisiones

- **Errores por causa** (decisión 5 del plan: `ACCESS_NETWORK_STATE`, un permiso normal sin diálogo):
  - *Sin Internet:* el teléfono no tiene red. «No hay conexión a Internet. Comprueba tu conexión e inténtalo de nuevo.»
  - *Servicio no disponible:* hay red, pero no responde el servidor (conexión rechazada, tiempo agotado o 5xx; B4: un 5xx ya no se
    muestra como «sin conexión»). «NetProtect no puede conectarse al servicio ahora. Inténtalo de nuevo en unos momentos.»
  - *Sesión caducada:* se pasa a la variante «Vuelve a entrar» con «Tu sesión caducó.».
  - *Demasiados intentos* (429 al canjear el código): «Hiciste demasiados intentos. Espera unos minutos y vuelve a intentarlo.»
  - El resto sigue con el texto genérico actual.
- **Fotografía del login:** la de la lámina es `study.jpg` del panel web (`frontend/public/login/`, de los assets de diseño del
  dueño). Se reutiliza en la app, redimensionada a 1080 px de ancho y con un peso de ~100 KB.
- **Cerrar sesión pide confirmación** («¿Cerrar sesión?») donde el menor puede pulsarlo con la protección activa: Vincular
  (arriba a la derecha) y «Más ajustes». Cerrar sesión no desvincula. En `TutorAccountScreen` y `RoleCheckFailedScreen` no hay
  protección activa y no pide confirmación.
- **«Más ajustes»** es una ruta nueva del shell, accesible desde Inicio, con «Cerrar sesión» y la nota «Para desvincular este
  dispositivo, pídeselo a tu tutor. Solo tu tutor puede hacerlo.» (la lámina dice «Solo él»: se cambia, porque no se supone el
  género del tutor). La fila «Privacidad» se añade en S66, cuando exista esa pantalla; no se muestra una fila que no lleve a
  ninguna parte. El rediseño completo de Inicio es S64: aquí solo cambia «Cerrar sesión» por una fila «Más ajustes».
- No cambia el backend. `SupervisedShell` solo recibe una ruta nueva y un parámetro; los efectos de los servicios no se tocan
  (G-19).

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [ ] T2 — Errores tipados: `UiError` con los casos de arriba, comprobación de red con `ConnectivityManager` y el permiso
  `ACCESS_NETWORK_STATE`, mapeo en el login, la restauración de sesión, el chequeo de salud y Vincular, función pura con
  tests JVM. Delegado (**Opus**: red, sesión y un permiso nuevo).
- [ ] T3 — Pantallas: Carga, Inicio de sesión (con la fotografía y las variantes de error y «Vuelve a entrar»), Vincular
  (`NpCodeField`, estados esperando, vinculando e inválido), «Más ajustes» y el diálogo de confirmación, con sus frames de
  galería y tests. Delegado (**Opus**: oficio visual; un escritor distinto de T2).
- [ ] T4 — Revisión visual en el emulador a letra 1.0 y 2.0 contra las láminas 01, 02 y 10, y arreglos. Inline (miro yo las
  capturas; los lotes de arreglo, delegados).
- [ ] T5 — `verifier` (**Haiku**), `security-reviewer` (**Opus**: toca el manejo de sesión y los errores de login) y CI.

## Criterios de aceptación

- Sin Internet y servicio caído se distinguen; un 5xx no se muestra como «sin conexión»; el 429 de Vincular tiene su texto.
- Cerrar sesión pide confirmación en Vincular y en «Más ajustes», y los servicios no se tocan al cancelar.
- Las pantallas siguen las láminas 01, 02 y 10 en estructura y textos, y con letra al 200 % no se corta nada.
- `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde; `security-reviewer` sin ALTA.

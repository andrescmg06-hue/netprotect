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
- [ ] T2 — Bienvenida de 2 pasos: pantallas, bandera, estado de presentación en el shell, frames de galería y tests. Delegado (**Opus**: shell y primera impresión del menor).
- [ ] T3 — Aviso de batería: comprobación, intent a ajustes, icono, pantalla/aviso, galería y tests. Delegado (**Opus**).
- [ ] T4 — «Se desactivó un permiso»: detección al volver, aviso con «Activar de nuevo», galería y tests. Delegado (**Opus**: shell, `ON_RESUME`).
- [ ] T5 — Emulador: pasada de letra 1.0 y 2.0 de las pantallas nuevas y de las que faltan, «quitar animaciones», quitar un permiso en Ajustes y recuperarlo, y la **prueba G-19**. Inline.
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

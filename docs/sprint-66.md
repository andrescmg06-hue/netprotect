# Sprint 66 — Bloqueo, Privacidad y Detalle de permiso

Sexto sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`, fila S66). Láminas 07 (App bloqueada, 7 variantes), 10.5 (Privacidad)
y 11 (Detalle de permiso), con los textos de `02_PANTALLAS.md` §07, §10.5 y §11. Riesgo medio. Las pantallas de bloqueo no pasan por el shell (se dibujan en un
overlay de ventana o en `BlockScreenActivity`); Privacidad y Detalle son rutas de presentación nuevas del shell, así que G-19 sigue aplicando.

Rama `sprint-66-bloqueo-privacidad`, creada desde `android-redesign` (`40fd78d`), en el worktree `C:/Users/andre/np-s66`.

## Decisiones

- **Bloqueo (lámina 07): solo cambia el estilo.** Panel navy con «APP BLOQUEADA» como etiqueta pequeña, el candado y la ficha de la app, y debajo, sobre crema, el motivo como titular
  serif con su detalle y la fila-pista con filetes; nota al pie y «Ir al inicio» fijo abajo. **Se mantienen los textos de `BlockPresentation.kt`** (PLAN §2.1: «cambio solo de
  estilo»): la lámina trae frases distintas («Llegaste al límite diario de esta app.»), pistas que no están en el texto de §07 y alguna que podría ser falsa (la de categoría dice
  «bloqueadas por ahora» y el código dice «limita» a propósito porque una regla de categoría puede ser un límite o un horario; «Se desbloquea cuando termine el horario» promete un
  desbloqueo que el código no garantiza). La nota al pie pasa a «El bloqueo cubre la pantalla de la app, pero no la cierra.» (PLAN §2.2, es verdad). El candado se anima una sola
  vez y respeta «quitar animaciones» (`rememberReducedMotion`, S62).
- **Privacidad (lámina 10.5): solo textos verdaderos según el código.** Se publica «NetProtect no graba ni guarda lo que se ve en tu pantalla.» (sin `ImageReader`/`MediaRecorder` en
  Android ni en la web, el backend solo retransmite señalización y el vídeo va cifrado por TURN). Se **añade** lo que la lámina omite y es cierto: el teléfono también envía al tutor la
  lista de apps y el tiempo de uso de cada una (cada 5 min, `APP_SYNC_INTERVAL_MS`) y el latido lleva la versión de Android y de la app, la zona horaria y si el acceso al uso está
  concedido. **No se afirma ninguna retención**: solo la de ubicación (7 días) está documentada; la del uso de apps no, y `docs/security-baseline.md` la declara control diferido. «Sobre
  grabación y almacenamiento…: texto pendiente de validar» es una nota de diseño y no se publica.
- **Detalle de permiso (lámina 11): los pasos reales de cada permiso**, distintos entre sí (PLAN §2.1): el acceso al uso abre la lista general de «Acceso especial», donde hay que buscar
  NetProtect; la ubicación aproximada **no** abre Ajustes (es el cuadro de Android, «Permitir ubicación aproximada»); superponer sí abre la página del permiso de NetProtect; el
  administrador de dispositivo abre la pantalla de activación del sistema y **solo avisa al tutor**, no impide desinstalar. Los textos de pasos de los permisos que no están en
  `02_PANTALLAS.md` se redactan sin inventar capacidades y reutilizando las descripciones de `PermissionsScreen`. «Comprobando…» solo se muestra mientras se espera la relectura al
  volver de Ajustes (no hay espera artificial). No hay código de intents nuevo: el detalle usa los lanzadores que el shell ya tiene.
- **Entradas:** fila «Privacidad» en Inicio, junto a «Más ajustes» (decisión 4 de `03_DECISIONES_Y_PENDIENTES.md`: se llega desde Inicio, no desde Más ajustes); en Permisos, una acción
  compacta «Ver detalle» bajo cada permiso, sin cambiar el botón principal. «Atrás» del detalle vuelve a Permisos, no a Inicio.
- **Ubicación denegada para siempre:** hoy no hay salida. El detalle de ubicación ofrece «Abrir Ajustes de la app» (`ACTION_APPLICATION_DETAILS_SETTINGS`) solo en ese caso.
- No cambia el backend. Ningún efecto de servicio del shell cambia de clave (G-19).

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint y poner `CONTINUAR.md` al día. Inline. (este commit)
- [ ] T2 — Bloqueo restilado: las 7 variantes sobre `BlockScreenContent`, nota al pie nueva, candado animado una vez, galería y tests (`BlockPresentationTest`, `BlockScreenContentTest`).
  Delegado (**Opus**: oficio visual; sin shell).
- [ ] T3 — Privacidad y Detalle: pantallas, rutas `Privacy` y `PermissionDetail` en `SupervisedRoute`, el `BackHandler`, la fila de Inicio y «Ver detalle» en Permisos, el callback
  «Abrir Ajustes de la app», galería y tests. Delegado (**Opus**: toca el shell y textos de privacidad de un menor).
- [ ] T4 — Revisión visual en el emulador a letra 1.0 y 2.0 contra las láminas 07, 10.5 y 11, y la **prueba G-19** (3 min navegando, incluyendo Privacidad y Detalle). Inline.
- [ ] T5 — `verifier` (**Haiku**), `security-reviewer` (**Opus**: que los textos de privacidad sean verdad y que el detalle solo dispare los lanzadores existentes), CI y PR.

## Criterios de aceptación

- Las 7 variantes del bloqueo siguen la lámina 07 en estructura y conservan los textos del código; la nota al pie es la nueva.
- Ninguna afirmación de Privacidad o Detalle es falsa según el código (cada una con su fuente en `docs/sprint-66-evidence.md`); ninguna promete retención que no esté documentada.
- Ubicación y administrador de dispositivo no dicen que abren Ajustes; el administrador no dice que impide desinstalar.
- La prueba G-19 con salida real; `security-reviewer` sin ALTA.
- `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde.

## Pendiente que necesita una persona

- Ver las 7 variantes del bloqueo en el teléfono real con reglas reales (la técnica de `docs/sprint-49-evidence.md` §3 sirve para provocarlas en el emulador), y los pasos de cada permiso en
  One UI del Galaxy S25 FE (S67 los repasa).

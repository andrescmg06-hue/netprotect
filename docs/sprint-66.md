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
- [x] T2 — `19f5133`, `5721a0b`. `BlockScreenContent` en v2: panel navy con «APP BLOQUEADA» como etiqueta, el candado (`BlockPadlock`, animado una vez, respeta «quitar animaciones» y no se reinicia al
  recomponer) y la ficha de la app; debajo, sobre crema, el motivo como titular serif y la pista (fila con icono y filetes en límite diario, semanal, horario y modo escolar; detalle gris en los demás). Textos
  del código intactos; solo cambia la nota al pie: «El bloqueo cubre la pantalla de la app, pero no la cierra.». Mismas firmas públicas (el overlay y `BlockScreenActivity` no se tocan). Los campos `tone` y
  `badgeIcon` ya no se dibujan (la lámina usa un solo candado azul) y se conservan documentados. Con un solo texto por motivo, «bloqueada» muestra titular y detalle algo redundantes: se valora en T4.
  `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 1m 12s, 182 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` en
  `Pixel_8` → **113 tests, 0 fallos**. Delegado (Opus). A mirar en T4: los márgenes de las barras del sistema dentro del overlay (el escritor usó un margen mínimo fijo por si la ventana no los recibe).
- [x] T3 — `89e80f2`, `e9a438c`. `PrivacyScreen` con solo lo que el código respalda (latido cada minuto con versión, zona horaria y si el acceso al uso está concedido; lista de apps y tiempo de uso cada 5 min; aviso de cada
  bloqueo; ubicación aproximada cada 15 min; no se graba ni se guarda la pantalla; **ninguna retención**) y `PermissionDetailScreen` con pasos reales distintos por permiso (ubicación: cuadro de Android, no Ajustes;
  administrador: pantalla de activación, solo avisa al tutor, no impide desinstalar; «Abrir Ajustes de la app» con la ubicación pendiente). El botón del administrador dice «Activar protección» (no «Activar en Ajustes»).
  Rutas `Privacy` y `PermissionDetail` solo de presentación (`detailPermission` es `rememberSaveable`, ninguna clave de efecto cambió; «Atrás» del detalle → Permisos), fila «Privacidad» en Inicio y «Ver detalle» bajo cada
  permiso. Comprobé yo cada afirmación de privacidad contra el código (`SyncWorker`, `reportRuleEvent`, campos del latido, `onDisableRequested`, manifiesto). No se implementó «Comprobando…» (la relectura es instantánea y sería una
  espera falsa). `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 1m 26s, 194 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` → **129 tests, 0 fallos**.
  Delegado (Opus).
- [x] T4 — Emulador `Pixel_8`: revisión visual a letra 1.0 y 2.0 contra las láminas 07, 10.5 y 11; **bloqueo real** con YouTube (regla `BLOCK` que ya existía, sin tocar la base de datos) con navegación por gestos y por tres botones y
  letra 1.0 y 2.0: «Ir al inicio» siempre por encima de la barra del sistema, así que los márgenes del overlay no son un problema; **prueba G-19** en la app real incluyendo Privacidad y Detalle (3 min 15 s, mismos `ServiceRecord` y proceso,
  un latido por minuto, B1 en cada vuelta, sin 5xx). Emulador restaurado (gestos y letra 1.0). Evidencia en `docs/sprint-66-evidence.md`. Inline.
- [x] T5 — `security-reviewer` (**Opus**): **sin hallazgos ALTA**; 2 arreglados, ambos medidos en el emulador antes de tocar el texto. MEDIA: Privacidad decía «cada minuto mientras la app esté abierta; cuando no lo está, también
  puede enviarlo con menos frecuencia», y con NetProtect en segundo plano (sin cerrarla, el lanzador delante) el latido llegó cada minuto (18:21, 18:22, 18:23) y la lista de apps cada 5 (18:15:10, 18:20:10): para un menor «abierta» es «en pantalla»
  y el texto subestimaba lo que se envía; ahora «mientras NetProtect esté en marcha, aunque uses otras apps», sin afirmar nada de lo que pasa al cerrarla (no se probó), y lo mismo en Estado (la frase era de antes del sprint).
  BAJA: el Detalle de «Mostrar sobre otras apps» decía «se abre la página de este permiso para NetProtect»; en Android 11+ el sistema ignora el paquete y abre la lista general «Display over other apps» (visto en el emulador API 36); ahora lo dice y manda
  buscar NetProtect. El revisor confirmó que el resto de las afirmaciones de privacidad son verdad, que `onOpenAppSettings` solo abre los ajustes de esta app, que las rutas nuevas solo se pintan en `Linked` y que ninguna clave de efecto cambió.
  `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks` (sin caché) → BUILD SUCCESSFUL en 2m 39s, 194 tests, 0 fallos, lint 0 errores; `connectedDebugAndroidTest` → **129 tests, 0 fallos**.
  Falta el CI de los 8 jobs, que corre con el PR.

## Criterios de aceptación

- Las 7 variantes del bloqueo siguen la lámina 07 en estructura y conservan los textos del código; la nota al pie es la nueva.
- Ninguna afirmación de Privacidad o Detalle es falsa según el código (cada una con su fuente en `docs/sprint-66-evidence.md`); ninguna promete retención que no esté documentada.
- Ubicación y administrador de dispositivo no dicen que abren Ajustes; el administrador no dice que impide desinstalar.
- La prueba G-19 con salida real; `security-reviewer` sin ALTA.
- `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde.

## Pendiente que necesita una persona

- Ver las 7 variantes del bloqueo en el teléfono real con reglas reales (la técnica de `docs/sprint-49-evidence.md` §3 sirve para provocarlas en el emulador), y los pasos de cada permiso en
  One UI del Galaxy S25 FE (S67 los repasa).

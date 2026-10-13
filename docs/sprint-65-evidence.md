# Sprint 65 — evidencia

Comandos reales y su salida. Árbol de trabajo: `C:/Users/andre/np-s65`, rama `sprint-65-consentimiento-estado`. Emulador `Pixel_8` (Android 16),
build de debug, cuenta supervisada del dueño ya vinculada. Capturas y registros en `docs/android-redesign/supervisado-v2/s65/`.

## Builds sin caché y pruebas por tarea

`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --rerun-tasks`, ejecutados por un `verifier` distinto del escritor,
con `:app:testDebugUnitTest` ejecutada (no UP-TO-DATE):

| Tarea | Resultado |
|---|---|
| T2 (B2 y ventana de consentimiento) | BUILD SUCCESSFUL en 2m 6s, 162 tests, 0 fallos, lint 0 errores |
| T3 (shell, B5 en el teléfono) | BUILD SUCCESSFUL en 2m 25s, 170 tests, 0 fallos, lint 0 errores |
| T5a (consentimiento, Estado, banner) | BUILD SUCCESSFUL en 2m 34s, 175 tests, 0 fallos; `connectedDebugAndroidTest` en `Pixel_8`: **110 tests, 0 fallos** |
| T5b (avisos permanentes) | BUILD SUCCESSFUL en 1m 15s, 178 tests, 0 fallos, lint 0 errores |
| T4 (web + test de backend) | `make test` en contenedor: **287 passed**, 0 fallos (285 antes + los 2 casos nuevos); `eslint` y `tsc` de la web pasan; `next build` no se pudo correr localmente (enlace de `node_modules`) y lo cubre el CI |

## T6 — emulador

### Revisión visual (galería `ScreensGalleryActivity`, letra 1.0 y 2.0)

- **Consentimiento** (láminas 06-1 y 06-2): cabecera con flecha y escudo, tres pasos «1 · Leer, 2 · Autorizar, 3 · Android», titular serif, las tres líneas con
  icono, casilla, «Esta solicitud caduca en 2 min / 1 min 48 s» con barra de progreso, nota «Después, Android mostrará su propio aviso…», pie fijo con
  «Marca la casilla para continuar.», «Continuar» y «Ahora no». Sin solicitud (`requestedAt = null`) no hay reloj ni barra.
- **Estado** (lámina 05): lista plana con «Activo» y «Sin confirmar» en ubicación, «Último reporte: hace 1 min», pie fijo y «Volver al inicio»; el texto de
  ubicación en «Sin confirmar» dice «No hay una ubicación reciente confirmada.» (el de la lámina, «Todavía no recibimos una ubicación», era inexacto con un
  envío de hace más de 35 min).
- A letra 2.0 las etiquetas de los pasos pasan a una por línea y nada se corta ni se parte por letras.
- Diferencias aceptadas con las láminas: «Continuar» deshabilitado en azul al 55 %, no gris; pasos pendientes más oscuros por contraste AA; sin fila
  «Visualización de pantalla» en la lista de Estado; la galería pone el banner sobre Estado como en el shell.

### Prueba G-19 — navegar no reinicia servicios

Guion en `g19-serviceRecords.log`. Durante 3 min 11 s se repitió 6 veces: Permisos → Atrás → Estado → Atrás → Más ajustes → Atrás → Inicio del sistema → volver a la app.

```
T0 14:15:59 UTC  ServiceRecord{438cea2 … RuleEnforcementService}    createTime=-10s578ms   lastStartId=1
                 ServiceRecord{e718733 … LocationReportingService}  createTime=-10s575ms   lastStartId=1   pid 14083
T1 14:19:10 UTC  ServiceRecord{438cea2 … RuleEnforcementService}    createTime=-3m22s97ms  lastStartId=1
                 ServiceRecord{e718733 … LocationReportingService}  createTime=-3m22s94ms  lastStartId=1   pid 14083
```

Mismos `ServiceRecord`, mismo proceso y `lastStartId=1`: ningún servicio se paró ni se relanzó. Backend (`backend-heartbeat-y-b1.log`): **latidos a las 14:16:49,
14:17:49 y 14:18:49** (uno por minuto) y **6 consultas B1** `GET /devices/me`, todas 200, sin ningún 5xx ni `Traceback`.

### Avisos permanentes (`dumpsys notification --noredact`, `avisos-permanentes.txt`)

Control de apps: «Control de apps funcionando en este dispositivo.» · Ubicación: «Se envía una ubicación aproximada cada 15 minutos.» Los textos antiguos
(«Verificando las reglas…», «Tu tutor puede ver la ubicación…») ya no aparecen.

## Pendiente (declarado)

- **No probado de punta a punta con un tutor real:** la solicitud de vista remota desde el panel web, el consentimiento con la cuenta atrás real, «Atrás» y la caducidad
  enviando `dismissed` o `expired` y la web mostrándolos, y el límite de espera de 135 s en un navegador. Pedir la vista exige iniciar sesión con Google en el panel; el teléfono
  solo recibe la solicitud por el WebSocket. Lo cubren la lógica pura (`ConsentFlowTest`, 8 tests), el test de backend y la lectura del código.
- **No probado:** el **toque real sobre la notificación de vista remota** (que abra la app, que no la duplique y que se pueda detener desde ahí): esa notificación solo existe
  durante una vista remota. Tampoco si los avisos permanentes se pueden descartar en Android 16, TalkBack ni «quitar animaciones».
- **Riesgo anotado para S67:** a letra 2.0 el pie fijo del consentimiento y de Estado ocupa buena parte de una pantalla pequeña; el contenido hace scroll por encima, pero hay
  que revisarlo en la pasada de 200 %.
- B5 es de **mejor esfuerzo y sin cola**: con el socket del teléfono caído, el tutor no se entera de que la solicitud se cerró (la web lo cubre con su límite de 135 s).
- La ubicación «Sin confirmar» solo se vio en la galería, no con un envío real fallando.
- T7 (`security-reviewer` y CI) sigue abierta.

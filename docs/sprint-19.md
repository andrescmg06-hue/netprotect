# Sprint 19 — Funcionamiento offline

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 18) pide: "Room como fuente local de reglas, horarios,
límites, listas y cola de eventos pendientes. Estrategia de conflicto y versión de política; las
reglas siguen aplicándose sin Internet." Hasta este sprint, `RuleEnforcementService` (Sprint 8)
guardaba su copia de las reglas sólo en variables locales del bucle de sondeo: sobrevivía a una
caída de red a mitad de sesión, pero un arranque en frío sin conectividad (el proceso murió y el
usuario reabre la app sin señal) partía de "todo permitido, sin horario escolar" en vez de la
última configuración real conocida, y un bloqueo que no se podía reportar
(`client.reportRuleEvent`) se perdía en silencio (`runCatching` sin manejo). El propio código de
`SupervisedScreen.kt` (comentario del *heartbeat*, Sprint 18) ya señalaba que "un horario en
segundo plano real (WorkManager) es tarea del Sprint 19" — confirmado también en el plan.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-072 | Como dispositivo supervisado que arranca sin conexión, quiero seguir aplicando la última configuración de reglas conocida en vez de permitir todo. |
| HU-073 | Como dispositivo supervisado, quiero que un bloqueo que no pude reportar por falta de red no se pierda, sino que se reporte en cuanto vuelva la conexión. |
| HU-074 | Como dueño del proyecto, quiero que el *heartbeat* y la sincronización de uso de apps sigan ocurriendo aunque la app no esté en primer plano. |

## Criterios de aceptación

1. Al arrancar `RuleEnforcementService` sin conectividad (o antes de que la primera petición de
   red responda), el dispositivo evalúa apps contra la última configuración cacheada en Room, no
   contra los valores de reserva "todo permitido, sin horario escolar" — salvo que nunca se haya
   cacheado nada (primer arranque real, sin caché).
2. Cada `getActiveRules()` exitoso reemplaza por completo el caché local (reglas, categorías,
   asignaciones, política, horario escolar) — sustitución total, no fusión: ver "Estrategia de
   conflicto" abajo.
3. Un `reportRuleEvent` que falla queda encolado en Room (`pending_rule_events`) en vez de
   perderse; una `getActiveRules()` exitosa posterior intenta primero vaciar la cola antes de
   continuar.
4. `SyncWorker` (WorkManager, periódico, con `NetworkType.CONNECTED` como restricción) envía
   *heartbeat*, sincroniza aplicaciones (si el permiso de uso sigue concedido) y vacía la cola de
   eventos pendientes, incluso con la app fuera de primer plano — programado/cancelado junto con
   los demás servicios en `SupervisedScreen`.
5. Ni `RuleEnforcementService` ni `SyncWorker` dependen de un `accessToken` fijo pasado una vez:
   ambos renuevan su propio token contra el `refresh_token` cifrado ya almacenado
   (`TokenStore`, Sprint 3) en vez de fallar en silencio pasados los 15 minutos de vida del token
   de acceso.

## Decisiones de diseño y su motivo

### Estrategia de conflicto: el servidor siempre gana, sin fusión

El dispositivo supervisado nunca edita una regla — sólo el tutor lo hace, vía el panel/la app de
tutor, siempre contra el backend. No hay, por tanto, ninguna edición local con la que "el servidor"
pueda entrar en conflicto: cada `RulesCacheStore.replaceAll()` borra por completo las cuatro tablas
de caché de ese dispositivo y las vuelve a insertar desde la respuesta más reciente de
`GET /rules/active`. "La versión de política" no es un número que este sprint invente y exponga en
ningún sitio: no hay ningún requisito ni enunciado que lo pida, el propio `AppRuleEvent` del backend
tampoco versiona contra qué configuración se evaluó un bloqueo (Sprint 8), y añadir uno sólo para
tener "una estrategia de versión" habría sido una abstracción sin usuario. Lo único que se guarda es
`cachedAtEpochMs`, un marcador de antigüedad para depuración, no una clave de fusión.

### Cola de eventos pendientes: sólo bloqueos de reglas, con entrega al menos una vez

"Cola de eventos pendientes" se interpretó, deliberadamente, como el reporte de bloqueos ya
aplicados (`AppRuleEvent`) — es la única señal cuya pérdida cambia lo que el tutor ve sin que nada
la reemplace después (a diferencia del *heartbeat*, donde el siguiente ping ya deja obsoleto al que
se perdió, o de la ubicación, que Sprint 13 ya aceptó perder si el proceso muere). Sin clave de
idempotencia: un evento reportado con éxito pero cuya respuesta nunca llegó a este dispositivo (una
conexión cortada justo tras el 200) puede reportarse dos veces. Aceptado con el mismo criterio que
"sin reinicio tras la muerte del proceso" del Sprint 8 — el peor caso es una fila duplicada en
`app_rule_events` o un `occurrence_count` de alerta un poco inflado, no un dato perdido ni una
regla mal aplicada.

### `SyncWorker`: complementa el sondeo en primer plano, no lo reemplaza

`SupervisedScreen` ya envía *heartbeat* cada 60s y sincroniza apps cada 5 min mientras la pantalla
está compuesta — más frecuente de lo que WorkManager permite (`MIN_PERIODIC_INTERVAL_MILLIS` = 15
min, un piso de la plataforma, no una elección de este proyecto). Se mantienen ambos: los bucles en
primer plano para la cadencia ajustada mientras la app está abierta, `SyncWorker` como red de
seguridad cuando no lo está — incluida la única vía que sigue funcionando si el proceso de
`RuleEnforcementService` muere (WorkManager corre en su propia ejecución, independiente del proceso
de la app que lo programó), aunque *sólo* para *heartbeat*/sincronización/cola pendiente: el propio
bloqueo de apps sigue sin reiniciarse solo (límite ya aceptado desde el Sprint 8, sin cambios).

### Renovar el `access_token` en segundo plano, sin tocar `AuthRepository`

El *access token* dura 15 minutos (`access_token_ttl_minutes`), pero tanto
`RuleEnforcementService` como `SyncWorker` corren sesiones mucho más largas que eso entre aperturas
de la app — sin renovarlo, todas sus peticiones habrían empezado a fallar con 401 en silencio a los
15 minutos de cualquier sesión, lo que habría hecho falsa la promesa central de este sprint para el
caso mucho más común que "llevar horas sin Internet": simplemente llevar más de 15 minutos
encendido. `BackgroundTokenRefresher` (nuevo) resuelve esto leyendo el mismo `refresh_token`
cifrado en el Keystore que ya usa `AuthRepository.restoreSession()` (Sprint 3) y llamando al mismo
`AuthClient.refresh()` — reutiliza infraestructura ya probada, sin tocar el modelo de sesión de la
UI en primer plano (`HomeScreen`/`AuthRepository`), que queda exactamente igual. Persiste la
rotación del `refresh_token` (el backend lo rota en cada uso) para no dejar el siguiente intento
—desde la UI o desde otro *worker*— usando uno ya revocado.

### Room vía KSP, no kapt

Se intentó primero `kapt` (el procesador de anotaciones histórico de Kotlin) y falló en la primera
compilación real: el backend `javac` de `room-compiler:2.7.1` empaqueta su propia versión fija de
`kotlin-metadata-jvm`, que sólo entiende metadatos hasta el formato 2.2 — el compilador Kotlin
2.3.21 de este proyecto emite formato 2.3, así que Room ni siquiera llegaba a procesar la primera
entidad (`IllegalArgumentException: Provided Metadata instance has version 2.3.0, while maximum
supported version is 2.2.0`). KSP lee los símbolos de Kotlin directamente por la API del propio
plugin del compilador, no por esa librería aparte, y no tiene ese techo — cambiar a
`com.google.devtools.ksp` (versión `2.3.11`, independiente de la del compilador desde que KSP dejó
el esquema `<kotlin>-<ksp>`) resolvió la compilación sin más cambios. Ver evidencia para el error
real y la corrida en verde posterior.

## Fuera de alcance

- **Ubicación, historial, estadísticas, alertas**: sin cola de reintentos propia. Ubicación ya
  aceptó perder reportes si el proceso muere desde el Sprint 13; los demás son de sólo lectura
  desde el dispositivo (nunca escribe historial/estadísticas/alertas), así que no hay nada que
  encolar del lado del dispositivo.
- **Reintento con backoff exponencial o límite de antigüedad en la cola**: `PendingRuleEventStore`
  reintenta en cada `getActiveRules()` exitoso y en cada corrida de `SyncWorker`; no hay un tope de
  cuántas veces o durante cuánto tiempo se reintenta un evento. Aceptable a esta escala (un
  dispositivo genera, como mucho, unos pocos bloqueos por hora); revisar si en algún momento la
  tabla crece sin límite en un dispositivo real.
- **Renovación de `access_token` en `AuthRepository`/UI en primer plano**: el mismo problema de
  fondo (un token de 15 minutos, sin renovación periódica) también existe ahí, pero está fuera del
  enunciado de este sprint (offline en el dispositivo supervisado) y tocar el modelo de sesión de
  `HomeScreen` es un cambio más amplio — queda anotado aquí para quien retome el proyecto, no
  resuelto.
- **Migraciones de esquema de Room**: la base de datos nace en `version = 1`; no existe todavía
  una versión anterior contra la que probar una migración real.

## Android

- `core/storage/`: `Entities.kt` (`CachedAppRuleEntity`/`CachedCategoryAssignmentEntity`/
  `CachedCategoryRuleEntity`/`CachedDevicePolicyEntity`/`PendingRuleEventEntity`), `Daos.kt`
  (`RulesCacheDao`/`PendingRuleEventDao`), `NetProtectDatabase.kt`, `RulesCacheStore.kt` (mapeo
  hacia/desde `core.rules.*` + transacción de reemplazo total), `PendingRuleEventStore.kt` (cola +
  vaciado).
- `core/auth/BackgroundTokenRefresher.kt` (nuevo): renovación de `access_token` independiente de
  `AuthRepository`, para trabajo en segundo plano.
- `core/sync/SyncWorker.kt` (nuevo, `CoroutineWorker`): *heartbeat* + sincronización de apps +
  vaciado de la cola pendiente, cada 15 min (piso de WorkManager), sólo con conectividad.
- `RuleEnforcementService.kt`: carga el caché de Room al arrancar, lo reemplaza tras cada fetch
  exitoso, encola los `reportRuleEvent` fallidos, renueva su token cada 10 min.
- `SupervisedScreen.kt`: programa/cancela `SyncWorker` junto con los demás servicios.
- `build.gradle.kts` (raíz y `app`): plugin KSP, dependencias `androidx.room:room-{runtime,ktx,
  compiler}:2.7.1` y `androidx.work:work-runtime-ktx:2.10.0`.

## Verificación

Ver `docs/sprint-19-evidence.md` para comandos y salida real.

## No se marca como verificado

- Login real de Google, mismo límite recurrente en todos los sprints anteriores.
- Recorrido manual en un emulador/dispositivo Android viendo el caché sobrevivir a un reinicio en
  frío sin red, la cola de eventos pendientes vaciarse al recuperar conexión, o `SyncWorker`
  ejecutar con la app cerrada — se verificó `./gradlew test assembleDebug` (el mismo comando que
  corre CI) en verde, pero nadie instaló ni ejerció la app en un dispositivo/emulador real en esta
  sesión.

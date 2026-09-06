# Sprint 9 — Lista blanca y negra

## Objetivo

El tutor puede invertir la política por defecto de un dispositivo: en vez de "todo permitido salvo
lo que bloquee" (lista negra, el comportamiento hasta el Sprint 8), puede ponerlo en "todo bloqueado
salvo lo que apruebe" (lista blanca). Con eso, la regla `ALLOW` que el Sprint 8 introdujo sin efecto
propio pasa a tener uno real: es la que aprueba una app cuando el dispositivo bloquea por defecto.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-034 | Como tutor, quiero poner un dispositivo en modo "sólo apps aprobadas", para que mi hijo pequeño únicamente pueda usar lo que yo autoricé. |
| HU-035 | Como tutor, quiero volver al modo "permitir salvo lo bloqueado" sin perder las reglas que ya definí. |
| HU-036 | Como tutor, quiero ver claramente en qué modo está cada dispositivo, para no confundir uno con otro. |
| HU-037 | Como usuario supervisado, quiero que el modo "sólo apps aprobadas" nunca me deje sin teléfono, sin pantalla de inicio ni sin acceso a Ajustes. |

## Criterios de aceptación

1. Cada dispositivo tiene una política por defecto (`ALLOW` o `BLOCK`); los dispositivos existentes
   quedan en `ALLOW`, que es exactamente el comportamiento previo al sprint (la migración no cambia
   la conducta de nada que ya estuviera funcionando).
2. Sólo un tutor vinculado puede cambiarla, con el mismo 404 uniforme del resto del proyecto para
   "no existe" y "no es tuyo"; el cambio queda auditado.
3. Cambiar de modo no borra ni altera ninguna regla existente: al volver a `ALLOW`, las reglas
   previas siguen aplicando igual que antes.
4. El dispositivo supervisado recibe la política junto con sus reglas y la evalúa localmente, sin
   una consulta extra por cada apertura de app.
5. Con política `BLOCK`: una app sin regla queda bloqueada; una app con regla `ALLOW` queda
   permitida; una app con regla `BLOCK` sigue bloqueada; `DAILY_LIMIT` y `SCHEDULE` siguen
   evaluándose como en el Sprint 8 (permiten la app mientras no se supere el límite o no se esté
   dentro de la franja).
6. Con política `ALLOW`: el comportamiento es idéntico al del Sprint 8, sin ninguna diferencia
   observable.
7. **Nunca se bloquea**, en ningún modo: la app de inicio (launcher), la app de teléfono (la elegida
   por el usuario y la preinstalada), Ajustes, ni NetProtect misma — resueltas en tiempo de
   ejecución, no hardcodeadas.
8. La pantalla de bloqueo distingue el motivo "este dispositivo sólo permite apps aprobadas" de los
   motivos del Sprint 8, y el evento reportado al backend registra ese motivo como tal.
9. El panel web muestra el modo actual de cada dispositivo y permite cambiarlo, advirtiendo qué
   implica activar el modo lista blanca.

## Procedimiento obligatorio de la Fase C — verificación antes de codificar

Aplicado y documentado en `docs/android/capability-matrix.md` (sección "Sprint 9"). Lo verificado:

- El launcher actual se resuelve con `PackageManager.resolveActivity()` sobre `ACTION_MAIN` +
  `CATEGORY_HOME` con `MATCH_DEFAULT_ONLY`; Ajustes con `Settings.ACTION_SETTINGS`. Ambas pueden
  devolver `null`.
- El teléfono sale de `TelecomManager.getDefaultDialerPackage()` **y** `getSystemDialerPackage()`
  (verificado en el javadoc real de AOSP): el usuario puede haber cambiado el dialer por defecto, así
  que las dos son candidatas legítimas. Las dos pueden devolver `null`.
- La visibilidad de paquetes de Android 11+ no es un problema aquí porque el proyecto ya tiene
  `QUERY_ALL_PACKAGES` desde el Sprint 7.
- Límite honesto documentado: si alguna resolución devuelve `null`, esa app no entra en la lista
  protegida y podría bloquearse. No se compensa con paquetes hardcodeados, que varían por fabricante
  y darían una falsa sensación de cobertura.

## Decisiones de diseño relevantes

- **Un modo por dispositivo, no una tabla nueva de "listas".** Decisión explícita del dueño del
  proyecto tras plantear la alternativa de construir el modelo completo de `plan-desarrollo.md`
  (listas con alcance por dispositivo *y* por tutor, con prioridad entre cuatro niveles). Lo que hace
  falta para "lista blanca/negra" es invertir el valor por defecto: la lista negra ya existe desde el
  Sprint 8 (las reglas `BLOCK`) y la lista blanca son las reglas `ALLOW` una vez que el defecto
  bloquea. Construir una tabla paralela duplicaría `app_rules` sin agregar capacidad.
- **La política vive en `devices`, no en una tabla `device_policies` nueva.** Es una sola columna;
  una tabla aparte sería ceremonia hoy. Cuando los Sprints 11/12 agreguen más ajustes por dispositivo
  (horarios, modo escolar) tendrá sentido evaluar extraerlos juntos, con datos reales sobre la mesa.
- **La política es del dispositivo, no del vínculo tutor-dispositivo.** Un dispositivo puede tener
  más de un tutor (`tutor_devices` es N:N) y las reglas del Sprint 8 ya son por dispositivo,
  compartidas entre sus tutores. Que la política siguiera al vínculo obligaría a resolver "¿qué pasa
  si un tutor dice lista blanca y el otro lista negra?" — una pregunta sin respuesta buena que
  desaparece manteniendo la coherencia con lo ya existente. Quién la cambió queda en `audit_logs`.
- **`ALLOW` recupera su significado, y se documenta el cambio.** El Sprint 8 dejó escrito, tanto en
  el modelo como en la UI, que `ALLOW` no se distinguía de "sin regla". Este sprint lo cambia: hay
  que actualizar esos textos en vez de dejarlos mintiendo.
- **La lista de apps protegidas se resuelve en el dispositivo, no se configura desde el backend.**
  Depende de qué launcher y qué dialer tiene *ese* teléfono; el backend no lo sabe ni debería. El
  tutor tampoco la edita: no es una preferencia, es una barrera de seguridad.
- **El motivo del bloqueo por política es un valor nuevo (`DEFAULT_POLICY`), no un `BLOCK`
  reutilizado.** Un padre que revisa el historial necesita distinguir "bloqueé esta app a propósito"
  de "esta app quedó fuera porque el dispositivo sólo permite lo aprobado". Implica extender el
  `CheckConstraint` de `app_rule_events` con una migración, que es justamente el punto: el registro
  debe poder expresar la diferencia.

## Fuera de alcance

- Listas con alcance por tutor (una lista que el tutor define una vez y aplica a todos sus
  dispositivos) — descartado explícitamente para este sprint, ver decisiones de diseño.
- Categorías de apps — Sprint 10, que además dará el siguiente nivel de la cadena de prioridad.
- Listas de navegación web / dominios: `user-stories.md` las menciona junto con las de apps, pero el
  filtrado web necesita `VpnService`, cuyo mecanismo todavía no tiene Fase C propia. Se separa a
  propósito para no mezclar dos motores de bloqueo distintos en un sprint, igual que se hizo en el 8.
- Normalización de zona horaria de `SCHEDULE` — sigue siendo del Sprint 11.

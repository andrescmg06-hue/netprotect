# Sprint 12 — Modo escolar

## Objetivo

El tutor activa un "modo escolar" por dispositivo: una franja horaria (07:00-14:00 por defecto,
configurable) en la que el dispositivo se vuelve más estricto automáticamente — sin que el tutor
tenga que cambiar reglas a mano dos veces al día. Fuera de esa franja, el dispositivo vuelve a su
comportamiento normal.

## Interpretación del alcance — decisión explícita

`plan-desarrollo.md` (Paso 11) describe el modo escolar en dos líneas: "perfil de reglas con
vigencia horaria (07:00-14:00 por defecto, configurable)" y "aplicación local sin conexión
permanente al backend". No hay más detalle en el repo. Interpretación adoptada, documentada como
tal (no como la única lectura posible):

**El modo escolar fuerza la política por defecto del dispositivo a `BLOCK` (modo lista blanca)
mientras está dentro de su franja horaria, sin tocar ni reemplazar ninguna regla existente.** Una
`AppRule`/`CategoryRule` con `ALLOW` sigue aprobando esa app también en horario escolar — es la
misma forma en que ya funciona la política por defecto fuera de este sprint (Sprint 9), reutilizada
en vez de inventar una segunda cadena de prioridad paralela ("perfil" separado con sus propias
reglas). Un tutor que quiera que una app funcione *sólo* fuera de horario escolar sigue pudiendo
lograrlo con una `SCHEDULE` normal — modo escolar no reemplaza esa herramienta, la complementa para
el caso común "bloquear todo salvo lo aprobado durante la escuela" sin tener que armar una
`SCHEDULE` por cada app.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-047 | Como tutor, quiero activar un modo escolar con una franja horaria, para que el dispositivo bloquee todo lo no aprobado automáticamente en ese horario. |
| HU-048 | Como tutor, quiero configurar la franja horaria del modo escolar (no sólo aceptar 07:00-14:00 por defecto). |
| HU-049 | Como tutor, quiero que mis reglas de apps/categorías aprobadas sigan funcionando durante el horario escolar, sin tener que duplicarlas. |
| HU-050 | Como tutor, quiero apagar el modo escolar y que el dispositivo vuelva a su comportamiento normal de inmediato. |

## Criterios de aceptación

1. `devices` gana modo escolar: activado/desactivado, hora de inicio, hora de fin, días de la
   semana — mismos campos y semántica que `SCHEDULE` (minutos desde medianoche, bitmask lunes=bit0).
2. Con el modo escolar activo y dentro de su franja, una app **sin** `AppRule` ni `CategoryRule`
   propia queda bloqueada (`BlockReason.SCHOOL_MODE`), igual que la política `BLOCK` ya bloquea hoy
   — pero sin que el tutor tenga que cambiar la política por defecto del dispositivo dos veces al
   día.
3. Una app **con** `AppRule`/`CategoryRule` sigue su propia regla sin cambios: `ALLOW` la aprueba
   incluso en horario escolar; `BLOCK` la bloquea siempre; `SCHEDULE`/`DAILY_LIMIT`/`WEEKLY_LIMIT`
   se evalúan exactamente igual que fuera de horario escolar.
4. Fuera de la franja horaria (o con el modo desactivado), el dispositivo se comporta exactamente
   como si el modo escolar no existiera — la política por defecto real del dispositivo
   (`default_app_policy`, Sprint 9) es la que manda.
5. Apps protegidas (`ProtectedPackages`, Sprint 9) siguen exentas del modo escolar, mismo criterio
   que ya las exime de la política por defecto.
6. Evaluado localmente en el dispositivo con la hora local ya disponible — no depende de una
   conexión al backend más allá del mismo refresco periódico de reglas que ya existe desde el
   Sprint 8 (satisface "aplicación local sin conexión permanente").
7. El tutor puede activar, configurar y desactivar el modo escolar desde el panel web.

## Decisiones de diseño relevantes

- **No es una "cuarta prioridad" ni un perfil de reglas paralelo — es una variante con vigencia
  horaria de algo que el proyecto ya tiene**: la política por defecto. Reutilizar
  `defaultPolicyOutcome` con una condición extra (¿hay modo escolar activo ahora?) es
  sustancialmente menos código y menos superficie de prueba que un sistema de perfiles
  intercambiables, y cumple los 4 criterios de aceptación que sí están escritos en el plan. Si un
  sprint futuro necesita perfiles de reglas completos e independientes, se construye entonces con
  un caso de uso real que lo justifique.
- **Un solo motivo de bloqueo nuevo, `SCHOOL_MODE`**, distinto de `DEFAULT_POLICY` — mismo criterio
  que ya separó `CATEGORY` de `DEFAULT_POLICY` en el Sprint 10: un tutor revisando el historial
  necesita distinguir "esto se bloqueó porque activé el modo escolar" de "esto se bloqueó porque el
  dispositivo está en modo lista blanca todo el tiempo".
- **Reutiliza la validación y el bitmask de `SCHEDULE` tal cual** (minutos 0-1439, días bit0=lunes)
  en vez de inventar un formato nuevo para la franja horaria del modo escolar.
- **Sin Fase C nueva**: la evaluación de franja horaria ya está verificada y probada desde el
  Sprint 8 (incluida la franja que cruza medianoche); modo escolar reutiliza esa misma función.

## Fuera de alcance

- Perfiles de reglas completos, intercambiables o definidos por el tutor más allá de este único
  modo con nombre fijo — no lo pide ningún criterio de este sprint (ver decisión de diseño arriba).
- Modo escolar por categoría o por app individual (por ejemplo, "modo escolar sólo para Juegos") —
  es una configuración por dispositivo completo, como el resto de la política por defecto.

## Backend

- `devices` gana `school_mode_enabled`/`school_mode_start_minute`/`school_mode_end_minute`/
  `school_mode_days_mask` (migración `e4ff038efe89`, con el `CheckConstraint` de
  `app_rule_events` ampliado a mano para aceptar `SCHOOL_MODE`, mismo motivo recurrente desde el
  Sprint 10). `PUT /devices/{id}/school-mode` (tutor) activa/desactiva y configura la franja;
  desactivar limpia los campos de ventana para no dejar una fila a medio configurar. `GET
  /devices/{id}/rules/active` incluye `school_mode` en la misma respuesta que ya trae reglas,
  categorías y política por defecto.

## Android

- `SchoolMode` (nuevo) y `BlockReason.SCHOOL_MODE`. `RuleEvaluator.defaultPolicyOutcome()`
  reutiliza `isWithinSchedule()` (ya existente para `SCHEDULE`) para decidir si el modo escolar
  está en su ventana ahora mismo — sin lógica de horario nueva. Apps protegidas
  (`ProtectedPackages`) siguen exentas del modo escolar, igual que de la política por defecto.

## Web

- `DeviceRulesPanel` gana un segundo interruptor (activar/desactivar + franja horaria + días),
  junto al de la política por defecto — misma UI de horario ya usada para `SCHEDULE`.

## Verificación

Backend: **138 pruebas pasan** (7 nuevas en `test_school_mode_integration.py`), verde dos veces
seguidas en contenedor limpio, `ruff check` limpio. Ciclo upgrade → downgrade → upgrade verificado;
`psql` confirma que activar sin ventana falla y que `SCHOOL_MODE` se acepta como motivo de bloqueo.

Android: `./gradlew test assembleDebug assembleRelease` — **34 pruebas** en `RuleEvaluatorTest` (27
previas + 7 nuevas: bloquea dentro de la ventana, no bloquea fuera, no hace nada si está
desactivado, una regla `ALLOW`/de categoría sigue aplicando durante el horario escolar, respeta la
máscara de días, y un dispositivo ya en modo lista blanca sigue reportando `DEFAULT_POLICY` fuera
del horario escolar). Ambos APK empaquetan.

Web: `npm run lint` (`--max-warnings=0`) y `npm run build` en verde.

Pendiente, mismo límite de siempre: verificación end-to-end con login real de Google.

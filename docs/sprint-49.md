# Sprint 49 — Supervisado: pantalla «App bloqueada» en sus 7 variantes

Décimo sprint del rediseño Android (`docs/android-redesign/sprints/S49-supervisado-app-bloqueada.md`). Mockup 16.
Decisión aplicada: D-11 (a): motivo, app (icono y nombre locales), categoría real y frase de reinicio solo donde es
verdad; sin franja horaria concreta. Es la pantalla que el menor ve a diario: el tono y la verdad importan más que
en cualquier otra.

## Qué se hizo

- **Claude — datos y estructura** (`a926fda`, `4facdcb`):
  - `BlockPresentation`: una función pura de los 7 motivos reales (`BlockReason`) a distintivo, color, mensaje y pista,
    probada con `BlockReason.entries`, de modo que un motivo nuevo sin mapear rompe el test. Los mensajes son los que la
    pantalla ya usaba.
  - **Reinicios verificados en el motor** (`AppInventoryCollector`): el contador diario empieza a la medianoche local y
    el semanal el lunes. Por eso solo el límite diario promete «se reinicia mañana» y el semanal «el lunes».
  - La **categoría real** de la app llega desde `RuleEnforcementService` hasta el overlay y la actividad
    (`EXTRA_CATEGORY_LABEL`). Qué se bloquea, cuándo y por qué no cambia.
  - El overlay y la actividad dibujan con `NetProtectTheme` (antes eran oscuros y sin tema).
  - La letra pequeña ya no nombra la forma de evadir el bloqueo.
  - `BlockScreenContent` se mudó a `feature/supervised/block/` para delegar un solo archivo sin tocar `core/rules`.
- **DeepSeek** (`docs/delegated/done/sprint-49-app-bloqueada.md`, `ff8da4a`): el diseño del mockup 16, 8 tests de UI
  y 9 estados en la galería.
- **Claude — revisión:** sin correcciones necesarias. Todo coincide con el mockup y con los textos del encargo.

## Cambios visibles para el menor

- Pantalla clara y con la marca: candado con un distintivo del color del motivo, «APP BLOQUEADA», el nombre y el
  icono real de la app, su categoría real si tiene una asignada, una tarjeta de color con la explicación y «Ir al
  inicio».
- Siete variantes: bloqueo directo, límite diario, límite semanal, horario, categoría, horario escolar y «solo apps
  aprobadas».

## Decisiones (aprobadas por el dueño)

- **Pistas por motivo, solo verdaderas** (texto en `BlockPresentation`). La de categoría dice «la limita», no «la
  bloquea», porque una regla de categoría puede ser un límite o un horario.
- **Letra pequeña:** «Este bloqueo cubre la pantalla; la app puede seguir abierta en segundo plano.». Es honesta sobre
  el límite del mecanismo sin explicar cómo saltarlo.
- **Icono real de la app** leído localmente, con monograma si no está.

## Diferencias con el mockup aceptadas

| Mockup 16 | Qué se hizo | Por qué |
|---|---|---|
| Horas concretas en «horario» y «horario escolar» | Sin horas | D-11 (b) no se eligió |
| Categorías inventadas («Entretenimiento», «Navegador») | La categoría real o ninguna | Son las del enum |
| Textos propios por variante | Los textos del motor + pistas verificadas | Evitar promesas falsas |

## Pendiente

- Verificar en el Galaxy S25 FE (la ventana superpuesta se comprobó en el emulador).
- Nota menor, sin tocar: el mensaje de «solo apps aprobadas» conserva las tildes antiguas («sólo», «ésta»); es el texto
  original de la pantalla y no se cambió.

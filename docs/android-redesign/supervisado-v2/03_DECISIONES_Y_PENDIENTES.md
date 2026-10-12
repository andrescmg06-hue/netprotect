# 03 · Decisiones tomadas y preguntas por validar

## Decisiones tomadas (por el dueño)

1. **Solo tema claro.** No se diseña ni implementa tema oscuro.
2. **Cerrar sesión pide confirmación** con un diálogo corto ("¿Cerrar sesión?"). Cerrar sesión **no** desvincula.
3. **Solo el tutor desvincula el dispositivo.** El menor nunca ve un botón "Desvincular". Si el tutor desvincula, la app muestra "Este dispositivo ya no está vinculado" con "Vincular de nuevo" y "Cerrar sesión".
4. **Privacidad y "Más ajustes" se alcanzan desde Inicio.**
5. **Una sola ruta sin barra inferior:** Inicio es el centro; Permisos y Estado son pantallas de detalle con flecha atrás y "Volver al inicio".
6. **Verdad antes que decoración:** la ubicación llega cada ~15 min y jamás se rotula "en tiempo real"; nada de datos inventados.
7. **Variante A es la línea base;** la Variante B (pestañas) solo se construye si la prueba con usuarios lo indica.

## Compatibilidad con el kit anterior (S40–S51)

- Las 14 decisiones de `docs/android-redesign/DECISIONES.md` siguen vigentes.
- Donde este rediseño contradiga una captura de `docs/android-redesign/mockups/` (12–17), **mandan estas láminas**: son más recientes y corrigen textos falsos de aquellas (p. ej. "cerrar sesión desvincula").
- Lo ya construido en S48 (vinculación y permisos) y S49 (bloqueo en 7 variantes) **no se tira**: se reestiliza y se extiende.

## Preguntas técnicas por validar

Mientras no haya respuesta, las pantallas usan un estado neutral o un texto cauto. **Responderlas leyendo el código y, si hace falta, el backend, antes de implementar la pantalla afectada.** Marca cada una como resuelta aquí, con la evidencia (archivo y línea).

| # | Pregunta | Qué asumen hoy las láminas | Afecta a |
|---|---|---|---|
| 1 | ¿Qué errores devuelve la API para distinguir sin Internet, servicio caído, sesión caducada y error desconocido? | Acceso muestra cuatro mensajes separados | 01 |
| 2 | ¿La sesión de Google se restaura siempre en silencio? | "Restaurando tu sesión…" breve; si falla, cae al login | 01 |
| 3 | ¿El backend devuelve la expiración del código de vinculación? | No se muestra cuenta regresiva | 02 |
| 4 | ¿Cuál es la cadencia real del reporte del dispositivo? | "Cada minuto mientras la app esté abierta" (`HEARTBEAT_INTERVAL_MS = 60_000L`, visto el 28/09) | 03, 05 |
| 5 | ¿La ubicación se envía cada ~15 min en segundo plano, solo con la app abierta u otro esquema? | "Cada unos 15 minutos", nunca "en tiempo real" | 05, 10 |
| 6 | ¿Cómo se comprueba cada permiso y a qué ajuste exacto se navega? | Detalle abre Ajustes y comprueba al volver | 04, 11 |
| 7 | ¿"Protección contra desinstalación" solo avisa al tutor o hace algo más? | El texto dice que avisa; no afirma que impida desinstalar | 04 |
| 8 | ¿Qué mecanismo muestra el bloqueo y qué límites tiene según versión y fabricante? | "Cubre la pantalla de la app, pero no la cierra" | 07 |
| 9 | ¿Quién inicia la vista remota y cómo expira la solicitud? | Caduca en 2 min (documento fuente); el contador es de ejemplo | 06 |
| 10 | ¿Cuál es el flujo exacto del consentimiento y del diálogo de captura de Android? | Consentimiento propio y luego el aviso del sistema, sin imitarlo | 06 |
| 11 | ¿Qué eventos terminan una vista remota y cómo se refleja en ambas apps? | Se detiene con "Detener" o al caducar | 03, 05, 06 |
| 12 | ¿Se garantiza que no se graba ni guarda ninguna captura? | Consentimiento, Estado y Privacidad dicen "no graba". **Quitar o ajustar si no es cierto** | 05, 06, 10 |
| 13 | ¿Qué consecuencias tiene "Cambiar de modo", quién puede hacerlo y a quién se avisa? | Pantalla marcada "por validar"; puede no existir | 10 |
| 14 | ¿Qué diferencia hay entre cerrar sesión y desvincular, y qué queda en el teléfono? ¿Cómo detecta la app que el tutor desvinculó? | Cerrar sesión con confirmación; desvincular solo el tutor | 10 |
| 15 | ¿Qué texto va cuando no hay ninguna ubicación frente a cuando está desactualizada? | "Sin confirmar" cuando aún no hay dato | 05 |
| 16 | ¿Cuándo se puede mostrar "Todo funciona", "Falta un paso" o "Sin reporte reciente"? | Todo funciona = 4 permisos + comunicación reciente. Umbral sin definir (propuesta: 5 min) | 03 |

## Otras cosas por validar (de la lámina 13)

- Pasos de batería en un **Samsung real** (los nombres de los ajustes cambian por fabricante y por versión de One UI).
- Textos de la bienvenida (los validan el dueño y, si es posible, un tutor y un menor).
- El aviso de Android "Se desactivó un permiso": qué eventos dispara realmente la app (¿se detecta al volver al primer plano o con un servicio?).

## Fuera de alcance de este rediseño (no empezar sin pedirlo)

Tabletas/horizontal/plegables, tema oscuro, editor de reglas, VPN, y la Variante B.

# 04 · Plan de sprints propuesto (borrador para que Claude Code lo refine)

Es una **propuesta**, no una orden. Claude Code debe verificar cada sprint contra el código real y ajustarla; el resultado va en `PLAN_IMPLEMENTACION.md` (ver `CLAUDE_CODE_PROMPT.md`).

## Punto de partida (según `docs/android-redesign/CONTINUAR.md`, 01/10/2026)

- S40–S49 hechos (tutor completo, supervisado: Vincular, Vinculado, Permisos y Bloqueo).
- **S50** (Consentimiento, Servicios, reconexión realtime B-02) está **pendiente** y **S51** (endurecimiento y PR a `main`) también.
- La rama de integración es `android-redesign`; cada sprint vive en `sprint-NN-<slug>` con PR hacia esa rama. Nada se hace commit ni push sin OK del dueño.

## Cómo encaja este rediseño

Recomendación: **ejecutar S50 ya con las láminas v2** (Consentimiento de 3 pasos + Estado + notificaciones) para no construir dos veces esa pantalla, y **mover S51 al final** para que endurezca todo de una vez. Decídelo con el dueño en la primera sesión.

## Sprints propuestos (numeración continua tras S51; si se integra con S50, ajusta)

| Sprint | Contenido | Láminas | Riesgo | Dueño sugerido |
|---|---|---|---|---|
| S52 | **Sistema v2**: tokens de color/tipo/movimiento, `NpSeal`, `NpHeroPanel`, `NpRail`, botón primario con chip, botón de texto, "reducir movimiento", escala de fuente 200 %, marcos en la galería de depuración | 08, Guía | Medio | Claude (base) + DeepSeek (componentes) |
| S53 | **Acceso y Vincular** con los estados nuevos, errores separados, confirmación al cerrar sesión | 01, 02, 10 (cerrar sesión) | Bajo | DeepSeek, Claude revisa |
| S54 | **Inicio, Permisos y Detalle de permiso**; regla de "Todo funciona / Falta un paso / Sin reporte reciente" | 03, 04, 11 | Alto (no detener servicios, G-19) | Claude (shell) + DeepSeek (pantallas) |
| S55 | **Consentimiento (3 pasos), Estado, notificaciones y banner de vista remota**; B-02 reconexión realtime (absorbe S50) | 05, 06 | Muy alto → Opus | Claude |
| S56 | **Más ajustes, Privacidad, "Cambiar de modo" (si existe), dispositivo desvinculado por el tutor, restilo del Bloqueo** | 07, 10 | Medio (backend para el desvinculado) | Claude (backend/shell) + DeepSeek |
| S57 | **Casos reales**: bienvenida, batería por fabricante, permiso desactivado + aviso; pasada de 200 %, TalkBack, contraste y cierre (absorbe S51) | 13, 08 | Medio | Claude |
| Prueba | Prototipo clicable + plan de 5 personas; decide Variante A/B | 14, 15 | — | Dueño + Claude |

## Reglas que se mantienen (de `CONTINUAR.md`)

- Sin ViewModel ni Hilt (D-02): state holders con `mutableStateOf`, funciones puras testeadas en JVM.
- Nada que cambie con el tiempo dentro de `SupervisedState.Linked` (reiniciaría los servicios).
- Las pantallas no lanzan `Intent`s ni permisos: todo pasa por el shell.
- Tras tocar el shell: navegar 3 min y comprobar con `adb shell dumpsys activity services com.netprotect.app` que los `ServiceRecord` son los mismos y que el latido sigue cada minuto en el backend.
- Tokens siempre por `session.authorized { token -> … }`.
- Datos de un menor: nunca coordenadas en pantalla ni en logs.
- En tests de UI: `performScrollTo().performClick()`.
- Galería de depuración: frames del sprint al principio; ningún diálogo dentro de la galería.
- DeepSeek no ve imágenes: la revisión visual contra estas capturas la hace Claude.

## Definición de terminado (por pantalla)

1. La pantalla coincide con la captura en estructura, jerarquía y textos literales (no hace falta igualar el píxel).
2. Cada dato mostrado es real; lo que no existe se omite o usa el texto cauto de `03_DECISIONES_Y_PENDIENTES.md`.
3. Verificada en emulador y con captura propia; en teléfono real lo que requiera hardware (Samsung S25 FE) se declara pendiente si no se probó.
4. Probada con letra al 200 %, TalkBack y "quitar animaciones".
5. `./gradlew test assembleDebug lintDebug` en verde; evidencia en `docs/sprint-NN-evidence.md`.

# Rediseño v2 de la app Android · modo supervisado

Paquete de entrega: láminas de diseño, especificación por pantalla, decisiones, plan y prueba con usuarios, listo para que Claude Code arme el plan de implementación. Complementa (no reemplaza) el kit de `docs/android-redesign/` (sprints S40–S51).

**Para empezar:** abre Claude Code en la raíz del repo (`netprotect/`) y pega el contenido de [`CLAUDE_CODE_PROMPT.md`](CLAUDE_CODE_PROMPT.md).

## Contenido

| Archivo | Para qué |
|---|---|
| [`CLAUDE_CODE_PROMPT.md`](CLAUDE_CODE_PROMPT.md) | Prompt listo para pegar: pide leer todo, verificar contra el código y producir `PLAN_IMPLEMENTACION.md` **sin escribir código todavía** |
| [`01_DISENO.md`](01_DISENO.md) | Colores, tipografía, componentes, movimiento y criterios de accesibilidad |
| [`02_PANTALLAS.md`](02_PANTALLAS.md) | Cada pantalla: archivo de la app, reglas de datos, textos literales y captura |
| [`03_DECISIONES_Y_PENDIENTES.md`](03_DECISIONES_Y_PENDIENTES.md) | Decisiones del dueño y 16 preguntas técnicas por validar |
| [`04_PLAN_SPRINTS.md`](04_PLAN_SPRINTS.md) | Propuesta de sprints S52–S57 y cómo encaja con S50/S51 |
| [`05_PRUEBA_USUARIOS.md`](05_PRUEBA_USUARIOS.md) | Plan de prueba con 5 personas y regla de decisión A/B |
| `capturas/laminas/` | Las 16 láminas completas (PNG) |
| `capturas/pantallas/` | Cada pantalla por separado, a 2×, nombrada `NN-lámina-n-estado.png` |
| `fuentes/canvas/` | Fuentes editables de las láminas (`*.dc.html` + `canvas.json`) |
| `fuentes/generador/` | Scripts Python que generan las láminas (tokens y componentes compartidos en `lib.py`) |

## Las 16 láminas

| Nº | Lámina | Estados |
|---|---|---|
| 00 | Flujo de navegación | — |
| 01 | Cargando e inicio de sesión | 5 |
| 02 | Vincular este dispositivo | 3 |
| 03 | Inicio (dispositivo vinculado) | 4 |
| 04 | Permisos | 2 |
| 05 | Estado de NetProtect | 2 |
| 06 | Consentimiento y notificaciones permanentes | 3 |
| 07 | App bloqueada | 7 |
| 08 | Letra al 200 % | 3 |
| 09 | Guía para Compose y movimiento | — |
| 10 | Sesión, vínculo, modo y privacidad | 5 |
| 11 | Detalle de permiso | 3 |
| 12 | Preguntas técnicas por validar | — |
| 13 | Casos reales (bienvenida, batería, permiso desactivado) | 5 |
| 14 | Variante B: Inicio unificado (A/B) | 2 |
| 15 | Plan de prueba con usuarios | — |

## Advertencias

- Las capturas usan **Playfair Display** o una serif del sistema como aproximación; la fuente definitiva está pendiente.
- Los nombres, horas y cifras ("Andres Mosquera", "samsung SM-S731B", "Hace 12 min", contadores) son **ejemplos**. La app debe mostrar datos reales o un texto cauto.
- Lo marcado *por validar* (frases "no graba", Cambiar de modo, pasos de batería, umbral de "Sin reporte reciente") no se publica como verdad hasta comprobarlo.
- Las láminas viven también en el canvas de Claude (privado del dueño); estas capturas son su copia versionable.

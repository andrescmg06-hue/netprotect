# Prompt para Claude Code (pegar tal cual en la raíz del repo `netprotect/`)

````text
Vas a planificar la implementación del rediseño v2 del modo SUPERVISADO de la app Android de NetProtect. Esta primera sesión es SOLO de planificación: no escribas código de la app, no hagas commits ni push, no cambies ramas sin mi OK.

## Qué leer, en este orden
1. CLAUDE.md, AGENTS.md y las reglas por área que se cargan solas (`.claude/rules/`).
2. docs/android-redesign/CONTINUAR.md, SPRINT_PLAN.md, DECISIONES.md y sprints/S48, S49, S50 y S51 (estado y proceso ya establecidos: no los rompas).
3. docs/android-redesign/supervisado-v2/README.md y luego 01 a 05, en orden.
4. Mira las capturas: capturas/laminas/*.png (las 16) y las de capturas/pantallas/ que necesites. Los textos literales de cada pantalla están en 02_PANTALLAS.md; las capturas mandan para estructura y jerarquía.

## Qué quiero que hagas
1. Explora el código real de mobile/ (feature/supervised/*, ui/theme, ui/components, core/permissions, core/screenshare, SupervisedShell.kt) con el agente Explore y confírmame, con rutas y líneas, qué existe ya y qué falta para cada pantalla. No supongas: comprueba con grep antes de citar un componente, color o icono.
2. Intenta responder las 16 preguntas técnicas de 03_DECISIONES_Y_PENDIENTES.md leyendo el código (y el backend si hace falta). Para cada una dime: respuesta con evidencia (archivo:línea) o "no se puede saber sin una persona/dispositivo". Corrige las láminas en tu plan donde la realidad las contradiga, sobre todo las frases "no graba" y el desvinculado por el tutor.
3. Revisa la propuesta de 04_PLAN_SPRINTS.md: ajusta el orden y el alcance, y dime cómo encaja con S50 y S51 pendientes (mi recomendación: hacer S50 ya con las láminas v2 y mover S51 al final; discútelo conmigo).
4. Escribe docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md con: mapa pantalla → archivo → cambio, huecos de datos/backend, riesgos (en especial: no detener servicios al partir SupervisedShell, G-19), qué delegas a DeepSeek y qué haces tú, criterios de terminado por sprint y qué pruebas necesitan una persona o el Samsung real.
5. Termina con una lista corta de las decisiones que me tocan a mí, con tu recomendación en cada una.

## Reglas que no se negocian
- Nada se da por terminado sin evidencia real de ejecución; lo que no pueda probarse se declara pendiente.
- Cero datos inventados: los nombres, horas y cifras de las capturas son ejemplos. La ubicación llega cada ~15 min y nunca se llama "en tiempo real".
- Solo tema claro. Cerrar sesión pide confirmación y no desvincula; solo el tutor desvincula.
- Accesibilidad: objetivos ≥ 48 dp, AA, letra al 200 %, TalkBack, "quitar animaciones".
- Cambios mínimos y reutilizar lo existente (S42, S48, S49); sin refactors ni dependencias que no estén justificados.
- Pídeme OK antes de crear ramas, commits o implementar.

Cuando termines, resúmeme en pocas líneas el plan y las decisiones pendientes.
````

## Si prefieres un prompt más corto

````text
Lee docs/android-redesign/supervisado-v2/README.md y sigue las instrucciones de CLAUDE_CODE_PROMPT.md de esa carpeta. Solo planificación: sin código ni commits hasta que yo apruebe PLAN_IMPLEMENTACION.md.
````

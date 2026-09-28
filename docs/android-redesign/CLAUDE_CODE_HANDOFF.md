# CLAUDE_CODE_HANDOFF — cómo se trabaja el rediseño Android

Documento de proceso. Claude Code lo lee al empezar cada sprint; DeepSeek **no** necesita leerlo
(su contrato es `AGENTS.md` + el archivo de delegación).

## 1. Qué leer y qué no (contexto mínimo)

Notación: `{pkg}` = `mobile/app/src/main/java/com/netprotect/app`. En los encargos para DeepSeek
se escriben siempre las rutas completas.

Por sprint, Claude Code carga **solo**: este documento, el archivo del sprint
(`docs/android-redesign/sprints/SNN-*.md`), las entradas de `DECISIONES.md` que ese sprint cita, los
mockups de sus pantallas y `mockups/README.md`. `CLAUDE.md` y la regla del área se cargan solas.

No leer enteros: `PROJECT_AUDIT.md`, `ARCHITECTURE_GAPS.md`, `docs/historial-estado.md`,
`docs/sprint-NN.md` antiguos. Si hace falta algo de ahí, delegar la búsqueda en `Explore` pidiendo
`ruta:línea`. Las verificaciones pesadas (Gradle, Docker) van por `verifier`, no por el hilo principal.
Entre la fase A y la fase C de un sprint, sesión nueva o `/compact`.

## 2. Roles

| Quién | Hace | No hace |
|---|---|---|
| **Claude Code** | Explora, planifica, escribe el encargo, implementa lo sensible (auth, tokens, realtime, servicios, reglas de bloqueo, backend, navegación base), revisa y corrige lo de DeepSeek, verifica, documenta, cierra | Commit/push sin permiso; decisiones de `DECISIONES.md` por su cuenta |
| **DeepSeek V4 Pro (OpenCode)** | Implementa el encargo: pantallas Compose, componentes, formateadores, recursos, clientes HTTP que copian un patrón existente, tests JVM | Tocar archivos fuera de "Archivos permitidos"; backend; `core/auth`, `core/network/HttpJsonClient.kt`, `core/rules`, `core/screenshare`, `core/location`, `core/sync`, `core/tamper`; dependencias nuevas; commits |
| **Tú** | Resuelves decisiones, abres OpenCode, apruebas commits, pruebas con personas reales (login Google, diálogo de captura) | — |

Los dos agentes **nunca trabajan a la vez**: la misma carpeta, sin worktrees. `delegate-lock.json`
queda vacío (solo tiene sentido con trabajo simultáneo).

## 3. Ciclo de un sprint

```
A. Claude (sesión 1)  explorar → explicar → planear → [implementar lo suyo] → escribir encargo
      commit 1: docs(sprint-NN): plan y encargo        (+ commit aparte si Claude implementó algo)
B. DeepSeek (OpenCode) "Ejecuta docs/delegated/pending/sprint-NN-<slug>.md"
      mueve el encargo a active/ → implementa → verifica → escribe su informe → lo mueve a done/
C. Claude (sesión 2)  commit 2 con lo de DeepSeek TAL CUAL → revisar → verifier → security-reviewer
      → corregir → capturas vs mockups → evidencia → /cerrar-sprint NN
      commit 3: fix(sprint-NN): correcciones de revisión     commit 4: docs(sprint-NN): evidencia y cierre
```

El commit 2 separa lo que escribió DeepSeek de lo que corrigió Claude: así se ve en el historial qué
tan bien trabajó DeepSeek. Si no compila, el mensaje lo dice (`… — sin revisar, no compila`).

## 4. Protocolo de Claude Code (10 fases)

1. **Explorar**: rama y árbol limpios (`GIT_OPTIONAL_LOCKS=0 git status`), leer los archivos que el
   sprint lista, confirmar que existen y que el código dice lo que el plan supone.
2. **Explicar**: en 10–20 líneas, qué funciona hoy, qué se va a tocar, qué puede romperse, y
   cualquier diferencia entre el plan y el repo.
3. **Planear**: pasos concretos, qué hace Claude y qué va al encargo. Si aparece algo marcado
   `[NECESITA DECISIÓN]` sin resolver, **detenerse y preguntar**.
4. **Alcance**: no añadir nada que el sprint no pida; la deuda que aparezca se anota en
   `docs/tasks.md` con P0–P3, no se arregla de paso (salvo P0).
5. **Implementar** (solo la parte de Claude).
6. **Tests**: los del sprint, vía `verifier`.
7. **Seguridad**: `security-reviewer` cuando el sprint lo indique.
8. **Arquitectura**: diff contra `ARCHITECTURE_GAPS.md` §3 y la regla `android.md`; sin colores
   literales, sin datos inventados, un archivo por pantalla.
9. **Corregir** lo que salga de 6–8.
10. **Entregar** el informe del §9.

## 5. El encargo para DeepSeek

Se escribe en `docs/delegated/pending/sprint-NN-<slug>.md` con **el mismo formato que
`docs/delegated/done/sprint-39-login-reemplazo.md`** (ya probado):

1. Objetivo (resultado visible, verificable).
2. Decisiones ya tomadas (tabla; "no las reabras").
3. Lo que NO debes tocar (lista explícita de rutas) + reglas del proyecto que aplican.
4. Punto de partida: archivos a leer, con rutas exactas y qué contiene cada uno; mockups a replicar.
5. Archivos permitidos (crear/modificar) — **lista cerrada**.
6. Pasos numerados, pequeños, cada uno con su comprobación (`./gradlew compileDebugKotlin` tras
   cada archivo nuevo).
7. Verificación: comandos exactos y qué salida se espera; qué capturas tomar.
8. Informe final (sección `## Informe de DeepSeek` al final del mismo archivo): archivos, salidas
   reales, pendientes, decisiones que tuvo que tomar, preguntas `[PREGUNTA PARA CLAUDE]`.

Reglas de redacción: nombres de archivo y firmas concretas; los textos en español que deben aparecer,
literales; nunca "haz algo parecido a…". Lo que DeepSeek no deba decidir, se decide en el encargo.
Tamaño: un encargo que DeepSeek pueda terminar en una sesión (≈ 1–4 pantallas). Si es más, dos
encargos en secuencia.

## 6. Git

- Integración (si D-06 = a): `main` → `android-redesign`; cada sprint en `sprint-NN-<slug>` desde
  `android-redesign`, PR de vuelta con CI verde. S51 hace el PR `android-redesign` → `main`.
- Commits del §3. Nunca en el mismo commit: backend y Android; dependencia nueva y pantallas;
  refactor y funcionalidad; código de seguridad y UI.
- Mensajes: tipo(sprint-NN): qué — y en el cuerpo, el porqué. Firma según la configuración del repo.
- Todo git con `GIT_OPTIONAL_LOCKS=0`. Sin `commit`/`push` sin que el dueño lo apruebe.

## 7. Verificación en Android

| Qué | Cómo |
|---|---|
| Compila cada archivo nuevo | `cd mobile && ./gradlew compileDebugKotlin` |
| Tests JVM + APK | `./gradlew test assembleDebug` |
| APIs por encima de `minSdk 26` | `./gradlew lintDebug` (no está en CI) |
| Tests de UI (si D-13 = a) | `./gradlew connectedDebugAndroidTest` con emulador |
| Capturas | `adb exec-out screencap -p > "%TEMP%\np-sprintNN\NN-pantalla-estado.png"` — **fuera del repo** |
| Comparar con el mockup | Claude abre la captura y el `.jpg` del mockup y lista diferencias de composición; los datos se comparan contra la API, no contra el mockup |
| Datos reales | Backend dev (`make dev`), teléfono físico como supervisado y emulador (`Pixel_8`, API 36) como tutor, o al revés |
| Estados | Por pantalla: cargando, vacío, error (backend apagado / modo avión) y con datos |

Lo que exige una persona (login real de Google, aceptar el diálogo de captura) se declara
**pendiente**, no se simula.

## 8. Definition of Done (común)

- [ ] Todo lo del alcance funciona en dispositivo o emulador, con evidencia real.
- [ ] `./gradlew test assembleDebug lintDebug` en verde (y backend `make test` si se tocó).
- [ ] Cero colores/tamaños literales fuera de `ui/theme`; componentes de `ui/components`.
- [ ] Estados cargando/vacío/error por lista; textos de retraso donde la mockup o el plan los piden.
- [ ] Ningún dato inventado (revisado contra `mockups/README.md`).
- [ ] Ninguna funcionalidad previa perdida (inventario de S40).
- [ ] `security-reviewer` sin hallazgos ALTA cuando aplica.
- [ ] `docs/sprint-NN.md`, `docs/sprint-NN-evidence.md`, `docs/progress.md`, `docs/tasks.md`,
      README (`## Alcance del Sprint NN`) actualizados; encargo en `docs/delegated/done/`.

## 9. Informe final de Claude al cerrar

Archivos modificados · funcionalidades implementadas · tests ejecutados (salida real) · tests
fallidos · decisiones tomadas · qué corrigió de DeepSeek (y patrón del error, para mejorar los
próximos encargos) · pendientes · siguiente sprint recomendado.

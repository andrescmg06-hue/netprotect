# Cómo continuar el rediseño Android (estado al 01/10/2026, tras el Sprint 50)

Para quien retome el trabajo. Complementa `CLAUDE_CODE_HANDOFF.md` (el proceso) con **dónde estamos**, **cómo preparar
el entorno** y **lo que ya aprendimos** haciendo nueve sprints.

## 1. Dónde estamos

| Sprint | Pantallas | Estado |
|---|---|---|
| S40–S47 | Base, sesión, diseño, login, todo el modo tutor (mockups 1–11) | ✅ Hechos, fusionados en `android-redesign` |
| S48 | Supervisado: Vincular, Vinculado, Permisos (12–14) + B-01 | ✅ Hecho (PR #10) |
| S49 | App bloqueada, 7 variantes (16) | ✅ Hecho (PR #11) |
| S50 | Consentimiento de vista remota y Servicios (15, 17) + reconexión realtime (B-02) | ✅ Hecho en local (rama `sprint-50-consentimiento`); **falta push + PR** y H-02 |
| **S51** | **Endurecimiento, accesibilidad, regresión, `lintDebug` en CI y PR `android-redesign` → `main`** | ⏭️ **Siguiente.** Plan: `sprints/S51-endurecimiento-y-cierre.md`. Conviene Opus |

- **Decisiones:** las 14 de `DECISIONES.md` están **resueltas**; ningún sprint restante necesita una decisión nueva
  para empezar.
- **Historia:** cada sprint tiene su `docs/sprint-NN.md` (qué y por qué) y su `docs/sprint-NN-evidence.md`
  (comandos y salidas reales).
- **Estado global:** `docs/progress.md`; backlog y bugs: `docs/tasks.md`.

## 2. Ramas

- Integración: **`android-redesign`**. Aún no se ha fusionado en `main`; eso lo hace el S51.
- Cada sprint: `sprint-NN-<slug>`, creado desde `android-redesign` y con PR de vuelta a `android-redesign`. Se
  fusiona con `gh pr merge N --merge` **solo con los 8 jobs de CI en verde**.
- Antes de empezar un sprint:

  ```bash
  GIT_OPTIONAL_LOCKS=0 git checkout android-redesign && git pull && git checkout -b sprint-49-app-bloqueada
  ```

## 3. Entorno

| Pieza | Cómo |
|---|---|
| Backend de desarrollo | `make dev` (o `docker compose up -d`). API en `localhost:8000`; el emulador la ve como `10.0.2.2:8000`. Tras cambiar el backend: `docker compose up -d --build backend` (no monta el código) |
| Tests del backend | `make test` (siempre en contenedor). `ruff` no está instalado en Windows: `docker compose -f compose.test.yaml run --rm --no-deps --entrypoint "" backend sh -c "ruff check --no-cache app tests alembic"` |
| Android | `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest` (≈ 4–6 min con emulador) |
| Emulador | AVD `Pixel_8` (Android 16): `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Pixel_8 -no-snapshot-save -no-boot-anim -gpu swiftshader_indirect -memory 2048`. Sin `swiftshader` las capturas salen negras; con poca RAM libre Windows lo cierra |
| `adb` | No está en el PATH: `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"` |
| Panel web | `cd frontend && npm run dev` → `localhost:3000`. Si `npm ci` falla con `EPERM` es porque `next dev` bloquea un binario; usa `npm install` o cierra el servidor |
| Docker desde Git Bash | `export MSYS_NO_PATHCONV=1` antes de cualquier `docker` con rutas de contenedor |

**Cuentas de prueba** (las del dueño; el inicio de sesión con Google lo hace una persona):
- **Tutor:** `andrescmg05@gmail.com`. Tiene vinculados el Samsung (`samsung SM-S731B`) y el emulador
  (`Google sdk_gphone64_x86_64`).
- **Supervisado:** `andrescmg06@gmail.com`. Es la cuenta del emulador en modo supervisado; el emulador quedó
  así al cerrar el S48.

Para cambiar de cuenta: en la app, **Más → Cerrar sesión** (tutor) o **Cerrar sesión** (supervisado), y entrar con la
otra.

## 4. El ciclo (resumen; el detalle está en `CLAUDE_CODE_HANDOFF.md`)

1. **Claude Code** — pega el **PROMPT A** del archivo del sprint.
   - Claude explora, te explica y pide OK al plan.
   - Implementa lo sensible y hace commits.
   - Escribe el encargo en `docs/delegated/pending/`, hace commit, empuja y te da el prompt de OpenCode.
2. **OpenCode (DeepSeek V4 Pro)** — `Ejecuta el encargo docs/delegated/pending/sprint-NN-….md siguiendo AGENTS.md.`
3. **Claude Code** — pega el informe de DeepSeek.
   - Claude guarda lo suyo en un commit **tal cual** y después revisa: diff, grep de patrones prohibidos, galería y
     pantalla real contra los mockups, tests y prueba real.
   - Corrige en commits aparte.
   - Documenta (`sprint-NN.md`, `-evidence.md`, `progress.md`, `tasks.md`, README), abre el PR y lo fusiona con el CI
     en verde.

Nada se hace commit ni push sin el OK del dueño.

## 5. Lo que aprendimos (aplicarlo en cada encargo nuevo)

**En los encargos para DeepSeek:**
- Da las **firmas finales** de las pantallas y deja un cuerpo provisional funcional. DeepSeek solo reemplaza cuerpos;
  la lógica, la carga y los textos calculados ya llegan hechos. Funciona muy bien.
- **Textos literales** entre comillas, y una tabla de **«lo que el mockup dice mal»**. Los mockups inventan datos y
  frases falsas; `mockups/README.md` las lista.
- En los tests de UI, **siempre** `performScrollTo().performClick()`: el emulador de CI (API 30) tiene la pantalla
  más pequeña, y un toque fuera de vista falla solo allí.
- **Galería de depuración:**
  - los frames del sprint van **al principio**, porque deslizar la galería con `adb` salta de forma errática;
  - **ningún diálogo** en la galería: un `Dialog` modal la tapa entera.
- Antes de citar un icono, color o parámetro, comprueba que existe (`grep` en `ui/icons/NpIcons.kt`,
  `ui/theme/Color.kt` y la firma del componente). DeepSeek no puede inventar lo que falta y se queda bloqueado.
- DeepSeek **no ve imágenes**: la revisión visual siempre la hace Claude.
- Le pasa a menudo que los nombres de sus capturas no coinciden con el contenido. Revísalas abriéndolas.

**Probar de verdad un bloqueo (S49):** para ver cada pantalla de bloqueo en el emulador no hace falta crear reglas a
mano en el panel web. `docs/sprint-49-evidence.md` §3 describe la técnica: insertar una regla de prueba en la base de
desarrollo (`app_rules`, `category_rules` + `app_category_assignments`, o los campos de política y horario escolar de
`devices`), esperar ≈ 70 s a que el servicio la descargue (sondea cada minuto), abrir la app y esperar a que aparezca
una fila nueva en `app_rule_events`. Para los límites diario y semanal hay que abrir antes la app ≈ 80 s para tener
uso registrado (el mínimo del límite es 1 minuto). Se borra todo al final y se comprueba con un `count`.

**En el código:**
- **Modo supervisado:** `SupervisedShell.kt` tiene los servicios.
  - No metas nada que cambie con el tiempo dentro de `SupervisedState.Linked`: reiniciaría todos los servicios.
  - Las pantallas no lanzan `Intent`s ni permisos; todo pasa por el shell.
  - La prueba obligatoria tras tocarlo: navegar 3 minutos y comprobar con `adb shell dumpsys activity services
    com.netprotect.app` que los `ServiceRecord` son los mismos, y en los logs del backend que el latido sigue cada
    minuto.
- **Sin ViewModel ni Hilt** (D-02): state holders con `mutableStateOf`, funciones puras testeadas en JVM.
- **Datos de un menor:**
  - nunca coordenadas en pantalla ni en logs;
  - «Dentro de «zona»» solo con certeza;
  - la auditoría no guarda datos de ubicación.
- **Ubicación en el emulador:** no reporta, porque la app usa solo `NETWORK_PROVIDER` y el emulador solo tiene GPS.
  Los reportes se verifican en el teléfono real.
- **Tokens:** todo va por `session.authorized { token -> … }` (renovación única, S41).
- **CI:** `npm audit --audit-level=high` rompe **todos** los PR cuando se publica un aviso nuevo en una dependencia,
  aunque sea de desarrollo. Arreglo típico: `npm audit fix` en `frontend/`, que solo toca el lockfile (S47).

## 6. Pendiente que necesita una persona

- **Samsung Galaxy S25 FE:** verificar el aspecto de las pantallas rediseñadas, que lleguen reportes de ubicación y la
  pantalla de Ubicación del tutor con datos reales (S45 y S48).
- **Vinculación desde cero** con un código generado por una **segunda** cuenta de tutor (S44 y S48).
- **H-01 / H-02** (login real de Google en el teléfono y otras comprobaciones humanas): ver `docs/tasks.md`.

## 7. Bugs conocidos (en `docs/tasks.md`)

- **B-02:** el canal realtime de Android no se reconecta si cae. Lo resuelve el S50.
- **B-03:** una misma cuenta puede ser tutor y supervisado sin aviso claro (backend).
- **B-05:** el panel web consulta alertas cada minuto con un token caducado y recibe 401. Se ve en los logs del
  backend si queda una pestaña abierta.
- **B-06:** con letra grande, algunos botones se aplastan. Lo revisa el S51.

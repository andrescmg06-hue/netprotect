# NetProtect — instrucciones para OpenCode (DeepSeek)

> Este archivo lo lee OpenCode. Claude Code usa `CLAUDE.md`. Instalado en la raíz en el Sprint 40.

NetProtect es una plataforma de control parental (proyecto académico de Seguridad Informática; maneja
datos de menores): una app Android (Kotlin/Compose) con modo Tutor y modo Supervisado, un panel web
(Next.js) y un backend FastAPI + PostgreSQL + Redis.

## Tu papel

Eres el **implementador**. Claude Code planifica, revisa y corrige; tú ejecutas **un encargo** escrito
en `docs/delegated/pending/`. Nadie más trabaja en la carpeta mientras tú trabajas.

1. Lee el encargo **entero** antes de tocar nada. Muévelo a `docs/delegated/active/`.
2. Modifica **solo** los archivos de su sección "Archivos permitidos". Si necesitas otro, no lo toques:
   escríbelo como `[PREGUNTA PARA CLAUDE]` en tu informe y sigue con lo demás.
3. Las "Decisiones ya tomadas" no se reabren.
4. Al terminar, escribe `## Informe de DeepSeek` al final del encargo y muévelo a `docs/delegated/done/`.

## Nunca

- `git commit`, `git push`, `git reset`, `git checkout` de archivos, borrar ramas.
- Leer o editar `.env*`, `secrets/`, `mobile/local.properties`, `mobile/app/google-services.json`.
- Editar `.claude/`, `CLAUDE.md`, `AGENTS.md`, `opencode.json`, `frontend/AGENTS.md`, migraciones de
  `backend/alembic/versions/`.
- Tocar `backend/` o, en Android, `core/auth/`, `core/network/HttpJsonClient.kt`, `core/rules/`,
  `core/screenshare/`, `core/location/`, `core/sync/`, `core/tamper/` salvo que el encargo lo liste.
- Añadir dependencias de Gradle/npm/pip salvo que el encargo lo diga.
- Inventar salidas de comandos. Lo que no pudiste probar, lo declaras pendiente.

## Reglas de Android (resumen; el detalle está en `.claude/rules/android.md`)

- Sin Hilt, DI ni ViewModel. Estado con `remember`/`rememberSaveable` y *state holders* simples.
- Colores, tipografía, formas: **solo** desde `ui/theme` (`NetProtectTheme`). Nada de `Color(0xFF…)`
  en pantallas. Componentes desde `ui/components`.
- Textos en español (es-CO), en el código, tal como los da el encargo.
- Objetivos táctiles ≥ 48 dp; cada icono con `contentDescription` (o `null` si es decorativo);
  el nivel de una alerta nunca solo con color: icono + texto + color.
- Cada `.kt` nuevo: `cd mobile && ./gradlew compileDebugKotlin` antes de seguir.
- Al final: `./gradlew test assembleDebug lintDebug` y pega la salida real en el informe.
- Git siempre con `GIT_OPTIONAL_LOCKS=0` (el repo está en OneDrive).

## Verdad sobre decoración

Los mockups de `docs/android-redesign/mockups/` marcan **cómo se ve**, no **qué datos hay**. Los datos
salen de la API o del dispositivo. Antes de dibujar una pantalla, lee su apartado en
`docs/android-redesign/mockups/README.md`: ahí está lo que el mockup muestra y **no** existe
(p. ej. nombre de lugar en Ubicación, "porcentaje de tiempo usado" en Estadísticas). Si dudas si un
dato existe, no lo inventes: pregunta en el informe.

# D-05 — `make android-check` debe usar `./gradlew`

## 1. Objetivo
Que el target `android-check` del `Makefile` use el wrapper del repositorio (`./gradlew`), igual que CI,
en lugar de un `gradle` global. Cierra la deuda D-05 de `docs/tasks.md`.

## 2. Lo que ya está hecho
Nada que repetir. Hoy el target es:
```
android-check:
	cd mobile && gradle test assembleDebug
```
CI (`.github/workflows/ci.yml`, job Android) ejecuta `./gradlew test assembleDebug`.

## 3. Decisiones ya tomadas (no se reabren)
- Se usa `./gradlew` con los mismos argumentos que hoy (`test assembleDebug`). No se añaden tareas (`lintDebug` es el S51, no esto).
- Ver Engram `decision/android-check-gradlew`.

## 4. Memoria a consultar
- `mem_search` de `decision/android-check-gradlew` (proyecto `netprotect`).

## 5. Archivos permitidos / prohibidos
- **Permitido: solo `Makefile`.**
- Prohibido todo lo demás: `.github/`, `docs/` (Claude actualiza `docs/tasks.md`), `mobile/`, `CLAUDE.md`, `AGENTS.md`, `opencode.json`.

## 6. Especificación
- Cambia únicamente la línea del target `android-check`: `cd mobile && gradle test assembleDebug` → `cd mobile && ./gradlew test assembleDebug`.
- La línea sigue empezando con **un carácter TAB** (es una receta de make), no con espacios. Verifícalo (p. ej. `cat -A Makefile | sed -n '30,34p'` debe mostrar `^I`).
- No cambies ningún otro target ni la línea `.PHONY`. No añadas `chmod`, variables ni objetivos nuevos.
- `make` **no está instalado** en esta máquina: no intentes instalarlo ni uses `make -n`.

## 7. Tests
No aplica (cambio de una línea de configuración). La comprobación es la sección 8.

## 8. Verificación (pega la salida real de cada comando)
1. `git diff -- Makefile` → debe mostrar exactamente 1 línea quitada y 1 añadida.
2. `cat -A Makefile | sed -n '30,34p'` → la línea del target empieza con `^I`.
3. `cd mobile && ./gradlew test assembleDebug` → debe terminar en `BUILD SUCCESSFUL` (tarda unos minutos). Si falla por una razón ajena al Makefile, pega el error y decláralo; no lo arregles.

## 9. Informe final
Añade `## Informe de DeepSeek` al final de este archivo y muévelo a `docs/delegated/done/`.
No hagas commit.

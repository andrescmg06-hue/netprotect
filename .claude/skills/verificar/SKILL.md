---
name: verificar
description: Matriz de verificación de NetProtect por área (backend, frontend, android, integración, infra) con los comandos exactos del Makefile y CI, y las trampas del entorno Windows/Docker. Usar al comprobar que un cambio funciona.
---

## Matriz — de lo rápido a lo lento

| Área cambiada | 1. Rápido | 2. Completo |
|---|---|---|
| `backend/` | `cd backend && ruff check app tests alembic` · `pytest -q -m "not integration"` | `make test` |
| `backend/alembic/` o `models/` | lo anterior + `docker compose -f compose.test.yaml build backend migrate` | `make test` |
| `frontend/` | `cd frontend && npm run lint` | `npm run build` (incluye typecheck) · E2E solo si se pide |
| `mobile/` | `cd mobile && ./gradlew compileDebugKotlin` | `./gradlew test assembleDebug lintDebug` |
| `infra/`, `compose*.yaml` | `docker compose -f <archivo> config -q` | `make test` · `sh scripts/verify_turn.sh` si tocó coturn |

## Trampas conocidas (cada una ocurrió de verdad)
1. Integración **siempre en contenedor**: desde Windows contra puertos publicados cada petición tarda ~1,4 s.
2. `compose.test.yaml` no persiste datos: **`run --rm migrate` antes de cada `up`**, siempre.
3. Nunca `up --build` con `migrate` en `depends_on` junto con `--abort-on-container-exit`: `migrate` termina por diseño y tumba el stack.
4. Si cambió `backend/`, reconstruir juntos: `docker compose -f compose.yaml build backend web migrate`.
5. `export MSYS_NO_PATHCONV=1` antes de `docker` con rutas de contenedor; confirmar con `docker inspect` qué se ejecutó.
6. `next build` con `EBUSY .next/standalone` → matar el `node .next/standalone/server.js` anterior.
7. Un `.kt` nuevo no está verificado hasta que `compileDebugKotlin` lo compiló. `lintDebug` es el único que detecta APIs por encima de `minSdk 26`.
8. Un test de WebSocket **colgado** suele ser una excepción silenciosa en el handler, no un fallo del test.
9. `compose.yaml` y `compose.test.yaml` usan el puerto 8000: detener `backend`/`web` de dev antes de levantar test.
10. Verificaciones contra servicios reales llevan un **control negativo** que debe fallar.

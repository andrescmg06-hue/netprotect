# NetProtect

Plataforma de control parental: **una** app Android (Kotlin/Compose) que funciona como Tutor o
Supervisado según decide el backend, un panel web (Next.js) para el tutor, y un backend
FastAPI + PostgreSQL + Redis como única fuente de verdad. Proyecto académico de Seguridad
Informática; maneja datos de menores.

## La regla que gobierna todo

**Nada se da por terminado sin evidencia real de que se ejecutó.** Sin mocks disfrazados de
pruebas, sin «debería funcionar». Lo que no se pudo probar (p. ej. un login real de Google o una
persona aceptando la captura de pantalla) se declara pendiente, explícitamente.

## Mapa

| Ruta | Qué es |
|---|---|
| `backend/app/` | API: `api/v1/endpoints/`, `api/deps.py` (autorización), `models/`, `schemas/`, `services/` |
| `backend/alembic/versions/` | Migraciones — las versionadas **no se editan** |
| `frontend/src/` | Panel web: `components/`, `components/ui/` (primitivas), `lib/apiClient.ts` |
| `mobile/app/src/main/java/com/netprotect/app/` | `core/` (reglas, sync, realtime, tamper) y `feature/` (tutor, supervisado) |
| `infra/`, `compose*.yaml` | Caddy, coturn, monitoreo; dev / test / prod |
| `docs/` | `progress.md` (estado), `tasks.md` (backlog), `sprint-NN.md` + `-evidence.md` (historia) |
| `docs/android/capability-matrix.md` | Qué es viable en Android. **Consultar antes de proponer una función de control** |
| `docs/security-baseline.md` | Controles y matriz de permisos |

## Comandos

- Integración (verificación real): `make test` — siempre en contenedor, nunca contra puertos publicados desde Windows
- Backend rápido: `cd backend && pytest -q -m "not integration"` · lint: `ruff check app tests alembic`
- Frontend: `cd frontend && npm run lint && npm run build`
- Android: `cd mobile && ./gradlew test assembleDebug lintDebug`
- Migración: `make revision m="descripcion"`

## Cómo trabajar

1. **Investigar antes de modificar.** Leer el código implicado y la regla del área (se carga sola).
   Para búsquedas amplias o «¿por qué se decidió X?», delegar a `Explore` pidiendo rutas y líneas,
   no volcados de archivos.
2. **Cambios mínimos.** Sin refactors, dependencias ni «mejoras» que no se pidieron. No cambiar
   rutas de API ni lógica de negocio en tareas de UI.
3. **Verificar con `verifier`**, no corriendo Gradle/Docker/`next build` en el hilo principal.
4. **Revisar seguridad con `security-reviewer`** si se tocó auth, pairing, realtime, location,
   audit, `deps.py`, infra o secretos.
5. **Actualizar `docs/progress.md`** al terminar algo significativo. Cierre de sprint: `/cerrar-sprint NN`.

## Reglas globales

- Secretos: nunca leer `.env` ni `secrets/`. Cada secreto con su propia variable, documentada en los tres `.env.*.example`.
- Git: todos los comandos con `GIT_OPTIONAL_LOCKS=0` (el repo está en OneDrive). No hacer commit ni push sin que se pida.
  Mensajes de commit explicando el *por qué*.
- Skills y agentes `impeccable*`, `animate`, etc. son de terceros: no editarlos (se reinstalan).
- `frontend/AGENTS.md` lo regenera `next dev`: no editarlo.
- Windows: `export MSYS_NO_PATHCONV=1` antes de cualquier `docker` con rutas de contenedor.

## Dónde está el resto

- Convenciones y lecciones por área → `.claude/rules/` (cargan solas según la ruta)
- Procedimientos → skills `verificar`, `cerrar-sprint`
- Diseño visual → `DESIGN.md`, `PRODUCT.md`, skill `impeccable`
- Historia completa → `docs/sprint-NN.md`; recuento anterior → `docs/historial-estado.md`
- Planes → `docs/planning/`; manuales → `docs/manuals/`

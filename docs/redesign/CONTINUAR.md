# Cómo continuar el rediseño del panel web (estado al 04/10/2026)

Para quien retome el trabajo desde una PC distinta. Complementa `PLAN_SPRINTS.md` (el plan) con **dónde estamos**,
**cómo preparar el entorno** y **qué hay que saber antes de tocar nada**. Todo lo necesario está en el repo: no
depende de la memoria local ni de archivos que solo existan en la PC del dueño.

## 1. Dónde estamos

| Sprint | Contenido | Estado |
|---|---|---|
| S52 | Fase 0: informe de inspección y propuesta (sin código) | Hecho y aprobado el 04/10/2026. `docs/sprint-52.md` |
| S53 | Base visual: tokens, tipografía serif, primitivas, galería | Hecho. `docs/sprint-53.md` y `docs/sprint-53-evidence.md` (falta `/cerrar-sprint 53`, del dueño) |
| S54 | Marco: Login → Sidebar → Header → banda | Código hecho; documentación al día. Falta la revisión con sesión real de Google. `docs/sprint-54.md` |
| S55 | Inicio, Dispositivos, Vinculación | Código hecho; documentación al día. `docs/sprint-55.md` |
| S56 | Apps, Reglas, Política y horario, Categorías | Código hecho; documentación al día. `docs/sprint-56.md` |
| S57 | Geocercas, Ubicación, Historial | Código hecho; documentación al día; sin revisión de seguridad registrada. `docs/sprint-57.md` |
| S58 | Estadísticas, Alertas, Silenciadas | Código hecho; documentación al día. `docs/sprint-58.md` |
| S59 | Vista remota, Auditoría, Perfil (y verificación integrada de S54–S59) | Código hecho; documentación al día; falta probar Vista remota con un teléfono real y la revisión de seguridad. `docs/sprint-59.md` |
| S60 | Cierre: accesibilidad, favicon, imágenes, estados vacíos, e2e, `/cerrar-sprint` | **En curso.** `docs/sprint-60.md` |

El código de S54–S59 lo escribió Cristian el 04/10/2026 en una sola tanda (commits `35af89d`, `c76ceea`, `bdfa6e8`,
`8be5abd`, `7b6b117`, `7ec38c7`/`ccde5cf` y `eb2db05`, que corrige defectos y cambia la serif a Newsreader). Sus
documentos de seguimiento y evidencia se escribieron después, en el S60 (T1). La evidencia visual de S54–S59 está
concentrada en `docs/redesign/s59/LEEME.md` (e2e «1 passed» y 18 capturas, tomadas antes de `eb2db05`).

## 2. Ramas

- Cadena real de ramas: `sprint-52-rediseno-web-fase-0` → `sprint-53-rediseno-web-base-visual` →
  `sprint-54-rediseno-web-marco` (con el código de S54–S59) → `fix/s54-auth-log-sin-pii` (arreglo del log de auth,
  `e9e4f69`) → `sprint-60-rediseno-web-cierre` (S60, rama de trabajo actual).
- **`web-redesign`** (creada desde `sprint-51-cierre`, que incluye el S50 y el S51 del rediseño Android) **solo trae los
  documentos del S52**; no contiene el código del rediseño. El pase del rediseño a `main` lo decide el dueño al cerrar
  el S60.
- El plan original preveía `sprint-NN-<slug>` desde `web-redesign` con PR de vuelta a ella; no es lo que ocurrió. Push,
  merge y PR solo cuando el dueño los pida.

> `web-redesign` arrastra los commits del S50/S51 (Android) porque nacieron en `sprint-51-cierre`, que aún no está en
> `main`. No afecta al panel web: el código de `frontend/` es idéntico. Si el S51 se fusiona primero, basta con
> actualizar `web-redesign` desde `main`.

## 3. Entorno desde cero

| Pieza | Cómo |
|---|---|
| Node | 22 (es la versión que usa CI) |
| Panel web | `cd frontend && npm ci && npm run dev` → `http://localhost:3000`. Si `npm ci` falla con `EPERM` es porque `next dev` bloquea un binario: cierra el servidor o usa `npm install` |
| Galería de diseño | `http://localhost:3000/design-system`. **No usa sesión ni backend** (comprobado: no importa `useAuth` ni `sessionStorage`), así que sirve para las Fases 1 y de diseño sin montar nada más |
| Backend (para ver datos reales) | `cp .env.development.example .env` y `docker compose up --build` (ver `docs/environments.md`). API en `localhost:8000` |
| Verificación | `cd frontend && npm run lint && npm run build` (lint con `--max-warnings=0`). En Claude Code: agente `verifier` |
| Pruebas e2e | `frontend/e2e/dashboard.spec.ts` (Playwright; en CI contra un build de producción y el backend en Docker) |
| Docker desde Git Bash en Windows | `export MSYS_NO_PATHCONV=1` antes de cualquier `docker` con rutas de contenedor |
| Git si el repo está en OneDrive | prefijar los comandos con `GIT_OPTIONAL_LOCKS=0` |

**Iniciar sesión en el panel.** El login es solo con Google y lo hace una persona; ni este repo ni el CI pueden
fabricar un token de Google. Opciones para ver las vistas con datos:

1. Pedir al dueño que agregue tu cuenta de Google como usuario de prueba y que te comparta el ID de cliente de Google
   para tu `.env` (los secretos **no están en git**: nunca se commitean `.env` ni `secrets/`).
2. Para capturas y pruebas automáticas sin Google: la sesión semilla que usa CI
   (`backend/scripts/seed_test_session.py` + `frontend/e2e/global-setup.ts`, que además crea un dispositivo vinculado y
   una regla). Su docstring explica por qué no es un atajo de autenticación.

Lo que no se pueda probar en tu entorno (por ejemplo la vista remota real con un teléfono) se **declara pendiente** en
el `docs/sprint-NN-evidence.md`; nunca se da por hecho.

## 4. Herramientas que ya vienen en el repo (`.claude/`)

Al clonar y abrir Claude Code en la raíz recibes, versionados: reglas por área (`.claude/rules/frontend.md` carga sola al
tocar `frontend/`), los agentes `verifier` y `security-reviewer`, y las skills `verificar`, `cerrar-sprint` e `impeccable`.
La memoria de Engram del dueño **no** viaja con el repo; por eso lo aprendido se escribe en `docs/sprint-NN.md` y, si es
permanente, en la regla del área.

## 5. Cómo empezar

1. Leer `01_PROMPT_CLAUDE_CODE.md` (rol, reglas y fases), `02_DESIGN_TARGET.md` (tokens) y `03_VISTAS.md` (vistas).
2. Abrir Claude Code en la raíz del repo, en la rama `sprint-52-…`, y pegar:

   > Lee `docs/redesign/01_PROMPT_CLAUDE_CODE.md` y `docs/redesign/PLAN_SPRINTS.md` y ejecuta únicamente el Sprint 52
   > (Fase 0), siguiendo `docs/sprint-52.md`. No toques código hasta que yo apruebe el informe.

3. Revisar `fase-0-informe.md`, aprobar y pasar al S53. **Un sprint por vez, una vista por commit.**

## 6. Lecciones y trampas ya conocidas

- El prompt original habla de «copiar la carpeta `netprotect-handoff/` al repo»: eso ya está hecho (esta carpeta). Si ves
  `docs/redesign_tmp/` o `docs/netprotect-handoff.zip` en la PC del dueño son copias sobrantes, **no forman parte del repo**.
- Los mockups muestran «NetProject» en el login: es un error del mockup. El nombre es siempre **NetProtect**.
- Los mockups muestran un wordmark serif distinto al logo oficial (sans): **manda el logo oficial**; no se rediseña.
- `logo-full-on-dark.png` es derivado del oficial y está por validar. Faltan el isotipo blanco monocromo y un SVG limpio:
  pedirlos al vectorial original.
- Las ilustraciones de `assets/ilustraciones/` (estilo neón/vidrio) son más recargadas que la dirección sobria: úsalas
  pequeñas o sustitúyelas por ilustración lineal propia. Pesan ~750 KB cada una: optimizar antes de servirlas.
- No editar `frontend/AGENTS.md` (lo regenera `next dev`) ni skills/agentes `impeccable*` (son de terceros).
- No usar Tailwind, librerías de gráficos ni mapas externos.

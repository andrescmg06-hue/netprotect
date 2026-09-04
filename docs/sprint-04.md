# Sprint 4 — Roles y RBAC

## Objetivo

Que elegir "Tutor" no conceda privilegios administrativos automáticamente: el backend decide qué
puede hacer cada usuario, nunca el cliente. Este sprint entrega la selección de rol (autoservicio,
sin riesgo por sí sola) y la infraestructura de autorización real — quién puede actuar sobre qué
recurso — para que los sprints 5 y 6 (vinculación, dispositivos) la usen desde el primer endpoint.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-012 | Como usuario autenticado, quiero elegir si voy a usar NetProtect como Tutor o Supervisado. |
| HU-013 | Como equipo de seguridad, quiero que el backend verifique la propiedad de un dispositivo en cada acceso, no sólo el rol declarado. |

## Criterios de aceptación

1. `POST /users/me/roles` concede el rol solicitado (TUTOR o SUPERVISADO) sin condiciones — poseer
   el rol no es, por sí solo, acceso a ningún dispositivo.
2. La operación es idempotente: pedir un rol ya concedido no crea una fila duplicada ni cambia
   `granted_at`.
3. Un usuario puede sostener ambos roles a la vez.
4. Un código de rol desconocido (`ADMIN`, etc.) se rechaza con 400.
5. `require_tutor_of_device` permite el acceso sólo si existe un vínculo activo en `tutor_devices`;
   un dispositivo ajeno o inexistente responde 404 en ambos casos (nunca 403, para no revelar cuál
   de los dos es).
6. Desvincular un dispositivo (`unlinked_at`) revoca el acceso del tutor de inmediato.
7. `require_role` deniega con 403 a quien no sostiene ninguno de los roles permitidos.

## Tareas técnicas y estado

| Tarea | Estado | Evidencia |
|---|---|---|
| `POST /users/me/roles`, `GET /users/me/roles` | Implementados | `backend/app/api/v1/endpoints/roles.py` |
| Dependencia `require_role(*codes)` | Implementada | `backend/app/api/deps.py` |
| Dependencia `require_tutor_of_device` (404 anti-IDOR) | Implementada | `backend/app/api/deps.py` |
| Matriz de permisos documentada | Implementada | `docs/security-baseline.md` |
| Pruebas de endpoints de roles (5) | Implementadas | `backend/tests/test_roles_integration.py` |
| Pruebas de autorización (6, incluye IDOR y revocación) | Implementadas | `backend/tests/test_authorization_integration.py` |

## Decisiones de diseño relevantes

- **Poseer un rol no es autorización a un recurso.** `require_role` sólo gatea acciones que no
  apuntan a un dispositivo concreto (como pedir un rol). El acceso real a UN dispositivo se decide
  fila por fila con `require_tutor_of_device`, que consulta `tutor_devices` directamente — nunca se
  confía en lo que el token dice sobre el rol para decidir sobre un recurso específico.
- **404, no 403, para un dispositivo ajeno.** Devolver 403 confirmaría que el dispositivo existe pero
  no es tuyo, lo que ayuda a quien intenta enumerar IDs válidos. Se prueba explícitamente que ambos
  casos (no existe / no es tuyo) responden igual.
- **No se construyó ningún endpoint de dispositivos todavía.** La gestión de dispositivos es el
  Sprint 6. `require_tutor_of_device` se prueba directamente contra filas insertadas por el propio
  test (usando los modelos del Sprint 2), no a través de un endpoint inventado para la ocasión — así
  el sprint entrega la lógica de autorización real sin adelantarse al módulo que la va a usar.
- **La concesión de rol es incondicional.** Ni TUTOR ni SUPERVISADO necesitan aprobación: lo que
  importa es qué dispositivos terminan asociados a cada quien (Sprint 5, vinculación), no qué "modo"
  declaró el usuario. Esto es exactamente lo que pedía el enunciado original: "Seleccionar 'Tutor' no
  debe conceder privilegios administrativos automáticamente."

## Ejecución

```bash
docker compose up --build -d
```

```bash
curl -X POST http://localhost:8000/api/v1/users/me/roles \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"role_code": "TUTOR"}'
```

## Verificación

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q
ruff check app tests alembic
```

## Seguridad del sprint

- Toda ruta nueva exige un access token válido (`get_current_user`), sin excepción.
- La autorización por recurso se decide siempre en el backend con una consulta a la base de datos,
  nunca a partir de un campo del token.
- Un dispositivo ajeno y uno inexistente son indistinguibles desde afuera (404 en ambos).

## Fuera de alcance

CRUD de dispositivos (Sprint 6, que es quien realmente usará `require_tutor_of_device` en una ruta
HTTP), vinculación por código (Sprint 5), rate limiting (se aplica primero donde más urge: el código
de vinculación del Sprint 5).

## Definition of Done del Sprint 4

Los 7 criterios de aceptación tienen una prueba automatizada que los ejecuta contra PostgreSQL real,
incluida la revocación de acceso al desvincular y el caso IDOR (tutor A / dispositivo de tutor B).
`ruff` limpio. CI es el criterio definitivo; ver `docs/sprint-04-evidence.md`.

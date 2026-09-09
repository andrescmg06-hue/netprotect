# Sprint 22 — Evidencia

## Backend: ruff

```
$ python -m ruff check app tests alembic
All checks passed!
```

## Backend: primera corrida de la suite nueva — 6 fallos propios del test, no del endpoint

```
backend-1  | FAILED tests/test_audit_integration.py::test_a_tutor_sees_their_own_audited_actions_in_reverse_chronological_order
backend-1  | FAILED tests/test_audit_integration.py::test_device_linked_is_recorded_under_the_supervised_actor_not_the_tutor
backend-1  | FAILED tests/test_audit_integration.py::test_filtering_by_action - AssertionE...
backend-1  | FAILED tests/test_audit_integration.py::test_pagination_returns_total_count_and_the_correct_page
backend-1  | FAILED tests/test_audit_integration.py::test_a_user_cannot_see_another_users_audit_entries
backend-1  | FAILED tests/test_audit_integration.py::test_export_returns_a_csv_with_the_filtered_rows
backend-1  | 6 failed, 244 passed, 5 warnings in 58.61s
```

Dos causas reales, ambas en las pruebas nuevas, no en `audit.py`:

1. `DELETE /pairing/codes/current` devuelve `200`, no `204` como asumí al escribir el test —
   corregido a `200` en las 4 aserciones que lo usaban.
2. `_make_account` (el helper de fixtures compartido con `test_alerts_integration.py`) ya genera
   sus propias dos entradas de auditoría por cuenta creada (`LOGIN` desde `POST /auth/google`,
   `ROLE_GRANTED` desde `POST /users/me/roles`) — mis aserciones asumían que la cuenta de prueba
   empezaba con el registro vacío. Corregidas para: comparar sólo los N registros más recientes en
   vez de la lista completa, o filtrar explícitamente por `resource_type=pairing_code` cuando el
   test necesitaba un conteo exacto ajeno a login/rol.

## Backend: pruebas de integración en Docker (PostgreSQL/Redis reales) — segunda corrida

```
$ docker compose -f compose.test.yaml build backend
$ docker compose -f compose.test.yaml run --rm migrate
$ docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
...
backend-1  | 250 passed, 4 warnings in 161.88s (0:02:41)
backend-1 exited with code 0
```

250 pruebas (241 preexistentes + 9 nuevas de auditoría), todas en verde contra PostgreSQL y Redis
reales en contenedor. Ninguna migración nueva que verificar: `audit_logs` ya existía desde el
Sprint 2 (migración `e27f40867c61`), este sprint no toca el esquema.

## Frontend

Primera corrida de `npm run lint`, un fallo real de `AuditPanel.tsx` — exactamente la regla que
`CLAUDE.md` ya documentaba para este proyecto:

```
frontend/src/components/AuditPanel.tsx
  37:5  error  Calling setState synchronously within an effect can trigger cascading renders
  react-hooks/set-state-in-effect
✖ 1 problem (1 error, 0 warnings)
```

`AuditPanel` reseteaba el estado a `{kind: "loading"}` de forma síncrona al principio del efecto
(para que cambiar de filtro/página mostrara "Cargando…" de inmediato) — la regla lo rechaza igual
que rechazaría cualquier `setState` síncrono en un efecto. Corregido quitando esa línea: el estado
`"loading"` queda sólo como valor inicial de `useState`, mismo patrón exacto que ya usan
`AlertsPanel`/`HistoryPanel` (un cambio de filtro muestra la página anterior hasta que la nueva
respuesta llega, en vez de parpadear a "Cargando…"). Segunda corrida:

```
$ npm run lint
> eslint . --max-warnings=0
(sin salida — limpio)

$ npm run build
> next build
✓ Compiled successfully in 825ms
  Running TypeScript ...
  Finished TypeScript in 2.2s ...
✓ Generating static pages using 4 workers (3/3) in 1110ms
```

## Android

```
$ ./gradlew compileDebugKotlin
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 2m 43s
```

Compila `AuditClient.kt` y los cambios en `TutorScreen.kt` (nueva sección de cuenta "Mi
actividad", fuera del bucle de dispositivos) sin errores.

## Revisión de seguridad

`/security-review` sobre el diff completo (el script de captura automática del skill no obtuvo el
diff por un problema de directorio de trabajo dentro de la sesión — se recuperó manualmente con
`git diff` y se revisó igual). Sin hallazgos de alta confianza:

- `GET /users/me/audit` y su exportación filtran siempre por `AuditLog.actor_user_id ==
  current_user.id` como primer elemento de la lista de filtros, sin que ningún query param pueda
  sobreescribirlo — ningún llamante puede ver ni exportar filas de otro usuario, sin importar su
  rol (verificado también con `test_a_user_cannot_see_another_users_audit_entries`).
- Todas las consultas usan SQLAlchemy Core parametrizado (`select(...).where(*filters)`); ningún
  SQL crudo ni interpolación de strings.
- Sin vector de inyección de fórmulas en el CSV exportado: `action`/`resource_type` son siempre
  literales fijos del propio backend (nunca texto libre de usuario), y `resource_id` es o un UUID
  o un código de rol de un enum cerrado — ningún campo persistido en `audit_logs` contiene texto
  arbitrario que un atacante controle.
- `AuditPanel.tsx` no usa `dangerouslySetInnerHTML` ni inserta HTML dinámico; React ya escapa el
  contenido renderizado.
- `limit`/`offset`/`from_date`/`to_date` están validados por Pydantic (`Query(ge=..., le=...)`,
  tipo `datetime`); una entrada inválida devuelve 422 automático, no llega a la consulta.

`CLAUDE.md`, `README.md` y `docs/security-baseline.md` actualizados.

## CI en GitHub Actions

Commit `cf40fbd` ("feat: add sprint 22 audit log query and export"), corrida
[34312016658](https://github.com/andrescmg06-hue/netprotect/actions/runs/34312016658):

```
✓ android      in 2m0s
✓ integration  in 1m11s
✓ frontend     in 30s
✓ backend      in 28s
```

Los 4 jobs en verde en un runner limpio de GitHub Actions. Sprint 22 cerrado.

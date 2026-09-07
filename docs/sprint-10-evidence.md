# Evidencia de verificación — Sprint 10

Fecha: 06-07/09/2026.

## Catálogo de categorías

No hay Fase C que aplicar (sin capacidad de Android nueva). Sí hubo una decisión de producto
bloqueante: "las 11 categorías del enunciado" no existen en ningún archivo del repo. Se le
preguntó directamente al dueño del proyecto, que confirmó no tener el documento original y pidió
proponer un catálogo razonable — el usado queda documentado en `docs/sprint-10.md`.

## Backend

### Migraciones

```text
alembic upgrade head    → bd93f41d0fa3 -> 9600bc530f75, categories: app category assignments and category rules
alembic downgrade -1    → revierte limpiamente
alembic upgrade head    → reaplicada

alembic upgrade head    → 9600bc530f75 -> 709f0e4bf5a9, app rule events: allow CATEGORY as an applied rule type
alembic downgrade -1    → revierte limpiamente
alembic upgrade head    → reaplicada
```

Verificado con `psql` contra el PostgreSQL de desarrollo real:

```text
--- categoría inválida (debe fallar) ---
ERROR:  new row for relation "app_category_assignments" violates check constraint "ck_app_category_assignments_valid"

--- category_rules DAILY_LIMIT sin minutos (debe fallar) ---
ERROR:  new row for relation "category_rules" violates check constraint "ck_category_rules_daily_limit_requires_minutes"

--- CATEGORY como rule_type_applied (debe aceptarse) ---
INSERT 0 1

--- valor inválido en app_rule_events (debe seguir rechazándose) ---
ERROR:  new row for relation "app_rule_events" violates check constraint "ck_app_rule_events_type_valid"
```

### Suite completa en contenedor

```text
docker compose -f compose.test.yaml build backend migrate
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 125 passed, 3 warnings   (primera corrida)
→ backend-1 | 125 passed, 3 warnings   (segunda corrida, base y Redis recreados desde cero)
```

16 pruebas nuevas en `tests/test_categories_integration.py`: asignación/reasignación de categoría
(upsert), categoría inválida rechazada (422), listado y borrado con las dos formas de "no existe",
regla de categoría con validación por tipo, upsert de regla de categoría, borrado con 404 cruzado
entre dispositivos, `GET /rules/active` incluyendo categorías y reglas de categoría, el dispositivo
reportando un evento `CATEGORY` end-to-end (no sólo el `CheckConstraint`), y auditoría de las
cuatro acciones nuevas (`APP_CATEGORY_ASSIGNED/UNASSIGNED`, `CATEGORY_RULE_CREATED/DELETED`).

`ruff check app tests alembic` → limpio.

## Android

```text
./gradlew compileDebugKotlin testDebugUnitTest --tests RuleEvaluatorTest → BUILD SUCCESSFUL, 24 pruebas
./gradlew test assembleDebug assembleRelease                            → BUILD SUCCESSFUL en 5m32s
```

7 pruebas nuevas en `RuleEvaluatorTest`: una regla de categoría bloquea sin regla de app propia;
una regla de app gana sobre la de su categoría; sin categoría asignada cae en la política por
defecto; una categoría asignada sin regla también cae en la política por defecto; `ALLOW` de
categoría aprueba en modo lista blanca; `DAILY_LIMIT` de categoría bajo el límite aprueba; sobre el
límite reporta `CATEGORY`, no la política por defecto.

## Web

```text
npm run lint   → eslint . --max-warnings=0, limpio
npm run build  → Compiled successfully; TypeScript sin errores; 3 páginas estáticas
```

## `/security-review`

Corrido sobre el diff completo (backend, Android, web). Sin hallazgos de alta confianza: las seis
rutas nuevas (`app-categories`, `category-rules`) reutilizan exactamente
`require_tutor_of_device`/`require_supervised_owner_of_device` y el patrón de borrado
device-scoped ya establecido; el `CheckConstraint` de categorías se construye sólo desde una tupla
de Python fija, nunca desde datos de la petición; sin `dangerouslySetInnerHTML` en el panel nuevo;
sin superficie nueva en Android (evaluación local pura, sin permisos ni E/S nuevos).

## No se marca como verificado

Mismo límite de siempre: el flujo end-to-end con login real de Google (el tutor asigna una
categoría real, el dispositivo la descarga y bloquea de verdad una app real de esa categoría) no
se probó — requiere que una persona elija cuenta en el selector del sistema. Lo que sí se verificó
en su lugar: cada pieza por separado (backend con prueba automatizada positiva y negativa,
`RuleEvaluator` con prueba unitaria real de la cadena de prioridad, ambos APK empaquetando, panel
web compilando limpio).

## CI en GitHub Actions

Pendiente — se corre y se registra en un commit de cierre separado, mismo patrón que los Sprints 8
y 9.

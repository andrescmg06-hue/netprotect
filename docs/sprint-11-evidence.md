# Evidencia de verificación — Sprint 11

Fecha: 07/09/2026.

## Backend

Migración `9fa64cf938a5` (ciclo upgrade → downgrade → upgrade verificado), con `CheckConstraint`
para `weekly_limit_minutes` escrito a mano (mismo motivo que el Sprint 10: autogenerate no detecta
cambios en el cuerpo de un constraint existente). Verificado con `psql`:

```text
--- WEEKLY_LIMIT sin minutos (debe fallar) ---
ERROR:  new row for relation "app_rules" violates check constraint "ck_app_rules_weekly_limit_requires_minutes"
--- WEEKLY_LIMIT con minutos (debe aceptarse) ---
INSERT 0 1
```

```text
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
→ backend-1 | 131 passed, 3 warnings   (primera corrida)
→ backend-1 | 131 passed, 3 warnings   (segunda corrida, base y Redis recreados desde cero)
```

6 pruebas nuevas: `WEEKLY_LIMIT` para `AppRule` y `CategoryRule` (creación + 422 sin minutos), y
zona horaria reportada/ausente en `GET /devices/{id}` tras el heartbeat. `ruff check` limpio.

## Android

```text
./gradlew compileDebugKotlin testDebugUnitTest --tests RuleEvaluatorTest → BUILD SUCCESSFUL, 27 pruebas
./gradlew test assembleDebug assembleRelease                            → BUILD SUCCESSFUL en 5m46s
```

3 pruebas nuevas: `WEEKLY_LIMIT` no bloquea bajo el límite, bloquea exactamente en el límite, y es
independiente del uso diario (uso de hoy alto pero semana bajo el límite no bloquea).

## Web

```text
npm run lint   → eslint . --max-warnings=0, limpio
npm run build  → Compiled successfully; TypeScript sin errores; 3 páginas estáticas
```

## `/security-review`

Sin hallazgos de alta confianza. El `CheckConstraint` nuevo y la migración se construyen sólo con
literales fijos; el campo `timezone` del heartbeat está acotado por `Field(max_length=64)`,
asignado vía ORM parametrizado, y sólo se renderiza como texto plano escapado en React/Compose —
ningún endpoint perdió su dependencia de autorización (sólo inserciones en el diff de los tres
archivos de endpoints).

## No se marca como verificado

Mismo límite de siempre: verificación end-to-end con login real de Google no se hizo (requiere una
persona). Tampoco se verificó con un dispositivo real en una zona horaria distinta a la de la
máquina de desarrollo — `TimeZone.getDefault().id` es una API estándar de Java sin comportamiento
específico de plataforma que verificar, así que no se consideró necesaria una Fase C, pero el valor
reportado en la práctica (con hardware real en otro huso horario) no se probó.

## CI en GitHub Actions

Pendiente — se corre y se registra en un commit de cierre separado, mismo patrón que los sprints
anteriores.

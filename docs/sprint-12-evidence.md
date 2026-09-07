# Evidencia de verificación — Sprint 12

Fecha: 07/09/2026.

## Backend

Migración `e4ff038efe89` (ciclo upgrade → downgrade → upgrade verificado), con el `CheckConstraint`
de `app_rule_events` ampliado a mano (mismo motivo recurrente desde el Sprint 10). Verificado con
`psql`:

```text
--- school_mode_enabled sin ventana (debe fallar) ---
ERROR:  new row for relation "devices" violates check constraint "ck_devices_school_mode_requires_window"
--- con ventana completa (debe aceptarse) ---
UPDATE 1
--- SCHOOL_MODE como rule_type_applied (debe aceptarse) ---
INSERT 0 1
```

```text
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
→ backend-1 | 138 passed, 3 warnings   (primera corrida)
→ backend-1 | 138 passed, 3 warnings   (segunda corrida, base y Redis recreados desde cero)
```

7 pruebas nuevas en `test_school_mode_integration.py`: valor por defecto desactivado, activar con
ventana, activar sin ventana rechazado (422), desactivar limpia la ventana, tutor ajeno no puede
cambiarlo (404), `rules/active` incluye `school_mode`, y el dispositivo puede reportar un evento
`SCHOOL_MODE`. `ruff check` limpio.

## Android

```text
./gradlew compileDebugKotlin testDebugUnitTest --tests RuleEvaluatorTest → BUILD SUCCESSFUL, 34 pruebas
./gradlew test assembleDebug assembleRelease                            → BUILD SUCCESSFUL en 19m34s
```

7 pruebas nuevas: bloquea dentro de la ventana, no bloquea fuera, no hace nada desactivado, una
regla `ALLOW` de app sigue aprobando durante horario escolar, una regla de categoría también sigue
aplicando (reportada como `CATEGORY`, no `SCHOOL_MODE`), respeta la máscara de días, y un
dispositivo ya en modo lista blanca reporta `DEFAULT_POLICY` fuera del horario escolar.

## Web

```text
npm run lint   → eslint . --max-warnings=0, limpio
npm run build  → Compiled successfully; TypeScript sin errores; 3 páginas estáticas
```

## `/security-review`

Sin hallazgos de alta confianza. `PUT /devices/{id}/school-mode` usa exactamente
`require_tutor_of_device`, igual que el endpoint de política del Sprint 9; el `CheckConstraint`
nuevo y la migración se construyen sólo con literales fijos; la validación de Pydantic
(`model_validator`) corre antes de cualquier escritura en base de datos; y ningún endpoint existente
perdió su dependencia de autorización — el diff es puramente aditivo.

## No se marca como verificado

Mismo límite de siempre: verificación end-to-end con login real de Google no se hizo (requiere una
persona). Tampoco se probó con un tutor activando el modo escolar y viendo en vivo el bloqueo
disparado por la franja horaria en un dispositivo físico — verificado en su lugar con pruebas
automatizadas (backend) y unitarias (la lógica de evaluación en Android).

## CI en GitHub Actions

Run [`34153288085`](https://github.com/andrescmg06-hue/netprotect/actions/runs/34153288085), commit
`d54ae4b`, los 4 jobs en verde: `android` (1m42s), `integration` (1m5s), `frontend` (30s), `backend`
(17s). Con esto el Sprint 12 queda cerrado según la regla de `CLAUDE.md`.

---
paths:
  - "backend/app/models/**"
  - "backend/alembic/**"
---

# Base de datos (SQLAlchemy models + Alembic)

- **Nunca `Base.metadata.create_all`**: todo cambio de esquema es una migración de Alembic
  versionada, sin excepción, ni siquiera para "probar algo rápido" en local.
- Una migración ya versionada (commiteada) **no se edita** — se crea una nueva. Flujo:
  `make revision m="descripcion"` genera el archivo, se completa a mano (Alembic no siempre
  detecta bien tipos/constraints), se aplica con `migrate` (nunca `alembic upgrade head` suelto
  contra la base de `compose.yaml`, ver el error de migración desactualizada más abajo).
- Modelos con `Mapped`/`mapped_column` (SQLAlchemy 2.0), consistente con el resto de `backend/app`.

---

**Nota del Sprint 11**: `weekly_limit_minutes` es columna propia (en `app_rules` y
`category_rules`), no un rename de `daily_limit_minutes` — evita tocar la API y las suites de los
Sprints 8-10 sin necesidad. La semana es calendario (lunes 00:00 hora local), no una ventana móvil
de 7 días, coherente con `schedule_days_mask` (bit 0 = lunes). `devices.timezone` (IANA, reportado
en cada heartbeat) es sólo para que el tutor interprete lo que ve — la evaluación en Android ya
usaba correctamente la hora local del dispositivo desde el Sprint 8, no hacía falta reescribirla.

**Nota del Sprint 15**: no se creó ninguna tabla de "historial" genérica — `AppRuleEvent` y
`GeofenceEvent` ya eran insert-only, así que sólo ganaron retención (90 días, purga al escribir,
mismo patrón que `DeviceLocationReport` desde el Sprint 13) y un endpoint que los lee y combina
(`app/api/v1/endpoints/history.py`), sin persistir nada nuevo. Ubicación cruda queda fuera de la
línea de tiempo unificada a propósito: ya tiene su propia vista (Sprint 13) y mezclar hasta 96
puntos/día con eventos discretos enterraría la señal. Ver `docs/sprint-15.md`.

**Nota del Sprint 16**: mismo criterio que el Sprint 15 — sin tabla de agregación nueva, todo se
calcula al vuelo con `GROUP BY` en Python sobre `DeviceApplicationUsage`/`AppCategoryAssignment`/
`AppRuleEvent`/`AppRule`/`CategoryRule`, que ya existían. Los periodos (hoy/7d/30d) son fechas UTC
del servidor, no del huso horario del dispositivo — mismo criterio ya aceptado para `usage_date`
desde el Sprint 7 (etiqueta opaca que el servidor no recalcula). El cumplimiento de límites sólo
aplica a reglas `DAILY_LIMIT` (de app o de categoría); `WEEKLY_LIMIT` y `SCHEDULE` no tienen un
"día cumplido/incumplido" que calcular. Un día sin uso reportado no cuenta ni a favor ni en contra.
Ver `docs/sprint-16.md`.

- `compose.yaml` (el stack de desarrollo persistente) tiene `backend`, `web` y `migrate` como
  servicios con imágenes independientes aunque `backend` y `migrate` compartan el mismo
  `Dockerfile` de `backend/`. Reconstruir `backend`/`web` con `docker compose build` no reconstruye
  `migrate`. Si además se corrió `alembic upgrade head` directo desde el entorno local contra esta
  base (para verificar una migración nueva), la base queda en una revisión que el contenedor
  `migrate` desactualizado no reconoce, y el siguiente `up` falla con
  `Can't locate revision identified by '<revision>'`. Reconstruir los tres servicios juntos
  (`docker compose -f compose.yaml build backend web migrate`) cuando cualquiera de los dos cambie.

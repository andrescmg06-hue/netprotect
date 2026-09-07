# Sprint 10 — Categorías

## Objetivo

El tutor clasifica las apps de un dispositivo supervisado en categorías (Redes sociales, Juegos,
Streaming, etc.) y define una regla por categoría, en vez de tener que repetir la misma regla app
por app. Una regla puesta directamente sobre una app sigue ganando por encima de la de su
categoría; una app sin categoría ni regla propia sigue la política por defecto del dispositivo
(Sprint 9), exactamente como hasta ahora.

## Catálogo de categorías — decisión explícita, no una cita del enunciado

El plan (`docs/planning/plan-desarrollo.md`, Paso 10) menciona "las 11 categorías del enunciado",
pero ese catálogo no existe en ningún archivo de este repositorio — el documento original de 48
secciones queda fuera del repo (ver `CLAUDE.md`). No se inventó una lista y se hizo pasar por la
especificación oficial: se le preguntó directamente al dueño del proyecto, que confirmó no tener
el documento y pidió proponer un catálogo razonable. El que seguimos:

`SOCIAL_MEDIA`, `GAMES`, `STREAMING`, `EDUCATION`, `PRODUCTIVITY`, `COMMUNICATION`, `NEWS`,
`SHOPPING`, `FINANCE`, `UTILITIES`, `ADULT_CONTENT`

Fijo y no editable por el tutor en este sprint — igual que `RuleType`, se declara como constantes
de Python + `CheckConstraint`, no como una tabla con filas que alguien podría borrar y dejar huérfanas
las asignaciones existentes.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-038 | Como tutor, quiero asignar una categoría a una app instalada en el dispositivo supervisado. |
| HU-039 | Como tutor, quiero definir una regla (bloquear/permitir/límite diario/horario) para toda una categoría de apps a la vez, en vez de repetirla app por app. |
| HU-040 | Como tutor, quiero que una regla que puse directamente sobre una app siga ganando por encima de la regla de su categoría. |
| HU-041 | Como tutor, quiero ver y eliminar las asignaciones de categoría y las reglas de categoría que definí. |
| HU-042 | Como usuario supervisado, quiero que una app sin categoría asignada ni regla propia siga funcionando igual que hasta ahora (según la política por defecto del dispositivo). |

## Criterios de aceptación

1. El tutor puede asignar una de las 11 categorías fijas a una app (`package_name`) de un
   dispositivo — upsert por `(device_id, package_name)`: asignar de nuevo reemplaza, no duplica.
2. El tutor puede crear una regla (`BLOCK`/`ALLOW`/`DAILY_LIMIT`/`SCHEDULE`, mismos campos y
   validación que `AppRule`) para una categoría en un dispositivo — upsert por
   `(device_id, category)`.
3. **Prioridad probada explícitamente, no sólo documentada**: si existe una `AppRule` para el
   paquete, gana ella y ni la categoría ni la política por defecto se evalúan. Si no existe, pero
   el paquete tiene una categoría asignada con una regla de categoría, se evalúa esa regla. Si
   ninguna de las dos existe, se aplica la política por defecto del dispositivo (Sprint 9).
4. Igual que una `AppRule` (Sprint 9): una regla de categoría `ALLOW`, o una `DAILY_LIMIT`/
   `SCHEDULE` de categoría que en este momento no bloquea, cuenta como aprobación en modo lista
   blanca — un tutor que aprobó "Juegos: máximo 1 hora/día" aprobó esa categoría para esa hora,
   no sólo dejó pasar el caso por descuido.
5. Las apps protegidas (`ProtectedPackages`, Sprint 9) siguen exentas sólo de cuando la respuesta
   sería la política por defecto — una regla de categoría puesta a propósito por el tutor las sigue
   afectando igual que ya afecta una `AppRule` puesta a propósito.
6. `GET /devices/{id}/rules/active` (que el dispositivo ya consulta) devuelve también las
   asignaciones de categoría y las reglas de categoría, en la misma respuesta que las `AppRule` y
   la política — mismo motivo que unir la política ahí en el Sprint 9: que no puedan desincronizarse
   entre dos llamadas.
7. El tutor puede listar y eliminar asignaciones y reglas de categoría, con el mismo 404-para-ambos
   anti-IDOR del resto del proyecto.
8. Crear/actualizar/eliminar una asignación o una regla de categoría queda auditado.

## Decisiones de diseño relevantes

- **Dos tablas nuevas, no una:** `app_category_assignments` (qué categoría tiene cada app en un
  dispositivo) y `category_rules` (qué regla tiene cada categoría en un dispositivo) son conceptos
  distintos con ciclos de vida distintos — un tutor puede reasignar la categoría de una app sin
  tocar la regla de esa categoría, o cambiar la regla de "Juegos" sin reasignar ninguna app. Fundirlas
  en una sola tabla acoplaría dos decisiones que el tutor toma por separado.
- **`category_rules` reutiliza exactamente la forma de `AppRule`** (mismo `rule_type`, mismas
  columnas de horario/límite, mismos `CheckConstraint`) — la única diferencia real es la clave
  (`category` en vez de `package_name`). No se generalizó en una tabla polimórfica única: el
  proyecto ya evitó ese tipo de abstracción prematura en sprints anteriores, y el costo de dos
  tablas casi idénticas es menor que el de una capa de indirección para "algo con clave A o clave B".
- **La cadena de prioridad vive en `RuleEvaluator` (Android), no en el backend.** El backend sólo
  expone los tres niveles (`rules`, `category_assignments` + `category_rules`, `default_app_policy`)
  en una sola respuesta; quien decide bloquear o no sigue siendo el dispositivo, evaluando
  localmente — mismo principio ya establecido en el Sprint 8 (evitar una consulta de red por cada
  apertura de app).
- **Sin prioridad entre categorías**: una app tiene como máximo una categoría por dispositivo
  (restricción única en `app_category_assignments`), así que nunca hay que decidir entre "regla de
  Juegos vs. regla de Redes sociales" para la misma app. Si el proyecto alguna vez permite varias
  categorías por app, esa seguirá siendo una decisión de un sprint futuro, no de este.
- **Catálogo fijo, no una tabla editable** (ver sección de arriba): con sólo 11 valores conocidos y
  sin necesidad de que el tutor cree categorías propias en este sprint, una tabla con filas
  borrables introduciría un problema real (asignaciones huérfanas) sin resolver ningún requisito
  real de este sprint.
- **Reutiliza `require_tutor_of_device`/`require_supervised_owner_of_device`** sin variantes nuevas,
  mismo patrón que cada sprint anterior.

## Fuera de alcance

- Clasificación automática de apps por categoría (heurística, API de alguna tienda) — el tutor
  asigna la categoría a mano, mismo nivel de esfuerzo manual que ya existe para las `AppRule`.
- Categorías de **dominios/navegación web** — la propia historia original (`user-stories.md`)
  menciona "aplicaciones y navegación", pero el filtrado web con `VpnService` sigue sin construirse
  (evaluado para Sprint 8/9, pospuesto ambas veces) y no tiene su propia Fase C todavía. Categorías
  de apps no depende de eso; se separa para no bloquear este sprint en uno que no se ha empezado.
- Categorías editables/creadas por el tutor — catálogo fijo por ahora (ver decisiones de diseño).
- Ninguna verificación nueva de Android: este sprint no toca ningún permiso ni mecanismo del sistema
  operativo que no estuviera ya verificado en la Fase C de los Sprints 7-9 — es dato nuevo sobre el
  mismo motor de evaluación ya construido, no un mecanismo nuevo.

## Backend

- `app_category_assignments` y `category_rules` (migración `9600bc530f75`), más una migración
  separada (`709f0e4bf5a9`, escrita a mano porque el autogenerate de Alembic no detecta cambios en
  el cuerpo de un `CheckConstraint` existente) que agrega `CATEGORY` como valor válido de
  `app_rule_events.rule_type_applied`.
- `POST/GET /devices/{id}/app-categories`, `DELETE /devices/{id}/app-categories/{id}` (tutor):
  upsert por `(device_id, package_name)`, mismo patrón anti-IDOR del resto del proyecto.
- `POST/GET /devices/{id}/category-rules`, `DELETE /devices/{id}/category-rules/{id}` (tutor):
  upsert por `(device_id, category)`, misma validación por tipo que `AppRule`.
- `GET /devices/{id}/rules/active` (dispositivo supervisado) ahora también devuelve
  `category_assignments` y `category_rules` en la misma respuesta, sin round-trips adicionales.

## Android

- `Category`, `CategoryAssignment`, `CategoryRule`, y `BlockReason.CATEGORY` nuevos en
  `core/rules/AppRule.kt`.
- `RuleEnforcementClient.getActiveRules()` parsea las categorías junto con las reglas.
- `RuleEvaluator.evaluate()` implementa la cadena de prioridad: regla por app → regla de categoría
  (si la app tiene categoría asignada) → política por defecto — con `evaluateRuleFields()` como
  helper compartido entre `AppRule` y `CategoryRule` (misma forma, misma validación).
- `BlockScreenActivity` explica "tu tutor bloqueó la categoría a la que pertenece esta app" cuando
  el motivo es `CATEGORY`.

## Web

- `DeviceCategoriesPanel` (componente nuevo): asignar/quitar categoría a una app, y crear/eliminar
  la regla de una categoría — mismo patrón de formularios y listas que `DeviceRulesPanel`.
- `DevicesPanel` gana el botón "Gestionar categorías" junto a los ya existentes.

## Verificación

Backend: **125 pruebas pasan** (16 nuevas en `test_categories_integration.py`), verde dos veces
seguidas en contenedor limpio, con `ruff check` limpio. Ciclo upgrade → downgrade → upgrade
verificado para ambas migraciones nuevas contra PostgreSQL real, incluida la comprobación directa
con `psql` de que los `CheckConstraint` nuevos (categoría inválida, `DAILY_LIMIT` sin minutos, y el
valor `CATEGORY` en `app_rule_events`) se comportan como se espera.

Android: `./gradlew test assembleDebug assembleRelease` en verde — **24 pruebas** en
`RuleEvaluatorTest` (17 previas + 7 nuevas de la cadena de prioridad de categorías), ambos APK
empaquetan sin errores.

Web: `npm run lint` (`--max-warnings=0`) y `npm run build` (TypeScript sin errores) en verde.

`/security-review`: sin hallazgos de alta confianza — las seis rutas nuevas reutilizan
exactamente el patrón anti-IDOR ya establecido, sin SQL dinámico con datos de usuario, sin
`dangerouslySetInnerHTML`, sin superficie nueva en Android.

Pendiente, mismo límite de siempre: verificación end-to-end con login real de Google (requiere una
persona eligiendo cuenta en el selector del sistema). CI en GitHub Actions: pendiente de correr
tras el push.

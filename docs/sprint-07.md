# Sprint 7 — Inventario de aplicaciones

## Objetivo

El tutor ve qué apps tiene instaladas el dispositivo supervisado y cuánto tiempo se usó cada una
hoy, en la app Android del tutor y en el panel web. El dispositivo supervisado reporta esto solo,
sin acción manual del usuario más allá de conceder el permiso de estadísticas de uso una vez.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-024 | Como tutor, quiero ver qué apps tiene instaladas el dispositivo supervisado. |
| HU-025 | Como tutor, quiero ver cuánto tiempo se usó cada app hoy. |
| HU-026 | Como tutor, quiero saber si una app fue desinstalada del dispositivo supervisado. |
| HU-027 | Como dispositivo supervisado, mi app reporta el inventario sola, sin que yo tenga que hacer nada salvo activar el acceso a uso una vez. |

## Criterios de aceptación

1. El dispositivo supervisado reporta su lista de apps instaladas —sólo las que tienen ícono
   propio, no los cientos de paquetes de sistema sin interfaz— y el tiempo de uso del día actual.
2. Sincronizar dos veces el mismo día **sobrescribe** el tiempo de uso de ese día, no lo acumula:
   `UsageStatsManager` ya entrega un total corriente, no un incremento.
3. Una app que deja de reportarse en una sincronización se marca como desinstalada
   (`uninstalled_at`) sin borrar su historial de uso previo.
4. Una app reinstalada dentro del mismo dispositivo deja de aparecer como desinstalada.
5. Sólo el tutor vinculado ve el inventario de un dispositivo; sólo el propio dispositivo
   supervisado puede sincronizar el suyo — mismo patrón 404 uniforme del resto del proyecto para
   "no existe" y "no es tuyo".
6. La app del tutor (Android) y el panel web muestran la lista de apps con su uso más reciente,
   ordenada por tiempo de uso.
7. El acceso a estadísticas de uso se pide con una explicación visible en la propia app antes de
   enviar al usuario a Ajustes — Android no ofrece una forma de concederlo desde la app misma.

## Procedimiento obligatorio de la Fase C — verificación antes de codificar

Aplicado y documentado en detalle en `docs/android/capability-matrix.md` antes de escribir una
sola línea de Android. Resumen de lo verificado contra fuentes oficiales actuales (no memoria de
entrenamiento):

- **Enumerar apps** exige `QUERY_ALL_PACKAGES` porque Android 11+ oculta por defecto los paquetes
  instalados y no podemos declarar de antemano cuáles tendrá un dispositivo supervisado.
- **Tiempo de uso** exige `UsageStatsManager` + el permiso especial `PACKAGE_USAGE_STATS`,
  concedido por el usuario en Ajustes, no por un diálogo de la app.
- Ambos permisos están sujetos a políticas de **Google Play** (formulario de declaración,
  divulgación destacada, política anti-stalkerware para apps de control parental) — pero esas
  políticas se activan al **publicar** en la tienda, no al usar el permiso. Este proyecto se
  instala por sideload (Android Studio/`adb`) para el trabajo de la universidad, así que hoy no
  aplican. Se verificó además que Play Protect (la protección del propio teléfono) no bloquea la
  instalación sideloaded por estos dos permisos — sólo por otros cuatro que no usamos
  (`RECEIVE_SMS`, `READ_SMS`, notificaciones, accesibilidad). Queda documentado para si el
  proyecto se publicara alguna vez, no implementado ahora porque no hace falta.

## Decisiones de diseño relevantes

- **Dos tablas independientes, no una jerarquía con `ON DELETE CASCADE` entre ellas.**
  `device_applications` (el catálogo: qué apps existen) y `device_application_usage` (cuánto se
  usó cada una, por día) se enlazan sólo por `(device_id, package_name)`, sin llave foránea entre
  sí. El historial de uso de una app debe sobrevivir a que esa app se desinstale más adelante; si
  `device_application_usage` dependiera de `device_applications` con cascada, borrar o reemplazar
  el catálogo se llevaría el historial con él.
- **Baja lógica del catálogo (`uninstalled_at`), nunca borrado.** La misma razón que
  `revoked_at`/`unlinked_at` en sprints anteriores: "ya no está" y "nunca existió" son hechos
  distintos, y el tutor tiene derecho a ver que una app fue removida, no que desapareció sin
  explicación.
- **El día de uso se sobrescribe, no se acumula.** `UsageStatsManager` ya entrega el total del día
  hasta el momento de la consulta; sumarlo a sincronizaciones anteriores del mismo día duplicaría
  el tiempo. La restricción única `(device_id, package_name, usage_date)` hace de esto un upsert
  natural.
- **Filtrado a apps con lanzador, no todo lo que devuelve `PackageManager`.** Un teléfono típico
  tiene cientos de paquetes de sistema sin interfaz (proveedores de telefonía, servicios internos)
  que no significan nada para un padre. Se filtra a lo que tiene una actividad con
  `CATEGORY_LAUNCHER` — lo que el usuario vería en su propio launcher — incluyendo apps del
  sistema con interfaz real (Cámara, Teléfono) que sí importan.
- **`GET /devices/{id}/applications` usa `DISTINCT ON` de PostgreSQL**, no cargar todo el
  historial de uso en Python para quedarse con el más reciente. Es una elección específica de
  Postgres, deliberada: este proyecto no persigue portabilidad entre motores de base de datos, y
  sin retención automática todavía (ver "Fuera de alcance"), el historial de uso sólo va a crecer.
- **Reutiliza `require_supervised_owner_of_device` y `require_tutor_of_device`** tal cual, sin
  variantes: sincronizar es una acción del propio dispositivo, listar es una acción del tutor —
  exactamente la misma distinción que ya gobierna heartbeat vs. listado de dispositivos.
- **La app del tutor no fuerza el permiso de uso; el panel web tampoco.** Ninguno de los dos puede
  concederlo — sólo el dispositivo supervisado lo hace, en su propia sesión. La lista de apps
  simplemente aparece vacía o sin datos de uso hasta que el supervisado lo active; no hay bloqueo
  artificial del lado del tutor.

## Cambios de base de datos

Migración `fedd07a82d4e`: dos tablas nuevas, `device_applications` y `device_application_usage`,
cada una con `device_id` indexado y su propia restricción única (ver decisiones arriba). Ciclo
upgrade → downgrade → upgrade verificado contra el PostgreSQL de desarrollo real, no sólo el de
pruebas.

También se le da uso por primera vez a `device_status.last_sync_at` (columna que existía desde el
esquema original del Sprint 2 sin ningún endpoint que la actualizara): cada sincronización de
aplicaciones la pone al día.

## Backend

- `POST /devices/{device_id}/applications/sync` (dispositivo supervisado): recibe el catálogo
  completo de apps instaladas más el uso de un día, reconcilia el catálogo (upsert + baja lógica
  de lo que falta) y hace upsert del uso de ese día.
- `GET /devices/{device_id}/applications` (tutor): catálogo completo con el uso más reciente
  disponible por app, ordenado alfabéticamente (el cliente reordena por uso al mostrar).

## Android

- `AppInventoryCollector`: `PackageManager` filtrado a apps con lanzador; `UsageStatsManager`
  para el uso de hoy, sumando por si `queryUsageStats` devuelve más de un bucket por paquete.
- `UsageAccessPermission`: consulta `AppOpsManager` (única forma confiable de saber si
  `PACKAGE_USAGE_STATS` está concedido, al no existir diálogo runtime) y abre
  `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- `SupervisedScreen`: tarjeta explicando por qué se necesita el acceso a uso antes de mandar al
  usuario a Ajustes, con botón de reverificación manual. Una vez concedido, sincroniza cada 5
  minutos mientras la pantalla está en primer plano — mismo enfoque que el heartbeat del Sprint 6,
  sin scheduler en segundo plano todavía.
- `TutorScreen`: botón "Ver apps" por dispositivo, lista expandible ordenada por uso.

## Web

- `DevicesPanel` gana un botón "Ver apps" por dispositivo; `DeviceApplicationsList` (componente
  nuevo) carga y muestra el inventario, ordenado por uso.

## Ejecución

```bash
docker compose up --build -d

# el propio dispositivo supervisado sincroniza
curl -X POST http://localhost:8000/api/v1/devices/<id>/applications/sync \
  -H "Authorization: Bearer <token_supervisado>" -H "Content-Type: application/json" \
  -d '{"usage_date":"2026-09-05","installed_apps":[{"package_name":"com.instagram.android","app_label":"Instagram","is_system_app":false}],"daily_usage":[{"package_name":"com.instagram.android","foreground_seconds":1800}]}'

# el tutor consulta
curl http://localhost:8000/api/v1/devices/<id>/applications -H "Authorization: Bearer <token_tutor>"
```

Panel web: `http://localhost:3000` → dispositivo vinculado → "Ver apps".

## Verificación

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

cd frontend && npm run lint && npm run build

cd mobile && ./gradlew test assembleDebug assembleRelease
```

## Seguridad y privacidad del sprint

- **Minimización real, no sólo declarada.** No se recolectan íconos, capturas, ni eventos de uso
  con marca de tiempo fina — sólo nombre de paquete, etiqueta visible y segundos de uso agregados
  por día. Nada que un padre no vería igual mirando el teléfono directamente.
- **Control de acceso**: exactamente las mismas dependencias de autorización del resto del
  proyecto (`require_tutor_of_device`, `require_supervised_owner_of_device`), sin variante nueva
  que revisar.
- **Sin auditoría de la sincronización en sí**, por la misma razón que el heartbeat no se audita:
  ocurre cada pocos minutos mientras la app está abierta, y el propio historial de
  `device_application_usage` ya es el registro. Si el tutor pide la lista, tampoco se audita —
  leer no es una acción que revisar después, a diferencia de renombrar o desvincular.
- **Retención pendiente, declarada como tal.** No existe todavía una tarea que purgue filas de
  `device_application_usage` antiguas (no hay scheduler en el proyecto — mismo estado que el
  heartbeat del Sprint 6). Es deuda real, no un descuido oculto: se documenta aquí para que quien
  continúe el proyecto no asuma que ya está resuelto.

## Fuera de alcance

- Categorías de apps, listas blancas/negras y reglas de bloqueo — Sprints 8 a 10.
- `usage_events` de grano fino para evaluación de reglas en tiempo real — Sprint 8, con su propio
  modelo de datos; este sprint sólo agrega totales diarios para mostrar, no para hacer cumplir
  nada.
- Zona horaria explícita del dispositivo en `usage_date` — se trata como una fecha calendario que
  el dispositivo asigna, sin normalizar contra su zona horaria real; queda para el Sprint 11
  (horarios y modo escolar), que ya necesita resolver esto de todas formas.
- Purga/retención automática del historial de uso (ver sección de seguridad arriba).
- El diseño de notificación persistente / foreground service para modo Supervisado que exigiría
  Google Play si este proyecto se publicara — no aplica mientras la instalación sea por sideload;
  documentado en `docs/android/capability-matrix.md` para si algún día cambia.

## Definition of Done del Sprint 7

Los 7 criterios de aceptación tienen prueba automatizada contra PostgreSQL real: 14 pruebas nuevas
en `tests/test_applications_integration.py`, verdes dos veces seguidas en contenedor limpio, sobre
una suite total de 76. Android compila, pasa sus pruebas y empaqueta debug y release. El panel web
pasa lint estricto y build de producción. Ver `docs/sprint-07-evidence.md`.

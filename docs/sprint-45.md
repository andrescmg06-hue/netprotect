# Sprint 45 — Tutor: Apps, Ubicación y Geocercas

Sexto sprint del rediseño Android (`docs/android-redesign/sprints/S45-tutor-apps-ubicacion-geocercas.md`). Mockups 5, 6 y 7.
Decisiones aplicadas: D-05 (privacidad de la ubicación), D-07 (iconos de apps: el local si existe, monograma si no).
Son datos de un menor: el criterio del sprint es **no mostrar más de lo que los datos permiten**.

## Qué se hizo

- **Claude — lógica y enrutado** (`f557e7e`):
  - `GeoMath`: distancia (haversine) y `certainlyInside`. "Dentro de «zona»" solo se dice cuando distancia + precisión ≤
    radio; con precisión peor que el radio nunca, ni en el centro. Una lectura "quizá dentro" no dice nada.
  - `locationView` (la zona más pequeña que contiene con certeza la lectura) y `geofencesView` (último evento de cada
    zona por `geofence_id`, historial del más nuevo al más viejo). `GeofenceEvent` lee `geofence_id`.
  - `sortApps` (instaladas primero, más uso, nombre) y `filterApps` (sin mayúsculas ni tildes; también el paquete).
  - `usageLabel` ("1 h 24 min", "12 min · ayer", "12 min · 24 sep", "Sin uso registrado") y `eventTimeLabel`
    ("Hoy, 8:12 a. m.") — el uso ya no finge ser de hoy cuando es de otro día.
  - `Loader<T>` (carga + 404 como "no existe o sin acceso") y `AppIcon` (icono real del `PackageManager` local; si la
    app no está en este teléfono, monograma con color estable por paquete).
  - `TutorShell` enruta las tres secciones a sus pantallas; "Abrir en mapa" lanza un intent `geo:` solo al tocar.
  - 14 tests JVM (`SectionViewsTest`).
- **DeepSeek** (`docs/delegated/done/sprint-45-apps-ubicacion.md`, `20aa9e1`): las tres pantallas, la cabecera compartida
  del dispositivo, 18 tests de UI, 15 estados nuevos en la galería y retirada de las tres secciones antiguas de
  `LegacySections.kt`.
- **Claude — revisión** (`04e40dd`): el buscador de Apps pasa a ancho completo con "Actualizar" debajo; al lado del botón
  quedaba tan estrecho que su texto se partía en dos líneas.

## Cambios visibles para el tutor

- Apps: buscador, orden por uso, iconos, "Desinstalada", y la fecha del uso cuando no es de hoy.
- Ubicación: "Última ubicación conocida", hora de la lectura, precisión aproximada, "Dentro de «zona»" solo si es seguro,
  "Abrir en mapa" con aviso si no hay app de mapas, y texto de retraso.
- Geocercas: radio, último evento (Entró / Salió / Sin actividad) e historial de entradas/salidas, con el aviso de ~15 min.

## Decisiones (aprobadas por el dueño)

- **Sin coordenadas** en Ubicación: el mapa externo ya las recibe al tocar, y en pantalla solo aumentan el riesgo si
  alguien mira por encima del hombro.
- **Un solo icono** para todas las geocercas: elegir casa/colegio por el nombre sería adivinar.
- **Apps del sistema se muestran** como hasta ahora.

## Diferencias con los mockups aceptadas

| Mockup | Lo que dibuja | Qué se hizo | Por qué |
|---|---|---|---|
| 6 | Nombre del lugar ("Acacías, Meta") | Nada | Obtenerlo enviaría la ubicación del menor a un tercero (D-05) |
| 6 | Mapa con el punto | Nada | Sin teselas ni SDK de mapas; el intent `geo:` delega en la app del usuario |
| 7 | Ciudad bajo cada geocerca | "Radio N m" | El dato no existe |
| 7 | Iconos distintos por zona | Uno solo | No hay tipo de zona en la API |
| 5 | Flecha en cada app | Sin flecha | No hay detalle de app |
| 5–7 | Miniatura de tablet | Icono genérico | La API no distingue tablet de teléfono |

## Pendiente

- Prueba con datos reales de ubicación: requiere el teléfono supervisado (las filas están cifradas en la base de datos).
  Los estados se verificaron en la galería y con tests.
- El espaciado del historial lo fija `TimelineItem` (componente compartido); se revisa en S46, que también lo usa.
- Aspecto en el Galaxy S25 FE.

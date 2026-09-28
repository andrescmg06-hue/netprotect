# Sprint 45 — Tutor: Apps, Ubicación y Geocercas (pantallas 5, 6 y 7)

**Prioridad: HIGH** — ubicación y geocercas son datos de un menor: el riesgo aquí es mostrar más o
distinto de lo que hay. **Dueño:** DeepSeek (pantallas) + Claude (revisión, seguridad). **Rama:**
`sprint-45-apps-ubicacion`. **Mockups:** `05`, `06`, `07`. **Decisiones:** D-05, D-07.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Las rutas Apps, Ubicación y Geocercas del shell muestran las pantallas nuevas; sus secciones
antiguas salen de `LegacySections.kt`. Todo dato mostrado sale de la API; ningún texto sugiere tiempo
real.

## Problema que resuelve

G-12 y G-13. Hoy son secciones desplegables oscuras; el mockup añade datos que no existen (lugar,
mapa, ciudad de la geocerca) que hay que no copiar.

## Dependencias

S44 (shell y rutas). D-05 y D-07 resueltas.

## Alcance

- **Apps** (mockup 5, `GET /devices/{id}/applications`): cabecera de dispositivo compacta ("Uso
  sincronizado periódicamente · última actualización hace N min", con `last_seen_at`); buscador
  (filtro local por nombre); "Actualizar"; filas ordenadas por uso descendente con icono (D-07),
  nombre (`app_label`) y uso: si `latest_usage_date` es hoy, "1 h 24 min"; si no, "12 min · ayer" o
  "12 min · 24 sep"; sin dato, "Sin uso registrado". Instaladas antes que desinstaladas; las
  desinstaladas con badge "Desinstalada" (`uninstalled_at`). Apps de sistema: mismo criterio que hoy
  (confirmar en `INVENTARIO.md`). Vacío, carga, error. `InfoBanner`: "Los tiempos de uso se
  sincronizan periódicamente y pueden tener un retraso de algunos minutos." **Sin** crear reglas.
- **Ubicación** (mockup 6, `GET .../location/latest`), según D-05: tarjeta "Última ubicación
  conocida" + "hace N min"; hora de la lectura (`captured_at`); "Precisión aproximada: ~{m} m";
  si D-05 (a): "Dentro de «{geocerca}»" cuando la lectura cae en una geocerca (cálculo local,
  distancia ≤ radio − precisión; si es dudoso, no se dice), para lo que se cargan también las
  geocercas; botón "Abrir en mapa" (el intent `geo:` actual, con el texto "Se abrirá tu aplicación de
  mapas con la última ubicación conocida del dispositivo."); **sin** nombre de lugar ni mapa.
  Vacío: "Este dispositivo todavía no ha reportado su ubicación." `InfoBanner` de retraso.
- **Geocercas** (mockup 7, `GET .../geofences` y `.../geofences/events`): "Detección aproximada ·
  ~15 min" y "Las entradas y salidas se detectan en el siguiente reporte."; lista de geocercas con
  icono, nombre, "Radio {m} m" (no ciudad) y último evento derivado de los eventos ("Entró" verde /
  "Salió" naranja / "Sin actividad", con hora); "Historial de entradas/salidas" en línea de tiempo.
  Solo lectura: ningún botón de crear/editar. Vacíos: "No hay geocercas configuradas. Se crean desde
  el panel web." / "Sin entradas ni salidas registradas."
- Quitar de `LegacySections.kt` las tres secciones sustituidas.

## Fuera de alcance

Mapa embebido o teselas; geocodificación; historial de ubicaciones (`/location/history`, es del web);
crear/editar geocercas; reglas por app.

## Archivos/módulos afectados

Nuevos `feature/tutor/apps/AppsScreen.kt`, `feature/tutor/location/LocationScreen.kt`,
`feature/tutor/geofences/GeofencesScreen.kt`, `ui/format/GeoMath.kt` (si D-05 = a),
`ui/components/AppIcon.kt` (D-07); modificados `feature/tutor/TutorShell.kt` (rutas),
`feature/tutor/legacy/LegacySections.kt` (quitar secciones). Clientes sin cambios
(`ApplicationsClient`, `LocationClient`, `GeofenceClient`).

## Trabajo por capa

Backend, web, BD: ninguno.

## Seguridad

Invariante 6: la ubicación no sale del teléfono del tutor hacia ningún servicio (el intent `geo:` lo
lanza la persona). Sin coordenadas ni nombres de apps en logs. El icono local (D-07 b) no envía nada.
`security-reviewer` obligatorio (ubicación).

## Testing

JVM: "dentro de geocerca" (bordes: justo en el radio, precisión mayor que el radio → no se afirma),
último evento por geocerca, texto de uso según fecha. UI (si D-13 = a): vacío/error/datos de las tres.
Manual con el teléfono supervisado reportando: capturas de las tres pantallas con datos y vacías.

## Criterios de aceptación

- [ ] Ningún nombre de lugar, ciudad ni mapa; ningún texto de "tiempo real".
- [ ] Uso de apps con su fecha real cuando no es de hoy.
- [ ] "Abrir en mapa" abre la app de mapas.
- [ ] Geocercas sin botones de edición; último evento correcto.
- [ ] Secciones antiguas retiradas; nada perdido respecto a `INVENTARIO.md`.
- [ ] `security-reviewer` sin hallazgos ALTA; `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF.

## Riesgos

El mockup "empuja" a copiar lugar y mapa. Zonas horarias: las horas se muestran en la zona del
teléfono del tutor (como el web con es-CO).

## Decisiones técnicas

"Dentro de" solo si la certeza es clara (distancia + precisión ≤ radio). Orden de apps: uso de la
última fecha disponible, descendente; empate por nombre.

---

## PROMPT A — Claude Code

```
Sprint 45 del rediseño Android de NetProtect: Apps, Ubicación y Geocercas del tutor.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S45-tutor-apps-ubicacion-geocercas.md), mockups/README.md §5–7, los
mockups 05, 06 y 07, D-05 y D-07, docs/sprint-44.md (patrón de estado y errores de DeepSeek).
Código: feature/tutor/legacy/LegacySections.kt (secciones actuales), los clientes
Applications/Location/Geofence, TutorShell.kt, ui/components, backend/app/schemas de applications,
location y geofence (para confirmar los campos).

Fases 1–3; espera mi OK. Escribe docs/delegated/pending/sprint-45-apps-ubicacion.md (formato del
Sprint 39): tres pantallas, textos literales, campos exactos de la API que usa cada dato, lo que NO se
dibuja del mockup, funciones puras con sus tests, lista cerrada de archivos, pasos y verificación.
Commit docs(sprint-45) con mi OK.
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-45-apps-ubicacion.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-45-apps-ubicacion.md. Lee su informe; commit de lo suyo
tal cual con mi OK. Fases 6–10 del HANDOFF:
- Diff contra el encargo y contra mockups/README.md §5–7: ¿algún lugar, ciudad, mapa, "tiempo real",
  botón de edición o dato no presente en la API? Elimínalo.
- verifier: test assembleDebug lintDebug (+ UI tests).
- Capturas (teléfono supervisado reportando datos) en %TEMP%\np-sprint45\: las tres pantallas con
  datos, vacías y con error; compáralas con 05, 06 y 07.
- security-reviewer (ubicación, invariante 6). Corrige.
- docs, /cerrar-sprint 45, informe del §9. Commits separados con mi OK.
```

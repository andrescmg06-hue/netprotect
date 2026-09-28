# Sprint 36 — Ubicación, Geocercas e Historial

Sexto sprint de `docs/planning/plan-frontend.md`.

## Qué se hizo

- **Ubicación** (`DeviceLocationPanel`): métricas de precisión y última captura, tarjeta con las
  coordenadas y "Actualizar". Se conserva exactamente la lógica del Sprint 13: mapa embebido de
  Google Maps si `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` está configurada, texto + enlace externo si no.
- **`GeofenceMap`** (compartido, nuevo): un esquema propio en SVG, no un mapa real — decisión
  tomada al empezar el sprint y ya registrada en `plan-frontend.md`. Proyecta cada geocerca y la
  última ubicación conocida a un plano local en metros (`lon·cos(lat)·111320`, `lat·111320`), así
  que el radio de cada círculo queda a escala real entre zonas sin ninguna librería de mapas ni
  envío de coordenadas a un servicio externo. Con cero puntos muestra un `EmptyState`; con un
  único punto usa su propio radio como referencia de encuadre.
- **Geocercas** (`GeofencePanel`): el mapa como elemento principal arriba, seguido de la lista
  (`DataTable`, con `ConfirmDialog` para eliminar — mismo patrón que Desvincular en Dispositivos
  desde el Sprint 34) y el formulario de creación/edición, con "Usar última ubicación conocida" y
  el historial de entradas/salidas como antes. Se agregó una segunda llamada a
  `getLatestLocation` (mismo endpoint que ya usaba el botón de prefill) sólo para alimentar el
  marcador de "última ubicación" del mapa — ninguna llamada nueva a un endpoint que no existiera.
- **`Timeline`** (compartido, nuevo, en `components/ui/`): agrupa una lista de eventos por día de
  calendario preservando el orden en que llegan, con un marcador de color por `Tone` y la hora de
  cada uno.
- **Historial** (`HistoryPanel`): mismo `GET /devices/{id}/history` de siempre, ahora sobre
  `Timeline` con un filtro local (`SegmentedControl`: Todo / Bloqueos / Geocercas) y "Actualizar".
  El filtro es sólo de presentación — mismos eventos, mismo orden del backend, nada recalculado.

## Decisiones

- **Esquema SVG, no Leaflet/OpenStreetMap.** Ver `plan-frontend.md`: enviar la ubicación de un
  menor a un servidor de teselas externo no es aceptable, y un mapa real con clic-para-crear sería
  una función nueva, no un rediseño de la que ya existe (hoy se escribe la coordenada a mano o se
  usa "última ubicación conocida"). El esquema deja explícito en su propio pie de página que no es
  un mapa real, para que un tutor no lo confunda con calles o terreno.
- **Ubicación cruda sigue fuera del historial unificado** (regla ya establecida en el Sprint 15):
  se mantiene así.

## Verificaciones

`impeccable detect --json` sobre los 10 archivos del sprint: sin hallazgos. ESLint, `tsc` y
`next build` en verde.

Datos reales sembrados contra `compose.test.yaml` (un tutor y un dispositivo emparejados de
verdad vía `POST /pairing/codes` + `POST /pairing/redeem`, dos geocercas creadas con
`POST /geofences`, cuatro reportes de ubicación reales que cruzan la geocerca "Casa" — generando
sus propios eventos `ENTER`/`EXIT` reales desde `evaluate_geofence_transitions()`, no insertados a
mano — y dos eventos de regla aplicada vía `POST /rule-events`). Capturas reales a 1440 y 390 px
en las tres vistas; `document.documentElement.scrollWidth` igual al ancho del viewport en las seis
combinaciones, sin desbordamiento horizontal.

Cada acción de las tres vistas se ejecutó una vez contra ese backend real con Playwright
(script temporal, borrado al cerrar el sprint): crear geocerca, abrir "Editar" y confirmar que
precarga sus datos, cancelar la edición, eliminar con confirmación (cancelando el diálogo una vez
y confirmándolo la siguiente), ver el historial de entradas/salidas, "Actualizar" en Ubicación, y
los tres filtros de Historial (Todo/Bloqueos/Geocercas) — las 13 aserciones pasaron contra
respuestas reales de la API, no datos mockeados.

## Tropiezo

El puerto 8000 de `compose.test.yaml` colisionó con el `backend` del stack de desarrollo
(`compose.yaml`), que también lo publica en `127.0.0.1:8000` — hubo que detener `backend`/`web`
del stack de desarrollo mientras corría la verificación y restaurarlos al cerrar el sprint (ya
anotado como parte del ritual de cada sprint). Además, el primer intento de `docker compose -f
compose.test.yaml up -d api_server` falló al chocar con ese puerto y dejó un contenedor a medio
crear sin su publicación de puerto; `docker compose ... rm -sf api_server` seguido de `up -d`
de nuevo lo recreó correctamente. Por último, el *refresh token* del tutor (rotativo y de un solo
uso, ver nota del Sprint 25) se agotó varias veces entre corridas del script de Playwright — se
reemplazó reinsertando una fila `UserSession` nueva para el mismo tutor ya sembrado, con las
funciones propias y no mockeadas del proyecto (mismo mecanismo que
`backend/scripts/seed_test_session.py`), en vez de crear un tutor distinto sin el dispositivo ya
vinculado.

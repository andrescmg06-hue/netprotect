# Sprint 57 — Rediseño editorial del panel web, ubicación: Geocercas, Ubicación, Historial

Sexto sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Código de Cristian del 04/10/2026, commit `8be5abd`
(10:11); algunos defectos se corrigieron después en `eb2db05`. Documento escrito **después**, en el S60 (T1), a partir
del mensaje y de las estadísticas de los commits y de `docs/redesign/s59/LEEME.md`.

## Objetivo

Que las tres vistas de monitoreo dejen la rejilla «métricas arriba, tarjetas abajo»: cada una con **un protagonista**,
sin enviar coordenadas de un menor a terceros (sin teselas ni librería de mapas).

## Vistas recompuestas

| Vista (`SectionKey`) | Qué cambió (según el commit) |
|---|---|
| Geocercas (`geofences`) | Centrada en el mapa: el esquema hecho a mano llena un espacio enmarcado (a sangre en móvil) con el formulario al lado, en el orden ubicación → radio → nombre. La zona sin guardar se dibuja en vivo; el radio es un deslizador logarítmico (10 m – 100 km) más un campo exacto, validado contra el techo de la API (100 000 m). Un texto fijo «Avisa al entrar y al salir» sustituye al tipo «Solo salidas» del mockup, que el backend no tiene. |
| Ubicación (`location`) | Se lee como un instrumento de seguimiento: la geocerca que contiene al dispositivo se calcula en el cliente (haversine, la de menor radio que lo contiene) o «Fuera de zonas conocidas», con coordenadas, precisión aproximada, antigüedad y el retraso de ~15 min declarado. La última ubicación se sigue leyendo una vez por montaje y una por «Actualizar», porque cada lectura se audita (`LOCATION_VIEWED`). El mapa de Google embebido queda solo si existe su clave. Debajo, los movimientos recientes entre zonas. |
| Historial (`history`) | Línea de tiempo agrupada por día: fecha grande a la izquierda, hora primero, una línea continua, icono por tipo de evento, nombres de app vía `listDeviceApplications` y «Modo escolar activo» para los bloqueos de `SCHOOL_MODE`. Los días antiguos se pliegan; «Actualizar» mantiene los datos en pantalla mientras recarga. |

`GeofenceMap` se mide con `ResizeObserver`, de modo que una unidad del SVG es un píxel: llena cualquier alto, mantiene
nítidas las etiquetas y dibuja una cuadrícula de distancias redondas, barra de escala y marca de norte. Los colores de
zona son tokens, no hex fijos. Sigue sin teselas ni librería de mapas.

## Decisiones

- **Se omite lo que el backend no tiene**: búsqueda de direcciones, deltas y líneas de ruta.
- **Esquema SVG propio**, nunca teselas externas (decisión vigente desde los sprints 31–38 y regla del plan).
- **Geocercas ya no relee la última ubicación auditada tras cada guardado**, porque guardar una zona no mueve el
  dispositivo (menos entradas de auditoría innecesarias).
- Los campos de latitud y longitud vacíos ahora se rechazan en lugar de enviarse como `0`.

## Tareas

Ruta: no consta en las fuentes; no se inventa.

- [x] T1 — `GeofencePanel` y `GeofenceMap` recompuestos (`8be5abd`).
- [x] T2 — `DeviceLocationPanel` recompuesto, con la geocerca contenedora calculada en el cliente (`8be5abd`).
- [x] T3 — `HistoryPanel` como línea de tiempo por día (`8be5abd`).
- [x] T4 — Defectos de la verificación con datos reales corregidos en el código (`eb2db05`, ver más abajo; sin recaptura).
- [x] T5 — E2E del conjunto S54–S59 en verde, antes de `eb2db05` (`docs/redesign/s59/LEEME.md`, «1 passed, 15,7 s»).
- [ ] T6 — `npm run lint` y `tsc`: **sin registro** para `8be5abd` ni para `eb2db05`.
- [ ] T7 — Revisión con `security-reviewer`: el plan (§4, S57) la exige porque la vista toca ubicación, y las fuentes
  consultadas **no registran** que se haya hecho. Pendiente. (Las lecturas siguen auditándose, según el commit, pero eso
  es la afirmación del autor, no una comprobación.)
- [ ] T8 — Capturas a 1440 y 390 px de cada vista en el repo. Hay `geofences-1440`, `geofences-390`, `location-1440` y
  `history-1440`; faltan `location-390` e `history-390`. Después de `eb2db05` no se recapturó nada.
- [x] T9 — Documentación de seguimiento y evidencia (este archivo y `docs/sprint-57-evidence.md`), escrita en el S60.

## Defectos de esta familia hallados en la verificación del S59 (`LEEME.md`) y su corrección

El commit `eb2db05` declara haber corregido; **no hay recaptura** que lo demuestre.

| Defecto (LEEME) | Corrección declarada en `eb2db05` |
|---|---|
| `geofences-390`: etiquetas de «Colegio San Rafael» y «Última ubicación» superpuestas | «geofence map labels no longer collide on phones» (`GeofenceMap.tsx`) |
| `history`: la hora se parte en dos líneas («10:30 a. / m.») | «history times stay on one line» (`HistoryPanel.module.css`) |
| Coordenadas con espacio tras el signo («- 74.07210») | «coordinates use a true minus sign» |

## Pendiente conocido

- Revisión de seguridad (T7) sin registro.
- Cifras con pie de la serif anterior («04 OCT 2026»: el 6 se leía como 8) y barra de la «e»: el cambio a Newsreader
  (`eb2db05`) apunta a esto, pero **no se comprobó en pantalla real** ni con recaptura.
- La ubicación real de un teléfono no se probó: los datos de ubicación fueron sembrados por la API (4 ubicaciones).
- No se midió el contraste con herramienta.

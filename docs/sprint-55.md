# Sprint 55 — Rediseño editorial del panel web, núcleo: Inicio, Dispositivos, Vinculación

Cuarto sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Código de Cristian del 04/10/2026, commit `c76ceea`
(10:01, sobre el marco del S54). Este documento se escribió **después**, en el S60 (T1), a partir del mensaje y de las
estadísticas de ese commit y de `docs/redesign/s59/LEEME.md`; el sprint no tuvo documento de seguimiento propio mientras
se ejecutó.

## Objetivo

Recomponer las tres vistas de entrada del panel en la dirección editorial, sin la rejilla «tarjetas de métricas arriba,
tarjetas abajo», con datos reales de la API y omitiendo lo que el backend no tiene (regla 2 del plan).

## Vistas recompuestas

| Vista (`SectionKey`) | Qué cambió (según el commit) |
|---|---|
| Inicio (`overview`) | Responde de un vistazo cómo están los dispositivos, qué requiere atención y qué pasó. Las cuatro cifras forman una franja con divisores finos (el uso de hoy es la suma exacta de las categorías, no de `top_apps`, que está limitada a diez). El dispositivo principal es el protagonista; la actividad reciente es una línea de tiempo editorial hecha con eventos reales del historial; los accesos rápidos son una lista corta. |
| Dispositivos (`devices`) | Lista a la izquierda, detalle a la derecha, con los datos, un estado de seguridad derivado del estado real y enlaces a las secciones por dispositivo en lugar de pestañas que la API no puede respaldar. |
| Vinculación (`pairing`) | El código de seis dígitos es el protagonista, con la cuenta atrás real de 180 s (no los 10 minutos del mockup). |

`DashboardShell` solo saluda por el nombre de pila y pasa dos props de navegación.

## Decisiones

- **Se omite lo que el backend no tiene**: % frente a ayer, serie de 7 días, consejo del día, favoritos, vista de mapa y
  el botón «Bloquear».
- **Componentes compartidos que nacen aquí** (el S53 había dicho que nacerían con la primera vista que los necesite):
  `Timeline` (agrupación por día, nodo más nuevo en azul, las props antiguas siguen funcionando), `LoadState`,
  `ReadOnlyField`, `lib/format.ts` y `lib/errors.ts`.
- **Tiempo del código de vinculación**: 180 s, el valor real del backend.

## Tareas

Ruta: no consta en las fuentes (el commit es único y lo firma Cristian); no se inventa.

- [x] T1 — `OverviewPanel` recompuesto (`c76ceea`: `OverviewPanel.tsx` y su CSS).
- [x] T2 — `DevicesPanel` recompuesto (`c76ceea`).
- [x] T3 — `PairingPanel` recompuesto (`c76ceea`).
- [x] T4 — Piezas compartidas: `Timeline`, `LoadState`, `ReadOnlyField`, `lib/format.ts`, `lib/errors.ts`, exportadas
  desde `ui/index.ts` (`c76ceea`).
- [x] T5 — `tsc` y `eslint` en verde: registrado **solo en el mensaje del commit** («tsc and eslint pass»); no hay salida
  guardada.
- [ ] T6 — Capturas a 1440 y 390 px de cada vista en el repo. Hay `overview-1440`, `overview-390`, `devices-1440` y
  `pairing-1440` en `docs/redesign/s59/`; faltan `devices-390` y `pairing-390` (existen en el juego de 34 fuera del
  repo). Ver `docs/sprint-55-evidence.md`.
- [x] T7 — Documentación de seguimiento y evidencia (este archivo y `docs/sprint-55-evidence.md`), escrita en el S60.

## Pendiente conocido

- El commit dice «not yet viewed with real data». La vista con datos reales llegó después, en la verificación integrada
  del S59 (`docs/redesign/s59/LEEME.md`), con capturas previas a las correcciones de `eb2db05`.
- El plan (§4, S55) mencionaba un «`StatusBadge` definitivo» como pieza nacida aquí; el commit no lo menciona (el
  `StatusBadge` se había evolucionado en el S53). No consta que se haya tocado en este sprint.
- Restos que el S53 señaló para S54/S55 (velo del cajón del `Sidebar` con el navy antiguo; discos redondos de
  `MetricCard` y `EmptyState`): ninguna de las fuentes consultadas dice que se hayan resuelto; no se verificó en el código.
- Defectos hallados en la verificación del S59 que tocan estas vistas: ver `docs/sprint-55-evidence.md`.
- La revisión visual con una sesión real de Google la hace el dueño (como en el S54); no se puede simular.

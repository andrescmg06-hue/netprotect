# Sprint 58 — Rediseño editorial del panel web, datos y alertas: Estadísticas, Alertas, Silenciadas

Séptimo sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Código de Cristian del 04/10/2026, commit `7b6b117`
(10:00); correcciones posteriores en `eb2db05`. Documento escrito **después**, en el S60 (T1), a partir del mensaje y de
las estadísticas de los commits y de `docs/redesign/s59/LEEME.md`.

## Objetivo

Que las tres vistas de lectura de datos y alertas sigan la dirección editorial (rojo solo para lo crítico), con datos
reales y sin series que la API no tiene.

## Vistas recompuestas

| Vista (`SectionKey`) | Qué cambió (según el commit) |
|---|---|
| Estadísticas (`statistics`) | Se sustituyen las cuatro tarjetas y la rejilla 2x2 de gráficos por una lectura editorial: una cifra exacta de uso (suma de categorías, porque `top_apps` se detiene en diez), y apps, categorías, bloqueos por motivo y cumplimiento del límite diario en un carril de dos columnas separado por líneas finas. Los resultados van asociados a su petición, de modo que el panel muestra un estado de carga en vez de parpadear «sin datos» mientras carga un periodo. |
| Alertas (`alerts`) | Los contadores por nivel y el filtro por nivel se fusionan en una tira discreta sobre una bandeja maestro-detalle (el detalle se abre bajo su fila en paneles estrechos); «Silenciar» se oculta para alertas ya silenciadas. |
| Silenciadas (`silenced`) | Un libro de registro simple: qué, cuándo vuelve a avisar o «Indefinido», «Reactivar», y un enlace de vuelta a Alertas. |

## Decisiones

- **D2 (colores de nivel)**, en `alertFormatting`: rojo solo para `CRITICA`; `ALTA` ámbar; `ADVERTENCIA` neutra con icono
  ámbar; `INFO` informativa; formas de icono crecientes, para que el color nunca sea lo único que indique el nivel.
- **Se omite lo que la API no tiene**, en Silenciadas: fecha en que se silenció, conteos diarios, filtros de periodo y
  botón de crear. (El plan también preveía omitir series por día/hora y el calendario de silencios.)
- Uso total = suma de categorías, no de `top_apps`.

## Tareas

Ruta: no consta en las fuentes; no se inventa.

- [x] T1 — `StatisticsPanel` recompuesto (`7b6b117`).
- [x] T2 — `AlertsPanel` recompuesto, en modos bandeja y silenciadas (`7b6b117`).
- [x] T3 — Decisión D2 en `lib/alertFormatting.ts` (`7b6b117`).
- [x] T4 — Defectos de la verificación con datos reales corregidos (`eb2db05`): dona con colores distintos y neutro para
  apps sin categoría; tira de niveles de Alertas ya no se corta a 390 px; Alertas reutiliza la lista de dispositivos del
  shell en vez de pedirla otra vez. Hecho en el código; **sin recaptura** que lo muestre en pantalla.
- [x] T5 — E2E del conjunto S54–S59 en verde, antes de `eb2db05` (`docs/redesign/s59/LEEME.md`).
- [ ] T6 — `npm run lint` y `tsc`: **sin registro** para `7b6b117` ni `eb2db05`.
- [ ] T7 — Capturas a 1440 y 390 px de cada vista en el repo. Hay `statistics-1440` y `alerts-1440`; **no hay ninguna de
  `silenced`** y faltan `statistics-390` y `alerts-390`. Después de `eb2db05` no se recapturó nada.
- [x] T8 — Documentación de seguimiento y evidencia (este archivo y `docs/sprint-58-evidence.md`), escrita en el S60.

## Pendiente conocido

- Silenciadas sin captura en el repo. Sí consta que se sembró una alerta silenciada 7 días (TikTok), pero no su vista.
- Defectos del LEEME no mencionados en la lista de `eb2db05`: en la dona, Streaming y Comunicación (verde y verde-azulado)
  casi iguales y, a 390 px, «Redes sociales» truncado («Redes socia…»). No consta que se hayan resuelto.
- No consta que la tira de niveles se haya comprobado de nuevo a 390 px tras la corrección.
- No se midió el contraste con herramienta.

# Sprint 46 — Tutor: Historial, Estadísticas y Alertas

Séptimo sprint del rediseño Android (`docs/android-redesign/sprints/S46-tutor-historial-estadisticas-alertas.md`).
Mockups 8, 9 y 10. Decisión aplicada: D-09 (silencio indefinido con confirmación; quitarlo solo desde la web).

## Qué se hizo

- **Claude — lógica, acciones y enrutado** (`b59f9a6`, `b11d8e2`):
  - `AlertsClient` llama por primera vez a las dos acciones que el panel web ya tenía:
    - `markRead`: `POST …/alerts/{id}/read`, con cuerpo vacío;
    - `silence`: `POST …/alerts/{id}/silence`, con `{"days": null}`.
    Además lee los silencios (`GET …/alert-silences`, solo lectura), para que una alerta silenciada muestre «Silenciada»
    en lugar de un botón que parece no hacer nada. La autorización sigue siendo del backend: una alerta ajena da 404.
  - `AlertsController` (estado simple, D-02):
    - una acción a la vez: un doble toque en «Silenciar» envía una sola petición;
    - silenciar solo tras confirmar;
    - tras cada acción recarga sin vaciar la lista, para que la alerta no se mueva bajo el dedo;
    - si algo falla, el error queda visible con su motivo.
  - `StatisticsController`: si el tutor cambia de periodo rápido, descarta la respuesta tardía del periodo anterior.
  - Funciones puras (13 tests JVM, más 2 al corregir):
    - historial agrupado por el día **en la zona del tutor**: un evento a las 03:00 UTC es «Ayer» en Bogotá;
    - nombres de app sacados de la lista de apps del dispositivo, con el paquete si no aparece;
    - motivos de bloqueo con los textos del web;
    - cumplimiento expresado como días dentro del límite;
    - solo los motivos de bloqueo con conteo mayor que 0, hasta 7;
    - filtros de alertas.
  - `TimelineItem`: la línea vertical sigue la altura de la fila en lugar de medir siempre 48 dp. El hueco se había visto
    en Geocercas (S45).
  - Las seis secciones del dispositivo ya tienen pantalla propia, así que desaparece la rama de respaldo a lo antiguo.
- **DeepSeek** (`docs/delegated/done/sprint-46-historial-alertas.md`, `d6b8686`):
  - las tres pantallas;
  - retirada de las secciones antiguas y de `LegacyDeviceSectionScreen`;
  - 18 tests de UI y 12 estados en la galería.
- **Claude — revisión** (`87a3cd8`, `8ef245c`):
  - «Marcar leída», el texto del web: «Marcar como leída» se partía en dos líneas;
  - sin indicador de carga en ese botón, porque también giraba al silenciar;
  - ancho fijo para la duración, para que las barras terminen alineadas;
  - «< 1 min» en las barras: «menos de 1 min» se cortaba con datos reales;
  - la fila de acciones centrada;
  - fuera de la galería el estado con el diálogo abierto, porque una ventana modal la tapaba entera.

## Cambios visibles para el tutor

- **Alertas:**
  - «Marcar leída» y «Silenciar» funcionan desde el móvil y se reflejan en el panel web;
  - filtros Todas, No leídas, Críticas y Advertencias;
  - nivel con icono, texto y color;
  - «Repetido N veces».
- **Estadísticas:**
  - periodo Hoy, 7 días o 30 días;
  - apps más usadas con barras;
  - bloqueos por motivo real;
  - cumplimiento como «5 de 7 días».
- **Historial:** línea de tiempo por día («Hoy · 29 de septiembre de 2026»), con el nombre de la app y el motivo del
  bloqueo.

## Decisiones (aprobadas por el dueño)

- **«Críticas» = CRITICAL + HIGH:** son las que requieren atención.
- **Mostrar «Silenciada»** leyendo los silencios. Quitarlo sigue siendo solo desde la web (D-09).
- **Nombre de la app** tomado de la lista de apps del dispositivo, en las tres pantallas.
- **Barra de cumplimiento de un solo color:** el mockup la pinta verde o naranja sin un umbral definido.

## Diferencias con los mockups aceptadas

| Mockup | Lo que dibuja | Qué se hizo | Por qué |
|---|---|---|---|
| 9 | «Porcentaje del tiempo permitido usado» y «78 %» | «Días dentro del límite…» y «5 de 7 días» | Es lo que calcula el backend |
| 9 | Tres tarjetas fijas de bloqueos | Solo los motivos reales con conteo | No inventar datos |
| 10 | «Protección… desactivada», «Ubicación actualizada» | Solo los 9 tipos reales | Esos tipos no existen |
| 10 | Descripción larga y flecha en cada alerta | Nada | No hay dato ni pantalla de detalle |
| 8 | Descripción bajo cada geocerca | Solo el título | Repetía el título |

## Pendiente

- Aspecto en el Galaxy S25 FE.
- Los paquetes del sistema sin etiqueta en la lista de apps se muestran con su nombre de paquete: es el dato que hay.

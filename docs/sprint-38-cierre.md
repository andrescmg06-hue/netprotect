# Sprint 38 — Cierre del rediseño del panel web

Segunda mitad del Sprint 38 (la primera, Vista remota y Auditoría, está en `docs/sprint-38.md`):
el pase final que `docs/planning/plan-frontend.md` pide antes de dar el plan por terminado.

## Qué se hizo

### Consistencia y restos del sistema anterior

- **Se eliminó todo el CSS heredado de los Sprints 1–24** de `globals.css` (~300 líneas: `.rulesPanel`,
  `.appList`, `.statusPill`, `.remoteScreenVideo`, …) y los alias de variables (`--surface`,
  `--muted`, …). Antes de borrar se verificó con `grep` que ningún `.tsx` ni CSS Module los leyera;
  el único uso que quedaba (`authError` en `GoogleSignInButton`) pasó a su propio CSS Module, con
  `role="alert"` para que el error de inicio de sesión se anuncie.
- **El reset global de `<button>` era un defecto real, no sólo deuda.** La regla antigua
  `:where(button)` daba a todo botón nativo borde, padding, `width: fit-content` y
  `margin-top: 1rem`. Tiene especificidad cero, así que un CSS Module la pisaba… sólo en las
  propiedades que declaraba. Las filas de la bandeja de Alertas y las tarjetas de política de
  "Política y horario escolar" no declaraban `margin`, y `.option` tampoco `width` ni `color`:
  heredaban 16 px de separación extra y el ancho/color de un botón de los Sprints 1–24. Se
  reemplazó por un reset neutro (sin margen, borde, fondo ni padding; `font`/`color` heredados).
- **`DataTable.onRowClick` se eliminó**: no lo usaba ningún panel, y un `onClick` en un `<tr>` no
  es alcanzable por teclado — era una trampa de accesibilidad esperando a su primer uso.

### Animación

- Cada sección entra con un *fade* + 6 px de desplazamiento (250 ms, sólo `opacity`/`transform`,
  sin *reflow*): `DashboardShell` pone `key={activeSection}` en `<main>`.
- `prefers-reduced-motion: reduce` anula animaciones y transiciones de toda la app (ya existía;
  se amplió con `animation-iteration-count` y `scroll-behavior`).
- Efecto colateral buscado a propósito: el contenido por dispositivo va en un contenedor con
  `key={activeDevice.id}`. Al cambiar de dispositivo el panel anterior se desmonta y su limpieza
  corre — antes, cambiar de dispositivo con la Vista remota abierta dejaba la sesión WebRTC del
  dispositivo anterior transmitiendo bajo la tarjeta del nuevo. La cabecera y su selector quedan
  fuera de ese `key`, así que el foco del teclado sobrevive al cambio.

### Accesibilidad

- **Contraste**: `--color-text-subtle` (#94a3b8, ~2.6:1 sobre blanco) se usaba como color de texto
  real en la hora de cada evento del Historial, la pista de "Cerrar sesión" en Perfil, el pie del
  login y los *placeholders*. Todo pasa a `--color-text-muted` (5.3:1). El token queda sólo para
  elementos decorativos (el chevron de Inicio).
- **`SegmentedControl`** se anunciaba como `radiogroup` sin cumplir su patrón de teclado: ahora
  tiene un único *tab stop* (la opción elegida) y flechas/Inicio/Fin que mueven y seleccionan.
- **Gráficos**: el donut de Estadísticas lleva como nombre accesible sus datos reales ("Uso por
  categoría: Redes sociales 3 h 30 min, …") en vez de un rótulo genérico, y su leyenda (que repite
  lo mismo) se oculta a lectores de pantalla para no leerlo dos veces. Cada barra de cumplimiento
  es un `progressbar` con `aria-valuenow`/`aria-valuetext` ("Sin datos" cuando no hay días
  evaluados).
- **Estado**: la fila seleccionada del maestro-detalle de Alertas lleva `aria-current`; el
  indicador de pasos de Vista remota, `aria-current="step"`, y el mensaje de fin de sesión,
  `role="status"` (las fases de espera ya lo tenían vía `Spinner`).
- Ya cubierto desde sprints anteriores y revisado: foco visible global (`:focus-visible` con
  anillo), `aria-label` en todos los botones de sólo icono del *shell*, iconos decorativos con
  `aria-hidden`.

### Revisión de seguridad

Revisión de toda la rama frente a `main` en lo que toca al frontend, sin hallazgos de confianza
≥ 8/10. Lo revisado y por qué no es un hallazgo:

- Sin `dangerouslySetInnerHTML`, `eval` ni `new Function` en todo `src/`.
- Todo texto controlado por un usuario (nombres de dispositivos, geocercas, paquetes, acciones de
  auditoría) se renderiza como texto de React, escapado — incluido dentro del SVG de geocercas.
- La única ampliación de la CSP en estos sprints es `img-src https://lh3.googleusercontent.com`
  (foto de perfil de Google del tutor), un solo host, y `Avatar` la pide con
  `referrerPolicy="no-referrer"`.
- El iframe y el enlace de Google Maps se construyen sólo con `latitude`/`longitude` numéricos del
  backend (sin cambios desde el Sprint 13); el enlace externo lleva `rel="noreferrer"`.
- Los colores en atributos `style` salen de constantes propias (`chartColors.ts`), nunca de datos.
- La exportación CSV descarga un `Blob` con nombre fijo.

## Qué no se pudo hacer

- **Comparación directa contra los 17 mockups**: las imágenes se entregaron en la conversación y no
  están guardadas en el repositorio, así que el pase de consistencia se hizo contra el plan y la
  coherencia interna del sistema de diseño (mismos tokens, mismas primitivas, mismos patrones de
  tarjeta/tabla/estado vacío en las 16 vistas), no contra cada imagen. Si se quiere esa comparación,
  conviene versionarlas en `docs/design/` y repetirla.

## Verificaciones

Pendientes de completar con su salida real (ver `docs/sprint-38-cierre.md` en el commit de cierre).

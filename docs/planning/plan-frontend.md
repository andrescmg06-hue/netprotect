# Plan — Rediseño del panel web (Sprints 31–38)

## Objetivo

Reemplazar el sistema visual del panel web (hoy: tema oscuro, Arial, CSS plano en un solo
`globals.css`, sin iconos ni gráficos) por un sistema de diseño SaaS claro y consistente, fiel a
los mockups de referencia en **aspecto**, pero representando **sólo lo que el sistema hace de
verdad**. Ninguna ruta de API, WebSocket ni lógica de negocio cambia.

Fuente visual: los 17 mockups entregados el 27/09/2026 y el logo (`frontend/public/brand/`).
Fuente funcional: los componentes actuales de `frontend/src/components/` — cada botón que existe
hoy sigue existiendo; nada nuevo se agrega por verse bien.

## Reglas del plan

1. **Fidelidad visual, no funcional, al mockup.** Donde el mockup muestra algo que el backend no
   tiene (likes/seguidores, capturas, grabar sesión, bloquear dispositivo, historial de códigos,
   plantillas/excepciones de horario, calendario de silencios, sesiones múltiples, exportar
   PDF/Excel, limpiar auditoría, series por día/hora), se omite o se sustituye por el dato real
   equivalente con el mismo estilo de tarjeta.
2. **Sin cambios de backend.** Si una vista necesita un dato que no existe, se deja fuera y se
   anota como backlog; no se inventa ni se simula.
3. **Componentes primero, vistas después.** Ninguna vista estiliza a mano algo que ya exista como
   componente (`Card`, `MetricCard`, `StatusBadge`, `DataTable`…).
4. **La lógica se conserva.** Se reescribe el JSX y el CSS de cada panel; los estados, efectos y
   llamadas de `apiClient.ts` se mueven tal cual (incluido el patrón de `react-hooks/set-state-in-effect`
   de `CLAUDE.md`).
5. **Se navega igual.** El hash `#section=…&device=…` y `DashboardShell` siguen siendo el router.

## Decisiones técnicas

| # | Decisión | Motivo |
|---|---|---|
| T1 | CSS propio con *design tokens* en `:root` + **CSS Modules** por componente. Sin Tailwind. | Next lo trae integrado, cero dependencias, y evita migrar 16 paneles a otra sintaxis. |
| T2 | **Inter** vía `next/font/google`. | Se descarga en build y se sirve desde el propio origen: cumple `font-src 'self'` de la CSP. |
| T3 | **`lucide-react`** para todos los iconos. | Un solo estilo de trazo, SVG inline (sin cambios de CSP). |
| T4 | **Gráficos en SVG propio** (donut, barras horizontales, progreso). Sin librería. | Los datos reales son listas agregadas por periodo (Sprint 16), no series temporales: bastan 3 gráficos simples. |
| T5 | Tema claro únicamente. | Lo pide el brief; el tema oscuro actual se retira. |
| T6 | Buscador del header = **navegación local** (filtra secciones y dispositivos ya cargados). | No hay endpoint de búsqueda; esto es funcional con datos que ya existen. |
| T7 | Campana del header = total de alertas **no leídas** de los dispositivos. Engranaje → "Perfil y sesión". | Mismos endpoints de Alertas; no existe página de configuración. |

## Navegación final (sidebar)

- **Cuenta:** Inicio · Perfil y sesión
- **Dispositivos:** Dispositivos · Vinculación · Apps del dispositivo · Reglas por aplicación ·
  Política y horario escolar · Categorías · Geocercas
- **Monitoreo:** Ubicación · Historial · Estadísticas · Alertas · Silenciadas · Vista remota
- **Auditoría:** Auditoría

"Resumen" pasa a llamarse **Inicio**. "Ubicación" no aparece en el brief pero existe hoy: se
conserva en Monitoreo (regla 1 del brief: no eliminar funcionalidades).

## Definición de terminado (cada sprint)

1. ESLint, `tsc` y `next build` limpios.
2. E2E de Playwright en verde (se actualiza en el mismo sprint si cambia un texto o selector).
3. Cada vista del sprint revisada en el navegador contra su mockup y con datos reales del stack
   de desarrollo; toda acción de la vista probada una vez (crear, editar, borrar, etc.).
4. Sin cambios en `backend/`, `mobile/` ni en contratos de `apiClient.ts`.
5. `docs/sprint-NN.md` + `-evidence.md` cortos (qué cambió, capturas, qué del mockup se omitió).

---

## Sprint 31 — Fundaciones del sistema de diseño

- Tokens en `globals.css`: colores (azul `#246BFE`/`#1456D9`, azul oscuro `#102B63`, fondo
  `#F5F9FF`, verde `#16B364`, rojo `#F04438`, naranja `#F79009`, púrpura `#7A5AF8`, grises),
  radios (8/12/16), sombras suaves, escala de espaciado (4–32), tipografía.
- Inter (`next/font`), `lucide-react`.
- Logo en `public/brand/`: versión completa optimizada (el PNG original pesa 1 MB) y versión solo
  escudo para sidebar colapsado.
- Componentes base en `src/components/ui/`: `Button` (primary/secondary/danger/ghost), `Card`,
  `MetricCard`, `StatusBadge`, `PageHeader`, `EmptyState`, `Input`, `Select`, `TimeInput`,
  `DayPicker`, `SegmentedControl`, `ConfirmDialog`, `Spinner`.
- Se retira el tema oscuro; las vistas existentes siguen funcionando aunque se vean a medio
  camino hasta su sprint.

## Sprint 32 — AppShell: sidebar, header y selector de dispositivo

- `Sidebar`: logo, bloque de perfil + cerrar sesión, 4 grupos con iconos, estado activo azul.
- `Header`: buscador de navegación (T6), campana con no leídas (T7), avatar + nombre.
- `DeviceSelector` como tarjeta (nombre, plataforma, estado) en la cabecera de las vistas por
  dispositivo.
- Responsive: sidebar reducido en tablet, drawer en móvil.
- `DashboardShell` conserva estado, hash y el fetch único de dispositivos; sólo cambia el marco.

## Sprint 33 — Login, Inicio y Perfil

- **Login:** layout de dos columnas del mockup; botón de Google, tarjeta de infraestructura con
  los 5 servicios y "Volver a comprobar". Sin registro ni formularios.
- **Inicio:** estado de infraestructura, métricas reales (dispositivos en línea / total, alertas
  no leídas, bloqueos de hoy, tiempo de uso de hoy — sumados de los endpoints existentes),
  tabla "Dispositivos recientes", accesos rápidos. Sin gráficos de redes sociales.
- **Perfil y sesión:** avatar, nombre, correo, cuenta de Google, zona horaria e idioma del
  navegador, tarjeta "Seguridad de la cuenta" y "Cerrar sesión" como acción de peligro. No se
  muestran fecha de creación ni último acceso (la API no los devuelve).

## Sprint 34 — Dispositivos, Vinculación y Apps del dispositivo

- Componente `DataTable` (filas con icono, badges, acciones, hover, versión tarjeta en móvil).
- **Dispositivos:** lista + panel de detalle del seleccionado; Renombrar, Desvincular (con
  `ConfirmDialog`), Actualizar.
- **Vinculación:** código grande, cuenta atrás con barra, Generar otro / Revocar, guía de 4 pasos.
- **Apps:** métricas (tiempo total, instaladas, desinstaladas), tabla con búsqueda local y
  barra de uso relativo, badge "Desinstalada".

## Sprint 35 — Reglas, Política y horario escolar, Categorías

- Componente `RuleForm` compartido (tipo de regla + minutos / horario + días): hoy el mismo
  formulario está duplicado en `AppRulesPanel` y `DeviceCategoriesPanel`.
- **Reglas:** métricas por tipo, tabla de reglas activas con eliminar, "Crear nueva regla",
  historial de bloqueos.
- **Política:** selector elegante entre los dos modos, toggle de horario escolar con franja y
  días, resumen visual de la franja sobre 24 h.
- **Categorías:** tarjetas de las 11 categorías con nº de apps asignadas y regla actual,
  asignación app→categoría, reglas por categoría.

## Sprint 36 — Ubicación, Geocercas e Historial

- **Ubicación:** última posición, precisión y hora; mapa embebido si hay clave, enlace si no.
- **Geocercas:** formulario, lista, historial de entradas/salidas y aviso de detección
  aproximada (~15 min). **Decisión a confirmar al empezar el sprint:** mapa como elemento
  principal. Opciones: (a) Leaflet + OpenStreetMap — mapa real, pero añade dependencia, cambia la
  CSP y envía coordenadas aproximadas a un servidor de teselas externo; (b) vista esquemática en
  SVG propia con las zonas a escala y la última ubicación, sin servicios externos.
- **Historial:** componente `Timeline` agrupado por día, filtro por tipo (bloqueo / geocerca),
  Actualizar.

## Sprint 37 — Estadísticas, Alertas y Silenciadas

- Componentes `ChartCard`, `DonutChart`, `BarList`, `ProgressBar` (T4).
- **Estadísticas:** Hoy / 7 días / 30 días; donut por categoría, barras de apps más usadas,
  bloqueos por tipo, cumplimiento de límites con barras de progreso.
- **Alertas:** métricas por nivel, pestañas por nivel, lista con icono/nivel/repeticiones,
  panel de detalle; Marcar leída y Silenciar.
- **Silenciadas:** lista con vencimiento y Reactivar; empty state.

## Sprint 38 — Vista remota, Auditoría y cierre

- **Vista remota:** estados (solicitar → esperando → consentimiento → negociando → activa),
  marco de teléfono alrededor del video, duración de la sesión, "Detener", aviso de privacidad.
  Sin acciones remotas extra (no existen, y grabar contradice Sprint 23).
- **Auditoría:** filtros, tabla, paginación de 20, Exportar CSV.
- Pase final: revisión de consistencia vista por vista contra los mockups, responsive,
  animaciones (150–250 ms, `prefers-reduced-motion`), accesibilidad básica (foco visible,
  contraste, `aria-*`), `/security-review`, actualización de README/CLAUDE.md.

## Fuera de alcance

- Rediseño de la app Android (plan propio después de este).
- Todo lo marcado como omitido en la regla 1; si alguno se quiere de verdad, es un sprint de
  backend aparte.
- Sprint 30 del plan TURN sigue pendiente y es independiente de este plan.

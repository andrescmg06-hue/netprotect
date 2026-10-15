# Fase 0 — Informe de inspección y propuesta (Sprint 52)

Estado: **pendiente de aprobación del dueño**. No se modificó ninguna línea de `frontend/`, `backend/` ni `mobile/`.
Rama `sprint-52-rediseno-web-fase-0` (desde `web-redesign`). Fecha: 04/10/2026.

Cómo se hizo: tres exploradores de solo lectura (arquitectura y estilos; componentes repetidos; datos reales frente a
mockups) y verificación propia de los contrastes por cálculo. Toda cita es `ruta:línea` y se puede comprobar. Lo que no
se pudo medir está en §8 como **pendiente**.

## 1. Arquitectura del frontend

- **Tres rutas** (`frontend/src/app`): `page.tsx` es la puerta de autenticación (`DashboardShell` si hay sesión, si no
  `LoginScreen`, `page.tsx:11-19`); `design-system/` (galería, `notFound()` en producción, `page.tsx:8`); `layout.tsx`.
  No hay rutas dinámicas: todo el panel es una sola página cliente.
- **`DashboardShell`** (`components/DashboardShell.tsx`, 333 líneas) es el único dueño del estado compartido: carga de
  dispositivos (`:74-129`), sección y dispositivo activos (`:163-166`), alertas sin leer con sondeo cada 60 s
  (`:31`, `:137-157`) y el cajón móvil. Pinta `Sidebar`, `Header`, el `PageHeader` (`:232-242`, **solo aquí**) y el
  panel de la sección (`:270-327`). `<main key={activeSection}>` remonta la sección para la animación de entrada
  (`:231`); los paneles por dispositivo van en `<div key={activeDevice.id}>` (`:289-294`) para cerrar la sesión
  WebRTC al cambiar de dispositivo.
- **Router por hash** `#section=<key>&device=<uuid>` (`DashboardShell.tsx:42-61`), escrito con `replaceState` (sin
  historial). Las 16 `SectionKey` y sus grupos están en `lib/dashboardSections.ts:31-47`. Cambiar los grupos del
  sidebar es tocar `group` y `SECTION_GROUPS` (`:193`); las claves **no se renombran** (rompería enlaces).
- **Estado**: solo `DashboardShell` por props. No hay store ni contexto salvo `AuthContext`.
- **`AuthContext`** (`contexts/AuthContext.tsx`): token de acceso en memoria, refresh token en `sessionStorage`
  (`:13`); el refresco ocurre **una sola vez al montar** (`:59-85`), sin refresco proactivo ni reintento por 401
  (`apiClient.ts:67-85`). Preexistente y fuera del alcance del rediseño: no se toca.
- **Dependencias**: `next 16.3.8`, `react 19.2.0`, `lucide-react ^1.48.0`, `@playwright/test`. Sin Tailwind, gráficos
  ni mapas. `next.config.ts`: `output: "standalone"` (`:33`), sin clave `images`; CSP en `:14-30`.
- **Tres layouts del shell**: sidebar ≥1200 (270 px), riel 900–1199 (84 px), cajón <900 (300 px). Los cortes
  1199/900/899 están escritos a mano en 5 lugares (`Sidebar.module.css`, `Header.module.css`, `Header.tsx:25-52`,
  `DashboardShell.module.css`, `DESIGN.md`), sin token.

## 2. Sistema de estilos actual

- **`:root`** en `globals.css:3-73`: color (primary `#246bfe`, navy `#102b63`, fondo `#f5f9ff` con degradado), 8 pasos
  de texto (12–32 px), 8 de espaciado (4–40 px), radios 8/10/14/16, sombras `shadow-card`/`shadow-pop`/`focus-ring`,
  3 duraciones. Solo Inter (`layout.tsx:9`); **no hay serif**.
- **`DESIGN.md` vive en la raíz** (no en `frontend/`), 401 líneas, describe «The Calm Blue Frame». **Prohíbe lo que
  el rediseño exige**: «no dark surfaces» (`:393`), «no second typeface» (`:397`), tarjetas de 16 px con sombra. Debe
  reescribirse en el mismo sprint que cambie los tokens (S53).
- **49 módulos CSS**; solo 12 tienen valores fijos (25 hex, 11 `rgba()`, 5 radios en px) frente a 51 usos de
  `var(--radius-*)`. Los paneles están casi limpios, así que **cambiar valores de token mueve las 16 vistas sin tocar su
  JSX** (la estrategia del S53 es viable). Mayores infractores: `login/LoginScreen.module.css` (12, con paleta propia
  `--login-*`, `:14-25`), `shell/Sidebar.module.css` (7), `ui/Button.module.css` (5), `RemoteViewPanel.module.css`
  (4, radios 32 y 20). Fuera de CSS: `lib/chartColors.ts` repite 11 hex (incluido `#246bfe`).
- **Primitivas** (`components/ui/`, barrel de 20 exportaciones): AppIcon, BarList, Button, Card/CardHeader,
  ChartCard, ConfirmDialog, DataTable, DayPicker, DonutChart, EmptyState, Field/Input/Select, Logo,
  MetricCard/MetricGrid, PageHeader, ProgressBar, SegmentedControl, Spinner, StatusBadge, Switch, Timeline.
  Del shell: Sidebar, Header, DeviceSelector, Avatar, `deviceStatus.ts`.

## 3. Componentes repetidos e inconsistencias

15 de 16 paneles usan `Card`; `DevicePolicyPanel` re-declara el CSS de tarjeta y usa h3 propios
(`DevicePolicyPanel.module.css:8-13`). Ocho vistas siguen la rejilla «N métricas arriba, M tarjetas abajo» que el
brief prohíbe: Inicio (4+2), Apps (3+1), Reglas (4+2), Geocercas (1+mapa+2), Ubicación (2+1), Estadísticas (4+2×2),
Alertas (4+2) y Categorías (11 casillas).

**Repetido y candidato a global** (solo lo que se repite ≥3 veces):

| Patrón | Dónde se reimplementa | Propuesta |
|---|---|---|
| Cabecera de tarjeta a ras (`.cardHead`, `.listHead`, `.head`) | Alerts, AppRules, Audit, Categories, Geofence, Overview, Devices, History, Location, ChartCard | Ranura de cabecera en `Card` |
| Estados cargando / vacío / error (`.error` ×13 con 4 paddings, `.emptyWrap` ×6, trío Spinner+EmptyState+`<p>`) | 13 módulos | Un `LoadState` |
| `describeError()` copiada 13 veces | Alerts, AppRules, Audit, Shell, Categories, Location, Policy, Devices, Geofence, History, Pairing, Statistics, Apps | Una función en `lib/` |
| Par etiqueta/valor de solo lectura | `AccountPanel.tsx:12-20` (un `Field` local que choca de nombre con `ui/Field`), `DevicesPanel.tsx:212-244`, `AlertsPanel.tsx:295-316` | `ReadOnlyField` (`<dl>`) |
| Icono redondo teñido por tono | MetricCard, Timeline, AlertsPanel `.rowIcon`, AppIcon | Un `ToneIcon` |
| Fila-botón de lista maestro-detalle | Overview, Devices, Alerts, DeviceSelector | `ListRow` |
| Mapa tipo de regla → tono de `StatusBadge` | `AppRulesPanel.tsx:183`, `DeviceCategoriesPanel.tsx:228,264` (esta sin el tono morado) | `ruleFormatting` |
| Formateadores de fecha y duración | Devices:17 = Overview:14; Overview:19 ≈ Statistics:44; mm:ss en Remote:42 y Pairing:29 | `lib/format.ts` |
| Botón «Actualizar» | 7 paneles (no está en Reglas, Categorías, Apps, Pairing) | Dentro de la barra de filtros |

**Inconsistencias medidas**: niveles de encabezado (h1 → h3, sin h2; el login tiene h2 antes del h1); fechas en 4
formatos; el mismo concepto con tonos distintos («Tiempo de uso» morado en Inicio/Apps pero info en Estadísticas;
«Bloqueos» warning en Inicio pero danger en Estadísticas); cortes de columnas en 900/1024/640/480/520 px; solo
Devices, Pairing y el shell ofrecen «Reintentar»; `StatisticsPanel` muestra «Sin datos» mientras carga; `Actualizar`
en Historial/Estadísticas/Alertas/Auditoría no pone `loading` y deja datos viejos a la vista.

## 4. Assets

| | Hoy (`frontend/public`) | Paquete (`docs/redesign/assets`) |
|---|---|---|
| Logos | `brand/logo-full.png` 720×208 (102 KB), `logo-shield.png` 219×256 (53 KB) | Mismos nombres y dimensiones, **hash distinto** (¿re-exportados o editados?) + `logo-full-on-dark.png` (derivado, sin validar) |
| Login | `login/background-desk-clean.png` 617 KB y `devices-showcase.png` **1467 KB** (se descartan) | `photo-login-study.jpg` 1536×1024, 182 KB |
| Banda de vistas | — | `photo-alpine-banner.jpg` 2048×768, 219 KB |
| Tarjeta de marca / detalle | — | `photo-twilight-valley.jpg` 147 KB, `photo-greenery-soft.jpg` 45 KB |
| Ilustraciones | — | `illus-apps-grid`, `illus-rules-lock`, `illus-shield-phone`: 724–805 KB cada una, 900×822 |

- Las fotos caben en el presupuesto (≤250 KB). **Las 3 ilustraciones no** (≈3× el límite) y su estilo neón/vidrio choca
  con la dirección sobria; el propio `CONTINUAR.md` §6 lo reconoce.
- **Faltan**: isotipo blanco monocromo, SVG limpio del logo, ilustraciones de Auditoría, Silenciadas y Alertas (campana,
  campana tachada, documentos).
- Hay que decidir cuál de los dos juegos de logo es el oficial antes de copiar nada (§9, D5).

## 5. Datos reales frente a mockups (matriz por vista)

Leyenda: **OMITIR** = el mockup lo muestra y la API no lo tiene. **SUSTITUIR** = se usa el dato real equivalente. Cada
fila se verificó contra `apiClient.ts` y los esquemas del backend.

| Vista | Omitir | Sustituir por |
|---|---|---|
| **Login** | Ordenadores flotantes | Corregir «NetProject» → **NetProtect**; foto `photo-login-study` |
| **Inicio** | «% vs. ayer», serie de 7 días, minibarras, «Consejo del día», favoritos, vista Mapa, botón «Bloquear» (no hay endpoint), Ctrl K, Actividad/Bloqueos/Reportes del sidebar | Cifras planas; «Apps más usadas» **sí existe** (`top_apps`, ≤10, una llamada por dispositivo) con `AppIcon` de letra; actividad = `HistoryEvent` + `first_seen_at` + `captured_at`. Ojo: sumar `top_apps` (`OverviewPanel.tsx:61`) subestima el uso; sumar `categories[].total_seconds` es exacto |
| **Dispositivos** | Foto y tipo de dispositivo, «−18% vs. ayer», «+2 nuevas», pestañas sin API | `platform`+`os_version`+`app_version`; ubicación como coordenadas ± precisión y «hace N min» (sin dirección); «Estado de seguridad» derivado de `status==="ALERT"`; zona horaria real (`timezone`) |
| **Vinculación** | Historial de códigos; «válido 10 minutos» | **TTL real 180 s** (`core/config.py:34`), cuenta atrás con `expires_in_seconds`; sin «Google Play» (la app se instala por sideload) |
| **Apps** (sin mockup) | — | Lista/grilla con `package_name`, `app_label`, `latest_usage`, instaladas/desinstaladas; vacío con `illus-apps-grid` |
| **Reglas** (sin mockup) | — | Filas BLOQUEAR · PERMITIR · LÍMITE · HORARIO; **LÍMITE se parte en diario y semanal** (5 tipos reales) |
| **Política y horario** | «Copiar a todos», **filas por día**, plantillas/excepciones. El mockup repite «Jue» dos veces (error) | **Una** `ScheduleBar` de 24 h + `DayPicker` (la API tiene una ventana y una máscara de días, `schemas/device.py:68-87`); ventanas nocturnas válidas; selectores de hora como alternativa de teclado |
| **Categorías** (sin mockup) | — | Las **11** del backend (el brief dice 10); apps por categoría desde las asignaciones |
| **Geocercas** | Teselas, «Buscar dirección», «Solo salidas», deltas, ruta punteada | Esquema SVG propio; lat/lon + «usar última ubicación»; radio hasta 100 000 m (slider + campo); «Entradas/Salidas hoy» filtrando eventos en cliente |
| **Ubicación** | Dirección («Acacías, Meta»), «Actualizar ubicación» como solicitud (solo refetch), selector de fechas, iconos por zona | Nombre de la **zona contenedora** calculado en cliente; coordenadas, precisión, «hace hh:mm»; declarar el retraso de ~15 min |
| **Historial** | Subtítulo con dirección, rango de fechas del servidor, iconos de zona | Timeline por día, filtros Todo/Bloqueos/Geocercas; «Modo escolar activo» = `SCHOOL_MODE`; nombre de app vía `listDeviceApplications` |
| **Estadísticas** (sin mockup) | Series por día/hora, comparación con periodo previo, agregado entre dispositivos | `DonutChart` categorías, `BarList` apps, `ProgressBar` cumplimiento (solo `DAILY_LIMIT`) |
| **Alertas** | — | Contadores por nivel derivados de `alerts[].level`; «Más recientes» ordena en cliente |
| **Silenciadas** | «Hoy/7/30 días», «Silenciadas hoy», **«cuándo se silenció»** (la tabla no guarda fecha de creación), botón «Silenciar una alerta» (solo se silencia desde una alerta existente) | «Cuándo vuelve» = `silenced_until` (nulo = Indefinido); «Reactivar» = `deleteAlertSilence`; enlace a Alertas |
| **Vista remota** | **Batería, Wi-Fi/dBm** (no existen en ningún modelo), «Rotar», «Pantalla completa» (señalización cerrada, sin canal de comandos), «Actualizado hace…» | EN VIVO + temporizador, dispositivo, aviso de consentimiento real; **el `<video>` sigue siempre montado** |
| **Auditoría** | Limpiar, PDF/Excel, selector de dispositivo en cabecera (`perDevice:false`), columna «Usuario» (siempre el propio) | Columnas acción, recurso, fecha, IP; «Dispositivo» solo si `resource_type==="device"`; filtros de fecha **existen en la API** pero hoy no se muestran; CSV real |
| **Perfil** | Editar perfil, foto, **Sesiones activas**, «Cerrar todas», ubicación de sesión; el idioma «Inglés» del mockup es texto fijo | Nombre, correo, foto de Google, zona horaria e idioma **del navegador**, «Cerrar sesión»; una línea «sesión abierta solo en este navegador» |

Contradicciones entre el paquete y el repo que hay que resolver (§9): «rojo solo en crítico» frente a
`alertFormatting.ts:11-16` (HIGH = danger, CRITICAL = purple); «un renglón por día» (`02_DESIGN_TARGET.md:108`) frente
a la API; «10 minutos» frente a 180 s; `GET /devices/{id}/location/history` existe en el backend pero **no** en
`apiClient.ts` (la ruta del mockup exigiría un cliente nuevo, sin tocar el backend).

## 6. Propuesta de design system

**Valores que cambian (mismos nombres)**: `primary #246bfe→#1769ff`, `primary-text →#1456d9`, `navy →#11243d`,
`bg →#f5f3ee`, `bg-gradient` pasa a color plano, `surface-muted →#f8f7f3`, `border →#d9d6cf`,
`border-strong →#8a8678`, `text →#11243d`, `text-muted →#5b6779`, `neutral-soft →#f3f1ec`,
`primary-softer #f3f7ff→#f3f1ec` (**cambio semántico: deja de ser azul**; 20 usos como lavado de hover), radios
`4/6/8/8`, `shadow-card`, `shadow-pop` y `focus-ring`. Sin cambio: estados (success/danger/warning/purple), espaciado
1–10, escala de texto, movimiento.

**Nuevos** (20): `navy-950/900/700`, `sidebar-bg/text/muted/line/active`, `primary-on-dark`, `cream-50/200`,
`space-12/16/24`, `sidebar-width`, `content-max`, `band-height`, `font-serif`, `text-4xl/5xl`. Más: Playfair en
`layout.tsx` (`--font-playfair`), `eyebrow` y `quote`.

**Ajustes que el paquete no trae** (salen de esta inspección):
1. Dos pares fallan contraste y se corrigen (§7): placeholder y el gris del brief sobre crema.
2. Separar `--color-text-subtle` de `--color-border-strong`: hoy el paquete los iguala en `#8a8678`.
3. Un `--focus-ring` para fondo oscuro (el del paquete lleva el hueco crema y se ve mal sobre el sidebar navy).
4. Tokenizar los 5 cortes de layout y los literales de ancho (270/84/300 px, 1440 px, altos 72/64), y reconciliar
   `sidebar-width 264`/`content-max 1240` con los literales actuales.
5. Eliminar duplicados confusos tras el cambio: `primary-softer = neutral-soft = cream-200 (#f3f1ec)`;
   `surface-muted = cream-50`; `navy = text`; `radius-lg = radius-xl`. Propuesta: mantener los nombres antiguos como
   alias de los nuevos y no crear `cream-*` aparte.
6. Sustituir los literales sueltos (`#fff` ×7 archivos, `#b9c9e4` ×3, `rgba(36,107,254,.25)`, `html{background}`,
   `scrollbar-color`, `::selection`, `chartColors.ts`) por tokens.

**Componentes globales** (evolucionan los existentes; **mantienen sus props**): `PageHeader` con banda, `Card`
(panel abierto) + ranura de cabecera, `MetricCard` (cifra con divisores), `StatusBadge`, `Timeline`, `EmptyState` con
ilustración opcional, `Field`, `DataTable`. **Nuevos, solo los que justifica la repetición medida en §3**: `ScheduleBar`,
`LoadState`, `ReadOnlyField`, `ListRow`, `ToneIcon`, `lib/format.ts`. Cualquier otro componente nuevo queda fuera
(regla «menos es más»).

**Orden propuesto** (el del plan): S53 base → S54 Login/Sidebar/Header/banda → S55 Inicio·Dispositivos·Vinculación →
S56 Apps·Reglas·Política·Categorías → S57 Geocercas·Ubicación·Historial → S58 Estadísticas·Alertas·Silenciadas →
S59 Vista remota·Auditoría·Perfil → S60 cierre.

## 7. Riesgos

Contrastes calculados (WCAG, mínimo 4.5:1 texto, 3:1 no texto):

| Par | Razón | Resultado |
|---|---|---|
| Blanco sobre azul `#1769ff` | 4.67 | OK |
| `#11243d` sobre crema `#f5f3ee` | 14.09 | OK |
| `#5b6779` sobre crema / crema oscura | 5.17 / 5.08 | OK |
| `#697586` (gris del brief) sobre crema | **4.22** | **FALLA**; sobre blanco 4.68 OK |
| `#1456d9` sobre crema / blanco | 5.63 / 6.24 | OK |
| Placeholder `#8a8678` sobre blanco / crema | **3.64 / 3.29** | **FALLA** (es texto) |
| Borde de control `#8a8678` sobre crema / blanco | 3.29 / 3.64 | OK (no texto, ≥3) |
| Foco `#1769ff` sobre crema | 4.21 | OK |
| Sidebar: `#e8edf5` / `#9fb0c6` / `#7fa8ff` sobre `#0d1b2a` | 14.80 / 7.87 / 7.41 | OK |
| Activo del sidebar: blanco sobre `#1769ff` | 4.67 | OK |
| Textos de estado sobre su fondo suave (success/danger/warning/purple) | 4.92 / 5.04 / 5.43 / 5.85 | OK |

- **Corrección propuesta**: `--color-text-subtle` → `#6f6b60` (4.80 sobre crema, 5.32 sobre blanco, 4.71 sobre
  `#f3f1ec`), manteniendo `#8a8678` solo para bordes. Texto secundario sobre crema: usar siempre `#5b6779`.
- **Texto sobre la banda fotográfica**: no se midió (depende del recorte y del velo). Pendiente para S54, con
  medición real sobre la imagen.
- **CSP**: sin cambio. `next/font/google` se sirve desde el propio origen (`font-src 'self'`, `next.config.ts:14-30`)
  y las fotos de `public/` entran por `img-src 'self'`. El mapa de Google embebido (`frame-src`) no se toca.
- **Rendimiento de imágenes**: no hay clave `images` en `next.config.ts` y `next/image` solo se usa en `ui/Logo.tsx`
  (con `unoptimized`) y `login/HeroShowcase.tsx`. Las 3 ilustraciones (≈750 KB) exceden el límite de 250 KB;
  `devices-showcase.png` (1467 KB) debe desaparecer.
- **e2e de Playwright** (`frontend/e2e/dashboard.spec.ts`, un único test continuo porque el refresh token es de un
  solo uso, `:6-22`) restringe solo: la navegación `getByRole("navigation", { name: "Secciones del panel" })`
  (`Sidebar.tsx:73`); **exactamente un h1** por vista con el texto de la sección (Inicio, Dispositivos, Vinculación,
  Reglas por aplicación); un botón cuyo nombre accesible contiene el nombre del dispositivo; y el texto
  `com.instagram.android` **una sola vez** en la fila de la regla. No hay pruebas unitarias.
- **Estructuras que el restilizado no debe mover**: `<video>` de Vista remota siempre montado
  (`RemoteViewPanel.tsx:371-387`); efectos con `.then/.catch` y bandera `cancelled` (`react-hooks/set-state-in-effect`);
  identidad estable de `loadRules` y `reloadDevices` (reabrirían el WebSocket); y `DevicePolicyPanel`, que **depende
  del remontaje** del subárbol por dispositivo para re-sembrar su estado local.
- **Cambiar tokens mueve las 16 vistas a la vez** (riesgo alto, ya señalado en el plan). Mitigación: capturas base
  (T8, pendiente) y no tocar JSX de vistas en S53.
- **Entorno local**: `frontend/node_modules` está desactualizado (sin `lucide-react`, `next 16.3.3` en vez de
  `16.3.8`), así que `npm run build` falla aquí hasta correr `npm ci`. Node local 24.14.1; CI usa Node 22. Solo
  verificado vía Docker (el build de la imagen `web` pasó).
- **Sesión**: el acceso de 15 min no se renueva ni reintenta tras 401 (§1). No afecta al diseño, pero sí a probar
  vistas con datos durante mucho rato.

## 8. Capturas base (T8): pendiente

No se tomaron. Requieren backend y una sesión de tutor (`CONTINUAR.md` §3); el login es solo con Google y lo completa
una persona. Opción para S53: la sesión semilla de CI (`backend/scripts/seed_test_session.py` +
`frontend/e2e/global-setup.ts`) con `compose.test.yaml`, deteniendo antes el `backend`/`web` de desarrollo (mismo
puerto). Debe hacerse **antes de cambiar tokens**.

## 9. Decisiones abiertas (requieren al dueño)

- **D1 Serif**: Playfair Display es una aproximación a los mockups (que son imágenes). Confirmar con quien diseñó el
  paquete. Opciones a comparar en la galería antes de fijar: Playfair, Newsreader, Source Serif 4.
- **D2 Rojo de alertas**: ¿CRÍTICA = rojo y ALTA = naranja (lo que dice el paquete), o se mantiene HIGH = rojo y
  CRITICAL = morado (lo que hace el código)? Recomendación: rojo solo en CRÍTICA, ALTA en naranja.
- **D3 Horario escolar**: la API admite una ventana y una máscara de días. Propuesta: una sola `ScheduleBar` + selector
  de días (no un renglón por día).
- **D4 Banda fotográfica en todas las vistas**: el paquete la pone en el `PageHeader` de cada vista; el brief original
  pide fotografía solo donde aporte. Propuesta: banda delgada (168 px) con velo crema y **una sola** imagen reutilizada.
- **D5 Logo**: los PNG del paquete difieren por hash de los que ya están en `public/brand/`. Confirmar cuál es el
  oficial. Los mockups muestran un wordmark serif; `CONTINUAR.md` §6 dice que manda el oficial (sans).
- **D6 Assets faltantes**: isotipo blanco, SVG limpio, ilustraciones de Auditoría/Silenciadas/Alertas. Mientras no
  lleguen: ilustración lineal propia en SVG, pequeña.
- **D7 Ancho del shell**: adoptar `sidebar-width 264` y `content-max 1240` del paquete (hoy 270 y 1440) o conservar
  los actuales.
- **D8 Cliente de ubicación**: añadir en `apiClient.ts` la función para `GET /devices/{id}/location/history`
  (existe en el backend) o prescindir de la ruta del mapa. No toca el backend.
- **D9 Categorías**: 11 reales frente a las 10 del brief; se usan las 11.
- **D10 Orden de vistas**: se mantiene el del plan.

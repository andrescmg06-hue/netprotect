# Plan por sprints — Rediseño editorial del panel web (S52–S60)

Plan de ejecución del paquete `docs/redesign/`. El **qué** (dirección visual, tokens, vistas) está en
`01_PROMPT_CLAUDE_CODE.md`, `02_DESIGN_TARGET.md` y `03_VISTAS.md`; este archivo fija el **cuándo y en qué orden**.
Para arrancar desde cero: `CONTINUAR.md`.

## 1. Punto de partida (verificado el 04/10/2026)

- El último sprint cerrado en el repo es el **S51** (rediseño Android). Este trabajo empieza en el **S52**.
- El panel web **ya tuvo un rediseño** (Sprints 31–39: sistema de diseño, 16 paneles, login del mockup, fuera Tailwind).
  Este es el segundo: `components/ui/` y `components/shell/` existen y **se evolucionan**, no se crean.
- Rama de integración: **`web-redesign`**, creada desde `sprint-51-cierre` (la única con el S50/S51 y la
  numeración al día). Cada sprint sale de ella como `sprint-NN-<slug>` y vuelve con un PR a `web-redesign`.
  `web-redesign` → `main` lo decide el dueño al cerrar el S60.
- Todo el código a tocar está en `frontend/src/`. **No se toca `backend/`, ni rutas de API, ni lógica de negocio.**

## 2. Reglas que gobiernan todos los sprints

1. **Gana el repo sobre el prompt.** Si `01_PROMPT_CLAUDE_CODE.md` contradice `CLAUDE.md` o
   `.claude/rules/frontend.md`, vale el repo. Caso conocido: el prompt dice «no hacer commit»; el repo exige **un
   commit local por unidad de trabajo** en la rama del sprint (skill `work-unit-commits`). Push, merge y PR solo
   los hace quien lo decide (el dueño o quien él indique).
2. **El aspecto sigue al mockup; la funcionalidad, al backend.** Lo que un mockup muestra y la API no tiene, se
   omite o se sustituye por el dato real. Nunca se simula. Cada sprint confirma cada caso contra
   `frontend/src/lib/apiClient.ts` y `docs/planning/plan-frontend.md`.
3. Un CSS Module por componente sobre las variables de `globals.css`; sin Tailwind; primitivas desde
   `@/components/ui`; iconos solo `lucide-react`; fuentes solo `next/font/google`; sin librerías de gráficos ni mapas.
4. `DashboardShell` pinta el `PageHeader`. El hash `#section=…&device=…` sigue siendo el router; `SectionKey` no se renombra.
5. **Nada se da por terminado sin evidencia real.** Cada vista: `npm run lint && npm run build` (agente `verifier`)
   y capturas a **1440 px y 390 px** con Playwright. Lo que no se pueda probar se declara pendiente.
6. Cada sprint deja `docs/sprint-NN.md` (qué y por qué, tareas con casilla, ruta inline/delegada) y
   `docs/sprint-NN-evidence.md` (comandos con salida real, incluidos los fallos), y actualiza `docs/progress.md`.
7. Si un sprint toca auth, realtime, location o audit: pasar por el agente `security-reviewer`.

## 3. Mapa vista → sprint

| Vista (`SectionKey`) | Sprint | Mockup |
|---|---|---|
| Login, Sidebar, Header, banda del `PageHeader` | S54 | 01 |
| Inicio (`overview`), Dispositivos (`devices`), Vinculación (`pairing`) | S55 | 02, 03, 04 |
| Apps (`apps`), Reglas (`rules`), Política y horario (`policy`), Categorías (`categories`) | S56 | 05 (las demás sin mockup) |
| Geocercas (`geofences`), Ubicación (`location`), Historial (`history`) | S57 | 06, 07, 08 |
| Estadísticas (`statistics`), Alertas (`alerts`), Silenciadas (`silenced`) | S58 | 09, 10 (Estadísticas sin mockup) |
| Vista remota (`remote`), Auditoría (`audit`), Perfil (`account`) | S59 | 11, 12, 13 |

## 4. Sprints

### S52 — Fase 0: inspección y propuesta (sin código)
- **Entrega:** `docs/redesign/fase-0-informe.md` con los 7 puntos de la Fase 0 del prompt, más la matriz
  «dato real del backend frente a lo que muestra el mockup» por vista, y capturas base de las 16 vistas + login a
  1440 y 390 px (carpeta `docs/redesign/baseline/`) para detectar regresiones en S53.
- **Salida:** informe aprobado por el dueño. **Es la puerta del resto del plan.**
- **Decisiones que cierra:** tipografía serif definitiva (Playfair es una aproximación), qué assets faltan
  (isotipo blanco monocromo, SVG limpio, ilustraciones de Reglas/Auditoría/Silenciadas), orden de las vistas.
- **Ruta:** exploración delegada (4+ archivos); informe inline. Detalle: `docs/sprint-52.md`.
- **Riesgo:** ninguno (solo lectura). Las capturas base requieren backend y una sesión de tutor
  (ver `CONTINUAR.md` §3).

### S53 — Base visual (sin migrar vistas)
- **Entrega:** valores nuevos de `:root` en `globals.css` (mismos nombres, más los nuevos de `02_DESIGN_TARGET.md`);
  Playfair en `app/layout.tsx` (`--font-playfair`); `DESIGN.md` y galería `/design-system` al día; assets
  optimizados en `public/brand/` y `public/login/` (`next/image`, ≤ 250 KB cada uno); primitivas evolucionadas
  (`Card` → panel abierto, `Button`, `StatusBadge`, `EmptyState` con ilustración opcional, `PageHeader` con banda),
  **manteniendo sus props** para no romper los 16 paneles.
- **Verificación:** lint + build; galería a 1440/390 px; comparación contra `baseline/` del S52.
- **Riesgo alto:** cambiar tokens mueve las 16 vistas a la vez. Mitigación: baseline del S52 y no tocar JSX de vistas.
- **Ruta:** escritor delegado para primitivas; tokens y `layout.tsx` inline (decisión de diseño).

### S54 — Marco de la aplicación
- **Entrega, una por una y con capturas tras cada una:** Login (corregir «NetProject» → NetProtect; foto a sangre;
  «Continuar con Google») → Sidebar (navy, grupos Cuenta · Dispositivos · Monitoreo · Auditoría, activo azul,
  3 layouts: sidebar ≥1200, riel 900–1199, cajón <900) → Header (buscador local · notificaciones · configuración ·
  perfil) → banda del `PageHeader` pintada por `DashboardShell`.
- **Verificación:** los 3 layouts del shell; el login real de Google sigue funcionando (lo prueba una persona).
- **Riesgo:** la búsqueda sigue siendo local (secciones y dispositivos ya cargados); sin «Ctrl K» si no hay atajo hoy.

### S55 — Núcleo: Inicio, Dispositivos, Vinculación
- **Entrega:** `OverviewPanel` (saludo serif, cifras en franja con divisores, dispositivo principal protagonista,
  actividad reciente como `Timeline`, accesos rápidos como lista); `DevicesPanel` (lista a la izquierda, detalle a la
  derecha); `PairingPanel` (el código de 6 dígitos es el protagonista).
- **Omitir/sustituir:** «% vs. ayer», serie de 7 días, «Apps más usadas», «Consejo del día», vista Mapa, historial
  de códigos (no existen en el backend).
- **Componentes globales que nacen aquí:** `Timeline` editorial, `StatusBadge` definitivo.

### S56 — Reglas y horarios
- **Entrega:** Apps (`DeviceApplicationsList`, vacío con `illus-apps-grid`); Reglas (`AppRulesPanel`,
  `RuleTypeFields`: BLOQUEAR · PERMITIR · LÍMITE · HORARIO como filas; vacío con escudo); Política y horario
  (`DevicePolicyPanel` + **`ScheduleBar`** nuevo de 24 h arrastrable, un renglón por día); Categorías
  (`DeviceCategoriesPanel`, las 11 categorías reales del backend).
- **Riesgo:** `ScheduleBar` debe usar **la misma API de política** y conservar los selectores de hora como
  alternativa de teclado (accesibilidad). Sin plantillas ni «copiar a todos» si la API no lo permite.
- **Ruta:** `ScheduleBar` lo hace quien lidera (interacción compleja); Apps/Categorías/Reglas pueden delegarse.

### S57 — Ubicación: Geocercas, Ubicación, Historial
- **Entrega:** `GeofencePanel` + `GeofenceMap` (mapa grande a sangre, formulario integrado, lista debajo; **se
  mantiene el esquema SVG propio**, nunca teselas externas: no se envían coordenadas de un menor a terceros);
  `DeviceLocationPanel` (ubicación actual, precisión y hora junto al mapa; declarar el retraso de ~15 min);
  `HistoryPanel` (timeline real por día con filtros).
- **Seguridad:** toca location → `security-reviewer`. Las consultas de ubicación siguen auditándose (`LOCATION_VIEWED`).

### S58 — Datos y alertas: Estadísticas, Alertas, Silenciadas
- **Entrega:** `StatisticsPanel` (uso, apps top, categorías, bloqueos, cumplimiento; gráficos propios discretos, sin
  mockup); `AlertsPanel` en `inbox` (bandeja + detalle; INFO · ADVERTENCIA · ALTA · CRÍTICA; **rojo solo en
  crítica**) y en `silenced` (minimalista: alerta, cuándo se silenció, cuándo vuelve, reactivar).
- **Omitir:** series por día/hora y calendario de silencios si no hay API. Vacíos: campana y campana tachada.

### S59 — Zonas sensibles: Vista remota, Auditoría, Perfil
- **Entrega:** `RemoteViewPanel` (teléfono grande + EN VIVO, conexión, batería, red, «Finalizar vista remota»;
  aviso de consentimiento visible); `AuditPanel` (registro serio con filtros y CSV; vacío con documentos);
  `AccountPanel` (cuenta, seguridad, sesiones, preferencias).
- **Restricción dura:** el `<video>` de Vista remota debe seguir **siempre montado** (WebRTC). Se verifica con una
  sesión de vista remota real, no por inspección del código.
- **Omitir:** limpiar auditoría, PDF/Excel, edición de perfil y varias sesiones si la API no los tiene (la sesión es
  solo Google).
- **Seguridad:** toca realtime, audit y auth → `security-reviewer`.

### S60 — Cierre
- **Entrega:** contraste AA en todas las vistas, foco visible, `prefers-reduced-motion`, textos de estados vacíos,
  favicon (`logo-shield`), peso de imágenes, e2e de Playwright (`frontend/e2e/dashboard.spec.ts`) en verde,
  `docs/progress.md` y `README.md` (`## Alcance del Sprint N` por cada sprint) al día, `/cerrar-sprint 60`.
- **Pendiente conocido:** assets faltantes (ver S52); si siguen sin llegar, se usan los de `assets/` y se documenta.

## 5. Definición de «vista terminada»

- [ ] Compone con el sistema (no repite la rejilla «4 tarjetas arriba, 2 debajo»); un solo punto de énfasis azul.
- [ ] Toda la funcionalidad previa sigue (estados, efectos y llamadas intactos; patrón `react-hooks/set-state-in-effect`).
- [ ] Sin datos inventados: cada elemento del mockup sin API está omitido o sustituido, y anotado en el sprint.
- [ ] Texto ≥ 4.5:1, bordes de control ≥ 3:1, foco visible, teclado completo.
- [ ] Capturas a 1440 y 390 px (en el móvil se reinterpreta, no se comprime) en `docs/sprint-NN-evidence.md`.
- [ ] `npm run lint && npm run build` en verde vía `verifier`.
- [ ] Un commit por unidad de trabajo, con el porqué.

## 6. Delegación

Implementación acotada de CSS/JSX mecánico → DeepSeek con la skill `delegar-opencode` (encargo en
`docs/delegated/pending/`; quien lidera revisa, verifica y commitea). **No se delega:** decisiones de diseño
(S53 tokens y `layout.tsx`), `ScheduleBar`, Vista remota y cualquier cosa de auth/realtime/location/audit.

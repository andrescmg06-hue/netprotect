# Prompt para Claude Code — Rediseño editorial de NetProtect · Panel del tutor

> Este archivo y su paquete ya viven en el repo (`docs/redesign/`). Para arrancar, ver `CONTINUAR.md`; el reparto en
> sprints está en `PLAN_SPRINTS.md` (Fase 0 = S52, Fase 1 = S53, Fase 2 = S54, Fase 3 = S55–S59, Fase 4 = S60). Pega en Claude Code:
> **«Lee `docs/redesign/01_PROMPT_CLAUDE_CODE.md` y ejecuta únicamente la FASE 0. No toques código hasta que yo apruebe.»**
>
> Donde este prompt diga «no hacer commit», manda el repo: un commit local por unidad de trabajo en la rama del sprint;
> push, merge y PR solo cuando se pidan (`CLAUDE.md`).

---

## Tu rol

Actúa como director de diseño UX/UI senior y diseñador de producto digital para **NetProtect · PANEL DEL TUTOR**, una plataforma de control parental. Experiencia objetivo: **seguridad + control + confianza + acompañamiento**. Debe verse como un producto propio, serio y premium; no como un dashboard SaaS genérico, ni como una plantilla de Tailwind, ni como diseño generado por IA. El nombre correcto es siempre **NetProtect** (nunca «NetProject»; el mockup de login trae ese error).

## Reglas del repositorio que mandan sobre este brief

Estas reglas salen de `CLAUDE.md`, `.claude/rules/frontend.md` y `docs/planning/plan-frontend.md`. Si algo de este prompt las contradice, gana el repo:

1. **El aspecto sigue a los mockups; la funcionalidad sigue al backend.** Si un mockup muestra algo que la API no tiene, se omite o se usa el dato real equivalente. Nunca se simula. Ejemplos en los mockups que hoy **no** existen en el backend: «% vs. ayer», «Tiempo de uso 7 días» como serie diaria, «Consejo del día», «Dispositivos favoritos», Reportes/Actividad/Bloqueos como secciones, Ctrl K global, vista «Mapa» de dispositivos, datos de ejemplo (Tablet de prueba, 12 bloqueos, 4 h 12 min). Revisa `docs/planning/plan-frontend.md` para la lista completa y confirma cada caso contra `lib/apiClient.ts`.
2. **No cambiar rutas de API ni lógica de negocio** en esta tarea. Los estados, efectos y llamadas se mueven tal cual (respeta el patrón de `react-hooks/set-state-in-effect` descrito en `CLAUDE.md`).
3. **Estilos:** un CSS Module por componente sobre las variables de `frontend/src/app/globals.css`. Nada de colores, radios ni espacios escritos a mano. **Sin Tailwind.** Primitivas desde el barrel `@/components/ui`.
4. **Sin librerías de gráficos ni de mapas.** `DonutChart`, `BarList`, `ProgressBar`, `Timeline` y `GeofenceMap` son propios. El mapa de geocercas es un esquema a escala a propósito (no se envían coordenadas de un menor a servidores de teselas). «Mapa protagonista» significa más grande y a sangre, no un mapa externo.
5. **`DashboardShell` pinta el `PageHeader`**; los paneles no.
6. **El hash `#section=…&device=…` sigue siendo el router.** No renombrar `SectionKey`.
7. **Iconos:** solo `lucide-react`. **Fuentes:** `next/font/google` (se sirven desde el propio origen; la CSP exige `font-src 'self'`).
8. **Responsive:** escritorio = experiencia editorial completa; tablet = composición simplificada; móvil = reinterpretar, no comprimir (el shell ya tiene sidebar ≥1200, riel 900–1199, cajón <900).
9. **No hacer commit ni push** sin que se pida. Git siempre con `GIT_OPTIONAL_LOCKS=0`. No leer `.env` ni `secrets/`. No editar skills/agentes `impeccable*` ni `frontend/AGENTS.md`.
10. **Nada se da por terminado sin evidencia real.** Verifica con el agente `verifier` (`npm run lint && npm run build`) y revisa cada vista a 1440 px y 390 px (Playwright). Lo que no puedas probar, decláralo pendiente. Actualiza `docs/progress.md` al cerrar cada fase.

## Dirección visual definitiva

Ver `02_DESIGN_TARGET.md` (tokens, tipografía, espaciado, componentes) y el sistema de diseño publicado. Resumen:

- **Estética:** editorial, premium, sobria, cálida, tecnológica sin ser futurista, humana, funcional y original.
- **Paleta:** navy `#0D1B2A` / `#10233B`; azul de marca `#1769FF` / `#2563EB` **solo como acento** (no domina la interfaz); azul profundo `#17345C`; fondos crema `#F5F3EE` / `#F8F7F3` / `#F3F1EC`; blanco; gris piedra `#D9D6CF`; texto `#11243D`; gris texto `#697586` (solo sobre blanco: sobre crema usar `#5B6779`). Verde solo para estados positivos; rojo solo para alertas, bloqueos y críticos.
- **Tipografía:** serif elegante (Playfair Display como aproximación a los mockups; confirmar) para títulos de página, encabezados importantes y mensajes editoriales; sans (Inter, ya en el repo) para botones, formularios, tablas, navegación, estados, datos y etiquetas. Jerarquía muy clara. Nada geométrico ni futurista.
- **Espacio negativo:** mucho. Márgenes amplios, secciones bien separadas. Que parezca decisión, no falta de contenido.
- **Tarjetas:** reducirlas drásticamente. Prefiere líneas, divisores, bloques editoriales, paneles abiertos, composición asimétrica, listas, timelines y tablas. Prohibido el patrón «4 tarjetas arriba, 2 debajo, 3 debajo». Radios casi cuadrados (4 px por defecto, 8 px tope).
- **Sombras:** muy suaves o ninguna; separa con color, líneas y contraste tonal. Sin «floating UI» en todo.
- **Imágenes:** fotografía solo donde aporta (banda editorial de cada vista y login). Sin stock obvio ni 3D exagerado en cada pantalla.
- **Sidebar:** `#0D1B2A`, texto claro, iconos discretos, mucho espacio vertical, divisores finos, grupos diferenciados; ítem activo azul NetProtect, elegante (no botón gigante).
- **Header:** muy limpio. Izquierda buscador; derecha notificaciones, configuración y perfil. Sin segunda fila de tarjetas.
- **Logo:** no rediseñarlo. Conserva escudo, figura familiar, «NetProtect» y «PANEL DEL TUTOR». Usa los archivos de `assets/logos/`. Consistente en login, sidebar, estados vacíos, vistas y favicon; nunca enorme.
- **UI real vs. assets:** botones, inputs, navegación, tabs, timelines, gráficos, mapas, horarios, tablas, estados y filtros son componentes reales (nunca imágenes). Assets: fotografía de login, logo, ilustraciones de estados vacíos, mockup de teléfono, imágenes promocionales.
- **Principio UX principal:** no rediseñar todas las páginas como la misma página. Todas comparten el sistema visual; cada una aprovecha su naturaleza (ver `03_VISTAS.md`).
- **Menos es más:** antes de añadir un componente, pregúntate si mejora la experiencia; si no, no lo añadas. Si una decisión «bonita» parece plantilla de IA, descártala; prefiere la solución más simple, elegante, clara y propia.

## Procedimiento

### FASE 0 — Inspección y propuesta (sin escribir código)

Entrega un informe breve (en `docs/redesign/fase-0-informe.md`) que cubra:

1. Arquitectura del frontend: rutas, `DashboardShell`, estado compartido, `AuthContext`, dependencias.
2. Sistema de estilos actual (`globals.css`, `DESIGN.md`, CSS Modules, `components/ui/*`) y qué variables usa cada componente.
3. Componentes repetidos e inconsistencias entre los 16 paneles + login.
4. Assets existentes en `public/` (`brand/`, `login/`) frente a los de `assets/` del paquete.
5. Para cada vista: qué datos reales tiene el backend frente a lo que muestra su mockup (lista de omisiones/sustituciones).
6. Propuesta de design system: qué variables de `:root` cambian de valor (sin renombrar), cuáles son nuevas, y qué componentes pasan a ser globales (`PageHeader` con banda, `Timeline`, `ScheduleBar`, `StatusBadge`, `EmptyState` con ilustración, `Field`, `DataTable`).
7. Riesgos (contraste, CSP de fuentes, rendimiento de imágenes, e2e de Playwright existente).

**Detente y espera aprobación.**

### FASE 1 — Base visual (sin migrar vistas)

1. Sustituir los valores de `:root` en `globals.css` según `02_DESIGN_TARGET.md` (mismos nombres de variable; añadir las nuevas).
2. Cargar la serif con `next/font/google` en `app/layout.tsx` (`--font-playfair`) junto a Inter.
3. Actualizar `DESIGN.md` y la galería `app/design-system` para que reflejen los tokens nuevos.
4. Copiar los assets (`logo-full-on-dark.png`, fotografías optimizadas) a `public/brand/` y `public/login/`.
5. Evolucionar las primitivas de `components/ui` (Card → panel abierto con línea, Button, StatusBadge, EmptyState con ilustración opcional, PageHeader con banda). Mantener sus props para no romper los 16 paneles.
6. Verificar: lint, build y galería a 1440 y 390 px. **Detente y muestra capturas.**

### FASE 2 — Marco de la aplicación

Migrar en este orden, **una por una**, con verificación y capturas después de cada una: **Login → Sidebar → Header**.

### FASE 3 — Vistas (una por entrega; no todo de golpe)

Orden: Inicio · Dispositivos · Vinculación · Apps · Reglas · Política y horario · Categorías · Geocercas · Ubicación · Historial · Estadísticas · Alertas · Silenciadas · Vista remota · Auditoría · Perfil.

Para cada una: lee su mockup en `assets/mockups/`, compara con los datos reales, conserva toda la funcionalidad existente, reescribe JSX y CSS con los componentes globales, verifica a 1440 y 390 px, actualiza `docs/progress.md`.

### FASE 4 — Cierre

Revisión de contraste (AA), foco visible, `prefers-reduced-motion`, textos de estados vacíos, favicon, peso de imágenes, e2e de Playwright, `security-reviewer` si se tocó auth/realtime/location/audit. Cierra con `/cerrar-sprint NN` si aplica.

## Resultado esperado

NetProtect debe sentirse como **«un producto propio, serio y premium para acompañar y proteger el uso digital»** y no como «otro dashboard administrativo con tarjetas azules»: sobriedad, elegancia, claridad, personalidad, confianza, control, acompañamiento. **Menos elementos. Mejores elementos. Mejor composición.**

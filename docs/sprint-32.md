# Sprint 32 — AppShell: sidebar, header y selector de dispositivo

Segundo sprint de `docs/planning/plan-frontend.md`. Reemplaza el marco de todo el panel; las
vistas de adentro siguen siendo las de antes hasta su propio sprint (33–38).

Primer sprint hecho con el skill `impeccable`: `PRODUCT.md` (init), brief de superficie con
contrato de dirección en `.impeccable/surfaces/`, piso de calidad, detector, revisión final
independiente y `DESIGN.md` escrito desde lo construido.

## Qué se hizo

- **`Sidebar`** (`components/shell/`): logo, perfil con avatar y "Cerrar sesión", navegación por
  grupos con iconos Lucide (Inicio · Cuenta · Dispositivos · Monitoreo · Auditoría), ítem activo
  azul, contador de alertas sin leer. Tres formas con el mismo marcado: completo (≥1200 px), riel
  de iconos (900–1199 px, los textos solo se ocultan visualmente) y cajón (<900 px).
- **`Header`**: buscador de navegación local (secciones y dispositivos ya cargados; flechas, Enter,
  Escape), campana con el total real de alertas sin leer, engranaje → Perfil, menú de cuenta.
- **`DeviceSelector`**: tarjeta con nombre, plataforma y estado sobre un `<select>` nativo
  transparente (teclado, lector de pantalla y selector del teléfono sin reimplementarlos).
- **`PageHeader`** con título y descripción real de cada sección (`dashboardSections.ts` ganó
  `icon` y `description`; las claves no cambian, los enlaces `#section=` siguen funcionando).
- **CSP:** `img-src` incluye `lh3.googleusercontent.com`; el avatar de Google no se veía en ningún
  entorno (encontrado en el Sprint 31). Si falta la foto, se muestran iniciales.
- **E2E:** localiza por rol (`navigation`, `heading`) en vez de clases CSS; "Resumen" → "Inicio".

## Decisiones

- **Buscador honesto.** El mockup dice "Buscar dispositivos, apps, reglas…", pero no existe
  endpoint de búsqueda: el texto dice "Buscar secciones o dispositivos…" y hace exactamente eso.
- **Alertas sin leer por dispositivo.** No hay endpoint de cuenta; se consulta
  `GET /devices/{id}/alerts` por dispositivo, cada minuto y al entrar o salir de Alertas. Sin
  cambios en el backend.
- **En vistas de dispositivo la tarjeta reemplaza a la ruta de navegación**, como en los mockups.
- **Sin punto rojo permanente, sin foto de teléfono, sin flecha con un solo dispositivo**: el
  mockup los muestra siempre; aquí solo aparecen cuando son ciertos.
- **Ubicación** se mantiene, en Monitoreo.

## Pendiente para los sprints siguientes

Las 16 vistas internas conservan su aspecto transitorio (tarjeta blanca con el CSS heredado).

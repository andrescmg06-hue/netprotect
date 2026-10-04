# Sprint 54 — Rediseño editorial del panel web, marco de la aplicación

Tercer sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Rama `sprint-54-rediseno-web-marco`, creada desde
`sprint-53-rediseno-web-base-visual`. Ejecutado en modo autónomo por pedido del dueño (04/10/2026).

## Objetivo

Que el marco que rodea a las 16 vistas ya sea el nuevo: login, sidebar, header y la banda fotográfica del encabezado.
Las vistas en sí no se recomponen todavía (S55–S59).

## Decisiones

- **Logo (D5)**: los PNG del paquete y los de `public/brand/` son visualmente idénticos (el hash distinto es solo
  re-exportación). Manda el oficial. `logo-full-on-dark.png` se comprobó compuesto sobre `#0d1b2a`: legible, sirve para
  el sidebar navy.
- **Serif (D1)**: cambiada de Playfair Display a **Newsreader**. Primero se mantuvo Playfair tras compararla a 1× con
  Newsreader, Source Serif 4, Libre Caslon Text y Fraunces; pero la verificación con datos reales (S59) mostró, en tres
  mediciones independientes, que sus trazos finos hacen desaparecer la barra de la «e» en pantallas de 1×: «Geocercas»
  se lee «Gcoccrcas». Newsreader conserva el trazo. Sigue abierta para el diseñador.
- **Login**: el mockup dibuja un wordmark serif propio; se usa el logo oficial (`CONTINUAR.md` §6). El nombre es siempre
  NetProtect.
- **Insignia «1 Issue» de `next dev`**: confirmado que la causa es la CSP sin `unsafe-eval` (error de consola de React en
  desarrollo). No existe en producción y no se toca la CSP.

## Tareas

- [x] T1 — Login: panel crema con logo oficial, «Inicia sesión» en serif, «Continuar con Google»; fotografía del estudio a
  sangre (banner en móvil); solo tokens; se retiran `HeroShowcase`, `BackgroundScene`, `LoginCard`, `LoginFooter`,
  `LoginLogo` y las dos imágenes viejas (2 MB). Inline. Verificado: `tsc` y `eslint` en verde; render a 1440 y 390 px.
- [x] T2 — Sidebar navy: grupos con etiqueta, divisores finos, ítem activo azul, logo para fondo oscuro, 3 layouts.
  Delegado (escritor acotado; mismo trabajo que T3 y T4, una sola superficie del shell). El escritor movió el bloque de
  usuario y «Cerrar sesión» al pie del sidebar (como en los mockups) y no añadió la tarjeta de foto del pie: con 16
  secciones empujaría el sidebar por encima del alto de pantalla.
- [x] T3 — Header: buscador a la izquierda; notificaciones, configuración y perfil a la derecha. Delegado (solo CSS).
- [x] T4 — Banda del `PageHeader` encendida desde `DashboardShell`, sin frases inventadas; a ≤720 px el selector de
  dispositivo queda compacto debajo del título. Delegado.
- [ ] T5 — Verificación: `tsc` y `eslint` en verde (verificados por quien lidera); ningún `*Panel.tsx` cambió; imagen
  `web` reconstruida y sirviendo el login nuevo (200). **Pendiente: revisión visual del shell con sesión** (la hace el
  dueño al iniciar sesión con Google; capturas sembradas después).
- [ ] T6 — Cierre: `docs/sprint-54-evidence.md`, README, `docs/progress.md`.

## Pendiente conocido

- El login real con Google lo prueba una persona (el dueño).
- Commits: el dueño rechazó el commit del login durante la ejecución; los cambios quedan en el *stage* hasta que lo pida.

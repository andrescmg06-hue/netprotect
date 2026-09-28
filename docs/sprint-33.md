# Sprint 33 — Login, Inicio y Perfil

Tercer sprint de `docs/planning/plan-frontend.md`. Primer sprint sobre pantallas concretas
(el 31 y 32 fueron fundaciones y marco); usa los componentes y tokens que ya existen.

## Qué se hizo

- **Login** (`LoginHero` + `LoginPanel`, nuevos): layout de dos columnas — a la izquierda el
  encabezado y la comprobación de infraestructura (misma lógica del Sprint 1, solo rediseñada);
  a la derecha una tarjeta con el logo y el botón de Google. `page.tsx` conserva exactamente el
  mismo `fetch`/reintento/estado; solo cambió el JSX que lo muestra. El botón de Google sigue
  siendo el que renderiza la propia librería de Google (no se reestiliza un iframe de un tercero).
- **Inicio** (`OverviewPanel`, reescrito): cuatro métricas reales — dispositivos en línea/total,
  alertas sin leer (mismo cálculo que ya hacía `DashboardShell` para la campana del header, ahora
  recibido por prop en vez de recalculado), bloqueos de hoy y tiempo de uso de hoy, estas dos
  últimas sumando `GET /devices/{id}/statistics?period=today` de cada dispositivo — mismo endpoint
  del Sprint 16, sin tabla ni agregación nueva. Lista de dispositivos con acceso directo a
  "Dispositivos" o a "Alertas" si tiene sin leer, y accesos rápidos a Vinculación/Reglas/Alertas.
- **Perfil y sesión** (`AccountPanel`, reescrito): avatar, nombre, correo, tipo de cuenta, zona
  horaria e idioma. Estos dos últimos salen del navegador (`Intl`/`navigator.language`), no de la
  API — se leen después de montar el componente para que el servidor y el cliente rendericen lo
  mismo en el primer pintado (si no, React marca un error de hidratación). Tarjeta de seguridad y
  "Cerrar sesión" como acción de peligro.
- **Limpieza:** `globals.css` pierde las clases exclusivas de la pantalla de login anterior
  (`.shell`, `.hero`, `.eyebrow`, `.lead`, `.statusCard`, `.dot`, `.authCard` y sus hijos, `.grid`/
  `article`) — confirmado que ningún archivo `.tsx` las seguía usando antes de borrarlas.

## Decisiones

- **`DashboardShell` sigue poniendo el título y la descripción de cada sección** (`PageHeader`
  compartido, del Sprint 32). `AccountPanel`/`OverviewPanel` no traen uno propio — lo tenían al
  principio y se quitó al notar que salía duplicado.
- **Sin fecha de creación ni último acceso en Perfil**: la API (`CurrentUser`) no los devuelve;
  mostrarlos habría sido inventar el dato.
- **Sin "editar nombre" ni foto de perfil**: no existe endpoint para cambiar ninguno de los dos.
- **Sin gráficos de redes sociales en Inicio**: los del mockup (visitas, likes, seguidores) no
  corresponden a nada que este sistema mida.

## Verificaciones

`impeccable detect --json` sobre los 10 archivos del sprint: sin hallazgos. ESLint, `tsc`,
`next build` y el E2E de Playwright (contra `compose.test.yaml`, con `migrate` corrido antes esta
vez) en verde. Capturas reales a 1440 y 390 px de las tres pantallas, autenticado con una sesión
sembrada; sin desbordamiento horizontal en móvil (`scrollWidth` 390). Un ajuste de un lote: la
tarjeta de estado del login apilaba mal el botón junto al texto en móvil, corregido pasándola a
columna por debajo de 640 px.

## Tropiezo

Repetí el error que `CLAUDE.md` ya documenta: levanté `db`/`api_server` de `compose.test.yaml`
sin correr `migrate` antes (esa base no tiene volumen persistente), y el script de siembra falló
con `relation "users" does not exist`. Corregido corriendo `migrate` antes de sembrar.

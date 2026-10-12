# Sprint 44 — Tutor: navegación, Inicio, Dispositivos, Detalle y Más

Quinto sprint del rediseño Android (`docs/android-redesign/sprints/S44-tutor-inicio-y-detalle.md`). Mockups 3 y 4.
Decisiones aplicadas: D-01 (rutas propias), D-02 (sin ViewModel), D-08 (cuatro pestañas).

## Qué se hizo

- **Claude — estructura y lógica** (`673465f`):
  - `TutorShell` + `TutorRoute`: pila de rutas bajo una barra inferior Inicio · Dispositivos · Actividad · Más; el
    Detalle y sus 6 secciones se apilan encima de la pestaña; la pestaña marcada es la raíz de la pila; "atrás" sube un
    nivel, después vuelve a Inicio y desde Inicio sale de la app; la pila sobrevive a la rotación.
  - `TutorScreen.kt` (963 líneas, todo en una columna) desaparece. Sus 7 secciones (apps, ubicación, geocercas,
    historial, estadísticas, alertas, auditoría) pasan **sin reescribirse** a `legacy/LegacySections.kt`, con la misma
    carga que hacían al desplegarse y su aspecto oscuro, hasta S45–S47.
  - `TutorHomeController` y `DeviceDetailController` (estado simple, D-02) con 14 tests JVM: cuenta atrás del código a
    partir del tiempo transcurrido con reloj monótono (no un contador que se desvía); un "Revocar" fallido lo dice en
    vez de esconder el código; nombre válido = 1–255 caracteres sin espacios sobrantes (regla del backend); un 404 es
    "ya no existe o no tienes acceso".
  - `DeviceClient` lee `os_version`/`app_version` (ya venían del backend) y puede cargar un dispositivo (`getDevice`).
  - `LoadState` compartido (`Loading` / `Loaded` / `Failed(message, notFound)`).
  - Pantallas provisionales pero completas en ese commit, para que ninguna función se perdiera entre commits.
- **DeepSeek** (`docs/delegated/done/sprint-44-tutor-inicio.md`, `8f0a2cf`): las cuatro pantallas con el diseño, la lista
  de dispositivos compartida (`DeviceListItem`), 10 estados nuevos en la galería de pantallas y 18 tests de UI.
- **Claude — revisión** (`6a169a8`): la zona de peligro del Detalle con el fondo rosado del mockup (`NpCard` acepta color
  de fondo y borde); el parámetro que abre el diálogo de desvincular se llama `onAskUnlink` (antes `onConfirmUnlink`, que
  sonaba a desvincular); "Los datos se sincronizan periódicamente." en peso normal; el vacío de la lista ya no dice "desde
  Inicio" estando en Inicio.

## Cambios visibles para el tutor

- Barra inferior y pantallas separadas en lugar de una sola columna con botones "Ver/Ocultar".
- **Desvincular pide confirmación** ("¿Desvincular {nombre}?"). Antes borraba al primer toque (G-11, inventario T-12).
- Renombrar valida el nombre y muestra el error; Revocar muestra el error si falla (antes se silenciaban).
- Cuenta atrás real del código ("Vence en 2:57") y estado "El código venció" (antes "Válido por 3 minutos" fijo).
- "Android {versión}" leída del backend y "Última actividad: hace N min" en cada dispositivo.
- Pestaña **Más**: Cambiar de modo, Cerrar sesión y "Acerca de NetProtect" con la versión y la frase de privacidad
  aprobada por el dueño.

## Decisiones

- **Qué sobrevive a la rotación**: la pila de pantallas sí; los datos cargados y un código de vinculación activo no (se
  recargan, D-02). Un código que desaparece al rotar sigue válido en el backend hasta que vence o se genera otro.
- Las secciones antiguas conservan su fondo oscuro dentro de la app clara: es transitorio y más legible que el texto
  blanco sobre fondo claro.
- Frase de "Acerca de": "NetProtect solo muestra lo que el dispositivo supervisado comparte con tu cuenta. La ubicación es
  aproximada, nada se graba y cada acción queda en tu registro de actividad." (aprobada por el dueño).

## Diferencias con los mockups aceptadas

- Sin miniatura de tablet: icono genérico de dispositivo (la API no distingue tablet de teléfono).
- Una sola cabecera del dispositivo en el Detalle (el mockup la repite).
- El avatar de Inicio es solo la inicial, sin menú desplegable (no hay a dónde llevarlo).

## Pendiente

- Secciones del dispositivo y "Actividad" con el aspecto antiguo hasta S45–S47.
- `HomeScreen` conserva `Color(0xFF090B10)` para el modo supervisado (S48).
- Comprobación en el Galaxy S25 FE: todo se revisó en el emulador.

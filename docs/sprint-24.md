# Sprint 24 — Panel web completo

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 23) pide: "Las 16 secciones del dashboard sobre la misma
API y la misma cuenta del tutor." El documento de 48 secciones que originalmente nombraba esas 16
no está en este repo (mismo caso que las 11 categorías del Sprint 10 o las 5 señales de
manipulación del Sprint 20): la funcionalidad ya existía — nueve componentes distintos vivían
detrás de botones "mostrar/ocultar" apilados dentro de cada fila de `DevicesPanel`, sin una
navegación real ni un dispositivo activo compartido — pero nada la organizaba como un dashboard.
Este sprint es casi enteramente de panel web: la única pieza de backend que se toca es exponerle a
la web un endpoint que ya existía desde el Sprint 5 (generar/revocar un código de vinculación),
hasta ahora sólo alcanzable desde Android.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-071 | Como tutor, quiero navegar el panel por secciones claras (no una lista de botones "mostrar/ocultar" apilados) para encontrar lo que busco sin desplazarme por todo lo demás. |
| HU-072 | Como tutor con varios dispositivos, quiero elegir cuál estoy viendo desde un único selector que se mantenga al cambiar de sección, en vez de repetir cada panel por cada fila de dispositivo. |
| HU-073 | Como tutor, quiero generar y revocar un código de vinculación desde el panel web, sin depender de tener la app Android a mano. |
| HU-074 | Como tutor, quiero un enlace directo a una sección concreta (compartible, sobrevive a recargar la página) en vez de perder mi lugar cada vez que refresco. |

## Criterios de aceptación

1. El panel autenticado presenta 16 secciones de navegación agrupadas (Cuenta, Dispositivos,
   Control, Contexto, Seguridad), cada una montando un componente real — ninguna es una pantalla
   vacía de relleno.
2. Las secciones que dependen de un dispositivo (`perDevice`) comparten un único selector de
   "dispositivo activo" en el encabezado; cambiarlo no pierde la sección en la que se está.
3. La URL refleja la sección y el dispositivo activos (`#section=…&device=…`) y navegarla
   directamente — recargar, pegar el enlace, usar atrás/adelante del navegador — lleva a esa misma
   vista.
4. Generar un código de vinculación desde la web (`POST /pairing/codes`) funciona igual que desde
   Android: 6 dígitos, cuenta regresiva de 3 minutos, un botón "Revocar" que invoca
   `DELETE /pairing/codes/current`.
5. Ni `npm run lint` ni `npm run build` (Next 16 + TypeScript estricto) producen error ni warning.
6. Ningún componente reescribe lógica que ya existía y funcionaba: los nueve paneles por
   dispositivo del Sprint 6-23 se reutilizan tal cual o se dividen sin cambiar su comportamiento
   observable (ver "Decisiones de diseño").

## Decisiones de diseño y su motivo

### Qué son las 16 secciones, y por qué ese número no se forzó

La lista final (`frontend/src/lib/dashboardSections.ts`) es:

| Grupo | Secciones |
|---|---|
| Cuenta | Perfil y sesión |
| Dispositivos | Resumen · Dispositivos · Vinculación |
| Control | Aplicaciones instaladas · Reglas por aplicación · Política y horario escolar · Categorías |
| Contexto | Ubicación · Geocercas · Historial · Estadísticas |
| Seguridad | Alertas · Silenciadas · Vista remota · Auditoría |

Trece de esas dieciséis ya eran un componente propio desde su sprint original (Perfil viene del
`authCard` que ya vivía en `page.tsx`; Resumen y Vinculación son las únicas pantallas
verdaderamente nuevas de este sprint). Las tres restantes — Reglas/Política y Alertas/Silenciadas —
existían fusionadas en un componente cada una; se separaron porque son dos tareas distintas de un
tutor (ajustar el modo del dispositivo no es lo mismo que gestionar reglas app por app; revisar la
bandeja no es lo mismo que administrar qué se silencia), no para completar una cuenta. Si el
enunciado original nombra 16 secciones distintas a estas, esta lista queda documentada como
decisión propia — igual que las categorías del Sprint 10 — a corregir cuando se tenga ese
documento.

### `AppRulesPanel` + `DevicePolicyPanel`, no `DeviceRulesPanel`

El componente del Sprint 8/9/12 mezclaba dos cosas con ciclos de vida distintos: la política por
defecto y el horario escolar son 1-2 toggles de configuración del dispositivo (sus datos ya venían
como props del `Device` que `DevicesPanel` había cargado); las reglas por app son una lista con su
propio CRUD y su propio fetch. Se dividió en dos archivos sin tocar un solo endpoint ni cambiar el
payload de ninguna llamada — sólo qué JSX vive en cuál componente. El WebSocket de
`rules_changed` (Sprint 18) que ambos necesitan para quedarse en vivo se extrajo a
`useDeviceRulesRealtime` (`frontend/src/lib/`) en vez de duplicar el `useEffect` del socket dos
veces; como la navegación del dashboard sólo muestra una sección a la vez, nunca hay dos sockets
abiertos simultáneamente para el mismo dispositivo.

### `AlertsPanel` gana un prop `view`, no dos componentes

Al contrario que las reglas, separar alertas de silenciadas en dos componentes habría duplicado el
único fetch (`Promise.all([listDeviceAlerts, listAlertSilences])`) que además necesitan compartir:
silenciar una alerta actualiza el estado de ambas listas a la vez. `view: "inbox" | "silenced"`
sólo decide qué mitad del mismo estado se pinta.

### Vinculación desde la web: mismo endpoint de Sprint 5, sin cambios de backend

`POST /pairing/codes` / `DELETE /pairing/codes/current` (`backend/app/api/v1/endpoints/pairing.py`)
existen sin cambios desde el Sprint 5 — sólo exigen rol `TUTOR`, que ya se concede sin fricción al
entrar al panel. `PairingPanel` es el único componente nuevo con estado propio no trivial: una
cuenta regresiva puramente de cliente (`setInterval`, sin llamada de red) que refleja
`expires_in_seconds`; el backend sigue siendo la única fuente de verdad sobre si el código todavía
es canjeable — un código que llega a "0:00" en pantalla no invalida nada por sí mismo, sólo dice
"probablemente ya expiró, genera otro".

### Navegación por hash de URL, no rutas dinámicas de Next

`DashboardShell` guarda la sección y el dispositivo activos en `#section=…&device=…`
(`window.history.replaceState` al navegar, un listener de `hashchange` para atrás/adelante del
navegador y enlaces pegados) en vez de `useSearchParams`/rutas `/dashboard/[section]` del App
Router. Un componente cliente que usa `useSearchParams` exige un límite `<Suspense>` para el
prerenderizado estático de Next 16; como todo este panel ya vive detrás de un *gate* de
autenticación 100% cliente (nunca se sirve contenido sin sesión desde el servidor), esa garantía no
aporta nada aquí y sí añade una capa de complejidad. El costo aceptado: el enlace no es indexable
por un buscador — irrelevante para un panel privado tras login.

### Estado del dispositivo activo: derivado en el render, no sincronizado por un efecto

La primera versión mantenía `activeDeviceId` en un `useState` propio, actualizado por un
`useEffect` que reaccionaba a `devicesState` (para caer al primer dispositivo cuando la lista
carga, o soltar una selección que ya no existe tras desvincular). ESLint (`react-hooks/
set-state-in-effect`, ver nota del Sprint 3 en `CLAUDE.md` sobre reglas de hooks nuevas en esta
versión de Next) lo rechazó: un `setState` síncrono en el cuerpo de un efecto — no dentro de un
callback de promesa — dispara error aunque el efecto no haga ninguna llamada de red. Se resolvió
sin el efecto: `selectedDeviceId` guarda sólo la elección explícita del usuario; el dispositivo
*efectivo* (con la caída al primero de la lista) se calcula en cada render a partir de
`selectedDeviceId` y `devices`, sin estado ni efecto adicional — "you might not need an effect",
el propio consejo que da el mensaje de la regla.

## Hallazgo corregido durante la verificación: mismatch de hidratación en `AuthContext`

Al verificar visualmente el dashboard con un tutor ya autenticado (ver evidencia), la consola del
navegador mostraba `Uncaught Error: Hydration failed`. Causa real, preexistente desde el Sprint 3,
no introducida en este sprint: el `useState` inicial de `AuthProvider` decidía su valor con
`typeof window !== "undefined" && readStoredRefreshToken() ? "loading" : "unauthenticated"` —
en el servidor `typeof window` siempre es `"undefined"`, así que el HTML pre-renderizado siempre
asumía `"unauthenticated"`; en el cliente, cualquier tutor que volviera con una sesión guardada en
`sessionStorage` hidrataba con `"loading"`, un árbol distinto al que el servidor había enviado. No
era un artefacto de la prueba (inyectar el token antes de cargar cualquier script) — es lo que le
pasaría a cualquier tutor real que recargara la página con sesión activa.

Corregido dejando el estado inicial en una constante (`"loading"`) igual en servidor y cliente, y
moviendo la decisión completa —incluido el caso "no hay token guardado" → `"unauthenticated"`— al
único `useEffect` que ya existía, como parte de la misma cadena `.then()/.catch()` (un token
ausente se pliega en la misma rama de error que un refresh fallido, vía
`Promise.reject(new Error(...))`) en vez de un `setState` síncrono en el cuerpo del efecto — otra
vez la misma regla de hooks de arriba. Ver `docs/sprint-24-evidence.md` para la comparación antes/
después.

## Backend

Ningún modelo, migración ni endpoint nuevo. Cero archivos tocados bajo `backend/`.

## Web

- `frontend/src/lib/dashboardSections.ts` (nuevo): catálogo de las 16 secciones.
- `frontend/src/lib/useDeviceRulesRealtime.ts` (nuevo): hook del WebSocket `rules_changed`
  extraído de `DeviceRulesPanel`.
- `frontend/src/components/DashboardShell.tsx` (nuevo): la navegación real — sidebar agrupado,
  selector de dispositivo activo, hash de URL.
- `frontend/src/components/AccountPanel.tsx`, `OverviewPanel.tsx`, `PairingPanel.tsx` (nuevos).
- `frontend/src/components/AppRulesPanel.tsx`, `DevicePolicyPanel.tsx` (nuevos, reemplazan a
  `DeviceRulesPanel.tsx`, eliminado).
- `frontend/src/components/AlertsPanel.tsx`: prop `view` nuevo.
- `frontend/src/components/DevicesPanel.tsx`: deja de fetchear y de embeber los nueve paneles;
  pasa a ser presentacional (`state`/`reload` por props desde `DashboardShell`).
- `frontend/src/lib/apiClient.ts`: `PairingCode`, `generatePairingCode`, `revokePairingCode`.
- `frontend/src/contexts/AuthContext.tsx`: fix de hidratación (ver arriba).
- `frontend/src/app/page.tsx`: se reduce al *gate* de autenticación + landing pre-login (con el
  diagnóstico de infraestructura del Sprint 1, ya sin sentido dentro del dashboard autenticado);
  monta `DashboardShell` al autenticarse.
- `frontend/src/app/globals.css`: estilos del layout de dashboard (sidebar, selector de
  dispositivo, tarjeta de código de vinculación), responsive por debajo de 900px (sidebar pasa a
  franja horizontal con scroll).

## Android

Sin cambios — el alcance del Paso 23 es explícitamente el panel web.

## Verificación

Ver `docs/sprint-24-evidence.md` para comandos, salida real y las capturas de pantalla del
dashboard funcionando contra el backend real (Docker) con un tutor autenticado.

## No se marca como verificado

- Login real de Google desde el navegador — mismo límite recurrente en todos los sprints
  anteriores: exige que una persona elija su cuenta en el selector de Google. La verificación
  visual de esta sesión usó un usuario y una sesión creados directamente en la base de datos (mismo
  mecanismo que los *fixtures* de las pruebas de integración del backend), no un login real.
- CI en GitHub Actions — pendiente de la corrida en un runner limpio tras el push.
- `/security-review` sobre el diff de este sprint — pendiente, a correr antes de dar el sprint por
  cerrado.

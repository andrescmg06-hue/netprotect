# Decisiones pendientes del rediseño Android

Se resuelven **todas en el Sprint 40**, con el dueño del proyecto, antes de escribir código. Cada una
indica el sprint que bloquea. Al resolverla, Claude Code cambia `Estado` a `Resuelta` y anota la
opción elegida y la fecha. Ningún agente toma una de estas decisiones por su cuenta.

Formato: qué falta · por qué importa · opciones y consecuencias · recomendación.

---

## D-01 · Navegación entre pantallas — bloquea S42

Hoy no hay navegación: `HomeScreen` cambia entre estados y `TutorScreen` es un solo scroll.
Con 17 pantallas hace falta pila de navegación y botón "atrás" del sistema.

- **(a) Rutas propias**: `sealed interface` de rutas + pila en `rememberSaveable` + `BackHandler`.
  Sin dependencias nuevas; coherente con la regla "sin dependencias que no hagan falta". Hay que
  escribir el `Saver` de la pila para rotación.
- **(b) `navigation-compose`** con rutas tipadas: robusta y estándar, pero añade dependencia y el
  plugin de `kotlinx-serialization`.

**Recomendación: (a).** Dos niveles de profundidad y ningún *deep link* no justifican la dependencia.
Estado: Pendiente

## D-02 · ViewModel — bloquea S44

`.claude/rules/android.md`: "Sin Hilt/DI ni ViewModel todavía". Sin ViewModel, rotar la pantalla
vuelve a pedir los datos.

- **(a) Mantener la regla**: *state holders* simples recordados en la composición; recarga al rotar.
- **(b) Introducir `lifecycle-viewmodel-compose`** solo para pantallas de datos.

**Recomendación: (a)** ahora; revisarlo en S51 con evidencia (si la recarga molesta de verdad).
Estado: Pendiente

## D-03 · Iconos — bloquea S42

- **(a) SVG de Lucide convertidos a `VectorDrawable`** en `res/drawable/ic_*.xml`, solo los usados
  (~35). Licencia ISC, sin dependencia, aspecto idéntico al panel web.
- **(b) `material-icons-extended`**: dependencia grande y estilo distinto al web.
- **(c) Librería de terceros de Lucide para Compose**.

**Recomendación: (a).**
Estado: Pendiente

## D-04 · Tipografía Inter — bloquea S42

- **(a) Empaquetar Inter** (licencia OFL) en `res/font/`, pesos 400/500/600/700.
- **(b) Google Fonts descargable** (`ui-text-google-fonts`): dependencia nueva y depende de red/Play.
- **(c) Fuente del sistema.**

**Recomendación: (a).**
Estado: Pendiente

## D-05 · Ubicación sin nombre de lugar ni mapa — bloquea S45

El mockup muestra "Acacías, Meta, Colombia" y un mapa. Nombre de lugar = geocodificación inversa =
enviar coordenadas del menor a un tercero (choca con la invariante de privacidad 6 de
`security-reviewer`). Mapa = teselas externas (descartado en el Sprint 13).

- **(a)** Sin lugar ni mapa: hora, "hace N min", precisión, "Abrir en mapa" (intent `geo:`, ya
  existe, lo decide la persona) y, si la lectura cae dentro de una geocerca conocida, "Dentro de
  'Casa'" (cálculo local con radio + precisión, sin red).
- **(b)** `Geocoder` de Android en el teléfono del tutor: envía coordenadas a Google Play Services.
- **(c)** Geocodificación en el backend con un proveedor externo.

**Recomendación: (a).** Coordenadas en texto: mostrarlas en pequeño o no (subdecisión).
Estado: Pendiente

## D-06 · Integración de ramas — bloquea S40

`sprint-31-design-system` va 20 commits por delante de `main` (rediseño web + harness).

- **(a)** PR de `sprint-31-design-system` → `main` con CI verde; después rama de integración
  `android-redesign` desde `main`, y una rama por sprint hacia `android-redesign`.
- **(b)** Seguir trabajando sobre `sprint-31-design-system`.

**Recomendación: (a).** `main` no queda a medias (pantallas claras y oscuras mezcladas) mientras dure
el rediseño; se integra en S51.
Estado: Pendiente

## D-07 · Iconos de apps en la lista del tutor — bloquea S45

El backend no envía iconos.

- **(a)** Monograma (inicial + color derivado del paquete).
- **(b)** Icono local si esa misma app está instalada en el teléfono del tutor
  (`PackageManager`, `QUERY_ALL_PACKAGES` ya declarado); si no, monograma. Ningún dato sale del teléfono.

**Recomendación: (b).**
Estado: Pendiente

## D-08 · Barra inferior y "Más" — bloquea S44

Los mockups muestran Inicio / Dispositivos / Actividad / Más, pero ninguno define "Más".

- **(a)** Como el mockup: Inicio (vinculación + resumen), Dispositivos (lista → detalle → secciones),
  Actividad (auditoría), Más (Cambiar de modo, Cerrar sesión, Acerca de: versión y privacidad).
- **(b)** Tres pestañas: sin "Dispositivos" (la lista ya está en Inicio).

**Recomendación: (a)**, con el contenido de "Más" indicado.
Estado: Pendiente

## D-09 · "Silenciar" alerta — bloquea S46

El backend acepta `days` o `null` (indefinido). La web usa `null` y deja quitar silencios.

- **(a)** Silencio indefinido con diálogo de confirmación; quitar silencios solo desde la web.
- **(b)** Opciones 1 / 7 / 30 días / indefinido y gestión de silencios en el móvil.

**Recomendación: (a).**
Estado: Pendiente

## D-10 · Auditar la consulta de ubicación — bloquea S47

La especificación pone "consultar ubicación" como ejemplo de auditoría, pero hoy **no se registra**
(las acciones auditadas son de escritura: vincular, renombrar, reglas, alertas…).

- **(a)** Registrar `LOCATION_VIEWED` en `GET /devices/{id}/location/latest` (solo acción + id de
  dispositivo, nunca coordenadas). Cambio de backend con tests.
- **(b)** No registrarlo; "Mi actividad" muestra solo lo que ya se audita.

**Recomendación: (a)**: quién miró la ubicación de un menor es justo lo que una auditoría debe responder.
Estado: Pendiente

## D-11 · Detalle en la pantalla de bloqueo — bloquea S49

- **(a)** Motivo + app (icono y nombre locales) + categoría real si existe asignación + frase de
  reinicio para límite diario/semanal (tras verificarla en el motor).
- **(b)** Además, la franja horaria concreta (SCHEDULE / SCHOOL_MODE): obliga a ampliar lo que
  devuelve `RuleEvaluator` → toca el código de aplicación de reglas.

**Recomendación: (a)** ahora; (b) como tarea separada si se quiere.
Estado: Pendiente

## D-12 · Forma del consentimiento — bloquea S50

El consentimiento real es por solicitud; tras él, Android muestra su diálogo cada vez.

- **(a)** Pantalla del mockup con casilla "Autorizo…" que habilita "Continuar", más "Ahora no",
  con los textos corregidos (ver `mockups/README.md` §15).
- **(b)** Sin casilla: "Aceptar" / "Ahora no" (como hoy).

**Recomendación: (a)**: doble gesto explícito, coherente con el mockup, sin afirmaciones falsas.
Estado: Pendiente

## D-13 · Tests de UI de Compose — bloquea S42

- **(a)** Añadir `androidx.compose.ui:ui-test-junit4` (androidTest) y `ui-test-manifest` (debug): tests
  de estados (cargando/vacío/error/datos) por pantalla, en el job `android-instrumented` de CI.
- **(b)** Solo tests JVM de formateadores y mapeos + verificación manual con capturas.

**Recomendación: (a)**: son dependencias solo de prueba.
Estado: Pendiente

## D-14 · `lintDebug` en CI — bloquea S51 (opcional)

Deuda D-02 de `docs/tasks.md`: es el único chequeo que detecta APIs por encima de `minSdk 26`.

- **(a)** Añadirlo al job `android` de CI en S51.
- **(b)** Dejarlo manual.

**Recomendación: (a).**
Estado: Pendiente

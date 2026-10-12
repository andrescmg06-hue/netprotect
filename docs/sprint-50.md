# Sprint 50 — Supervisado: consentimiento de vista remota, Estado de NetProtect y reconexión realtime

Undécimo sprint del rediseño Android (`docs/android-redesign/sprints/S50-supervisado-consentimiento-y-servicios.md`).
Mockups 15 y 17. Decisión aplicada: D-12 (a): casilla «Autorizo…» que habilita «Continuar», más «Ahora no», con textos
verdaderos. Es el consentimiento de un menor para que vean su pantalla: aquí importa más que nada que cada frase sea
verdad.

## Qué se hizo

- **Claude — canal, registro y enrutado** (`12fa80f`):
  - **B-02, reconexión.** El canal por el que el teléfono escucha las peticiones de vista remota ahora se reconecta solo
    (`ReconnectingRealtimeChannel`):
    - esperas de 1, 2, 4, 8 y 16 s, y luego cada 30 s (`ReconnectPolicy`);
    - un token válido como primer mensaje en cada intento (invariante 11);
    - la espera solo se reinicia cuando el backend confirma `connected`, así que un rechazo inmediato (4401) también
      espera;
    - se detiene para siempre con 4404 (dispositivo ajeno) y se cierra limpio al salir del modo.

    Antes, una sola caída lo dejaba cerrado y toda petición posterior se perdía. La sesión de vista remota en sí **no**
    se reconecta (si cae, termina, como siempre) y `RuleEnforcementService` sigue con su consulta de cada minuto.
    `RealtimeClient` gana eventos de conexión opcionales; sus otros usuarios no cambian.
  - **`ServiceStatusRegistry`.** Los servicios avisan de su propio arranque y parada, y el latido de cada envío correcto.
    `servicesView` prefiere «Sin confirmar» a un «Activo» sin pruebas; el control de apps exige además la marca de vida
    de `EnforcementLiveness`. Nada cambia lo que hacen los servicios.
  - **Consentimiento como pantalla propia.**
    - Lo que se envía al aceptar o rechazar y el diálogo de Android son **idénticos** a los del Sprint 23.
    - Si el tutor cancela, o pasan 2 minutos sin respuesta, la pantalla se cierra **sin enviar un "no"** en nombre del
      menor.
    - «Atrás» no es una respuesta.
  - **Banda en la app** mientras se comparte la pantalla. Su «Detener» envía lo mismo que el botón de la notificación.
  - **«Estado de NetProtect»** es accesible desde Dispositivo vinculado.
  - 5 tests JVM.
- **DeepSeek** (`docs/delegated/done/sprint-50-consentimiento.md`, `8d16c2b`): las dos pantallas y la banda con el
  diseño de los mockups, 14 tests de UI y 6 estados en la galería. No tocó el shell ni `core/`.
- **Claude — revisión** (`9c2daab`): sin hueco de 24 dp bajo la flecha en Estado de NetProtect (lo señaló DeepSeek;
  venía del encargo), y los encabezados del consentimiento en estilo `Title`, como en el mockup.

## Cambios visibles para el menor

- Cuando el tutor pide ver la pantalla aparece una pantalla completa que explica qué permite, cuándo se usa y cómo
  detenerla. Hay que marcar «Autorizo a mi tutor a ver la pantalla de este dispositivo ahora» para poder pulsar
  «Continuar».
- Mientras se comparte, una banda azul arriba con «Detener», además de la notificación de Android.
- «Estado de NetProtect» muestra qué está activo de verdad: reporte, control de apps, ubicación y vista remota, con los
  títulos reales de las notificaciones.
- Las peticiones del tutor llegan aunque el teléfono haya perdido la conexión un rato.

## Decisiones (aprobadas por el dueño)

- **Título:** «Consentimiento para ver la pantalla», con el subtítulo «Tu tutor pidió ver la pantalla de este
  dispositivo ahora.».
- **Tiempo límite de 2 minutos** sin responder, sin enviar nada en nombre del menor. El backend no avisa al teléfono si
  el tutor cierra la pestaña.
- **Sin «Cerrar sesión»** en Estado de NetProtect: ya está en Dispositivo vinculado.

## Lo que el mockup decía mal y no se copió

| Mockup | Decía | Se puso |
|---|---|---|
| 15 | «Puedes revocar el permiso desde los ajustes del dispositivo» | «Puedes detenerla cuando quieras — desde la notificación o el aviso azul» |
| 15 | «Se te redirigirá a los ajustes del sistema» | «Android te pedirá confirmarlo en un diálogo del sistema» |
| 15 | «…desde la app NetProtect» | «…desde el panel web» |
| 15 | «…podrá ver la pantalla cuando lo solicite» (permiso permanente) | «Solo para esta sesión» |
| 17 | «Se detiene cuando tu tutor la finaliza» | «…o cuando tú la detienes» |
| 17 | Notificación «Supervisión activa» | Los tres títulos reales |

## Pendiente

- **H-02:** una persona acepta de verdad el diálogo de Android y el tutor ve la pantalla en el panel web, con la banda
  y «Estado de NetProtect» en «En curso».
- Repetir los tests de UI con el emulador tras los retoques (`9c2daab`): el emulador se cerró por falta de memoria del
  equipo. Los tests JVM, la compilación de los tests de UI y lint pasaron.
- Aspecto en el Galaxy S25 FE.

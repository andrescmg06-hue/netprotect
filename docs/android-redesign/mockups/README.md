# Mockups de la app Android — índice y correcciones de verdad

Los 17 mockups salieron del prompt `docs/planning/prompt-mockups-android.md`, que a su vez se escribió
leyendo el código real. Aun así, **la herramienta de mockups añadió datos de ejemplo y adornos que la
app no puede mostrar**. Este archivo dice, pantalla por pantalla, qué se copia del mockup y qué no.

**Regla:** del mockup se copia la **composición visual** (jerarquía, tarjetas, espaciado, colores,
iconos, tono del texto). Los **datos** salen siempre de la API o del propio dispositivo. Si el
mockup muestra un dato que no existe, se omite o se sustituye por el dato real equivalente, nunca se
simula. Los nombres de ejemplo ("Tablet de Sofía", "Andrés", YouTube…) son solo ilustrativos.

| # | Archivo | Pantalla | Sprint |
|---|---|---|---|
| 1 | `01-login.jpg` | Inicio de sesión | S43 |
| 2 | `02-elegir-modo.jpg` | Elegir modo | S43 |
| 3 | `03-tutor-inicio.jpg` | Inicio del tutor | S44 |
| 4 | `04-tutor-detalle-dispositivo.jpg` | Detalle del dispositivo | S44 |
| 5 | `05-tutor-apps.jpg` | Apps del dispositivo | S45 |
| 6 | `06-tutor-ubicacion.jpg` | Ubicación | S45 |
| 7 | `07-tutor-geocercas.jpg` | Geocercas | S45 |
| 8 | `08-tutor-historial.jpg` | Historial | S46 |
| 9 | `09-tutor-estadisticas.jpg` | Estadísticas | S46 |
| 10 | `10-tutor-alertas.jpg` | Alertas | S46 |
| 11 | `11-tutor-mi-actividad.jpg` | Mi actividad (auditoría) | S47 |
| 12 | `12-supervisado-vincular.jpg` | Vincular este dispositivo | S48 |
| 13 | `13-supervisado-vinculado.jpg` | Dispositivo vinculado | S48 |
| 14 | `14-supervisado-permisos.jpg` | Permisos del dispositivo | S48 |
| 15 | `15-supervisado-consentimiento.jpg` | Consentimiento de vista remota | S50 |
| 16 | `16-supervisado-app-bloqueada.jpg` | App bloqueada (7 variantes) | S49 |
| 17 | `17-supervisado-servicios.jpg` | Estado de NetProtect (servicios) | S50 |

## Sistema visual (aplica a todas)

Fuente de verdad de los tokens: `DESIGN.md` (raíz del repo), no los píxeles del JPG. Fondo
`#F5F9FF`, primario `#246BFE` (pulsado `#1456D9`), títulos `#102B63`, éxito `#16B364`, crítico
`#F04438`, advertencia `#F79009`, púrpura `#7A5AF8`, texto secundario `#64748B`/`#5B6B82`. Para
texto sobre fondos suaves se usan las variantes `*-text` de `DESIGN.md` (contraste AA). Tarjetas
blancas, radio 12–16 dp, sombra suave. Inter. Iconos de trazo tipo Lucide. Objetivos táctiles ≥ 48 dp.

## Correcciones por pantalla

**1 · Login.** Sin el subtítulo "PANEL DEL TUTOR" bajo el logo (aún no hay modo elegido y aquí
también entra el supervisado). "Servicio listo" resume `GET /api/v1/health/ready`; ya no se
muestran BD/Redis por separado (misma decisión que el login web, Sprint 39 D2). Estados: comprobando,
listo, no disponible (+ reintentar), error de inicio de sesión.

**2 · Elegir modo.** Correcto. El subtítulo "PANEL DEL TUTOR" bajo el logo no va aquí (todavía no hay
modo elegido): solo el logo.

**3 · Inicio del tutor.** El mockup solo dibuja el estado "sin código". Faltan, y hay que diseñarlos
con el mismo sistema: código de 6 dígitos grande, cuenta atrás real (`expires_in_seconds`, hoy 180 s),
"Revocar", estado "El código venció". "Android 13" sale de `os_version` del dispositivo. La
miniatura de tablet es decorativa: la API no distingue teléfono de tablet → icono genérico de
dispositivo.

**4 · Detalle.** Correcto. "Desvincular" **debe** pedir confirmación (hoy no la pide). Hay dos
bloques de cabecera repetidos en el mockup: basta uno.

**5 · Apps.** El uso mostrado es `latest_usage_seconds` del día `latest_usage_date`: si ese día no es
hoy, se dice ("12 min · ayer" o la fecha), no se presenta como uso de hoy. Iconos de marca: ver
decisión D-07 (icono local si la app existe también en el teléfono del tutor; si no, monograma).
El buscador es un filtro local sobre la lista ya cargada.

**6 · Ubicación.** **No** hay nombre de lugar ("Acacías, Meta, Colombia"): obtenerlo exige enviar las
coordenadas del menor a un servicio de geocodificación (invariante de privacidad). **No** hay mapa
embebido (decisión del Sprint 13: sin Maps SDK ni teselas). Ver D-05. Sí: hora de la lectura,
"hace N min", precisión "~200 m", "Abrir en mapa" (intent `geo:` ya existente) y aviso de retraso.

**7 · Geocercas.** Sin ciudad bajo el nombre (no se almacena): se muestra el radio ("Radio 150 m").
"Último evento" por geocerca se deriva del listado de eventos. Solo lectura.

**8 · Historial.** "Bloqueo de app · YouTube": el evento trae `package_name`; el nombre visible se
obtiene cruzando con la lista de apps del dispositivo y, si no aparece, se muestra el paquete. El
motivo sale de `rule_type_applied` con los textos del panel web.

**9 · Estadísticas.** ⚠️ El subtítulo del mockup es **falso**: "Porcentaje del tiempo permitido usado"
no es lo que calcula el backend. `compliance_rate` = días dentro del límite / días evaluados. Texto
correcto: "Días dentro del límite: 5 de 7". "Bloqueos por tipo" muestra los motivos reales con
conteo > 0 (hasta 7), no tres fijos.

**10 · Alertas.** Los tipos reales son 9 (`APP_BLOCKED`, `APP_LIMIT_REACHED`, `GEOFENCE_ENTER`,
`GEOFENCE_EXIT`, `PERMISSION_REVOKED`, `SERVICE_INACTIVE`, `HEARTBEAT_SILENCE`, `CLOCK_TAMPERING`,
`UNINSTALL_ATTEMPT`). "Protección contra desinstalación desactivada" y "Ubicación actualizada" del
mockup **no existen**: los textos salen del mapa de etiquetas del panel web. Nivel siempre con
icono + texto + color. Los filtros son locales. "Silenciar" = silencio indefinido (como la web).

**11 · Mi actividad.** La auditoría guarda acción + tipo/id de recurso + hora, **no** detalles como
"límite diario de 2 h para YouTube": se muestra la etiqueta de la acción y el recurso. Registra lo que
el tutor hace **en la app y en el panel web** (corregir "solo en la app"). Sin borrar, sin exportar.

**12 · Vincular.** Correcto. Código solo numérico de 6 dígitos (el backend genera 6 dígitos).

**13 · Vinculado.** ⚠️ "Cerrar sesión — Desvincular este dispositivo de tu cuenta" es **falso**:
cerrar sesión no desvincula (desvincular lo hace el tutor). Texto correcto: "Cierra la sesión en
este dispositivo". "Supervisado por" puede ser más de un tutor (`tutors` es una lista).
"Android 13" aquí sí puede leerse del propio dispositivo.

**14 · Permisos.** Correcto. El estado se vuelve a comprobar al volver de Ajustes.

**15 · Consentimiento.** ⚠️ El mecanismo real es **por solicitud**: aparece cuando el tutor pide ver
la pantalla, y después Android muestra **su propio diálogo** de captura cada vez (no es
configurable). Por tanto: "Puedes retirar este consentimiento desde los ajustes del dispositivo" y
"Se te redirigirá a los ajustes del sistema" son **falsos**. Correcto: "Android te pedirá
confirmarlo en un diálogo del sistema" y "Puedes detenerla en cualquier momento desde la
notificación". Ver D-12 sobre la casilla.

**16 · App bloqueada.** Las 7 variantes existen en el motor real (`BlockReason`). La categoría bajo
el nombre de la app sale de la asignación real y con los nombres reales (no existe "Entretenimiento"
ni "Navegador": las categorías son las del enum, p. ej. `STREAMING` → "Streaming"). Horas concretas
("de 10:00 p. m. a 6:00 a. m.") solo si se implementa D-11 (b). "Podrás volver a usarla mañana /
el próximo lunes" solo tras verificar cómo reinicia el motor el contador (la semana empieza en lunes).

**17 · Servicios.** Solo servicios que existen: reporte de estado (latido), control de apps
(`RuleEnforcementService`), ubicación (`LocationReportingService`) y vista remota
(`ScreenShareService`, solo mientras esté activa). "NetProtect no graba" es verdad (Sprint 23: el
video no se graba ni se persiste).

# Sprint 51 — Evidencia

## T9 — Regresión contra el inventario de S40

Fuente: `docs/android-redesign/INVENTARIO.md` (los puntos de la app anterior al rediseño). Cada punto se buscó en el
código actual (rama `sprint-51-cierre`) y los dudosos se comprobaron en el emulador con la build `minified`.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

**Resultado: 0 puntos perdidos.** Todos están presentes o sustituidos a propósito.

### Presentes sin cambio de comportamiento

H-01, H-03, H-05..H-08, H-10..H-14, H-16, H-19, T-01, T-05, T-08, T-15, S-01, S-02, S-10, S-16, S-17, B-01..B-06,
B-08, B-09, N-01..N-12, N-15..N-17, N-19, C-02..C-05, C-12, C-13, C-15..C-20.

Siguen sin confirmación, como antes: Revocar código (C-02), Cerrar sesión (C-03), Cambiar de modo (C-04), Vincular
y Elegir rol (C-05). La sincronización de apps sigue fallando en silencio (C-12).

### Sustituidos a propósito

| Punto | Antes | Ahora | Sprint |
|---|---|---|---|
| M-01, H-04, H-09 | `MaterialTheme`; «Servidor: … · BD: … · Redis: …» | Tema propio; «Servicio listo / no disponible» con «Reintentar» | S42, S43 |
| H-02, C-14, N-18, C-25 | Sin red: sesión borrada en silencio | Sesión conservada; «Sin conexión» y se retoma sola | S41 |
| H-15, T-02, S-06 | `detail` crudo o «HTTP nnn» | Mensajes en español (`UiError`) | S41 |
| H-17, H-18 | `TutorScreen`/`SupervisedScreen` monolíticos | `TutorShell`/`SupervisedShell` con `TokenSession` | S44, S48 |
| T-03, C-23 | Código sin caducidad en pantalla | Cuenta atrás «Vence en m:ss» y «El código venció» | S44 (G-09) |
| T-04, C-09 | Error al revocar silenciado | Error visible | S44 |
| T-06, T-07 | Sección «Ver/Ocultar» con 50 filas | Pestaña «Actividad», agrupada por día, paginada | S47 (G-17) |
| T-09, T-10 | `ONLINE`/`OFFLINE` crudos | «En línea / Desconectado / Alerta» y «hace N min» | S42 (G-07), S44 |
| T-11, C-06, C-08 | Renombrar sin validar; error silenciado | Valida 1–255; error visible; la edición sigue abierta | S44 |
| T-12, C-01, C-07 | Desvincular al primer toque, error silenciado | Diálogo de confirmación y error visible | S44 (G-11) |
| T-13 | Lista simple | Búsqueda, iconos, orden instaladas → uso | S45 (G-12) |
| T-14 | `Lat, Lng (±n m)` | Sin coordenadas: «Última lectura», «Precisión aproximada», «Dentro de» | S45 (G-13, D-05) |
| C-10 | Sin app de mapas: silencio | «No hay una aplicación de mapas en este teléfono.» | S45 |
| T-16 | Lista de texto | Pantalla Geocercas con «Entró · / Salió ·» | S45 |
| T-17 | Lista de texto | Agrupado por día, «Bloqueo de app · X», «Motivo: …» | S46 (G-14) |
| T-18, T-19 | Periodo recordado por dispositivo | Periodo guardado al girar; cumplimiento «5 de 7 días» | S46 (G-15), S51 |
| T-20 | Solo lectura | Marcar leída, Silenciar (con confirmación), filtros | S46 (G-16) |
| T-21 | 9 etiquetas | Las 9 siguen; 3 reescritas (ver abajo) | S46 |
| T-22, C-24 | Secciones desplegables sin reintento | Rutas apiladas; `ErrorState` con «Reintentar» | S44 (D-01) |
| T-23 | Cerrar sesión al pie | En Inicio y en la pestaña Más | S44 |
| S-03..S-05, C-22 | Campo de texto; sin bloqueo durante la llamada | 6 casillas; botón deshabilitado mientras vincula | S48 (G-20) |
| S-07, C-11 | Tarjeta fija; latido fallido en silencio | Varios tutores, última comunicación, «Sin conexión» | S48 |
| S-08, S-09, S-11 | Tarjeta de solicitud | Pantalla de consentimiento; reconexión; cierre a los 2 min | S50 (D-12, B-02) |
| S-12..S-15 | Tarjetas que desaparecen al conceder | Tarjetas «Configurado / Pendiente» siempre visibles | S48 |
| N-13 | Sin relectura al volver de Ajustes | `ON_RESUME` relee los 4 permisos | S48 |
| N-14 | Todo con `remember` | Pila, selecciones y código a medio escribir sobreviven al giro | S42, S51 |
| N-20, C-21 | `formatCapturedAt` estilo `MEDIUM` | «Hoy, 3:42 p. m.», «hace N min» | S42 |
| B-07 | Aviso que explicaba cómo evadir el bloqueo | «…la app puede seguir abierta en segundo plano.» | S49 |
| C-22 (resto) | Sin progreso en Generar, Guardar, Desvincular | Con estado «en curso» y bloqueo de doble toque | S44 |

### Cambios de texto que no estaban documentados (para el dueño)

- **S-13 (permiso de ubicación):** se quitó «sólo se comparte mientras esta app siga activa en segundo plano». La
  frase sigue siendo cierta, y la pantalla 17 lo cubre con la notificación «NetProtect comparte la ubicación».
- **T-21:** `PERMISSION_REVOKED`, `SERVICE_INACTIVE` y `HEARTBEAT_SILENCE` tienen redacción nueva, más precisa y con
  el mismo sentido.
- **S-15:** «tu tutor recibirá un aviso» pasó a «Permite avisar al tutor si se intenta desinstalar». Estaba en el
  plan de S48.

### Pendiente en un dispositivo real (T10)

- N-09: que el diálogo de notificaciones no reaparezca en cada vuelta a «Vinculado».
- N-13, S-12..S-15: volver de Ajustes y ver la tarjeta cambiar a «Configurado».
- S-08..S-11: consentimiento y vista remota real (H-02), que cubre también WebRTC con R8.

### Capturas

En `%TEMP%\np-sprint51\`, con la build `minified` y un supervisado de prueba vinculado por API:

- Tutor: `final-03-tutor-inicio` … `final-11-mi-actividad` (mockups 3–11).
- Supervisado: `t5-01-launch` (13, vinculado) y `t5-02-services` (17, Estado de NetProtect).

Sin capturar: 01–02 (login y elegir modo: requieren cerrar sesión y volver a entrar con Google), 12, 14, 15 y 16.
Quedan para T10 en el Galaxy; las de 12, 14, 15 y 16 están en las evidencias de S48, S49 y S50.

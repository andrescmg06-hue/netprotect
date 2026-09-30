# Sprint 48 — Supervisado: estructura, Vincular, Dispositivo vinculado y Permisos

Noveno sprint del rediseño Android (`docs/android-redesign/sprints/S48-supervisado-vincular-y-permisos.md`).
Mockups 12, 13 y 14. Decisiones aplicadas: D-01 (rutas propias) y D-02 (sin ViewModel).
El riesgo del sprint: partir `SupervisedScreen` mal **detiene los servicios** que protegen el teléfono del menor.

## Qué se hizo

- **Claude — backend, B-01** (`acd4716`): `GET /devices/me` respondía 500 (`MultipleResultsFound`) cuando una cuenta
  supervisada tenía más de una fila de dispositivo. Pasa al reinstalar la app, que genera un id de instalación nuevo,
  o con un segundo teléfono; la cuenta de pruebas del dueño lo tenía.
  - La app ahora dice qué instalación es (`?device_instance_id=`), un valor único por cuenta.
  - Sin el parámetro (versiones viejas de la app), gana el dispositivo más reciente con un tutor activo, y si no hay
    ninguno, el más reciente. Nunca un 500.
  - Un id de instalación de otra cuenta sigue dando 404.
  - 4 tests de integración.
- **Claude — Android** (`7a1bbd9`, `2981109`): `SupervisedScreen` (587 líneas) se convierte en `SupervisedShell`. La
  máquina de estados y los **ocho efectos** se copiaron con **las mismas claves**:
  - comprobar el vínculo;
  - el latido de cada minuto;
  - la sincronización de apps;
  - `RuleEnforcementService`;
  - `LocationReportingService`;
  - el socket de vista remota;
  - `SyncWorker`;
  - el permiso de notificaciones.

  La navegación (Vinculado ↔ Permisos) y los datos que solo se muestran viven en otro estado que ningún efecto usa
  como clave, y el shell nunca sale de la composición. La advertencia para el futuro está escrita en el propio
  archivo: no meter nada que cambie con el tiempo dentro de `SupervisedState.Linked`, porque cada cambio pararía y
  volvería a arrancar todos los servicios.

  Además:
  - Los permisos se vuelven a leer al volver a la app (`ON_RESUME`), por ejemplo desde Ajustes.
  - «Última comunicación» es el más reciente entre el último latido que este teléfono envió con éxito y el
    `last_seen_at` del servidor, sin peticiones extra.
  - «Supervisado por» muestra todos los tutores.
  - Se añadieron funciones puras con 5 tests JVM y pantallas provisionales.
  - `PermissionCard` pasó a decir «Configurado».
- **DeepSeek** (`docs/delegated/done/sprint-48-supervisado.md`, `983079e`): las tres pantallas, 20 tests de UI y
  13 estados en la galería. No tocó `SupervisedShell`.
- **Claude — revisión** (`d97b8c1`): los botones de `PermissionCard` ocupan todo el ancho, como en el mockup 14.

## Cambios visibles

- **Vincular:** seis casillas para el código, que aceptan pegarlo, errores en español y «Comprobar vínculo».
- **Dispositivo vinculado:**
  - nombre del dispositivo y versión de Android;
  - todos los tutores;
  - «Hace N min · Conectado / Sin conexión»;
  - resumen de permisos;
  - «Cerrar sesión» con el texto verdadero: «Cierra la sesión en este dispositivo.». El mockup decía «Desvincular»,
    y cerrar sesión no desvincula.
- **Permisos:** cuatro tarjetas con «Pendiente» / «Configurado», actualizadas al volver de Ajustes.
- **Vista remota (Sprint 23):** la tarjeta de consentimiento sigue funcionando igual, ahora en «Dispositivo
  vinculado», y la app vuelve a esa pantalla si llega una solicitud estando en Permisos. El rediseño del
  consentimiento es del S50.

## Decisiones (aprobadas por el dueño)

- **B-01:** identificar la instalación por su id, con una regla de respaldo para versiones viejas de la app.
- **Volver a comprobar los permisos al volver a la app.** Es el único cambio de comportamiento del sprint: si alguien
  retira el acceso a uso desde Ajustes, al volver se detiene el servicio de bloqueo, que sin ese permiso ya no puede
  funcionar. El latido sigue avisando al tutor con su señal de manipulación del Sprint 20.
- **«Última comunicación»** a partir del último latido local, sin peticiones extra.
- **Permisos ya concedidos:** solo «Configurado», sin botón; cómo retirarlos lo explica el aviso de la pantalla.

## Diferencias con los mockups aceptadas

| Mockup | Lo que dibuja | Qué se hizo | Por qué |
|---|---|---|---|
| 13 | «Cerrar sesión — Desvincular este dispositivo…» | «Cierra la sesión en este dispositivo.» | Era falso |
| 13 | Imagen de tablet | Icono genérico | La API no distingue tablet de teléfono |
| 13 | «Cerrar sesión» en rojo | En azul oscuro | `ListRow` no permite color de título; detalle menor |
| 14 | «Abrir Ajustes» en permisos ya concedidos | Sin botón | Decisión 4 |
| 14 | «DISPOSITIVO SUPERVISADO» junto a la flecha | Solo la marca | `NpTopBar` no tiene subtítulo |

## Pendiente

- **Reportes de ubicación con este shell:** el emulador no tiene ubicación por red, que es la única que usa la app
  (aproximada, Sprint 13). El servicio sigue vivo, pero no reporta. Hay que verificarlo en el Samsung.
- **Vincular desde cero con un código real** de una segunda cuenta de tutor (necesita una persona).
- Aspecto en el Galaxy S25 FE.

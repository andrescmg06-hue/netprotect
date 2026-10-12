# Sprint 44 — Evidencia

## 1. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
BUILD SUCCESSFUL
JVM: 110 tests, 0 fallos (15 nuevos: TutorNavigationTest 6, TutorStateTest 9)
Instrumentados (emulador Pixel_8, Android 16): 50 tests, 0 fallos (18 nuevos de DeepSeek)
lint: 0 errores, 25 avisos (sin cambios)
```

## 2. Prueba de humo con el backend de desarrollo (emulador, cuenta del dueño ya presente en el emulador)

Parte de Claude (pantallas provisionales), antes de delegar:

```text
POST   /api/v1/pairing/codes            200   generar código; cuenta atrás 2:57 → 2:52 en 5 s
DELETE /api/v1/pairing/codes/current    200   revocar
GET    /api/v1/users/me/audit           200   pestaña Actividad (sección antigua)
GET    /api/v1/devices/{id}             200   Detalle ("Android 14", leído de la API)
PATCH  /api/v1/devices/{id}             200   renombrar → "Tablet de prueba"; la lista se recarga
DELETE /api/v1/devices/{id}/link        0     al pulsar "Cancelar" en el diálogo
DELETE /api/v1/devices/{id}/link        200   al pulsar "Desvincular"; vuelve a la lista, ya vacía
```

Rotación en una sección (Alertas): la pantalla se conserva y se recarga. "Atrás": Detalle → lista → Inicio → sale de la app.

Para probar el Detalle se insertó en la base de datos **de desarrollo** un dispositivo de prueba ("Dispositivo de prueba
S44", usuario `s44-test@example.invalid`) vinculado a la cuenta del tutor; tras la prueba se borraron el dispositivo, su
estado, el vínculo y el usuario (comprobado: 0 filas). Las entradas de auditoría de esas acciones se conservan: la
auditoría es inmutable por diseño.

Tras el trabajo de DeepSeek: Inicio real contra el backend (lista vacía) y la galería de pantallas con todos los estados
(código activo, vencido, error; lista con datos, vacía y en error; Detalle cargado, renombrando con error y no encontrado;
Más), comparados con los mockups 3 y 4.

## 3. Revisión de seguridad (rol `security-reviewer`, ligera)

Sin hallazgos: el código de vinculación solo vive en memoria (no en disco ni en el estado guardado de la navegación, ni en
logs); desvincular solo sale del diálogo de confirmación y una vez; un 404 no muestra datos del dispositivo; las rutas
guardadas solo llevan IDs y un ID manipulado solo produce un 404 del backend; las galerías siguen fuera del manifiesto de
release. Dos observaciones de comportamiento, corregidas: al terminar de desvincular se cerraba la pantalla actual aunque
el tutor ya hubiera abierto otro dispositivo (ahora se cierra solo el detalle del desvinculado, con test); y tras un 404 la
lista no se recargaba (ahora sí).

## 4. Inventario (`docs/android-redesign/INVENTARIO.md`, T-01 a T-23)

Todo sigue accesible: vinculación (T-01–T-05), auditoría (T-06–T-07, pestaña Actividad), lista (T-08–T-10), renombrar
(T-11, ahora con validación y error visible), desvincular (T-12, ahora con confirmación), secciones (T-13–T-21, sin
cambios en `legacy/`), carga por sección al abrir (T-22) y cerrar sesión (T-23, en Inicio y en Más).

## 5. Fallos del camino

- El CI del S43 falló primero por el runner (no pudo descargar el emulador) y después de verdad: en su pantalla más
  pequeña, "Cerrar sesión" quedaba fuera de vista y el test lo tocaba sin desplazarse. Corregido en `9c1b079`; el encargo
  del S44 exige `performScrollTo()` antes de cada toque.
- El emulador se cerró una vez por falta de memoria del equipo; se volvió a arrancar con 2 GB.

## 6. No verificado

- Aspecto en el Galaxy S25 FE y login real de Google en el teléfono (H-01).
- Vinculación real de un teléfono supervisado con un código generado en esta pantalla (requiere una segunda cuenta).

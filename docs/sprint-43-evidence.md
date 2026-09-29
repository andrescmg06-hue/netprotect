# Sprint 43 — Evidencia

## 1. Android — `cd mobile && ./gradlew test lintDebug connectedDebugAndroidTest`

```text
BUILD SUCCESSFUL in 2m 1s
JVM: 95 tests, 0 fallos
lint: 0 errores, 25 avisos (sin cambios respecto al S42)
Instrumentados (emulador Pixel_8, Android 16): 32 tests, 0 fallos, 0 errores
  PendingRuleEventStoreTest 4 · RulesCacheStoreTest 4 (Room)
  ComponentsTest 7 · DesignGalleryTest 1 (S42)
  LoginScreenTest 8 · RoleSelectionScreenTest 8 (nuevos)
```

## 2. Comprobaciones de Claude en la revisión

- Archivos tocados por DeepSeek: solo los de la lista permitida; `HomeScreen.kt` no aparece en su diff.
- Firmas de las tres pantallas intactas (la app compila con las llamadas que dejó Claude en `HomeScreen`).
- `grep "Color(0x|fontSize=|RoundedCornerShape("` en `feature/home` y `src/debug`: solo `HomeScreen.kt:171`
  (`Color(0xFF090B10)`, el fondo del `Surface` que Claude conserva para el tutor y el supervisado).
- Ningún texto de infraestructura (BD, Redis, HTTP) en las pantallas; los tests lo comprueban.

## 3. Revisión visual en el emulador (galería de pantallas, `ScreensGalleryActivity`)

Revisados todos los estados: carga; login (comprobando, listo, no disponible con "Reintentar", listo con error);
elegir modo (con nombre, sin nombre → correo, con error). Comparados con `01-login.jpg` y `02-elegir-modo.jpg`:
composición, jerarquía, botón de Google con la "G" a color y flecha, línea de estado con punto de color,
tarjetas de modo con `IconTile`, botones "Elegir" primario y secundario, "Cerrar sesión" con icono.

## 4. Fallos del camino

- El emulador entregaba capturas negras de la ventana de la app tras arrancarla varias veces (la pantalla de inicio
  del sistema sí se capturaba; `logcat` mostraba fotogramas normales y ningún cierre). Se reinició con
  `-gpu swiftshader_indirect` y desde entonces las capturas funcionan. Es un problema del emulador, no del código.
- DeepSeek no pudo comprobar el desplazamiento de la galería con gestos sintéticos; con el renderizado por
  software sí responde (usado para las capturas de esta revisión).

## 5. No verificado

- Login real de Google y aspecto en el Galaxy S25 FE (H-01).
- Flujo completo contra el backend real (servicio caído → "Reintentar" → listo); lo cubren los tests de UI de cada
  estado y el cableado de `HomeScreen`, pero no se ejecutó de extremo a extremo.

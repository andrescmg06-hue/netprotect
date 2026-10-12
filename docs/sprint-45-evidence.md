# Sprint 45 — Evidencia

## 1. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
Parte de Claude (f557e7e): BUILD SUCCESSFUL · JVM 124 tests, 0 fallos (14 nuevos) · instrumentados 50, 0 fallos
Tras DeepSeek + arreglo del buscador (04e40dd): BUILD SUCCESSFUL in 3m 58s
Instrumentados (emulador Pixel_8, Android 16): 68 tests, 0 fallos (18 nuevos de DeepSeek)
lint (informe de DeepSeek antes del arreglo): 0 errores, 24 avisos (base 25: el aviso UseKtx de LegacySections.kt desapareció con el código retirado)
```

## 2. Revisión del trabajo de DeepSeek

- `git status`: solo archivos del encargo; `TutorShell`, `SectionViews` y los `*State` sin tocar.
- `grep` de `Color(0x`, `fontSize =`, `RoundedCornerShape(`, `Instant.now`, `LocalDate.now`, `latitude`, `longitude`
  en las tres carpetas y en `SectionDeviceHeader.kt`: sin coincidencias. Las pantallas no leen la latitud ni la longitud.
- `grep` de `AppsList|LocationSection|GeofenceSection|formatUsageDuration`: sin coincidencias (retirado).
- Emulador, galería de pantallas: Apps (6 apps con iconos reales de Chrome y YouTube, monogramas, "ayer", "Sin uso
  registrado"; vacío; error), Ubicación (con zona, sin zona, sin reporte, error) y Geocercas (3 zonas, historial, vacío,
  error), comparadas con los mockups 5–7. Ninguna muestra coordenadas, lugar ni mapa.

## 3. Revisión de privacidad (invariante de ubicación)

Hecha a mano por Claude: el agente `security-reviewer` no estaba disponible en esa sesión. Comprobado: la latitud y la
longitud solo se usan en `GeoMath`/`locationView` (cálculo) y en el intent `geo:` de `TutorShell` al tocar "Abrir en
mapa"; ninguna pantalla las dibuja ni las escribe en un log; sin geocodificación ni teselas; "Dentro de" exige
distancia + precisión ≤ radio (tests de límites: exacto, precisión mayor que el radio, entradas inválidas).

## 4. Fallos del camino

- Las capturas de la galería que entregó DeepSeek no llegaban a las pantallas del S45 (eran de sprints anteriores); se
  revisó la galería directamente en el emulador.
- Defecto encontrado en la revisión visual: el buscador de Apps se partía en dos líneas junto al botón. Corregido en
  `04e40dd`; después de ese cambio se repitió la batería completa (BUILD SUCCESSFUL), pero no se volvió a mirar la
  pantalla en el emulador.

## 5. No verificado

- Ubicación con una lectura real del teléfono supervisado; aspecto en el Galaxy S25 FE.

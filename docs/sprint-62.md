# Sprint 62 — Sistema visual v2 del supervisado («Sello»)

Segundo sprint del rediseño v2 (`docs/android-redesign/supervisado-v2/PLAN_IMPLEMENTACION.md`). La especificación
visual está en `01_DISENO.md` y en la lámina 09 (`capturas/laminas/09-Guia.png`). La paleta y la tipografía son las
del panel web: crema `#F5F3EE`, tinta `#11243D` y títulos en serif. La serif es **Newsreader**, la misma del panel web
(decisión 4, aprobada el 12/10/2026). El tema es solo claro (decisión del dueño).

Rama `sprint-62-sistema-v2`, creada desde `android-redesign` (`62867eb`), en el worktree `C:/Users/andre/np-s62`.

## Decisiones

- **Los tokens se renombran a los nombres de v2 en toda la app** y no se añaden en paralelo a los viejos. Sin el modo
  tutor (S61), todas las pantallas son del supervisado, así que la paleta nueva llega de forma coherente a todas. El
  diseño de cada pantalla (estructura, panel navy, rail…) se hace en su sprint: S63 a S66.
- Los colores semánticos de texto ya coinciden con v2: `#0B7A42` (verde), `#9A5000` (ámbar) y `#C4271C` (rojo).
- Radios de 4 dp en campos y botones, y forma de píldora en los chips. Elevación 0: se separa con líneas, no con sombras.
- Movimiento solo ante eventos, con «quitar animaciones» del sistema respetado. Sin animaciones en bucle, salvo la vista
  remota en curso, los indicadores de carga y el contador.
- `android:forceDarkAllowed="false"`: la app no se oscurece a la fuerza.
- La fuente se empaqueta (OFL), igual que Inter (D-04), con su licencia en el repositorio.

## Tareas

Ruta: **inline** = Claude directo; **delegado** = subagente escritor (modelo entre paréntesis).

- [x] T1 — Abrir el sprint. Inline. (este commit)
- [x] T2 — `e0631a7` (39 archivos). `test assembleDebug assembleDebugAndroidTest lintDebug` en verde: 85 tests unitarios y lint con 0 errores.
  Newsreader es la fuente variable oficial de Google Fonts (441 KB, OFL; licencia en `mobile/third_party_licenses/`). Contrastes
  comprobados a mano: texto ≥ 4,88:1 sobre crema; texto blanco sobre el azul de acción 4,67:1; `InkSubtle` (2,80:1) es solo
  decorativo. Ningún `ShieldNavy` era un relleno: los 35 usos eran texto o iconos y pasaron a `Ink`; `Navy` (#0D1B2A) existe sin
  usos hasta S63. El cuerpo pasa de 14/15 a 16 sp, así que los diseños se desplazan: lo revisa T4. Enunciado: Tokens v2: `Color.kt` (renombrar y revalorar en toda la app), `Type.kt` (Newsreader 600 para Display y
  Title; Inter para Body, BodySm, Label y Eyebrow; siempre en sp), `Shape.kt`, `Elevation.kt` (0 dp), nuevos
  `Spacing.kt` y `Motion.kt` (con un helper de «reducir movimiento»), `Tone.kt`, `forceDarkAllowed` y
  `ColorTokensTest`. Delegado (**Sonnet**: renombrado amplio con una tabla de equivalencias cerrada).
- [ ] T3 — Componentes: `NpHeroPanel` (panel navy con titular serif), `NpSeal` (sello de 4 arcos que se dibuja una sola
  vez), `NpRail` (recorrido vertical con el siguiente paso resaltado), `NpButton` v2 (≥ 60 dp, ficha de flecha) y
  `StatusPill` como chip. Incluye las secciones de `DesignGallery` y sus tests. Delegado (**Opus**: oficio visual y
  Canvas).
- [ ] T4 — Revisión visual: galerías en el emulador comparadas con la lámina 09 y con los componentes de las láminas
  03 y 04, letra al 200 % y «quitar animaciones». Inline (DeepSeek y los subagentes no ven imágenes). Las correcciones
  van en un lote, delegado.
- [ ] T5 — `verifier` (**Haiku**: `test assembleDebug assembleDebugAndroidTest lintDebug`) y CI. Sin
  `security-reviewer`: el sprint no toca auth, red, servicios ni datos.

## Criterios de aceptación

- Ningún color literal fuera de `ui/theme` y ningún nombre de token viejo en el código.
- Títulos en Newsreader empaquetada (con la licencia OFL en el repositorio) y textos en sp.
- `NpHeroPanel`, `NpSeal`, `NpRail`, `NpButton` v2 y el chip, en la galería y con tests.
- Con «quitar animaciones», el sello aparece completo sin animar.
- Galerías revisadas con letra al 200 %, sin cortes.
- `./gradlew test assembleDebug lintDebug` y los 8 jobs de CI en verde.

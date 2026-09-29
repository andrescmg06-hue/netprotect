# Sprint 43 — Pantallas de carga, inicio de sesión y elegir modo

Cuarto sprint del rediseño Android (`docs/android-redesign/sprints/S43-login-y-elegir-modo.md`). Primer
sprint que rediseña pantallas reales sobre el sistema de diseño del S42. Mockups 1 y 2.

## Qué se hizo

- **Claude (antes de delegar):**
  - `NpButton` acepta `tintIcon` (la "G" de Google conserva sus colores) y `trailingIcon` (flecha final).
  - `BrandHeader` tiene la variante apilada y centrada (`stacked`).
  - `HomeScreen` llama a `LoadingScreen`, `LoginScreen` y `RoleSelectionScreen` con parámetros planos y
    conserva toda la máquina de estados. Gana un contador de "Reintentar" (una comprobación nueva por toque,
    nunca un bucle) y el estado `ServiceStatus { Checking, Ready, Unavailable }`.
  - Se retiraron las pantallas viejas oscuras con sus colores literales.
- **DeepSeek** (`docs/delegated/done/sprint-43-login.md`): las tres pantallas, una galería de pantallas solo para
  debug (cada pantalla en cada estado) y 16 tests de UI.
- **Claude (revisión):** la galería de pantallas dibujaba su primer título bajo la barra de estado; ahora respeta
  los márgenes del sistema.

## Cambios visibles respecto a las pantallas anteriores

- El detalle de infraestructura del login ("Servidor: OK · BD: OK · Redis: OK") desaparece; queda "Servicio
  listo" / "Servicio no disponible". Misma decisión que el login web (Sprint 39 D2).
- "Reintentar" es nuevo: antes, un servicio caído obligaba a reiniciar la app.
- Tarjetas de modo con icono, botón "Elegir" con flecha y "Cerrar sesión" con icono.

## Sin cambios de comportamiento

Credential Manager, `AuthRepository`, `RoleClient`, `RolePreference` y el orden de estados no se tocaron.
Cancelar el selector de cuentas no es un error. El botón de Google sigue habilitado con el servicio caído.

## Decisiones

- **Sin subtítulo "PANEL DEL TUTOR"** en la marca (aún no hay modo elegido y aquí entra también el supervisado).
- **Fondo:** degradado de `SkyGround` a `SkyGroundEnd`; se omiten las ondas decorativas del mockup.
- **"Hola, {nombre}"** usa solo la primera palabra de `displayName` (o el correo completo si no hay nombre).
- **B-03** (misma cuenta como tutor y supervisado) **no se resuelve aquí**: `POST /users/me/roles` concede
  cualquier rol sin condiciones (`roles.py`), así que no hay error que mostrar; hace falta un cambio de backend
  (p. ej. rechazar el canje cuando el tutor del código es la misma cuenta). Anotado en `docs/tasks.md`.

## Diferencias con los mockups aceptadas

- El texto del botón de Google es azul (variante secundaria) y en el mockup 1 es azul marino.
- El título "Control parental" y el logo del mockup son algo más grandes y pesados.
- Sin ondas de fondo.

## Pendiente

- Login real con Google (H-01) y el aspecto en el Galaxy S25 FE: las pantallas se revisaron en el emulador.
- `HomeScreen` conserva `Color(0xFF090B10)` como fondo del `Surface`: lo usan el modo tutor y el supervisado,
  que se rediseñan en S44 y S48; entonces se retira.

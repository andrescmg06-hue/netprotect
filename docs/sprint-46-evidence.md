# Sprint 46 — Evidencia

## 1. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
Parte de Claude (b59f9a6): BUILD SUCCESSFUL in 3m 2s · JVM 137 tests, 0 fallos (13 nuevos) · instrumentados 68, 0 fallos
Tras DeepSeek + revisión (87a3cd8): BUILD SUCCESSFUL in 3m 46s · instrumentados 86, 0 fallos (18 nuevos de DeepSeek)
lint: 0 errores, 24 avisos (sin cambios)
Tras 8ef245c: testDebugUnitTest + installDebug sin fallos (2 aserciones nuevas de shortDuration)
```

## 2. Revisión del trabajo de DeepSeek

- **Archivos tocados:** `git status` muestra solo los del encargo. `ActivityViews.kt`, `ActivityState.kt` y `TutorShell.kt`
  no se tocaron.
- **Patrones prohibidos:**
  - `grep` de `Color(0x`, `fontSize =`, `Instant.now`, `CRITICAL`/`WARNING`/`"HIGH"` y `packageName` en las tres
    pantallas: solo aparece `AppIcon(row.packageName, …)`, que era la excepción declarada;
  - tampoco aparecen «Porcentaje» ni «%».
- **Hallazgos de DeepSeek:**
  - una contradicción en mi encargo sobre la cabecera de Alertas: eligió bien;
  - el frame con el diálogo modal tapaba toda la galería: se quitó.
- **Galería en el emulador:**
  - Historial (dos días, vacío, error);
  - Estadísticas (7 días, Hoy vacío, error);
  - Alertas (5 alertas, enviando, error de acción, vacía, error).
  Se compararon con los mockups 8–10; los defectos encontrados están en `docs/sprint-46.md`.

## 3. Prueba real (emulador con la cuenta de tutor del dueño, backend de desarrollo)

Estado inicial en la base: 4 alertas HIGH sin leer en los dos dispositivos del tutor y ningún silencio.

**Marcar leída** (samsung SM-S731B, alerta `fd091ab4…`, PERMISSION_REVOKED):

```text
backend: "POST /api/v1/devices/2219e3a5-…/alerts/fd091ab4-…/read HTTP/1.1" 200 OK
         seguido de GET …/alerts y GET …/alert-silences (recarga)
alerts:  fd091ab4-… | occurrence_count 1 | read_at 2026-09-30 00:37:40.623823+00
audit:   ALERT_READ | alert | fd091ab4-86f9-45be-883a-95449d1fefe8
```

- **App:** la marca «No leída» y el botón desaparecieron, y la lista no se movió.
- **Panel web:** la segunda alerta aparece sin el punto de no leída.

**Silenciar** (Google sdk_gphone64_x86_64, alerta `cbb4ea3c…`, SERVICE_INACTIVE), tras el diálogo de D-09:

```text
backend: "POST /api/v1/devices/da5e6b92-…/alerts/cbb4ea3c-…/silence HTTP/1.1" 200 OK
alert_silences: SERVICE_INACTIVE | silenced_until NULL (indefinido)
audit:   ALERT_SILENCED | alert_silence | SERVICE_INACTIVE
```

- **App:** muestra «Silenciada», también tras reiniciarla.
- **Panel web → Silenciadas:** «Servicio de control de apps detenido · Indefinido · Reactivar».

**Historial y Estadísticas con datos reales:**
- el historial del emulado está vacío y muestra el estado vacío;
- Estadísticas de 30 días muestra 10 apps con sus iconos locales.

Con estos datos se encontraron «menos de 1» cortado y «Silenciada» desalineada, ambos corregidos en `8ef245c`.

## 4. Revisión de seguridad (ligera, hecha por Claude)

- **Autorización:** las escrituras usan `session.authorized`, con renovación del token (S41), y la decide el backend
  (`require_tutor_of_device` y 404 uniforme). La app no la simula.
- **Doble envío:** no es posible; hay una acción a la vez y los botones quedan deshabilitados mientras se envía.
- **Logs:** ningún `Log`, `println` ni `printStackTrace` en el cliente, los controladores ni las pantallas nuevas; no se
  registran nombres de apps.
- **Silencios:** quitarlos no existe en la app (D-09). El `security-reviewer` no estaba disponible como agente en la
  sesión.

## 5. Fallos del camino

- **Capturas por adb:** deslizar la galería larga con `input swipe` saltaba de forma errática. Se resolvió poniendo los
  frames del S46 al principio.
- **Galería tapada:** el frame con `silenceTarget` abría un diálogo modal sobre toda la galería (lo detectó DeepSeek).

## 6. No verificado

- Aspecto en el Galaxy S25 FE.

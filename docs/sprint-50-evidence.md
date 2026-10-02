# Sprint 50 — Evidencia

## 1. Android

```text
Parte de Claude (12fa80f): BUILD SUCCESSFUL in 5m 17s · JVM 160, 0 fallos (5 nuevos) · instrumentados 124, 0 fallos · lint 0 errores, 24 avisos
Tras DeepSeek (8d16c2b, su informe): instrumentados 138, 0 fallos (14 nuevos) · lint 0 errores, 24 avisos
Tras los retoques (9c2daab): ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug → BUILD SUCCESSFUL in 1m 17s · lint 0 errores, 24 avisos
                             (connectedDebugAndroidTest no se repitió: el emulador se cerró por falta de memoria del equipo)
```

Tests JVM nuevos (`ServiceStatusTest`):
- las esperas de reconexión (1, 2, 4, 8, 16, 30, 30 s), sin desbordar en un corte largo;
- solo 4404 detiene los reintentos;
- el reporte está «Activo» solo con un latido reciente;
- el control de apps necesita el servicio y su marca de vida;
- el registro sigue lo que avisan los servicios.

## 2. Prueba real (emulador supervisado, backend de desarrollo)

**Reconexión (B-02):**

```text
15:40:54 WebSocket …/da5e6b92…/ws [accepted]        (al abrir la app)
15:41:03 backend: Shutting down                     (docker compose restart backend)
15:41:07 backend: Application startup complete.
15:41:16 WebSocket …/da5e6b92…/ws [accepted]        → reconectado 9 s después de volver el backend

15:42:05 modo avión ON  /  15:43:05 modo avión OFF
15:43:38 WebSocket …/da5e6b92…/ws [accepted]        → reconectado 33 s después de volver la red
```

**Consentimiento.** El tutor se simuló con un WebSocket propio desde el contenedor del backend (token de la cuenta de
tutor de desarrollo). Se registraron solo los tipos de mensaje, nunca SDP ni ICE:

| Caso | Lo que recibió el tutor | Pantalla del teléfono | Auditoría |
|---|---|---|---|
| «Ahora no» | `screen_share_consent granted=False` | vuelve a Dispositivo vinculado | `SCREEN_SHARE_CONSENT_DENIED` |
| Casilla + «Continuar», diálogo de Android cerrado sin aceptar | `granted=True` y luego `screen_share_stop reason=projection_cancelled` | foco en `com.android.systemui` (diálogo «Share your screen with NetProtect?») y vuelta | `CONSENT_GRANTED`, `STOPPED` |
| El tutor cancela (`screen_share_stop`) | — | vuelve sola a Dispositivo vinculado | `STOPPED` |
| 2 min sin respuesta | nada | a los 108 s sigue en Consentimiento; a los 130 s, en Dispositivo vinculado | solo `SCREEN_SHARE_REQUESTED` |

Durante la primera prueba del tiempo límite, el dueño tocó el emulador: marcó la casilla, aceptó y salió del selector
de apps. Se detectó por la auditoría y el logcat, y la prueba se repitió sin tocarlo con el resultado de la tabla.

## 3. Revisión de seguridad (hecha por Claude; obligatoria en este sprint)

- **Logs:** ningún `Log`/`println` en el código nuevo. `docker compose logs backend` de toda la prueba: 0 apariciones de
  `sdp`, `candidate`, `token=` o `eyJ`.
- **Invariante 11:** el token sigue encolado como primer frame en cada `connect()`, también en cada reconexión.
- **La captura solo arranca** desde el resultado afirmativo del diálogo de Android
  (`ScreenShareService.start` en el lanzador). El «sí» solo sale de «Continuar», habilitado únicamente con la casilla
  marcada, y la casilla empieza desmarcada en cada petición.
- **Nunca se responde en nombre del menor:** ni el tiempo límite ni «Atrás» envían respuesta.
- **Sin carga extra:** la reconexión espera hasta 30 s entre intentos, sin agotar la batería ni el límite de peticiones,
  y se detiene con 4404.
- **Agente:** el `security-reviewer` no estaba disponible en la sesión.

## 4. Revisión del trabajo de DeepSeek

- `git diff --stat` de `SupervisedShell.kt` y `core/`: vacío.
- El grep de `ajustes`, `revoc`, `redirig`, `Cerrar sesión`, `Intent`, `JSONObject`, `.send(` y colores literales en
  sus archivos: vacío.
- Capturas (`%TEMP%\sprint50-gallery\`) comparadas con los mockups 15 y 17.
- Hallazgo de DeepSeek (hueco de 24 dp tras la flecha): corregido.

## 5. No verificado

- **H-02:** una persona acepta el diálogo y el tutor ve la pantalla en el web.
- Los tests de UI tras `9c2daab`.
- Aspecto en el Galaxy S25 FE.

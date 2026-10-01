# Sprint 49 — Evidencia

## 1. Android — `cd mobile && ./gradlew test assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest`

```text
Parte de Claude (a926fda): BUILD SUCCESSFUL in 5m 28s · JVM 155, 0 fallos (6 nuevos; RuleEvaluatorTest intacto) · instrumentados 116, 0 fallos · lint 0 errores, 24 avisos
Tras DeepSeek (ff8da4a):   BUILD SUCCESSFUL in 5m 11s · instrumentados 124, 0 fallos (8 nuevos) · lint 0 errores, 24 avisos
```

Tests nuevos (`BlockPresentationTest`, JVM):
- los 7 motivos tienen presentación completa (se recorre `BlockReason.entries`);
- los mensajes son los que la pantalla ya usaba;
- solo el límite diario y el semanal prometen un reinicio;
- ninguna pista muestra horas (`\d{1,2}:\d{2}`) ni ofrece desbloquear, pedir o más tiempo;
- la pista de categoría usa el nombre real o ninguno, y nunca «null»;
- la letra pequeña no menciona Ajustes ni «revocar».

## 2. Revisión del trabajo de DeepSeek

- `git status`: solo `BlockScreenContent.kt`, `ScreensGallery.kt`, el test nuevo y el encargo. `core/rules` y
  `BlockPresentation.kt` sin tocar.
- El grep de `Color(0x`, `fontSize`, `RoundedCornerShape(`, `Build.`, `Intent`, `PackageManager`, `Activity`, `Window`,
  `statusBarsPadding`, `Desbloquear`, `Pedir` y `Solicitar` en `BlockScreenContent.kt` sale vacío (DeepSeek reescribió
  un comentario suyo que mencionaba `statusBarsPadding` para que el grep pasara; el código nunca lo usó).
- Hallazgo de DeepSeek: `createComposeRule` solo permite un `setContent` por test; resolvió los tests de bucle
  mutando el estado con `runOnIdle`.
- Galería (9 capturas, `%TEMP%\sprint49-gallery\`) comparada con el mockup 16.

## 3. Prueba real: 7 reglas en el emulador supervisado, YouTube, ventana superpuesta real

Dispositivo `da5e6b92…` (sin reglas al empezar). Para cada motivo se creó **una** regla en la base de datos de
desarrollo (los mismos datos que enviaría el panel web) y se esperó a que el servicio la descargara (sondeo de un
minuto). Tras 80 s de calentamiento con YouTube abierto, se abrió YouTube y se esperó a que el servicio registrara el
bloqueo en `app_rule_events`:

```text
01-BLOCK          BLOQUEADO · motivo registrado por el servicio: BLOCK
02-DAILY_LIMIT    BLOQUEADO · DAILY_LIMIT     (límite de 1 min, con uso de hoy)
03-WEEKLY_LIMIT   BLOQUEADO · WEEKLY_LIMIT    (límite de 1 min, con uso de la semana)
04-SCHEDULE       BLOQUEADO · SCHEDULE        (todos los días, 00:00–23:59)
05-CATEGORY       BLOQUEADO · CATEGORY        (YouTube asignada a STREAMING + regla BLOCK de la categoría)
06-SCHOOL_MODE    BLOQUEADO · SCHOOL_MODE     (horario escolar activo todo el día)
07-DEFAULT_POLICY BLOQUEADO · DEFAULT_POLICY  (política del dispositivo «BLOCK»)
08-AJUSTES        Ajustes NO se bloqueó con la política «solo apps aprobadas» (correcto)
```

Capturas en `%TEMP%\np-sprint49\`. Cada una muestra el distintivo y el color de su motivo, el icono real de YouTube, la
categoría «Streaming» solo en la variante de categoría, el texto del motivo y su pista, sin horas ni «null».

**«Ir al inicio» desde la ventana real:**

```text
antes del toque:   mCurrentFocus=Window{… u0 com.netprotect.app}
después del toque: mCurrentFocus=Window{… u0 com.google.android.apps.nexuslauncher/…NexusLauncherActivity}
```

**Limpieza:** `0 reglas app, 0 reglas categoría, 0 asignaciones` y el dispositivo vuelve a `ALLOW / escolar=false`
(comprobado en la base de datos).

## 4. Revisión de seguridad (ligera, hecha por Claude)

- La categoría que viaja por el `Intent` de `BlockScreenActivity` es solo el nombre de una categoría del enum
  (texto del propio usuario); la actividad sigue sin exportar. Ningún botón desbloquea ni pide tiempo.
- Sin nombres de apps en logs nuevos. `ProtectedPackages` y `RuleEvaluator` no se tocaron.
- El agente `security-reviewer` no estaba disponible en la sesión.

## 5. No verificado

- Aspecto en el Galaxy S25 FE.

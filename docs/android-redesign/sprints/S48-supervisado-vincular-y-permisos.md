# Sprint 48 — Supervisado: estructura, Vincular, Dispositivo vinculado y Permisos (12, 13, 14)

**Prioridad: HIGH** — sin vinculación y permisos no hay datos para el tutor; y partir
`SupervisedScreen` mal **detiene los servicios** (G-19, CRITICAL). **Dueño:** Claude (estructura,
servicios, cliente, B-01) + DeepSeek (pantallas). **Rama:** `sprint-48-supervisado`.
**Mockups:** `12`, `13`, `14`. **Decisiones:** D-01, D-02. `{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

El modo supervisado tiene tres pantallas nuevas (Vincular, Dispositivo vinculado, Permisos) y un
contenedor que mantiene vivos, **exactamente como hoy**, el latido de cada minuto, `SyncWorker`,
`RuleEnforcementService`, `LocationReportingService` y el canal realtime, sin importar la pantalla
visible. B-01 corregido.

## Problema que resuelve

G-19 a G-22. Hoy todo vive en `SupervisedScreen.kt` (587 líneas): presentación y los efectos que
arrancan los servicios.

## Dependencias

S41, S42. (No depende de las pantallas del tutor.)

## Alcance

**Claude (antes de delegar):**
1. `feature/supervised/SupervisedShell.kt`: la máquina de estados actual (comprobando / introduciendo
   código / vinculado) y **todos** los `LaunchedEffect`/`DisposableEffect` de servicios, latido,
   realtime y lanzadores de permisos movidos **sin cambios de comportamiento**; rutas internas
   Vinculado ↔ Permisos (y los huecos para Consentimiento y Servicios de S50). La tarjeta de
   consentimiento actual se mantiene funcionando tal cual hasta S50.
2. `DeviceClient.getMyDevice`: leer `status.last_seen_at` y la lista `tutors` (hoy solo una etiqueta).
3. B-01 (duplicados en `GET /devices/me`): reproducir con un test de integración, corregir lo mínimo,
   `make test`. Commit de backend aparte.
4. Exponer a las pantallas el estado de cada permiso con las funciones que ya existen en
   `core/permissions/` (sin reescribirlas).

**Encargo para DeepSeek:**
- **Vincular** (mockup 12): marca sin subtítulo, "Vincular este dispositivo", "Introduce el código que
  te dio tu tutor para vincular este dispositivo y comenzar a usar NetProtect.", tarjeta "Código de
  vinculación" / "Ingresa el código de 6 dígitos que te dio tu tutor.", `OtpInput` (solo dígitos,
  pegar), "Vincular dispositivo" (habilitado con 6 dígitos; cargando), "¿No tienes un código?
  Pídeselo a tu tutor.", tarjeta "Comprobando vínculo…" cuando corresponde, errores mapeados (código
  inválido o vencido, demasiados intentos, sin conexión) con los textos que fije Claude tras leer
  `pairing.py`. "Cerrar sesión".
- **Dispositivo vinculado** (mockup 13): marca con "DISPOSITIVO SUPERVISADO"; "Dispositivo
  vinculado", "Este dispositivo está vinculado a un tutor y ya forma parte de NetProtect."; tarjeta con
  nombre (`device_name`), `Android {Build.VERSION.RELEASE}` (dato local, verdadero), "Vinculado";
  "Supervisado por" con **todos** los tutores; "Última comunicación: hace N min" (`last_seen_at`) y
  estado; `InfoBanner` "Este dispositivo reporta su estado cada minuto mientras la app esté abierta."
  (`HEARTBEAT_INTERVAL_MS = 60_000L` en `SupervisedScreen.kt`, visto el 28/09); tarjeta "Permisos del dispositivo" con "{n} permisos
  pendientes" o "Todos los permisos están configurados" → Permisos; "Cambiar de modo" ("Salir del modo
  supervisado en este dispositivo."); "Cerrar sesión" con el texto **corregido** "Cierra la sesión en
  este dispositivo." (no desvincula).
- **Permisos** (mockup 14): una `PermissionCard` por permiso con los textos actuales de la app
  (inventario S40) o los del mockup si son equivalentes: "Acceso al uso de apps" (Abrir Ajustes + "Ya
  lo activé, verificar de nuevo"), "Ubicación aproximada" (Permitir ubicación aproximada — solo
  aproximada), "Mostrar sobre otras apps" (Abrir Ajustes), "Protección contra desinstalación" (Activar
  protección; "Permite avisar al tutor si se intenta desinstalar NetProtect; no elimina el dispositivo
  ni cambia contraseñas."). Estado "Pendiente"/"Configurado" re-comprobado al volver a la app.
  `InfoBanner` "Solo solicitamos los permisos necesarios… Puedes revocarlos en cualquier momento desde
  los ajustes del dispositivo." (esto sí es verdad para estos permisos).

## Fuera de alcance

Consentimiento y pantalla de servicios (S50); pantalla de bloqueo (S49); cambiar qué permisos se piden
o cuándo arrancan los servicios; ubicación precisa o en segundo plano.

## Archivos/módulos afectados

`{pkg}/feature/supervised/SupervisedScreen.kt` (se vacía hacia el shell), nuevos
`feature/supervised/SupervisedShell.kt`, `link/LinkDeviceScreen.kt`, `linked/LinkedDeviceScreen.kt`,
`permissions/PermissionsScreen.kt`; `core/network/DeviceClient.kt`; `core/permissions/*` (solo
lectura); backend `api/v1/endpoints/devices.py` o el servicio que arme `/devices/me` + su test (B-01).

## Seguridad

El código de vinculación no se registra; el límite de intentos del backend queda intacto (invariante
5). `security-reviewer` obligatorio (vinculación y servicios).

## Testing

Backend: test de B-01. JVM: conteo de permisos pendientes, validación del código. UI (si D-13 = a):
Vincular (vacío, completo, error), Vinculado, Permisos. **Prueba de servicios**: con el teléfono
supervisado, navegar Vinculado ↔ Permisos durante 3 min y comprobar en el backend que siguen llegando
latidos, uso y ubicación, y que el bloqueo sigue aplicándose (misma evidencia que en S40).

## Criterios de aceptación

- [ ] Los servicios siguen funcionando al navegar entre pantallas (evidencia del backend).
- [ ] B-01 corregido con test.
- [ ] Vincular con código real del tutor funciona; errores legibles.
- [ ] Estado de permisos correcto al volver de Ajustes.
- [ ] "Cerrar sesión" no dice que desvincula.
- [ ] Capturas contra 12, 13 y 14; `make test` y `test assembleDebug lintDebug` verdes.

## Definition of Done

La común del HANDOFF.

## Riesgos

Efectos que se reinician al navegar (claves de `LaunchedEffect` cambiadas por error). Pedir permisos
desde un contexto que no es una actividad visible (Android lo rechaza).

## Decisiones técnicas

Los efectos viven en el shell; las pantallas solo reciben estado y callbacks.

---

## PROMPT A — Claude Code

```
Sprint 48 del rediseño Android de NetProtect: estructura del modo supervisado, Vincular, Dispositivo
vinculado y Permisos, y el bug B-01. Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este
sprint (docs/android-redesign/sprints/S48-supervisado-vincular-y-permisos.md), mockups/README.md
§12–14, los mockups 12, 13 y 14, INVENTARIO.md (parte supervisada), docs/sprint-29.md (B-01).
Código: feature/supervised/SupervisedScreen.kt entero, core/permissions/*, DeviceClient.kt,
PairingClient.kt, backend/app/api/v1/endpoints/{devices,pairing}.py.

Fases 1–3; espera mi OK. Explícame antes de tocar nada qué efectos arrancan qué servicios y con qué
claves. Implementa tu parte (shell con los efectos movidos sin cambios, DeviceClient, B-01 con test,
commit de backend aparte). Prueba de servicios de 3 min navegando. verifier. Commits con mi OK.
Después escribe docs/delegated/pending/sprint-48-supervisado.md (formato del Sprint 39) con las tres
pantallas; deja claro que DeepSeek NO toca SupervisedShell.kt ni los efectos. Commit docs(sprint-48).
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-48-supervisado.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-48-supervisado.md. Lee su informe; commit de lo suyo tal
cual con mi OK. Fases 6–10 del HANDOFF:
- Diff: ¿tocó SupervisedShell o algún efecto? ¿textos falsos ("desvincula", ubicación precisa)?
- verifier: test assembleDebug lintDebug (+ UI tests).
- Repite la prueba de servicios de 3 min navegando; vincula de cero con un código real del tutor;
  concede y retira permisos y comprueba el estado al volver.
- Capturas en %TEMP%\np-sprint48\ contra 12, 13 y 14. security-reviewer. docs, /cerrar-sprint 48,
  informe del §9. Commits separados con mi OK.
```

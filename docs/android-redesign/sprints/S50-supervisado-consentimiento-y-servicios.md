# Sprint 50 — Supervisado: Consentimiento de vista remota, Estado de NetProtect y reconexión realtime (15, 17)

**Prioridad: HIGH** — privacidad y consentimiento del menor; transparencia de lo que corre en su
teléfono; riesgo técnico alto (realtime, `MediaProjection`, servicios). **Dueño:** Claude (casi todo) +
DeepSeek (maquetación de las dos pantallas). **Rama:** `sprint-50-consentimiento`.
**Mockups:** `15-supervisado-consentimiento.jpg`, `17-supervisado-servicios.jpg`. **Decisiones:** D-12.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Cuando el tutor pide ver la pantalla, el supervisado ve una pantalla completa de consentimiento con
información verdadera; mientras la vista remota está activa hay aviso visible dentro de la app además
de la notificación; existe una pantalla "Estado de NetProtect" que dice qué está corriendo de verdad;
y el canal realtime se reconecta solo, para que ninguna solicitud se pierda (B-02).

## Problema que resuelve

G-24, G-25, G-26. Hoy el consentimiento es una tarjeta dentro de la pantalla; no hay pantalla de
servicios; si el canal realtime se cae, la solicitud del tutor puede no llegar nunca.

## Dependencias

S48 (shell supervisado con los huecos de rutas). D-12 resuelta.

## Alcance

**Claude:**
1. B-02: reconexión del canal realtime del supervisado con espera creciente y tope, re-autenticación
   (el primer frame sigue siendo el token, invariante 11), y cierre limpio al salir del modo. Test de
   integración o, si no es posible en JVM, prueba real documentada (apagar/encender el backend y la red).
2. `core/status/ServiceStatusRegistry` (objeto de proceso con `StateFlow`): hora del último latido
   correcto; control de apps activo (`EnforcementLiveness`); servicio de ubicación corriendo; vista
   remota activa. Lo actualizan los propios servicios (`onStartCommand`/`onDestroy`) y el bucle del
   latido. Sin cambiar qué hacen los servicios.
3. Enrutado: `screen_share_request` → pantalla Consentimiento; `screen_share_stop` o timeout → volver.
   El envío de `screen_share_consent` true/false y el lanzamiento del diálogo del sistema quedan
   **idénticos**.
4. Confirmar en `docs/sprint-23.md` y `ScreenShareService` cada afirmación que irá en pantalla ("no se
   graba", "la detienes desde la notificación").

**Encargo para DeepSeek (solo presentación; recibe estado y callbacks ya hechos):**
- **Consentimiento** (mockup 15, D-12): "Tu tutor quiere ver esta pantalla" como título o
  "Consentimiento para ver la pantalla" (lo fija Claude); "¿Qué permite?" — "Que tu tutor vea la
  pantalla de este dispositivo en tiempo real, solo mientras dure esta sesión."; "¿Cuándo se puede
  usar?" — "Solo cuando el tutor lo solicita", "Con fines de supervisión", "Puedes detenerla en
  cualquier momento desde la notificación."; si D-12 = a, casilla "Autorizo a mi tutor a ver la
  pantalla de este dispositivo ahora" que habilita "Continuar"; "Ahora no"; `InfoBanner`: "Android te
  pedirá confirmarlo en un diálogo del sistema. Mientras se comparta verás un aviso permanente. No se
  graba ni se controla el dispositivo." (**no** "revocar desde los ajustes", **no** "se te redirigirá
  a los ajustes").
- **Estado de NetProtect** (mockup 17), desde `ServiceStatusRegistry`: "Reporte del dispositivo" con
  "Último reporte: hace N min" y "El dispositivo envía su estado cada minuto mientras la app esté
  abierta."; "Control de apps" activo/inactivo; "Ubicación" activa/inactiva; "Visualización de
  pantalla" solo si está activa ("Tu tutor está viendo la pantalla con tu consentimiento." + cómo
  detenerla); explicación de las notificaciones persistentes (con los títulos reales de las 3
  notificaciones); `InfoBanner` de privacidad; "Volver al inicio".
- Aviso en la app mientras la vista remota esté activa (banda superior) en las pantallas del supervisado.
- Acceso a "Estado de NetProtect" desde Dispositivo vinculado.

## Fuera de alcance

Ver la pantalla desde el móvil del tutor (es del web); grabar o capturar; consentimiento permanente
(el mecanismo de Android no lo permite); cambiar WebRTC/TURN.

## Archivos/módulos afectados

Claude: `core/network/RealtimeClient.kt`, `core/screenshare/ScreenShareService.kt`,
`core/location/LocationReportingService.kt`, `core/rules/RuleEnforcementService.kt` (solo avisos al
registro), nuevo `core/status/ServiceStatusRegistry.kt`, `feature/supervised/SupervisedShell.kt`.
DeepSeek: nuevos `feature/supervised/consent/ScreenShareConsentScreen.kt`,
`feature/supervised/services/ServicesStatusScreen.kt`, `ui/components/ActiveShareBanner.kt`,
`feature/supervised/linked/LinkedDeviceScreen.kt` (fila de acceso).

## Trabajo por capa

Backend, web, BD: ninguno (Claude confirma en `backend/app/services/realtime.py` que no hace falta).

## Seguridad

Invariantes 7 (sin SDP/ICE ni tokens en logs), 8 (TURN efímero), 11 (primer frame = token). El
consentimiento solo se envía tras el gesto explícito. La vista remota no puede empezar sin el diálogo
del sistema. `security-reviewer` **obligatorio**.

## Testing

JVM: política de reconexión (tiempos, tope, cancelación), registro de estado. Real: tutor pide ver la
pantalla desde el panel web → aparece la pantalla de consentimiento → "Ahora no" (el web lo refleja) →
nueva petición → aceptar → diálogo del sistema (**pendiente humano H-02** si no hay persona) → aviso en
la app + notificación → "Detener" en la notificación → todo se apaga y la pantalla de servicios lo
refleja. Cortar la red 1 min y volver: una nueva petición llega (B-02).

## Criterios de aceptación

- [ ] Ningún texto falso sobre revocar desde ajustes o redirigir a ajustes.
- [ ] Aceptar/Ahora no envían lo mismo que antes (evidencia del canal en el backend, sin SDP en logs).
- [ ] Aviso visible en la app durante la vista remota.
- [ ] Estado de NetProtect coincide con la realidad (probar con cada servicio encendido/apagado).
- [ ] Reconexión probada con corte de red.
- [ ] `security-reviewer` sin hallazgos ALTA; `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF + H-02 declarado si no hubo persona.

## Riesgos

Reconexión agresiva que agota batería o dispara el rate limit (tope y espera creciente). Registro de
estado que miente si un servicio muere sin `onDestroy` (usar la marca de vida existente y mostrar "sin
confirmar" si es antigua).

## Decisiones técnicas

La pantalla de servicios muestra "Sin confirmar" antes que un "Activo" que no se pueda asegurar.

---

## PROMPT A — Claude Code

```
Sprint 50 del rediseño Android de NetProtect: consentimiento de vista remota, Estado de NetProtect y
reconexión realtime (B-02). Es mayormente tuyo: realtime, MediaProjection y servicios no se delegan.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S50-supervisado-consentimiento-y-servicios.md), mockups/README.md §15 y
§17, los mockups 15 y 17, D-12, docs/sprint-23.md, docs/sprint-29.md (B-02).
Código: SupervisedShell.kt, core/network/RealtimeClient.kt, core/screenshare/*, core/location/
LocationReportingService.kt, core/rules/{RuleEnforcementService,EnforcementLiveness}.kt,
backend/app/services/realtime.py (solo lectura).

Fases 1–3; espera mi OK. Implementa B-02, ServiceStatusRegistry y el enrutado del consentimiento,
con tests; verifier; security-reviewer; commit propio con mi OK. Después escribe
docs/delegated/pending/sprint-50-consentimiento.md (formato del Sprint 39) solo con la presentación
de las dos pantallas y la banda de aviso, con los textos exactos ya verificados. Commit docs(sprint-50).
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-50-consentimiento.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-50-consentimiento.md. Lee su informe; commit de lo suyo
tal cual con mi OK. Fases 6–10 del HANDOFF: diff (¿tocó core/ o el shell?, ¿textos falsos?),
verifier, prueba real completa del flujo de vista remota (guíame; H-02 si no hay persona) y de la
reconexión con corte de red; capturas en %TEMP%\np-sprint50\ contra 15 y 17; security-reviewer
obligatorio. docs, /cerrar-sprint 50, informe del §9. Commits separados con mi OK.
```

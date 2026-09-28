# Sprint 44 — Tutor: estructura, Inicio, Dispositivos y Detalle (pantallas 3 y 4)

**Prioridad: HIGH** — desbloquea S45–S47 y arregla una acción destructiva sin confirmación.
**Dueño:** Claude (estructura, navegación, cliente) + DeepSeek (pantallas). **Rama:**
`sprint-44-tutor-inicio`. **Mockups:** `03-tutor-inicio.jpg`, `04-tutor-detalle-dispositivo.jpg`.
**Decisiones:** D-01, D-02, D-08. `{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

El modo tutor pasa de una columna de 963 líneas a una app con barra inferior (según D-08): Inicio
(vinculación + dispositivos), Dispositivos, Actividad, Más. Inicio y Detalle del dispositivo tienen el
diseño nuevo; las seis secciones del dispositivo siguen accesibles (con su aspecto antiguo, movido
sin cambios) hasta que S45–S47 las rediseñen. **Ninguna funcionalidad se pierde en ningún commit.**

## Problema que resuelve

G-06, G-08, G-09, G-10, G-11: sin navegación, monolito, cuenta atrás falsa ("Válido por 3 minutos"
fijo), `os_version` sin leer, desvincular sin confirmación y con errores silenciados.

## Dependencias

S41 (llamadas autenticadas y `UiError`), S42 (componentes y navegación). D-01, D-02, D-08 resueltas.

## Alcance

**Claude (antes de delegar):**
1. `feature/tutor/TutorShell.kt`: rutas (Inicio, Dispositivos, Detalle(id), Apps(id), Ubicación(id),
   Geocercas(id), Historial(id), Estadísticas(id), Alertas(id), MiActividad, Más), barra inferior,
   "atrás" del sistema, guardado de la pila al rotar.
2. Mover **tal cual** los composables de sección de `TutorScreen.kt` (apps, ubicación, geocercas,
   historial, estadísticas, alertas, auditoría) a `feature/tutor/legacy/LegacySections.kt` y
   enrutarlos desde las rutas correspondientes, con su carga de datos. Sin cambiar su aspecto.
3. `DeviceClient`: leer `os_version` y `app_version` del JSON (ya vienen del backend).
4. Patrón de estado para las pantallas nuevas según D-02 (un ejemplo completo en `TutorHomeScreen`
   que DeepSeek replique): cargar, recargar, `UiError`, sin estados imposibles.
5. `TutorScreen.kt` queda como punto de entrada que llama a `TutorShell` (o desaparece si
   `HomeScreen` llama directo al shell).

**Encargo para DeepSeek:**
- **Inicio** (mockup 3): `BrandHeader` "PANEL DEL TUTOR" + avatar con inicial; "Modo Tutor" y
  "Supervisa y gestiona los dispositivos vinculados."; "Cambiar de modo". Tarjeta de vinculación con
  estados: *sin código* ("Vincula un dispositivo nuevo", "Genera un código temporal para vincular un
  teléfono o tablet supervisado.", botón "Generar código de vinculación"), *generando*, *código
  activo* (`CodeDisplay` con los 6 dígitos, cuenta atrás mm:ss desde `expiresInSeconds` medida con
  reloj monótono, "Uso único.", botón "Revocar"), *vencido* ("El código venció. Genera uno nuevo."),
  *error*. Enlace "¿Ya se vinculó? Actualizar lista". "Dispositivos vinculados" + "Actualizar":
  filas con nombre, `Android {os_version}` (si falta, "Android"), `StatusPill`, "Última actividad:
  hace N min" y "Los datos se sincronizan periódicamente." Vacío: "Todavía no hay dispositivos
  vinculados." Carga y error. Fila "Mi actividad (auditoría)". `InfoBanner` "Sobre la información de
  los dispositivos: La ubicación, el uso de apps y las geocercas se sincronizan periódicamente y
  pueden tener un retraso de algunos minutos." "Cerrar sesión".
- **Dispositivos**: la misma lista (sin tarjeta de vinculación).
- **Detalle** (mockup 4): una sola cabecera de dispositivo (el mockup la repite: basta una);
  "Renombrar" en línea (campo "Nombre", 1–255 caracteres sin espacios sobrantes, "Guardar" deshabilitado
  si no es válido, "Cancelar", error visible); "Actualizar"; "Desvincular dispositivo" en tarjeta de
  peligro con `ConfirmDialog` ("¿Desvincular {nombre}?", "Se eliminará la conexión de este
  dispositivo con tu cuenta de tutor. Para volver a supervisarlo habrá que vincularlo de nuevo.",
  "Desvincular" / "Cancelar"); al desvincular con éxito vuelve a la lista; si falla, mensaje. Filas:
  Apps, Ubicación, Geocercas, Historial, Estadísticas, Alertas (textos del mockup) → rutas del shell.
  404 → "Este dispositivo ya no existe o no tienes acceso." y volver a la lista.
- **Más** (según D-08): "Cambiar de modo", "Cerrar sesión", "Acerca de NetProtect" (versión de la app
  y una frase de privacidad aprobada por Claude en el encargo).

## Fuera de alcance

Rediseñar las secciones (S45–S47); crear reglas, geocercas o vista remota desde el móvil; cambiar
endpoints; notificaciones push nuevas.

## Archivos/módulos afectados

`{pkg}/feature/tutor/TutorScreen.kt`, nuevos `feature/tutor/TutorShell.kt`,
`feature/tutor/legacy/LegacySections.kt`, `feature/tutor/home/TutorHomeScreen.kt`,
`feature/tutor/devices/DevicesScreen.kt`, `feature/tutor/device/DeviceDetailScreen.kt`,
`feature/tutor/more/MoreScreen.kt`; `core/network/DeviceClient.kt`; `feature/home/HomeScreen.kt` (solo
la llamada al shell).

## Trabajo por capa

- **Backend:** ninguno. Claude confirma que `PATCH /devices/{id}` y `DELETE /devices/{id}/link` tienen
  tests de autorización (barrido del Sprint 25).
- **Web / BD:** ninguno.

## Seguridad

El código de vinculación no va a logs. Desvincular exige confirmación explícita. La UI no decide
permisos: un 404 del backend se respeta. `security-reviewer` ligero (vinculación, desvincular).

## Testing

JVM: cuenta atrás (tiempo inyectado: activo → vencido), validación del nombre. UI (si D-13 = a):
Inicio con lista vacía / error / datos; diálogo de desvincular. Manual: generar código, vincular el
teléfono real, ver la cuenta atrás llegar a 0, revocar, renombrar, desvincular (y cancelar),
rotar la pantalla en Detalle, "atrás" del sistema en cada nivel.

## Criterios de aceptación

- [ ] Barra inferior y "atrás" funcionan; rotar no pierde la pantalla actual.
- [ ] Cuenta atrás real; al vencer, estado vencido; "Revocar" funciona.
- [ ] Desvincular pide confirmación; errores de renombrar/desvincular visibles.
- [ ] `Android {versión}` desde la API.
- [ ] Las 6 secciones y la auditoría siguen accesibles y funcionando (aspecto antiguo).
- [ ] Capturas de Inicio (sin código, código activo, vencido, vacío, error) y Detalle revisadas.
- [ ] `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF.

## Riesgos

Perder funciones al partir el monolito (mitigado con `LegacySections` movido tal cual y el
inventario). Pila de navegación que no se guarda al rotar. Cuenta atrás que sigue corriendo en
segundo plano (usar tiempo transcurrido, no un contador por tics).

## Decisiones técnicas

Estado por pantalla según D-02. La lista de "Dispositivos" y la de "Inicio" comparten un componente.

---

## PROMPT A — Claude Code

```
Sprint 44 del rediseño Android de NetProtect: estructura del modo tutor, Inicio, Dispositivos,
Detalle y Más. Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S44-tutor-inicio-y-detalle.md), mockups/README.md §3–4, los mockups
03 y 04, las decisiones D-01, D-02 y D-08, y docs/android-redesign/INVENTARIO.md (parte del tutor).
Código: feature/tutor/TutorScreen.kt, core/network/{DeviceClient,PairingClient}.kt, ui/navigation,
ui/components.

Fases 1–3 del HANDOFF; espera mi OK. Luego implementa tu parte (TutorShell, LegacySections movido
tal cual, DeviceClient con os_version/app_version, patrón de estado de ejemplo). verifier +
humo contra INVENTARIO.md: nada perdido. Commit feat(sprint-44) de tu parte con mi OK.
Después escribe docs/delegated/pending/sprint-44-tutor-inicio.md (formato del Sprint 39) con las
pantallas Inicio, Dispositivos, Detalle y Más: textos literales, componentes, estados, lista cerrada
de archivos, el patrón de estado que DeepSeek debe copiar, pasos y verificación. Commit docs(sprint-44).
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-44-tutor-inicio.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code

```
DeepSeek terminó docs/delegated/done/sprint-44-tutor-inicio.md. Lee su informe; commit de lo suyo
tal cual con mi OK. Fases 6–10 del HANDOFF:
- Diff contra el encargo (archivos, colores literales, lógica fuera de lugar, datos inventados).
- verifier: test assembleDebug lintDebug (+ UI tests).
- Emulador como tutor y teléfono como supervisado: capturas en %TEMP%\np-sprint44\ de Inicio (sin
  código, código activo, vencido, vacío, error) y Detalle (normal, renombrando, diálogo); compáralas
  con 03 y 04 y corrige.
- Prueba desvincular (confirmar y cancelar), renombrar con error (backend apagado), rotación, atrás.
- security-reviewer ligero. docs, /cerrar-sprint 44, informe del §9. Commits separados con mi OK.
```

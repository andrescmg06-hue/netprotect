# Sprint 43 — Login y Elegir modo (pantallas 1 y 2)

**Prioridad: HIGH** — primera impresión y puerta de todo; riesgo técnico bajo (sin cambiar el flujo de
autenticación). **Dueño:** DeepSeek (UI) + Claude (revisión, B-03). **Rama:** `sprint-43-login`.
**Mockups:** `01-login.jpg`, `02-elegir-modo.jpg`. **Decisiones:** ninguna nueva.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Las pantallas de carga, inicio de sesión y elección de modo se ven como los mockups 1 y 2, con los
componentes de S42, y se comportan exactamente igual que hoy (mismo flujo de Credential Manager, mismo
`RoleClient`, misma `RolePreference`).

## Problema que resuelve

Hoy son pantallas oscuras con colores literales dentro de `HomeScreen.kt`, que mezcla el enrutador de
estados con la presentación.

## Dependencias

S41 (mensaje de sesión expirada en el login), S42 (tema y componentes).

## Alcance

1. Separar presentación: `feature/home/LoginScreen.kt`, `feature/home/RoleSelectionScreen.kt`,
   `feature/home/LoadingScreen.kt`. `HomeScreen.kt` conserva la máquina de estados y las llamadas.
2. **Login** (mockup 1): `BrandHeader` **sin** el subtítulo "PANEL DEL TUTOR" que dibuja el mockup
   (en el login aún no hay modo y también entra el supervisado), título "Control parental". Texto: "Inicia sesión con tu cuenta de Google para continuar como tutor o como
   dispositivo supervisado." Botón "Iniciar sesión con Google" con la "G" y flecha. Línea de estado:
   "Comprobando servicio…" / "Servicio listo" (punto verde) / "Servicio no disponible" + "Reintentar".
   Ya no se muestran BD/Redis por separado (misma decisión que el login web, Sprint 39 D2). Error de
   inicio de sesión visible dentro de la tarjeta; cancelar el selector de cuentas no es error. Pie:
   "Solo puedes iniciar sesión con Google."
3. **Elegir modo** (mockup 2): logo sin subtítulo, "Hola, {nombre}" (o el correo si no hay nombre),
   "¿Cómo vas a usar este dispositivo?", "Selecciona el modo que mejor describa cómo vas a utilizar
   esta app.", dos tarjetas ("Soy tutor" con `users`, botón primario; "Este es el dispositivo
   supervisado" con `smartphone`, botón secundario), error, "Cerrar sesión".
4. Estado de carga con marca y `LoadingState`.
5. B-03 (misma cuenta como tutor y supervisado): Claude averigua en la fase A qué responde
   `POST /users/me/roles` en ese caso. Si hay un error distinguible, el encargo incluye el mensaje
   claro; si hace falta cambiar el backend, se anota en `tasks.md` y **no** se hace aquí.

## Fuera de alcance

Cambiar `AuthRepository`, Credential Manager, `RoleClient`, `RolePreference`, el orden de estados;
registro por correo; "olvidé mi contraseña" o cualquier opción que no sea Google.

## Archivos/módulos afectados

`{pkg}/feature/home/HomeScreen.kt` (solo presentación), nuevos `LoginScreen.kt`,
`RoleSelectionScreen.kt`, `LoadingScreen.kt`. Se usan (sin modificar) `ui/theme`, `ui/components`,
`core/network/InfrastructureHealthClient.kt`.

## Trabajo por capa

Backend, web, base de datos: ninguno (salvo lectura para B-03).

## Seguridad

Pantalla de autenticación: sin cambios de lógica, sin mostrar detalles de infraestructura (se retiran
BD/Redis), sin registrar el correo en logs. `security-reviewer` ligero sobre el diff.

## Testing

Si D-13 = a: tests de UI de `LoginScreen` (comprobando / listo / no disponible / error) y
`RoleSelectionScreen` (error visible). JVM: nada nuevo salvo que se extraiga lógica. Manual en
teléfono: login real (pendiente humano H-01 si no hay cliente OAuth), elegir cada modo, cerrar sesión,
backend apagado → "Servicio no disponible" → encenderlo → "Reintentar" → "Servicio listo".

## Criterios de aceptación

- [ ] Capturas de login (3 estados de servicio + error) y de elegir modo, revisadas contra los mockups.
- [ ] Mismo comportamiento que el inventario de S40 para estas pantallas.
- [ ] Sin colores literales; componentes de `ui/components`.
- [ ] `test assembleDebug lintDebug` verde.

## Definition of Done

La común del HANDOFF.

## Riesgos

Romper el flujo de sesión al mover código (por eso la lógica no se mueve). El botón "Reintentar" no
debe lanzar comprobaciones en bucle.

## Decisiones técnicas

"Servicio listo" solo cuando backend, BD y Redis responden `ok` en `/health/ready`; cualquier otro caso
es "no disponible". El detalle técnico no se muestra al usuario.

---

## PROMPT A — Claude Code

```
Sprint 43 del rediseño Android de NetProtect: pantallas Login y Elegir modo.
Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md, este sprint
(docs/android-redesign/sprints/S43-login-y-elegir-modo.md), docs/android-redesign/mockups/README.md
§1–2 y los mockups 01-login.jpg y 02-elegir-modo.jpg. Código: feature/home/HomeScreen.kt,
core/network/InfrastructureHealthClient.kt, ui/components (lo que existe) y, para B-03,
backend/app/api/v1/endpoints/roles.py.

Fases 1–3 del HANDOFF; espera mi OK. Luego escribe el encargo
docs/delegated/pending/sprint-43-login.md con el formato de
docs/delegated/done/sprint-39-login-reemplazo.md: textos literales en español, componentes a usar,
lista cerrada de archivos permitidos, "no mover lógica de HomeScreen", pasos con compileDebugKotlin,
verificación y capturas. Incluye lo que aprendiste de los errores de DeepSeek en S42 (docs/sprint-42.md).
Commit docs(sprint-43) con mi OK. Dime cuándo abrir OpenCode.
```

## PROMPT B — OpenCode con DeepSeek V4 Pro

```
Ejecuta el encargo docs/delegated/pending/sprint-43-login.md siguiendo AGENTS.md.
```

## PROMPT C — Claude Code (revisar, corregir, cerrar)

```
DeepSeek terminó docs/delegated/done/sprint-43-login.md. Lee su informe. Propón el commit de su
trabajo tal cual ("feat(sprint-43): … (DeepSeek, sin revisar)") y espera mi OK.
Después, fases 6–10 del HANDOFF para S43:
- Diff contra el encargo: archivos fuera de lista, lógica movida de HomeScreen, colores literales.
- verifier: test assembleDebug lintDebug (+ UI tests si D-13 = a).
- Capturas en el teléfono (%TEMP%\np-sprint43\): login comprobando/listo/no disponible/error y elegir
  modo; compáralas tú con 01-login.jpg y 02-elegir-modo.jpg y corrige diferencias de composición.
- Humo: login, elegir tutor, volver, elegir supervisado, cerrar sesión (contra INVENTARIO.md).
- security-reviewer ligero. docs, /cerrar-sprint 43, informe del §9. Commits separados con mi OK.
```

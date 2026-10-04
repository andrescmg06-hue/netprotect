# Sprint 59 — Rediseño editorial del panel web, zonas sensibles: Vista remota, Auditoría, Perfil

Octavo sprint del rediseño (`docs/redesign/PLAN_SPRINTS.md` §4). Código de Cristian del 04/10/2026: `7ec38c7` (10:00) y
`ccde5cf` (10:11, cableado de contexto del dispositivo); correcciones y verificación con datos reales en `eb2db05`
(10:43). Documento escrito **después**, en el S60 (T1), a partir del mensaje y de las estadísticas de los commits y de
`docs/redesign/s59/LEEME.md`.

## Objetivo

Que las tres vistas que quedaban construidas como tarjetas con cabecera pasen a la dirección editorial **sin cambiar su
comportamiento** de tiempo real, auditoría y autenticación. Este sprint también concentra la **verificación integrada
de S54–S59** con datos reales (ver `docs/sprint-59-evidence.md`).

## Vistas recompuestas

| Vista (`SectionKey`) | Qué cambió (según los commits) |
|---|---|
| Vista remota (`remote`) | El marco del teléfono se dibuja siempre y contiene el mismo `<video>` siempre montado (mismo ref, `autoPlay`/`playsInline`/`muted`, misma regla de ocultación), así la pista WebRTC sigue llegando a un nodo que nunca se desmonta. Una columna de estado al lado muestra «EN VIVO» con el tiempo real transcurrido, el estado de conexión, el dispositivo (prop opcional, solo para mostrar), el aviso de consentimiento existente, el paso a paso y «Finalizar vista remota» (el mismo `stop()`). |
| Auditoría (`audit`) | Un panel abierto con título, filtros de texto libre, exportación CSV, tabla sobria y la misma paginación de 20 por página. Las filas de dispositivo leen «Dispositivo» más el nombre si se le pasan los dispositivos, o el identificador si no. El estado vacío usa un dibujo pequeño de documentos de lucide. |
| Perfil y sesión (`account`) | Una página de ajustes con secciones separadas por líneas en lugar de tarjetas flotantes. El `Field` local pasa a llamarse `Fact` para no tapar a `ui/Field`. |

`ccde5cf`: `DashboardShell` entrega a Vista remota y a Auditoría el nombre y la versión de Android del dispositivo
supervisado desde la lista que ya tiene, **sin petición nueva**. Ambos paneles lo reciben como prop opcional.

## Decisiones

- **Se omite lo que ningún modelo tiene**: batería, red, rotar y pantalla completa en Vista remota; en Auditoría,
  «Limpiar», PDF/Excel, columna Usuario y selector de dispositivo; en Perfil, edición de perfil, subida de foto y lista
  de sesiones (la sesión es solo Google).
- **Restricción dura cumplida por construcción**: el `<video>` sigue siempre montado (afirmación del commit; la prueba
  con una sesión real de vista remota **no se hizo**, ver Pendiente).
- Serif definitiva: cambio de Playfair a Newsreader (`eb2db05`), registrado en `docs/sprint-54.md`; sigue abierta para el
  diseñador.

## Tareas

Ruta: no consta en las fuentes; no se inventa.

- [x] T1 — `RemoteViewPanel` recompuesto con el `<video>` siempre montado (`7ec38c7`).
- [x] T2 — `AuditPanel` recompuesto (`7ec38c7`).
- [x] T3 — `AccountPanel` recompuesto (`7ec38c7`).
- [x] T4 — Contexto del dispositivo (nombre, versión de Android) en Vista remota y Auditoría desde `DashboardShell`
  (`ccde5cf`).
- [x] T5 — Verificación integrada con datos reales: sembrado por la API real, 34 capturas fuera del repo (18 dentro),
  E2E «1 passed, 15,7 s», sin errores de consola ni HTTP >= 400, sin desbordes horizontales
  (`docs/redesign/s59/LEEME.md`; detalle en `docs/sprint-59-evidence.md`).
- [x] T6 — Defectos de la verificación corregidos en el código (`eb2db05`): Auditoría muestra etiquetas de recurso en
  español en vez de nombres de la API (`lib/auditFormatting.ts`), el perfil y el correo parten bien en línea, y los de
  las demás vistas (ver S55–S58). Sin recaptura.
- [ ] T7 — Prueba de Vista remota con una sesión real (un teléfono compartiendo pantalla). **No hecha**: el LEEME solo
  la vio en estado inactivo. Pendiente del dueño.
- [ ] T8 — Revisión con `security-reviewer`: el plan (§4, S59) la exige (toca realtime, audit y auth); las fuentes
  **no registran** que se haya hecho.
- [ ] T9 — `npm run lint` y `tsc`: **sin registro** para `7ec38c7`, `ccde5cf` ni `eb2db05`.
- [ ] T10 — Capturas a 1440 y 390 px de cada vista en el repo. Hay `remote-1440`, `audit-1440` y `account-1440`; faltan
  las tres de 390 px (existen en el juego de 34 fuera del repo).
- [x] T11 — Documentación de seguimiento y evidencia (este archivo y `docs/sprint-59-evidence.md`), escrita en el S60.

## Pendiente conocido

- Vista remota real con una persona: pendiente (T7). Es la verificación que el plan declara obligatoria para este sprint.
- Revisión de seguridad (T8) sin registro; además de este sprint, afecta a S57 (ubicación).
- El LEEME registra el HEAD `8be5abd` y dice que las capturas se tomaron **antes** de `eb2db05`: muestran los
  defectos, no sus correcciones, y el título en Playfair. No hay verificación posterior de las correcciones.
- Contradicción menor entre fuentes: el LEEME dice «árbol limpio salvo `compose.web.yaml` sin versionar»; ese fichero se
  versionó después en `857f60f`.
- La evidencia de S54–S58 está concentrada en este mismo LEEME (título «Sprints 54-59», carpeta `s59`), no repartida por
  sprint; los `-evidence.md` de S54–S58 remiten a él.
- El login del LEEME muestra «Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.»; el botón real de Google no se capturó.
- No se comprobó con capturas que el cajón de 390 px desplace su lista hasta «Auditoría» y «Cerrar sesión».

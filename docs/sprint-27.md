# Sprint 27 — Documentación y presentación

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 26) pide: "Documento técnico, manuales de instalación,
usuario y administrador, plan de pruebas, análisis de riesgos, modelo de seguridad, política de
privacidad y manual de despliegue."

Es el último de los 27 sprints del roadmap (`docs/planning/roadmap.md`) y el único que no agrega
código: consolida en documentos legibles por una persona que no vivió los 26 sprints anteriores lo
que ya existe, verificado, repartido en `docs/sprint-01.md` … `docs/sprint-26.md`, sus
`-evidence.md`, `docs/security-baseline.md`, `docs/android/capability-matrix.md`,
`docs/architecture.md` y los ADR. No inventa alcance nuevo ni corrige el producto — donde el
producto tiene un límite real (una funcionalidad del plan original que no se construyó, un permiso
pendiente de un humano), estos documentos lo dicen explícitamente en vez de callarlo, siguiendo la
misma regla que gobierna todo el proyecto (`CLAUDE.md`, "la regla que gobierna todo lo demás").

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-081 | Como evaluador del proyecto, quiero un documento técnico único que explique la arquitectura, el modelo de datos y las decisiones de diseño, sin tener que reconstruirlo leyendo 26 documentos de sprint. |
| HU-082 | Como persona que retoma este proyecto en una máquina nueva, quiero un manual de instalación que me lleve de un repositorio recién clonado a los tres carriles (backend, web, Android) corriendo localmente. |
| HU-083 | Como tutor o persona supervisada, quiero un manual de usuario que explique qué puedo hacer con la app y el panel web, sin jerga técnica. |
| HU-084 | Como quien administra una instancia real de NetProtect, quiero un manual de administrador que explique monitorización, backups, rotación de secretos y respuesta a alertas de manipulación. |
| HU-085 | Como responsable de calidad, quiero un plan de pruebas que documente qué se prueba, con qué herramienta y con qué evidencia real, en vez de una promesa de cobertura. |
| HU-086 | Como responsable del proyecto, quiero un análisis de riesgos que liste honestamente lo que no se construyó o quedó limitado, no sólo lo que salió bien. |
| HU-087 | Como responsable de seguridad, quiero un modelo de seguridad formal (activos, amenazas, controles, mapeo a OWASP) que dé contexto a los 36 controles ya listados en `docs/security-baseline.md`. |
| HU-088 | Como usuaria cuyos datos (o los de su hijo/a) pasan por este sistema, quiero una política de privacidad real que diga qué se recolecta, por qué, cuánto se retiene y cómo se protege. |
| HU-089 | Como quien va a desplegar NetProtect en un servidor real, quiero un manual de despliegue paso a paso que use los mismos comandos ya verificados en el Sprint 26, sin reinventarlos. |

## Criterios de aceptación

1. Los nueve documentos existen en `docs/manuals/`, en español, y cada uno cita el sprint o archivo
   de origen de cada afirmación técnica en vez de restablecer hechos de memoria.
2. Ninguna funcionalidad se describe como existente si no lo está — en particular, el control de
   navegación web por `VpnService`/DNS (`docs/planning/plan-desarrollo.md`, Paso 8) se documenta
   como **no construido**, con la razón real (pospuesto desde el Sprint 9 por necesitar su propia
   fase, `docs/sprint-09.md`), no como un rasgo del producto.
3. Cada comando de instalación/despliegue citado en `manual-instalacion.md`/`manual-despliegue.md`
   ya fue ejecutado y verificado en un sprint anterior (Sprint 1, 21, 26) — este sprint no inventa
   comandos nuevos sin probar.
4. `analisis-riesgos.md` incluye tanto los riesgos abiertos originales de
   `docs/planning/plan-desarrollo.md` (sección 5) como los límites reales descubiertos después
   (DNS cifrado, `MediaProjection` no persistente, ausencia de TURN, Firebase/Maps pendientes,
   políticas de Play Store no aplicables por *sideload*, latencia de geofencing de ~15 min).
5. `modelo-seguridad.md` mapea los 36 controles de `docs/security-baseline.md` a categorías de
   amenaza (STRIDE) y a OWASP Top 10 / API Top 10, sin repetir su texto íntegro.
6. `politica-privacidad.md` es coherente con lo que el código realmente hace (cifrado Fernet de
   ubicación desde el Sprint 13, retención de 7/90 días, auditoría sin retención) — verificado
   releyendo el código citado, no sólo los documentos de sprint.
7. `README.md` gana su sección `## Alcance del Sprint 27` (convención de cada cierre de sprint,
   ver `CLAUDE.md`, "Errores que ya se cometieron una vez").
8. CI en GitHub Actions sigue en verde tras el commit de este sprint (no se tocó código de
   aplicación, pero se verifica igual, como con cualquier otro sprint).

## Decisiones de diseño y su motivo

### Por qué nueve archivos separados y no un único documento largo

El Paso 26 nombra nueve entregables con audiencias distintas (evaluador técnico, quien instala,
usuaria final, quien administra un despliegue real, responsable de calidad, responsable de
seguridad, responsable de privacidad). Mezclarlos en un solo archivo obligaría a cualquier lector a
saltar secciones que no le corresponden. `docs/manuals/` sigue el mismo patrón ya usado por
`docs/sprint-NN.md`/`docs/sprint-NN-evidence.md`: un archivo, un propósito, referenciado desde
`CLAUDE.md`.

### Por qué estos documentos citan en vez de repetir

`docs/security-baseline.md` (36 controles), `docs/android/capability-matrix.md` (744 líneas) y los
26 `docs/sprint-NN.md` ya son la fuente de verdad verificada de cada decisión, con su fecha, su
sprint y su evidencia real. Copiar ese contenido a un nuevo documento lo duplica y lo desactualiza
en cuanto uno de los dos cambie — el mismo motivo por el que `CLAUDE.md` ya dice "no lo repitas de
memoria, léelos". Los nueve documentos de este sprint sintetizan y organizan por audiencia, citando
la fuente concreta para quien necesite el detalle completo.

### Por qué el control de navegación web se documenta como no construido, no como "V2"

`docs/planning/plan-desarrollo.md` (Paso 8) pedía explícitamente "evaluación e implementación con
`VpnService` local... filtrado por dominio". `docs/sprint-09.md` y
`docs/android/capability-matrix.md` (línea 18, línea 615) documentan que se evaluó y se pospuso
explícitamente por necesitar su propia fase, y nunca se retomó en los sprints 10-26. A diferencia de
cámara/micrófono en la supervisión remota (Sprint 23, marcados "V2" como decisión de alcance
consciente desde su propio sprint), este es un paso completo del plan original que quedó sin
construir — la honestidad exigida por `CLAUDE.md` aplica igual de fuerte a un sprint de
documentación: no se presenta como una función menor pendiente, se presenta como lo que es.

### Por qué el manual de administrador es distinto del manual de despliegue

`manual-despliegue.md` cubre el evento único de llevar NetProtect a un servidor real por primera
vez (cuenta cloud, dominio, TLS, CD) — literalmente los comandos ya verificados en
`docs/sprint-26-evidence.md`. `manual-administrador.md` cubre la operación continua de una instancia
que ya está corriendo: leer Grafana/Prometheus/Loki, rotar un secreto, responder a una alerta
`CRITICAL` de manipulación, ejecutar un backup/restore, aprobar un despliegue en el *environment*
`production`. Son audiencias que pueden ser la misma persona pero en momentos distintos del ciclo de
vida del sistema, igual que un manual de "instalar Linux" es distinto de uno de "administrar
Linux".

### Por qué "usuario" cubre Tutor y Supervisado en un solo manual

NetProtect es una única app Android que decide el rol en tiempo de ejecución (`HomeScreen`,
Sprint 6) y un panel web sólo para el rol Tutor. No existe un tercer rol "administrador de producto"
dentro de la aplicación — la palabra "administrador" del Paso 26 se interpreta, como el resto de
decisiones propias de este proyecto (categorías del Sprint 10, 16 secciones del Sprint 24), como
quien administra el *despliegue*, no un rol de negocio adicional. Un único `manual-usuario.md` con
una sección por rol evita inventar un manual de administrador de producto que no correspondería a
ningún rol real del sistema.

## Alcance no cubierto en este sprint (pendiente explícito)

- No se traduce ningún manual a inglés ni a otro idioma.
- No se generan PDFs ni diapositivas de presentación — los nueve documentos quedan en Markdown
  versionado, igual que el resto de `docs/`; convertirlos a un formato de entrega específico (si el
  criterio de evaluación lo exige) es una decisión de quien presenta el proyecto, no de este sprint.
- No se documenta nada sobre el control de navegación web más allá de constatar que no se
  construyó — construirlo está fuera del alcance de un sprint de documentación.

Ver `docs/sprint-27-evidence.md` para la verificación real de enlaces, comandos citados y
coherencia con el código.

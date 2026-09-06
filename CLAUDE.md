# NetProtect — guía para quien continúe este proyecto con Claude Code

Este archivo se carga automáticamente cada vez que Claude Code abre este repositorio. Léelo también
tú si eres humano y estás retomando el proyecto: explica cómo se ha construido hasta ahora y cómo
seguir sin romper las reglas que lo mantienen honesto.

## Qué es esto

NetProtect es una plataforma de control parental: una única app Android (Kotlin/Compose) que opera
como Tutor o Supervisado según lo decide el backend, un panel web (Next.js) para el tutor, y un
backend FastAPI + PostgreSQL + Redis que es la única fuente de verdad para ambos. El documento
original con el alcance completo (48 secciones, 27 sprints) está fuera de este repo; lo que importa
de él ya quedó traducido a artefactos versionados — ver la sección "Dónde está cada cosa" abajo.

## La regla que gobierna todo lo demás

**Nada se marca como terminado sin evidencia real de que se ejecutó.** No mocks disfrazados de
pruebas, no "debería funcionar", no marcar un criterio de aceptación como cumplido por inspección del
código. Si algo no se pudo probar (por ejemplo, un login real de Google exige que una persona elija
su cuenta en un selector — ningún agente puede hacer eso), se dice explícitamente que quedó
pendiente, en vez de darlo por bueno.

Esto se sostiene con un patrón de dos documentos por sprint:

- `docs/sprint-NN.md` — objetivo, historias de usuario, criterios de aceptación, decisiones de
  diseño y **por qué** se tomaron (no sólo qué se hizo).
- `docs/sprint-NN-evidence.md` — comandos ejecutados realmente, con su salida real. Incluye los
  errores encontrados en el camino y cómo se corrigieron, no sólo el resultado final feliz.

Antes de decir que un sprint está cerrado: la suite de pruebas corre en verde dentro de contenedores
Docker reales (no mocks de base de datos), y **CI en GitHub Actions pasa en los 4 jobs**
(`backend`, `frontend`, `android`, `integration`) en un runner limpio — eso es lo que de verdad
certifica que algo funciona independientemente de esta máquina.

## Cómo se construye, sprint por sprint

1. Explicar el objetivo del sprint.
2. Historias de usuario y criterios de aceptación.
3. Decisiones de diseño — especialmente las de seguridad, explicadas con su motivo.
4. Cambios de base de datos (modelos SQLAlchemy + migración Alembic).
5. Backend.
6. Cliente Android y/o web, cuando el sprint los toque.
7. Pruebas de integración contra PostgreSQL/Redis reales en Docker — nunca mockeadas salvo la pieza
   que exige una persona real (p. ej. la verificación de Google se simula con
   `unittest.mock.patch` sobre la función que llama a Google, no sobre la lógica propia).
8. Verificación local, luego en `compose.test.yaml` dentro de contenedor (ver nota de rendimiento
   abajo), luego push y CI.
9. Documentar (`sprint-NN.md` + `sprint-NN-evidence.md`), incluyendo cualquier hallazgo o error real
   encontrado en el camino — no se ocultan los tropiezos, se documentan y se corrigen.

No se avanza al siguiente sprint sin cerrar el anterior con CI en verde, salvo que quede pendiente
explícitamente algo que sólo un humano puede hacer (y quede anotado como tal).

## Dónde está cada cosa

- `docs/planning/plan-desarrollo.md` — el plan completo de 27 pasos, con qué instalar y cuándo.
- `docs/planning/roadmap.md` — la lista de sprints y su incremento.
- `docs/sprint-01.md` … `docs/sprint-07.md` (+ sus `-evidence.md`) — el historial real, sprint por
  sprint. Son la fuente de verdad de qué existe y por qué; no lo repitas de memoria, léelos.
- `docs/security-baseline.md` — controles de seguridad aplicados y la matriz de permisos (quién
  puede hacer qué, y por qué se decidió así).
- `docs/diagrams/06-modelo-datos-fisico-sprint2.md` — el ER real del esquema, con las decisiones de
  diseño de cada tabla.
- `docs/android/capability-matrix.md` — qué es técnicamente viable en Android y qué no, con
  referencias oficiales. Antes de asumir que una función de control parental es posible, mirar aquí.

## Estado actual (05/09/2026)

Sprints 1 a 8 completos y verificados en CI (Sprint 8: run `34004410772`, los 4 jobs en verde).
Existe: arquitectura y Docker; base de datos con migraciones; login
con Google (backend + web + Android); roles y autorización por recurso (`require_tutor_of_device`,
404 uniforme para "no existe" y "no es tuyo"); vinculación por código de 6 dígitos con HMAC, límite
de intentos y revocación; listado/detalle/renombrado/desvinculación de dispositivos con estado
calculado por heartbeat, en Android (app del tutor y del supervisado) y en el panel web; inventario
de apps instaladas y tiempo de uso diario, reportado por el dispositivo supervisado y visible en la
app del tutor y en el panel web; reglas por app (bloquear/permitir/límite diario/horario) definidas
por el tutor en el panel web y aplicadas localmente por el dispositivo supervisado mediante un
foreground service que sondea `UsageStatsManager` y muestra una pantalla de bloqueo propia,
reportando cada bloqueo aplicado. Android tiene un router real (`HomeScreen`: sesión → rol → modo
Tutor/Supervisado); la pantalla de diagnóstico del Sprint 1 (`SprintOneScreen`) ya no existe, su
chequeo de infraestructura vive ahora en la pantalla de sesión cerrada.

**Nota importante descubierta en el Sprint 7, válida para cualquier sprint futuro que toque
permisos Android sensibles**: las políticas de Google Play (formulario de declaración de permisos,
divulgación destacada, política anti-stalkerware para apps de control parental) sólo se activan si
la app se **publica** en la tienda. Este proyecto se instala por sideload (Android Studio/`adb`)
para el trabajo de la universidad, así que esas políticas quedan documentadas pero no aplican hoy.
Sí sigue aplicando siempre, publicado o no: el permiso mismo debe existir, declararse correctamente
y (si es especial) concederse por el mecanismo real de Android — sideload no exime de eso. Ver el
detalle verificado en `docs/android/capability-matrix.md`.

**Nota del Sprint 8, válida para cualquier sprint futuro que toque foreground services**: un
foreground service que sondea en segundo plano exige notificación persistente por requisito del
propio Android desde la API 26 — no es sólo la política anti-stalkerware de Play (que sigue
aplicando sólo si se publica). Con `targetSdk` 34+ además hace falta declarar un
`foregroundServiceType`; si el caso de uso no encaja en ninguno predefinido, `specialUse` es el
único que sirve, con su propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justificando el uso (revisada
por Google sólo al publicar). Verificado también en ejecución real: Android 12+ rechaza arrancar
un foreground service si la app no tiene antes una actividad visible — hay que arrancarlo siempre
desde un efecto de una pantalla en primer plano, nunca desde un contexto de fondo.

**Siguiente: Sprint 9 — Lista blanca y negra.** El modelo de listas por dispositivo y por tutor
necesita una prioridad de reglas definida y probada explícitamente (lista negra > lista blanca >
categoría > regla por defecto, o el orden que se justifique) — a diferencia del Sprint 8, donde sólo
existía un alcance (una regla por app), aquí conviven varios alcances a la vez y hay que decidir cuál
gana. De paso, este sprint es el primero en que la regla `ALLOW` del Sprint 8 deja de ser un
no-efecto documentado (`app_rules`, ver su docstring) y empieza a servir para algo real: sobreescribir
un bloqueo más amplio (lista o categoría) para una app puntual.

## Entorno de trabajo

- Windows con Docker Desktop, Android Studio (SDK 36 + un AVD con Google APIs) y Node/Python locales
  para correr pruebas fuera de contenedor cuando hace falta iterar rápido.
- `docs/sprint-01-paso-0.md` tiene el detalle de cómo se dejó Android Studio y Docker funcionando en
  esta máquina la primera vez, por si hay que replicarlo en otra.
- Variables de entorno: copiar `.env.development.example` a `.env` y completar los valores marcados
  como `change_me_*` o `your-*`. El `.env` real **nunca** se versiona.
- Credenciales de Google Cloud (`GOOGLE_WEB_CLIENT_ID`) ya existen para este proyecto en la cuenta de
  Google Cloud del dueño original; pídeselas directamente o crea un proyecto de Google Cloud propio
  siguiendo `docs/sprint-03.md` (sección de autenticación) — la app está en modo "Prueba", así que
  cualquier cuenta que use el login debe estar agregada como *tester* en la pantalla de
  consentimiento OAuth.

### Nota de rendimiento en Windows

Las pruebas de integración corren en segundos dentro de un contenedor y en varios minutos si se
ejecutan desde Windows contra los puertos publicados (cada petición paga ~1.4s en el proxy de
puertos de Docker Desktop). Para verificar, usar siempre:

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

No `docker compose -f compose.test.yaml up --build ...` con `migrate` en `depends_on`: ese flag
aborta todo el stack en cuanto cualquier contenedor termina, y `migrate` termina por diseño — mató
`backend` antes de que corriera sus pruebas la primera vez que se intentó (ver `docs/sprint-02-evidence.md`).

## Herramientas de apoyo instaladas

- **Impeccable** (`/impeccable`) — guía de diseño para el acabado visual de la web y de Android.
  Instalado a nivel de proyecto en `.claude/`, licencia Apache-2.0, corre local sin claves ni red.
  Las definiciones (skill, referencias, agentes) están versionadas; el binario del detector y
  `settings.local.json` no — en una máquina nueva se reinstala con `npx impeccable install`.
  Empezar con `/impeccable init` una sola vez para fijar el contexto de diseño del producto.
- **Skills de Emil Kowalski** (MIT, `emilkowalski/skills`) — `emil-design-eng` (criterio de diseño),
  `animate`, `review-animations`, `improve-animations`, `find-animation-opportunities`,
  `animation-vocabulary`, `apple-design` y `pick-ui-library`. Markdown puro, versionados. Se
  reinstalan con `npx skills@latest add emilkowalski/skills -s <nombre> -a claude-code --copy -y`.
  No se instalaron `animate-expo`, `write-swift` ni `ask-sonner`: este proyecto no usa React Native,
  ni Swift, ni Sonner. Ojo: estos skills están pensados para web (React/CSS); para las pantallas de
  Compose sirve el criterio de diseño, no el código de las recetas.
- **`/security-review`** — viene incluido con Claude Code, no hay que instalar nada. Revisa los
  cambios pendientes de la rama buscando vulnerabilidades. Correrlo **antes de cerrar cada sprint**,
  junto con las pruebas: este proyecto maneja datos sensibles de menores (inventario de apps, uso,
  y más adelante ubicación), así que la revisión de seguridad es parte del cierre, no un extra.
- **`/code-review`** — también incluido. Revisa el diff buscando errores de corrección y
  simplificaciones. Útil antes de un commit grande de sprint.

## Convenciones de código

- Backend: FastAPI + SQLAlchemy 2.0 (`Mapped`/`mapped_column`) + Alembic. `ruff check app tests
  alembic` debe pasar limpio (config en `backend/pyproject.toml`, con `B008` ignorado a propósito:
  es el patrón `Depends(...)` de FastAPI, no el bug que esa regla busca).
- Nunca `Base.metadata.create_all`: todo cambio de esquema es una migración de Alembic versionada.
- Secretos: cada uno con su propio nombre de variable y su propio valor — nunca reutilizar
  `JWT_SECRET` para otra cosa "porque ya existe". Si algo necesita un secreto nuevo, generarlo con
  `secrets.token_urlsafe(48)` y documentarlo en los tres `.env.*.example`.
- Frontend: Next.js App Router, componentes cliente (`"use client"`). Ver `src/contexts/AuthContext.tsx`
  para el patrón de sesión actual (en memoria + `sessionStorage`, no cookie `HttpOnly` — decisión
  documentada y deliberadamente pospuesta en `docs/sprint-03.md`).
- Android: sin Hilt/DI ni ViewModel todavía — el proyecto es pequeño y se ha mantenido así a
  propósito; no introducir esas dependencias sin que el tamaño del proyecto lo justifique.
- Commits: mensajes explicando el *por qué*, no sólo el qué. Co-autoría con el modelo que hizo el
  trabajo (revisar la guía de atribución vigente en cada sesión).

## Errores que ya se cometieron una vez — no repetirlos

- Un JWT firmado con HS256 puede coincidir carácter por carácter con otro si sólo cambia el último
  byte de la firma (relleno de Base64). Para pruebas que "alteran" un token, tocar un carácter del
  medio, no el último.
- Los tokens de acceso necesitan un `jti` aleatorio: sin él, dos emitidos en el mismo segundo para el
  mismo usuario son idénticos.
- Mezclar `TestClient` (corre la app en su propio *event loop*) con un fixture de base de datos que
  usa el motor global de la aplicación falla en contenedor (asyncpg rechaza conexiones de otro loop).
  El fixture compartido en `backend/tests/conftest.py` crea su propio motor por test; usarlo siempre.
- Las pruebas que ejercitan rate limiting necesitan una IP/host único por test — los contadores viven
  15 minutos en Redis y se filtran entre pruebas si comparten dirección.
- `compose.test.yaml` corre PostgreSQL sin volumen persistente a propósito. Reconstruir sólo
  `backend` y volver a hacer `up` sin repetir `run --rm migrate` primero deja una base sin tablas
  (falla con `relation "..." does not exist` en casi todas las pruebas, no sólo las nuevas). `migrate`
  corre aparte, siempre, antes de cada `up` — nunca asumir que la corrida anterior lo dejó aplicado.
- Un archivo Kotlin nuevo no está verificado hasta que `./gradlew compileDebugKotlin` (o más)
  corre sobre él al menos una vez. Un import que falta (p. ej. `Modifier.width` sin
  `androidx.compose.foundation.layout.width`) no lo marca ningún editor por sí solo; sólo el
  compilador real. No declarar un archivo Android terminado sin haberlo compilado.
- La regla de ESLint `react-hooks/set-state-in-effect` (la trae Next 16) rechaza que un `useEffect`
  invoque, directa o indirectamente, cualquier función que llame a `setState` — incluso una función
  `async` donde el `setState` ocurre después de un `await`. La forma que sí acepta: encadenar
  `.then()/.catch()` directamente en el cuerpo del efecto (con una bandera `cancelled` si hace falta
  cancelar), de modo que cada `setState` quede dentro de un callback de promesa ya resuelta, nunca de
  forma síncrona ni delegado a un helper. Ver `frontend/src/app/page.tsx` (patrón ya existente desde
  el Sprint 3) o `frontend/src/components/DevicesPanel.tsx` (Sprint 6) como referencia.
- `compose.yaml` (el stack de desarrollo persistente) tiene `backend`, `web` y `migrate` como
  servicios con imágenes independientes aunque `backend` y `migrate` compartan el mismo
  `Dockerfile` de `backend/`. Reconstruir `backend`/`web` con `docker compose build` no reconstruye
  `migrate`. Si además se corrió `alembic upgrade head` directo desde el entorno local contra esta
  base (para verificar una migración nueva), la base queda en una revisión que el contenedor
  `migrate` desactualizado no reconoce, y el siguiente `up` falla con
  `Can't locate revision identified by '<revision>'`. Reconstruir los tres servicios juntos
  (`docker compose -f compose.yaml build backend web migrate`) cuando cualquiera de los dos cambie.

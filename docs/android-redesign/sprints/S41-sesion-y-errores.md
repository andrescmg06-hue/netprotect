# Sprint 41 — Sesión que no caduca a los 15 minutos y errores honestos

**Prioridad: CRITICAL** — seguridad de sesión y bloqueo funcional: toda pantalla nueva hace
peticiones autenticadas. **Dueño:** solo Claude Code (código de tokens: nunca se delega).
**Rama:** `sprint-41-sesion`. **Mockups:** ninguno. **Decisiones:** ninguna.
`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`.

## Objetivo

Al terminar: la app puede estar abierta más de 15 minutos en modo tutor o supervisado y las
peticiones siguen funcionando; hay un único punto de renovación de tokens, con exclusión mutua, que
usan también los servicios y el worker; cuando la sesión ya no se puede renovar, la app vuelve al login
con "Tu sesión expiró. Vuelve a iniciar sesión."; los errores de red llegan a la UI como categorías
con mensaje en español, nunca como `HTTP 401` crudo ni tragados en silencio.

## Problema que resuelve

- `AuthRepository.accessToken` se fija al iniciar sesión y se pasa como `String` a
  `TutorScreen`/`SupervisedScreen`; el backend lo caduca a los 15 min (`access_token_ttl_minutes`) y
  la UI no reintenta tras 401 (G-01).
- `BackgroundTokenRefresher` no tiene exclusión mutua y lo llaman `RuleEnforcementService` y
  `SyncWorker`. El backend rota el refresh token y trata la reutilización como robo (revoca la
  familia). Dos refresh concurrentes con el mismo token podrían cerrar la sesión en todas partes
  (G-02, **verificar** con el código de `backend/app/services` antes de afirmarlo).
- Renombrar y desvincular usan `runCatching` sin informar (G-03).

## Dependencias

S40 (línea base y decisiones).

## Alcance

1. `core/auth/TokenProvider` (objeto de proceso, `Mutex`): `validAccessToken()`, y
   `refreshAfterUnauthorized(staleToken)` *single-flight* (si otro llamante ya renovó desde
   `staleToken`, devuelve el nuevo sin ir a la red). Persiste la rotación con `TokenStore`.
2. `BackgroundTokenRefresher` y `AuthRepository` delegan en él (mismo proceso: los servicios no
   declaran `android:process`, **verificar**).
3. Ayudante `authorized { token -> … }`: ante `ApiException(401)` renueva una vez y reintenta una
   vez; si la renovación falla, emite "sesión expirada".
4. `core/network/UiError`: `Offline` (IOException/timeout), `SessionExpired`, `NotFound` (el 404
   uniforme: "No existe o no tienes acceso"), `RateLimited` (429), `Server` (5xx), `Unknown`; con
   mensaje en español.
5. `HomeScreen` escucha "sesión expirada" → `SignedOut("Tu sesión expiró. Vuelve a iniciar sesión.")`.
6. Cambios **mínimos** en `TutorScreen`/`SupervisedScreen`: sus llamadas pasan por `authorized`, y
   renombrar/desvincular muestran el error. Sin rediseño visual.

## Fuera de alcance

Backend; cualquier cambio visual; nuevas pantallas; `TokenStore` (formato de almacenamiento);
cambiar la duración de los tokens.

## Archivos/módulos afectados

`{pkg}/core/auth/{AuthRepository,BackgroundTokenRefresher,TokenStore}.kt`, nuevo
`core/auth/TokenProvider.kt`, `core/network/HttpJsonClient.kt` (solo si hace falta exponer el código
de estado), nuevo `core/network/UiError.kt`, `core/rules/RuleEnforcementService.kt` y
`core/sync/SyncWorker.kt` (solo la llamada al refresher), `feature/home/HomeScreen.kt`,
`feature/tutor/TutorScreen.kt`, `feature/supervised/SupervisedScreen.kt`. Tests en
`mobile/app/src/test/java/com/netprotect/app/core/auth/`.

## Trabajo por capa

- **Backend / Web / BD:** ninguno (leer `backend/app/api/v1/endpoints/auth.py` y el servicio de
  refresh para confirmar la regla de reutilización).
- **Android:** lo del alcance.

## Seguridad

Invariante 10 (refresh rotativo; reutilización = revocación). Tokens solo en `TokenStore` (invariante
11) y en memoria; **nunca** en logs, excepciones con mensaje ni `toString`. El refresh token no se
expone fuera de `core/auth`. `security-reviewer` obligatorio.

## Testing

- JVM (`src/test`): *single-flight* (N corrutinas concurrentes con el mismo token caducado → **una**
  llamada de red), reintento único tras 401, fallo de renovación → sesión expirada, mapeo
  `ApiException`/`IOException` → `UiError`. Para poder probarlo, `TokenProvider` recibe la función de
  renovación por constructor (sin librerías de mocks).
- Real: en el backend de desarrollo, el dueño pone `ACCESS_TOKEN_TTL_MINUTES=1` en su `.env` local
  (Claude no lee `.env`; **verificar** antes en `backend/app/core/config.py` y `compose.yaml` que el
  backend de desarrollo toma esa variable); app abierta 3 min en modo tutor, recargar dispositivos: funciona, y el log
  del backend muestra `/auth/refresh` sin 401 en bucle ni revocaciones. Repetir con el supervisado
  (servicios corriendo) para ver que no hay revocación por carrera.
- `./gradlew test assembleDebug lintDebug`.

## Criterios de aceptación

- [ ] Sesión de tutor de > TTL sigue funcionando (evidencia con TTL = 1 min).
- [ ] Refresh concurrente → una sola llamada (test JVM verde).
- [ ] Renovación imposible → login con el mensaje de sesión expirada.
- [ ] Renombrar/desvincular con error muestran el mensaje.
- [ ] Ningún token en logs (`grep` sobre el diff + `security-reviewer`).
- [ ] Servicios y `SyncWorker` siguen renovando (log del backend).

## Definition of Done

La común del HANDOFF + `security-reviewer` sin hallazgos ALTA.

## Riesgos

Bucle de renovación si el backend responde 401 también al refresh (limitar a un reintento). Romper
los servicios de fondo (probar el supervisado ≥ 3 min con TTL = 1). Una renovación en curso al cerrar
sesión (cancelar y limpiar).

## Decisiones técnicas (de Claude, con este criterio)

Sin dependencias nuevas; exclusión mutua con `kotlinx.coroutines.sync.Mutex`. Si resulta que los
servicios corren en otro proceso, **detenerse**: el `Mutex` no basta y hay que decidir con el dueño.

---

## PROMPT PARA CLAUDE CODE (único; este sprint no se delega)

```
Sprint 41 del rediseño Android de NetProtect: sesión que sobrevive a los 15 minutos y modelo de
errores. Es código de tokens: lo implementas tú, no se delega a DeepSeek.

Lee solo: docs/android-redesign/CLAUDE_CODE_HANDOFF.md y
docs/android-redesign/sprints/S41-sesion-y-errores.md. Sigue las 10 fases del HANDOFF.

1. Explora: AuthRepository, TokenStore, BackgroundTokenRefresher, HttpJsonClient, dónde se usan
   (RuleEnforcementService, SyncWorker, HomeScreen, TutorScreen, SupervisedScreen), el manifest
   (¿algún android:process?) y en el backend la regla de reutilización del refresh token.
2. Explícame: ¿la carrera G-02 es real con este código? Muéstrame las líneas. ¿Qué vas a tocar y qué
   puede romperse?
3. Plan en pasos. Espera mi OK antes de implementar.
4. Implementa el alcance del sprint (TokenProvider single-flight con Mutex, ayudante authorized con un
   único reintento, UiError, sesión expirada → login). En TutorScreen/SupervisedScreen, cambios
   mínimos: sin rediseño.
5. Tests JVM del sprint. Luego verifier: ./gradlew test assembleDebug lintDebug.
6. Guíame para la prueba real con ACCESS_TOKEN_TTL_MINUTES=1 (yo edito mi .env; tú no lo lees) en
   tutor y en supervisado; recoge la evidencia que te pase.
7. security-reviewer sobre el diff. Corrige hallazgos.
8. docs/sprint-41.md, -evidence.md, progress.md, tasks.md (G-01…G-03 → done), README. /cerrar-sprint 41.
9. Informe final del §9 del HANDOFF y propuesta de commits: feat(sprint-41) y docs(sprint-41).
   Sin commit sin mi OK.
```

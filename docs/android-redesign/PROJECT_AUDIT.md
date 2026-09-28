# PROJECT_AUDIT — estado real de NetProtect para el rediseño Android

> Auditoría de solo lectura, 28/09/2026, rama `sprint-31-design-system` @ `2f7ce9e` (Sprint 39).
> `{pkg}` = `mobile/app/src/main/java/com/netprotect/app`. Todo lo que dice este documento se leyó en
> el código; lo no comprobado lleva **(verificar)**.

## A. Estado del repositorio

- Último trabajo: Sprint 39 (login web del mockup) y el harness de Claude Code (commit `a94ca3f`:
  `CLAUDE.md` de 62 líneas, 6 reglas en `.claude/rules/`, agentes `verifier` y `security-reviewer`,
  skills `verificar` y `cerrar-sprint`, 4 hooks, `docs/progress.md`, `docs/tasks.md`,
  `docs/delegated/`).
- La cola de delegación **ya se usó**: `docs/delegated/done/sprint-39-login-reemplazo.md` es el
  formato probado de encargo a DeepSeek. Este plan lo reutiliza.
- `main` va 20 commits por detrás de la rama actual (D-06). Árbol limpio salvo
  `docs/planning/prompt-mockups-android.md` (sin versionar).
- No existe `AGENTS.md` en la raíz: hoy OpenCode lee `CLAUDE.md` como respaldo, que menciona
  `verifier` y `security-reviewer`, que en OpenCode no existen (lo resuelve S40).

## B. Arquitectura encontrada (Android)

Una sola `MainActivity` → `HomeScreen()` dentro de `MaterialTheme` por defecto. No hay tema propio,
navegación, ViewModel, DI ni carpeta `ui/`. `res/` solo tiene `values/` y `xml/`: **no hay icono de
lanzador, fuentes ni drawables**.

| Archivo | Líneas | Qué hace |
|---|---|---|
| `{pkg}/feature/home/HomeScreen.kt` | 338 | Máquina de estados: Loading → SignedOut → SelectingRole → Tutor/Supervisado. Login Google, línea de estado del servidor, elección de rol (`RoleClient` + `RolePreference`) |
| `{pkg}/feature/tutor/TutorScreen.kt` | 963 | **Todo el modo tutor en una columna**: vinculación, lista de dispositivos con secciones desplegables (apps, ubicación, geocercas, historial, estadísticas, alertas) y auditoría |
| `{pkg}/feature/supervised/SupervisedScreen.kt` | 587 | Vincular, vinculado, 4 permisos, tarjeta de consentimiento **y los efectos que arrancan todos los servicios** (latido cada minuto, `SyncWorker`, `RuleEnforcementService`, `LocationReportingService`, canal realtime) |
| `{pkg}/feature/supervised/BlockScreenActivity.kt` | 138 | `BlockScreenContent` + textos de los 7 motivos. Lo usa también `core/rules/BlockOverlayController.kt` (ventana superpuesta) |
| `{pkg}/core/network/*Client.kt` | 14 clientes | `HttpJsonClient` sobre `java.net` + `org.json`; `ApiException(statusCode)` |
| `{pkg}/core/auth/` | | `AuthRepository` (token de acceso en memoria), `TokenStore` (refresh cifrado en Keystore), `BackgroundTokenRefresher` (servicios/worker), `RolePreference`, `LinkedDeviceStore` |
| `{pkg}/core/rules/` | | `RuleEvaluator` (motivos `BLOCK`, `DAILY_LIMIT`, `WEEKLY_LIMIT`, `SCHEDULE`, `CATEGORY`, `SCHOOL_MODE`, `DEFAULT_POLICY`), `RuleEnforcementService`, `ProtectedPackages`, `EnforcementLiveness` |
| `{pkg}/core/{location,screenshare,sync,tamper,storage,inventory,permissions}/` | | Servicios en primer plano de ubicación y captura, `SyncWorker`, administrador de dispositivo, Room, permisos |

Colores escritos a mano en cada composable (tema oscuro `#090B10`/`#121722`). Textos en español,
en el código (no en `strings.xml`).

## C. Stack real

Kotlin 2.3.21, AGP 8.13.2, Compose BOM 2026.06.00 + Material 3, `compileSdk`/`targetSdk` 36,
`minSdk` 26, Java 17. Credential Manager + `googleid`, OkHttp (solo WebSocket), Room + KSP,
WorkManager, `stream-webrtc-android`. Sin Hilt, Retrofit, navegación ni ViewModel (decisión
documentada). Tests: `RuleEvaluatorTest` (JVM, 350 líneas) y 2 tests instrumentados de Room.
Backend: FastAPI + SQLAlchemy 2 async + PostgreSQL + Redis, 29 archivos de test. Web: Next.js 16
con los tokens de `DESIGN.md` (la misma paleta del mockup Android). CI: 8 jobs (`android` corre
`./gradlew test assembleDebug`; `android-instrumented` corre en emulador).

## D. Funcionalidades existentes frente a las 17 pantallas

**La funcionalidad ya existe casi entera; lo que falta es sobre todo presentación, navegación y
algunos huecos concretos.**

| # | Pantalla | Hoy (dónde) | API que la alimenta | Hueco principal |
|---|---|---|---|---|
| 1 | Login | `HomeScreen` `SignedOutContent` | `POST /auth/google`, `GET /health/ready` | Solo visual |
| 2 | Elegir modo | `HomeScreen` `RoleSelectionContent` | `POST /users/me/roles` | Visual; B-03 |
| 3 | Inicio tutor | `TutorScreen` (cabecera, vinculación, lista) | `POST/DELETE /pairing/codes*`, `GET /devices` | Cuenta atrás (hoy texto fijo "Válido por 3 minutos"), `os_version` no se lee, navegación |
| 4 | Detalle | `TutorScreen` `DeviceRow` | `PATCH /devices/{id}`, `DELETE /devices/{id}/link` | **Desvincular sin confirmación**; errores de renombrar/desvincular **se tragan** (`runCatching`) |
| 5 | Apps | `AppsList`/`AppUsageRow` | `GET /devices/{id}/applications` | Buscar, fecha del uso, iconos |
| 6 | Ubicación | `LocationSection` | `GET /devices/{id}/location/latest` | Solo visual; sin lugar ni mapa (D-05) |
| 7 | Geocercas | `GeofenceSection` | `GET .../geofences`, `GET .../geofences/events` | Último evento por geocerca (derivable) |
| 8 | Historial | `HistorySection` | `GET .../history` | Agrupar por día; nombre de app |
| 9 | Estadísticas | `StatisticsSection` | `GET .../statistics?period=` | Visual; semántica de cumplimiento |
| 10 | Alertas | `AlertsSection` (solo lectura) | `GET .../alerts`, **`POST .../alerts/{id}/read`**, **`POST .../alerts/{id}/silence`** | El cliente Android no tiene "marcar leída" ni "silenciar" (el backend sí) |
| 11 | Mi actividad | `AuditSection` | `GET /users/me/audit?limit=&offset=` | Etiquetas, agrupación, paginación; D-10 |
| 12 | Vincular | `SupervisedScreen` `EnteringCode` | `POST /pairing/redeem` | Entrada de 6 casillas, errores |
| 13 | Vinculado | `SupervisedScreen` `Linked` | `GET /devices/me` (`tutors`, `status.last_seen_at`) | Última comunicación; resumen de permisos; B-01 |
| 14 | Permisos | Tarjetas en `SupervisedScreen` | local | Pantalla propia; recomprobar al volver |
| 15 | Consentimiento | Tarjeta en `SupervisedScreen` | canal realtime (`screen_share_request`/`_consent`) | Pantalla completa; textos; B-02 |
| 16 | App bloqueada | `BlockScreenContent` (+ overlay) | local (`RuleEvaluator`) | Visual; icono y categoría |
| 17 | Servicios | **No existe** (solo las 3 notificaciones) | local | Pantalla nueva + registro de estado de servicios |

Fuera de alcance en el móvil pero **existente en el repo** (FUERA DE ALCANCE / REVISAR — no se
elimina, no se lleva al móvil): editor de reglas, política, horario escolar y categorías
(`rules.py`, `categories.py`, panel web); crear/editar geocercas (`POST/PUT/DELETE geofences`, web);
vista remota del lado del tutor (web); exportar auditoría (`GET /users/me/audit/export`, web);
historial de ubicaciones (`GET .../location/history`). Ninguno aparece hoy en la app Android; el
riesgo es solo que un agente los añada al "completar" pantallas.

## E. Funcionalidades faltantes

Ver `ARCHITECTURE_GAPS.md`. Resumen: sistema de diseño, navegación, sesión que sobreviva a los
15 min, modelo de errores, 3 acciones de cliente (leer/silenciar alerta, cuenta atrás), pantalla de
servicios, auditoría de consultas (decisión), reconexión realtime (B-02).

## F. Problemas arquitectónicos

1. **El token de acceso de la UI no se renueva.** `AuthRepository.accessToken` se fija al iniciar
   sesión y se pasa como `String` a `TutorScreen`/`SupervisedScreen`. Caduca a los 15 min
   (`access_token_ttl_minutes`); no hay reintento tras 401. Una sesión de tutor de más de 15 min
   empieza a fallar.
2. **Posible carrera de refresh (verificar).** `BackgroundTokenRefresher` no tiene exclusión mutua y
   lo llaman `RuleEnforcementService` y `SyncWorker`. El backend rota el refresh token y trata la
   reutilización como robo (revoca la familia, invariante 10). Dos refresh casi simultáneos con el
   mismo token podrían cerrar la sesión en todas partes.
3. **Errores silenciados.** Renombrar y desvincular usan `runCatching` sin mostrar el fallo.
4. **Monolito de UI** (`TutorScreen.kt` 963 líneas, deuda D-06).
5. **Sin tema**: 100 % colores literales; cambiar el sistema visual exige tocar cada composable.
6. **Servicios atados a la composición de `SupervisedScreen`**: al partirla en pantallas, los
   efectos deben quedar en un contenedor siempre compuesto o los servicios se detendrían al navegar.

## G. Riesgos

| Riesgo | Impacto | Mitigación en el plan |
|---|---|---|
| Mover efectos de servicios a una subpantalla | Se dejan de reportar datos o de aplicar reglas | S48: los efectos pasan intactos a `SupervisedShell`; prueba explícita |
| Refresh concurrente | Cierre de sesión global | S41 antes que cualquier pantalla |
| El mockup induce a inventar datos | UI que miente (ubicación, cumplimiento, auditoría) | `mockups/README.md` + revisión de Claude |
| DeepSeek toca código de seguridad | Regresión en auth/realtime/reglas | Reparto: esos archivos nunca se delegan |
| Overlay sin tema | Pantalla de bloqueo sin estilos | S49: envolver la composición del overlay en el tema |
| Repo en OneDrive | `index.lock`, `EBUSY` | `GIT_OPTIONAL_LOCKS=0` (ya en `settings.json`) |
| Pruebas que exigen una persona | Login Google, diálogo de captura | Se declaran pendientes (H-01, H-02) |

## H. Dependencias

```
S40 decisiones + línea base + AGENTS.md
 └─ S41 sesión y errores ──────────────┐
     └─ S42 tema, componentes, navegación
         ├─ S43 login + elegir modo
         ├─ S44 shell tutor + inicio + detalle
         │    ├─ S45 apps, ubicación, geocercas
         │    ├─ S46 historial, estadísticas, alertas
         │    └─ S47 mi actividad (+ auditoría de consultas)
         ├─ S48 shell supervisado + vincular + vinculado + permisos
         │    └─ S50 consentimiento + servicios + reconexión realtime
         ├─ S49 app bloqueada (solo depende de S42; va tras S48 por orden, no por dependencia)
         └─ S51 endurecimiento, accesibilidad, regresión, integración a main (depende de todos)
```

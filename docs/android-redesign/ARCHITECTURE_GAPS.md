# ARCHITECTURE_GAPS — de la app actual a las 17 pantallas

`{pkg}` = `mobile/app/src/main/java/com/netprotect/app`. Prioridad justificada por dependencia,
seguridad, bloqueo funcional, riesgo y valor. Dueño: quién implementa (**C** = Claude Code,
**D** = DeepSeek vía OpenCode; Claude revisa siempre lo de DeepSeek).

## 1. Huecos

| Id | Hueco | Prioridad y motivo | Sprint | Dueño |
|---|---|---|---|---|
| G-01 | Token de la UI caduca a los 15 min sin renovarse | **CRITICAL** — bloquea el uso real de todas las pantallas del tutor; seguridad de sesión | S41 | C |
| G-02 | Posible carrera entre refresh concurrentes (verificar) | **CRITICAL** — puede revocar la familia de tokens (cierre de sesión global) | S41 | C |
| G-03 | Sin modelo de errores (se muestran `HTTP 401` crudos o se tragan) | **HIGH** — cada pantalla necesita estados de error honestos | S41 | C |
| G-04 | Sin tema, tipografía, iconos, logo ni icono de lanzador | **CRITICAL** — dependencia de las 17 pantallas | S42 | D |
| G-05 | Sin componentes compartidos (tarjeta, badge de estado, estados vacío/carga/error, fila, cabecera de dispositivo, barra inferior…) | **CRITICAL** — evita 17 implementaciones distintas | S42 | D |
| G-06 | Sin navegación ni "atrás" del sistema | **CRITICAL** — estructura de 17 pantallas | S42 (esqueleto) / S44 / S48 | C |
| G-07 | Formateadores ("hace 12 min", "1 h 24 min", Hoy/Ayer) y etiquetas en español dispersos | **HIGH** — consistencia con el panel web; testeable en JVM | S42 | D |
| G-08 | `TutorScreen.kt` monolítico (D-06) | **HIGH** — imposible rediseñar sin partirlo | S44 | C (esqueleto) + D |
| G-09 | Cuenta atrás real del código de vinculación | **MEDIUM** — el backend ya da `expires_in_seconds` | S44 | D |
| G-10 | `os_version`/`app_version` no se leen en `DeviceClient` | **LOW** — dato ya disponible | S44 | C |
| G-11 | Desvincular sin confirmación; fallos silenciados | **HIGH** — acción destructiva e irreversible | S44 | D |
| G-12 | Apps: búsqueda, fecha real del uso, iconos | **MEDIUM** | S45 | D |
| G-13 | Ubicación/geocercas con textos de retraso y sin datos inventados | **HIGH** — datos de un menor; privacidad | S45 | D |
| G-14 | Historial agrupado por día con nombre de app | **MEDIUM** | S46 | D |
| G-15 | Estadísticas con la semántica real de cumplimiento | **MEDIUM** — el mockup la describe mal | S46 | D |
| G-16 | Cliente Android sin "marcar leída" ni "silenciar" | **HIGH** — acciones pedidas; el backend ya autoriza y audita | S46 | D (cliente) |
| G-17 | Mi actividad con etiquetas, agrupación y paginación | **MEDIUM** | S47 | D |
| G-18 | La consulta de ubicación no se audita (D-10) | **HIGH si se aprueba** — trazabilidad sobre datos de menores | S47 | C (backend) |
| G-19 | `SupervisedScreen` monolítico con los efectos de servicios dentro | **CRITICAL** — partirlo mal detiene los servicios | S48 | C |
| G-20 | Vincular con entrada de 6 casillas y errores claros | **MEDIUM** | S48 | D |
| G-21 | Vinculado: última comunicación, varios tutores, resumen de permisos | **MEDIUM** | S48 | D |
| G-22 | B-01 dispositivos duplicados en `GET /devices/me` | **HIGH** — la pantalla 13 depende de ese endpoint | S48 | C |
| G-23 | Pantalla de bloqueo: 7 variantes con icono y categoría reales | **HIGH** — la ve el menor a diario | S49 | C (datos) + D (UI) |
| G-24 | Pantalla de consentimiento completa con textos verdaderos | **HIGH** — privacidad y consentimiento | S50 | C + D |
| G-25 | Registro de estado de servicios (latido, reglas, ubicación, vista remota) | **HIGH** — transparencia exigida (pantalla 17) | S50 | C |
| G-26 | B-02 reconexión del canal realtime | **HIGH** — sin ella, la solicitud de vista remota puede no llegar | S50 | C |
| G-27 | Accesibilidad, rotación, sin conexión, build release, código muerto | **HIGH** — cierre de calidad | S51 | C (+ D en arreglos mecánicos) |

## 2. Deuda encontrada, clasificada (no se convierte sola en trabajo del sprint)

| Nivel | Deuda | Decisión |
|---|---|---|
| P0 | G-01, G-02 | Se resuelven en S41, antes de todo |
| P1 | G-11 (desvincular sin confirmar), G-19, B-01, B-02 | En el sprint que toca esa pantalla |
| P2 | D-06 monolito; colores literales | Se resuelven de paso al rediseñar |
| P2 | `lintDebug` fuera de CI (D-02 de `tasks.md`) | D-14, S51 |
| P3 | Textos en código y no en `strings.xml` | Se mantiene (convención del proyecto, solo español) |
| P3 | Sin *type checking* en Python, sin cobertura | Fuera de este plan |

## 3. Arquitectura objetivo (propuesta; cada sprint la confirma contra el repo)

```
{pkg}/
├── MainActivity.kt                  NetProtectTheme { HomeScreen() }
├── ui/
│   ├── theme/                       Color.kt, Type.kt, Shape.kt, Theme.kt   (tokens de DESIGN.md)
│   ├── components/                  NpCard, NpButton, StatusPill, SeverityBadge, InfoBanner,
│   │                                ListRow, EmptyState, LoadingState, ErrorState, DeviceHeader,
│   │                                NpTopBar, NpBottomBar, SegmentedControl, ProgressBar,
│   │                                TimelineItem, CodeDisplay, OtpInput, ConfirmDialog
│   ├── format/                      RelativeTime, Durations, DayGrouping, *Labels (copiadas del web)
│   └── navigation/                  rutas + pila (D-01)
├── core/
│   ├── auth/TokenProvider.kt        S41: único punto de refresh, con exclusión mutua
│   ├── network/UiError.kt           S41: 401/404/429/5xx/sin red → mensajes en español
│   └── status/ServiceStatusRegistry.kt   S50
└── feature/
    ├── home/        HomeScreen (router, igual), LoginScreen, RoleSelectionScreen
    ├── tutor/       TutorShell, home/, devices/, device/, apps/, location/, geofences/,
    │                history/, statistics/, alerts/, activity/, more/
    └── supervised/  SupervisedShell (efectos de servicios), link/, linked/, permissions/,
                     consent/, services/, BlockScreenActivity + BlockScreenContent
res/  font/ (Inter), drawable/ (ic_* Lucide, logo), mipmap-*/ (icono de lanzador)
src/debug/  DesignGalleryActivity (galería de componentes para capturas; nunca en release)
```

Principios: sin Hilt ni ViewModel salvo que D-02 diga otra cosa; un archivo por pantalla; los
clientes HTTP no cambian de forma; ninguna pantalla muestra datos que la API no da; la
autorización sigue viviendo en el backend (ocultar un botón no es seguridad).

## 4. Backend: qué cambia y qué no

No hace falta ningún endpoint nuevo para las 17 pantallas. Cambios posibles, todos de Claude:
G-18 (auditar `LOCATION_VIEWED`, si D-10 = a) y B-01. Todo lo demás consume endpoints existentes,
ya cubiertos por el barrido de autorización del Sprint 25.

# Análisis de riesgos — NetProtect

Audiencia: responsables del proyecto y evaluadores. Lista honesta de lo que no se construyó, quedó
limitado, o depende de algo fuera del control del equipo — no sólo lo que salió bien. Cada riesgo
cita su origen real, no una suposición.

## 1. Riesgos técnicos y de producto

| # | Riesgo | Origen | Estado / mitigación |
|---:|---|---|---|
| R1 | El control de navegación web (filtrado de dominios por `VpnService`/DNS) del plan original **no se construyó** | `docs/planning/plan-desarrollo.md`, Paso 8; pospuesto en `docs/sprint-09.md` por necesitar su propia fase | Sin mitigación — es una funcionalidad ausente, no un límite de una funcionalidad existente. NetProtect controla apps, no navegación web. |
| R2 | DNS cifrado (DoH/DoT) evadiría cualquier filtrado por dominio si se construyera | `docs/planning/plan-desarrollo.md`, sección 5 (riesgo original) | No aplica hoy porque R1 no está construido; si se retoma, requiere bloquear resolutores DoH conocidos y documentar que no es un filtrado absoluto. |
| R3 | El bloqueo de apps sin *device owner* es limitado (detección de app en primer plano + pantalla propia, no una API de bloqueo real) | `docs/sprint-08.md`; `docs/android/capability-matrix.md` | Aceptado y documentado desde el Sprint 8 — es el único mecanismo viable para una app instalada normalmente. Una app maliciosa con más privilegios que NetProtect podría evadirlo; fuera del modelo de amenaza de este proyecto (ver `docs/manuals/modelo-seguridad.md`). |
| R4 | `MediaProjection` no permite consentimiento permanente — cada sesión de supervisión remota exige un diálogo nuevo | `docs/sprint-23.md`, verificado contra la fuente oficial de Android | Aceptado por diseño: es un límite de la plataforma, no algo que NetProtect pueda evitar sin violar el modelo de permisos de Android. |
| R5 | Supervisión remota sin servidor TURN — no conectará detrás de NAT de operadora en muchos casos reales | `docs/sprint-23.md`, límite declarado | Pendiente del Paso 25 (coturn); sólo STUN público hoy. |
| R6 | Latencia de detección de geocercas de ~15 minutos, no los 2-6 minutos de la Geofencing API real | `docs/sprint-14.md` — decisión consciente de no añadir `play-services-location` | Aceptado; alternativa evaluada y descartada para no requerir `ACCESS_BACKGROUND_LOCATION`. |
| R7 | Cámara y micrófono en supervisión remota quedan fuera (V2) | `docs/sprint-23.md` | Alcance recortado desde su propio sprint, con el mismo criterio de evaluación formal que el resto de capacidades Android. |
| R8 | Un dispositivo que deja de reportarse **para siempre** no genera una alerta de manipulación por silencio — sólo se ve como `OFFLINE` | `docs/sprint-20.md` | Aceptado: distinguir "apagado a propósito" de "manipulado permanentemente" sin más señales no es posible de forma confiable. |
| R9 | El bloqueo de apps y el reporte de ubicación se detienen si el proceso de Android muere (swipe en Recientes, presión de memoria) — sin reinicio automático | `docs/sprint-08.md`, `docs/sprint-13.md` | Aceptado desde el Sprint 8; `SyncWorker` (Sprint 19) mitiga parcialmente para heartbeat/uso, no para el bloqueo en sí. |
| R10 | Firebase Cloud Messaging y Google Maps Platform no tienen proyecto/clave real en este repositorio | `docs/sprint-18.md`, `docs/sprint-13.md` | Estructura lista (registro de token, mapa embebido con *fallback* a texto); pendiente de un humano con cuenta de Google Cloud. |

## 2. Riesgos de seguridad y privacidad

| # | Riesgo | Origen | Estado / mitigación |
|---:|---|---|---|
| R11 | Datos sensibles de menores (ubicación, uso de apps, alertas) | `docs/planning/plan-desarrollo.md`, sección 5 | Minimización, cifrado (Fernet para ubicación), retención corta (7-90 días), control de acceso por fila, auditoría — ver `docs/manuals/politica-privacidad.md`. |
| R12 | Sin certificate pinning en Android | `docs/security-baseline.md`, "Controles diferidos conscientemente" | No hay todavía un certificado de producción real contra el cual fijarlo (depende de R14). |
| R13 | CSP del panel web usa `'unsafe-inline'` en `script-src`/`style-src` | `docs/security-baseline.md` | Next.js App Router no genera un nonce por request sin cambiar de arquitectura (Sprint 21); riesgo residual aceptado. |
| R14 | Sin dominio de producción real todavía — Caddy emite desde su CA interna, no Let's Encrypt real | `docs/sprint-26.md`, "Qué queda pendiente de un humano" | Pendiente del Paso 25 (cuenta cloud, dominio). |
| R15 | Sin gestor de secretos cloud concreto elegido — el mecanismo (`secrets/README.md`) es agnóstico pero el proveedor real no está decidido | `docs/sprint-26.md` | Pendiente del Paso 25. |
| R16 | El *environment* `production` de GitHub no tiene revisor obligatorio configurado — cualquier push a `main` con `PROD_HOST` configurado desplegaría sin aprobación manual | `docs/sprint-26-evidence.md`, sección 14 | Requiere permisos de administrador sobre el repositorio que el equipo no tuvo durante el Sprint 26; mitigado hoy porque `PROD_HOST` tampoco existe (el job se salta). |
| R17 | Sin endpoint de "borrar todos los datos de este usuario/dispositivo" bajo demanda | Constatado al escribir `docs/manuals/manual-administrador.md`/`politica-privacidad.md` | La retención automática (7/90 días) purga con el tiempo, pero no hay un mecanismo de borrado inmediato ante una solicitud explícita. Riesgo real no mitigado hoy. |
| R18 | Rotación de claves (Fernet, JWT) no está automatizada | `docs/security-baseline.md`, "Controles diferidos conscientemente" | Rotación manual posible (ver `docs/manuals/manual-administrador.md` §3), sin calendario forzado. |

## 3. Riesgos de proceso y plataforma (originales del plan)

| # | Riesgo | Mitigación aplicada |
|---:|---|---|
| R19 | Versiones muy recientes de AGP, Gradle y Compose podían romper la compilación | Versiones fijadas y verificadas desde el Paso 0 (`docs/sprint-01-evidence.md`); un hallazgo real similar (KSP vs. `kapt` con Kotlin 2.3.21, Sprint 19) se resolvió cambiando de herramienta, no bajando de versión. |
| R20 | Las políticas de Google Play (declaración de permisos, anti-*stalkerware*) no se verifican porque el proyecto se instala por *sideload* | `CLAUDE.md`, nota del Sprint 7 — documentado explícitamente como no aplicable hoy, pero el permiso en sí (declaración correcta, concesión por el mecanismo real de Android) sí se exige siempre. |
| R21 | Cambios internos de FastAPI 0.141 (aplanado de `app.routes`) rompieron silenciosamente dos mecanismos distintos (barrido de rutas del Sprint 25, `prometheus-fastapi-instrumentator` en el Sprint 26) | Detectado y corregido en ambos casos con *duck-typing*/código propio en vez de depender de la forma interna de la librería; ver `docs/sprint-25.md`, `docs/sprint-26.md`. |

## 4. Qué NO es un riesgo abierto (ya resuelto y verificado)

Para contraste — ítems que empezaron como riesgo y ya tienen mitigación verificada con evidencia
real, no sólo diseñada: RBAC por recurso con 404 uniforme (Sprint 4-6), rate limiting de fuerza
bruta en vinculación y auth (Sprint 5, 21), TLS obligatorio a nivel de aplicación y ahora real con
Caddy (Sprint 21, 26), backups probados con restauración real (Sprint 26), escaneo de seguridad
real con ZAP/MobSF con hallazgos corregidos (Sprint 21).

## 5. Cómo se mantiene esta lista honesta

Cada fila cita el documento donde se descubrió o decidió el riesgo — no se agregó ningún riesgo
genérico de plantilla. Si un sprint futuro resuelve alguno de estos, la fila se mueve a la sección 4
con su evidencia, no se borra.

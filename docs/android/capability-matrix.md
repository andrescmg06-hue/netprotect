# Matriz preliminar de capacidades Android

Fecha de revisión: 30/08/2026. Esta matriz evita asumir capacidades que una aplicación Android convencional no posee.

| Capacidad | API/mecanismo oficial | Requisito principal | Limitación relevante | Decisión |
|---|---|---|---|---|
| Acceso a Internet | `INTERNET` | Permiso normal en manifiesto | No autoriza datos sensibles por sí mismo | Sprint 1 |
| Estadísticas de uso | `UsageStatsManager` | `PACKAGE_USAGE_STATS` + concesión del usuario en Ajustes para la mayoría de consultas | No equivale a control total de otras apps | Evaluar Sprint 7 |
| Filtrado de tráfico local | `VpnService` | Preparación/consentimiento del usuario | Sólo una app VPN puede estar preparada a la vez; el usuario puede revocar | Evaluar Sprint 8 |
| Geolocalización | Location Services | Permisos de ubicación según alcance | Restricciones de background y precisión | Sprint 13 |
| Geocercas | Geofencing API | `ACCESS_FINE_LOCATION`; background location al aplicar según target/uso | Límite de geocercas y latencia en background | Sprint 14 |
| Notificaciones | `NotificationListenerService` | Acceso habilitado por el usuario | Debe minimizarse el contenido recolectado | Evaluar Sprint 17/23 |
| Captura de pantalla | `MediaProjection` | Consentimiento del usuario y foreground service `mediaProjection` | En Android moderno el consentimiento no puede reutilizarse indefinidamente; cada sesión debe respetar las reglas vigentes | Evaluar Sprint 23 |
| Cámara remota | Camera + foreground service cuando aplique | `CAMERA` y estado/flujo permitido | Permisos while-in-use y restricciones para iniciar desde background | V2/Futuro |
| Micrófono remoto | AudioRecord/MediaRecorder + FGS cuando aplique | `RECORD_AUDIO` | Restricciones while-in-use/background | V2/Futuro |
| Administración empresarial profunda | Device Policy APIs / DPC | Aprovisionamiento como device/profile owner cuando corresponda | No debe asumirse para una instalación parental convencional de Play Store | Fuera del MVP salvo caso justificado |

## Referencias oficiales consultadas

- Android Developers — `UsageStatsManager`.
- Android Developers — `VpnService`.
- Android Developers — Geofencing.
- Android Developers — `NotificationListenerService`.
- Android Developers — `MediaProjectionManager` y cambios de comportamiento de Android 14+.
- Android Developers — foreground service types y restricciones de background.

La matriz debe revisarse nuevamente en el sprint que implemente cada capacidad porque las políticas y restricciones de Android pueden cambiar.

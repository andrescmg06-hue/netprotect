# Vistas: mockup, componente del repo y composición

Los mockups están en `assets/mockups/`. «Componente» es el archivo de `frontend/src/components/` que hoy renderiza la sección (el router es `DashboardShell` + `lib/dashboardSections.ts`). El aspecto sigue al mockup; los datos, al backend.

| # | Vista (`SectionKey`) | Mockup | Componente actual | Composición objetivo | Omitir o sustituir (no existe en el backend) |
| --- | --- | --- | --- | --- | --- |
| 1 | Login | `mock-01-login` | `login/*` | Columna izquierda crema: logo, «Inicia sesión» (serif 64), texto breve, «Continuar con Google». Fotografía del estudio a sangre a la derecha. | Corregir «NetProject» → NetProtect. Sin ordenadores flotantes. |
| 2 | Sidebar | todos | `shell/Sidebar.tsx` | Navy, grupos Cuenta · Dispositivos · Monitoreo · Auditoría, activo azul. | Ítems de mockup que no son secciones (Actividad, Bloqueos, Reportes, dispositivos favoritos). |
| 3 | Header | todos | `shell/Header.tsx` | Buscador · notificaciones · configuración · perfil. | «Ctrl K» solo si ya existe atajo. |
| 4 | Inicio (`overview`) | `mock-02-inicio` | `OverviewPanel` | Saludo serif sobre la banda; cifras en franja con divisores; «Tus dispositivos» con el principal protagonista; actividad reciente como timeline; accesos rápidos como lista. | «% vs. ayer», serie de 7 días, «Apps más usadas» y «Consejo del día» si no hay dato real; vista Tarjetas/Mapa. |
| 5 | Dispositivos (`devices`) | `mock-03-dispositivos` | `DevicesPanel` | Lista izquierda, detalle derecha (estado, técnica, pestañas, Renombrar/Desvincular). | Pestañas o métricas sin API. |
| 6 | Vinculación (`pairing`) | `mock-04-vinculacion` | `PairingPanel` | Flujo guiado; el código de 6 dígitos es protagonista; pasos y estado. | Historial de códigos. |
| 7 | Apps (`apps`) | sin mockup | `DeviceApplicationsList` | Lista o grilla limpia; vacío con `illus-apps-grid`. | — |
| 8 | Reglas (`rules`) | sin mockup | `AppRulesPanel`, `RuleTypeFields` | Centro de control: BLOQUEAR · PERMITIR · LÍMITE · HORARIO como filas. | — |
| 9 | Política y horario (`policy`) | `mock-05-politica-horario` | `DevicePolicyPanel` | Modo por defecto + `ScheduleBar` de 24 h arrastrable. | Plantillas/excepciones de horario, «copiar a todos» si la API no lo permite. |
| 10 | Categorías (`categories`) | sin mockup | `DeviceCategoriesPanel` | Colección con jerarquía de tamaños (11 categorías del backend; el brief lista 10). | Usar las categorías reales. |
| 11 | Geocercas (`geofences`) | `mock-06-geocercas` | `GeofencePanel`, `GeofenceMap` | Mapa grande a sangre, formulario integrado (ubicación → radio → nombre → alertas), lista debajo. | Mapa real de teselas: se mantiene el esquema propio. |
| 12 | Ubicación (`location`) | `mock-07-ubicacion` | `DeviceLocationPanel` | Ubicación actual, precisión y hora junto al mapa; historial debajo. | Declarar el retraso de ~15 min. |
| 13 | Historial (`history`) | `mock-08-historial` | `HistoryPanel` | Timeline real por día con filtros. | — |
| 14 | Estadísticas (`statistics`) | sin mockup | `StatisticsPanel` | Uso, apps top, categorías, bloqueos, cumplimiento; gráficos propios discretos. | Series por día/hora. |
| 15 | Alertas (`alerts`) | `mock-09-alertas` | `AlertsPanel` view=inbox | Bandeja + detalle; INFO · ADVERTENCIA · ALTA · CRÍTICA; rojo solo en crítico. | — |
| 16 | Silenciadas (`silenced`) | `mock-10-silenciadas` | `AlertsPanel` view=silenced | Minimalista: alerta, cuándo se silenció, cuándo vuelve, reactivar. | Calendario de silencios. |
| 17 | Vista remota (`remote`) | `mock-11-vista-remota` | `RemoteViewPanel` | Teléfono grande + EN VIVO, conexión, batería, red, «Finalizar vista remota». | El `<video>` debe seguir **siempre montado** (WebRTC). Aviso de consentimiento visible. |
| 18 | Auditoría (`audit`) | `mock-12-auditoria` | `AuditPanel` | Registro serio: acción, recurso, fecha, usuario, dispositivo; filtros; CSV. | Limpiar auditoría, PDF/Excel. |
| 19 | Perfil (`account`) | `mock-13-perfil` | `AccountPanel` | Página de configuración: cuenta, seguridad, sesiones, preferencias. | Edición de perfil y varias sesiones si la API no los tiene (la sesión es solo Google). |

Faltan mockups de Apps, Reglas, Categorías y Estadísticas: se diseñan con el mismo sistema y la composición descrita.

## Estados vacíos con personalidad

Apps → ilustración de aplicaciones · Reglas → escudo · Auditoría → documentos · Alertas → campana · Silenciadas → campana tachada. Ligeras, sin saturar.

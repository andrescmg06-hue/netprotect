# Manual de usuario — NetProtect

Audiencia: personas que usan NetProtect en el día a día — quien supervisa (Tutor) y quien es
supervisado. Sin jerga técnica. Para el detalle de por qué algo funciona como funciona, ver
`docs/manuals/documento-tecnico.md`.

NetProtect es una única app Android: al iniciar sesión, la app decide sola si te muestra la
pantalla de Tutor o la de Supervisado, según cómo estés vinculado — nunca hay que elegir un modo a
mano.

## 1. Primeros pasos (ambos roles)

1. Instalar la app en el teléfono.
2. Iniciar sesión con una cuenta de Google.
3. La app te lleva automáticamente a tu pantalla: **Tutor** si vas a supervisar, **Supervisado** si
   este teléfono va a ser supervisado.

## 2. Si sos Tutor

### 2.1 Vincular un dispositivo

1. Desde la app (o el panel web) generá un **código de 6 dígitos**.
2. El código vale por unos minutos y sólo se puede usar una vez.
3. En el teléfono que vas a supervisar, ingresá ese código.
4. El dispositivo queda vinculado a tu cuenta de inmediato.

Podés desvincular un dispositivo en cualquier momento — el acceso se corta al instante, no en la
próxima sincronización.

### 2.2 Panel web del tutor

El panel web (`http://<tu-dominio>` o `http://localhost:3000` en desarrollo) agrupa 16 secciones de
navegación en 5 grupos (`frontend/src/lib/dashboardSections.ts`, Sprint 24):

| Grupo | Sección | Para qué sirve |
|---|---|---|
| Cuenta | Perfil y sesión (`AccountPanel`) | Tus datos de cuenta. |
| Dispositivos | Resumen (`OverviewPanel`) | Estado general de tus dispositivos de un vistazo. |
| Dispositivos | Dispositivos (`DevicesPanel`) | Ver, renombrar y desvincular tus dispositivos. |
| Dispositivos | Vinculación (`PairingPanel`) | Generar un código de 6 dígitos nuevo. |
| Control | Aplicaciones instaladas (`DeviceApplicationsList`) | Qué apps tiene instaladas el dispositivo y cuánto las usa. |
| Control | Reglas por aplicación (`AppRulesPanel`) | Permitir, bloquear, poner límite diario/semanal u horario a una app puntual. |
| Control | Política y horario escolar (`DevicePolicyPanel`) | Elegir entre "permitir todo salvo lo que bloqueo" o "sólo lo que apruebo", y configurar el modo escolar. |
| Control | Categorías (`DeviceCategoriesPanel`) | Aplicar una regla a toda una categoría (redes sociales, juegos, streaming, educación, productividad, comunicación, noticias, compras, finanzas, utilidades, contenido adulto) de una vez. |
| Contexto | Ubicación (`DeviceLocationPanel`) | Última ubicación conocida (mapa si hay clave de Google Maps configurada, texto si no). |
| Contexto | Geocercas (`GeofencePanel`) | Crear zonas (casa, colegio) y ver cuándo el dispositivo entra o sale. |
| Contexto | Historial (`HistoryPanel`) | Línea de tiempo de bloqueos y entradas/salidas de geocercas. |
| Contexto | Estadísticas (`StatisticsPanel`) | Apps más usadas y cumplimiento de límites, hoy / 7 días / 30 días. |
| Seguridad | Alertas (`AlertsPanel`) | Bandeja de notificaciones — incluye avisos de manipulación del dispositivo (ver §2.3). |
| Seguridad | Silenciadas (`AlertsPanel`) | Qué tipos de alerta dejaste de recibir para cada dispositivo, y por qué. |
| Seguridad | Vista remota (`RemoteViewPanel`) | Ver la pantalla del dispositivo en vivo, con consentimiento explícito de la persona supervisada (ver §2.4). |
| Seguridad | Auditoría (`AuditPanel`) | Registro de tus propias acciones sobre tus dispositivos. |

La app Android del Tutor (`TutorScreen`) ofrece el mismo control sobre dispositivos, reglas,
ubicación (con enlace a la app de mapas del teléfono), historial, estadísticas y alertas en modo
sólo lectura para algunas secciones — el panel web es la herramienta principal para configurar
reglas.

### 2.3 Alertas de manipulación

Si alguien intenta desinstalar la app, revocar el permiso de uso, detener el servicio de reglas,
cambiar la hora del teléfono, o el dispositivo deja de reportarse sin explicación, te llega una
alerta `HIGH` o `CRITICAL` en la bandeja. NetProtect **nunca bloquea** estas acciones — sólo las
registra y te avisa. La razón: bloquear a la fuerza requeriría privilegios que una app instalada
normalmente (sin ser "propietaria del dispositivo") no tiene, y forzarlos rompería el
funcionamiento normal del teléfono.

### 2.4 Ver la pantalla en vivo

Podés pedir ver la pantalla del dispositivo supervisado desde el panel web. La persona supervisada
recibe un diálogo del propio Android pidiendo su consentimiento — **cada vez**, sin excepción: es
una limitación de la plataforma Android, no una elección de NetProtect (el sistema operativo no
permite guardar ese permiso para usarlo después en silencio). Si lo rechaza, no ves nada.

## 3. Si sos Supervisado

### 3.1 Vincularte

Tu tutor te da un código de 6 dígitos. Lo ingresás en la app y tu teléfono queda vinculado. Podés
ver en todo momento a qué cuenta estás vinculado.

### 3.2 Qué hace la app en tu teléfono

- Aplica las reglas que definió tu tutor (apps permitidas/bloqueadas, límites de tiempo, horarios,
  modo escolar) — cuando abrís una app bloqueada, ves una pantalla propia de NetProtect explicando
  por qué, no un cierre abrupto.
- Sigue aplicando las reglas aunque no tengas Internet en ese momento (usa la última versión que
  descargó).
- Reporta tu ubicación aproximada cada ~15 minutos mientras la app está activa — nunca en segundo
  plano indefinido sin que vos lo veas correr.
- Nunca bloquea el teléfono (llamadas), Ajustes, el launcher, ni la propia app NetProtect, en
  ningún modo.

### 3.3 Tus propios controles

- Podés ver en la app qué reglas tenés activas.
- Si tu tutor pide ver tu pantalla, te llega un diálogo del sistema — vos decidís si aceptar, cada
  vez.
- Podés revocar el permiso de "administrador de dispositivo" (si lo activaste) desde Ajustes en
  cualquier momento; eso genera una alerta para tu tutor, pero no te lo impide.

## 4. Preguntas frecuentes

**¿NetProtect puede filtrar qué páginas web visito?**
No. El filtrado de navegación web por DNS/VPN estaba planeado pero no se construyó (ver
`docs/manuals/analisis-riesgos.md`) — NetProtect controla apps, no sitios web dentro del navegador.

**¿Mi tutor puede ver mi pantalla sin avisarme?**
No. Cada sesión de pantalla compartida exige tu consentimiento explícito en ese momento, por diseño
de Android.

**¿Qué pasa si desinstalo la app?**
Podés hacerlo — no hay ningún mecanismo que te lo impida. Tu tutor recibe una alerta.

**¿Cuánto tiempo se guarda mi ubicación?**
7 días. Ver `docs/manuals/politica-privacidad.md` para el detalle completo de retención por tipo de
dato.

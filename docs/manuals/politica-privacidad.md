# Política de privacidad — NetProtect

Audiencia: cualquier persona cuyos datos pasa por NetProtect — tutor, supervisado, o quien evalúa
el manejo de datos del proyecto. Describe lo que el sistema **realmente hace** hoy, verificado
contra el código citado, no una plantilla genérica. NetProtect es un proyecto académico; esta
política describe su comportamiento técnico real, no constituye un aviso legal de una empresa.

## 1. Qué datos se recolectan y por qué

| Dato | Para qué | Quién lo ve | Base |
|---|---|---|---|
| Nombre, correo, foto (de la cuenta de Google) | Identificar la cuenta | El propio usuario; el tutor no ve el correo del supervisado más allá de lo necesario para la vinculación | Necesario para operar el servicio |
| Inventario de apps instaladas y tiempo de uso diario | Que el tutor pueda definir reglas y ver estadísticas | Tutor del dispositivo | Consentimiento del tutor al vincular; es el propósito central del producto |
| Ubicación aproximada (cada ~15 min) | Última ubicación conocida y geocercas | Tutor del dispositivo | Consentimiento del tutor al vincular y del supervisado al conceder el permiso de ubicación en Android |
| Eventos de bloqueo de reglas | Historial y estadísticas de cumplimiento | Tutor del dispositivo | Propósito central del producto |
| Entradas/salidas de geocercas | Alertas y estadísticas | Tutor del dispositivo | Propósito central del producto |
| Señales de manipulación (permiso revocado, servicio detenido, reloj desfasado, intento de desinstalación) | Alertar al tutor de un intento de evadir la supervisión | Tutor del dispositivo | Propósito central del producto |
| Metadatos del dispositivo (nombre, modelo, versión de Android, versión de la app, zona horaria) | Mostrar el parque de dispositivos y calcular horarios locales | Tutor del dispositivo | Necesario para operar el servicio |
| Token FCM (si Firebase está configurado) | Despertar al dispositivo cuando cambia una regla | Ninguna persona directamente — uso interno del backend | Necesario para el funcionamiento en tiempo real |
| Registro de auditoría de acciones propias | Que cada usuario pueda revisar su propio historial de acciones | Sólo el propio actor (`actor_user_id`) | Trazabilidad y rendición de cuentas |

**Lo que NetProtect no recolecta hoy**: contenido de mensajes, historial de navegación web,
grabaciones de audio/video persistentes (la supervisión remota es en vivo, no se graba —
`docs/sprint-23.md`), contactos, ni contraseñas de Google (nunca se almacenan).

## 2. Cómo se protegen

- **Cifrado en reposo**: la ubicación (latitud/longitud) se cifra con Fernet antes de guardarse en
  PostgreSQL (Sprint 13) — un volcado directo de la base de datos no revela coordenadas en claro.
- **Cifrado en tránsito**: TLS obligatorio en producción, terminado por Caddy con certificado real
  o de su CA interna sin dominio propio (Sprint 21, 26).
- **Cifrado en el dispositivo**: el refresh token de sesión se cifra con AES-256-GCM usando una
  clave del Android Keystore, nunca en texto plano (Sprint 3).
- **Control de acceso**: cada dato de un dispositivo sólo es visible para el tutor vinculado a ese
  dispositivo específico — verificado por fila, no por rol declarado (`docs/security-baseline.md`).
- **Auditoría**: cada acción sensible queda registrada con quién, qué y cuándo (Sprint 2, 3, 22).
- **Secretos de cifrado fuera del repositorio**: la clave de cifrado de ubicación
  (`LOCATION_ENCRYPTION_KEY`) y el resto de secretos viven como archivos en el servidor, nunca
  versionados (Sprint 26).

## 3. Cuánto tiempo se retiene cada dato

| Dato | Retención | Mecanismo |
|---|---|---|
| Ubicación (`DeviceLocationReport`) | 7 días | Purga automática al recibir cada nuevo reporte, sólo de las filas de ese mismo dispositivo (Sprint 13) |
| Eventos de bloqueo de reglas (`AppRuleEvent`) | 90 días | Purga al escribir (Sprint 15) |
| Eventos de geocercas (`GeofenceEvent`) | 90 días | Purga al escribir (Sprint 15) |
| Alertas (`Alert`) | 90 días | Purga al escribir (Sprint 17) |
| Registro de auditoría (`AuditLog`) | Sin retención — registro inmutable, por decisión explícita del plan | Nunca se purga automáticamente (Sprint 22) |

Estos números son configuración del backend (`app/core/config.py`:
`location_retention_days`, `app_rule_event_retention_days`, `geofence_event_retention_days`,
`alert_retention_days`), no un límite de la base de datos — un despliegue real podría ajustarlos,
siempre documentando el cambio.

## 4. Con quién se comparten datos

- **Google**: sólo para verificar la identidad (OAuth/OIDC) al iniciar sesión — NetProtect nunca ve
  ni almacena la contraseña de Google. Si se configura una clave de Google Maps
  (`NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`, pendiente de un humano, Sprint 13), el navegador del tutor
  consulta el mapa embebido directamente con Google al ver una ubicación — sin esa clave, el panel
  muestra sólo coordenadas en texto.
- **Firebase (Google)**: si se configura un proyecto real (pendiente de un humano, Sprint 18), el
  backend envía notificaciones push al dispositivo supervisado vía la API de FCM. Sin proyecto
  configurado, esto no ocurre.
- **Nadie más**: no hay integraciones con terceros de analítica, publicidad, ni venta de datos.

## 5. Derechos de la persona sobre sus datos

- **Acceso**: el tutor ve todos los datos de sus dispositivos vinculados en el panel web/app; el
  supervisado ve las reglas que se le aplican; cualquier usuario puede exportar su propio registro
  de auditoría (`GET /users/me/audit/export`, Sprint 22).
- **Revocación**: el supervisado puede revocar el permiso de "administrador de dispositivo" o
  desinstalar la app en cualquier momento — nada se lo impide técnicamente (esto genera una alerta
  para el tutor, no un bloqueo).
- **Desvinculación**: el tutor puede desvincular un dispositivo en cualquier momento; el acceso se
  corta de inmediato.
- **Borrado**: la retención automática (tabla §3) purga la mayoría de los datos con el tiempo sin
  intervención. **Limitación real, no resuelta hoy**: no existe un endpoint de "eliminar todos los
  datos de esta cuenta/dispositivo ahora" bajo demanda — ver `docs/manuals/analisis-riesgos.md`
  (R17). Una solicitud de borrado inmediato hoy requeriría intervención manual de quien administra
  la base de datos.

## 6. Menores de edad

NetProtect es, por su propia naturaleza, una herramienta de control parental: la persona
"Supervisado" puede ser, y frecuentemente será, un menor de edad. Por eso:

- La recolección de ubicación y uso de apps requiere que el tutor (adulto responsable) inicie la
  vinculación — el supervisado no puede auto-vincularse a un tutor sin ese código.
- Los datos se minimizan al propósito del producto (no se recolecta más de lo necesario para
  reglas, ubicación y alertas).
- No se realiza ningún tratamiento de estos datos con fines distintos al control parental (sin
  publicidad, sin perfilado comercial, sin venta a terceros).

## 7. Preguntas o solicitudes

Al ser un proyecto académico sin una entidad legal operando un servicio comercial, cualquier
solicitud sobre datos personales (acceso, corrección, borrado) debe dirigirse directamente a quien
administra la instancia concreta que se esté usando (ver
`docs/manuals/manual-administrador.md`, sección 7).

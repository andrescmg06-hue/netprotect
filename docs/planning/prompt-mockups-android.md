# Prompt para los mockups de la app Android de NetProtect

> Generado a partir del código real de `mobile/` (pantallas `HomeScreen`, `TutorScreen`,
> `SupervisedScreen`, `BlockScreenActivity`) y de `PRODUCT.md` / `DESIGN.md`. Pegar tal cual en la
> herramienta de mockups.

```
Diseña los mockups de la app Android de NetProtect (plataforma de control parental para una
universidad). Es UNA sola app que funciona en dos modos, "Tutor" y "Supervisado", según lo que
elija la persona tras iniciar sesión. Formato: teléfono Android (412×915 dp), un frame por pantalla,
en español (es-CO).

## Dirección visual (obligatoria)
- Debe sentirse de la misma familia que el panel web de NetProtect ya rediseñado: tema CLARO,
  tarjetas blancas con radios de 12–16 px y sombras suaves, fondo #F5F9FF, tipografía Inter,
  iconos de trazo único (estilo Lucide).
- Colores: azul primario #246BFE (hover #1456D9), azul oscuro #102B63 para títulos, verde #16B364
  (éxito/en línea), rojo #F04438 (error/crítico), naranja #F79009 (advertencia), púrpura #7A5AF8
  (acento), grises azulados para texto secundario (#64748B).
- Sensación: seguridad, confianza, tecnología, simplicidad, control, protección familiar. NO
  infantil, NO oscuro, NO plantilla genérica de admin. Hoy la app es oscura (#121722); el mockup
  la reemplaza por este sistema claro.
- Logo: escudo azul con adulto y niño abstractos; "Net" azul oscuro + "Protect" azul brillante;
  subtítulo "PANEL DEL TUTOR" solo en el modo tutor.
- Móvil primero: objetivos táctiles ≥ 48 dp, contraste WCAG AA, estados vacíos, de carga y de error
  en cada lista. Navegación inferior en modo tutor; en modo supervisado no hace falta.

## Regla que gobierna todo: verdad sobre decoración
Solo se dibuja lo que la app puede hacer de verdad. Nada de botones ni datos que el sistema no
tenga. Los datos de ejemplo pueden ser verosímiles (p. ej. "Tablet de Sofía"), pero jamás
inventes funciones, métricas ni testimonios. Los datos llegan con retraso y NUNCA se presentan
como tiempo real: la ubicación se actualiza cada ~15 min, las entradas/salidas de geocercas se
detectan en el siguiente reporte y el uso se sincroniza periódicamente. Dilo en la interfaz
("Última ubicación conocida · hace 12 min", "Detección aproximada, ~15 min").

## Pantallas comunes (2)
1. **Inicio de sesión**: logo, título "Control parental", texto "Inicia sesión con tu cuenta de
   Google para continuar como tutor o como dispositivo supervisado", botón "Iniciar sesión con
   Google" (solo Google; sin correo/contraseña ni registro), línea pequeña de estado del servicio
   (comprobando / error / listo), y estado de error del inicio de sesión.
2. **Elegir cómo se usa este dispositivo**: saludo "Hola, {nombre}", pregunta "¿Cómo vas a usar
   este dispositivo?", dos tarjetas: "Soy tutor — Superviso otros dispositivos desde este teléfono
   o tablet." y "Este es el dispositivo supervisado — Este teléfono es el que un tutor va a
   supervisar."; botón "Elegir" en cada una y "Cerrar sesión".

## Modo Tutor
3. **Panel del tutor (inicio)**: cabecera "Modo Tutor" con "Cambiar de modo"; tarjeta de
   vinculación: "Generar código de vinculación" → código grande de 6 dígitos con cuenta atrás de
   vigencia, "Revocar", y "¿Ya se vinculó? Actualizar lista"; lista "Dispositivos vinculados"
   (nombre, plataforma, estado En línea / Desconectado, "Actualizar"); estado vacío "Todavía no hay
   dispositivos vinculados."; enlace "Mi actividad (auditoría)"; "Cerrar sesión".
4. **Tarjeta de dispositivo**: nombre, estado, acciones Renombrar (campo "Nombre" + Guardar/
   Cancelar) y Desvincular (con diálogo de confirmación), y accesos a: Apps, Ubicación, Geocercas,
   Historial, Estadísticas, Alertas.
5. **Apps del dispositivo**: lista con icono, nombre, tiempo de uso y etiqueta "Desinstalada" en las
   que ya no están; estado de carga y vacío.
6. **Ubicación**: última ubicación conocida, precisión y hora de la lectura, botón "Abrir en mapa"
   (abre la app de mapas externa; no hay mapa embebido con teselas, a propósito, por privacidad del
   menor). Aviso de que es aproximada y con retraso.
7. **Geocercas** (solo consulta en el móvil): lista de geocercas y "Historial de entradas/salidas"
   (Entró/Salió + nombre + hora), con el aviso de detección aproximada (~15 min).
8. **Historial**: línea de tiempo agrupada por día, con eventos de bloqueo y de geocerca.
9. **Estadísticas**: selector Hoy / 7 días / 30 días; "Apps más usadas" (barras), "Bloqueos" por
   tipo, "Cumplimiento de límites diarios" (barras de progreso); estados "Sin datos de uso.",
   "Ninguno en este periodo.", "Sin reglas de límite diario.".
10. **Alertas**: lista con nivel INFO / WARNING / HIGH / CRITICAL (icono + color + texto, nunca solo
    color), repeticiones y hora; vacío "Sin alertas para este dispositivo.".
11. **Mi actividad (auditoría)**: registro de las acciones del propio tutor (acción + recurso +
    hora). Es de solo lectura: no se puede borrar ni editar; dilo en la interfaz.

## Modo Supervisado
12. **Vincular este dispositivo**: campo "Código" de 6 dígitos, botón "Vincular dispositivo", estado
    "Comprobando vínculo…" y mensaje de error ("No se pudo vincular").
13. **Dispositivo vinculado**: "Dispositivo vinculado", "Supervisado por {tutor}", y el texto
    honesto "Este dispositivo reporta su estado cada minuto mientras la app esté abierta."; lista
    de permisos pendientes/concedidos; "Cambiar de modo" y "Cerrar sesión".
14. **Permisos**, una tarjeta por permiso con explicación clara de para qué sirve y botón:
    - "Acceso a uso de apps" — "Abrir Ajustes" + "Ya lo activé, verificar de nuevo".
    - "Ubicación aproximada" — "Permitir ubicación aproximada" (solo aproximada, nunca precisa ni en
      segundo plano).
    - "Mostrar sobre otras apps" — "Permitir" (sin él la pantalla de bloqueo no puede cubrir una app).
    - "Protección contra desinstalación" — "Activar protección" (avisa al tutor si alguien intenta
      desinstalar; NO permite borrar el dispositivo ni cambiar contraseñas).
15. **Solicitud de vista remota (consentimiento)**: "Tu tutor quiere ver esta pantalla", texto de
    que Android pedirá confirmarlo otra vez y se verá un aviso mientras dure; botones "Aceptar" y
    "Ahora no". Nunca se graba ni se controla el dispositivo. Incluye la notificación persistente
    visible durante toda la sesión.
16. **Pantalla de bloqueo "APP BLOQUEADA"** (cubre la app): nombre de la app y motivo, en sus 7
    variantes: "Tu tutor bloqueó esta app." · "Ya usaste el tiempo diario permitido para esta app."
    · "Ya usaste el tiempo semanal permitido para esta app." · "Esta app está bloqueada en este
    horario." · "Tu tutor bloqueó la categoría a la que pertenece esta app." · "Es horario escolar
    y esta app no está aprobada para este momento." · "Este dispositivo sólo permite las apps que
    tu tutor aprobó, y ésta no está aprobada."; botón "Ir al inicio". Tono claro y respetuoso, no
    punitivo; nunca bloquea Teléfono, Ajustes ni la propia app.
17. **Notificaciones del sistema** (foreground services, obligatorias en Android): reporte de estado
    y ubicación en curso, y vista remota en curso. Transparentes para el menor.

## Fuera de alcance: NO dibujar (la app hoy no lo hace)
Crear o editar reglas por app/categoría, política por defecto, horario escolar, crear geocercas,
ver la pantalla del supervisado desde el móvil del tutor (eso se hace en el panel web), filtrado
web/VPN, bloquear o apagar el dispositivo, grabar o capturar pantalla, ubicación en tiempo real,
chat, "me gusta"/seguidores, exportar PDF/Excel, borrar la auditoría, registro con correo. Si
crees que alguna debería existir en el móvil, márcala aparte como PROPUESTA FUTURA, nunca como
parte del diseño base.

## Entregables
Un frame por cada pantalla numerada (1–17), más un mini sistema de diseño (colores, tipografía,
botones, tarjetas, badges de estado, iconos), estados vacío/carga/error de las listas, y un flujo
que una las pantallas: inicio de sesión → elegir modo → (tutor: panel → dispositivo → sección) o
(supervisado: vincular → permisos → vinculado → consentimiento / bloqueo).
```

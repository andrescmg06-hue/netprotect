# Sprint 28 — Servidor TURN y credenciales efímeras

## Objetivo

Primer sprint del plan `docs/planning/plan-turn.md` (Sprints 28–30). Hace que coturn corra en los
tres entornos y que el backend emita credenciales efímeras que ese coturn acepte de verdad.

Parte de una verificación en vivo del 24/09/2026: la señalización de la vista remota del
Sprint 23 funciona de punta a punta (auditoría con `SCREEN_SHARE_REQUESTED`,
`SCREEN_SHARE_CONSENT_GRANTED` y `SCREEN_SHARE_STARTED`), pero el video no llega. Sin TURN no hay
camino entre un emulador y el navegador del host, ni entre dos redes móviles reales. Este sprint
no toca todavía los clientes: eso es el Sprint 29.

## Historias de usuario

| ID | Historia |
|---|---|
| HU-090 | Como responsable de infraestructura, quiero un servidor TURN corriendo en dev, test y prod, con configuración versionada y endurecida, para que la retransmisión exista en los tres entornos. |
| HU-091 | Como responsable de seguridad, quiero que el backend emita credenciales TURN efímeras sólo a los participantes del dispositivo, para que ninguna credencial fija viaje en la APK ni en el navegador. |

## Criterios de aceptación y resultado

| # | Criterio | Resultado |
|---|---|---|
| 1 | coturn arranca en los tres entornos, con la configuración versionada y sin secretos en el repo. | Cumplido. Dev y test levantados en vivo; el camino de producción (secreto por archivo + `external-ip`) se verificó arrancando coturn con esas variables. Ver la evidencia, tarea 2. |
| 2 | Una credencial emitida por el backend logra un Allocate real en coturn, y una caducada o alterada no. | Cumplido, con `scripts/verify_turn.sh` contra el coturn endurecido de test (relayed / rejected / rejected) y un control negativo. |
| 3 | Un usuario que no participa en el dispositivo no obtiene credenciales (404). | Cumplido. `test_an_unrelated_tutor_cannot_read_the_webrtc_config` sigue en verde: el endpoint conserva `require_device_participant`. |
| 4 | El relay rechaza a un peer en una IP privada. | Cumplido: `channel bind: error 403 (Forbidden IP)` contra un host interno, en el spike y en el coturn de desarrollo. |
| 5 | La suite completa del backend y CI pasan en verde. | Suite: 273/273 en `compose.test.yaml`. CI: ver `sprint-28-evidence.md`, sección de CI. |

## Decisiones de diseño y su motivo

Las decisiones de partida (D1–D8) están en `docs/planning/plan-turn.md`. Aquí sólo figura lo que
el sprint confirmó, cambió o agregó al verificarlo.

### coturn con IP fija, solo en su propia red (confirmada por el spike R1)

Bloquear las redes privadas (`denied-peer-ip`) es obligatorio: sin eso, un cliente autenticado
podría usar el relay para llegar a PostgreSQL o Redis. Pero el spike mostró que ese mismo bloqueo
corta el relay entre dos clientes del mismo coturn, porque la dirección de relay está dentro de
la red de Docker. La solución verificada es volver a permitir sólo la IP propia de coturn
(`allowed-peer-ip`). Eso exige una IP fija (`ipv4_address`); si no, cada recreación del
contenedor rompería el relay, o habría que abrir todo el rango. Además, coturn va en una red
aparte y nunca en la de la base de datos: el bloqueo por configuración es la segunda barrera, no
la única.

### `user-quota=16`, medido y no supuesto

La primera versión tenía `user-quota=4`, y rechazó una sesión legítima con
`486 Allocation Quota Reached`. Una sola conexión WebRTC abre una asignación por cada interfaz de
red local × URL de TURN × transporte, y un PC con Windows y adaptadores de Docker/WSL tiene
varias. Se midió el umbral (4 falla, 8 pasa) y se fijó en 16. `total-quota=40`, porque no tiene
sentido pasar de los 41 puertos de relay disponibles.

### El secreto nunca en argv ni en variables de producción

coturn no tiene convención `_FILE`, y un valor pasado como argumento lo puede leer cualquiera que
liste procesos. `infra/coturn/entrypoint.sh` escribe la configuración de ejecución en un archivo
`0600` y en producción lee el secreto de un Compose secret, igual que el backend desde el
Sprint 26. Sin secreto, coturn se niega a arrancar, en vez de quedar abierto sin autenticación.

### Nonce aleatorio en el usuario TURN, no el `user_id`

La parte después de los dos puntos del usuario TURN acaba en los logs de coturn. Un nonce nuevo
en cada emisión evita dejar ahí un identificador de la cuenta, y además convierte la cuota "por
usuario" de coturn en una cuota por credencial.

### Contrato compatible: `turn_servers` es un campo nuevo

`ice_servers` sigue siendo la lista de textos del Sprint 23, que la APK instalada parsea tal cual.
Las credenciales viajan en `turn_servers`, que un cliente viejo ignora.

### Por qué la prueba real contra coturn es un script y no un test de pytest

pytest corre dentro del contenedor `backend`, que no está en la red de coturn ni tiene un cliente
TURN, y el proyecto no simula lo que se puede probar de verdad. `scripts/verify_turn.sh` usa el
código propio del backend para emitir las credenciales y `turnutils_uclient` (incluido en la
imagen de coturn) para probarlas. CI lo ejecuta en el job `integration`.

## Qué queda para los sprints siguientes

- **Sprint 29:** el panel web y la app Android usan `turn_servers`. Allí se verifica el acceso
  por el puerto publicado desde el navegador (`localhost`) y desde el emulador (`10.0.2.2`), que
  este sprint no cubre.
- **Sprint 30:** prueba con relay forzado, manuales y el despliegue con servidor real (pendiente
  de un humano, como en el Sprint 26).
- **Backlog:** mensaje claro cuando la misma cuenta es tutor y supervisado del mismo dispositivo
  (hoy el backend descarta la solicitud en silencio).

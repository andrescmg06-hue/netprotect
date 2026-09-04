# Sprint 5 — Vinculación tutor ↔ dispositivo

## Objetivo

El flujo del código de 6 dígitos, completo y con la protección que un secreto tan corto exige: el
tutor genera un código, el dispositivo supervisado lo canjea, y a partir de ahí existe un vínculo
real (`tutor_devices`) que el resto del sistema puede autorizar. Incluye desvinculación y revocación.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-014 | Como tutor, quiero generar un código temporal para vincular un dispositivo. |
| HU-015 | Como supervisado, quiero introducir el código de mi tutor para vincular mi dispositivo. |
| HU-016 | Como tutor, quiero revocar un código que generé por error, antes de que alguien lo use. |
| HU-017 | Como tutor, quiero desvincular un dispositivo y perder el acceso de inmediato. |

## Criterios de aceptación

1. El código tiene exactamente 6 dígitos, se genera con el CSPRNG y vive 3 minutos.
2. Es de un solo uso: el segundo canje del mismo código falla.
3. Generar un código nuevo retira el anterior del mismo tutor.
4. El tutor puede revocar su código activo; después ya no sirve.
5. Un código desconocido, expirado, usado o revocado devuelven **exactamente la misma respuesta**.
6. Los intentos repetidos de canje se limitan por cuenta y por IP, y responden 429 con `Retry-After`.
7. Sólo un TUTOR genera códigos; sólo un SUPERVISADO los canja.
8. El mismo teléfono vinculándose dos veces reutiliza una sola fila de `devices`.
9. Un tutor desvincula su dispositivo y pierde el acceso; otro tutor no puede desvincularlo (404).
10. El código en claro nunca se guarda ni se registra en la auditoría.

## Decisiones de diseño relevantes

- **HMAC con clave de servidor, no hash a secas.** Un código de 6 dígitos tiene 10⁶ valores
  posibles: quien pueda leer `pairing_codes` recorre ese espacio completo en microsegundos, así que
  ni un hash simple ni un salt por fila (que vive en la misma fila) protegen nada. La clave
  (`PAIRING_CODE_PEPPER`) vive en la configuración de la aplicación, fuera de la base, de modo que
  recuperar un código exige comprometer **las dos cosas**, no una. Es un secreto distinto de
  `JWT_SECRET` a propósito: filtrar uno no debe filtrar el otro.
- **Una sola respuesta para todos los fallos.** `invalid_or_expired_code` cubre desconocido,
  expirado, usado y revocado. Distinguirlos le diría a quien adivina dígitos que acertó a un código
  real pero llegó tarde, que es justo la señal que no debe tener.
- **Canje serializado con `SELECT ... FOR UPDATE`.** Dos peticiones simultáneas con el mismo código
  se ordenan en la base: la segunda espera y encuentra `used_at` ya puesto. Sin ese bloqueo, ambas
  podrían crear un vínculo.
- **Rate limiting que falla cerrado.** Si Redis no responde, el canje devuelve 503 en lugar de
  pasar sin control: una protección contra fuerza bruta que desaparece cuando la caché se cae no es
  protección. El contador se incrementa y recibe su TTL en un único script Lua, para que nunca quede
  una clave sin expiración capaz de bloquear a alguien para siempre.
- **`revoked_at` es una columna aparte de `used_at`.** "El tutor canceló el código" y "un dispositivo
  lo canjeó" son hechos distintos; mezclarlos ensuciaría la auditoría.
- **Identidad estable del dispositivo (`device_instance_id`).** La app genera un identificador una
  vez y lo conserva; el backend lo usa para reconocer el mismo teléfono. Sin esto, vincularse a un
  segundo tutor crearía un dispositivo duplicado. El índice único está acotado al usuario supervisado
  para que una cuenta no pueda reclamar el identificador de otra.
- **Colisión de códigos resuelta al generar, no al canjear.** Si el código sorteado coincide con otro
  activo, se sortea otro; así el canje nunca es ambiguo. Tras varios intentos fallidos se responde
  503 en lugar de entregar un duplicado.
- **El supervisado ve quién lo supervisa.** El canje responde con el nombre y correo del tutor: quien
  lleva el teléfono tiene derecho a saber quién quedó vigilándolo.

## Cambios de infraestructura

`app/cache/redis_client.py` pasó de una implementación artesanal del protocolo RESP (sólo servía para
el `PING` del health check) al cliente oficial `redis` con pool de conexiones. Mantener dos caminos
distintos hacia Redis —uno hecho a mano para el health check y otro para el limitador— habría sido
peor que consolidar; además, un parser de protocolo escrito a mano es exactamente el tipo de código
donde se esconden errores sutiles.

## Ejecución

```bash
docker compose up --build -d

# tutor
curl -X POST http://localhost:8000/api/v1/pairing/codes -H "Authorization: Bearer <token_tutor>"

# supervisado
curl -X POST http://localhost:8000/api/v1/pairing/redeem \
  -H "Authorization: Bearer <token_supervisado>" -H "Content-Type: application/json" \
  -d '{"code":"739482","device_instance_id":"<uuid>","device_name":"Celular de Juan"}'
```

## Verificación

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
```

## Seguridad del sprint

- El código en claro sólo existe en la respuesta que lo entrega; en la base vive su HMAC.
- La auditoría registra `PAIRING_CODE_GENERATED`, `PAIRING_CODE_REVOKED`, `DEVICE_LINKED` y
  `DEVICE_UNLINKED` con IP de origen, y **nunca** el código.
- Los contadores de rate limiting guardan un identificador de cuenta o una IP con TTL de 15 minutos:
  no persisten más allá de la ventana que necesitan.

## Fuera de alcance

Listado y detalle de dispositivos, renombrado y estados de conexión (Sprint 6). La pantalla de
vinculación en Android y web (este sprint entrega la API; los clientes la consumen en el Sprint 6,
junto con la gestión de dispositivos que les da sentido en pantalla).

## Definition of Done del Sprint 5

Los 10 criterios de aceptación tienen prueba automatizada contra PostgreSQL y Redis reales: 14
pruebas de vinculación y 4 del limitador, 41 en total en la suite, verdes dos veces seguidas en
contenedor limpio. Ver `docs/sprint-05-evidence.md`.

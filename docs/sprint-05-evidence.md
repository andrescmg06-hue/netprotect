# Evidencia de verificación — Sprint 5

Fecha: 04/09/2026. Equipo: `andre`, Windows 11 Pro.

## Migración

```text
alembic upgrade head    → e27f40867c61 -> 7dbb7e44e8b7, pairing: revoked_at and stable device instance id
alembic downgrade -1    → revierte limpiamente
alembic upgrade head    → reaplicada
```

Añade `pairing_codes.revoked_at`, `devices.device_instance_id` y el índice único parcial
`uq_devices_instance_per_supervised_user`.

## Suite completa en contenedor

```text
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend

→ backend-1 | 41 passed, 3 warnings in 3.60s   (primera corrida)
→ backend-1 | 41 passed, 3 warnings in 3.48s   (segunda corrida, base y Redis recreados)
→ backend-1 exited with code 0
```

18 pruebas nuevas: 14 de vinculación (`tests/test_pairing_integration.py`) y 4 del limitador
(`tests/test_rate_limit_integration.py`), todas contra PostgreSQL y Redis reales.

Cubren: formato y vigencia del código; vínculo creado con estado `ONLINE`; auditoría del vínculo
**sin** el código; un solo uso; código expirado; generar uno nuevo retira el anterior; revocación
explícita; **las cuatro formas de fallo devuelven idéntica respuesta**; sólo TUTOR genera y sólo
SUPERVISADO canjea; el mismo teléfono reutiliza una fila de `devices` y queda con dos tutores;
desvincular quita el acceso y deja el estado en `UNLINKED`; un tutor ajeno recibe 404; y los intentos
repetidos terminan en 429 con `Retry-After`.

## Dos problemas reales encontrados al verificar

**1. Las pruebas mezclaban dos event loops.** Los tests que usan a la vez `TestClient` (que corre la
app en su propio loop) y el fixture de base de datos (que usaba el motor global de la aplicación desde
el loop de pytest) hacían que asyncpg recibiera conexiones nacidas en otro loop. En este equipo el
síntoma era ruido lento e intermitente; **dentro del contenedor fallaban 6 de 41 de forma
determinística**. Corregido con un `tests/conftest.py` cuyo fixture crea su propio motor
(`NullPool`) dentro del loop del test, y eliminando los cuatro fixtures duplicados que cada archivo
tenía. Es exactamente el tipo de fallo que se esconde hasta llegar a CI.

**2. El estado del rate limiting se filtraba entre corridas.** Todas las pruebas compartían la misma
IP de origen, y los contadores viven 15 minutos en Redis: al correr la suite dos veces seguidas, el
presupuesto por IP se agotaba y las últimas pruebas recibían 429 en lugar del resultado esperado.
Corregido dando a cada prueba un host de origen único.

También se ajustó `close_redis()` para que un fallo al cerrar el socket durante el apagado no se
propague: cerrar un cliente creado en otro loop lanzaba `RuntimeError` desde el lifespan de la
aplicación, y un error al cerrar nunca es accionable para quien llama.

## Nota sobre los tiempos locales

La misma suite tarda ~4 segundos dentro del contenedor y varios minutos ejecutada desde Windows
contra los puertos publicados: cada petición paga ~1.4 s en el proxy de puertos de Docker Desktop.
No es un problema del código; simplemente la verificación de referencia es la de contenedor, que es
además la que corre CI.

## CI en GitHub Actions

Run `33885229903`, los 4 jobs en verde: `backend` (24s), `frontend` (32s), `integration` (52s),
`android` (1m32s).

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con salida real, incluidos los dos defectos de
las pruebas encontrados y corregidos. La sección de CI se completa cuando el pipeline corra.

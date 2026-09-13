# Prueba de rendimiento (Sprint 25)

`load-test.js` es un perfil de carga **moderado y repetible**, no una prueba de estrés — ver
`docs/sprint-25.md` (decisión de diseño) para el porqué: sin infraestructura de producción real
(Sprint 26 sigue pendiente), medir un punto de quiebre hoy sólo describiría el contenedor de
desarrollo de quien lo ejecute, no el despliegue real. El valor de este script es servir de
referencia fija para comparar contra una corrida futura, no un veredicto de capacidad.

## Requisitos

- El binario de `k6` (no se versiona; en Windows sin permisos de administrador se puede usar el
  `.exe` portable de un release de GitHub sin instalar nada).
- El stack de `compose.test.yaml` con `db`, `redis` y `api_server` arriba (ver
  `docs/sprint-25.md`).

## Correrla en local

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up -d db redis api_server
K6_BIN=/ruta/a/k6.exe ./perf/run.sh
```

`run.sh` siembra un tutor, un supervisado y un dispositivo vinculado reales (mismo mecanismo que
`api-tests/` y `frontend/e2e/`), y corre `load-test.js` contra ellos: 10 usuarios virtuales
sostenidos 30 segundos (con rampas de 10s/5s), ejerciendo `GET /devices`, `GET /devices/{id}` y
`GET /devices/{id}/rules/active` — el tráfico de lectura real que un panel de tutor y un
dispositivo supervisado generan.

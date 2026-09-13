# Colección de API (Sprint 25)

`netprotect.postman_collection.json` es una colección Postman v2.1 ejercitada con Newman en el job
`api-collection` de CI. Cubre un flujo real de tutor + supervisado de extremo a extremo contra el
backend real (Docker): roles, vinculación por código, reglas de app, reglas activas, heartbeat, y
el caso anti-IDOR (un tutor sin relación con el dispositivo debe recibir 404).

## Por qué necesita un entorno generado, no credenciales fijas

`/auth/google` exige un ID token real de Google — Newman es una herramienta de caja negra, no
puede hacer lo que hacen los tests de pytest (`unittest.mock.patch` sobre la función que llama a
Google). Antes de correr Newman, un paso de CI ejecuta
`backend/scripts/seed_test_session.py` dentro del contenedor del backend (mismo mecanismo, sin
mockear nada de la lógica propia, ver el docstring de ese script) y usa su salida JSON para generar
`environment.json` con los tokens ya válidos. Ver `.github/workflows/ci.yml`, job
`api-collection`.

## Correrla en local

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up -d db redis api_server
SEED=$(docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py)
node api-tests/build_environment.js "$SEED" > api-tests/environment.json
npx newman run api-tests/netprotect.postman_collection.json -e api-tests/environment.json
```

# Secretos de producción

`compose.prod.yaml` lee estos ocho secretos como archivos (bloque `secrets:` de Docker Compose,
montados en `/run/secrets/<nombre>` dentro de cada contenedor), no como variables de entorno en
texto plano — así nunca aparecen en `docker inspect` ni en un `.env` persistente en el servidor.

En un despliegue real, cada archivo se genera **en el propio servidor de producción**, a partir de
lo que ofrezca el gestor de secretos elegido en el Paso 25 (Vault, el secret manager del proveedor
cloud, o un archivo cifrado con SOPS/age descifrado sólo en el momento de arrancar) — nunca se
copian desde esta máquina de desarrollo ni se commitean.

Archivos esperados en este directorio (ninguno se versiona, ver `.gitignore`):

| Archivo | Contenido | Cómo generarlo |
|---|---|---|
| `postgres_password.txt` | contraseña de PostgreSQL | `python -c "import secrets; print(secrets.token_urlsafe(32))"` |
| `redis_password.txt` | contraseña de Redis | igual que arriba |
| `database_url.txt` | `postgresql+asyncpg://<user>:<postgres_password>@db:5432/<db>` | ensamblado a mano combinando `POSTGRES_USER`/`POSTGRES_DB` (`.env.production`) con `postgres_password.txt` |
| `redis_url.txt` | `redis://:<redis_password>@redis:6379/0` | ensamblado a mano combinando con `redis_password.txt` |
| `jwt_secret.txt` | secreto HS256 para tokens propios | `python -c "import secrets; print(secrets.token_urlsafe(48))"` |
| `pairing_code_pepper.txt` | HMAC pepper de códigos de vinculación | igual que arriba — nunca reutilizar `jwt_secret.txt` |
| `location_encryption_key.txt` | clave Fernet para ubicación cifrada | `python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"` |
| `grafana_admin_password.txt` | contraseña del usuario admin de Grafana | `python -c "import secrets; print(secrets.token_urlsafe(24))"` |

`generate-dev-secrets.sh` (versionado, en este mismo directorio) rellena estos ocho archivos con
valores aleatorios de un solo uso — sirve para levantar `compose.prod.yaml` localmente sin dominio
ni cuenta cloud reales (ver `docs/sprint-26-evidence.md`), **no** para producción real: en
producción real cada valor sale del gestor de secretos del proveedor elegido, nunca de un script
en este repositorio.

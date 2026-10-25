# Sitios estáticos de otros proyectos

El servidor de producción puede alojar páginas web estáticas ajenas a NetProtect (por ejemplo, un
trabajo de otra materia) **sin tocar este repositorio cada vez** y sin darles acceso a la aplicación.

## Cómo está montado
```
navegador ──443──> caddy ──(red edge)──> sites:8080 ──> /srv/sites/www/<dominio>/
                     │
                     └── lee fragmentos de /srv/sites/conf  (uno por sitio)
```
- **`caddy`** termina TLS y gestiona un certificado por dominio. Solo ve los **fragmentos** de
  configuración (`/srv/sites/conf`, montado como `/etc/caddy/sites.d`, solo lectura), nunca los
  archivos de los sitios. El `Caddyfile` los incorpora con `import /etc/caddy/sites.d/*.caddy`.
- **`sites`** (`nginxinc/nginx-unprivileged`) sirve los archivos desde `/srv/sites/www/<dominio>/`.
  Está aislado a propósito: usuario sin privilegios, sistema de archivos de solo lectura, sin
  capacidades, 64 MB, **sin secretos ni base de datos montados**.
- Los fragmentos y los directorios de los sitios viven **en el servidor, fuera del repositorio**.
  Los crea un script de `root` que valida cada nombre y dominio; este repositorio no contiene
  ningún sitio ajeno.

## Garantías verificadas (25-oct-2026, con contenedores desechables)
| Prueba | Resultado |
|---|---|
| `caddy validate` sin carpeta de fragmentos, o con ella vacía | válido: el estado inicial no cambia nada |
| Un fragmento que redefine `{$API_DOMAIN}` | **rechazado**: `ambiguous site definition`; no puede secuestrar la API ni el panel |
| Un fragmento con sintaxis rota | rechazado |
| `nginx -t` con `infra/sites/nginx.conf` | correcto |
| Host que no sea `<algo>.netprotectapp.online` | conexión cerrada (sin respuesta) |
| Archivo oculto (`.secret`), enlace simbólico hacia el sistema, `../../etc/passwd` | 404, 404 y 400: no se sirve nada |
| Límites del contenedor | 64 MB, solo lectura, sin capacidades, usuario 101 |

## Qué NO cubre
- Aplicaciones con servidor, bases de datos o formularios que guarden datos: solo estático.
- Dominios fuera de `*.netprotectapp.online`. `panel.` y `api.` están reservados.
- El DNS de cada sitio (un registro A en Hostinger) lo crea el dueño del dominio.

## Orden al desplegarlo por primera vez
1. En el servidor deben existir antes `/srv/sites/conf` (de `root`) y `/srv/sites/www` (del usuario
   que sube los sitios). Si no, Docker los crea como `root` al montar y ese usuario no podrá escribir.
2. Fusionar este cambio, actualizar el clon y `docker compose … up -d`: **recrea `caddy`**, unos
   segundos sin servicio para el panel y la API, y arranca `sites`.
3. Comprobar que `panel.` y `api.` siguen respondiendo.

## Reversión
Quitar la línea `import` del `Caddyfile`, el volumen `/etc/caddy/sites.d` de `caddy` y el servicio
`sites`, y redesplegar. Los sitios dejan de servirse; el panel y la API no se ven afectados.

# Verificación del rediseño integrado (Sprints 54-59)

Capturas y prueba E2E del panel web ya recompuesto en la dirección editorial. No son una prueba automática de
regresión visual: se miran a ojo.

- Fecha: 04/10/2026
- Rama `sprint-54-rediseno-web-marco`, HEAD `8be5abd` (árbol limpio salvo `compose.web.yaml` sin versionar).
- Aquí van **18 PNG** (los 15 de 1440x900 pedidos: login + 14 vistas, y 3 de 390x844: `overview`, `policy`,
  `geofences`). El juego completo (34 PNG, 16 vistas + login a 1440 y 390, más `overview-1000` y
  `overview-390-drawer`) se generó y verificó fuera del repo.

## Comandos ejecutados

Docker desde Git Bash con `MSYS_NO_PATHCONV=1`.

```bash
docker compose -f compose.yaml -f compose.web.yaml stop web backend      # libera 3000 y 8000 (sin down)
docker compose -f compose.test.yaml build backend migrate api_server
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up -d db redis api_server
docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py   # una sesión para e2e, otra para capturas
cd frontend && npm run build && git checkout -- next-env.d.ts
node -e "...cpSync('public',...); cpSync('.next/static',...)"
PORT=3000 HOSTNAME=0.0.0.0 node .next/standalone/server.js               # en segundo plano
E2E_BASE_URL=http://localhost:3000 E2E_SEED_JSON=<seed> npx playwright test
node <seed-s59.cjs> ; node <capture-s59.cjs>                              # datos reales por API + capturas
```

## Resultado del E2E (`frontend/e2e/dashboard.spec.ts`)

**PASA** (1 passed, 15,7 s) contra el build de producción standalone recompuesto: la navegación "Secciones del
panel", un solo h1 por vista, el botón del dispositivo por nombre y `com.instagram.android` una sola vez.
Salvedad: se sirvió el standalone a mano y se pasó `E2E_BASE_URL` (el `webServer` de la config hace el mismo
build/copy), en vez de dejar que Playwright lo arranque.

## Datos sembrados (solo por la API real, sin escribir en la base)

Un tutor y un dispositivo `S59 Pixel de Sofía` vinculado; latido con permisos OK; inventario de 11 apps y uso de
hoy más 5 días previos; reglas por app de los 5 tipos (Instagram BLOCK, Duolingo ALLOW, YouTube DAILY_LIMIT 90,
Netflix SCHEDULE 20:00-22:00 L-V, Clash of Clans WEEKLY_LIMIT 420); 9 apps asignadas a categorías y 3 reglas de
categoría (Redes sociales límite diario, Juegos horario, Contenido para adultos bloquear); modo escolar
07:00-14:00 L-V activado; política ALLOW; 2 geocercas (Colegio San Rafael 300 m, Casa 150 m); 4 ubicaciones
(fuera, dentro, dentro, fuera: genera ENTRADA y SALIDA); 5 bloqueos (BLOCK x2, DAILY_LIMIT, SCHOOL_MODE,
CATEGORY); 6 alertas generadas, una silenciada 7 días (TikTok) y una marcada como leída.

## Qué se comprobó

34 PNG no vacíos (login + 16 vistas, 1440 y 390) más `overview-1000` (riel) y `overview-390-drawer`; sesión continua
con cambio de hash, sin recargas; sin errores de consola, errores de página ni respuestas HTTP >= 400; ningún
"Cargando" ni "…" en pantalla; ninguna vista con desbordamiento horizontal del documento (scrollWidth = ancho de
ventana en las 32). Todas las imágenes enumeradas se abrieron y se miraron una por una.

## Defectos encontrados

Ninguno bloqueante. Por vista:

- **geofences-390**: la etiqueta de la geocerca "Colegio San Rafael" y la de "Última ubicación" se superponen en el
  mapa (se lee "C Última ubicación el"). Es el defecto más claro.
- **statistics** (1440 y 390): en la dona "Por categoría" el color azul se repite (Redes sociales y Sin
  categoría), y Streaming/Comunicación quedan verde y verde-azulado casi iguales; a 390 "Redes sociales" se trunca
  como "Redes socia…".
- **account-390**: el correo parte a mitad de palabra ("tutor-c8654feefb@exa / mple.com"), el nombre ocupa 3 líneas
  y la etiqueta "Cuenta de Google" queda apretada. A 1440 el contenido termina en x=1342 mientras las demás vistas
  llegan a 1408 (columna más angosta).
- **history** (1440 y 390): la hora se parte en dos líneas ("10:30 a. / m.") en cada fila.
- **alerts-390**: la tira de contadores (Todas/Info/Advertencia/Alta/Crítica) queda cortada a la derecha (se ve
  un "0" a medias); es desplazable pero no se nota.
- **audit** (1440 y 390): se muestran identificadores crudos (`alert · c91ef6dc-…`, `app_category_assignment · uuid`,
  `alert_silence · APP_BLOCKED:com.tiktok.android`).
- **Títulos h1 en Playfair** (todas las vistas): en las capturas 1x la barra de la `e` casi no se ve y se leen
  como "Gcoccrcas", "Alcrtas", "Vista rcmota". En un recorte 1:1 (`Perfil y sesión`, 44 px, peso 600) la barra
  sí existe pero es muy fina; no se pudo confirmar en pantalla real ni a 2x (el recorte a 2x no cambió de
  tamaño). Ya anotado en S53; sigue pendiente de mirarlo en un monitor real.
- **Cifras con pie** en "04 OCT 2026" del Historial: el 6 de 2026 se lee como 8 a ese tamaño (no verificado con
  zoom).
- Menores: los números de coordenadas en mono muestran un espacio tras el signo ("- 74.07210"); el sidebar mide
  100vh en las capturas fullPage y deja ver solo hasta "Auditoría" (comportamiento esperado de la captura, con
  scroll interno).

Sin hallazgos de: sidebar/encabezado/banda rotos, estados seleccionado o hover invisibles, dos azules
compitiendo, tarjetas en cuadrícula residuales, vistas vacías con aspecto roto. Los tres layouts del marco
(sidebar completo a 1440, riel a 1000, cajón a 390) se ven correctos.

## Salvedades

- El login muestra "Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID." (el build de prueba no define esa
  variable); el botón real de Google no se pudo capturar.
- `remote` solo en estado inactivo (la sesión real exige a una persona en un teléfono).
- No se comprobó con capturas que el cajón de 390 desplace su lista hasta "Auditoría" y "Cerrar sesión"; en la
  imagen del cajón la lista se corta en "Vista remota".
- No se midió el contraste con herramienta; solo a ojo.

# Capturas DESPUÉS de la base visual (Sprint 53, T7)

Capturas del panel con los tokens y las primitivas nuevas (cambios sin confirmar en `frontend/`), hechas con el mismo
procedimiento que la línea base de `docs/redesign/baseline/` para poder compararlas a ojo. No son una prueba
automática.

- Fecha: 04/10/2026
- Rama `sprint-53-rediseno-web-base-visual`. Al empezar, HEAD era `4356204` y lo verificado fue ese commit más los
  cambios sin confirmar de `frontend/` y `DESIGN.md` tal como estaban al compilar. Durante la corrida el árbol se
  confirmó en `9223c8a`, `b98efcb` y `ebaba4d` (HEAD al terminar); no se repitió la captura sobre esos commits, así que
  el último ajuste (`fix(web): load the page-header band image eagerly`) puede no estar reflejado en estas imágenes.
- Aquí van **9 PNG**: 7 del juego de 34 capturas (login + 16 vistas a 1440 y 390) y las 2 de la galería. El juego
  completo se generó y verificó (34 PNG no vacíos, 2,1 MB) pero se dejó fuera del repo, en el directorio temporal.

| Archivo | Viewport | Qué muestra |
|---|---|---|
| `login-1440.png` | 1440x900 | Login sin sesión |
| `overview-1440.png`, `overview-390.png` | 1440x900, 390x844 | Inicio |
| `devices-1440.png`, `devices-390.png` | 1440x900, 390x844 | Dispositivos |
| `policy-1440.png` | 1440x900 | Política y horario escolar |
| `alerts-1440.png` | 1440x900 | Alertas |
| `gallery-1440.png`, `gallery-390.png` | 1440x900, 390x844 | Galería `/design-system` (página completa) |

Las vistas con sesión usan los mismos datos sembrados que la línea base (un tutor, un dispositivo "After S53 …" en
línea, una regla `com.instagram.android` Bloquear); el nombre del dispositivo, los ids y las horas cambian en cada
corrida y hay que ignorarlos al comparar. Todas son `fullPage` con animaciones desactivadas, Chromium de Playwright.

## Comandos ejecutados

Docker desde Git Bash con `MSYS_NO_PATHCONV=1`.

```bash
docker compose -f compose.yaml -f compose.web.yaml stop web backend      # libera 3000 y 8000 (sin down)
docker compose -f compose.test.yaml build backend migrate api_server
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up -d db redis api_server
docker compose -f compose.test.yaml run --rm api_server python scripts/seed_test_session.py   # JSON fuera del repo
cd frontend
npm run build                      # exit 0; luego git checkout -- next-env.d.ts
node -e "...cpSync('public',...); cpSync('.next/static',...)"           # como la línea base
PORT=3000 HOSTNAME=0.0.0.0 node .next/standalone/server.js               # en segundo plano
node <capture-after.cjs>           # script de la línea base, solo cambia la carpeta de salida y el nombre del dispositivo
npm run dev -- -p 3100             # solo para la galería (en producción la ruta da 404)
node <capture-gallery.cjs>         # galería a 1440 y 390, y medición de la banda
```

Al terminar: se detuvieron los procesos `node`, `docker compose -f compose.test.yaml down` (solo el stack de prueba) y
`docker compose -f compose.yaml -f compose.web.yaml up -d web backend`.

## Qué se comparó

Con la herramienta de lectura de imágenes, vista por vista contra la línea base: login, inicio, dispositivos,
vinculación, reglas, política, categorías, alertas, vista remota, auditoría y perfil a 1440; inicio, dispositivos,
política y auditoría a 390; además estadísticas y geocercas a 1440 (usan los archivos `.tsx` modificados) y la galería
a ambos tamaños. Se comparó también el alto de documento de las 32 vistas (baja entre 0 y 164 px, esperado por
las tarjetas de métrica sin caja y los paneles más compactos; solo `audit-390` sube 42 px porque dos UUID distintos
parten en dos líneas).

La corrida no tuvo errores de consola, errores de página ni respuestas HTTP >= 400, y no quedó ningún texto "Cargando…"
ni "…" en pantalla. La galería no desborda en horizontal (ancho de documento = ancho de ventana a 1440 y a 390).

## Regresiones de estructura o legibilidad

Ninguna de estructura (sin desbordes, recortes nuevos, estados seleccionado/hover invisibles, iconos perdidos ni
colapso de layout). Tres puntos de legibilidad, todos menores:

1. **Cifras en serif.** Playfair Display usa por defecto cifras de estilo antiguo; en `Dispositivos vinculados (1)`
   (`devices-1440.png`, `devices-390.png`) el `1` parece una `ı` minúscula. Afecta a cualquier título h1-h4 con números.
2. **Barra de la `e` en Playfair.** En el título grande de muestra de la galería (`Título en serif, el tono editorial`)
   la barra fina de la `e` desaparece en esta captura (Chromium headless, 1x) y se lee "scrif" / "cditorial". En los h1
   de las vistas la barra se ve. No se pudo comprobar en una pantalla real o a 2x; conviene mirarlo ahí.
3. **Contraste de la descripción en la banda** (ver abajo): cae a 4,14:1 en el final de la primera línea.

Diferencia de diseño a revisar (no es regresión): a 390 px la banda queda casi sin foto (velo crema al 92 %) y el
`aside` se estira a todo el ancho, así que la insignia "En línea" aparece como una franja verde.

Ya existían en la línea base y no cuentan: el icono de verificación gris sobre azul en la tarjeta de política seleccionada, el
sidebar recortado en las vistas más altas, la etiqueta "8:00" del horario encima del tramo azul a 390 y la columna IP
pegada al borde derecho en auditoría.

## Contraste de la banda (`PageHeader band`, galería a 1440)

Se ocultó el texto (`color: transparent`) manteniendo el layout, se capturó la banda (1232x211 px) y se leyeron los
píxeles reales de fondo bajo el rectángulo de cada línea de texto. El velo es crema sólido hasta el 45 % del ancho
(x = 554 px) y se desvanece hacia la foto.

| Texto | Color | Fondo más claro | Fondo más oscuro | Fondo medio | Contraste peor caso | Umbral |
|---|---|---|---|---|---|---|
| Título (44 px, 600) | `#11243d` | `#f5f3ee` | `#f5f3ee` | `#f5f3ee` | **14,09:1** | 3:1 (texto grande): cumple |
| Descripción, línea 1 (x 25-660) | `#5b6779` | `#f6f3ee` (5,18:1) | `rgb(218,219,220)` en x=659 (4,14:1) | `rgb(244,242,237)` (5,13:1) | **4,14:1** | 4,5:1: **no cumple** en el tramo x ≈ 626-660 |
| Descripción, línea 2 | `#5b6779` | `#f5f3ee` | `#f5f3ee` | `#f5f3ee` | 5,17:1 | 4,5:1: cumple |
| Frase en cursiva | `#5b6779` | `#f5f3ee` | `#f5f3ee` | `#f5f3ee` | 5,17:1 | 4,5:1: cumple |

Causa: la columna de texto llega a 640 px y el velo sólido solo cubre 554 px; lo que pase de ahí se apoya en el
degradado sobre la foto (a x=630 el peor caso es 4,39:1, a x=640 4,34:1, a x=650 4,24:1). Aplica a descripciones largas
con la banda ancha; el título y la frase quedan bien. A 390 px el velo es un 92 % crema; ahí no se midió.

## Límites

- No hay captura de línea base de la galería (no existía), solo "después".
- El login se ve sin botón de Google por la misma razón que en la línea base (el build no define el ID de cliente).
- La vista remota solo en estado inactivo; ocho vistas siguen en estado vacío porque el seed no crea sus datos.
- La galería se sirvió con `next dev` (la ruta es dev-only). El indicador de desarrollo de Next ("1 Issue") se ocultó
  con CSS en las capturas; viene de que la CSP de `next.config.ts` (Sprint 21) no permite `eval`, que React usa en
  modo desarrollo; no ocurre en el build de producción.
- Las capturas se hicieron a escala 1x en Chromium headless; el render de fuentes en una pantalla real puede diferir.
- Node v24.14.1, Next 16.3.8.

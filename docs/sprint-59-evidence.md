# Sprint 59 — Evidencia

Fecha: 04/10/2026. Código en `7ec38c7` (10:00) y `ccde5cf` (10:11); verificación con datos reales y correcciones en
`eb2db05` (10:43), todos de Cristian. Escrito en el S60 (T1) a partir de lo que dejaron las fuentes. Este sprint es el que
**contiene** la evidencia de la tanda: `docs/redesign/s59/LEEME.md` cubre S54–S59 a la vez, y los `-evidence.md` de
S54–S58 remiten a ese archivo en vez de copiarlo.

## 1. Lo que registra `docs/redesign/s59/LEEME.md` (resumen fiel)

- **Entorno**: rama `sprint-54-rediseno-web-marco`, HEAD `8be5abd`, árbol limpio salvo `compose.web.yaml` sin versionar
  (versionado después en `857f60f`). Fecha 04/10/2026.
- **Comandos** (Docker desde Git Bash con `MSYS_NO_PATHCONV=1`): parar `web` y `backend`; construir `backend`, `migrate` y
  `api_server` con `compose.test.yaml`; migrar; levantar `db redis api_server`; sembrar dos sesiones con
  `scripts/seed_test_session.py` (una para e2e, otra para capturas); `cd frontend && npm run build && git checkout --
  next-env.d.ts`; servir el `standalone` en el puerto 3000; `npx playwright test` con `E2E_BASE_URL` y `E2E_SEED_JSON`;
  sembrado de datos y capturas con dos scripts de nodo.
- **E2E** (`frontend/e2e/dashboard.spec.ts`): **1 passed, 15,7 s**, contra el build de producción standalone
  recompuesto. Salvedad: se sirvió a mano y se pasó `E2E_BASE_URL` (el `webServer` de la config hace el mismo
  build/copia) en vez de dejar que Playwright lo arranque.
- **Datos sembrados por la API real, sin escribir en la base**: un tutor y el dispositivo `S59 Pixel de Sofía`; latido
  con permisos OK; inventario de 11 apps y uso de hoy más 5 días previos; reglas por app de los 5 tipos; 9 apps en
  categorías y 3 reglas de categoría; modo escolar 07:00-14:00 L-V activado; política ALLOW; 2 geocercas; 4 ubicaciones;
  5 bloqueos; 6 alertas (una silenciada 7 días, una leída).
- **Qué se comprobó**: 34 PNG no vacíos (login + 16 vistas, 1440 y 390) más `overview-1000` y `overview-390-drawer`;
  sesión continua con cambio de hash sin recargas; sin errores de consola, de página ni HTTP >= 400; ningún «Cargando» ni
  «…» en pantalla; sin desbordamiento horizontal del documento (scrollWidth = ancho de ventana en las 32). Las imágenes
  enumeradas se abrieron y se miraron una por una.
- **Defectos y salvedades**: los del LEEME, repartidos por vista en `docs/sprint-57-evidence.md`,
  `docs/sprint-58-evidence.md` y abajo.

## 2. Capturas que sí existen en el repo (18 PNG)

En `docs/redesign/s59/` (verificado con `ls`). El LEEME dice «los 15 de 1440x900 pedidos» (login + 14 vistas) «y 3 de
390x844» (`overview`, `policy`, `geofences`). Todas anteriores a `eb2db05`.

| Sprint | Vista | 1440 | 390 |
|---|---|---|---|
| S54 | Login | `login-1440.png` | — |
| S55 | Inicio | `overview-1440.png` | `overview-390.png` |
| S55 | Dispositivos | `devices-1440.png` | — |
| S55 | Vinculación | `pairing-1440.png` | — |
| S56 | Apps | — | — |
| S56 | Reglas | `rules-1440.png` | — |
| S56 | Política y horario | `policy-1440.png` | `policy-390.png` |
| S56 | Categorías | `categories-1440.png` | — |
| S57 | Geocercas | `geofences-1440.png` | `geofences-390.png` |
| S57 | Ubicación | `location-1440.png` | — |
| S57 | Historial | `history-1440.png` | — |
| S58 | Estadísticas | `statistics-1440.png` | — |
| S58 | Alertas | `alerts-1440.png` | — |
| S58 | Silenciadas | — | — |
| S59 | Vista remota | `remote-1440.png` (solo estado inactivo) | — |
| S59 | Auditoría | `audit-1440.png` | — |
| S59 | Perfil | `account-1440.png` | — |

## 3. Defectos del LEEME de las vistas de S59 y qué dice `eb2db05`

| Defecto observado (LEEME) | Corrección declarada en `eb2db05` | Verificada |
|---|---|---|
| `account-390`: el correo parte a mitad de palabra, el nombre ocupa 3 líneas, «Cuenta de Google» apretada; a 1440 el contenido termina en x=1342 frente a 1408 de las demás | «profile column and email wrap properly» | No |
| `audit` (1440 y 390): identificadores crudos (`alert · c91ef6dc-…`, `app_category_assignment · uuid`, `alert_silence · APP_BLOCKED:com.tiktok.android`) | Etiquetas de recurso en español (`lib/auditFormatting.ts`) | No |
| Títulos `h1` en Playfair en todas las vistas (barra de la «e» casi invisible a 1×: «Gcoccrcas», «Alcrtas», «Vista rcmota»); en recorte 1:1 la barra existe pero es muy fina; no se pudo confirmar en pantalla real ni a 2× | Serif a Newsreader (`layout.tsx`, `globals.css`, `DESIGN.md`) | No (sin recaptura ni monitor real) |
| Menor: el sidebar mide 100vh en capturas `fullPage` | Comportamiento esperado de la captura | — |

Sin hallazgos de: sidebar, encabezado o banda rotos; estados seleccionado o hover invisibles; dos azules compitiendo;
tarjetas en cuadrícula residuales; vistas vacías con aspecto roto.

## 4. Evidencia que no existe

- **Vista remota real**: el LEEME declara «solo en estado inactivo (la sesión real exige a una persona en un teléfono)».
  El plan (§4, S59) exige verificar el `<video>` siempre montado con una sesión real, no por lectura del código.
- Login real con Google: el login sale con «Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.»; no se capturó el botón.
- Informe del `security-reviewer` (S57 y S59, exigido por el plan): sin registro.
- Salida guardada de `eslint`, `tsc` o `npm run build` de S56–S59 y `eb2db05`.
- Recaptura posterior a `eb2db05`.
- Cajón de 390 px desplazando su lista hasta «Auditoría» y «Cerrar sesión»: el LEEME declara que no se comprobó.
- Medición de contraste con herramienta: el LEEME dice «solo a ojo».
- En el repo: capturas de 390 px de 13 de las 16 vistas y del login, y ninguna de Apps ni de Silenciadas (el juego
  completo de 34 PNG, `overview-1000` y `overview-390-drawer` están **fuera del repo**).

## 5. Pendiente

- Prueba de Vista remota con una persona y un teléfono (dueño).
- Revisión con sesión real de Google (T5 de `docs/sprint-54.md`).
- Revisión de seguridad de S57 y S59.
- Recapturar tras `eb2db05` y subir las capturas que faltan al repo.
- Ver `docs/sprint-59.md`, «Pendiente conocido».

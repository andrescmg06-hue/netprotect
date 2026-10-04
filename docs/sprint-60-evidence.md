# Sprint 60 — Evidencia

Fecha: 04/10/2026. Rama `sprint-60-rediseno-web-cierre`, creada desde `fix/s54-auth-log-sin-pii` (`e9e4f69`), que parte de la
punta de `sprint-54-rediseno-web-marco` (`857f60f`, Cristian). Worktree `C:/Users/andre/np-s54`. Comandos ejecutados desde
Git Bash en Windows; las comprobaciones de contenedor y el e2e las hizo el agente `verifier` con el stack de
`compose.test.yaml` (proyecto `netprotect-test`). Lo que no consta aquí no se ejecutó.

Commits de la rama sobre `857f60f`: `e9e4f69` (fix de auth), `6f4db76` (apertura del sprint), `c307652` (T1), `d42997c`
(T2), `0d28665` (T3), `0630521` (T4), `af32303` (T5), `5779f50` (T6), `2f8c9b7`, `5bf8144`, `1b0dfec`, `11179d5`, `138377e`
(T9), `8caf627` y `6323737` (T7), `2b111fb`.

## 1. Verificación de cierre (agente `verifier`, modo completo)

```
BACKEND
$ docker compose -f compose.test.yaml build backend migrate api_server
PASS, exit 0 (3 s, todo en caché)
$ docker run --rm --entrypoint ruff netprotect-test-backend:latest check --no-cache app tests alembic
All checks passed!
$ make test          (make no existe en Git Bash: se ejecutaron a mano los pasos del target)
  docker compose -f compose.test.yaml build ; run --rm migrate ; up --abort-on-container-exit --exit-code-from backend db redis backend
backend-1  | 295 passed, 3 warnings in 34.10s          (los 3 pasos con exit 0, 49 s en total)

FRONTEND
$ cd frontend && npm run lint && npx tsc --noEmit        # eslint . --max-warnings=0
PASS, exit 0 los dos (10 s)
$ npx playwright test      # su webServer hace `npm run build` y sirve el standalone en el 3000
  ok 1 e2e\login.spec.ts:13:5 › a visitor without a session sees the login screen, not the dashboard (935ms)
  ok 2 e2e\dashboard.spec.ts:51:5 › a returning tutor can navigate the whole dashboard and see real backend data (15.0s)
  2 passed (26.6s)
```

Las cifras de `make test`, Playwright y del control negativo se contrastaron después contra los logs del `verifier`
(`mt.log`, `e2e.log`, `neg.log`). Los 3 avisos de pytest no son fallos y son preexistentes y de entorno: dos
`StarletteDeprecationWarning` (`httpx` con `starlette.testclient`, `HTTP_422_UNPROCESSABLE_ENTITY`) y un
`PytestCacheWarning` por permisos en `/app/.pytest_cache`.

### Control negativo del e2e del anillo de foco

El step «a primary button shows the keyboard focus ring…» debe fallar con el CSS anterior a la corrección del foco. Se
ejecutó el `dashboard.spec.ts` actual en un worktree desvinculado en `d42997c` (parent de `0d28665`), con sesión nueva:

```
1 failed (39 s)   e2e/dashboard.spec.ts:117
Error: the focus ring never replaced the resting shadow
Expected substring: "rgb(23, 105, 255)"
Received string:    "color(srgb 0.0901961 0.411765 1 / 0.25) 0px 1px 2px 0px"
Timeout 5000ms exceeded while waiting on the predicate
```

Sobre `np-s54` el mismo step pasa. Es decir, el test detecta la regresión de T3 y no solo pasa con el CSS actual.

## 2. Fallos del camino y cómo se corrigieron

1. **Primera ejecución del e2e: falló el step del anillo de foco** (`box-shadow` leído a mitad de transición).
   `Button.module.css` anima `box-shadow` 0,15 s y el test leía el valor justo tras el Tab:
   `oklab(0.571457 -0.0345269 -0.232818 / 0.25) 0px 1px 2px 0px, rgba(0, 0, 0, 0) 0px 0px 0px 0px`. Era un fallo del test, no
   del CSS: medido después, el valor estable es `rgb(245, 243, 238) 0px 0px 0px 2px, rgb(23, 105, 255) 0px 0px 0px 4px`.
   Corregido con `expect.poll` (`6323737`). Resultado: 2 ejecuciones seguidas en verde antes del cierre y la del cierre.
2. **`waitForLoadState("networkidle")` no servía.** Medido en las 12 secciones: se resuelve en 2–8 ms, porque en la SPA
   el estado ya está «idle» al cambiar de sección. Se sustituyó por una espera de 1 s antes de comprobar que no hay `alert`
   (medido: ninguno aparece ni tras 1,5 s) y se subió el timeout del test a 90 s (`6323737`).
3. **Lint del fix de auth (previo al cierre, PR #15).** Un primer `ruff check` falló con S105 en el test nuevo
   (`SECRET = "SECRET"`, centinela) y E501 en un comentario preexistente de `auth.py` (101 > 100). Corregidos con
   `# noqa: S105 -- <motivo>` y reajustando el comentario; después `ruff check` verde y `make test` con 295 passed.
4. **Contraste del login medido, no estimado.** Con el cristal al 60 % los textos pequeños salían a 4,35:1 en móvil y los
   grises atenuados a 3,35:1: no cumplían. Se ajustó hasta ≥ 4,6:1 (§4).
5. **Entorno.** La extensión de Chrome no conectó y `/design-system` da 404 en el build de producción, así que el
   anillo de foco no se pudo probar a mano con Tab: lo cubre el e2e. Chrome sin interfaz no admite ventanas de menos de
   ~500 px: no hay captura a 390 px.
6. **Desviaciones del `verifier` en el control negativo:** `git worktree add` en el scratchpad falló con
   `Filename too long` (`ScreenShareConsentScreen.kt`), se creó en `C:/Users/andre/AppData/Local/Temp/np-neg` y se borró;
   la junction de `node_modules` no funciona con Turbopack (`Symlink … points out of the filesystem root`), se retiró con
   `cmd /c rmdir` (el `node_modules` de `np-s54` quedó intacto, 281 entradas) y se usó `npm ci` en el temporal.
7. **Malentendido con el login.** Se aclaró la franja de foto a la izquierda del panel cuando se pedía hacer más
   transparente el panel. Se retiró el velo (`138377e`): la franja vuelve a ser la foto (diferencia media 1,9/255 frente
   a la foto sin tocar, ruido JPEG).

## 3. Cambios de S60 y su medición

**T2, contraste** (razones por luminancia WCAG): contador del sidebar blanco/`--color-danger` 3,76:1 → blanco/`--color-danger-text`
5,75:1; pista de `ScheduleBar` con `--color-border-strong` 3,64:1 sobre blanco. Se revisaron y dejaron sin cambio `.signOut`,
`DayPicker`, `DeviceSelector` y `DevicePolicyPanel` (los identifica el texto, el relleno o un check, no solo el borde).

**T3, foco:** el CSS servido por el contenedor reconstruido contiene `.primary:focus-visible{box-shadow:var(--focus-ring)}`,
`.rowSelected:focus-visible` y el bloque `@media (forced-colors: active)`. Confirmado en un navegador real por el e2e
(§1 y §2.1). El modo de alto contraste de Windows no se probó.

**T4, favicon** (generado con Pillow desde `logo-shield.png`): `/icon.png` 5 339 B (antes 53 954 B), `/favicon.ico` 7 134 B
(16, 32 y 48 px), `/apple-icon.png` 15 945 B; los tres devuelven `200` y el HTML trae los tres `<link rel>` con sus `sizes`.

**T5, imágenes** (medido con `curl` a `/_next/image` en el contenedor `web`): `study.jpg` 186 405 B → 36 306 B a `w=640`;
`band-alpine.jpg` 224 570 B → 51 949 B a `w=1080`; `logo-full.png` 103 981 B → 8 263 B (`w=256`) y 29 480 B (`w=640`),
`200 image/png` con `q=90` aceptado tras declarar `images.qualities: [75, 90]` (Next 16 solo admite 75 por defecto).

**T9, login:** capturas de Chrome sin interfaz sobre el `web` reconstruido, en `docs/redesign/s60/`: `login-1500.jpg`,
`login-520.jpg` y `login-vs-mockup.jpg` (referencia a la izquierda, resultado a la derecha). Luminosidad media del panel
renderizado 0,758 frente a 0,750 de la referencia. Contraste medido sobre la foto real desenfocada y aclarada (peor píxel del
bloque de contenido, texto navy pleno): escritorio ≥ 4,6:1 en 1280×720, 1366×768, 1500×844 y 1920×1080; tarjeta móvil
4,8–4,9:1 en 390×844, 414×896 y 520×900.

## 4. Seguridad (agente `security-reviewer`, `857f60f..HEAD`, 16 commits, 38 ficheros)

**0 ALTA, 0 MEDIA, 1 BAJA.** Sin bloqueo del cierre.
- Confirmado sin hallazgos: `InvalidGoogleTokenError` solo sale con cadenas literales y `from None` corta la cadena (el test
  comprueba `__cause__ is None`); la validación del token y el 401 uniforme no cambiaron; `next.config.ts` solo añade
  `images.qualities` (CSP y cabeceras intactas, sin `remotePatterns` ni `domains`, así que el optimizador no es proxy de URLs
  externas); el login no cambió de flujo ni añade llamadas o almacenamiento; el e2e no contiene tokens y
  `frontend/e2e/.session.json` sigue ignorado (`.gitignore:58`); búsqueda de secretos en `docs/`, `README.md` y `e2e` sin
  tokens ni claves reales (`docs/sprint-26-evidence.md:50` tiene un `JWT_SECRET` de prueba preexistente, fuera del rango);
  los 6 assets nuevos sin metadatos (`tEXt`, `Exif`, XMP, GPS).
- **BAJA (documental, corregida):** el README (Sprint 57) y `docs/sprint-57.md` afirmaban que Ubicación no enviaba
  coordenadas a terceros, pero `DeviceLocationPanel.tsx:252-258` incrusta Google Maps Embed con
  `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` y el enlace «Abrir en Google Maps» está siempre visible. Verificado en el código y
  corregido el texto en ambos ficheros.
- Revisiones previas de este sprint (mismo agente): S59 (Vista remota, Auditoría, Cuenta): sin hallazgos; S57 (Geocercas,
  Ubicación, Historial): 1 BAJA, el enlace a Google Maps siempre visible, **aceptado por el dueño como mejora**.

## 5. No verificado

- Revisión visual del panel con sesión real de Google (T5 de S54) y el login real de Google (H-01): los hace una persona.
- El login a 390 px reales o en un teléfono; las capturas llegan a 520 px.
- El modo de alto contraste de Windows y otros zooms o tamaños de fuente del sistema para el contraste del login.
- Los favicons en la pestaña del navegador y los logos a 1× y 2× a la vista.
- Vista remota con un teléfono real (H-02).
- Los 4,5:1 del login están medidos sobre la foto, no sobre píxeles de pantalla; la estabilidad del e2e tiene una muestra de
  3 ejecuciones completas en verde tras el arreglo (más la primera, que falló y se corrigió, §2.1).
- `ruff` y `pytest -m "not integration"` en el host (se usó la imagen y la suite completa en contenedor); Android,
  `scripts/verify_turn.sh` e infra, fuera del alcance de S60.
- CI en GitHub Actions: el sprint no se da por cerrado hasta que pasen los 8 jobs.

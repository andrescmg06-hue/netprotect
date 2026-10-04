# Sprint 52 — Evidencia

Sprint sin código del producto: la evidencia es la medición que respalda `docs/redesign/fase-0-informe.md`, no pruebas
de software. Fecha: 04/10/2026. Rama `sprint-52-rediseno-web-fase-0` (desde `web-redesign`, `74a28a9`).

## 1. Ninguna línea del producto cambió (criterio de aceptación)

```
$ git diff --name-only origin/web-redesign -- frontend backend mobile | wc -l
0
```

Los únicos cambios de la rama son `docs/redesign/fase-0-informe.md` y `docs/sprint-52.md` (commit `634dc48`), más este
archivo, el README y `docs/progress.md` del cierre.

## 2. Estado del repositorio al empezar

```
$ git fetch origin --prune
 * [new branch]      web-redesign -> origin/web-redesign
local main: 38b77b8   origin/main: 38b77b8   (0 adelante / 0 atrás)
$ git show --stat 7a05989 74a28a9
7a05989 docs(redesign): incorporar al repo el paquete de diseño del panel web      (27 archivos)
74a28a9 docs(redesign): plan por sprints S52–S60 y guía de arranque para el equipo (4 archivos)
```

`web-redesign` trae además 96 commits del rediseño Android (`sprint-41` a `sprint-51`) que aún no están en `main`.

## 3. Assets (T4)

Tamaños y dimensiones leídos del disco; comparación por hash:

```
logo-full:   False   (frontend/public/brand vs docs/redesign/assets/logos)
logo-shield: False
public/login/background-desk-clean.png 617 KB   public/login/devices-showcase.png 1467 KB
illus-apps-grid.png 724 KB   illus-rules-lock.png 805 KB   illus-shield-phone.png 739 KB
photo-alpine-banner.jpg 2048x768 219 KB   photo-login-study.jpg 1536x1024 182 KB
```

## 4. Contraste (T7), calculado con la fórmula WCAG 2.x

```
OK    4.67:1  blanco sobre azul marca #1769ff
OK   14.09:1  #11243d sobre crema #f5f3ee
OK    5.17:1  #5b6779 sobre crema #f5f3ee
FALLA  4.22:1  gris del brief #697586 sobre crema #f5f3ee
OK    4.68:1  gris del brief #697586 sobre blanco
FALLA  3.64:1  placeholder #8a8678 sobre blanco (texto)
FALLA  3.29:1  placeholder #8a8678 sobre crema (texto)
OK    3.29:1  borde de control #8a8678 sobre crema (no texto, mínimo 3)
OK    4.21:1  foco #1769ff sobre crema (no texto)
OK    7.87:1  sidebar #9fb0c6 sobre #0d1b2a
OK    4.67:1  activo del sidebar, blanco sobre #1769ff
```

Corrección propuesta y comprobada: `#6f6b60` da 4.80 (crema), 5.32 (blanco) y 4.71 (`#f3f1ec`).

## 5. Mapeo de código (T1–T3, T5)

Tres exploradores de solo lectura, con `ruta:línea` en el informe. Hallazgos que cambian el plan:

- `DESIGN.md` está en la **raíz** y sus «Don't» prohíben lo que el rediseño exige.
- 12 de 49 módulos CSS tienen valores fijos: cambiar tokens es viable sin tocar el JSX de los paneles.
- La API **no** tiene batería, red, fecha de creación de silencios, sesiones, edición de perfil ni horarios por día;
  el código de vinculación dura 180 s, no 10 minutos.
- `GET /devices/{id}/location/history` existe en el backend y no en `apiClient.ts`.

## 6. Errores y tropiezos del camino

- `npm run build` **falla en esta máquina** (28 errores `Can't resolve 'lucide-react'`): `frontend/node_modules` está
  desactualizado (sin `lucide-react`, `next 16.3.3` en vez de `16.3.8`). No es un fallo del código; se resuelve con
  `npm ci`. Se verificó que el build de la imagen Docker `web` (que instala desde cero) sí pasa. Node local 24.14.1; CI
  usa 22.
- Un comando de PowerShell con `-replace` y corchetes fue bloqueado por el entorno antes de ejecutarse; se comprobó que
  no dejó cambios y se repitió con `String.Replace`.

## 7. Pendiente

- **T8 (capturas base)**: no tomadas. Necesitan backend y una sesión de tutor; el S53 las toma antes de cambiar tokens.
- **Tipografía serif definitiva (D1)** y **logo oficial (D5)**: dependen del diseñador del paquete.
- CI en GitHub Actions: no aplica a este sprint (solo documentación); el PR #14 de seguimiento está en borrador.

## 8. Criterios de aceptación

- [x] El informe cubre los 7 puntos con evidencia (`ruta:línea`).
- [x] La matriz de datos reales por vista está completa (informe §5).
- [x] El dueño aprueba el informe: aprobado por el dueño el 04/10/2026 (confirmación en la conversación; no queda
  por escrito en el repositorio fuera de este registro).
- [x] Ninguna línea de `frontend/src/` cambió (§1).

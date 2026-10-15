# Diseño vigente

**El diseño que cuenta es el código de `main`.** Los documentos de diseño explican por qué es así; si
contradicen al código de `main`, manda el código.

## Qué mirar para implementar

| Superficie | Fuente vigente |
|---|---|
| Web (panel del tutor) | `DESIGN.md` + `frontend/src` (rediseño editorial S52–S60, fusionado en `main` con el PR #32) |
| App Android (solo supervisado) | `docs/android-redesign/supervisado-v2/` (rediseño v2, S61–S67) + el código en `mobile/` |

## Histórico: no usar como referencia para implementar

- `docs/android-redesign/` **fuera de `supervisado-v2/`**: kit v1 (S40–S51), incluidos los mockups del modo tutor
  de Android, que ya no existe (el tutor usa solo el panel web).
- `docs/redesign/`: paquete del rediseño web ya ejecutado (S52–S60). Sirve como registro de decisiones.

Los kits no se borran porque los `docs/sprint-NN.md` los citan.

## Regla

Antes de implementar UI, partir de `main` actualizado. Nunca de una rama vieja, un zip o una carpeta fuera del
repositorio.

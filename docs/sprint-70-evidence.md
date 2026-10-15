# Sprint 70 — evidencia (B-05)

Árbol `C:/Users/andre/np-b05`, rama `sprint-70-b05-sondeo-alertas`. E2E en un proyecto Docker propio (`-p np-b05-e2e`), con el panel en build de producción y Playwright en `mcr.microsoft.com/playwright:v1.63.0-noble`; sin tocar el stack de desarrollo.

## El test falla sin el cambio

Antes de tocar `AuthContext`: `dashboard.spec.ts` falló en el paso nuevo con `cycle 1: POST /auth/refresh after the access token's lifetime — Expected: 2, Received: 1` (pasados los 15 minutos, adelantando el reloj del navegador, no hubo renovación). Los tres tests de `session-renewal.spec.ts` fallaron por lo mismo.

## Después

- `npm run lint` y `npm run build` limpios.
- E2E completo dos veces desde cero, tras los arreglos de la revisión de seguridad: **4 passed, 0 failed** en ambas.
  - `dashboard.spec.ts`: contra el backend real, dos ciclos de renovación sin recargar, un solo `/auth/refresh` por ciclo, alertas con un `Bearer` nuevo que responden 200 y el refresh token nuevo en `sessionStorage`.
  - `session-renewal.spec.ts` (backend simulado con `page.route`, solo lógica del cliente): fallo de red → sigue la sesión y reintenta a los 30 s; refresh revocado → pantalla de acceso sin bucle; pestaña dormida con el token caducado → renueva al volver.

## Revisión de seguridad (`security-reviewer`, Opus)

Sin ALTA. Cuatro BAJA, corregidas: el doble canje al montar en desarrollo (StrictMode), revocar el token recién rotado si `/auth/me` falla al cargar, respetar la espera de 30 s al volver a la pestaña durante una caída, y una afirmación falsa mía en el comentario y en `docs/sprint-70.md` («revocar la familia»: el backend no tiene familias de tokens).
Fuera del cambio, abierto como **B-13**: `/auth/refresh` no bloquea la fila, así que el «un solo uso» no es atómico en el servidor.

## Pendiente / límites declarados

- El CI de los 8 jobs corre con el PR.
- El servidor nunca ve un token caducado en el E2E (solo se adelanta el reloj del navegador); no probado con una sesión real de más de 15 minutos con una persona.
- Sin cobertura E2E: revocar el token huérfano al cerrar sesión durante una renovación, respuestas 429/5xx (solo red caída) y el caso StrictMode.

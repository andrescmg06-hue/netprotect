# Sprint 70 — B-05: el panel web no renueva el token de acceso

Rama `sprint-70-b05-sondeo-alertas` (desde `main` tras B-09). Estado: en curso.

## Qué pasa hoy (investigado en el código)

- `frontend/src/contexts/AuthContext.tsx` canjea el refresh token **una sola vez**, al montar (`useEffect` de la línea 59), y guarda el access token en estado. Nada lo renueva después. El access token vive `access_token_ttl_minutes` (15 min; `auth.py:73` devuelve `expires_in`).
- A los 15 minutos **todas** las llamadas del panel dan 401, no solo las alertas: dispositivos, reglas, ubicación, etc. B-05 lo veía en el sondeo porque es lo único que corre solo cada minuto (`DashboardShell.tsx:154`).
- Ese sondeo traga el error (`.catch(() => [device.id, 0])`, línea 144): con el token caducado la campana y la insignia dicen «ninguna sin leer» aunque haya alertas. Es un dato de seguridad de un menor que se muestra mal sin avisar.

## Decisión de diseño

Arreglar la causa, no el síntoma: `AuthContext` renueva el access token **antes** de que caduque, con un único temporizador y una única renovación en vuelo (el refresh token rota; dos canjes simultáneos con el mismo token podrían revocar la familia, ver G-02).

- Programar la renovación a `expires_in - 60 s` (mínimo 10 s) tras obtener cualquier par de tokens (carga, login o renovación).
- Si el navegador durmió la pestaña y el token ya caducó (los temporizadores se retrasan en segundo plano), renovar también al volver a la pestaña (`visibilitychange`).
- Renovación fallida con 401 (refresh token inválido o revocado) → cerrar la sesión local como hoy hace la carga inicial. Fallo de red u otro estado → reintentar a los 30 s sin cerrar sesión.
- Sin cambios de backend, de rutas ni de la forma de las respuestas. No se toca el sondeo ni los paneles: reciben el `accessToken` nuevo por el contexto, como ahora.

## Fuera de alcance (anotado)

- Que el sondeo distinga «0 alertas» de «no pude leer». Con el token renovado deja de ser un caso habitual; si se quiere, es un cambio de interfaz aparte.
- D-03 (sesión en `sessionStorage` en vez de cookie HttpOnly).

## Tareas

- [x] T1 — Investigar y abrir el sprint. Inline. (este commit)
- [ ] T2 — Renovación programada en `AuthContext.tsx` y prueba E2E con Playwright (test primero: avanzar el reloj del navegador, comprobar que se llama a `/auth/refresh` y que las siguientes peticiones llevan el token nuevo). Delegado (**Opus**; sesión y tokens).
- [ ] T3 — `npm run lint && npm run build`, E2E completo en contenedor, `security-reviewer` (sesión/tokens) y PR. Inline.

## Criterios de aceptación

1. Con el panel abierto más de 15 minutos las peticiones siguen autenticadas (E2E con el reloj adelantado).
2. Una sola renovación a la vez; nunca dos canjes del mismo refresh token.
3. Refresh token revocado → vuelve a la pantalla de acceso, sin bucles.
4. Un fallo de red al renovar no cierra la sesión y se reintenta.
5. El refresh token nuevo sustituye al anterior en `sessionStorage` en cada renovación.

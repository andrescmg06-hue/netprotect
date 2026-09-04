# Evidencia de verificación — Sprint 4

Fecha: 04/09/2026. Equipo: `andre`, Windows 11 Pro.

## Pruebas locales

```bash
cd backend
RUN_INTEGRATION_TESTS=1 pytest -q
→ 25 passed, 2 warnings in 61.85s

ruff check app tests alembic
→ All checks passed!
```

Las 11 pruebas nuevas contra PostgreSQL real:

`tests/test_roles_integration.py` (5): sin autenticación → 401; usuario nuevo sin roles;
seleccionar TUTOR concede el rol y es idempotente (segunda llamada devuelve el mismo `granted_at`);
un usuario puede sostener TUTOR y SUPERVISADO a la vez; un `role_code` desconocido → 400.

`tests/test_authorization_integration.py` (6): `require_role` deja pasar a quien sostiene el rol y
rechaza con 403 a quien no; un tutor carga su propio dispositivo vinculado;
**un tutor no puede cargar el dispositivo de otro tutor (404, el caso IDOR que el sprint existe
para cerrar)**; un `device_id` que no existe también da 404 (mismo código que el caso anterior,
a propósito); desvincular un dispositivo revoca el acceso del tutor de inmediato.

## Bug real encontrado y corregido: la prueba de "token alterado" era estadísticamente inestable

Al correr la suite completa (25 pruebas) por primera vez, `test_tampered_and_expired_access_tokens_are_rejected`
—que ya venía en verde desde el Sprint 3— falló una vez: el token "alterado" fue aceptado como
válido (200 en vez de 401).

Causa: la prueba alteraba el **último carácter** del JWT. Un HMAC-SHA256 son 32 bytes, que no se
dividen exactamente en grupos de 6 bits al codificarse en Base64url; el último carácter de la firma
codificada tiene 2 bits que no representan ningún byte real de la firma (son relleno). Cambiar ese
carácter por uno específico puede, según cuál sea el carácter original, no cambiar en absoluto los
bytes reales de la firma — es decir, el token "alterado" a veces no estaba alterado de verdad. Esto
depende del contenido aleatorio del token (`jti` es aleatorio en cada login), así que el fallo era
posible en cualquier corrida, no reproducible a voluntad, y había pasado inadvertido en todas las
corridas anteriores del Sprint 3 por pura casualidad.

Corregido alterando el carácter del **medio** del token en lugar del último, lejos del límite de
relleno. Verificado con 3 corridas consecutivas de esa prueba sola, limpias, más la suite completa.

Este hallazgo no señala ningún problema en la verificación real de JWT (`jwt.decode` con
`algorithms=["HS256"]` sí valida la firma correctamente); el problema estaba únicamente en cómo la
prueba fabricaba un token "alterado".

## Docker — integración completa

```bash
docker compose -f compose.test.yaml build
docker compose -f compose.test.yaml run --rm migrate
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from backend db redis backend
→ backend-1 | 25 passed, 3 warnings in 2.03s
→ backend-1 exited with code 0
```

## CI en GitHub Actions

Pendiente de push al cierre de este documento; se completa con el resultado del run correspondiente.

## No se marca como verificado

Todo lo anterior son comandos ejecutados realmente, con salida real, incluido el hallazgo de la
prueba estadísticamente inestable y su corrección. La sección de CI se completa sólo después de que
el pipeline corra de verdad.

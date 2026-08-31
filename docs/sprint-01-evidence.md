# Evidencia de verificación — Sprint 1

Fecha: 30/08/2026.

## Ejecutado durante la construcción del paquete

### Backend unitario

Comando:

```bash
cd backend
python -m pytest -q -m "not integration"
```

Resultado:

```text
4 passed, 3 deselected
```

Las tres pruebas deseleccionadas son de integración y requieren PostgreSQL/Redis reales.

### Compilación sintáctica Python

```bash
python -m compileall backend/app backend/tests
```

Resultado: correcto.

### Estructura YAML

Los tres archivos Compose fueron parseados correctamente durante la construcción del paquete.

## Verificación que debe ejecutarse en el equipo de desarrollo

Este entorno de construcción no dispone de Docker/Compose ni de una instalación Gradle/Android SDK utilizable para realizar el build Android completo. Tampoco dispone de acceso de red para instalar las dependencias npm desde el registro durante esta sesión.

Por tanto, antes de cerrar formalmente el Definition of Done se debe registrar evidencia de:

1. `docker compose up --build`.
2. `GET /api/v1/health/ready` con PostgreSQL y Redis activos.
3. carga de `http://localhost:3000` mostrando el estado conectado.
4. `npm install`, `npm run lint`, `npm run build`.
5. `gradle test assembleDebug` o build equivalente desde Android Studio.
6. ejecución de la app Android en emulador mostrando Backend/PostgreSQL/Redis conectados.
7. pipeline CI en GitHub.

No se marca como verificado ningún build que no haya sido ejecutado realmente.

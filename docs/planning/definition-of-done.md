# Definition of Done

Una funcionalidad sólo se considera terminada cuando:

1. Está implementada, no sólo diseñada.
2. Está integrada con los componentes que correspondan.
3. Valida entradas y estados relevantes.
4. Maneja errores sin exponer detalles sensibles.
5. Tiene pruebas apropiadas para su riesgo y nivel.
6. Está documentada.
7. Cumple requisitos de seguridad y privacidad del sprint.
8. No contiene secretos reales hardcodeados.
9. Está versionada en Git.
10. Funciona en el ambiente correspondiente.
11. Tiene criterios de aceptación demostrables.
12. Sus dependencias y comandos de ejecución están registrados.
13. Cuando depende de Android, se verificaron API, versión, permisos, restricciones, políticas y viabilidad.

## Regla de cierre del Sprint 1

El Sprint 1 se cierra cuando las rutas `Web → Backend → PostgreSQL` y `Android → Backend → PostgreSQL` son demostrables, Redis está levantado y alcanzable por el backend, CI valida builds/pruebas disponibles y la configuración no incluye secretos reales.

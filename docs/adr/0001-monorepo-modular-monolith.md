# ADR-0001: Monorepo y monolito modular para el MVP

- Estado: Aceptado
- Fecha: 2026-08-30

## Contexto

NetProtect requiere Android, web, API, base de datos e infraestructura, pero sigue siendo un proyecto académico que debe poder desplegarse y auditarse sin complejidad operacional innecesaria.

## Decisión

Usar un monorepo y un backend monolítico modular durante el MVP.

## Consecuencias

Positivas: menor coste de despliegue, contratos más visibles, CI centralizado y desarrollo más rápido.

Riesgo: acoplamiento interno. Mitigación: separar módulos por dominio y prohibir acceso directo entre capas cuando aparezca lógica de negocio.

# Diagrama de despliegue

## Desarrollo local

```mermaid
flowchart LR
    Browser[Navegador localhost:3000] --> Web[Contenedor Web]
    Emulator[Android Emulator] -->|10.0.2.2:8000| API[Contenedor Backend]
    Web --> API
    API --> PG[(PostgreSQL)]
    API --> R[(Redis)]
```

## Producción objetivo

```mermaid
flowchart TB
    Mobile[Android] --> TLS[HTTPS / Edge]
    Browser[Web] --> TLS
    TLS --> WEB[Web NetProtect]
    TLS --> API[Backend NetProtect]
    API --> PG[(PostgreSQL administrado o endurecido)]
    API --> R[(Redis administrado o endurecido)]
    API -.-> FCM[Firebase Cloud Messaging]
    API -.-> WS[Canal WebSocket]
    SEC[Gestor de secretos] -.-> API
    OBS[Logs / métricas / alertas] -.-> API
```

El proveedor cloud y los servicios concretos se definirán en Sprint 26. El diagrama fija responsabilidades, no un proveedor específico.

# Diagrama de componentes

```mermaid
flowchart TB
    A[App Android NetProtect\nTutor / Supervisado] --> API[API segura FastAPI]
    W[NetProtect Web\nNext.js] --> API
    GI[Google Identity] -. Sprint 3 .-> API
    API --> ID[Identidad y sesiones]
    API --> RBAC[Autorización / RBAC]
    API --> PAIR[Vinculación]
    API --> DEV[Dispositivos]
    API --> RULES[Motor de reglas]
    API --> LOC[Ubicación / geocercas]
    API --> ALERTS[Alertas]
    API --> AUDIT[Auditoría]
    API --> PG[(PostgreSQL)]
    API --> REDIS[(Redis)]
    API -. sprints posteriores .-> RT[WebSockets / FCM]
    RT -.-> A
    RT -.-> W
```

En Sprint 1 se implementan los componentes sólidos `Android`, `Web`, `API`, `PostgreSQL` y `Redis`; los demás representan límites previstos, no funcionalidad ya implementada.

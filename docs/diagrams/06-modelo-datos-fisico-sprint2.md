# Modelo de datos físico — Sprint 2

Esquema real, generado por Alembic y verificado contra PostgreSQL 18. Cubre únicamente las tablas
núcleo (identidad, roles, dispositivos, vinculación, sesiones, auditoría). El resto de las tablas del
modelo conceptual (`docs/diagrams/05-modelo-datos-conceptual.md`) se crean en el sprint que las usa.

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : grants
    USERS ||--o{ TUTOR_DEVICES : tutors
    DEVICES ||--o{ TUTOR_DEVICES : linked
    USERS ||--o{ PAIRING_CODES : generates
    DEVICES ||--o{ PAIRING_CODES : "created by"
    USERS ||--o{ DEVICES : "is supervised on"
    DEVICES ||--|| DEVICE_STATUS : has
    USERS ||--o{ SESSIONS : owns
    USERS ||--o{ AUDIT_LOGS : acts

    USERS {
        uuid id PK
        string email UK
        string google_sub UK
        string display_name
        string avatar_url
        bool is_active
        timestamptz created_at
        timestamptz updated_at
    }
    ROLES {
        string code PK "TUTOR | SUPERVISADO"
        string description
    }
    USER_ROLES {
        uuid id PK
        uuid user_id FK
        string role_code FK
        timestamptz granted_at
    }
    DEVICES {
        uuid id PK
        string name
        string platform
        string os_version
        string app_version
        uuid supervised_user_id FK
        timestamptz created_at
        timestamptz updated_at
    }
    DEVICE_STATUS {
        uuid device_id PK_FK
        string status "ONLINE|OFFLINE|SYNCING|ALERT|RESTRICTED|UNLINKED"
        timestamptz last_seen_at
        timestamptz last_sync_at
        timestamptz updated_at
    }
    TUTOR_DEVICES {
        uuid id PK
        uuid tutor_user_id FK
        uuid device_id FK
        timestamptz linked_at
        timestamptz unlinked_at
    }
    PAIRING_CODES {
        uuid id PK
        uuid tutor_user_id FK
        string code_hash
        timestamptz expires_at
        timestamptz used_at
        uuid device_id FK
        timestamptz created_at
    }
    SESSIONS {
        uuid id PK
        uuid user_id FK
        string refresh_token_hash UK
        timestamptz issued_at
        timestamptz expires_at
        timestamptz revoked_at
        string user_agent
        string ip_address
    }
    AUDIT_LOGS {
        uuid id PK
        uuid actor_user_id FK
        string action
        string resource_type
        string resource_id
        jsonb extra
        string ip_address
        timestamptz created_at
    }
```

## Decisiones de diseño

- **Claves primarias UUID generadas en Python** (`uuid.uuid4`), no en PostgreSQL, para no depender de
  extensiones (`pgcrypto`/`uuid-ossp`) en el servidor.
- **`device_status` separada de `devices`**: el estado (online/offline/última conexión) cambia con cada
  heartbeat, mientras que los atributos del dispositivo casi no cambian. Separarlas evita que las
  escrituras frecuentes de estado bloqueen o infle el historial de la fila principal del dispositivo.
- **Índice único parcial en `tutor_devices`** (`tutor_user_id, device_id` únicos sólo donde
  `unlinked_at IS NULL`): impide dos vínculos activos entre el mismo tutor y el mismo dispositivo a la
  vez, pero permite re-vincular después de una desvinculación, conservando el historial.
- **`pairing_codes.code_hash` no es único**: los códigos son de 6 dígitos (1 000 000 de combinaciones);
  exigir unicidad global impediría reutilizar un valor después de que el código original expiró. La
  unicidad de "código activo" es una invariante de aplicación (Sprint 5), no de esquema.
- **`sessions` guarda el hash del refresh token, nunca el token**, igual que `pairing_codes` con el
  código. Ninguna tabla almacena un secreto en texto plano.
- **`audit_logs.extra` en vez de `metadata`**: `metadata` es un nombre reservado en SQLAlchemy
  declarativo (referencia al registro de `Base.metadata`).
- **Ningún `ON DELETE CASCADE` incondicional hacia `users`**: borrar un tutor no puede borrar
  silenciosamente los dispositivos que supervisa (`devices.supervised_user_id` usa `RESTRICT`); sí se
  permite cascada en las tablas puramente derivadas de la relación (`user_roles`, `tutor_devices`,
  `sessions`, `pairing_codes`).

## Migraciones

- Herramienta: Alembic, plantilla async (reutiliza el motor `asyncpg` de la aplicación).
- La URL de conexión nunca se escribe en `alembic.ini`; `alembic/env.py` la toma de
  `app.core.config.settings.database_url` en tiempo de ejecución.
- La migración inicial (`e27f40867c61`) siembra los roles `TUTOR` y `SUPERVISADO` como parte de
  `upgrade()`.
- Verificado en este sprint: `alembic upgrade head` y `alembic downgrade -1` contra PostgreSQL 18 real
  (Docker), en ambas direcciones, sin dejar tablas huérfanas.
- El backend nunca usa `Base.metadata.create_all`; toda la creación de esquema pasa por Alembic. Un
  servicio `migrate` en `compose.yaml` y `compose.test.yaml` corre `alembic upgrade head` y debe
  terminar con éxito antes de que `backend` arranque (`depends_on: condition: service_completed_successfully`).

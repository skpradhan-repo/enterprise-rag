-- V2: Application user mapping (mirrors Keycloak subjects)
CREATE TABLE app_user (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    email       VARCHAR(255) NOT NULL,
    roles       TEXT[] NOT NULL DEFAULT '{}',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_user_keycloak ON app_user(keycloak_id);
CREATE INDEX idx_app_user_tenant   ON app_user(tenant_id);

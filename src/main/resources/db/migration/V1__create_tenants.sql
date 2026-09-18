-- V1: Tenant table
CREATE TABLE tenant (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO tenant (id, name, slug) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Acme Healthcare', 'acme'),
    ('00000000-0000-0000-0000-000000000002', 'Beta Medical Group', 'beta');

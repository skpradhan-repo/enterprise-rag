-- V6: Audit events table for observability
CREATE TABLE audit_event (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID,
    user_id         UUID,
    event_type      VARCHAR(100) NOT NULL,
    correlation_id  VARCHAR(100),
    conversation_id UUID,
    model_name      VARCHAR(100),
    tool_name       VARCHAR(100),
    status          VARCHAR(20),
    latency_ms      BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event_tenant  ON audit_event(tenant_id);
CREATE INDEX idx_audit_event_user    ON audit_event(user_id);
CREATE INDEX idx_audit_event_type    ON audit_event(event_type);
CREATE INDEX idx_audit_event_created ON audit_event(created_at);

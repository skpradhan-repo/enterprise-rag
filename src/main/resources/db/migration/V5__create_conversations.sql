-- V5: Conversation and message tables
CREATE TABLE conversation (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    user_id     UUID NOT NULL REFERENCES app_user(id),
    title       VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_user   ON conversation(user_id);
CREATE INDEX idx_conversation_tenant ON conversation(tenant_id);

CREATE TABLE message (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,    -- USER | ASSISTANT | SYSTEM
    content         TEXT NOT NULL,
    source_doc_ids  UUID[] DEFAULT '{}',
    model_used      VARCHAR(100),
    latency_ms      INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_conversation ON message(conversation_id);
CREATE INDEX idx_message_created      ON message(created_at);

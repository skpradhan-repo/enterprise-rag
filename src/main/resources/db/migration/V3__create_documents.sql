-- V3: Document and document chunk tables
-- Use VARCHAR for status/type so Hibernate @Enumerated(EnumType.STRING) works without explicit casts
CREATE TABLE document (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id),
    uploaded_by     UUID NOT NULL REFERENCES app_user(id),
    title           VARCHAR(500) NOT NULL,
    file_name       VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(255),
    file_size_bytes BIGINT,
    doc_type        VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    status          VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    page_count      INT,
    tags            TEXT[] DEFAULT '{}',
    error_message   TEXT,
    storage_path    VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_tenant    ON document(tenant_id);
CREATE INDEX idx_document_status    ON document(status);
CREATE INDEX idx_document_uploaded  ON document(uploaded_by);

CREATE TABLE document_chunk (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenant(id),
    chunk_index     INT NOT NULL,
    page_number     INT,
    content         TEXT NOT NULL,
    token_count     INT,
    vector_store_id VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunk_document ON document_chunk(document_id);
CREATE INDEX idx_chunk_tenant   ON document_chunk(tenant_id);

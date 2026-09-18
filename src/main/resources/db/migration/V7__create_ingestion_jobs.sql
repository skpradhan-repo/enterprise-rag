-- V7: Ingestion job tracking table
CREATE TABLE ingestion_job (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenant(id),
    status          VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    chunks_created  INT DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingest_job_document ON ingestion_job(document_id);
CREATE INDEX idx_ingest_job_status   ON ingestion_job(status);
CREATE INDEX idx_ingest_job_tenant   ON ingestion_job(tenant_id);

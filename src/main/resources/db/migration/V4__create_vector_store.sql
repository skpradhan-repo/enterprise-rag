-- V4: PGVector store table (used by Spring AI VectorStore auto-configuration)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding VECTOR(768)   -- nomic-embed-text output dimension
);

-- HNSW index for fast approximate nearest-neighbour search
CREATE INDEX idx_vector_store_hnsw
    ON vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Metadata index for tenant + status filtering
CREATE INDEX idx_vector_store_metadata ON vector_store USING GIN(metadata);

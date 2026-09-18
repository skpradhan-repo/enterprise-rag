-- V8: Storage path for uploaded document binaries
ALTER TABLE document ADD COLUMN IF NOT EXISTS checksum VARCHAR(64);
CREATE INDEX idx_document_checksum ON document(checksum) WHERE checksum IS NOT NULL;

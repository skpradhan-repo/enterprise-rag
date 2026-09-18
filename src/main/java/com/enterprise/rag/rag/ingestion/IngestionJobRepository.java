package com.enterprise.rag.rag.ingestion;

import com.enterprise.rag.rag.ingestion.IngestionJobTracker.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {
    Optional<IngestionJob> findByDocumentId(UUID documentId);
}

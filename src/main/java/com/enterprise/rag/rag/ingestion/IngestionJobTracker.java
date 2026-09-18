package com.enterprise.rag.rag.ingestion;

import com.enterprise.rag.api.error.ErrorCode;
import com.enterprise.rag.api.error.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks the status of asynchronous ingestion jobs in the database.
 */
@Service
public class IngestionJobTracker {

    private final IngestionJobRepository repository;

    public IngestionJobTracker(IngestionJobRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IngestionJob createJob(UUID documentId, UUID tenantId) {
        IngestionJob job = new IngestionJob();
        job.setDocumentId(documentId);
        job.setTenantId(tenantId);
        job.setStatus("QUEUED");
        return repository.save(job);
    }

    @Transactional
    public void markRunning(UUID jobId) {
        IngestionJob job = find(jobId);
        job.setStatus("RUNNING");
        job.setStartedAt(OffsetDateTime.now());
        repository.save(job);
    }

    @Transactional
    public void markCompleted(UUID jobId, int chunksCreated) {
        IngestionJob job = find(jobId);
        job.setStatus("COMPLETED");
        job.setCompletedAt(OffsetDateTime.now());
        job.setChunksCreated(chunksCreated);
        repository.save(job);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        IngestionJob job = find(jobId);
        job.setStatus("FAILED");
        job.setCompletedAt(OffsetDateTime.now());
        job.setErrorMessage(errorMessage != null
                ? errorMessage.substring(0, Math.min(errorMessage.length(), 2000)) : null);
        repository.save(job);
    }

    @Transactional(readOnly = true)
    public IngestionJob get(UUID jobId) {
        return find(jobId);
    }

    private IngestionJob find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.JOB_NOT_FOUND, "Ingestion job '" + id + "' not found."));
    }

    // ── Inner entity (same aggregate) ──────────────────────────────────────
    @Entity
    @Table(name = "ingestion_job")
    @Getter
    @Setter
    public static class IngestionJob {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "document_id", nullable = false)
        private UUID documentId;

        @Column(name = "tenant_id", nullable = false)
        private UUID tenantId;

        @Column(nullable = false, length = 50)
        private String status = "QUEUED";

        @Column(name = "started_at")
        private OffsetDateTime startedAt;

        @Column(name = "completed_at")
        private OffsetDateTime completedAt;

        @Column(name = "chunks_created")
        private Integer chunksCreated = 0;

        @Column(name = "error_message", columnDefinition = "TEXT")
        private String errorMessage;

        @Column(name = "created_at", updatable = false)
        private OffsetDateTime createdAt = OffsetDateTime.now();

        @Column(name = "updated_at")
        private OffsetDateTime updatedAt = OffsetDateTime.now();

        @PreUpdate
        void onUpdate() { this.updatedAt = OffsetDateTime.now(); }
    }
}

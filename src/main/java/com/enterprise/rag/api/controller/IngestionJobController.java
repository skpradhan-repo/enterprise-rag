package com.enterprise.rag.api.controller;

import com.enterprise.rag.api.dto.response.IngestionJobResponse;
import com.enterprise.rag.rag.ingestion.IngestionJobTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion-jobs")
@Tag(name = "Ingestion Jobs", description = "Check document ingestion status")
public class IngestionJobController {

    private final IngestionJobTracker jobTracker;

    public IngestionJobController(IngestionJobTracker jobTracker) {
        this.jobTracker = jobTracker;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ingestion job status")
    public IngestionJobResponse get(@PathVariable UUID id) {
        IngestionJobTracker.IngestionJob job = jobTracker.get(id);
        IngestionJobResponse resp = new IngestionJobResponse();
        resp.setId(job.getId());
        resp.setDocumentId(job.getDocumentId());
        resp.setStatus(job.getStatus());
        resp.setChunksCreated(job.getChunksCreated());
        resp.setStartedAt(job.getStartedAt());
        resp.setCompletedAt(job.getCompletedAt());
        resp.setErrorMessage(job.getErrorMessage());
        return resp;
    }
}

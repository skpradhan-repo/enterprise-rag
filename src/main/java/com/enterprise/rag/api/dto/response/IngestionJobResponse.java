package com.enterprise.rag.api.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class IngestionJobResponse {
    private UUID id;
    private UUID documentId;
    private String status;
    private Integer chunksCreated;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String errorMessage;
}

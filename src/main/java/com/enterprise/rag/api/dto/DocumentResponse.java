package com.enterprise.rag.api.dto;

import com.enterprise.rag.document.domain.DocumentStatus;
import com.enterprise.rag.document.domain.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
        UUID           id,
        String         title,
        String         fileName,
        String         mimeType,
        Long           fileSizeBytes,
        DocumentType   docType,
        DocumentStatus status,
        Integer        pageCount,
        List<String>   tags,
        Instant        createdAt,
        Instant        updatedAt
) {}

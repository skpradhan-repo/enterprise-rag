package com.enterprise.rag.api.dto.response;

import com.enterprise.rag.document.domain.DocumentStatus;
import com.enterprise.rag.document.domain.DocumentType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class DocumentResponse {
    private UUID id;
    private UUID tenantId;
    private UUID uploadedBy;
    private String title;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private DocumentType docType;
    private DocumentStatus status;
    private Integer pageCount;
    private String[] tags;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID ingestionJobId;
}

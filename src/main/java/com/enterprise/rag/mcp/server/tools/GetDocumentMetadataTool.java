package com.enterprise.rag.mcp.server.tools;

import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.service.DocumentService;
import com.enterprise.rag.security.authorization.RagAuthorizationService;
import com.enterprise.rag.security.tenant.TenantContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool: Retrieve metadata for a specific document.
 */
@Component
public class GetDocumentMetadataTool {

    private final DocumentService documentService;

    public GetDocumentMetadataTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Tool(description = "Get metadata for a specific document by its ID, including title, type, status, page count, and tags.")
    public Map<String, Object> getDocumentMetadata(String documentId) {
        Document doc = documentService.getById(UUID.fromString(documentId));
        return Map.of(
                "id", doc.getId().toString(),
                "title", doc.getTitle(),
                "docType", doc.getDocType().name(),
                "status", doc.getStatus().name(),
                "pageCount", doc.getPageCount() != null ? doc.getPageCount() : 0,
                "tags", doc.getTags(),
                "createdAt", doc.getCreatedAt().toString()
        );
    }
}

package com.enterprise.rag.mcp.server.tools;

import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.domain.DocumentChunk;
import com.enterprise.rag.document.repository.DocumentChunkRepository;
import com.enterprise.rag.document.service.DocumentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP Tool: Retrieve the full chunked content of a document.
 */
@Component
public class GetDocumentByIdTool {

    private final DocumentService documentService;
    private final DocumentChunkRepository chunkRepository;

    public GetDocumentByIdTool(DocumentService documentService,
                                DocumentChunkRepository chunkRepository) {
        this.documentService = documentService;
        this.chunkRepository = chunkRepository;
    }

    @Tool(description = "Retrieve the full text content of a document by its ID, split into chunks with page numbers.")
    public Map<String, Object> getDocumentById(String documentId) {
        Document doc = documentService.getById(UUID.fromString(documentId));
        List<DocumentChunk> chunks = chunkRepository
                .findByDocumentIdOrderByChunkIndex(doc.getId());

        List<Map<String, Object>> chunkData = chunks.stream().map(c -> Map.<String, Object>of(
                "index", c.getChunkIndex(),
                "page", c.getPageNumber() != null ? c.getPageNumber() : 1,
                "content", c.getContent()
        )).collect(Collectors.toList());

        return Map.of(
                "id", doc.getId().toString(),
                "title", doc.getTitle(),
                "chunkCount", chunks.size(),
                "chunks", chunkData
        );
    }
}

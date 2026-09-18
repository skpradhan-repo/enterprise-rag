package com.enterprise.rag.mcp.server.tools;

import com.enterprise.rag.ai.vectorstore.VectorStoreService;
import com.enterprise.rag.security.authorization.MetadataFilterBuilder;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Tool: Search the enterprise knowledge base.
 * Authorization: ROLE_USER; results scoped to caller's tenant via metadata filter.
 */
@Component
public class SearchKnowledgeTool {

    private final VectorStoreService vectorStoreService;
    private final MetadataFilterBuilder filterBuilder;

    public SearchKnowledgeTool(VectorStoreService vectorStoreService,
                                MetadataFilterBuilder filterBuilder) {
        this.vectorStoreService = vectorStoreService;
        this.filterBuilder = filterBuilder;
    }

    @Tool(description = "Search the enterprise knowledge base for information relevant to a query. Returns ranked text excerpts with source metadata.")
    public List<Map<String, Object>> searchKnowledge(String query, Integer topK) {
        int k = topK != null ? Math.min(topK, 20) : 5;

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(0.6)
                .filterExpression(filterBuilder.buildTenantAndStatusFilter())
                .build();

        List<Document> results = vectorStoreService.search(request);

        return results.stream().map(doc -> Map.<String, Object>of(
                "content", doc.getText(),
                "source", doc.getMetadata().getOrDefault("document_title", "Unknown"),
                "page", doc.getMetadata().getOrDefault("page_number", 0),
                "score", doc.getMetadata().getOrDefault("distance", 0.0)
        )).collect(Collectors.toList());
    }
}

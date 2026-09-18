package com.enterprise.rag.rag.retrieval;

import com.enterprise.rag.api.dto.SourceReference;
import com.enterprise.rag.configuration.AppProperties;
import com.enterprise.rag.security.authorization.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Authorization-aware vector retrieval service.
 *
 * <p>All searches are automatically scoped to the current tenant via
 * {@link MetadataFilterBuilder#buildTenantAndStatusFilter()}.
 * This is the single point where vector search is performed — it must never
 * be bypassed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore          vectorStore;
    private final MetadataFilterBuilder filterBuilder;
    private final AppProperties         props;

    /**
     * Performs tenant-isolated similarity search.
     *
     * @param question the user question (will be embedded internally by Spring AI)
     * @return list of matching {@link Document} objects with metadata
     */
    public List<Document> retrieve(String question) {
        int    topK      = props.getRag().getTopK();
        double threshold = props.getRag().getSimilarityThreshold();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression(filterBuilder.buildTenantAndStatusFilter())
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("Retrieved {} chunks for query (topK={}, threshold={})",
                results.size(), topK, threshold);
        return results;
    }

    /**
     * Maps a Spring AI {@link Document} to a {@link SourceReference} for the API response.
     */
    public SourceReference toSourceReference(Document doc, int sourceIndex) {
        var meta = doc.getMetadata();
        return new SourceReference(
                parseUuid(meta.get("document_id")),
                (String) meta.getOrDefault("document_name", "Unknown"),
                parseInteger(meta.get("page_number")),
                doc.getId(),
                doc.getScore() != null ? doc.getScore() : 0.0,
                excerpt(doc.getText())
        );
    }

    private String excerpt(String text) {
        if (text == null) return "";
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    private UUID parseUuid(Object val) {
        if (val == null) return null;
        try { return UUID.fromString(val.toString()); } catch (Exception e) { return null; }
    }

    private Integer parseInteger(Object val) {
        if (val == null) return null;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }
}

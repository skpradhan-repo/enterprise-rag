package com.enterprise.rag.ai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service wrapper over Spring AI VectorStore.
 * All callers use this service; the underlying implementation (PgVectorStore) is autowired.
 */
@Service
public class VectorStoreService {

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void add(List<Document> documents) {
        vectorStore.add(documents);
    }

    public void delete(List<String> ids) {
        vectorStore.delete(ids);
    }

    public List<Document> search(SearchRequest request) {
        return vectorStore.similaritySearch(request);
    }
}

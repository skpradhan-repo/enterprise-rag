package com.enterprise.rag.rag.retrieval;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assembles a numbered context string from retrieved document chunks.
 * Each source is numbered [Source N] for citation by the LLM.
 */
@Component
public class ContextAssembler {

    public String assemble(List<Document> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "No relevant context found.";
        }

        StringBuilder sb = new StringBuilder();
        AtomicInteger counter = new AtomicInteger(1);

        for (Document doc : chunks) {
            int n = counter.getAndIncrement();
            String title = (String) doc.getMetadata().getOrDefault("document_title", "Unknown");
            Object page = doc.getMetadata().get("page_number");

            sb.append("[Source ").append(n).append("] ")
              .append("(Document: \"").append(title).append("\"");
            if (page != null) sb.append(", Page: ").append(page);
            sb.append(")\n");
            sb.append(doc.getText()).append("\n\n");
        }

        return sb.toString().trim();
    }
}

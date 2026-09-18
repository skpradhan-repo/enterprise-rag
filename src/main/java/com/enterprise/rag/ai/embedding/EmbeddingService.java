package com.enterprise.rag.ai.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wraps Spring AI's {@link EmbeddingModel} abstraction.
 *
 * <p>The embedding model is Ollama nomic-embed-text running locally.
 * Configuration is in {@code spring.ai.ollama.embedding.model}.
 * Business logic must depend on this service, not on Ollama-specific classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Generates an embedding vector for a single text string.
     *
     * @param text the text to embed (e.g. a user question)
     * @return float array of dimension 768 (nomic-embed-text)
     */
    public float[] embed(String text) {
        log.debug("Generating embedding for text of {} chars", text.length());
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        return response.getResult().getOutput();
    }

    /**
     * Returns the configured embedding model name for audit/observability.
     */
    public String getModelName() {
        return embeddingModel.getClass().getSimpleName();
    }
}

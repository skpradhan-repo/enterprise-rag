package com.enterprise.rag.rag.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Central store for all prompts used by the RAG and LLM layers.
 * Prompts are loaded from classpath resources so they can be versioned with the application.
 */
@Component
public class PromptTemplates {

    @Value("classpath:prompts/rag-system.txt")
    private Resource ragSystemPromptResource;

    @Value("classpath:prompts/fallback.txt")
    private Resource fallbackPromptResource;

    public String getRagSystemPrompt() {
        return load(ragSystemPromptResource);
    }

    public String getFallbackResponse() {
        return load(fallbackPromptResource);
    }

    private String load(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt template: " + resource.getFilename(), e);
        }
    }
}

package com.enterprise.rag.api.controller;

import com.enterprise.rag.api.dto.ChatRequest;
import com.enterprise.rag.api.dto.ChatResponse;
import com.enterprise.rag.api.dto.LlmDemoRequest;
import com.enterprise.rag.api.dto.LlmDemoResponse;
import com.enterprise.rag.rag.orchestration.RagOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * RAG query and LLM demo endpoints.
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
@Tag(name = "AI", description = "RAG query and LLM demonstration endpoints")
public class AiController {

    private final RagOrchestrator ragOrchestrator;
    private final ChatClient      chatClient;

    @Value("${spring.ai.ollama.chat.model:llama3.2}")
    private String modelName;

    /**
     * Full RAG pipeline: retrieval + context + LLM + sources.
     */
    @PostMapping("/api/v1/rag/query")
    @Operation(summary = "Ask a question using RAG (retrieval-augmented generation)")
    public ChatResponse ragQuery(@Valid @RequestBody ChatRequest request) {
        return ragOrchestrator.query(request.question(), request.conversationId());
    }

    /**
     * Direct LLM demo — no RAG, no vector search.
     * Demonstrates Spring AI ChatClient → Ollama LLM Server flow.
     */
    @PostMapping("/api/v1/llm/demo")
    @Operation(summary = "Direct LLM call (no RAG) — demonstrates LLM Client/Server pattern")
    public LlmDemoResponse llmDemo(@Valid @RequestBody LlmDemoRequest request) {
        long start = System.currentTimeMillis();
        String response = chatClient.prompt()
                .user(request.prompt())
                .call()
                .content();
        return new LlmDemoResponse(response, modelName, "Ollama", System.currentTimeMillis() - start);
    }
}

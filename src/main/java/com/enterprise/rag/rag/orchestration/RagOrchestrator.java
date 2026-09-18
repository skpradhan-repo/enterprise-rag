package com.enterprise.rag.rag.orchestration;

import com.enterprise.rag.api.dto.ChatResponse;
import com.enterprise.rag.api.dto.SourceReference;
import com.enterprise.rag.audit.service.AuditService;
import com.enterprise.rag.configuration.AppProperties;
import com.enterprise.rag.observability.RagMetrics;
import com.enterprise.rag.rag.prompt.PromptSanitizer;
import com.enterprise.rag.rag.prompt.PromptTemplates;
import com.enterprise.rag.rag.retrieval.RetrievalService;
import com.enterprise.rag.security.authorization.RagAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * RAG orchestration service — the central pipeline.
 *
 * <p>Flow:
 * <pre>
 *   sanitize question
 *   → retrieve authorized chunks (PGVector)
 *   → assemble context
 *   → build RAG prompt
 *   → call Ollama via ChatClient
 *   → attribute sources
 *   → audit
 *   → return response
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagOrchestrator {

    private final ChatClient             chatClient;
    private final VectorStore            vectorStore;
    private final RetrievalService       retrievalService;
    private final PromptSanitizer        sanitizer;
    private final PromptTemplates        prompts;
    private final AuditService           auditService;
    private final RagMetrics             metrics;
    private final RagAuthorizationService authService;
    private final AppProperties          props;

    @Value("${spring.ai.ollama.chat.model:llama3.2}")
    private String modelName;

    /**
     * Executes the full RAG query pipeline.
     *
     * @param question       raw user question
     * @param conversationId optional conversation ID for history context
     * @return structured chat response with answer and sources
     */
    public ChatResponse query(String question, String conversationId) {
        long startMs = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        // Step 1 — sanitize
        String sanitized = sanitizer.sanitize(question);
        if (sanitized.isBlank()) {
            return buildFallback(correlationId, conversationId, 0);
        }

        // Step 2 — retrieve authorized context chunks
        List<Document> chunks = retrievalService.retrieve(sanitized);

        if (chunks.isEmpty()) {
            log.debug("No relevant chunks found for question, returning fallback");
            return buildFallback(correlationId, conversationId, System.currentTimeMillis() - startMs);
        }

        // Step 3 — assemble context string
        String context = assembleContext(chunks);

        // Step 4 — call LLM via ChatClient with RAG system prompt
        String systemPrompt = prompts.getRagSystemPrompt()
                .replace("{question_answer_context}", context);

        String answer;
        try {
            answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(sanitized)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            metrics.recordChatFailure();
            return buildFallback(correlationId, conversationId, System.currentTimeMillis() - startMs);
        }

        // Step 5 — attribute sources
        List<SourceReference> sources = IntStream.range(0, chunks.size())
                .mapToObj(i -> retrievalService.toSourceReference(chunks.get(i), i + 1))
                .toList();

        long latency = System.currentTimeMillis() - startMs;
        metrics.recordChatSuccess(latency);

        // Step 6 — audit (async, non-blocking)
        auditService.auditRagQuery(
                authService.getCurrentUserId(),
                authService.getCurrentTenantId(),
                correlationId,
                conversationId,
                sources.stream().map(s -> s.documentId()).toList(),
                modelName,
                latency);

        log.info("RAG query complete: correlationId={}, sources={}, latencyMs={}", correlationId, sources.size(), latency);

        return new ChatResponse(
                answer,
                conversationId != null ? conversationId : UUID.randomUUID().toString(),
                correlationId,
                modelName,
                sources,
                latency,
                Instant.now());
    }

    private String assembleContext(List<Document> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String docName = (String) chunk.getMetadata().getOrDefault("document_name", "Unknown");
            Integer page   = (Integer) chunk.getMetadata().get("page_number");
            sb.append("[Source ").append(i + 1).append("] ")
              .append(docName);
            if (page != null) sb.append(" (page ").append(page).append(")");
            sb.append(":\n").append(chunk.getText()).append("\n\n");
        }
        String result = sb.toString();
        // Truncate if context exceeds max characters to stay within LLM context window
        int max = props.getRag().getMaxContextChars();
        return result.length() > max ? result.substring(0, max) + "\n[context truncated]" : result;
    }

    private ChatResponse buildFallback(String correlationId, String conversationId, long latencyMs) {
        return new ChatResponse(
                prompts.getFallbackResponse(),
                conversationId != null ? conversationId : UUID.randomUUID().toString(),
                correlationId,
                modelName,
                List.of(),
                latencyMs,
                Instant.now());
    }
}

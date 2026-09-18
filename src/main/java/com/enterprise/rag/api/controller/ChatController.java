package com.enterprise.rag.api.controller;

import com.enterprise.rag.api.dto.ChatRequest;
import com.enterprise.rag.api.dto.ChatResponse;
import com.enterprise.rag.conversation.service.ConversationService;
import com.enterprise.rag.rag.orchestration.RagOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "RAG chat endpoint")
public class ChatController {

    private final RagOrchestrator ragOrchestrator;
    private final ConversationService conversationService;

    public ChatController(RagOrchestrator ragOrchestrator,
                           ConversationService conversationService) {
        this.ragOrchestrator = ragOrchestrator;
        this.conversationService = conversationService;
    }

    @PostMapping
    @Operation(summary = "Send a message and receive a RAG-grounded answer with sources")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        // Resolve conversation: use provided ID or create a new one
        UUID convId = null;
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            convId = UUID.fromString(request.conversationId());
            conversationService.getById(convId); // verify ownership
        }

        ChatResponse result = ragOrchestrator.query(
                request.question(),
                request.conversationId()
        );

        // Persist messages if we have a conversation
        if (convId != null) {
            conversationService.addMessage(convId, "USER",    request.question(), null, 0L);
            conversationService.addMessage(convId, "ASSISTANT", result.answer(), result.model(), result.latencyMs());
        }

        return ResponseEntity.ok(result);
    }
}

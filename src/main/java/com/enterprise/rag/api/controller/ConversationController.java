package com.enterprise.rag.api.controller;

import com.enterprise.rag.api.dto.request.ConversationCreateRequest;
import com.enterprise.rag.api.dto.response.ConversationResponse;
import com.enterprise.rag.api.mapper.ConversationMapper;
import com.enterprise.rag.conversation.domain.Conversation;
import com.enterprise.rag.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "Conversation management")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;

    public ConversationController(ConversationService conversationService,
                                   ConversationMapper conversationMapper) {
        this.conversationService = conversationService;
        this.conversationMapper = conversationMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new conversation")
    public ConversationResponse create(@RequestBody ConversationCreateRequest request) {
        return conversationMapper.toResponse(conversationService.create(request));
    }

    @GetMapping
    @Operation(summary = "List the caller's conversations")
    public Page<ConversationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.list(PageRequest.of(page, size))
                .map(conversationMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation with its messages")
    public ConversationResponse get(@PathVariable UUID id) {
        return conversationMapper.toResponse(conversationService.getById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a conversation")
    public void delete(@PathVariable UUID id) {
        conversationService.delete(id);
    }
}

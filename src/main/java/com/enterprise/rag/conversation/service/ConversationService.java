package com.enterprise.rag.conversation.service;

import com.enterprise.rag.api.dto.request.ConversationCreateRequest;
import com.enterprise.rag.api.error.ResourceNotFoundException;
import com.enterprise.rag.conversation.domain.Conversation;
import com.enterprise.rag.conversation.domain.Message;
import com.enterprise.rag.conversation.repository.ConversationRepository;
import com.enterprise.rag.security.authorization.RagAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repo;
    private final RagAuthorizationService authService;

    @Transactional
    public Conversation create(ConversationCreateRequest request) {
        String title = request != null ? request.getTitle() : null;
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        UUID userId   = UUID.fromString(authService.getCurrentUserId());
        return repo.save(Conversation.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<Conversation> list(Pageable pageable) {
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        UUID userId   = UUID.fromString(authService.getCurrentUserId());
        return repo.findByTenantIdAndUserId(tenantId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Conversation getById(UUID id) {
        UUID tenantId = UUID.fromString(authService.getCurrentTenantId());
        UUID userId   = UUID.fromString(authService.getCurrentUserId());
        return repo.findByIdAndTenantIdAndUserId(id, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        Conversation conv = getById(id);
        repo.delete(conv);
    }

    @Transactional
    public Message addMessage(UUID conversationId, String role, String content,
                              String model, long latencyMs) {
        Conversation conv = getById(conversationId);
        Message msg = Message.builder()
                .conversation(conv)
                .role(role)
                .content(content)
                .modelUsed(model)
                .latencyMs((int) latencyMs)
                .build();
        conv.getMessages().add(msg);
        repo.save(conv);
        return msg;
    }
}

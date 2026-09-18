package com.enterprise.rag.conversation.repository;

import com.enterprise.rag.conversation.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @EntityGraph(attributePaths = "messages")
    Page<Conversation> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "messages")
    Optional<Conversation> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, UUID userId);
}

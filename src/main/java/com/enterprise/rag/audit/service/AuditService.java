package com.enterprise.rag.audit.service;

import com.enterprise.rag.audit.domain.AuditEvent;
import com.enterprise.rag.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Asynchronous audit service.
 *
 * <p>Audit events are written in a separate transaction so that
 * a business failure never rolls back an audit record.
 *
 * <p>Sensitive content (prompts, document text, answers) is NEVER stored.
 * Only safe metadata (IDs, model name, latency, status) is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditRagQuery(String userId, String tenantId, String correlationId,
                              String conversationId, List<UUID> sourceDocumentIds,
                              String modelName, long latencyMs) {
        try {
            repo.save(AuditEvent.builder()
                    .userId(parseUuid(userId))
                    .tenantId(parseUuid(tenantId))
                    .eventType("RAG_QUERY")
                    .correlationId(correlationId)
                    .conversationId(parseUuid(conversationId))
                    .modelName(modelName)
                    .status("SUCCESS")
                    .latencyMs(latencyMs)
                    .metadata(Map.of("sourceCount", sourceDocumentIds.size()))
                    .build());
        } catch (Exception e) {
            // Audit failure must never bubble up to the caller
            log.error("Failed to write RAG query audit event: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditIngestion(String userId, String tenantId, UUID documentId,
                               String status, long latencyMs) {
        try {
            repo.save(AuditEvent.builder()
                    .userId(parseUuid(userId))
                    .tenantId(parseUuid(tenantId))
                    .eventType("DOCUMENT_INGESTION")
                    .status(status)
                    .latencyMs(latencyMs)
                    .metadata(Map.of("documentId", documentId.toString()))
                    .build());
        } catch (Exception e) {
            log.error("Failed to write ingestion audit event: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditMcpTool(String userId, String tenantId, String toolName,
                              String status, long latencyMs) {
        try {
            repo.save(AuditEvent.builder()
                    .userId(parseUuid(userId))
                    .tenantId(parseUuid(tenantId))
                    .eventType("MCP_TOOL_CALL")
                    .toolName(toolName)
                    .status(status)
                    .latencyMs(latencyMs)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write MCP tool audit event: {}", e.getMessage());
        }
    }

    private UUID parseUuid(String val) {
        if (val == null) return null;
        try { return UUID.fromString(val); } catch (Exception e) { return null; }
    }
}

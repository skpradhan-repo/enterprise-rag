package com.enterprise.rag.audit.domain;

/**
 * Types of auditable events in the system.
 */
public enum AuditEventType {
    DOCUMENT_UPLOAD,
    DOCUMENT_DELETE,
    DOCUMENT_ACCESS,
    CHAT_REQUEST,
    CONVERSATION_CREATE,
    CONVERSATION_DELETE,
    LOGIN,
    LOGOUT,
    INGESTION_STARTED,
    INGESTION_COMPLETED,
    INGESTION_FAILED,
    ADMIN_ACTION,
    MCP_TOOL_CALL
}

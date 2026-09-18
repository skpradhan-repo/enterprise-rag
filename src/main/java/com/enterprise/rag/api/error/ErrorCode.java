package com.enterprise.rag.api.error;

/**
 * Business error codes returned in RFC 7807 problem details.
 */
public enum ErrorCode {
    // Document
    DOCUMENT_NOT_FOUND,
    DUPLICATE_DOCUMENT,
    INVALID_FILE_TYPE,
    FILE_TOO_LARGE,
    DOCUMENT_NOT_INDEXED,

    // Ingestion
    JOB_NOT_FOUND,
    INGESTION_FAILED,

    // Conversation / Chat
    CONVERSATION_NOT_FOUND,
    INVALID_CONVERSATION,
    MESSAGE_TOO_LONG,

    // LLM / Vector Store
    LLM_UNAVAILABLE,
    VECTOR_STORE_ERROR,
    EMBEDDING_FAILED,

    // Auth
    ACCESS_DENIED,
    TENANT_MISMATCH,

    // Generic
    VALIDATION_ERROR,
    INTERNAL_ERROR
}

package com.enterprise.rag.rag.ingestion;

public class DocumentExtractionException extends RuntimeException {
    public DocumentExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

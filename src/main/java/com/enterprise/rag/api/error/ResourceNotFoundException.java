package com.enterprise.rag.api.error;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(message);
    }
}

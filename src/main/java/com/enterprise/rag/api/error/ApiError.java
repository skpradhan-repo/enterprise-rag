package com.enterprise.rag.api.error;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * RFC 7807 Problem Details error response.
 * Never exposes stack traces, internal class names, or query details.
 */
public record ApiError(
        int status,
        String title,
        String detail,
        String correlationId,
        List<FieldViolation> violations,
        Instant timestamp
) {
    public static ApiError of(int status, String title, String detail) {
        return new ApiError(status, title, detail, UUID.randomUUID().toString(),
                List.of(), Instant.now());
    }

    public static ApiError withViolations(int status, String title, List<FieldViolation> violations) {
        return new ApiError(status, title, "Validation failed", UUID.randomUUID().toString(),
                violations, Instant.now());
    }

    public record FieldViolation(String field, String message) {}
}

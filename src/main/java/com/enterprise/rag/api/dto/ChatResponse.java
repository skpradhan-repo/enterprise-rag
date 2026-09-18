package com.enterprise.rag.api.dto;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        String answer,
        String conversationId,
        String correlationId,
        String model,
        List<SourceReference> sources,
        long latencyMs,
        Instant respondedAt
) {}

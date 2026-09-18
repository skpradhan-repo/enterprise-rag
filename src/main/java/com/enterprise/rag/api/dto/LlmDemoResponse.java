package com.enterprise.rag.api.dto;

public record LlmDemoResponse(
        String response,
        String model,
        String provider,
        long latencyMs
) {}

package com.enterprise.rag.api.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class MessageResponse {
    private UUID id;
    private String role;
    private String content;
    private List<SourceReference> sources;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
    private OffsetDateTime createdAt;
}

package com.enterprise.rag.api.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ChatResponse {
    private UUID conversationId;
    private UUID messageId;
    private String answer;
    private List<SourceReference> sources;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
}

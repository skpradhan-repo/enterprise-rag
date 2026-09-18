package com.enterprise.rag.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequest {

    @NotNull(message = "conversationId is required")
    private UUID conversationId;

    @NotBlank(message = "message must not be blank")
    @Size(max = 4000, message = "message must not exceed 4000 characters")
    private String message;

    @Positive
    private Integer topK = 5;

    private Double scoreThreshold = 0.65;
}

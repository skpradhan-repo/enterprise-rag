package com.enterprise.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LlmDemoRequest(
        @NotBlank @Size(max = 2048) String prompt
) {}

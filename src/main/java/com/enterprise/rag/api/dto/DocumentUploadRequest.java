package com.enterprise.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DocumentUploadRequest(
        @NotBlank @Size(max = 500) String title,
        List<String> tags
) {}

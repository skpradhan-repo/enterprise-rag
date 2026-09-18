package com.enterprise.rag.api.dto;

import java.util.UUID;

public record SourceReference(
        UUID    documentId,
        String  documentName,
        Integer pageNumber,
        String  chunkId,
        double  similarity,
        String  excerpt
) {}

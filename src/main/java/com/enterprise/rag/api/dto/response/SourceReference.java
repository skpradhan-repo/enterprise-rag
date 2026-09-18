package com.enterprise.rag.api.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class SourceReference {
    private UUID chunkId;
    private UUID documentId;
    private String documentTitle;
    private Integer pageNumber;
    private Double score;
    private String excerpt;
}

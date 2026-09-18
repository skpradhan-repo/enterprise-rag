package com.enterprise.rag.api.mapper;

import com.enterprise.rag.api.dto.response.DocumentResponse;
import com.enterprise.rag.document.domain.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {

    @Mapping(source = "fileSizeBytes", target = "fileSize")
    @Mapping(target = "ingestionJobId", ignore = true)
    DocumentResponse toResponse(Document document);

    default OffsetDateTime map(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

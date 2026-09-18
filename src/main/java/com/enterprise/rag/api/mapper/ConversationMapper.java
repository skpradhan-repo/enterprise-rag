package com.enterprise.rag.api.mapper;

import com.enterprise.rag.api.dto.response.ConversationResponse;
import com.enterprise.rag.api.dto.response.MessageResponse;
import com.enterprise.rag.conversation.domain.Conversation;
import com.enterprise.rag.conversation.domain.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConversationMapper {
    ConversationResponse toResponse(Conversation conversation);

    @Mapping(target = "sources", ignore = true)
    @Mapping(target = "promptTokens", ignore = true)
    @Mapping(target = "completionTokens", ignore = true)
    MessageResponse toMessageResponse(Message message);

    default OffsetDateTime map(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

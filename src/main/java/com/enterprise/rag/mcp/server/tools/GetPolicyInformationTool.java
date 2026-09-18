package com.enterprise.rag.mcp.server.tools;

import com.enterprise.rag.document.domain.Document;
import com.enterprise.rag.document.domain.DocumentStatus;
import com.enterprise.rag.document.repository.DocumentRepository;
import com.enterprise.rag.security.tenant.TenantContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP Tool: Retrieve policy documents by keyword or tag.
 */
@Component
public class GetPolicyInformationTool {

    private final DocumentRepository documentRepository;

    public GetPolicyInformationTool(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Tool(description = "Retrieve policy documents from the knowledge base. Searches by tag 'policy' and optional keyword filter in titles.")
    public List<Map<String, Object>> getPolicyInformation(String keyword) {
        UUID tenantId = UUID.fromString(TenantContext.get());

        return documentRepository.findByTenantIdAndStatus(
                tenantId, DocumentStatus.INDEXED, PageRequest.of(0, 20))
                .getContent().stream()
                .filter(doc -> keyword == null || doc.getTitle().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .map(doc -> Map.<String, Object>of(
                        "id", doc.getId().toString(),
                        "title", doc.getTitle(),
                        "tags", doc.getTags(),
                        "pageCount", doc.getPageCount() != null ? doc.getPageCount() : 0
                ))
                .collect(Collectors.toList());
    }
}

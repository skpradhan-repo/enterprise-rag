package com.enterprise.rag.mcp.tools;

import com.enterprise.rag.mcp.server.tools.GetDocumentByIdTool;
import com.enterprise.rag.mcp.server.tools.GetDocumentMetadataTool;
import com.enterprise.rag.mcp.server.tools.GetPolicyInformationTool;
import com.enterprise.rag.mcp.server.tools.SearchKnowledgeTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all MCP tools as Spring AI {@link ToolCallbackProvider} beans.
 *
 * <p>The Spring AI MCP Server webmvc starter automatically exposes these
 * tool callbacks via the SSE endpoint at {@code /mcp/sse}.
 *
 * <p>Authorization is enforced within each tool method using
 * {@link com.enterprise.rag.security.authorization.RagAuthorizationService}.
 * All tools are read-only in the MVP.
 */
@Configuration
@RequiredArgsConstructor
public class McpToolsConfig {

    private final SearchKnowledgeTool      searchKnowledgeTool;
    private final GetDocumentMetadataTool  getDocumentMetadataTool;
    private final GetDocumentByIdTool      getDocumentByIdTool;
    private final GetPolicyInformationTool getPolicyInformationTool;

    @Bean
    public ToolCallbackProvider enterpriseToolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        searchKnowledgeTool,
                        getDocumentMetadataTool,
                        getDocumentByIdTool,
                        getPolicyInformationTool)
                .build();
    }
}

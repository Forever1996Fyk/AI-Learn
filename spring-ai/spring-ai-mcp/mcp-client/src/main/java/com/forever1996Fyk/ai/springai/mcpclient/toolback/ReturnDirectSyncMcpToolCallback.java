package com.forever1996Fyk.ai.springai.mcpclient.toolback;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/24 22:54
 **/
public class ReturnDirectSyncMcpToolCallback extends SyncMcpToolCallback {
    private final boolean returnDirect;


    /**
     * Creates a callback with default settings.
     *
     * @param mcpClient the MCP client for tool execution
     * @param tool      the MCP tool to adapt
     * @deprecated use {@link #builder()} instead
     */
    public ReturnDirectSyncMcpToolCallback(McpSyncClient mcpClient, McpSchema.Tool tool, boolean returnDirect) {
        super(mcpClient, tool);
        this.returnDirect = returnDirect;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder()
                .returnDirect(returnDirect)
                .build();
    }
}

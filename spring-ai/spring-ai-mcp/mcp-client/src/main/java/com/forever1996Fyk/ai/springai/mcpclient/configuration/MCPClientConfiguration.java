package com.forever1996Fyk.ai.springai.mcpclient.configuration;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/25 11:06
 **/
@Configuration
public class MCPClientConfiguration {

//    @Bean
//    public ToolCallbackProvider toolCallbackProvider(List<McpSyncClient> mcpSyncClients) {
//        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build();
//    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, SyncMcpToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个非常有帮助的助手，可以调用工具来回答用户问题")
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();
    }
}

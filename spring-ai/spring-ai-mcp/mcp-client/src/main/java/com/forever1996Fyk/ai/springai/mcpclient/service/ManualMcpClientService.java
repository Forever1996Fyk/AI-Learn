package com.forever1996Fyk.ai.springai.mcpclient.service;

import com.forever1996Fyk.ai.springai.mcpclient.toolback.ReturnDirectMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/24 22:11
 **/
@Service
public class ManualMcpClientService {
    @Autowired
    private ChatModel chatModel;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
//        // STDIO
//        ServerParameters parameters = ServerParameters.builder("java")
//                .args("-jar", "D:\\LLMentor\\LLMentor\\mcp\\mcp-server-stdio\\target\\mcp-server-stdio-1.0.0-SNAPSHOT.jar")
//                .build();
//        StdioClientTransport stdioTransport = new StdioClientTransport(parameters, McpJsonMapper.createDefault());
//
//        McpSyncClient stdioClient = McpClient.sync(stdioTransport)
//                .clientInfo(new io.modelcontextprotocol.spec.McpSchema.Implementation("my‑client", "1.0"))
//                .requestTimeout(Duration.ofSeconds(10))
//                .build();
//
//        stdioClient.initialize();

//        // SSE
//        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://127.0.0.1:8081").sseEndpoint("/sse").build();
//        McpSyncClient sseClient = McpClient.sync(transport)
//                .clientInfo(new io.modelcontextprotocol.spec.McpSchema.Implementation("sse-client", "1.0"))
//                .requestTimeout(Duration.ofSeconds(10))
//                .build();
//        sseClient.initialize();

        // STREAMABLE
        HttpClientStreamableHttpTransport streamableTransport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:8082/stream/test/").endpoint("api/mcp").build();
        McpSyncClient streamableClient = McpClient.sync(streamableTransport)
                .clientInfo(new io.modelcontextprotocol.spec.McpSchema.Implementation("streamable-client", "1.0"))
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        streamableClient.initialize();

        List<McpSyncClient> clients = List.of(streamableClient);

        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(clients)
                .toolFilter((conn, tool) -> tool.name().startsWith("goods"))
                .build();
//        ReturnDirectMcpToolCallbackProvider provider = new ReturnDirectMcpToolCallbackProvider(List.of(sseClient), true);

        ToolCallback[] callbacks = provider.getToolCallbacks();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(callbacks)
                .build();
    }

    /**
     * 智能体调用
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}

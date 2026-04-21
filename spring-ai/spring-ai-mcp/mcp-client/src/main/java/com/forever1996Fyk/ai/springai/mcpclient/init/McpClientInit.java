package com.forever1996Fyk.ai.springai.mcpclient.init;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/21 15:16
 **/
@Component
public class McpClientInit implements CommandLineRunner {
    private final ChatClient.Builder chatClientBuilder;
    private final List<McpSyncClient> mcpSyncClients;

    public McpClientInit(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpSyncClients = mcpSyncClients;
    }

    @Override
    public void run(String... args) throws Exception {
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("你是一个可以查询天气的助手，可以调用工具回答用户关于天气相关问题。")
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();

        System.out.println("\n我是你的 AI 助手。\n");
        try (Scanner scanner = new Scanner(System.in)){
            while (true) {
                System.out.println("\n用户：");
                System.out.println("\n助手：" +
                        // chatClient.prompt(...)：将用户输入作为提示词
                        chatClient.prompt(scanner.nextLine())
                                .call()
                                // 调用 LLM 模型并获取响应内容
                                .content());
            }
        }

    }
}

package com.forever1996Fyk.ai.agentplus.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @program: AI-Learn
 * @description:
 * agentx 所有 MCP 客户端集中装配。
 * <p>
 * 目前两个 MCP 服务：
 * - mcp-echarts（本地 HTTP）：生成图表后上传 MinIO 返回 URL
 * - Tavily 搜索（远程 HTTP + Bearer 认证）：联网搜索工具
 * @author: YuKai Fan
 * @create: 2026/9/21 11:32
 **/
@Slf4j
@Configuration
public class AgentPlusMcpConfig {

    @Bean(destroyMethod = "close")
    public McpSyncClient chartMcpSyncClient(
            @Value("${data-agent.chart.mcp-url:http://localhost:3033/mcp}") String mcpUrl
    ) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(mcpUrl).build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(300))
                .build();
        client.initialize();
        log.info("[shushu-agent-plus] mcp-echarts 客户端初始化完成：{}", mcpUrl);
        return client;
    }

    @Bean(destroyMethod = "close", name = "tavilyMcpSyncClient")
    public McpSyncClient tavilyMcpSyncClient(
            @Value("${tavily.api-key:}") String apiKey,
            @Value("${tavily.mcp-url:https://mcp.tavily.com/mcp/}") String mcpUrl) {

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[shushu-agent-plus] Tavily API key 未配置（tavily.api-key），联网功能不可用");
            return null;
        }
        try {
            HttpClientStreamableHttpTransport transport =
                    HttpClientStreamableHttpTransport.builder(mcpUrl)
                            .customizeRequest(rb -> rb.header("Authorization", "Bearer " + apiKey))
                            .build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(300))
                    .build();
            client.initialize();
            log.info("[shushu-agent-plus] Tavily MCP 客户端初始化完成：{}", mcpUrl);
            return client;
        } catch (Exception e) {
            log.warn("[shushu-agent-plus] Tavily MCP 初始化失败，联网功能不可用：{}", e.getMessage());
            return null;
        }
    }
}

package com.forever1996Fyk.ai.agent.controller;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.forever1996Fyk.ai.agent.agent.websearch.WebSearchReactAgent;
import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:10
 **/
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController implements InitializingBean {
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private AgentTaskManager taskManager;
    @Autowired
    private AiSessionService aiSessionService;
    /**
     * 网页搜索工具回调
     */
    private ToolCallback[] webSearchToolCallbacks;

    @Value("${tavily.api-key}")
    private String tavilyApiKey;
    /**
     * Tavily MCP URL
     */
    @Value("${tavily.mcp-url}")
    private String tavilyMcpUrl;

    /**
     * 接收用户查询并返回流式响应， 使用联网搜索获取信息
     *
     * @param query          用户查询
     * @param conversationId 会话ID
     * @return 流式响应
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "智能问答", description = "接收用户查询并返回流式响应， 使用联网搜索获取信息")
    public Flux<String> webSearchStream(
            @RequestParam String query,
            @RequestParam String conversationId
    ) {
        log.info("收到网页搜索请求：query={}, conversationId={}", query, conversationId);

        if (StringUtils.isBlank(query) || StringUtils.isBlank(StringUtils.trim(query))) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        try {
            // 初始化WebSearchAgent
            WebSearchReactAgent agent = initWebSearchAgent();
            // 创建持久化记忆
            ChatMemory chatMemory = agent.createPersistentChatMemory(conversationId, 30);
            agent.setChatMemory(chatMemory);
            // 执行流式处理
            return agent.stream(conversationId, query);
        } catch (Exception e) {
            log.error("处理网页搜索请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    @GetMapping("/stop")
    @Operation(summary = "停止Agent执行", description = "停止指定会话的Agent执行，中断底层调用")
    public Map<String, Object> stopAgent(@RequestParam String conversationId) {
        log.info("收到停止请求: conversationId={}", conversationId);

        boolean success = taskManager.stopTask(conversationId);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("success", true);
            result.put("message", "已停止执行");
        } else {
            result.put("success", false);
            result.put("message", "没有找到正在执行的任务或已停止");
        }
        return result;
    }

    private WebSearchReactAgent initWebSearchAgent() {
        log.info("初始化WebSearchReactAgent...");
        return WebSearchReactAgent.builder()
                .name("web react")
                .chatModel(chatModel)
                .tools(webSearchToolCallbacks)
                .sessionService(aiSessionService)
                .taskManager(taskManager)
                .maxRounds(5)
                .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("开始初始化工具ToolCallback");

        // 初始化网页搜索工具回调
        initWebSearchToolCallbacks();

        log.info("工具ToolCallback初始化完成");
    }

    private void initWebSearchToolCallbacks() {
        log.info("初始化网页搜索工具回调...");
        // tavily 搜索引擎
        String authorizationHeader = "Bearer " + tavilyApiKey;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Authorization", authorizationHeader);

        // 下面是MCP Client配置，可以用yaml配置替代
        HttpClientStreamableHttpTransport tavTransport = HttpClientStreamableHttpTransport.builder(tavilyMcpUrl)
                .requestBuilder(builder).build();
        McpSyncClient tavilyMcp = McpClient.sync(tavTransport)
                .requestTimeout(Duration.ofSeconds(300))
                .build();
        tavilyMcp.initialize();

        List<McpSyncClient> mcpSyncClients = List.of(tavilyMcp);
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClients)
                .build();

       this.webSearchToolCallbacks =  provider.getToolCallbacks();
        log.info("网页搜索工具回调初始化完成，工具数量: {}", webSearchToolCallbacks.length);
    }
}

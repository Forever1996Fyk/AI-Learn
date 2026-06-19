package com.forever1996Fyk.ai.agent.controller;

import com.forever1996Fyk.ai.agent.agent.websearch.WebSearchReactAgent;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:10
 **/
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private AiSessionService aiSessionService;

    /**
     * 网页搜索工具回调
     */
    private ToolCallback[] webSearchToolCallbacks;

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

    private WebSearchReactAgent initWebSearchAgent() {
        log.info("初始化WebSearchReactAgent...");
        return WebSearchReactAgent.builder()
                .name("web react")
                .chatModel(chatModel)
                .tools(webSearchToolCallbacks)
                .sessionService(aiSessionService)
                .maxRounds(5)
                .build();
    }
}

package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.forever1996Fyk.ai.agent.service.AiPptTemplateService;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.forever1996Fyk.ai.agent.service.ImageGenerationService;
import com.forever1996Fyk.ai.agent.service.MinioService;
import com.forever1996Fyk.ai.agent.service.PptRenderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 22:57
 **/
@Data
@RequiredArgsConstructor
public class PptStateStrategyContext {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final AiPptInstService pptInstService;
    private final AiPptTemplateService pptTemplateService;
    private final PptRenderService pythonRenderService;
    private final ImageGenerationService imageGenerationService;
    private final MinioService minioService;
    private final AiSessionService sessionService;
    private final AgentTaskManager taskManager;
    private final List<ToolCallback> toolCallbacks;
    private final ChatMemory chatMemory;

    private Long currentSessionId;
    private String currentConversationId;
    private boolean modifyMode;
    private String modifyQuery;

    /**
     * 创建thinking类型响应
     */
    public String createThinkingResponse(String content) {
        return createJsonResponse(content, "thinking");
    }


    /**
     * 创建JSON响应
     */
    public String createJsonResponse(String content, String type) {
        return String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                type, content.replace("\"", "\\\"").replace("\n", "\\n"));
    }
}

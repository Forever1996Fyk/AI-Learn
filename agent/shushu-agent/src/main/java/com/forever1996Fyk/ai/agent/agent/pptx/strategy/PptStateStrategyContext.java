package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.forever1996Fyk.ai.agent.service.AiPptTemplateService;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.forever1996Fyk.ai.agent.service.ImageGenerationService;
import com.forever1996Fyk.ai.agent.service.MinioService;
import com.forever1996Fyk.ai.agent.service.PptRenderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Sinks;

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
    private final PptRenderService pptRenderService;
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
     * 创建text类型响应
     */
    public String createTextResponse(String content) {
        return createJsonResponse(content, "text");
    }


    /**
     * 创建JSON响应
     */
    public String createJsonResponse(String content, String type) {
        return String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                type, content.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    /**
     * 加载历史记忆并添加到消息列表
     *
     * @param conversationId 会话ID
     * @param messages      目标消息列表
     * @param skipSystem    是否跳过系统消息
     * @param addLabel     是否添加"对话历史："标签
     */
    public void loadChatHistory(String conversationId, List<Message> messages, boolean skipSystem, boolean addLabel) {
        if (conversationId != null && chatMemory != null) {
            List<Message> history = chatMemory.get(conversationId);
            if (CollectionUtils.isNotEmpty(history)) {
                if (addLabel) {
                    messages.add(new UserMessage("对话历史："));
                }
                for (Message msg : history) {
                    if (skipSystem && msg instanceof SystemMessage) {
                        continue;
                    }
                    messages.add(msg);
                }
            }
        }
    }

    /**
     * 判断是否可以进入下一步
     * 根据提示词约定的标记判断：
     * - 【开始生成PPT】：继续下一步
     * - 【暂停生成PPT】：停止并转向 FAILED
     */
    public boolean shouldContinueToNextStep(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        // 使用 trim 避免前后空格影响匹配
        String trimmedResponse = response.trim();

        // 优先检查明确的标记（使用精确匹配避免误判）
        if (trimmedResponse.contains("【开始生成PPT】") || trimmedResponse.contains("【开始生成PPT】".toLowerCase())) {
            return true;
        }

        if (trimmedResponse.contains("【暂停生成PPT】") || trimmedResponse.contains("【暂停生成PPT】".toLowerCase())) {
            return false;
        }

        // 兜容逻辑：如果没有找到明确标记，根据内容特征判断
        // 如果包含明确的疑问标记（问号、请问等），则不能继续
        String[] stopKeywords = {
                "【暂停生成PPT】", "【暂停生成ppt】",
                "请问", "请问您", "请问是否", "请提供", "请问需要",
                "请问想", "请问希望", "请问要", "请问您的"
        };

        String lowerResponse = trimmedResponse.toLowerCase();
        for (String keyword : stopKeywords) {
            if (lowerResponse.contains(keyword.toLowerCase())) {
                return false;
            }
        }
        // 默认可以继续
        return true;
    }

    /**
     * 继续执行状态机
     *
     * @param inst PPT 实例
     * @param sink 响应流
     * @param query 用户查询
     * @param thinkingBuffer 思考缓冲区
     */
    public void continueStateMachine(AiPptInstEntity inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        PptStateStrategyFactory.getInstance().executeNextState(inst, sink, query, thinkingBuffer, this);
    }

    /**
     * 保存 Disposable 到任务管理器
     *
     * @param conversationId 会话ID
     * @param disposable    Disposable 对象
     */
    public void setDisposable(String conversationId, reactor.core.Disposable disposable) {
        if (conversationId != null && taskManager != null && disposable != null) {
            taskManager.setDisposable(conversationId, disposable);
        }
    }
}

package com.forever1996Fyk.ai.agent.agent;

import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:08
 **/
@Slf4j
public class BaseAgent {
    protected final String name;
    protected final ChatModel chatModel;
    protected String agentType;

    @Setter
    protected ChatMemory chatMemory;

    protected AiSessionService sessionService;
    protected AgentTaskManager agentTaskManager;

    // 计时器
    protected long startTime;
    protected long firstResponseTime;

    /**
     * 用于基于当前对话使用的工具名称
     */
    protected Set<String> usedTools;

    protected Long currentSessionId;
    protected String currentQuestion;

    public BaseAgent(String name, ChatModel chatModel, String agentType) {
        this.name = name;
        this.chatModel = chatModel;
        this.agentType = agentType;
    }

    /**
     * 创建一个持久化的ChatMemory，用于保存会话历史
     *
     * @param conversationId 会话ID
     * @param maxMessages    最大记录数
     * @return 持久化的ChatMemory
     */
    public ChatMemory createPersistentChatMemory(String conversationId, int maxMessages) {
        if (sessionService == null) {
            log.warn("sessionService is null, cannot load chat memory");
            return MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
        }
        // 查询数据库中的对话历史
        List<AiSessionEntity> history = sessionService.listRecentByConversationId(conversationId, maxMessages);
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();

        if (CollectionUtils.isNotEmpty(history)) {
            // 反转历史记录顺序，确保按时间顺序添加
            for (int i = history.size() - 1; i >= 0; i--) {
                AiSessionEntity record = history.get(i);
                // 添加用户问题
                if (StringUtils.isNotBlank(record.getQuestion())) {
                    chatMemory.add(conversationId, new UserMessage(record.getQuestion()));
                }

                // 添加AI回复
                if (StringUtils.isNotBlank(record.getAnswer())) {
                    chatMemory.add(conversationId, new AssistantMessage(record.getAnswer()));
                }
            }
            log.debug("加载会话历史: conversationId={}, recordCount={}", conversationId, history.size());
        }
        return chatMemory;
    }

    /**
     * 检查当前会话是否正在运行任务
     *
     * @param conversationId 会话ID
     * @return 任务执行结果
     */
    protected Flux<String> checkRunningTask(String conversationId) {
        return null;
    }


    /**
     * 初始化计时器
     */
    protected void initTimers() {
        startTime = System.currentTimeMillis();
        firstResponseTime = 0;
    }

    /**
     * 加载历史记忆
     *
     * @param conversationId 会话ID
     * @param messages       消息列表
     * @param skipSystem     是否跳过系统提示词
     * @param addLabel       是否添加"对话历史："标签
     */
    protected void loadChatHistory(String conversationId, List<Message> messages, boolean skipSystem, boolean addLabel) {
        if (StringUtils.isNotBlank(conversationId) && Objects.nonNull(chatMemory)) {
            List<Message> history = chatMemory.get(conversationId);
            if (CollectionUtils.isEmpty(history)) {
                return;
            }
            if (addLabel) {
                messages.add(new UserMessage("对话历史："));
            }
            for (Message message : history) {
                if (skipSystem && message instanceof SystemMessage) {
                    continue;
                }
                messages.add(message);
            }
        }
    }


    /**
     * 获取总响应时间
     *
     * @return 总响应时间（毫秒）
     */
    protected long getTotalResponseTime() {
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取使用的工具列表字符串
     *
     * @return 逗号分隔的工具名称字符串
     */
    protected String getUsedToolsString() {
        if (usedTools == null || usedTools.isEmpty()) {
            return "";
        }
        return String.join(",", usedTools);
    }

    protected void recordFirstResponse() {
        if (firstResponseTime == 0 && startTime > 0) {
            firstResponseTime = System.currentTimeMillis() - startTime;
            log.debug("首次响应时间: {} ms", firstResponseTime);
        }
    }
}

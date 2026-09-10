package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.forever1996Fyk.ai.agentx.core.memory.store.SessionMessageStore;
import com.forever1996Fyk.ai.agentx.core.memory.util.MemoryInjector;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import com.forever1996Fyk.ai.agentx.core.model.ThinkingMode;
import com.forever1996Fyk.ai.agentx.core.prompt.PromptConstants;
import com.forever1996Fyk.ai.agentx.core.tools.toolsearch.DeferredToolRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/1 09:26
 **/
public class LoopMessageBuilder {

    private static final Logger log = LoggerFactory.getLogger(LoopMessageBuilder.class);

    private final String instructions;
    private final MemoryInjector memoryInjector;
    private final ThinkingMode thinkingMode;
    private final DeferredToolRegistry deferredToolRegistry;
    private final boolean todoWriteEnabled;
    private final boolean enableSession;
    private final SessionMessageStore sessionMessageStore;

    public LoopMessageBuilder(String instructions, MemoryInjector memoryInjector, ThinkingMode thinkingMode, DeferredToolRegistry deferredToolRegistry, boolean todoWriteEnabled, boolean enableSession, SessionMessageStore sessionMessageStore) {
        this.instructions = instructions;
        this.memoryInjector = memoryInjector;
        this.thinkingMode = thinkingMode;
        this.deferredToolRegistry = deferredToolRegistry;
        this.todoWriteEnabled = todoWriteEnabled;
        this.enableSession = enableSession;
        this.sessionMessageStore = sessionMessageStore;
    }

    public BuiltMessages buildInitialMessages(String query, RunnableParams params) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统提示词
        String systemPrompt = "";
        if (StringUtils.isNotBlank(instructions)) {
            systemPrompt = instructions;
        }
        systemPrompt = appendSection(systemPrompt, memoryInjector.buildMemorySection(params, query));

        String customParamSection = buildCustomParamSection(params);
        if (StringUtils.isNotBlank(customParamSection)) {
            systemPrompt = systemPrompt + customParamSection;
        }
        if (deferredToolRegistry != null) {
            systemPrompt = appendSection(systemPrompt, PromptConstants.TOOL_SEARCH_GUIDANCE);
        }
        if (todoWriteEnabled) {
            systemPrompt = appendSection(systemPrompt, PromptConstants.TODO_WRITE_GUIDANCE);
        }
        if (StringUtils.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 历史消息链（优先 work_messages 压缩视图，回退 original_messages）
        String conversationId = params != null ? params.getConversationId() : null;
        if (enableSession && sessionMessageStore != null && StringUtils.isNotBlank(conversationId)) {
            List<Message> history = sessionMessageStore.getMessages(conversationId, "working_messages");
            if (history.isEmpty()) {
                history = sessionMessageStore.getMessages(conversationId, "original_messages");
            }
            for (Message message : history) {
                if (!(message instanceof SystemMessage)) {
                    messages.add(message);
                }
            }
        }

        // 3. 边界标记: 当前的消息为历史/背景, 此后为本轮新增
        int newMsgStartIndex = messages.size();

        // 4. 当前用户问题
        String userContent = query;
        if (params != null && params.getOutputType() != null) {
            BeanOutputConverter<?> converter = new BeanOutputConverter<>(
                    params.getOutputType().toTypeReference()
            );
            userContent = userContent + "\n" + converter.getFormat();
        }

        if (thinkingMode == ThinkingMode.DISABLED) {
            userContent = userContent + "\n<no_think>";
        }
        messages.add(new UserMessage(userContent));
        return new BuiltMessages(messages, newMsgStartIndex);
    }


    private String buildCustomParamSection(RunnableParams params) {
        if (params == null || params.getCustomParams() == null || params.getCustomParams().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 系统参数（LLM 可见）\n");
        for (Map.Entry<String, Object> entry : params.getCustomParams().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }


    private static String appendSection(String base, String section) {
        if (section == null || section.isEmpty()) {
            return base;
        }
        return base.isEmpty() ? section : base + "\n\n" + section;
    }
}

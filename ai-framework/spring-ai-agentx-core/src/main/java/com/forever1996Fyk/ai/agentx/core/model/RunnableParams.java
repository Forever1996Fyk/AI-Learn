package com.forever1996Fyk.ai.agentx.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 * Agent 调用参数。
 *
 * 用于传递执行时的额外参数，支持会话管理、长期记忆、上下文注入和工具参数替换。
 *
 * conversationId 用于 SessionMessageStore 会话管理与流式停止。
 * userId 用于长期记忆的用户维度标识，跨会话持久。
 *
 * customParams 和 toolParams 分别面向不同对象：
 * - addParam("language", "zh-CN")：将参数注入系统提示词，LLM 可见并可直接使用。
 * - addToolParam("userId", "123")：不注入系统提示词，只在工具执行前按 inputSchema 注入真实参数。
 *
 * toolParams 适合传递 userId、token、租户 ID 等不应依赖 LLM 生成的运行时参数。
 *
 * @author: YuKai Fan
 * @create: 2026/9/1 09:02
 **/
public class RunnableParams {

    private final String conversationId;
    private final String userId;
    private final Map<String, Object> customParams;

    private RunnableParams(Builder builder) {
        this.conversationId = builder.conversationId;
        this.userId = builder.userId;
        this.customParams = builder.customParams;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getCustomParams() {
        return customParams;
    }

    public static class Builder {
        private String conversationId;
        private String userId;
        private Map<String, Object> customParams;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        public Builder customParams(Map<String, Object> customParams) {
            this.customParams = customParams;
            return this;
        }
        public RunnableParams build() {
            return new RunnableParams(this);
        }
    }
}

package com.forever1996Fyk.ai.agent.agent;

import com.forever1996Fyk.ai.agent.prompts.PlanExecutePromptsFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/12 23:55
 **/
public class PlanExecuteAgent {

    private final ChatModel chatModel;

    private final List<ToolCallback> tools;

    /**
     * plan&execute最大轮数
     */
    private final int maxRounds;

    /**
     * 上下文context压缩阈值
     */
    private final int contextCharLimit;

    /**
     * 控制工具并发调用上限
     * 因为一般我的调用工具可能是toolCall也可能是MCP，但是不管怎么样，再并发场景下，
     * 每个与大模型进行对话多次工具调用时，如果并发过高可能导致工具或MCP服务压力太大导致，调用失败
     * 所以用信号量来限制并发
     */
    private final Semaphore toolSemaphore;

    /**
     * 单个工具调用失败最大重试次数
     */
    private final int maxToolRetries;

    private PlanExecutePromptsFactory planExecutePromptsFactory;

    private ChatMemory chatMemory;

    public PlanExecuteAgent(ChatModel chatModel,
                            List<ToolCallback> tools,
                            int maxRounds,
                            int contextCharLimit,
                            int maxToolRetries,
                            PlanExecutePromptsFactory planExecutePromptsFactory,
                            ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.tools = tools;
        this.maxRounds = maxRounds;
        this.contextCharLimit = contextCharLimit;
        this.maxToolRetries = maxToolRetries;
        this.toolSemaphore = new Semaphore(3);
        this.planExecutePromptsFactory = planExecutePromptsFactory;
        this.chatMemory = chatMemory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatModel chatModel;
        private List<ToolCallback> tools = new ArrayList<>();

        // 默认迭代5轮
        private int maxRounds = 5;

        // 默认context压缩阈值20000字符
        private int contextCharLimit = 50000;

        // 默认工具重试次数2次
        private int maxToolRetries = 2;

        private PlanExecutePromptsFactory planExecutePromptsFactory;

        private ChatMemory chatMemory;

        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder tools(List<ToolCallback> tools) {
            this.tools = tools;
            return this;
        }

        public Builder tools(ToolCallback... tools) {
            this.tools = Arrays.asList(tools);
            return this;
        }

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public Builder contextCharLimit(int contextCharLimit) {
            this.contextCharLimit = contextCharLimit;
            return this;
        }

        public Builder maxToolRetries(int maxToolRetries) {
            this.maxToolRetries = maxToolRetries;
            return this;
        }

        public Builder planExecutePromptsFactory(PlanExecutePromptsFactory planExecutePromptsFactory) {
            this.planExecutePromptsFactory = planExecutePromptsFactory;
            return this;
        }

        public PlanExecuteAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new PlanExecuteAgent(chatModel, tools, maxRounds, contextCharLimit, maxToolRetries, planExecutePrompts, chatMemory);
        }
    }
}

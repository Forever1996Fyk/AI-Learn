package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.forever1996Fyk.ai.agentx.core.interrupt.PauseStateStore;
import com.forever1996Fyk.ai.agentx.core.memory.LongTermMemoryManager;
import com.forever1996Fyk.ai.agentx.core.memory.store.ConversationStore;
import com.forever1996Fyk.ai.agentx.core.memory.store.SessionMessageStore;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import com.forever1996Fyk.ai.agentx.core.model.ThinkingMode;
import com.forever1996Fyk.ai.agentx.core.tools.toolsearch.DeferredToolRegistry;
import com.forever1996Fyk.ai.agentx.core.trace.TraceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 *
 * Agent ReAct 循环执行器：多轮迭代调用 LLM、执行工具，直到产出最终答案或达到上限。
 * 要求 ChatClient 配置 internalToolExecutionEnabled(false)，工具调用由本类控制。
 *
 * @author: YuKai Fan
 * @create: 2026/9/1 09:09
 **/
public class AgentLoopExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopExecutor.class);

    private final int maxRounds;
    private final int maxRetries;
    private final String askUserToolName;
    private final AgentTaskManager taskManager;

    private final ThinkingMode thinkingMode;
    private AgentLoopExecutor(Builder builder) {
        this.maxRounds = builder.maxRounds;
        this.maxRetries = builder.maxRetries;
        this.askUserToolName = builder.askUserToolName;
        this.taskManager = builder.taskManager;
        this.thinkingMode = builder.thinkingMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 流式执行 ReAct 循环，返回 AgentStreamEvent 流。
     */
    public Flux<String> stream(String query, RunnableParams params) {
        String conversationId = params != null ? params.getConversationId() : null;
    }

    public static class Builder {
        private ChatClient chatClient;
        private int maxRounds = 100;
        private List<ToolCallback> tools;
        private AgentTaskManager taskManager;
        private SessionMessageStore sessionMessageStore;
        private ConversationStore conversationStore;
        private String instructions;
        private ChatModel chatModel;
        private LongTermMemoryManager longTermMemoryManager;
        private boolean enableSession = true;
        private boolean enableTrace = true;
        private String askUserToolName;
        private ThinkingMode thinkingMode = ThinkingMode.DISABLED;
        private int maxRetries = 3;
        private DeferredToolRegistry deferredToolRegistry;
        private List<Advisor> advisors;
        private TraceStore traceStore;
        private PauseStateStore stateStore;


        public Builder chatClient(ChatClient v) {
            this.chatClient = v;
            return this;
        }

        public Builder maxRounds(int v) {
            this.maxRounds = v;
            return this;
        }

        public Builder tools(List<ToolCallback> v) {
            this.tools = v;
            return this;
        }

        public Builder taskManager(AgentTaskManager v) {
            this.taskManager = v;
            return this;
        }

        public Builder sessionMessageStore(SessionMessageStore v) {
            this.sessionMessageStore = v;
            return this;
        }

        public Builder conversationStore(ConversationStore v) {
            this.conversationStore = v;
            return this;
        }

        public Builder instructions(String v) {
            this.instructions = v;
            return this;
        }

        public Builder longTermMemoryManager(LongTermMemoryManager v) {
            this.longTermMemoryManager = v;
            return this;
        }

        public Builder chatModel(ChatModel v) {
            this.chatModel = v;
            return this;
        }

        public Builder enableSession(boolean v) {
            this.enableSession = v;
            return this;
        }

        public Builder enableTrace(boolean v) {
            this.enableTrace = v;
            return this;
        }

        public Builder askUserToolName(String v) {
            this.askUserToolName = v;
            return this;
        }

        public Builder thinkingMode(ThinkingMode v) {
            this.thinkingMode = v;
            return this;
        }

        public Builder maxRetries(int v) {
            this.maxRetries = v;
            return this;
        }

        public Builder deferredToolRegistry(DeferredToolRegistry v) {
            this.deferredToolRegistry = v;
            return this;
        }

        public Builder advisors(List<Advisor> v) {
            this.advisors = v;
            return this;
        }

        public Builder traceStore(TraceStore v) {
            this.traceStore = v;
            return this;
        }

        public Builder stateStore(PauseStateStore v) {
            this.stateStore = v;
            return this;
        }

        public AgentLoopExecutor build() {
            Objects.requireNonNull(chatClient, "chatClient must not be null");
            return new AgentLoopExecutor(this);
        }
    }
}

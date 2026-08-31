package com.forever1996Fyk.ai.agentx.core.agent;

import com.forever1996Fyk.ai.agentx.core.advisors.PauseAdvisor;
import com.forever1996Fyk.ai.agentx.core.advisors.RequestLoggingAdvisor;
import com.forever1996Fyk.ai.agentx.core.agent.internal.AgentTaskManager;
import com.forever1996Fyk.ai.agentx.core.context.ContextPolicy;
import com.forever1996Fyk.ai.agentx.core.interrupt.PauseStateStore;
import com.forever1996Fyk.ai.agentx.core.memory.LongTermMemoryConfig;
import com.forever1996Fyk.ai.agentx.core.memory.LongTermMemoryManager;
import com.forever1996Fyk.ai.agentx.core.memory.store.ConversationStore;
import com.forever1996Fyk.ai.agentx.core.memory.store.DataSourceStorageFactory;
import com.forever1996Fyk.ai.agentx.core.memory.store.SessionMessageStore;
import com.forever1996Fyk.ai.agentx.core.model.ThinkingMode;
import com.forever1996Fyk.ai.agentx.core.tools.AskUserTool;
import com.forever1996Fyk.ai.agentx.core.tools.toolsearch.DeferredToolRegistry;
import com.forever1996Fyk.ai.agentx.core.tools.toolsearch.ToolSearchConfig;
import com.forever1996Fyk.ai.agentx.core.trace.TraceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.web.servlet.server.Session;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 * ReactAgent - 基于 ReAct 范式的智能体实现。
 * <p>
 * 实现 Reasoning（推理）+ Acting（行动）循环模式，通过多轮对话完成复杂任务。
 * 支持自动多轮推理和工具调用、流式输出、会话记忆管理。
 * <p>
 * ChatClient 必须配置 internalToolExecutionEnabled(false) 以将工具执行权转交给框架。
 * @author: YuKai Fan
 * @create: 2026/8/31 15:01
 **/
public class ReactAgent {
    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);

    private final String name;
    private final String description;
    private final ChatClient chatClient;
    private final int maxRounds;
    private final List<ToolCallback> tools;

    /**
     * 系统提示词
     */
    private final String instructions;
    /**
     * 当前会话存储
     */
    private final ConversationStore conversationStore;
    /**
     * 会话级别存储
     */
    private final SessionMessageStore sessionMessageStore;
    /**
     * 长期记忆管理
     */
    private final LongTermMemoryManager longTermMemoryManager;

    /**
     * 是否启用会话存储
     */
    private boolean enableSession;
    /**
     * 任务管理器
     */
    private final AgentTaskManager taskManager;

    private final ThinkingMode thinkingMode;
    private final int maxRetries;
    /**
     * 上下文压缩策略
     */
    private final ContextPolicy contextPolicy;
    /**
     * 需要延迟加载的 tools
     */
    private final DeferredToolRegistry deferredToolRegistry;
    /**
     * Trace 审计存储层
     */
    private final TraceStore traceStore;
    private final boolean enableTrace;

    /**
     * Agent 暂停状态存储。默认内存实现，可通过 {@link Builder#stateStore(PauseStateStore)} 自定义。
     * <p>用于 {@link #interrupt(String)} 时自动持久化 PauseState，
     * 配合 {@link #hasInterruptedState(String)} / {@link #resumeStream(String)} 实现断点重连。
     */
    private final PauseStateStore stateStore;

    private ReactAgent(SessionMessageStore sessionMessageStore,
                       ConversationStore conversationStore,
                       LongTermMemoryManager longTermMemoryManager,
                       TraceStore traceStore,
                       PauseStateStore pauseStateStore) {

    }

    public static class Builder {
        private String name;
        private String description;
        private ChatModel chatModel;
        private final List<ToolCallback> tools = new ArrayList<>();
        private final List<Advisor> advisors = new ArrayList<>();
        private int maxRounds = 100;
        private AgentTaskManager taskManager;
        private String instructions;
        private DataSource dataSource;
        private LongTermMemoryConfig longTermMemoryConfig;
        private boolean enableSession = true;
        private boolean askUser = false;
        private ThinkingMode thinkingMode = ThinkingMode.DISABLED;
        private int maxRetries = 3;
        private ContextPolicy contextPolicy;
        private DeferredToolRegistry deferredToolRegistry;
        private boolean enableTrace = true;
        private PauseStateStore stateStore;


        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder tools(ToolCallback... tools) {
            if (tools != null) {
                for (ToolCallback tool : tools) {
                    this.tools.add(tool);
                }
            }
            return this;
        }

        public Builder tools(List<ToolCallback> tools) {
            if (tools != null) {
                this.tools.addAll(tools);
            }
            return this;
        }

        public Builder advisors(Advisor... advisors) {
            this.advisors.addAll(List.of(advisors));
            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            if (advisors != null) {
                this.advisors.addAll(advisors);
            }
            return this;
        }

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public Builder taskManager(AgentTaskManager taskManager) {
            this.taskManager = taskManager;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * 配置 DataSource，框架自动管理会话存储与追踪存储。
         * <p>
         * 框架将自动创建：
         * - SessionMessageStore：当前会话三态（agentx_session 表）
         * - ConversationStore：调用边界（agentx_conversation 表）
         * - TraceStore：LLM 调用审计（agentx_trace 表）
         * <p>
         * 长期记忆是独立维度，通过 {@link #longTermMemory(LongTermMemoryConfig)} 单独控制。
         * <p>
         * 依赖要求：spring-jdbc（或 spring-boot-starter-jdbc）
         *
         * @param dataSource 数据源
         */
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * 启用长期记忆。传入 {@link LongTermMemoryConfig} 即启用，不调用即不启用。
         * <p>
         * 配置内封装了独立的 PgVector DataSource 与 EmbeddingModel，
         * 与 {@link #dataSource(DataSource)} 指向的关系型库互不相关。
         * <p>
         * 启用后，框架会：
         * <ul>
         *   <li>每次调用开始时按 userId + query 语义检索相关记忆，注入 system prompt</li>
         *   <li>调用终态（completed）后异步抽取本次 transcript 的可跨会话事实，写入向量库</li>
         *   <li>命中相似记忆时通过 LLM 合并，避免冲突</li>
         * </ul>
         *
         * @param config 长期记忆配置
         */
        public Builder longTermMemory(LongTermMemoryConfig config) {
            this.longTermMemoryConfig = config;
            return this;
        }

        /**
         * 是否启用会话历史记录（agentx_session）。
         * <p>
         * 禁用后，框架不保存对话历史到数据库，适用于 SubAgent 等无状态场景。
         * 默认启用。
         *
         * @param enabled true 启用（默认），false 禁用
         */
        public Builder enableSession(boolean enabled) {
            this.enableSession = enabled;
            return this;
        }

        /**
         * 启用 Human-in-the-Loop：Agent 可主动向用户提问。
         * <p>
         * 启用后框架自动：
         * - 注册 AskUserTool（LLM 可调用 ask_user 工具）
         * - 创建 PauseAdvisor 拦截 ask_user，暂停循环等待外部回答
         * <p>
         * 使用 callForResult / streamForResult 获取 AgentResult.Paused，
         * 然后调用 resume 继续对话。
         * <p>
         * 默认 false。需要自定义用户输入工具时，通过 {@code tools()} 注册工具，
         * 并通过 {@link PauseAdvisor.Builder#askUserTool(String)} 配置。
         *
         * @param askUser true 启用
         */
        public Builder askUser(boolean askUser) {
            this.askUser = askUser;
            return this;
        }

        /**
         * 启用后，LLM 输出中的 &lt;think&gt;...&lt;/think&gt; 内容会被拆分为
         * {@link AgentStreamEvent.Thinking} 事件，标签外的内容为 {@link AgentStreamEvent.Text} 事件。
         * <p>
         * 适用于 MiniMax M2.7 <think>标签的模型。
         * 默认 false。
         *
         * @param enabled true 启用
         * @deprecated 使用 {@link #thinkingMode(ThinkingMode)} 替代，如 {@code thinkingMode(ThinkingMode.THINK_TAG)}
         */
        @Deprecated
        public Builder thinkTagEnabled(boolean enabled) {
            this.thinkingMode = enabled ? ThinkingMode.THINK_TAG : ThinkingMode.DISABLED;
            return this;
        }

        /**
         * 配置思考模型的输出格式。
         * <p>
         * 不同厂商的思考模型通过不同方式返回推理过程，调用方需根据模型类型选择对应模式：
         * <ul>
         *   <li>{@link ThinkingMode#THINK_TAG} - 思考内容嵌入 content 中的 &lt;think/&gt; 标签（MiniMax等）</li>
         *   <li>{@link ThinkingMode#REASONING_CONTENT} - 思考内容通过独立 reasoning_content 字段返回（DeepSeek、Qwen3.6 等）</li>
         * </ul>
         * <p>
         * 默认 {@link ThinkingMode#DISABLED}，不处理思考内容。
         *
         * @param thinkingMode 思考模式
         */
        public Builder thinkingMode(ThinkingMode thinkingMode) {
            this.thinkingMode = thinkingMode != null ? thinkingMode : ThinkingMode.DISABLED;
            return this;
        }

        /**
         * 配置 LLM 调用的最大重试次数。
         * <p>
         * 当 LLM 调用失败（网络异常、服务端错误等）时，框架自动重试。
         * 所有异常统一重试，不区分类型。
         * <p>
         * 流式模式下，重试时会发出 {@link AgentStreamEvent.Error} 事件通知调用方。
         * <p>
         * 默认 3 次。设为 0 禁用重试。
         *
         * @param maxRetries 最大重试次数
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 配置上下文压缩策略，启用后 Agent 在每轮 LLM 调用前自动压缩历史消息。
         * <p>
         * 压缩策略包括：
         * <ul>
         *   <li>micro_compact: 替换旧工具结果和长参数为占位符</li>
         *   <li>auto_compact: token 超阈值时用 LLM 摘要替换旧消息</li>
         * </ul>
         * <p>
         * 不配置时上下文压缩不启用，Agent 行为完全不变。
         *
         * <p>示例：
         * <pre>{@code
         * // 默认配置
         * .contextPolicy(ContextPolicy.defaults())
         *
         * // 自定义配置
         * .contextPolicy(ContextPolicy.builder()
         *     .tokenThreshold(30000)
         *     .lastKeep(80)
         *     .build())
         * }</pre>
         *
         * @param contextPolicy 压缩策略
         */
        public Builder contextPolicy(ContextPolicy contextPolicy) {
            this.contextPolicy = contextPolicy;
            return this;
        }

        /**
         * 配置延迟加载工具，启用 ToolSearch 能力。
         * <p>
         * 启用后，这些工具不会一次性注入 ChatClient，而是由 LLM 按需搜索和加载。
         * LLM 在需要更多工具时调用 {@code tool_search} 元工具进行搜索。
         * <p>
         * 搜索模式：
         * <ul>
         *   <li>KEYWORD: 纯关键词匹配（Jieba 分词 + 打分排序）</li>
         *   <li>LLM: 纯 LLM 选择（构建精简 catalog，一次 LLM 调用）</li>
         *   <li>HYBRID: 先关键词匹配，无结果时 LLM 兜底（默认）</li>
         * </ul>
         * <p>
         * 不调用此方法时 ToolSearch 不启用，所有工具一次性注入，行为完全不变。
         *
         * <p>示例：
         * <pre>{@code
         * ReactAgent agent = ReactAgent.builder()
         *     .chatModel(chatModel)
         *     .tools(bashTool, readFileTool)              // alwaysLoad
         *     .deferredTools(ToolSearchConfig.defaults(),  // 搜索配置
         *         slackTool, emailTool, calendarTool)      // 延迟加载
         *     .build();
         * }</pre>
         *
         * @param config 搜索配置
         * @param tools  延迟加载的工具
         */
        public Builder deferredTools(ToolSearchConfig config, ToolCallback... tools) {
            if (tools != null && tools.length > 0) {
                this.deferredToolRegistry = DeferredToolRegistry.create(
                        config, List.of(tools), this.chatModel);
            }
            return this;
        }


        /**
         * 启用 LLM 调用审计（内置 {@link RequestLoggingAdvisor}）。
         * <p>
         * 启用后，每次 LLM 调用前会打印入参 JSON，并将请求/响应记录到 {@code agentx_trace} 表。
         * 需要同时配置 {@link #dataSource(DataSource)} 才会入库；仅有 dataSource 或仅有 enableTrace 均不满足。
         * <p>
         * 默认 true。
         *
         * @param enableTrace true 启用（默认），false 禁用
         */
        public Builder enableTrace(boolean enableTrace) {
            this.enableTrace = enableTrace;
            return this;
        }

        /**
         * 配置 Agent 状态存储，启用用户主动中断的断点重连能力。
         * <p>
         * 未配置时默认使用 {@link InMemoryPauseStateStore}（单节点、进程内、TTL 7 天）。
         * 生产环境跨进程持久化请传入 JDBC / Redis 实现。
         *
         * @param stateStore 状态存储实例
         */
        public Builder stateStore(PauseStateStore stateStore) {
            this.stateStore = stateStore;
            return this;
        }

        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            SessionMessageStore sessionMessageStore = null;
            ConversationStore conversationStore = null;

            //  是否实际写入由各 enableXXX 在运行时控制
            TraceStore traceStore = null;
            if (dataSource != null) {
                sessionMessageStore = DataSourceStorageFactory.createSessionMessageStore(dataSource);
                conversationStore = DataSourceStorageFactory.createConversationStore(dataSource);
                traceStore = DataSourceStorageFactory.createTraceStore(dataSource);
            }

            // 根据 LongTermMemoryConfig 构建长期记忆
            LongTermMemoryManager longTermMemoryManager = null;
            if (longTermMemoryConfig != null) {
                longTermMemoryManager = new LongTermMemoryManager(longTermMemoryConfig, chatModel);
            }

            // taskManager 默认实例化：流式停止 / 用户主动中断都依赖它，
            // 不再要求调用方显式注入（与 stateStore 默认策略一致）
            if (taskManager == null) {
                taskManager = new AgentTaskManager();
            }

            if (askUser) {
                tools.addAll(List.of(AskUserTool.create()));

                boolean askUserCovered = advisors.stream()
                        .filter(a -> a instanceof PauseAdvisor)
                        .map(a -> (PauseAdvisor) a)
                        .anyMatch(pa -> pa.shouldIntercept("ask_user"));
                if (!askUserCovered) {
                    advisors.add(PauseAdvisor.builder().askUserTool("ask_user").build());
                }
            }

            // requestLogging=true 时自动注册内置 RequestLoggingAdvisor
            if (enableTrace) {
                advisors.add(new RequestLoggingAdvisor(chatModel));
            }
        }
    }
}

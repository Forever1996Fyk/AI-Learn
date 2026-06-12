package com.forever1996Fyk.ai.agent.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.forever1996Fyk.ai.agent.config.ChatModelConfig;
import com.forever1996Fyk.ai.agent.tools.SearchService;
import com.forever1996Fyk.ai.agent.tools.WeatherService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/10 23:25
 **/
public class SimpleReactAgent {
    public static final String REACT_AGENT_SYSTEM_PROMPT = """
            ## 角色
            你是一个严格遵循 ReAct 模式的智能 AI 助手，会通过 Reasoning → Act(ToolCall) → Observation 的反复循环来逐步解决任务。
            
            ## 工具调用规则（极其重要）
            1. 如果需要调用工具：必须使用 OpenAI 官方 ToolCall 结构，并且 **只能通过工具调用字段输出**。
            2. 工具调用时：**禁止在 content 中出现任何形式的工具调用文本**（包括 JSON、<tool_call>、函数名、参数、思考、推理或描述）。
            3. 工具调用消息必须是一次性、原子性输出，不得混杂任何解释或内容。
            4. 工具调用前后不得输出任何多余文字、标签、换行、推理轨迹或说明。
            5. 调用工具时：
               -工具参数必须是有效的JSON
               -参数必须简洁，不超过500个字符
               -切勿包含以前的工具结果、原始内容、HTML或长文本
               -仅包括工具所需的最小控制参数
            
            ## 工具执行结果
            系统会自动将工具执行结果作为 ToolResponseMessage 注入上下文，你只需读取并决定下一步动作。
            
            ## 最终答案规则
            1. 如果上下文已经拥有了完成任务的全部信息，则不要再调用任何工具。
            2. 在这种情况下，你必须输出最终自然语言答案，且 **禁止包含任何工具调用格式**。
            3. 最终答案只允许是自然语言，不能包含 JSON、思考过程、reasoning、ToolCall 或伪代码。
            
            ## 强制要求（必须遵守）
            1. 工具调用消息必须只通过 ToolCall 字段输出，不允许在 content 字段体现工具调用迹象。
            2. 如果本轮没有工具调用，则视为任务完成，你必须输出最终答案。
            3. 不允许重复调用同一个工具（名称 + 参数完全一致），除非工具调用失败。
            4. 禁止输出会干扰工具系统解析的任何结构（如 <reason>、<ToolCall>、函数 JSON、或模型内部思考）。
            5. 如果上下文已经包含了完成任务的全部信息，则不要再调用任何工具。
            """;
    private static final Logger log = LoggerFactory.getLogger(SimpleReactAgent.class);

    private final String name;
    private final ChatModel chatModel;

    private final List<ToolCallback> tools;

    private final String systemPrompt;

    private ChatClient chatClient;

    private int maxRounds;

    private ChatMemory chatMemory;

    public SimpleReactAgent(String name, ChatModel chatModel, List<ToolCallback> tools, String systemPrompt, int maxRounds, ChatMemory chatMemory) {
        this.name = name;
        this.chatModel = chatModel;
        this.tools = tools;
        this.systemPrompt = systemPrompt;
        this.maxRounds = maxRounds;
        this.chatMemory = chatMemory;

        initChatClient();

        if (this.chatClient == null) {
            throw new IllegalStateException("ChatClient 初始化失败！");
        }
    }

    private void initChatClient() {
        try {
            ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                    .toolCallbacks(tools)
                    // 这里关闭工具自动执行
                    .internalToolExecutionEnabled(false)
                    .build();

            ChatClient.Builder builder = ChatClient.builder(chatModel);
            this.chatClient = builder.defaultOptions(toolOptions)
                    .defaultToolCallbacks(tools)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("ChatClient 初始化失败：" + e.getMessage(), e);
        }
    }


    /**
     * 非流式输出
     *
     * @param question
     * @return
     */
    public String call(String question) {
        return callInternal(null, question);
    }

    // 带会话记忆
    public String call(String conversationId, String question) {
        return callInternal(conversationId, question);
    }

    public String callInternal(String conversationId, String question) {
        List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        boolean useMemory = StringUtils.isNotBlank(conversationId) && chatMemory != null;

        messages.add(new SystemMessage(REACT_AGENT_SYSTEM_PROMPT));
        messages.add(new SystemMessage(systemPrompt));

        // 加载历史记忆
        if (useMemory) {
            List<Message> history = chatMemory.get(conversationId);
            if (CollectionUtils.isNotEmpty(history)) {
                messages.addAll(history);
            }
        }

        messages.add(new UserMessage("<question>" + question + "</question>"));

        // 添加记忆
        if (useMemory) {
            chatMemory.add(conversationId, new UserMessage(question));
        }

        int round = 0;

        while (true) {
            round++;
            if (maxRounds > 0 && round > maxRounds) {
                log.warn("=== 达到 maxRounds（{}），强制生成最终答案 ===", maxRounds);
                messages.add(new UserMessage("""
                        你已达到最大推理轮次限制。
                        请基于当前已有的上下文信息，
                        直接给出最终答案。
                        禁止再调用任何工具。
                        如果信息不完整，请合理总结和说明。
                        """));

                String finalText = chatClient.prompt().messages(messages).call().content();
                if (useMemory) {
                    chatMemory.add(conversationId, new AssistantMessage(finalText));
                }
                return finalText;
            }

            ChatClientResponse clientResponse = chatClient.prompt().messages(messages)
                    .call().chatClientResponse();
            String aiText = clientResponse.chatResponse().getResult().getOutput().getText();

            AssistantMessage.Builder builder = AssistantMessage.builder().content(aiText);

            // 没有工具调用，视为最终答案
            if (!clientResponse.chatResponse().hasToolCalls()) {
                return aiText;
            }

            // 有工具调用
            messages.add(builder.toolCalls(clientResponse.chatResponse().getResult().getOutput().getToolCalls()).build());

            clientResponse.chatResponse()
                    .getResult()
                    .getOutput()
                    .getToolCalls()
                    .forEach(toolCall -> {
                        String toolName = toolCall.name();
                        String argsJson = toolCall.arguments();
                        ToolCallback callback = findTool(toolName);
                        if (callback == null) {
                            addErrorToolResponse(messages, toolCall, "工具未找到:" + toolName);
                            return;
                        }

                        Object result;

                        try {
                            result = callback.call(argsJson);
                            ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, result.toString());
                            messages.add(ToolResponseMessage.builder().responses(List.of(response)).build());
                        } catch (Exception e) {
                            addErrorToolResponse(messages, toolCall, "工具调用异常:" + e.getMessage());
                        }
                    });
        }
    }

    private enum RoundMode {
        UNKNOWN,
        FINAL_ANSWER,
        TOOL_CALL,

        ;
    }

    /**
     * 每轮执行的状态标记位
     */
    private static class RoundState {
        RoundMode mode = RoundMode.UNKNOWN;

        boolean firstChunkHandled;

        StringBuilder textBuffer = new StringBuilder();
        List<AssistantMessage.ToolCall> toolCalls = Collections.synchronizedList(new ArrayList<>());
    }


    /**
     * 流式输出
     *
     * @param question
     * @return
     */
    public Flux<String> stream(String question) {
        return streamInternal(null, question);
    }

    /**
     * 流式输出(带会话记忆)
     *
     * @param conversationId
     * @param question
     * @return
     */
    public Flux<String> stream(String conversationId, String question) {
        return streamInternal(conversationId, question);
    }

    private Flux<String> streamInternal(String conversationId, String question) {
        List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        boolean useMemory = StringUtils.isNotBlank(conversationId) && chatMemory != null;

        messages.add(new SystemMessage(REACT_AGENT_SYSTEM_PROMPT));
        messages.add(new SystemMessage(systemPrompt));

        // 加载历史记忆
        if (useMemory) {
            List<Message> history = chatMemory.get(conversationId);
            if (CollectionUtils.isNotEmpty(history)) {
                messages.addAll(history);
            }
        }

        messages.add(new UserMessage("<question>" + question + "</question>"));

        // 添加记忆
        if (useMemory) {
            chatMemory.add(conversationId, new UserMessage(question));
        }

        // 构建流式发射器
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 迭代轮次
        AtomicLong roundCounter = new AtomicLong(0);
        // 是否发送最终结果标记位
        AtomicBoolean hasSentFinalResult = new AtomicBoolean(false);

        // 收集最终答案，存储 memory
        StringBuilder finalAnswerBuffer = new StringBuilder();

        scheduleRound(messages, sink, roundCounter, hasSentFinalResult, finalAnswerBuffer, useMemory, conversationId);

        return sink.asFlux()
                // 收集最终答案
                .doOnNext(finalAnswerBuffer::append)
                .doOnCancel(() -> hasSentFinalResult.set(true))
                .doFinally(signalType -> {
                    log.info("最终答案：{}", finalAnswerBuffer);
                });
    }

    private void scheduleRound(List<Message> messages, Sinks.Many<String> sink, AtomicLong roundCounter, AtomicBoolean hasSentFinalResult, StringBuilder finalAnswerBuffer, boolean useMemory, String conversationId) {
        roundCounter.incrementAndGet();
        RoundState roundState = new RoundState();
        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> processChunk(chunk, sink, roundState))
                .doOnComplete(() -> finishRound(messages, sink, roundState, roundCounter, hasSentFinalResult, finalAnswerBuffer, useMemory, conversationId))
                .doOnError(err -> {
                    if (!hasSentFinalResult.get()) {
                        hasSentFinalResult.set(true);
                        sink.tryEmitError(err);
                    }
                })
                .subscribe();
    }

    /**
     * 一轮流式输出结束后，判断是否发送最终结果还是工具调用
     *
     * @param messages
     * @param sink
     * @param roundState
     * @param roundCounter
     * @param hasSentFinalResult
     * @param finalAnswerBuffer
     * @param useMemory
     * @param conversationId
     */
    private void finishRound(List<Message> messages, Sinks.Many<String> sink, RoundState roundState, AtomicLong roundCounter, AtomicBoolean hasSentFinalResult, StringBuilder finalAnswerBuffer, boolean useMemory, String conversationId) {
        // 如果整轮都没有 tool_call，那么表示就是最终答案
        if (roundState.mode != RoundMode.TOOL_CALL) {
            String finalText = roundState.textBuffer.toString();
            sink.tryEmitComplete();
            hasSentFinalResult.set(true);
            if (useMemory) {
                chatMemory.add(conversationId, new AssistantMessage(finalText));
            }
            return;
        }

        // 如果是工具调用，那么需要将工具调用结果发送给模型
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(roundState.textBuffer.toString())
                .toolCalls(roundState.toolCalls)
                .build();
        messages.add(assistantMessage);

        // 判断是否达到最大轮次
        if (maxRounds > 0 && roundCounter.get() >= maxRounds) {
            log.info("达到最大轮次，结束对话");
            if (!hasSentFinalResult.get()) {
                // 强制输出结果
                forceFinalStream(messages, sink, hasSentFinalResult);
            }
            return;
        }

        // 执行工具并迭代进入下一轮
        executeToolCalls(roundState.toolCalls, messages, hasSentFinalResult, () -> {
            if (!hasSentFinalResult.get()) {
                scheduleRound(messages, sink, roundCounter, hasSentFinalResult, finalAnswerBuffer, useMemory, conversationId);
            }
        });
    }

    private void forceFinalStream(List<Message> messages, Sinks.Many<String> sink, AtomicBoolean hasSentFinalResult) {
        // AssistantMessage包含toolcall，必须后面是ToolResponseMessage，否则会报错400
        messages.add(new UserMessage("""
                你已达到最大推理轮次限制。
                请基于当前已有的上下文信息，
                直接给出最终答案。
                禁止再调用任何工具。
                如果信息不完整，请合理总结和说明。
                """));

        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> {
                    if (chunk == null) {
                        return;
                    }
                    String text = chunk.getResult().getOutput().getText();
                    if (StringUtils.isNotBlank(text) && !hasSentFinalResult.get()) {
                        sink.tryEmitNext(text);
                    }
                }).doOnComplete(() -> {
                    hasSentFinalResult.set(true);
                    sink.tryEmitComplete();
                })
                .doOnError(err -> {
                    hasSentFinalResult.set(true);
                    sink.tryEmitError(err);
                })
                .subscribe();
    }

    private void executeToolCalls(List<AssistantMessage.ToolCall> toolCalls, List<Message> messages, AtomicBoolean hasSentFinalResult, Runnable onComplete) {
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalToolCalls = toolCalls.size();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            // 这里使用 boundedElastic 线程池，避免阻塞主线程
            Schedulers.boundedElastic().schedule(() -> {
                if (hasSentFinalResult.get()) {
                    completeToolCall(completedCount, totalToolCalls, onComplete);
                    return;
                }

                String toolName = toolCall.name();
                String arguments = toolCall.arguments();

                ToolCallback callback = findTool(toolName);
                if (callback == null) {
                    addErrorToolResponse(messages, toolCall, "工具未找到：" + toolName);
                    completeToolCall(completedCount, totalToolCalls, onComplete);
                    return;
                }

                try {
                    String result = callback.call(arguments);
                    ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(
                            toolCall.id(),
                            toolName,
                            result
                    );
                    messages.add(ToolResponseMessage.builder().responses(List.of(toolResponse)).build());
                } catch (Exception e) {
                    addErrorToolResponse(messages, toolCall, "工具执行失败：" + e.getMessage());
                } finally {
                    completeToolCall(completedCount, totalToolCalls, onComplete);
                }
            });
        }
    }

    private void completeToolCall(AtomicInteger completedCount, int totalToolCalls, Runnable onComplete) {
        int current = completedCount.incrementAndGet();
        if (current >= totalToolCalls) {
            onComplete.run();
        }
    }

    private void processChunk(ChatResponse chunk, Sinks.Many<String> sink, RoundState roundState) {
        if (chunk == null) {
            return;
        }
        Generation generation = chunk.getResult();
        String text = generation.getOutput().getText();
        // 虽然是分段输出，但是每一段输出都可以判断是否是 ToolCall（因为 OpenAI 的定义的 FunctionCall 固定格式，每次输出都可以判断是否有 ToolCall）
        List<AssistantMessage.ToolCall> toolCalls = generation.getOutput().getToolCalls();

        // 如果 ToolCall不为空，则表示是工具调用
        if (!toolCalls.isEmpty()) {
            roundState.mode = RoundMode.TOOL_CALL;
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                mergeToolCall(roundState, toolCall);
            }
            return;
        }

        if (StringUtils.isNotBlank(text)) {
            sink.tryEmitNext(text);
            roundState.textBuffer.append(text);
        }
    }

    private void mergeToolCall(RoundState roundState, AssistantMessage.ToolCall toolCall) {
        for (int i = 0; i < roundState.toolCalls.size(); i++) {
            AssistantMessage.ToolCall existing = roundState.toolCalls.get(i);
            if (existing.id().equals(toolCall.id())) {
                String mergedArgs = existing.arguments() + toolCall.arguments();
                roundState.toolCalls.set(i, new AssistantMessage.ToolCall(existing.id(), "function", existing.name(), mergedArgs));
                return;
            }
        }
        roundState.toolCalls.add(toolCall);
    }

    private ToolCallback findTool(String name) {
        return tools.stream()
                .filter(t -> t.getToolDefinition().name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void addErrorToolResponse(List<Message> messages, AssistantMessage.ToolCall toolCall, String errorMsg) {
        ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
                toolCall.id(),
                toolCall.name(),
                "{ \"error\": \"" + errorMsg + "\" }"
        );
        messages.add(
                ToolResponseMessage.builder()
                        .responses(List.of(response))
                        .build()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private ChatModel chatModel;
        private List<ToolCallback> tools;
        private String systemPrompt = "";

        private int maxReflectionRounds;

        private int maxRounds;

        private List<Advisor> advisors;

        private ChatMemory chatMemory;

        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder tools(ToolCallback... tools) {
            this.tools = Arrays.asList(tools);
            return this;
        }

        public Builder tools(List<ToolCallback> tools) {
            this.tools = tools;
            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            this.advisors = advisors;
            return this;
        }

        public Builder advisors(Advisor... advisors) {
            this.advisors = Arrays.asList(advisors);
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder maxReflectionRounds(int maxReflectionRounds) {
            this.maxReflectionRounds = maxReflectionRounds;
            return this;
        }

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public SimpleReactAgent build() {
            if (chatModel == null) {
                throw new IllegalArgumentException("chatModel 不能为空！");
            }
            return new SimpleReactAgent(name, chatModel, tools, systemPrompt, maxRounds, chatMemory);
        }
    }

    public static void main(String[] args) {
        ChatModel chatModel = ChatModelConfig.getChatModel();

        ToolCallback[] toolCallbacks = ToolCallbacks.from(new WeatherService(), new SearchService());

        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();

        SimpleReactAgent agent = SimpleReactAgent.builder()
                .name("simple-agent")
                .chatModel(chatModel)
                .tools(toolCallbacks)
                .chatMemory(chatMemory)
                .maxRounds(5)
                .systemPrompt("你是专业的研究分析助手！")
                .build();

        String question = """
                请你根据北京今天的天气、未来七天的天气趋势、以及上海今天的天气，并搜索北京天气的预警情况，生成一份不少于 600 字的综合分析报告。
                """;

//        System.out.println(agent.call(question));

//        System.out.println(agent.call(question));

        agent.stream(question)
                .doOnNext(chunk -> {
                    System.out.print(chunk);
                })
                .doOnError(error -> System.err.println("\n出错：" + error))
                .doOnComplete(() -> System.out.println("\n\n=== 流式输出全部完成 ==="))
                .blockLast();
    }
}

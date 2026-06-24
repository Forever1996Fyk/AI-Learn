package com.forever1996Fyk.ai.agent.agent.file;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forever1996Fyk.ai.agent.agent.BaseAgent;
import com.forever1996Fyk.ai.agent.agent.websearch.WebSearchReactAgent;
import com.forever1996Fyk.ai.agent.domain.AgentResponse;
import com.forever1996Fyk.ai.agent.domain.SaveQuestionRequest;
import com.forever1996Fyk.ai.agent.domain.UpdateAnswerRequest;
import com.forever1996Fyk.ai.agent.domain.record.AgentState;
import com.forever1996Fyk.ai.agent.domain.record.RoundState;
import com.forever1996Fyk.ai.agent.domain.record.SearchResult;
import com.forever1996Fyk.ai.agent.enums.RoundMode;
import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.prompts.ReactAgentPrompts;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.forever1996Fyk.ai.agent.util.ThinkTagParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/22 23:27
 **/
public class FileReactAgent  extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(WebSearchReactAgent.class);
    private ChatClient chatClient;
    private final List<ToolCallback> tools;
    private final String systemPrompt;
    private final List<Advisor> advisors;
    private int maxRounds;

    @Setter
    private String currentFileId;

    private static final ObjectMapper MAPPER = new ObjectMapper();


    public FileReactAgent(String name, ChatModel chatModel, List<ToolCallback> tools, String systemPrompt, int maxRounds,
                               ChatMemory chatMemory, List<Advisor> advisors, AiSessionService sessionService, AgentTaskManager taskManager) {
        super(name, chatModel, "file");
        this.tools = tools;
        this.systemPrompt = systemPrompt;
        this.advisors = advisors;
        this.sessionService = sessionService;
        this.chatMemory = chatMemory;
        this.maxRounds = maxRounds;
        this.taskManager = taskManager;

        // 初始化工具使用集合
        this.usedTools = Sets.newHashSet();

        initChatClient();

        if (this.chatClient == null) {
            throw new IllegalStateException("ChatClient 初始化失败！");
        }
    }

    private void initChatClient() {
        try {
            ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                    .toolCallbacks(tools)
                    // 关闭工具自动执行
                    .internalToolExecutionEnabled(false)
                    .build();

            ChatClient.Builder builder = ChatClient.builder(chatModel);
            if (CollectionUtils.isNotEmpty(advisors)) {
                builder.defaultAdvisors(advisors);
            }
            this.chatClient = builder.defaultOptions(options).defaultToolCallbacks(tools).build();
        } catch (Exception e) {
            throw new RuntimeException("ChatClient 初始化失败：" + e.getMessage(), e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    @Override
    public Flux<String> execute(String conversationId, String question) {
        return streamInternal(conversationId, question);
    }


    /**
     * 流式输出（带文件ID）
     */
    public Flux<String> stream(String conversationId, String question, String fileId) {
        setCurrentFileId(fileId);
        return streamInternal(conversationId, question);
    }


    public Flux<String> stream(String conversationId, String question) {
        return streamInternal(conversationId, question);
    }

    private Flux<String> streamInternal(String conversationId, String question) {
        List<Message> messages = Collections.synchronizedList(Lists.newArrayList());
        boolean useMemory = StringUtils.isNotBlank(conversationId) && Objects.nonNull(chatMemory);

        // 检查是否已有任务再执行
        // 因为在一般问答聊天智能体中，一个会话只能存在一个正在执行的任务，不管是豆包还是通义都是一样
        // 如果存在正在执行的任务则返回：该会话正在执行中，请稍后再试
        Flux<String> checkResult = checkRunningTask(conversationId);
        if (checkResult != null) {
            return checkResult;
        }

        // 初始化计时器
        initTimers();

        // 构造一个流式发射器，用于向前端流式的输出内容
        // many()表示可以添加多个元素，
        // onBackpressureBuffer()表示如果无法立即处理，则将元素缓存起来，等待处理
        // unicast() 表示创建一个无界流，即元素数量没有限制，可以无限添加元素
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 注册任务到任务管理器
        AgentTaskManager.TaskInfo taskInfo = registerTask(conversationId, sink);
        if (taskInfo == null && StringUtils.isNotBlank(conversationId) && taskManager != null) {
            return Flux.error(new IllegalStateException("该会话正在执行中，请稍后再试"));
        }

        // 加载 System Prompt
        // 必须保证系统提示词放在message列表的最开始位置
        messages.add(new SystemMessage(ReactAgentPrompts.getWebSearchPrompt()));
        if (StringUtils.isNotBlank(systemPrompt)) {
            // 如果用户设置了系统提示词，则把用户设置的系统提示词，添加到message列表中
            messages.add(new SystemMessage(systemPrompt));
        }

        // 加载历史记忆
        loadChatHistory(conversationId, messages, true, true);

        messages.add(new UserMessage("<question>" + question + "</question>"));
        messages.add(new UserMessage("<fileid>" + currentFileId + "</fileid>"));
        currentQuestion = question;

        // 添加记忆并保存到数据库
        if (sessionService != null) {
            AiSessionEntity savedSession = sessionService.saveQuestion(
                    SaveQuestionRequest.builder()
                            .sessionId(conversationId)
                            .question(question)
                            .fileid(currentFileId)
                            .build()
            );
            currentSessionId = savedSession.getId();
        }

        // ======开始 ReAct Agent流程 ======
        // 设置迭代次数
        AtomicInteger roundCounter = new AtomicInteger(0);
        // 是否发送最终结果标记位
        AtomicBoolean hasSentFinalResult = new AtomicBoolean(false);

        hasSentFinalResult.set(false);
        roundCounter.set(0);

        // 收集最终答案（纯文本）, 存储memory
        StringBuilder finalAnswerBuffer = new StringBuilder();
        // 收集思考过程
        StringBuilder thinkingBuffer = new StringBuilder();

        // 当前ReAct Agent的state, 这里主要保存当前搜索的结果，包括搜索连接，标题和内容
        AgentState agentState = new AgentState();

        // 这里与之前的SimpleReactAgent的逻辑一样，也就是reasoning-action-observation的过程
        // 流程就是对大模型的输出进行判断，是否存在ToolCall，如果存在则执行tool_call, 再进行observation，也就是将结果返回给大模型进行判断是否满足用户的问题，如果不满足，则继续reasoning, 如此循环，直到满足用户问题或者达到最大轮次数
        scheduleRound(messages, sink, roundCounter, hasSentFinalResult, finalAnswerBuffer, useMemory, conversationId, agentState, thinkingBuffer);

        return sink.asFlux()
                .doOnNext(chunk -> {
                    // 记录首次响应时间
                    recordFirstResponse();
                    try {
                        AgentResponse response = JSON.parseObject(chunk, AgentResponse.class);
                        if ("text".equals(response.getType())) {
                            finalAnswerBuffer.append(response.getContent());
                        } else if ("thinking".equals(response.getType())) {
                            thinkingBuffer.append(response.getContent());
                        }
                    } catch (Exception e) {
                        // 解析失败，直接拼接
                        finalAnswerBuffer.append(chunk);
                    }
                })
                .doOnCancel(() -> {
                    hasSentFinalResult.set(true);
                    // 关闭当前任务
                    if (taskManager != null) {
                        taskManager.stopTask(conversationId);
                    }
                })
                .doFinally(signalType -> {
                    log.info("最终答案: {}", finalAnswerBuffer);
                    log.info("思考过程: {}", thinkingBuffer);
                    // 保存结果到会话
                    saveSessionResult(conversationId, finalAnswerBuffer, thinkingBuffer, agentState);

                    // 流结束时移除任务
                    if (taskManager != null) {
                        taskManager.stopTask(conversationId);
                    }
                });
    }

    private void saveSessionResult(String conversationId, StringBuilder finalAnswerBuffer, StringBuilder thinkingBuffer, AgentState agentState) {
        if (sessionService != null && currentSessionId != null && !finalAnswerBuffer.isEmpty()) {
            long totalResponseTime = getTotalResponseTime();
            String toolsStr = getUsedToolsString();
            String referenceJson = "";
            if (CollectionUtils.isNotEmpty(agentState.getSearchResults())) {
                // 如果当前用到了search的工具，则解析search结果
                referenceJson = createReferenceResponse(JSON.toJSONString(agentState.getSearchResults()));
            }
            UpdateAnswerRequest request = UpdateAnswerRequest.builder()
                    .id(currentSessionId)
                    .answer(finalAnswerBuffer.toString())
                    .thinking(thinkingBuffer.toString())
                    .tools(toolsStr)
                    .reference(referenceJson)
                    .firstResponseTime(firstResponseTime)
                    .totalResponseTime(totalResponseTime)
                    .build();
            sessionService.updateAnswer(request);
            log.info("结果已保存到会话: sessionId={}", conversationId);
        }
    }

    private void scheduleRound(List<Message> messages, Sinks.Many<String> sink, AtomicInteger roundCounter, AtomicBoolean hasSentFinalResult, StringBuilder finalAnswerBuffer, boolean useMemory, String conversationId, AgentState agentState, StringBuilder thinkingBuffer) {
        // ReAct 轮次 +1
        roundCounter.incrementAndGet();
        RoundState state = new RoundState();

        Disposable disposable = chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> processChunk(chunk, sink, state))
                .doOnComplete(() -> finishRound(messages, sink, state, roundCounter, hasSentFinalResult, finalAnswerBuffer, useMemory, conversationId, agentState, thinkingBuffer))
                .doOnError(err -> {
                    if (!hasSentFinalResult.get()) {
                        hasSentFinalResult.set(true);
                        sink.tryEmitError(err);
                    }
                }).subscribe();

        // 保存Disposable到任务管理器
        if (StringUtils.isNotBlank(conversationId) && taskManager != null) {
            taskManager.setDisposable(conversationId, disposable);
        }
    }

    /**
     * 当前轮次结束后处理工具调用
     */
    private void finishRound(List<Message> messages, Sinks.Many<String> sink, RoundState state, AtomicInteger roundCounter, AtomicBoolean hasSentFinalResult, StringBuilder finalAnswerBuffer, boolean useMemory, String conversationId, AgentState agentState, StringBuilder thinkingBuffer) {
        // 如果整轮都没有tool_call，那就是最终答案了
        if (state.getMode() != RoundMode.TOOL_CALL) {
            String referenceJson;
            String finalText = state.textBuffer.toString();
            // 输出参考链接
            if (CollectionUtils.isNotEmpty(agentState.getSearchResults())) {
                String reference = JSON.toJSONString(agentState.getSearchResults());
                referenceJson = createReferenceResponse(reference);
                sink.tryEmitNext(referenceJson);
            }

            // 输出推荐问题
            if (isEnableRecommendations()) {
                String recommendations = generateRecommendations(conversationId, currentQuestion, finalText);
                if (StringUtils.isNotBlank(recommendations)) {
                    // 用于保存数据库存储
                    currentRecommendations = recommendations;
                    String recommendJson = createRecommendResponse(recommendations);
                    sink.tryEmitNext(recommendJson);
                }
            }

            sink.tryEmitComplete();
            hasSentFinalResult.set(true);
            return;
        }

        AssistantMessage assistantMessage = AssistantMessage.builder().toolCalls(state.toolCalls).build();
        messages.add(assistantMessage);

        if (maxRounds > 0 && roundCounter.get() >= maxRounds) {
            // 当前ReAct轮次已达到最大次数
            forceFinalStream(messages, sink, hasSentFinalResult, state, conversationId, useMemory, agentState, thinkingBuffer);
            return;
        }

        // 执行工具调用
        executeToolCalls(sink, state, messages, hasSentFinalResult, agentState, () -> {
            if (!hasSentFinalResult.get()) {
                scheduleRound(messages, sink, roundCounter,
                        hasSentFinalResult, finalAnswerBuffer,
                        useMemory, conversationId, agentState, thinkingBuffer);
            }
        });
    }

    /**
     * 生成推荐问题
     *
     * @param conversationId  会话ID
     * @param currentQuestion 当前问题
     * @param currentAnswer   当前答案
     * @return 推荐问题JSON字符串，失败返回null
     */
    private String generateRecommendations(String conversationId, String currentQuestion, String currentAnswer) {
        if (!isEnableRecommendations()) {
            return null;
        }

        try {
            List<Message> messages = Lists.newArrayList();
            // 添加系统提示词
            messages.add(new SystemMessage(ReactAgentPrompts.getRecommendPrompt()));

            // 添加历史消息
            loadChatHistory(conversationId, messages, true, true);

            // 添加当前会话消息（最新的消息,放在最后）
            messages.add(new UserMessage("当前会话："));
            messages.add(new UserMessage(currentQuestion));
            if (StringUtils.isNotBlank(currentAnswer)) {
                messages.add(new AssistantMessage(currentAnswer));
            }

            // 添加格式化输出转换器
            BeanOutputConverter<List<String>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
            });

            // 添加格式说明消息
            messages.add(new UserMessage("请根据上述对话生成3个推荐问题。输出格式为：\n" + converter.getFormat()));

            // 调用模型生成推荐问题
            String response = ChatClient.builder(chatModel).build()
                    .prompt()
                    .messages(messages)
                    .call().content();

            if (StringUtils.isNotBlank(response)) {
                List<String> recommendations = converter.convert(response);
                if (CollectionUtils.isNotEmpty(recommendations)) {
                    String jsonStr = JSON.toJSONString(recommendations);
                    log.info("生成推荐问题成功: {}", jsonStr);
                    return jsonStr;
                }
            }
            log.warn("生成推荐问题失败，响应格式无效: {}", response);
            return null;
        } catch (Exception e) {
            log.error("生成推荐问题异常", e);
            return null;
        }
    }

    private void forceFinalStream(List<Message> messages, Sinks.Many<String> sink, AtomicBoolean hasSentFinalResult, RoundState state, String conversationId, boolean useMemory, AgentState agentState, StringBuilder thinkingBuffer) {
        // 创建新的消息列表，确保系统提示词在最前面
        List<Message> newMessages = Lists.newArrayList();
        // 添加系统提示词
        newMessages.add(new SystemMessage(ReactAgentPrompts.getWebSearchPrompt()));
        if (StringUtils.isNotBlank(systemPrompt)) {
            newMessages.add(new SystemMessage(systemPrompt));
        }

        // 添加原有消息（跳过系统消息）
        for (Message message : messages) {
            if (!(message instanceof SystemMessage)) {
                newMessages.add(message);
            }
        }

        // 添加限制提示
        newMessages.add(new UserMessage("""
                你已达到最大推理轮次限制。
                请基于当前已有的上下文信息，
                直接给出最终答案。
                禁止再调用任何工具。
                如果信息不完整，请合理总结和说明。
                """));

        // 替换原消息列表
        messages.clear();
        messages.addAll(newMessages);

        // 收集最终文本
        StringBuilder finalTextBuffer = new StringBuilder();

        Disposable disposable = chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> {
                    if (chunk == null || chunk.getResult() == null || chunk.getResult().getOutput() == null) {
                        return;
                    }

                    String text = chunk.getResult().getOutput()
                            .getText();
                    if (StringUtils.isNotBlank(text) && !hasSentFinalResult.get()) {
                        sink.tryEmitNext(createTextResponse(text));
                        finalTextBuffer.append(text);
                    }
                })
                .doOnComplete(() -> {
                    // 输出参考链接 todo

                    // 输出推荐问题 todo

                    hasSentFinalResult.set(true);
                    sink.tryEmitComplete();
                })
                .doOnError(err -> {
                    hasSentFinalResult.set(true);
                    sink.tryEmitError(err);
                }).subscribe();

        // 保存Disposable到任务管理器
        if (StringUtils.isNotBlank(conversationId) && taskManager != null) {
            taskManager.setDisposable(conversationId, disposable);
        }
    }

    private void executeToolCalls(Sinks.Many<String> sink, RoundState state, List<Message> messages, AtomicBoolean hasSentFinalResult, AgentState agentState, Runnable onComplete) {
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalToolCalls = state.toolCalls.size();

        // 保证顺序一致性
        Map<String, ToolResponseMessage.ToolResponse> responseMap = Maps.newConcurrentMap();
        for (AssistantMessage.ToolCall toolCall : state.toolCalls) {
            // 多工具并行调用
            Schedulers.boundedElastic().schedule(() -> {
                if (hasSentFinalResult.get()) {
                    completeToolCall(completedCount, totalToolCalls, responseMap, state.toolCalls, messages, onComplete);
                    return;
                }

                String toolName = toolCall.name();
                String arguments = toolCall.arguments();
                ToolCallback callback = findTool(toolName);
                if (callback == null) {
                    // 工具未找到，返回错误消息
                    responseMap.put(toolCall.id(), new ToolResponseMessage.ToolResponse(
                            toolCall.id(), toolName, "{ \"error\": \"工具未找到：" + toolName + "\" }"
                    ));
                    completeToolCall(completedCount, totalToolCalls, responseMap, state.toolCalls, messages, onComplete);
                    return;
                }

                if (toolName.contains("loadContent")) {
                    JSONObject args = JSON.parseObject(arguments);
                    String question = (String) args.get("question");
                    // 发送 thinking 消息，表示正在加载文件内容
                    String loadThink = "📂 正在检索文件内容，请稍等...";
                    sink.tryEmitNext(createThinkingResponse(loadThink));
                }

                try {
                    String result = callback.call(arguments);
                    // 记录使用的工具
                    recordUsedTool(toolName);

                    // 表示使用了tavily搜索工具
                    if (toolName.contains("tavily")) {
                        parseSearchResult(result, agentState);
                    }

                    // 将结果放入responseMap, key为toolCall.id()
                    responseMap.put(toolCall.id(), new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, arguments));
                } catch (Exception e) {
                    // 执行工具失败时，也放入responseMap
                    responseMap.put(toolCall.id(), new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, "{ \"error\": \"工具执行失败: " + e.getMessage() + "\" }"));
                } finally {
                    completeToolCall(completedCount, totalToolCalls, responseMap, state.toolCalls, messages, onComplete);
                }
            });
        }
    }

    private void parseSearchResult(String result, AgentState agentState) {
        try {
            JsonNode root = MAPPER.readTree(result);
            if (!root.isArray() || root.isEmpty()) {
                return;
            }
            JsonNode first = root.get(0);
            JsonNode textNode = first.get("text");
            if (textNode == null || textNode.isNull()) {
                return;
            }
            JsonNode textJson;
            if (textNode.isTextual()) {
                textJson = MAPPER.readTree(textNode.asText());
            } else {
                textJson = textNode;
            }

            JsonNode results = textJson.get("results");
            if (results == null || !results.isArray()) {
                return;
            }

            for (JsonNode item : results) {
                String url = getSafe(item, "url");
                String title = getSafe(item, "title");
                String content = getSafe(item, "content");

                if (url != null && !url.isBlank()) {
                    agentState.getSearchResults().add(new SearchResult(url, title, content));
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSafe(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private void completeToolCall(AtomicInteger completedCount, int totalToolCalls, Map<String, ToolResponseMessage.ToolResponse> responseMap, List<AssistantMessage.ToolCall> toolCalls, List<Message> messages, Runnable onComplete) {
        int current = completedCount.incrementAndGet();
        if (current >= totalToolCalls) {
            // 按原始toolCalls的顺序重组结果
            List<ToolResponseMessage.ToolResponse> sortedResponses = Lists.newArrayList();
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                ToolResponseMessage.ToolResponse response = responseMap.get(toolCall.id());
                if (response != null) {
                    sortedResponses.add(response);
                } else {
                    // 如果某个工具调用没有响应，添加一个错误响应
                    sortedResponses.add(new ToolResponseMessage.ToolResponse(
                            toolCall.id(), toolCall.name(), "{ \"error\": \"工具响应丢失\" }"));
                }
            }

            // 一次性添加所有工具响应
            messages.add(ToolResponseMessage.builder()
                    .responses(sortedResponses)
                    .build()
            );
            onComplete.run();
        }
    }

    private ToolCallback findTool(String toolName) {
        return tools.stream()
                .filter(t -> t.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElse(null);
    }

    private void processChunk(ChatResponse chunk, Sinks.Many<String> sink, RoundState state) {
        if (chunk == null || chunk.getResult() == null || chunk.getResult().getOutput() == null) {
            return;
        }

        Generation gen = chunk.getResult();
        String text = gen.getOutput().getText();
        List<AssistantMessage.ToolCall> toolCalls = gen.getOutput().getToolCalls();

        // 如果toolCalls不为空，则表示当前需要工具调用
        if (CollectionUtils.isNotEmpty(toolCalls)) {
            state.mode = RoundMode.TOOL_CALL;

            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                // 合并toolCall消息，因为有的模型可能存在把toolCall的数据消息流式分段传输
                mergeToolCall(state, toolCall);
            }
            return;
        }

        if (StringUtils.isNotBlank(text)) {
            ThinkTagParser.ParseResult parseResult = ThinkTagParser.parse(text, state.inThink);
            state.inThink = parseResult.inThink();
            for (ThinkTagParser.Segment segment : parseResult.segments()) {
                if (segment.thinking()) {
                    sink.tryEmitNext(createThinkingResponse(segment.content()));
                } else {
                    sink.tryEmitNext(createTextResponse(segment.content()));
                    state.textBuffer.append(segment.content());
                }
            }
        }
    }

    private void mergeToolCall(RoundState state, AssistantMessage.ToolCall toolCall) {
        for (int i = 0; i < state.toolCalls.size(); i++) {
            AssistantMessage.ToolCall existing = state.toolCalls.get(i);
            if (existing.id().equals(toolCall.id())) {
                String mergedArgs = existing.arguments() + toolCall.arguments();
                state.toolCalls.set(i, new AssistantMessage.ToolCall(existing.id(), "function", existing.name(), mergedArgs));
                return;
            }
        }

        // 新加入的toolCall
        state.toolCalls.add(toolCall);
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
        private AiSessionService sessionService;
        private AgentTaskManager taskManager;

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


        public Builder sessionService(AiSessionService aiSessionService) {
            this.sessionService = aiSessionService;
            return this;
        }

        public Builder taskManager(AgentTaskManager taskManager) {
            this.taskManager = taskManager;
            return this;
        }

        public FileReactAgent build() {
            if (chatModel == null) {
                throw new IllegalArgumentException("chatModel 不能为空！");
            }
            return new FileReactAgent(name, chatModel, tools, systemPrompt, maxRounds, chatMemory, advisors, sessionService, taskManager);
        }
    }
}

package com.forever1996Fyk.ai.agent.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.forever1996Fyk.ai.agent.config.ChatModelConfig;
import com.forever1996Fyk.ai.agent.prompts.PlanExecutePromptsFactory;
import com.forever1996Fyk.ai.agent.tools.SearchService;
import com.forever1996Fyk.ai.agent.tools.WeatherService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/12 23:55
 **/
public class PlanExecuteAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanExecuteAgent.class);
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
            return new PlanExecuteAgent(chatModel, tools, maxRounds, contextCharLimit, maxToolRetries, planExecutePromptsFactory, chatMemory);
        }
    }

    public String call(String question) {
        return callInternal(null, question);
    }

    public String call(String conversationId, String question) {
        return callInternal(conversationId, question);
    }

    public String callInternal(String conversationId, String question) {
        boolean useMemory = StringUtils.isNotBlank(conversationId) && chatMemory != null;

        // 全局状态
        OverAllState state = new OverAllState(conversationId, question);

        if (useMemory) {
            List<Message> history = chatMemory.get(conversationId);
            if (CollectionUtils.isNotEmpty(history)) {
                history.forEach(state::addMessage);
            }
        }

        // 当前用户问题
        state.addMessage(new UserMessage(question));

        if (useMemory) {
            chatMemory.add(conversationId, new UserMessage(question));
        }

        while (maxRounds <= 0 || state.getRound() < maxRounds) {
            state.nextRound();
            log.info("===== Plan-Execute Round {} =====", state.getRound());

            // 1. 生成计划
            List<PlanTask> plan = generatePlan(state);
            log.info("【Execution Plan】\n\n" + plan);
            state.addMessage(new AssistantMessage("【Execution Plan】\n" + plan));

            if (CollectionUtils.isEmpty(plan) || plan.stream().allMatch(t -> t.id() == null)) {
                log.info("===== No execution needed, direct answer =====");
                break;
            }

            // 2. 执行计划
            Map<String, TaskResult> results = executePlan(plan, state);

            // 3. 批判（也就是反思机制）
            CritiqueResult critique = critique(state);

            if (critique.passed()) {
                log.info("===== Goal satisfied, finish =====");
                break;
            }

            log.info("===== critique Goal not satisfied, continue round =====,\n reason is {} ", critique.feedback);
            state.addMessage(new AssistantMessage("""
                    【Critique Feedback】
                    %s
                    """.formatted(critique.feedback())));

            // 4. 压缩context
            compressIfNeeded(state);
        }

        if (state.round == maxRounds) {
            log.info("===== Max rounds reached, force finish =====");
        }

        // 总结输出
        return summarize(state);
    }

    private String summarize(OverAllState state) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(PlanExecutePromptsFactory.buildPrompts(planExecutePromptsFactory).getSummarizePrompt()),
                new UserMessage("""
                        【用户原始问题】
                        %s
                        
                        【执行上下文（含工具结果）】
                        %s
                        """.formatted(
                        state.getQuestion(),
                        renderMessages(state.getMessages())
                ))
        ));

        String answer = chatModel.call(prompt).getResult().getOutput().getText();
        // 追加记忆
        if (state.conversationId != null && chatMemory != null) {
            chatMemory.add(state.conversationId, new AssistantMessage(answer));
        }
        return answer;
    }

    private void compressIfNeeded(OverAllState state) {

        // 如果当前上下文信息的总字符数达到阈值，那么就开启压缩
        if (state.currentChars() < contextCharLimit) {
            return;
        }

        log.warn("===== Context too large, compressing ,size is {} =====", state.currentChars());


        // 而压缩的本质其实也就是交给大模型，把当前的所有上下文信息，压缩到规定的字数限制
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("""                             
                             ## 最大压缩限制（必须遵守）
                             - 你输出的最终内容【总字符数（包含所有标签、空格、换行）】
                                不得超过：%s
                             - 这是硬性上限，不是建议
                             - 如超过该限制，视为压缩失败
                        
                        """.formatted(contextCharLimit) + PlanExecutePromptsFactory.buildPrompts(planExecutePromptsFactory).getCompressPrompt()),

                new UserMessage(renderMessages(state.getMessages()))
        ));

        String snapshot = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        state.clearMessages();
        state.addMessage(new SystemMessage("【Compressed Agent State】\n" + snapshot));
        log.warn("===== Context compress has completed, size is {} =====", state.currentChars());
    }

    private CritiqueResult critique(OverAllState state) {
        BeanOutputConverter<CritiqueResult> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
        });

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(PlanExecutePromptsFactory.buildPrompts(planExecutePromptsFactory).getCritiquePrompt()),
                new UserMessage(renderMessages(state.getMessages()))
        ));
        String raw = chatModel.call(prompt).getResult().getOutput().getText();

        return converter.convert(raw);
    }

    private Map<String, TaskResult> executePlan(List<PlanTask> plan, OverAllState state) {
        Map<String, TaskResult> results = new ConcurrentHashMap<>();

        Map<Integer, List<PlanTask>> groupedTask = plan.stream().collect(Collectors.groupingBy(PlanTask::order));

        // 保存每个工具的运行结果，因为可能存在工具依赖，所以需要保存工具结果
        Map<String, String> accumulatedResults = new ConcurrentHashMap<>();

        // 相同的order并发执行，不同的order按顺序串行
        for (Integer order : groupedTask.keySet()) {
            // 保存工具执行快照
            String dependencySnapshot = renderDependencySnapshot(accumulatedResults);
            List<PlanTask> tasks = groupedTask.get(order);

            List<CompletableFuture<Void>> futures = tasks.stream()
                    .map(task -> CompletableFuture.runAsync(() -> {
                        try {
                            // 控制并发, 获取许可证
                            toolSemaphore.acquire();

                            if (task == null || StringUtils.isBlank(task.id())) {
                                return;
                            }
                            // 执行工具
                            TaskResult taskResult = executeWithRetry(task, dependencySnapshot);
                            results.put(task.id(), taskResult);

                            if (taskResult.success() && StringUtils.isNotBlank(taskResult.output())) {
                                accumulatedResults.put(task.id(), taskResult.output());
                            }
                            state.addMessage(new AssistantMessage("""
                                    【Completed Task Result】
                                    taskId: %s
                                    success: %s
                                    result:
                                    %s
                                    error:
                                    %s
                                    【End Task Result】
                                    """.formatted(
                                    task.id(),
                                    taskResult.success(),
                                    taskResult.output(),
                                    taskResult.error())
                            ));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();

                            results.put(task.id(), new TaskResult(
                                    task.id(),
                                    false,
                                    null,
                                    "Task execution interrupted"
                            ));
                        } finally {
                            // 释放许可
                            toolSemaphore.release();
                        }
                    })).toList();

            // 等待当前order组全部完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();
        }

        return results;
    }

    private TaskResult executeWithRetry(PlanTask task, String dependencySnapshot) {
        int attempt = 0;
        Throwable lastError = null;
        while (attempt < maxToolRetries) {
            attempt++;
            try {
                SimpleReactAgent agent = SimpleReactAgent.builder()
                        .chatModel(chatModel)
                        .tools(tools)
                        .maxRounds(5)
                        .systemPrompt(PlanExecutePromptsFactory.buildPrompts(planExecutePromptsFactory).getExecutePrompt())
                        .build();

                String result = agent.call("""
                        【Available Results】
                        %s
                                                
                        【Current Task】
                        %s
                        """.formatted(
                        dependencySnapshot.isBlank() ? "NONE" : dependencySnapshot,
                        task.instruction
                ));
                return new TaskResult(task.id(), true, result, null);
            } catch (Exception e) {
                lastError = e;
                log.warn("Task {} failed attempt {}/{}", task.id(), attempt, maxToolRetries, e);
            }
        }
        return new TaskResult(
                task.id(),
                false,
                null,
                lastError == null ? "unknown error" : lastError.getMessage()
        );
    }

    private List<PlanTask> generatePlan(OverAllState state) {

        // 获取所有工具描述
        // 这里只需要获取工具的名称和描述即可，不需要具体的参数，因为提出计划阶段只需要告诉大模型我有哪些工具，然后让大模型生成对应的调用工具的计划即可
        String toolDesc = renderToolDescriptions();
        BeanOutputConverter<List<PlanTask>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
        });

        // 生成计划的系统提示词
        SystemMessage systemMessage = new SystemMessage(
                String.format("""
                                    当前时间是：%s。
                                                        
                                    当前是迭代的第 %s 轮次。
                                                        
                                    ## 可用工具说明（仅用于规划参考）
                                    %s
                                                        
                                    ## 输出format
                                    %s
                                    
                                    %s
                                """, LocalDateTime.now(ZoneId.of("Asia/Shanghai")),
                        state.round,
                        toolDesc,
                        converter.getFormat(),
                        PlanExecutePromptsFactory.buildPrompts(planExecutePromptsFactory).getPlanPrompt())
        );

        // 生成计划的用户提示词
        UserMessage userMessage = new UserMessage("【对话历史】\n\n" + renderMessages(state.getMessages()));
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        String json = chatModel.call(prompt).getResult().getOutput().getText();
        return converter.convert(json);
    }

    private String renderToolDescriptions() {
        if (tools == null || tools.isEmpty()) {
            return "（当前无可用工具）";
        }

        StringBuilder sb = new StringBuilder();
        for (ToolCallback tool : tools) {
            sb.append("- ")
                    .append(tool.getToolDefinition().name())
                    .append(": ")
                    .append(tool.getToolDefinition().description())
                    .append("\n");
        }
        return sb.toString();
    }

    private String renderMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            sb.append("\n\n[").append(m.getMessageType()).append("]\n\n")
                    .append(m.getText());
        }
        return sb.toString();
    }

    private String renderDependencySnapshot(Map<String, String> results) {

        if (results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        results.forEach((taskId, output) -> {
            sb.append("- taskId: ")
                    .append(taskId)
                    .append("\n")
                    .append("  output:\n")
                    .append(output)
                    .append("\n\n");
        });

        return sb.toString();
    }

    /**
     * 计划任务
     *
     * @param id
     * @param instruction
     * @param order
     */
    public record PlanTask(String id, String instruction, int order) {
    }

    /**
     * 任务结果
     *
     * @param taskId
     * @param success
     * @param output
     * @param error
     */
    public record TaskResult(
            String taskId,
            boolean success,
            String output,
            String error
    ) {
    }

    /**
     * 批判(反思)结果
     *
     * @param passed
     * @param feedback
     */
    public record CritiqueResult(boolean passed, String feedback) {
    }


    public static class OverAllState {
        private final String conversationId;
        private final String question;
        private final List<Message> messages = new ArrayList<>();
        private int round = 0;

        public OverAllState(String conversationId, String question) {
            this.conversationId = conversationId;
            this.question = question;
        }

        public void nextRound() {
            round++;
        }

        public void addMessage(Message message) {
            messages.add(message);
        }

        public String getConversationId() {
            return conversationId;
        }

        public String getQuestion() {
            return question;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public int getRound() {
            return round;
        }

        public void setRound(int round) {
            this.round = round;
        }

        /**
         * 当前上下文信息的字符串总数
         * @return
         */
        public int currentChars() {
            return messages.stream()
                    .mapToInt(msg -> msg.getText() == null ? 0 : msg.getText().length())
                    .sum();
        }

        public void clearMessages() {
            messages.clear();
        }
    }

    public static void main(String[] args) {
        ChatModel chatModel = ChatModelConfig.getChatModel();

        ToolCallback[] toolCallbacks = ToolCallbacks.from(new WeatherService(), new SearchService());

        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();

        PlanExecuteAgent agent = PlanExecuteAgent.builder()
                .chatModel(chatModel)
                .tools(toolCallbacks)
                .maxRounds(3)
                .maxToolRetries(2)
                .chatMemory(chatMemory)
                .contextCharLimit(1000).build();

        String result = agent.call("""
                请你先查询北京今天的天气，再搜索本周末北京天气的预警情况，并基于本周末北京的天气预警情况，搜索北京本周末适合旅游打卡的景点有哪些，最终生成一份不少于 500 字的综合天气分析报告。
                """);

        System.out.println("\n===== FINAL ANSWER =====");
        System.out.println(result);
    }
}

package com.forever1996Fyk.ai.agent.agent.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forever1996Fyk.ai.agent.advisor.HITLAdvisor;
import com.forever1996Fyk.ai.agent.config.ChatModelConfig;
import com.forever1996Fyk.ai.agent.tools.SearchService;
import com.forever1996Fyk.ai.agent.tools.WeatherService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/14 22:52
 **/
public class HITLReactAgent {
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
                        
            ## 反思机制
            如果在反思过程中，助手判断当前回答未能完全满足用户问题，或者达到最大反思轮次，你必须遵循以下规则：
            1. 尽最大可能利用当前已有的信息给出完整回答，即使信息不完全，也要合理推断或总结现有数据。
            2. 如果某些关键信息缺失，可在答案中用合理措辞提示用户，如“根据现有信息判断…”或“可进一步确认…”。
            3. 最终输出必须尽量满足用户需求，保证逻辑清晰、结论可靠、表达完整，即便未能完美覆盖所有反思反馈。
            """;

    private final String name;
    private final ChatModel chatModel;
    private final List<ToolCallback> tools;
    private ChatClient chatClient;
    private final List<Advisor> advisors;

    private ObjectMapper objectMapper = new ObjectMapper();

    private int maxRounds;

    public HITLReactAgent(String name, ChatModel chatModel, List<ToolCallback> tools, List<Advisor> advisors, int maxRounds) {
        this.name = name;
        this.chatModel = chatModel;
        this.tools = tools;
        this.advisors = advisors;
        this.maxRounds = maxRounds;

        initChatClient();

        if (this.chatClient == null) {
            throw new IllegalStateException("ChatClient 初始化失败！");
        }
    }

    private void initChatClient() {
        try {
            ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                    .toolCallbacks(tools)
                    .internalToolExecutionEnabled(false)
                    .build();

            this.chatClient = ChatClient.builder(chatModel)
                    .defaultOptions(toolOptions)
                    .defaultAdvisors(advisors == null ? new ArrayList() : advisors)
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
    public AgentResult call(String question) {

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(REACT_AGENT_SYSTEM_PROMPT));
        messages.add(new UserMessage(question));

        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(HITLAdvisor.HITL_STATE_KEY, new HITLState());

        return run(messages, context);
    }

    private AgentResult run(List<Message> messages, Map<String, Object> context) {
        int round = 0;
        while (true) {
            round++;
            if (maxRounds > 0 && round > maxRounds) {
                String content = chatClient.prompt().messages(messages).call().content();
                return new AgentFinished(content);
            }

            ChatClientResponse response = chatClient.prompt()
                    .messages(messages)
                    .advisors(spec -> context.forEach(spec::param))
                    .call()
                    .chatClientResponse();

            // 判断HITL_REQUIRED是否为true，说明需要人工介入，返回中断元数据
            if (Boolean.TRUE.equals(response.context().get(HITLAdvisor.HITL_REQUIRED))) {
                // 先执行不需要HITL的工具调用，避免等待人工审批
                List<AssistantMessage.ToolCall> nonInterrupted = (List<AssistantMessage.ToolCall>) response.context().get(HITLAdvisor.HITL_NON_INTERCEPT_TOOLS);
                if (CollectionUtils.isNotEmpty(nonInterrupted)) {
                    messages.add(
                            AssistantMessage.builder()
                                    .toolCalls(response.chatResponse().getResult().getOutput().getToolCalls())
                                    .build()
                    );

                    // 执行无需中断的工具，把结果加入messages
                    for (AssistantMessage.ToolCall toolCall : nonInterrupted) {
                        ToolCallback tool = findTool(toolCall.name());
                        String result = tool.call(toolCall.arguments());
                        messages.add(
                                ToolResponseMessage.builder()
                                        .responses(
                                                List.of(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result))
                                        ).build()
                        );
                    }
                }
                // 返回中断元数据
                return new AgentInterrupted(
                        (List<PendingToolCall>) response.context().get(HITLAdvisor.HITL_PENDING_TOOLS),
                        List.copyOf(messages),
                        context
                );
            }

            if (!response.chatResponse().hasToolCalls()) {
                return new AgentFinished(response.chatResponse().getResult().getOutput().getText());
            }

            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .toolCalls(
                            response.chatResponse()
                                    .getResult()
                                    .getOutput()
                                    .getToolCalls()
                    ).build();

            messages.add(assistantMessage);

            for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
                ToolCallback tool = findTool(tc.name());
                String result = tool.call(tc.arguments());

                messages.add(ToolResponseMessage.builder().responses(
                        List.of(new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), result))).build());
            }
        }
    }

    public AgentResult resume(AgentInterrupted interrupted, List<PendingToolCall> feedbacks) {
        List<Message> messages = new ArrayList<>(interrupted.checkpointMessages());
        Map<String, Object> context = interrupted.context();

        HITLState hitlState = (HITLState) context.get(HITLAdvisor.HITL_STATE_KEY);

        // 检查是否存在无需中断的工具已执行（通过判断最后一个消息是否是ToolResponseMessage）
        boolean hasNonInterruptedTool = !messages.isEmpty() &&
                messages.getLast() instanceof ToolResponseMessage;

        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

        for (PendingToolCall feedback : feedbacks) {
            // 过滤已处理的工具调用，避免重新执行HITL
            if (hitlState.isConsumed(feedback.id())) {
                continue;
            }

            // 标记为已处理
            hitlState.markConsumed(feedback.id());

            // 如果用户同意执行，标记该工具名称为已审批，后续同名工具调用自动通过
            if (feedback.result() == PendingToolCall.FeedbackResult.APPROVED) {
                hitlState.markToolNameApproved(feedback.name());
            }

            toolCalls.add(new AssistantMessage.ToolCall(feedback.id(), "function", feedback.name(), feedback.arguments()));
        }

        // 只有在有中断工具执行的情况下，才需要补全tool_call的消息
        if (!toolCalls.isEmpty() && !hasNonInterruptedTool) {
            messages.add(
                    AssistantMessage.builder()
                            .toolCalls(toolCalls)
                            .build()
            );
        }

        for (PendingToolCall feedback : feedbacks) {
            // 把消费过的工具调用结果添加到消息中
            if (hitlState.isConsumed(feedback.id())) {
                String result;
                if (feedback.result() == PendingToolCall.FeedbackResult.REJECTED) {
                    result = "用户不同意执行此工具，工具名称：" + feedback.name() + ", 工具描述:" + feedback.description();
                } else {
                    // 这里同意和编辑简单处理，实际可以让用户重新编辑arguments
                    ToolCallback tool = findTool(feedback.name());
                    result = tool.call(feedback.arguments());
                }

                messages.add(
                        ToolResponseMessage.builder()
                                .responses(List.of(new ToolResponseMessage.ToolResponse(feedback.id(), feedback.name(), result))).build()
                );
            }
        }

        // 继续执行主循环
        return run(messages, context);
    }

    private ToolCallback findTool(String name) {
        return tools.stream()
                .filter(t -> t.getToolDefinition().name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private String name;
        private ChatModel chatModel;
        private List<ToolCallback> tools;

        private int maxRounds;

        private List<Advisor> advisors;

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

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public HITLReactAgent build() {
            if (chatModel == null) {
                throw new IllegalArgumentException("chatModel 不能为空！");
            }
            return new HITLReactAgent(name, chatModel, tools, advisors, maxRounds);
        }
    }

    public static void main(String[] args) {
        ChatModel chatModel = ChatModelConfig.getChatModel();
        ToolCallback[] toolCallbacks =
                ToolCallbacks.from(new WeatherService(), new SearchService());

        // 拦截 getWeather 和 search
        HITLAdvisor hitlAdvisor = new HITLAdvisor(Set.of("getWeather","search"));
        HITLReactAgent agent = HITLReactAgent.builder()
                .name("HITLReactAgent")
                .chatModel(chatModel)
                .advisors(List.of(hitlAdvisor))
                .tools(Arrays.stream(toolCallbacks).toList())
                .build();
        // 第一次 call
        AgentResult result = agent.call("北京今天的天气如何？并搜索下北京有什么好吃的饭店？基于以上的结果，搜索下，上海如何去这个好吃的饭店，上海");

        while (result instanceof AgentInterrupted interrupted) {
            System.out.println("===== HITL 中断 =====");

            // 模拟人工审批：一个工具对应一次用户输入
            List<PendingToolCall> feedbacks = new ArrayList<>();
            for (PendingToolCall pendingToolCall : interrupted.pendingToolCalls()) {
                System.out.println("=================== 需要用户审批的工具 =================");
                System.out.println("工具: " + pendingToolCall.name());
                System.out.println("参数: " + pendingToolCall.arguments());
                System.out.println("请输入审批结果（同意/拒绝）：");

                Scanner scanner = new Scanner(System.in);
                String approval = scanner.nextLine();

                if (approval.equalsIgnoreCase("同意")) {
                    feedbacks.add(pendingToolCall.approve());
                } else {
                    feedbacks.add(pendingToolCall.reject("用户拒绝使用"));
                }
            }

            // 再次发起调用
            result = agent.resume(interrupted, feedbacks);
        }

        if (result instanceof AgentFinished finished) {
            System.out.println("===== 最终结果 =====");
            System.out.println(finished.content());
        }
    }
}

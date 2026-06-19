package com.forever1996Fyk.ai.agent.agent.websearch;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.forever1996Fyk.ai.agent.agent.BaseAgent;
import com.forever1996Fyk.ai.agent.domain.SaveQuestionRequest;
import com.forever1996Fyk.ai.agent.domain.UpdateAnswerRequest;
import com.forever1996Fyk.ai.agent.domain.record.AgentState;
import com.forever1996Fyk.ai.agent.prompts.ReactAgentPrompts;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.google.common.collect.Lists;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:12
 **/
public class WebSearchReactAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(WebSearchReactAgent.class);
    private ChatClient chatClient;
    private final List<ToolCallback> tools;
    private final String systemPrompt;
    private final List<Advisor> advisors;


    public WebSearchReactAgent(String name, ChatModel chatModel, List<ToolCallback> tools, String systemPrompt, int maxRounds,
                               ChatMemory chatMemory, List<Advisor> advisors, AiSessionService sessionService) {
        super(name, chatModel, "websearch");
        this.tools = tools;
        this.systemPrompt = systemPrompt;
        this.advisors = advisors;
        this.sessionService = sessionService;
        this.chatMemory = chatMemory;
    }

    public static Builder builder() {
        return new Builder();
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

        // 注册任务到任务管理器 todo

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
        currentQuestion = question;

        // 添加记忆并保存到数据库
        if (sessionService != null) {
            AiSessionEntity savedSession = sessionService.saveQuestion(
                    SaveQuestionRequest.builder()
                            .sessionId(conversationId)
                            .question(question)
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
                })
                .doOnCancel(() -> {
                    hasSentFinalResult.set(true);
                })
                .doFinally(signalType -> {
                    log.info("最终答案: {}", finalAnswerBuffer);
                    log.info("思考过程: {}", thinkingBuffer);
                    // 保存结果到会话
                    saveSessionResult(conversationId, finalAnswerBuffer, thinkingBuffer, agentState);
                });
    }

    private void saveSessionResult(String conversationId, StringBuilder finalAnswerBuffer, StringBuilder thinkingBuffer, AgentState agentState) {
        if (sessionService != null && currentSessionId != null && finalAnswerBuffer.length() > 0) {
            long totalResponseTime = getTotalResponseTime();
            String toolsStr = getUsedToolsString();
            String referenceJson = "";
            if (CollectionUtils.isNotEmpty(agentState.getSearchResults())) {
                // 如果当前用到了search的工具，则解析search结果
//                referenceJson = createReferenceResponse(JSON.toJSONString(agentState.searchResults));
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

        public WebSearchReactAgent build() {
            if (chatModel == null) {
                throw new IllegalArgumentException("chatModel 不能为空！");
            }
            return new WebSearchReactAgent(name, chatModel, tools, systemPrompt, maxRounds, chatMemory, advisors, sessionService);
        }
    }
}

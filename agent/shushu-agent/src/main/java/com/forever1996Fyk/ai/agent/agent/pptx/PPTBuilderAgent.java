package com.forever1996Fyk.ai.agent.agent.pptx;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.forever1996Fyk.ai.agent.agent.BaseAgent;
import com.forever1996Fyk.ai.agent.agent.pptx.strategy.PptStateStrategyContext;
import com.forever1996Fyk.ai.agent.agent.pptx.strategy.PptStateStrategyFactory;
import com.forever1996Fyk.ai.agent.domain.PptIntentResult;
import com.forever1996Fyk.ai.agent.domain.SaveQuestionRequest;
import com.forever1996Fyk.ai.agent.domain.UpdateAnswerRequest;
import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.forever1996Fyk.ai.agent.service.AiPptTemplateService;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.forever1996Fyk.ai.agent.service.ImageGenerationService;
import com.forever1996Fyk.ai.agent.service.MinioService;
import com.forever1996Fyk.ai.agent.service.PptIntentRecognizerService;
import com.forever1996Fyk.ai.agent.service.PptRenderService;
import com.forever1996Fyk.ai.agent.service.impl.PptIntentRecognizerServiceImpl;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 22:42
 **/
@Slf4j
public class PPTBuilderAgent extends BaseAgent {
    private final ChatClient chatClient;
    private final AiPptInstService pptInstService;
    private final AiPptTemplateService pptTemplateService;
    private final PptRenderService pptRenderService;
    private final ImageGenerationService imageGenerationService;
    private final MinioService minioService;

    private final List<ToolCallback> toolCallbacks;
    private final PptIntentRecognizerService intentRecognizerService;
    private PptStateStrategyContext strategyContext;


    public PPTBuilderAgent(ChatModel chatModel,
                           AiSessionService sessionService,
                           AiPptInstService pptInstService,
                           AiPptTemplateService pptTemplateService,
                           AgentTaskManager agentTaskManager,
                           PptRenderService pptRenderService,
                           ImageGenerationService imageGenerationService,
                           MinioService minioService,
                           List<ToolCallback> toolCallbacks,
                           PptIntentRecognizerService intentRecognizerService) {
        super("PPTBuilderAgent", chatModel, "pptx");
        this.sessionService = sessionService;
        this.taskManager = agentTaskManager;
        this.toolCallbacks = toolCallbacks;

        this.pptInstService = pptInstService;
        this.pptTemplateService = pptTemplateService;
        this.pptRenderService = pptRenderService;
        this.imageGenerationService = imageGenerationService;
        this.minioService = minioService;

        // 意图识别器
        this.intentRecognizerService = intentRecognizerService;

        this.chatClient = ChatClient.builder(chatModel).build();

        // 初始化工具记录集合
        this.usedTools = Sets.newHashSet();
    }

    public PPTBuilderAgent(ChatModel chatModel,
                           AiSessionService sessionService,
                           AiPptInstService pptInstService,
                           AiPptTemplateService pptTemplateService,
                           AgentTaskManager agentTaskManager,
                           PptRenderService pptRenderService,
                           ImageGenerationService imageGenerationService,
                           MinioService minioService,
                           List<ToolCallback> toolCallbacks) {
        super("PPTBuilderAgent", chatModel, "pptx");
        this.sessionService = sessionService;
        this.taskManager = agentTaskManager;
        this.toolCallbacks = toolCallbacks;

        this.pptInstService = pptInstService;
        this.pptTemplateService = pptTemplateService;
        this.pptRenderService = pptRenderService;
        this.imageGenerationService = imageGenerationService;
        this.minioService = minioService;

        this.chatClient = ChatClient.builder(chatModel).build();

        // 意图识别器
        this.intentRecognizerService = new PptIntentRecognizerServiceImpl(chatClient, pptInstService);

        // 初始化工具记录集合
        this.usedTools = Sets.newHashSet();
    }


    @Override
    public Flux<String> execute(String conversationId, String question) {
        log.info("开始PPT处理: conversationId={}, question={}", conversationId, question);

        // 检查是否已有任务在执行
        Flux<String> checkResult = checkRunningTask(conversationId);
        if (checkResult != null) {
            return checkResult;
        }


        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 注册任务到管理器
        AgentTaskManager.TaskInfo taskInfo = registerTask(conversationId, sink);
        if (taskInfo == null && conversationId != null && taskManager != null) {
            return Flux.error(new IllegalStateException("该会话正在执行中，请稍后再试"));
        }
        // 收集思考过程
        StringBuilder thinkingBuffer = new StringBuilder();
        // 收集最终答案
        StringBuilder finalAnswerBuffer = new StringBuilder();
        try {
            // 初始化策略上下文
            initStrategyContext();

            // 1. 意图识别 todo
            PptIntentResult intentResult = intentRecognizerService.recognize(conversationId, question);

            // 保存对话
            if (sessionService != null) {
                AiSessionEntity savedSession = sessionService.saveQuestion(
                        SaveQuestionRequest.builder()
                                .sessionId(conversationId)
                                .question(question)
                                .build()
                );
                currentSessionId = savedSession.getId();
            }

            // 3. 根据意图路由
            switch (intentResult.intent()) {
                case CREATE_PPT -> handleCreateIntent(conversationId, question, sink, thinkingBuffer);
                case MODIFY_PPT -> handleModifyIntent(conversationId, question, sink, thinkingBuffer);
                case RESUME_PPT -> handleResumeIntent(conversationId, question, sink, thinkingBuffer);
                default -> {
                    sink.tryEmitNext(createThinkingResponse("❌ 无法识别您的意图，请重新表述\n"));
                    sink.tryEmitComplete();
                }
            }
        } catch (Exception e) {
            log.error("PPT处理异常", e);
            sink.tryEmitError(e);
        }
        return sink.asFlux()
                .doOnNext(chunk -> {
                    // 解析 JSON，分离收集 text 和 thinking
                    try {
                        JSONObject json = JSON.parseObject(chunk);
                        String type = json.getString("type");
                        if ("thinking".equals(type)) {
                            thinkingBuffer.append(json.getString("content"));
                        } else if ("text".equals(type)) {
                            finalAnswerBuffer.append(json.getString("content"));
                        }
                    } catch (Exception e) {
                        // 解析失败，忽略
                    }
                })
                .doOnCancel(() -> taskManager.stopTask(conversationId))
                .doFinally(signalType -> {
                    log.info("PPT处理完成");
                    log.info("最终答案: {}", finalAnswerBuffer);
                    log.info("思考过程: {}", thinkingBuffer);
                    // 保存结果到会话
                    if (sessionService != null && currentSessionId != null && !finalAnswerBuffer.isEmpty()) {
                        UpdateAnswerRequest request = UpdateAnswerRequest.builder()
                                .id(currentSessionId)
                                .answer(finalAnswerBuffer.toString())
                                .thinking(thinkingBuffer.toString())
                                .build();
                        sessionService.updateAnswer(request);
                        log.info("PPT结果已保存到会话: sessionId={}", currentSessionId);
                    }

                    // 流结束时移除任务
                    taskManager.stopTask(conversationId);
                })
                .doOnError(err -> log.error("PPT处理流输出异常", err));
    }

    /**
     * 处理恢复PPT意图
     */
    private void handleResumeIntent(String conversationId, String question, Sinks.Many<String> sink, StringBuilder thinkingBuffer) {
        // 获取最新的PPT实例
        AiPptInstEntity inst = pptInstService.getLatestInst(conversationId);

        if (inst == null) {
            String response = "当前会话中没有PPT实例，无法继续。请先创建一个PPT。";
            sink.tryEmitNext(createTextResponse(response));
            saveResultToSession(null, response, thinkingBuffer);
            sink.tryEmitComplete();
            return;
        }

        PptInstStatus status = inst.getStatusEnum();

        // 如果已经是SUCCESS状态，询问用户是否要修改
        if (status == PptInstStatus.SUCCESS) {
            sink.tryEmitNext(createThinkingResponse("当前PPT已经成功生成，如果您要修改，请说明具体修改需求。\n"));
            String response = "当前PPT已经成功生成。如果您需要修改，请说明具体的修改需求。";
            sink.tryEmitNext(createTextResponse(response));
            sink.tryEmitComplete();
            return;
        }

        sink.tryEmitNext(createThinkingResponse("正在从状态 " + status + " 继续执行PPT生成...\n"));

        // 直接从当前状态执行状态机
        PptStateStrategyFactory.getInstance().executeNextState(inst, sink, question, thinkingBuffer, strategyContext);
    }

    /**
     * 处理修改PPT意图
     */
    private void handleModifyIntent(String conversationId, String question, Sinks.Many<String> sink, StringBuilder thinkingBuffer) {
        // 获取最新的PPT实例
        AiPptInstEntity inst = pptInstService.getLatestInst(conversationId);

        if (inst == null) {
            String response = "当前会话中没有已生成的PPT，无法修改。请先生成一个PPT。";
            sink.tryEmitNext(createTextResponse(response));
            saveResultToSession(null, response, thinkingBuffer);
            sink.tryEmitComplete();
            return;
        }

        // 读取已有ppt_schema
        String currentSchema = inst.getPptSchema();
        if (currentSchema == null || currentSchema.isEmpty()) {
            String response = "该PPT没有Schema数据，无法修改。";
            sink.tryEmitNext(createTextResponse(response));
            saveResultToSession(null, response, thinkingBuffer);
            sink.tryEmitComplete();
            return;
        }

        sink.tryEmitNext(createThinkingResponse("正在修改PPT...\n"));

        // 设置修改操作标记和修改需求
        strategyContext.setModifyMode(true);
        strategyContext.setModifyQuery(question);

        // 生成修改后的Schema
        executeModifyFlow(inst, question, sink, thinkingBuffer);
    }

    /**
     * 修改PPT流程
     */
    private void executeModifyFlow(AiPptInstEntity inst, String question, Sinks.Many<String> sink, StringBuilder thinkingBuffer) {
        sink.tryEmitNext(createThinkingResponse("正在分析修改需求...\n"));
        sink.tryEmitNext(createThinkingResponse("正在修改PPT内容...\n"));

        // 直接调用 SchemaStrategy 继续执行（会处理图片生成、渲染等）
        PptStateStrategyFactory.getInstance().executeSchemaStrategy(inst, sink, question, thinkingBuffer, strategyContext);
    }

    /**
     * 处理创建PPT意图
     */
    private void handleCreateIntent(String conversationId, String question, Sinks.Many<String> sink, StringBuilder thinkingBuffer) {
        sink.tryEmitNext(createThinkingResponse("开始创建新的PPT...\n"));

        // 创建新的PPT实例
        AiPptInstEntity inst = pptInstService.createInst(conversationId, question);

        // 启动状态机循环
        PptStateStrategyFactory.getInstance().executeNextState(inst, sink, question, thinkingBuffer, strategyContext);
    }

    /**
     * 初始化策略上下文
     */
    private void initStrategyContext() {
        strategyContext = new PptStateStrategyContext(
                chatClient,
                chatModel,
                pptInstService,
                pptTemplateService,
                pptRenderService,
                imageGenerationService,
                minioService,
                sessionService,
                taskManager,
                toolCallbacks,
                chatMemory
        );
        strategyContext.setCurrentSessionId(currentSessionId);
    }

    /**
     * 保存结果到会话
     */
    private void saveResultToSession(AiPptInstEntity inst, String result, StringBuilder thinkingBuffer) {
        if (sessionService == null || currentSessionId == null) {
            return;
        }

        try {
            UpdateAnswerRequest request = UpdateAnswerRequest.builder()
                    .id(currentSessionId)
                    .answer(result)
                    .thinking(thinkingBuffer.toString())
                    .build();
            sessionService.updateAnswer(request);
            String conversationId = inst != null ? inst.getConversationId() : String.valueOf(currentSessionId);
            log.info("PPT生成结果已保存到会话: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("保存结果到会话失败", e);
        }
    }


}

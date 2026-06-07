package com.forever1996Fyk.ai.intelligent.customer.chat.controller;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.CommonChatService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.IntentRecognitionService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.TitleSummaryService;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatConversationEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatApplicationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/5 23:53
 **/
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private CommonChatService commonChatService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatConversationService chatConversationService;
    @Autowired
    private IntentRecognitionService intentRecognitionService;
    @Autowired
    private ChatApplicationService chatApplicationService;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatModelApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatModelBaseUrl;

    /**
     * 流式对话接口
     * <p>
     * 入参：userId、content（用户问题）、conversationId（可选）
     * 返回：SSE 流，每个 token 逐字推送；流结束前推送一条 [DONE] 事件携带 conversationId
     * <p>
     *
     * @param userId         用户ID
     * @param content        用户问题
     * @param conversationId 会话ID（可选，不传则自动创建新会话）
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> send(
            @RequestParam String userId,
            @RequestParam String content,
            @RequestParam(required = false) String conversationId
    ) {
        // 1. 处理会话：没有 conversationId 则创建新会话
        final String finalConversationId;
        if (!StringUtils.hasText(conversationId)) {
            // 同步：先用 content 前 20 个字符作为临时标题，快速建立回话
            String tempTitle = content.substring(0, Math.min(20, content.length()));
            finalConversationId = chatConversationService.createConversation(userId, tempTitle);
            log.info("创建新会话：conversationId={}, tempTitle={}", finalConversationId, tempTitle);
            // 异步：用虚拟线程调用 LLM 生成摘要标题，完成后回写到数据库
            Thread.ofVirtual().name("title-summary-" + finalConversationId).start(() -> {
                try {
                    // 1. 构建轻量级模型实例
                    OpenAiChatModel titleChatModel = OpenAiChatModel.builder()
                            .apiKey(chatModelApiKey)
                            // 轻量级模型，快速响应
                            .modelName("qwen3.5-flash")
                            // 适度创造性
                            .temperature(0.7)
                            .baseUrl(chatModelBaseUrl)
                            // 关闭思考链，加速响应
                            .customQueryParams(Map.of("enable_thinking", "false"))
                            .build();

                    // 2. 创建 AI Service 代理
                    TitleSummaryService titleSummaryService = AiServices.builder(TitleSummaryService.class)
                            .chatModel(titleChatModel)
                            .build();

                    // 3. 调用 LLM 生成标题
                    String aiTitle = titleSummaryService.generateTitle(content);

                    // 4. 更新数据库
                    chatConversationService.updateTitle(finalConversationId, aiTitle);
                    log.info("异步标题更新完成: conversationId={}, title={}", finalConversationId, aiTitle);
                } catch (Exception e) {
                    log.warn("异步标题生成失败, 保留临时标题: conversationId={}", finalConversationId, e);
                }
            });
        } else {
            finalConversationId = conversationId;
        }

        // 2. 保存用户信息
        String messageId = chatMessageService.saveUserMessage(finalConversationId, content);
        String assistantMessageId = chatMessageService.saveAssistantMessage(finalConversationId);

//        IntentRecognitionResult intentRecognitionResult = intentRecognitionService.chat(content);
//        // 如果意图识别关联性为 false，即表示与业务没有关联，就调用通用LLM 做对话
//        if (!intentRecognitionResult.related()) {
//            return commonChatService.streamChat(content)
//                    .concatWith( Flux.just("[DONE]:" + finalConversationId));
//        }

//        return chatApplicationService.streamChat(new ChatParam(userId, finalConversationId, messageId, content, assistantMessageId, intentRecognitionResult));
        // 这里需要再过程输出前面加上类似[PROGRESS]的标签，这样前端才能知道这个输出不是正文而是过程。
        return Flux.just("[PROGRESS]:正在识别您的意图...")
                .concatWith(
                        Mono.fromCallable(() -> intentRecognitionService.chat(content))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(intentRecognitionResult -> {
                                    // 如果用户问题不相关，使用一个通用的LLM做对话
                                    StringBuilder contentBuilder = new StringBuilder();
                                    if (!intentRecognitionResult.related()) {
                                        return Flux.concat(
                                                Flux.just("[PROGRESS]:正在为您生成回答..."),
                                                commonChatService.streamChat(content)
                                                        .concatWith( Flux.just("[DONE]:" + finalConversationId)));
                                    }

                                    // 5. 相关问题，走RAG流程（进度由内部组件发出）
                                    return chatApplicationService.streamChat(new ChatParam(userId, finalConversationId, messageId, content, assistantMessageId, intentRecognitionResult));
                                })
                )
                .doOnError(e -> log.error("流式对话异常：conversationId={}", finalConversationId))
                // 6. 在流末尾追加一条 [DONE] 事件，携带 conversationId
                .concatWith(Mono.just("[DONE]:" + finalConversationId));
    }

    /**
     * 查询指定用户的对话列表，按更新时间倒序排序
     *
     * @param userId 用户ID
     */
    @GetMapping("/list")
    public List<ChatConversationEntity> listConversations(@RequestParam String userId) {
        return chatConversationService.getConversationsByUserId(userId);
    }

    /**
     * 查询指定对话的消息列表，按创建时间正序排序
     *
     * @param conversationId 会话ID
     */
    @GetMapping("/messages")
    public List<ChatMessageEntity> listMessages(@RequestParam String conversationId) {
        return chatMessageService.getMessagesByConversationId(conversationId);
    }

    /**
     * 删除对话（同时删除该对话下所有消息）
     *
     * @param conversationId 会话ID
     */
    @DeleteMapping("/{conversationId}")
    public boolean deleteConversation(@PathVariable String conversationId) {
        chatMessageService.deleteMessagesByConversationId(conversationId);
        return chatConversationService.deleteConversation(conversationId);
    }
}

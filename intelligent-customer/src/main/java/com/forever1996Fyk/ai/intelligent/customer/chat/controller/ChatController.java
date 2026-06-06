package com.forever1996Fyk.ai.intelligent.customer.chat.controller;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.CommonChatService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.IntentRecognitionService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.TitleSummaryService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

        IntentRecognitionResult intentRecognitionResult = intentRecognitionService.chat(content);
        // 如果意图识别关联性为 false，即表示与业务没有关联，就调用通用LLM 做对话
        if (!intentRecognitionResult.related()) {
            return commonChatService.streamChat(content)
                    .concatWith( Flux.just("[DONE]:" + finalConversationId));
        }
        return Flux.just("[DONE]:" + finalConversationId);
    }
}

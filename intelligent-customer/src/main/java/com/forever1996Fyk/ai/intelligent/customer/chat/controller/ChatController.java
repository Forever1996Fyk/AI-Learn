package com.forever1996Fyk.ai.intelligent.customer.chat.controller;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.TitleSummaryService;
import com.forever1996Fyk.ai.intelligent.customer.auth.service.AuthService;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatSource;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatConversationEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatApplicationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ChatController  {
    @Autowired
    private AuthService authService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatApplicationService chatApplicationService;
    @Autowired
    private ChatConversationService chatConversationService;

    /**
     * 流式对话接口
     * <p>
     * 入参：userId、content（用户问题）、conversationId（可选）
     * 返回：SSE 流，每个 token 逐字推送；流结束前推送一条 [DONE] 事件携带 conversationId
     * <p>
     *
     * @param content        用户问题
     * @param conversationId 会话ID（可选，不传则自动创建新会话）
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> send(
            @RequestParam String content,
            @RequestParam(required = false) String conversationId
    ) {
        // 从当前上下文获取
        String userId = authService.getCurrentUserId();
        return chatApplicationService.chat(userId, content, conversationId, ChatSource.USER_WEB);
    }

    /**
     * 查询指定用户的对话列表，按更新时间倒序排序
     *
     * @param userId 用户ID
     */
    @GetMapping("/list")
    public List<ChatConversationEntity> listConversations() {
        String userId = authService.getCurrentUserId();
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

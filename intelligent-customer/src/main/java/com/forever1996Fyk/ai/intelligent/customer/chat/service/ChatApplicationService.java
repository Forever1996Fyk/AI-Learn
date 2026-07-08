package com.forever1996Fyk.ai.intelligent.customer.chat.service;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatSource;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 21:53
 **/
public interface ChatApplicationService {

    /**
     * chat
     *
     * @param userId         userId
     * @param content        content
     * @param conversationId conversationId
     * @param chatSource     chatSource
     * @return response
     */
    Flux<String> chat(String userId, String content, String conversationId, ChatSource chatSource);

    /**
     * chat
     *
     * @param chatParam chatParam
     * @return response
     */
    String chat(ChatParam chatParam);


    /**
     * chat
     *
     * @param chatParam chatParam
     * @return response
     */
    Flux<String> streamChat(ChatParam chatParam);
}

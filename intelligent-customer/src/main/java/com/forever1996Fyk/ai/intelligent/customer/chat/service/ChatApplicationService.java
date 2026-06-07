package com.forever1996Fyk.ai.intelligent.customer.chat.service;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
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

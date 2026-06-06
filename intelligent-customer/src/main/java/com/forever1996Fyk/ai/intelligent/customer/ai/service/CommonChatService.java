package com.forever1996Fyk.ai.intelligent.customer.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/6 17:45
 **/
@AiService
public interface CommonChatService {

    @SystemMessage("你是一个智能客服，请回答用户的问题。")
    Flux<String> streamChat(@UserMessage String message);
}

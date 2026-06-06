package com.forever1996Fyk.ai.intelligent.customer.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/6 22:35
 **/
public interface IntelligentCustomerChatAiService {

    Flux<String> streamChat(@MemoryId String conversationId, @UserMessage String message);

    String chat(@MemoryId String conversationId, @UserMessage String message);
}

package com.forever1996Fyk.ai.langchain4j.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 21:27
 **/
@AiService
public interface LangChainMemoryAiService {


    String chatMemory(@MemoryId String memoryId, @UserMessage String userMessage);
}

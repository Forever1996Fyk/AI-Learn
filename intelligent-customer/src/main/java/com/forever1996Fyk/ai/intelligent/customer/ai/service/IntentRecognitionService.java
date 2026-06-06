package com.forever1996Fyk.ai.intelligent.customer.ai.service;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * @program: AI-Learn
 * @description: 意图识别服务
 * @author: YuKai Fan
 * @create: 2026/6/6 17:11
 **/

// 这里用到wiringMode=EXPLICIT，防止注入全部的 LangChain4j 的组件导致启动报错，使用EXPLICIT 只会注册IntentRecognitionService 的 bean，但是需要手动注入 chatModel
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, streamingChatModel = "openAiStreamingChatModel")
public interface IntentRecognitionService {

    @SystemMessage(fromResource = "prompts/intent-recognition-new-prompt.txt")
    IntentRecognitionResult chat(@UserMessage String userMessage);
}

package com.forever1996Fyk.ai.intelligent.customer.ai.service;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @program: AI-Learn
 * @description: 意图识别服务
 * @author: YuKai Fan
 * @create: 2026/6/6 17:11
 **/
@AiService
public interface IntentRecognitionOldService {

    @SystemMessage(fromResource = "prompt/intent-recognition-old-prompt.txt")
    IntentRecognitionResult chat(@UserMessage String userMessage);
}

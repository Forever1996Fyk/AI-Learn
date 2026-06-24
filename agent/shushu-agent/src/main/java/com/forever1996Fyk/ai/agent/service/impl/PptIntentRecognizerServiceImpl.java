package com.forever1996Fyk.ai.agent.service.impl;

import com.forever1996Fyk.ai.agent.domain.PptIntentResult;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.forever1996Fyk.ai.agent.service.PptIntentRecognizerService;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 23:12
 **/
public class PptIntentRecognizerServiceImpl implements PptIntentRecognizerService {
    private final ChatClient chatClient;
    private final AiPptInstService pptInstService;

    public PptIntentRecognizerServiceImpl(ChatClient chatClient, AiPptInstService pptInstService) {
        this.chatClient = chatClient;
        this.pptInstService = pptInstService;
    }

    @Override
    public PptIntentResult recognize(String conversationId, String query) {
        return null;
    }
}

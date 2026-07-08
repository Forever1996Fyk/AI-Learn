package com.forever1996Fyk.ai.intelligent.customer.ai.model;

import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatSource;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 22:08
 **/
public record ChatParam(
        String userId,
        String conversationId,
        String messageId,
        String content,
        String assistantMessageId,
        IntentRecognitionResult intentRecognitionResult,
        ChatSource chatSource
) {
}

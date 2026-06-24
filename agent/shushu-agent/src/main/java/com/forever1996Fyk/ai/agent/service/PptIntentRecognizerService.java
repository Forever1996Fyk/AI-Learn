package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.domain.PptIntentResult;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 22:59
 **/
public interface PptIntentRecognizerService {

    /**
     * 意图识别
     *
     * @param conversationId 会话ID
     * @param query          查询内容
     * @return PPT意图识别结果
     */
    PptIntentResult recognize(String conversationId, String query);
}

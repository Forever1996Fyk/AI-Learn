package com.forever1996Fyk.ai.agent.domain;

import com.forever1996Fyk.ai.agent.enums.PptIntent;

/**
 * @program: AI-Learn
 * @description: 意图识别结果
 * @author: YuKai Fan
 * @create: 2026/6/24 23:00
 **/
public record PptIntentResult(PptIntent intent, String reason) {
}
